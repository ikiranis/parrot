package eu.apps4net.parrotApp.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import eu.apps4net.parrotApp.exceptions.ProcessingErrorException;
import eu.apps4net.parrotApp.models.LibraryFolder;
import eu.apps4net.parrotApp.models.MediaFile;
import eu.apps4net.parrotApp.models.MediaKind;
import eu.apps4net.parrotApp.models.ScanJobState;
import eu.apps4net.parrotApp.models.ScanPhase;
import eu.apps4net.parrotApp.models.ScanResult;
import eu.apps4net.parrotApp.repositories.MediaFileRepository;
import eu.apps4net.parrotApp.services.tagscanner.MediaTagScanner;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Service that recursively scans a server-side directory for media files and
 * persists them in three phases:
 *
 * 1. Folder scan (COLLECTING) – a single directory-tree walk collects every leaf directory
 *    (a directory containing at least one direct file) across all configured library folders
 *    and, in the same pass, counts the total number of media files on disk.
 * 2. File scan (SCANNING) – for each changed leaf directory, detects media files by
 *    extension, determines their {@link MediaKind}, and saves a {@link MediaFile} record
 *    for each new file.  Already-indexed files are skipped.
 * 3. Tag scan (TAGGING) – dispatches each saved {@link MediaFile} to the appropriate
 *    {@link MediaTagScanner} implementation based on its {@link MediaKind}.
 *
 * Both the file scan and the tag scan distribute their work across a pool of
 * {@code maxThreads} worker threads using a shared cursor, so that a worker which finishes
 * its current unit immediately picks up the next one.  This keeps every thread busy even
 * when the work is unevenly distributed (a few very large folders among many small ones),
 * which static range-partitioning fails to do.
 *
 * In background scans, phases 2 and 3 run concurrently: the tag scanner starts in a
 * separate thread alongside the file scanner and polls the database for newly added files.
 * If no untagged records are found while the file scan is still in progress, the tag
 * scanner sleeps in 5-second increments and wakes immediately when the file scan finishes.
 *
 * When called with a {@link ScanJobState}, phase transitions and every counter increment
 * are mirrored into the job state so that background scans expose live progress and error
 * logs over the REST API.
 */
@Service
public class MediaScanService {

	/** Logger for phase timing and progress diagnostics. */
	private static final Logger log = LoggerFactory.getLogger(MediaScanService.class);

	/** Number of MediaFile records fetched from the DB per batch during Phase 3 (tag scanning). */
	private static final int TAG_SCAN_BATCH_SIZE = 1000;

	/**
	 * Number of files a tag-scan worker claims from the shared cursor at a time. Small enough
	 * to keep load balanced across threads, large enough to amortise the per-batch JDBC insert.
	 */
	private static final int TAG_CHUNK_SIZE = 50;

	/**
	 * Maximum number of candidate filenames per directory existence-check query. Bounds the size
	 * of the {@code IN} list so that a directory with very many files is queried in several
	 * smaller statements rather than one oversized one.
	 */
	private static final int EXISTENCE_QUERY_CHUNK_SIZE = 500;

	/**
	 * Number of rows inserted per flush in Phase 2. A directory's new files are saved in batches of
	 * this size instead of one statement for the whole directory, so that the {@code insertLock} is
	 * held only briefly at a time. This keeps a single very large folder from blocking every other
	 * worker's inserts for the entire folder, which otherwise stalls throughput and then makes the
	 * Added counter jump in one big step when the folder finally commits.
	 */
	private static final int INSERT_FLUSH_SIZE = 1000;

	/** Supported image file extensions (lower-case, without the leading dot). */
	private static final Set<String> IMAGE_EXTENSIONS = Set.of(
			"jpg", "jpeg", "png", "gif", "bmp", "webp", "tiff", "tif"
	);

	/** Supported video file extensions (lower-case, without the leading dot). */
	private static final Set<String> VIDEO_EXTENSIONS = Set.of(
			"mp4", "mkv", "avi", "mov", "wmv", "flv", "webm", "m4v"
	);

	/** Supported audio file extensions (lower-case, without the leading dot). */
	private static final Set<String> AUDIO_EXTENSIONS = Set.of(
			"mp3", "flac", "wav", "aac", "ogg", "m4a", "wma"
	);

	/** Repository for querying and persisting media file records. */
	private final MediaFileRepository mediaFileRepository;

	/** Service used to check and persist folder records. */
	private final FolderService folderService;

	/** Service used to read application settings, including {@code maxThreads}. */
	private final SettingService settingService;

	/** Service used to retrieve the configured library folder paths. */
	private final LibraryFolderService libraryFolderService;

	/**
	 * JDBC template used for bulk inserts in Phase 2, bypassing the JPA persistence
	 * context so that inserted rows never accumulate in Hibernate's first-level cache.
	 */
	private final JdbcTemplate jdbcTemplate;

	/**
	 * Serialises concurrent INSERT batches so that Derby does not time out on lock
	 * acquisition when multiple scanner threads flush rows simultaneously.
	 */
	private final Object insertLock = new Object();

	/**
	 * Map of {@link MediaKind} to its corresponding {@link MediaTagScanner},
	 * built from all {@link MediaTagScanner} beans present in the application context.
	 */
	private final Map<MediaKind, MediaTagScanner> tagScanners;

	/**
	 * Constructs a new {@code MediaScanService}.
	 * All {@link MediaTagScanner} beans in the context are collected and indexed by
	 * their supported {@link MediaKind}.
	 *
	 * @param mediaFileRepository   repository for media file persistence
	 * @param folderService         service for folder change detection and persistence
	 * @param settingService        service for reading application settings
	 * @param libraryFolderService  service for retrieving configured library folder paths
	 * @param jdbcTemplate          JDBC template for bulk inserts
	 * @param scannerList           all registered {@link MediaTagScanner} implementations
	 */
	public MediaScanService(MediaFileRepository mediaFileRepository,
							FolderService folderService,
							SettingService settingService,
							LibraryFolderService libraryFolderService,
							JdbcTemplate jdbcTemplate,
							List<MediaTagScanner> scannerList) {
		this.mediaFileRepository = mediaFileRepository;
		this.folderService = folderService;
		this.settingService = settingService;
		this.libraryFolderService = libraryFolderService;
		this.jdbcTemplate = jdbcTemplate;
		this.tagScanners = new EnumMap<>(MediaKind.class);
		for (MediaTagScanner scanner : scannerList) {
			tagScanners.put(scanner.getSupportedKind(), scanner);
		}
	}

	/**
	 * Scans the library folder matching the given path for media files and saves them
	 * to the database. The given path must exactly match a configured {@link LibraryFolder}.
	 *
	 * Phase 1 collects all leaf directories under the library folder and asks
	 * {@link FolderService#checkAndSaveFolder} whether each one has changed.
	 * Phase 2 scans the direct files of every changed leaf directory and persists a
	 * {@link MediaFile} for each new file whose extension maps to a known {@link MediaKind}.
	 * Phase 3 iterates over the saved files and invokes the matching {@link MediaTagScanner}.
	 *
	 * @param folderPath absolute path to the library folder to scan; must match a configured library folder
	 * @return {@link ScanResult} with counts of added, skipped, and errored files
	 * @throws ProcessingErrorException if the path does not match any configured library folder
	 */
	public ScanResult scanFolder(String folderPath) {
		LibraryFolder libraryFolder = libraryFolderService.findMatchingForPath(folderPath)
				.orElseThrow(() -> new ProcessingErrorException(
						"No library folder configured for path: " + folderPath));
		return scanLibraryFolder(libraryFolder, null);
	}

	/**
	 * Scans all configured {@link LibraryFolder} paths and aggregates the results
	 * into a single {@link ScanResult}.
	 * Each library folder is scanned independently.
	 *
	 * @return aggregated {@link ScanResult} across all library folders, or a no-op result
	 *         if no library folders are configured
	 */
	public ScanResult scanLibraryFolders() {
		List<LibraryFolder> libraryFolders = libraryFolderService.getAll();

		if (libraryFolders.isEmpty()) {
			return new ScanResult(0, 0, 0, 0, 0, "No library folders configured");
		}

		int totalAdded = 0, totalSkipped = 0, totalErrors = 0, totalFoldersScanned = 0, totalFoldersSkipped = 0;

		for (LibraryFolder libraryFolder : libraryFolders) {
			ScanResult result = scanLibraryFolder(libraryFolder, null);
			totalAdded += result.added();
			totalSkipped += result.skipped();
			totalErrors += result.errors();
			totalFoldersScanned += result.foldersScanned();
			totalFoldersSkipped += result.foldersSkipped();
		}

		return new ScanResult(totalAdded, totalSkipped, totalErrors, totalFoldersScanned, totalFoldersSkipped,
				"Scan complete. Added: " + totalAdded +
				", Skipped: " + totalSkipped +
				", Errors: " + totalErrors +
				", Folders scanned: " + totalFoldersScanned +
				", Folders skipped: " + totalFoldersSkipped);
	}

	/**
	 * Scans all configured {@link LibraryFolder} paths in the background, writing live
	 * progress into the provided {@link ScanJobState} as each file and folder is processed.
	 *
	 * The scan proceeds in three phases:
	 * 1. COLLECTING – discover all leaf directories across every library folder.
	 * 2. SCANNING   – inspect each directory for new media files (concurrent with phase 3).
	 * 3. TAGGING    – read metadata tags for newly added files (concurrent with phase 2).
	 *
	 * Phases 2 and 3 run in parallel: the tag scanner polls the database for untagged records
	 * while the file scan is running. If no untagged records are found while the file scan is
	 * still in progress, the tag scanner sleeps for up to 3 minutes before retrying.
	 *
	 * Marks the job state as completed or failed when both phases finish.
	 * Intended to be called from {@link ScanJobService} on a background thread.
	 *
	 * @param state the job state to update throughout the scan
	 */
	void scanLibraryFolders(ScanJobState state) {
		List<LibraryFolder> libraryFolders = libraryFolderService.getAll();

		if (libraryFolders.isEmpty()) {
			state.complete("No library folders configured");
			return;
		}

		try {
			// Read the configured thread count once; reused by both concurrent phases below.
			int maxThreads = settingService.getMaxThreads();

			// Record pre-scan DB count so the frontend can show context for re-scans
			state.setInitialFilesCount((int) mediaFileRepository.count());

			// Phase 1: collect all leaf directories (and count on-disk media files) via a parallel walk
			state.setPhase(ScanPhase.COLLECTING);
			long collectStart = System.nanoTime();
			List<LeafDirEntry> allLeafDirs = collectAllLeafDirs(libraryFolders, state, maxThreads);
			state.setTotalFolders(allLeafDirs.size());
			log.info("Scan COLLECTING done in {} ms with {} threads: {} leaf folders, {} media files on disk",
					millisSince(collectStart), maxThreads, allLeafDirs.size(), state.getTotalMediaFilesInLibrary());

			// Phases 2 and 3 run concurrently.
			// fileScanDone is set in a finally block so the tag thread always gets a termination signal.
			AtomicBoolean fileScanDone = new AtomicBoolean(false);
			ExecutorService phaseExecutor = Executors.newFixedThreadPool(2);
			long scanStart = System.nanoTime();

			Future<?> scanFuture = phaseExecutor.submit(() -> {
				try {
					state.setPhase(ScanPhase.SCANNING);
					scanAllLeafDirs(allLeafDirs, state, maxThreads);
					allLeafDirs.clear();
				} finally {
					fileScanDone.set(true);
					log.info("Scan SCANNING done in {} ms: added {}, skipped {}, folders scanned {}, skipped {}",
							millisSince(scanStart), state.getAdded(), state.getSkipped(),
							state.getFoldersScanned(), state.getFoldersSkipped());
				}
			});

			Future<?> tagFuture = phaseExecutor.submit(() -> {
				runTagScannersParallel(state, fileScanDone, maxThreads);
				log.info("Scan TAGGING done in {} ms: tagged {}, errors {}",
						millisSince(scanStart), state.getTagged(), state.getErrors());
			});

			phaseExecutor.shutdown();
			awaitAll(List.of(scanFuture, tagFuture), state);

			String summary = "Added: " + state.getAdded() +
					", Skipped: " + state.getSkipped() +
					", Tagged: " + state.getTagged() +
					", Errors: " + state.getErrors() +
					", Folders scanned: " + state.getFoldersScanned() +
					", Folders skipped: " + state.getFoldersSkipped();
			if (state.isCancelRequested()) {
				state.cancel("Scan cancelled. " + summary);
			} else {
				state.complete("Scan complete. " + summary);
			}
		} catch (Exception e) {
			state.fail("Scan failed: " + e.getMessage());
		}
	}

	/**
	 * Phase 1 (background) — walks every configured library folder once and collects all leaf
	 * directories into a flat list paired with their library root for later use in tag scanning.
	 * The total number of media files found on disk is summed across all walks and written to
	 * {@code state} as the progress denominator. Errors accessing individual library folders are
	 * logged to {@code state} and skipped.
	 *
	 * @param libraryFolders configured library folders to walk
	 * @param state          job state for error logging and the on-disk media total
	 * @param maxThreads     number of threads to use for each parallel directory-tree walk
	 * @return flat list of all leaf directory entries across all library folders
	 */
	private List<LeafDirEntry> collectAllLeafDirs(List<LibraryFolder> libraryFolders, ScanJobState state,
												  int maxThreads) {
		List<LeafDirEntry> allLeafDirs = new ArrayList<>();
		int totalMediaFiles = 0;
		for (LibraryFolder lf : libraryFolders) {
			if (state.isCancelRequested()) break;
			Path root = Paths.get(lf.getPath());
			if (!Files.exists(root) || !Files.isDirectory(root)) {
				state.incrementErrors();
				state.addErrorLog("Library folder not accessible: " + lf.getPath());
				continue;
			}
			try {
				CollectResult collected = collectLeafDirs(root, maxThreads, state);
				for (LeafDirInfo info : collected.leafDirs()) {
					allLeafDirs.add(new LeafDirEntry(
							info.dir(), root, lf, info.fileCount(), info.totalBytes(), info.mediaCount()));
				}
				totalMediaFiles += collected.mediaCount();
			} catch (Exception e) {
				state.incrementErrors();
				state.addErrorLog("Failed to walk " + lf.getPath() + ": " + e.getMessage());
			}
		}
		state.setTotalMediaFilesInLibrary(totalMediaFiles);
		return allLeafDirs;
	}

	/**
	 * Phase 2 (background) — distributes the leaf directories across a pool of worker threads
	 * using a shared cursor, so that whenever a worker finishes a directory it immediately
	 * claims the next unprocessed one regardless of how the work is distributed. Each directory
	 * is listed from disk exactly once, and its files are reused for change detection and saving.
	 *
	 * @param leafDirs   all leaf directory entries to process
	 * @param state      job state for live counter updates and error logging
	 * @param maxThreads maximum number of worker threads to use
	 */
	private void scanAllLeafDirs(List<LeafDirEntry> leafDirs, ScanJobState state, int maxThreads) {
		int total = leafDirs.size();
		if (total == 0) return;

		int threads = Math.min(maxThreads, total);
		Set<String> ensuredAncestors = ConcurrentHashMap.newKeySet();
		AtomicInteger cursor = new AtomicInteger(0);

		ExecutorService executor = Executors.newFixedThreadPool(threads);
		List<Future<?>> futures = new ArrayList<>();

		for (int t = 0; t < threads; t++) {
			futures.add(executor.submit(() -> {
				int idx;
				while (!state.isCancelRequested() && (idx = cursor.getAndIncrement()) < total) {
					processLeafDir(leafDirs.get(idx), state, ensuredAncestors);
				}
			}));
		}

		executor.shutdown();
		awaitAll(futures, state);
	}

	/**
	 * Phase 2 (background) — processes a single leaf directory using the file counts already
	 * captured during the Phase 1 walk: asks {@link FolderService#checkAndSaveFolder} whether the
	 * directory changed (without re-stating its files), and either records it as skipped or saves
	 * its new media files and marks it finished. An unchanged directory therefore touches no files
	 * on disk at all.
	 *
	 * @param entry            the leaf directory entry, including its Phase 1 tallies
	 * @param state            job state for live counter updates and error logging
	 * @param ensuredAncestors per-scan cache of ancestor folders already ensured
	 */
	private void processLeafDir(LeafDirEntry entry, ScanJobState state, Set<String> ensuredAncestors) {
		Path leafDir = entry.leafDir();

		boolean hasChanges;
		try {
			hasChanges = folderService.checkAndSaveFolder(leafDir, entry.root(), entry.libraryFolder(),
					entry.fileCount(), entry.totalBytes(), ensuredAncestors);
		} catch (Exception e) {
			state.incrementErrors();
			state.addErrorLog("Folder check failed: " + leafDir + " — " + e.getMessage());
			return;
		}

		if (!hasChanges) {
			state.incrementFoldersSkipped();
			state.addSkipped(entry.mediaCount());
			return;
		}

		state.incrementFoldersScanned();
		saveDirectoryFiles(leafDir, entry.libraryFolder(), entry.root(), state);
		String relativePath = entry.root().relativize(leafDir).toString();
		folderService.markFinished(entry.libraryFolder(), relativePath);
	}

	/**
	 * Phase 3 (parallel background) — polls the database for {@link MediaFile} records without
	 * tags and processes them while Phase 2 is still running.
	 *
	 * Each kind is drained with a forward id cursor: every batch fetches untagged rows with an id
	 * greater than the highest id seen so far for that kind, so already-drained rows are never
	 * re-examined and the whole backlog is processed in O(n) rather than the O(n squared) that
	 * results from repeatedly fetching page 0 and skipping a growing prefix of tagged rows. New
	 * rows inserted by the concurrent file scan have higher ids and are picked up on a later pass.
	 *
	 * If no untagged records are found and the file scan has not yet finished, the thread sleeps
	 * in 2-second increments (up to 3 minutes total) and exits the sleep immediately when the file
	 * scan finishes. Once the file scan is complete and no untagged records remain, the method returns.
	 *
	 * A single worker pool is created for the whole tagging phase and reused across every batch,
	 * rather than being torn down and recreated per batch.
	 *
	 * The progress denominator is set in three stages:
	 * 1. Before the first batch: a one-time count of pre-existing untagged rows gives a non-zero
	 *    baseline for re-scans where a previous run was interrupted before completing tagging.
	 * 2. While the file scan is running: {@code initialUntagged + state.getAdded()} tracks the
	 *    growing total so the frontend shows a meaningful ratio (e.g. "14,200 / 81,353") instead
	 *    of the misleading "tagged + one-batch" figure that would otherwise result.
	 * 3. When the file scan finishes: the remaining untagged rows are counted once to set an
	 *    exact final denominator.
	 *
	 * @param state        job state for live counter updates, phase transitions, and error logging
	 * @param fileScanDone flag set to {@code true} by Phase 2 when all directories have been processed
	 * @param maxThreads   maximum number of worker threads to use
	 */
	private void runTagScannersParallel(ScanJobState state, AtomicBoolean fileScanDone, int maxThreads) {
		ExecutorService executor = Executors.newFixedThreadPool(maxThreads);
		Map<MediaKind, Long> watermark = new EnumMap<>(MediaKind.class);
		for (MediaKind kind : tagScanners.keySet()) {
			watermark.put(kind, 0L);
		}

		// Count pre-existing untagged files once at startup. On a first scan this is near zero;
		// on a re-scan after an interrupted previous run it can be large, and without this baseline
		// the denominator would start at 0 and the display would be misleading until files are added.
		long initialUntagged = 0;
		for (MediaKind kind : tagScanners.keySet()) {
			initialUntagged += mediaFileRepository.countByKindWithoutPhotoTag(kind);
		}
		if (initialUntagged > 0) {
			state.setTotalFiles((int) initialUntagged);
		}
		final long baseline = initialUntagged;

		try {
			boolean finalTotalSet = false;

			while (true) {
				if (state.isCancelRequested()) {
					break;
				}

				// Once the file scan has finished the row set is stable, so count the remaining
				// untagged rows a single time to fix an accurate progress denominator.
				if (fileScanDone.get() && !finalTotalSet) {
					long remaining = 0;
					for (MediaKind kind : tagScanners.keySet()) {
						remaining += mediaFileRepository.countByKindWithoutPhotoTag(kind);
					}
					state.setTotalFiles((int) (state.getTagged() + remaining));
					state.setPhase(ScanPhase.TAGGING);
					finalTotalSet = true;
				}

				boolean processedAny = false;

				for (Map.Entry<MediaKind, MediaTagScanner> entry : tagScanners.entrySet()) {
					MediaKind kind = entry.getKey();
					List<MediaFile> batch = mediaFileRepository.findByKindWithoutPhotoTagAfterId(
							kind, watermark.get(kind), PageRequest.of(0, TAG_SCAN_BATCH_SIZE));
					if (batch.isEmpty()) {
						continue;
					}
					processedAny = true;
					// Batch is ordered by ascending id, so the last element carries the highest id.
					watermark.put(kind, batch.get(batch.size() - 1).getId());

					// While the file scan is still adding rows, track "pre-existing untagged + newly
					// added this session" as the running denominator. This produces a meaningful ratio
					// like "14,200 / 81,353" rather than "14,200 / 15,000" (tagged + one batch).
					if (!finalTotalSet) {
						state.setTotalFiles((int) Math.max(state.getTotalFiles(), baseline + state.getAdded()));
					}

					processTagBatch(batch, entry.getValue(), state, executor, maxThreads);
				}

				if (!processedAny) {
					if (fileScanDone.get()) {
						break;
					}
					try {
						long deadline = System.currentTimeMillis() + 3 * 60 * 1000L;
						while (!fileScanDone.get() && !state.isCancelRequested()
								&& System.currentTimeMillis() < deadline) {
							Thread.sleep(2_000L);
						}
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
						return;
					}
				}
			}
		} finally {
			executor.shutdown();
		}
	}

	/**
	 * Distributes a single tag batch across worker threads using a shared cursor on the supplied
	 * executor, invoking the given {@link MediaTagScanner} for chunks of {@link #TAG_CHUNK_SIZE}
	 * files at a time. Workers claim the next chunk as soon as they finish the previous one, so
	 * threads stay busy even when individual files take very different amounts of time to tag.
	 * The library root for each file is resolved directly from its {@link LibraryFolder} association.
	 *
	 * @param batch    MediaFile records to tag
	 * @param scanner  the scanner to use for every file in this batch
	 * @param state    job state for live counter updates and error logging
	 * @param executor the shared worker pool for the tagging phase
	 * @param threads  the maximum number of worker threads available
	 */
	private void processTagBatch(List<MediaFile> batch, MediaTagScanner scanner, ScanJobState state,
								 ExecutorService executor, int threads) {
		int total = batch.size();
		if (total == 0) return;

		int chunks = (int) Math.ceil((double) total / TAG_CHUNK_SIZE);
		int workers = Math.max(1, Math.min(threads, chunks));
		AtomicInteger cursor = new AtomicInteger(0);
		List<Future<?>> futures = new ArrayList<>();

		for (int w = 0; w < workers; w++) {
			futures.add(executor.submit(() -> {
				int start;
				while ((start = cursor.getAndAdd(TAG_CHUNK_SIZE)) < total) {
					int end = Math.min(start + TAG_CHUNK_SIZE, total);
					List<MediaFile> chunk = batch.subList(start, end);
					try {
						scanner.scanTagsBatch(chunk, mf -> Paths.get(mf.getLibraryFolder().getPath()));
					} catch (Exception e) {
						state.addErrors(chunk.size());
						state.addErrorLog("Batch tag scan failed: " + e.getMessage());
					}
					state.addTagged(chunk.size());
				}
			}));
		}

		awaitAll(futures, state);
	}

	/**
	 * Scans a single library folder recursively, mirroring each counter increment into
	 * {@code jobState} when provided so that background scans expose live progress.
	 *
	 * @param libraryFolder the library folder to scan
	 * @param jobState      job state to update in real time, or {@code null} for synchronous scans
	 * @return {@link ScanResult} with counts of added, skipped, and errored files
	 */
	private ScanResult scanLibraryFolder(LibraryFolder libraryFolder, ScanJobState jobState) {
		Path root = Paths.get(libraryFolder.getPath());

		if (!Files.exists(root)) {
			return new ScanResult(0, 0, 0, 0, 0, "Folder does not exist: " + libraryFolder.getPath());
		}

		if (!Files.isDirectory(root)) {
			return new ScanResult(0, 0, 0, 0, 0, "Path is not a directory: " + libraryFolder.getPath());
		}

		ScanContext ctx = new ScanContext(libraryFolder, jobState);

		List<LeafDirInfo> leafDirs = collectLeafDirs(root, settingService.getMaxThreads(), jobState).leafDirs();

		scanLeafDirectories(leafDirs, ctx);
		leafDirs.clear();
		runTagScanners(ctx);
		ctx.savedEntries.clear();

		return new ScanResult(ctx.added.get(), ctx.skipped.get(), ctx.errors.get(),
				ctx.foldersScanned.get(), ctx.foldersSkipped.get(),
				"Scan complete. Added: " + ctx.added.get() +
				", Skipped: " + ctx.skipped.get() +
				", Errors: " + ctx.errors.get() +
				", Folders scanned: " + ctx.foldersScanned.get() +
				", Folders skipped: " + ctx.foldersSkipped.get());
	}

	/**
	 * Phase 1 (parallel walk) — walks {@code root} recursively across a {@link ForkJoinPool} and
	 * collects every directory that contains at least one direct regular file, while counting the
	 * total number of media files (by extension) encountered along the way. Each subdirectory is
	 * forked as its own task, so the work is spread over up to {@code maxThreads} threads instead
	 * of crawling the tree on a single thread; this is the dominant cost on large or
	 * high-latency (e.g. network) filesystems. A directory is detected as a leaf by observing its
	 * own direct files, so no directory is listed a second time.
	 *
	 * Directories whose names start with {@code #} (e.g. {@code #recycle}) or {@code .}
	 * (hidden directories) are skipped entirely, as are any directories that cannot be accessed.
	 *
	 * @param root       the root directory to walk
	 * @param maxThreads the maximum number of threads for the parallel walk
	 * @param state      job state polled for cancellation, or {@code null} for synchronous scans
	 * @return the leaf directories and the on-disk media file count discovered under {@code root}
	 */
	private CollectResult collectLeafDirs(Path root, int maxThreads, ScanJobState state) {
		List<LeafDirInfo> leafDirs = Collections.synchronizedList(new ArrayList<>());
		AtomicInteger mediaCount = new AtomicInteger(0);

		ForkJoinPool pool = new ForkJoinPool(Math.max(1, maxThreads));
		try {
			pool.invoke(new DirectoryWalkTask(root, leafDirs, mediaCount, state));
		} finally {
			pool.shutdown();
		}

		return new CollectResult(new ArrayList<>(leafDirs), mediaCount.get());
	}

	/**
	 * Returns the number of whole milliseconds elapsed since the given {@link System#nanoTime()}
	 * reading.
	 *
	 * @param startNanos a prior {@code System.nanoTime()} value
	 * @return elapsed time in milliseconds
	 */
	private static long millisSince(long startNanos) {
		return (System.nanoTime() - startNanos) / 1_000_000L;
	}

	/**
	 * Phase 2 (synchronous) — distributes {@code leafDirs} across a pool of worker threads using
	 * a shared cursor so that idle workers immediately claim the next unprocessed directory.
	 * For each directory, delegates to {@link FolderService#checkAndSaveFolder}; changed
	 * directories have their direct regular files scanned and saved.
	 *
	 * @param leafDirs directories to process, each with its Phase 1 tallies
	 * @param ctx      mutable scan state shared across all threads
	 */
	private void scanLeafDirectories(List<LeafDirInfo> leafDirs, ScanContext ctx) {
		int total = leafDirs.size();
		if (total == 0) return;

		int threads = Math.min(settingService.getMaxThreads(), total);
		Set<String> ensuredAncestors = ConcurrentHashMap.newKeySet();
		AtomicInteger cursor = new AtomicInteger(0);

		ExecutorService executor = Executors.newFixedThreadPool(threads);
		List<Future<?>> futures = new ArrayList<>();

		for (int t = 0; t < threads; t++) {
			futures.add(executor.submit(() -> {
				int idx;
				while ((idx = cursor.getAndIncrement()) < total) {
					processLeafDir(leafDirs.get(idx), ctx, ensuredAncestors);
				}
			}));
		}

		executor.shutdown();
		awaitAll(futures, ctx);
	}

	/**
	 * Phase 2 (synchronous) — processes a single leaf directory using the file counts captured
	 * during the Phase 1 walk: asks {@link FolderService#checkAndSaveFolder} whether the directory
	 * changed (without re-stating its files), and either records it as skipped or scans and saves
	 * its new media files and marks it finished.
	 *
	 * @param info             the leaf directory and its Phase 1 tallies
	 * @param ctx              mutable scan state shared across all threads
	 * @param ensuredAncestors per-scan cache of ancestor folders already ensured
	 */
	private void processLeafDir(LeafDirInfo info, ScanContext ctx, Set<String> ensuredAncestors) {
		Path leafDir = info.dir();

		boolean hasChanges;
		try {
			hasChanges = folderService.checkAndSaveFolder(
					leafDir, ctx.root, ctx.libraryFolder, info.fileCount(), info.totalBytes(), ensuredAncestors);
		} catch (Exception e) {
			ctx.incrementErrors("Folder check failed: " + leafDir, e.getMessage());
			return;
		}

		if (!hasChanges) {
			ctx.incrementFoldersSkipped();
			ctx.addSkipped(info.mediaCount());
			return;
		}

		ctx.incrementFoldersScanned();
		scanDirectoryFiles(leafDir, ctx);
		String relativePath = ctx.root.relativize(leafDir).toString();
		folderService.markFinished(ctx.libraryFolder, relativePath);
	}

	/**
	 * Phase 3 (synchronous) — distributes the saved entries across worker threads using a shared
	 * cursor, processing {@link #TAG_CHUNK_SIZE} entries at a time. For each entry, looks up the
	 * registered {@link MediaTagScanner} for its {@link MediaKind} and invokes tag scanning.
	 *
	 * @param ctx mutable scan state holding the saved entries, scan root, and error counter
	 */
	private void runTagScanners(ScanContext ctx) {
		// Snapshot to a plain list so disjoint index ranges can be read concurrently without locking.
		List<FileScanEntry> entries = new ArrayList<>(ctx.savedEntries);
		int total = entries.size();
		if (total == 0) return;

		int chunks = (int) Math.ceil((double) total / TAG_CHUNK_SIZE);
		int threads = Math.max(1, Math.min(settingService.getMaxThreads(), chunks));
		AtomicInteger cursor = new AtomicInteger(0);

		ExecutorService executor = Executors.newFixedThreadPool(threads);
		List<Future<?>> futures = new ArrayList<>();

		for (int t = 0; t < threads; t++) {
			futures.add(executor.submit(() -> {
				int start;
				while ((start = cursor.getAndAdd(TAG_CHUNK_SIZE)) < total) {
					int end = Math.min(start + TAG_CHUNK_SIZE, total);
					List<FileScanEntry> chunk = entries.subList(start, end);
					Map<MediaKind, List<FileScanEntry>> byKind = chunk.stream()
							.collect(Collectors.groupingBy(e -> e.mediaFile().getKind()));
					for (Map.Entry<MediaKind, List<FileScanEntry>> kindEntry : byKind.entrySet()) {
						MediaTagScanner scanner = tagScanners.get(kindEntry.getKey());
						if (scanner == null) continue;
						List<MediaFile> kindFiles = kindEntry.getValue().stream()
								.map(FileScanEntry::mediaFile).collect(Collectors.toList());
						try {
							scanner.scanTagsBatch(kindFiles, mf -> ctx.root);
						} catch (Exception e) {
							ctx.incrementErrors("Batch tag scan failed for " + kindEntry.getKey(), e.getMessage());
						}
						ctx.addTagged(kindFiles.size());
					}
				}
			}));
		}

		executor.shutdown();
		awaitAll(futures, ctx);
	}

	/**
	 * Phase 2 (synchronous) — processes all regular files from a single leaf directory
	 * as one batch: fetches existing filenames with one query, builds new
	 * {@link MediaFile} objects for unseen files, and persists them all with one
	 * {@code saveAll()} call so that N files become 1 SELECT + 1 transaction instead of
	 * N individual SELECTs + N individual transactions.
	 *
	 * @param leafDir the directory being scanned
	 * @param ctx     mutable scan state updated as files are processed
	 */
	private void scanDirectoryFiles(Path leafDir, ScanContext ctx) {
		String relativePath = ctx.root.relativize(leafDir).toString();

		// List the media files present on disk in this directory, keyed by filename, matched by
		// extension so the listing needs no per-file stat (Phase 1 already stat'd them).
		Map<String, Path> mediaByName = new LinkedHashMap<>();
		try (DirectoryStream<Path> entries = Files.newDirectoryStream(leafDir)) {
			for (Path entry : entries) {
				if (resolveKind(entry).isPresent()) {
					mediaByName.put(entry.getFileName().toString(), entry);
				}
			}
		} catch (IOException e) {
			ctx.incrementErrors("Failed to list directory: " + leafDir, e.getMessage());
			return;
		}
		if (mediaByName.isEmpty()) return;

		Set<String> existing = existingMediaFilenames(
				ctx.libraryFolder, relativePath, new ArrayList<>(mediaByName.keySet()));

		List<Path> newPaths = new ArrayList<>();
		List<MediaFile> toSave = new ArrayList<>();

		for (Map.Entry<String, Path> media : mediaByName.entrySet()) {
			if (existing.contains(media.getKey())) {
				ctx.incrementSkipped();
				continue;
			}
			MediaKind kind = resolveKind(media.getValue()).orElseThrow();
			newPaths.add(media.getValue());
			toSave.add(new MediaFile(ctx.libraryFolder, relativePath, media.getKey(), null, kind));
		}

		if (toSave.isEmpty()) return;

		try {
			List<MediaFile> saved = mediaFileRepository.saveAll(toSave);
			for (int i = 0; i < saved.size(); i++) {
				ctx.savedEntries.add(new FileScanEntry(saved.get(i), newPaths.get(i)));
			}
			ctx.addAdded(saved.size());
		} catch (Exception e) {
			ctx.incrementErrors("Batch save failed for: " + relativePath, e.getMessage());
		}
	}

	/**
	 * Phase 2 (background) — processes all regular files from a single leaf directory
	 * as one batch: fetches existing filenames with one projection query, then inserts
	 * new rows via JDBC so that no entities are loaded into the JPA persistence context.
	 * Phase 3 re-queries the DB for untagged records, so no entry tracking is needed here.
	 *
	 * New rows are flushed to the database in batches of {@link #INSERT_FLUSH_SIZE} rather than one
	 * statement for the whole directory, so the Added counter advances steadily and a single huge
	 * folder never holds the insert lock long enough to stall the other worker threads.
	 *
	 * @param leafDir       the directory being scanned
	 * @param libraryFolder the library folder this directory belongs to
	 * @param root          the library folder root path, used to compute the relative path
	 * @param state         job state for counter updates and error logging
	 */
	private void saveDirectoryFiles(Path leafDir, LibraryFolder libraryFolder, Path root, ScanJobState state) {
		String relativePath = root.relativize(leafDir).toString();

		Map<String, MediaKind> mediaByName = listMediaFilenames(leafDir, state);
		if (mediaByName.isEmpty()) return;

		Set<String> existing = existingMediaFilenames(
				libraryFolder, relativePath, new ArrayList<>(mediaByName.keySet()));

		List<Object[]> batch = new ArrayList<>(INSERT_FLUSH_SIZE);
		int skipped = 0;

		for (Map.Entry<String, MediaKind> media : mediaByName.entrySet()) {
			if (existing.contains(media.getKey())) {
				skipped++;
				continue;
			}
			batch.add(new Object[]{libraryFolder.getId(), relativePath, media.getKey(), null, media.getValue().name()});
			if (batch.size() >= INSERT_FLUSH_SIZE) {
				flushInsertBatch(batch, relativePath, state);
				batch.clear();
			}
		}

		if (!batch.isEmpty()) flushInsertBatch(batch, relativePath, state);
		if (skipped > 0) state.addSkipped(skipped);
	}

	/**
	 * Inserts one batch of media-file rows under the {@link #insertLock} and reports the count.
	 * The lock is held only for the duration of this batch so that concurrent workers can
	 * interleave their own batches between flushes.
	 *
	 * @param rows         the rows to insert; not modified
	 * @param relativePath the directory's relative path, for error messages
	 * @param state        job state for counter updates and error logging
	 */
	private void flushInsertBatch(List<Object[]> rows, String relativePath, ScanJobState state) {
		try {
			synchronized (insertLock) {
				jdbcTemplate.batchUpdate(
						"INSERT INTO media_file (library_folder_id, path, filename, hash, kind) VALUES (?, ?, ?, ?, ?)",
						rows
				);
			}
			state.addAdded(rows.size());
		} catch (Exception e) {
			state.addErrors(rows.size());
			state.addErrorLog("Batch save failed for: " + relativePath + " — " + e.getMessage());
		}
	}

	/**
	 * Lists the media files directly in {@code leafDir}, keyed by filename and mapped to their
	 * {@link MediaKind}. Entries are matched by file extension via {@link #resolveKind}, so this
	 * performs a single directory read with no per-file {@code stat}; the directory's files were
	 * already stat'd once during the Phase 1 walk.
	 *
	 * @param leafDir the directory to list
	 * @param state   job state for error logging if the directory cannot be read
	 * @return a map of media filename to kind, preserving directory order; empty on read failure
	 */
	private Map<String, MediaKind> listMediaFilenames(Path leafDir, ScanJobState state) {
		Map<String, MediaKind> mediaByName = new LinkedHashMap<>();
		try (DirectoryStream<Path> entries = Files.newDirectoryStream(leafDir)) {
			for (Path entry : entries) {
				resolveKind(entry).ifPresent(kind -> mediaByName.put(entry.getFileName().toString(), kind));
			}
		} catch (IOException e) {
			state.incrementErrors();
			state.addErrorLog("Failed to list directory: " + leafDir + " — " + e.getMessage());
		}
		return mediaByName;
	}

	/**
	 * Resolves the {@link MediaKind} for a file based on its extension.
	 *
	 * @param path the file path to inspect
	 * @return an {@link Optional} containing the matched {@link MediaKind},
	 *         or {@link Optional#empty()} if the extension is not recognised
	 */
	private Optional<MediaKind> resolveKind(Path path) {
		String name = path.getFileName().toString().toLowerCase();
		int dot = name.lastIndexOf('.');
		if (dot < 0) return Optional.empty();
		String ext = name.substring(dot + 1);
		if (IMAGE_EXTENSIONS.contains(ext)) return Optional.of(MediaKind.IMAGE);
		if (VIDEO_EXTENSIONS.contains(ext)) return Optional.of(MediaKind.VIDEO);
		if (AUDIO_EXTENSIONS.contains(ext)) return Optional.of(MediaKind.AUDIO);
		return Optional.empty();
	}

	/**
	 * Returns which of the given candidate filenames already exist in the database for the given
	 * directory, using an index-driven lookup keyed by the candidate names so the cost is
	 * proportional to the directory size rather than to the size of the whole library.
	 *
	 * The candidates are queried in bounded chunks so that a directory holding a very large number
	 * of files does not produce an oversized {@code IN} list.
	 *
	 * @param libraryFolder the library folder the directory belongs to
	 * @param relativePath  the directory path relative to the library folder root
	 * @param candidates    the media filenames discovered on disk in that directory
	 * @return the subset of {@code candidates} already present in the database for that directory
	 */
	private Set<String> existingMediaFilenames(LibraryFolder libraryFolder, String relativePath,
											   List<String> candidates) {
		if (candidates.isEmpty()) return Set.of();
		Set<String> existing = new HashSet<>();
		for (int i = 0; i < candidates.size(); i += EXISTENCE_QUERY_CHUNK_SIZE) {
			List<String> chunk = candidates.subList(i, Math.min(i + EXISTENCE_QUERY_CHUNK_SIZE, candidates.size()));
			existing.addAll(mediaFileRepository.findExistingFilenames(libraryFolder, relativePath, chunk));
		}
		return existing;
	}

	/**
	 * Waits for all futures to complete, recording interrupt/execution errors on the state.
	 *
	 * @param futures futures to await
	 * @param state   job state for error counting
	 */
	private void awaitAll(List<Future<?>> futures, ScanJobState state) {
		for (Future<?> future : futures) {
			try {
				future.get();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				state.incrementErrors();
			} catch (ExecutionException e) {
				state.incrementErrors();
			}
		}
	}

	/**
	 * Waits for all futures to complete, recording interrupt/execution errors on the context.
	 *
	 * @param futures futures to await
	 * @param ctx     scan context for error counting
	 */
	private void awaitAll(List<Future<?>> futures, ScanContext ctx) {
		for (Future<?> future : futures) {
			try {
				future.get();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				ctx.incrementErrors();
			} catch (ExecutionException e) {
				ctx.incrementErrors();
			}
		}
	}

	/**
	 * Holds all mutable state for a single synchronous scan run.
	 * A new instance is created per {@link #scanLibraryFolder} call.
	 * When a {@link ScanJobState} is supplied, every counter increment is mirrored into
	 * it so that background scans expose live progress over the REST API.
	 */
	private static class ScanContext {

		/** The library folder being scanned. */
		final LibraryFolder libraryFolder;

		/** The root directory path of this scan, derived from {@code libraryFolder}. */
		final Path root;

		/** Optional job state to mirror every counter update into for live progress reporting. */
		private final ScanJobState jobState;

		/** Count of media files newly added to the database. */
		final AtomicInteger added = new AtomicInteger(0);

		/** Count of files skipped because they are already indexed. */
		final AtomicInteger skipped = new AtomicInteger(0);

		/** Count of files or folders that caused an error during processing. */
		final AtomicInteger errors = new AtomicInteger(0);

		/** Count of folders found to have changes and fully scanned. */
		final AtomicInteger foldersScanned = new AtomicInteger(0);

		/** Count of folders skipped because they are unchanged since the last scan. */
		final AtomicInteger foldersSkipped = new AtomicInteger(0);

		/** Accumulated entries for tag-scanning in Phase 3; must be thread-safe. */
		final List<FileScanEntry> savedEntries = Collections.synchronizedList(new ArrayList<>());

		/**
		 * @param libraryFolder the library folder being scanned
		 * @param jobState      optional job state to mirror increments into; may be {@code null}
		 */
		ScanContext(LibraryFolder libraryFolder, ScanJobState jobState) {
			this.libraryFolder = libraryFolder;
			this.root = Paths.get(libraryFolder.getPath());
			this.jobState = jobState;
		}

		void addAdded(int n) {
			added.addAndGet(n);
			if (jobState != null) jobState.addAdded(n);
		}

		void incrementSkipped() {
			skipped.incrementAndGet();
			if (jobState != null) jobState.incrementSkipped();
		}

		void addSkipped(int n) {
			skipped.addAndGet(n);
			if (jobState != null) jobState.addSkipped(n);
		}

		void incrementErrors() {
			errors.incrementAndGet();
			if (jobState != null) jobState.incrementErrors();
		}

		void incrementErrors(String path, String errorMsg) {
			errors.incrementAndGet();
			if (jobState != null) {
				jobState.incrementErrors();
				jobState.addErrorLog(path + " — " + errorMsg);
			}
		}

		void incrementFoldersScanned() {
			foldersScanned.incrementAndGet();
			if (jobState != null) jobState.incrementFoldersScanned();
		}

		void incrementFoldersSkipped() {
			foldersSkipped.incrementAndGet();
			if (jobState != null) jobState.incrementFoldersSkipped();
		}

		void addTagged(int n) {
			if (jobState != null) jobState.addTagged(n);
		}
	}

	/**
	 * Result of a single Phase 1 directory walk: the leaf directories discovered (each with the
	 * file counts captured during the walk) and the total number of media files (by extension)
	 * found beneath the walked root.
	 *
	 * @param leafDirs   directories that contain at least one direct regular file
	 * @param mediaCount total count of files whose extension maps to a known {@link MediaKind}
	 */
	private record CollectResult(List<LeafDirInfo> leafDirs, int mediaCount) {}

	/**
	 * A leaf directory together with the per-directory tallies captured during the Phase 1 walk.
	 * Carrying these forward lets Phase 2 run its change detection (and skip-accounting) without a
	 * second pass over the directory's files, since the walk already stat'd every entry.
	 *
	 * @param dir        the leaf directory
	 * @param fileCount  number of direct regular files in the directory
	 * @param totalBytes total byte size of the direct regular files (for the change-detection hash)
	 * @param mediaCount number of direct files whose extension maps to a known {@link MediaKind}
	 */
	private record LeafDirInfo(Path dir, int fileCount, long totalBytes, int mediaCount) {}

	/**
	 * Fork/join task that walks a single directory during Phase 1: it lists the directory's direct
	 * entries, records the directory as a leaf if it holds at least one regular file, tallies media
	 * files by extension, and forks a child task for each non-skipped subdirectory. Sharing the
	 * collecting list and counter across all tasks lets the whole tree be walked in parallel.
	 */
	private final class DirectoryWalkTask extends RecursiveAction {

		/** The directory this task is responsible for walking. */
		private final Path dir;

		/** Shared, thread-safe sink for leaf directories (with their per-directory tallies). */
		private final List<LeafDirInfo> leafDirs;

		/** Shared counter of media files (by extension) discovered across the whole walk. */
		private final AtomicInteger mediaCount;

		/** Job state polled for cancellation; null for synchronous scans that cannot be cancelled. */
		private final ScanJobState state;

		/**
		 * @param dir        the directory to walk
		 * @param leafDirs   shared sink for discovered leaf directories
		 * @param mediaCount shared media-file counter
		 * @param state      job state polled for cancellation, or {@code null} when not cancellable
		 */
		DirectoryWalkTask(Path dir, List<LeafDirInfo> leafDirs, AtomicInteger mediaCount, ScanJobState state) {
			this.dir = dir;
			this.leafDirs = leafDirs;
			this.mediaCount = mediaCount;
			this.state = state;
		}

		@Override
		protected void compute() {
			if (state != null && state.isCancelRequested()) {
				return;
			}

			int fileCount = 0;
			long totalBytes = 0L;
			int dirMediaCount = 0;
			List<DirectoryWalkTask> subtasks = new ArrayList<>();

			try (DirectoryStream<Path> entries = Files.newDirectoryStream(dir)) {
				for (Path entry : entries) {
					BasicFileAttributes attrs;
					try {
						attrs = Files.readAttributes(entry, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
					} catch (IOException e) {
						continue;
					}
					if (attrs.isDirectory()) {
						String name = entry.getFileName() != null ? entry.getFileName().toString() : "";
						if (name.startsWith("#") || name.startsWith(".")) {
							continue;
						}
						subtasks.add(new DirectoryWalkTask(entry, leafDirs, mediaCount, state));
					} else if (attrs.isRegularFile()) {
						fileCount++;
						totalBytes += attrs.size();
						if (resolveKind(entry).isPresent()) {
							dirMediaCount++;
						}
					}
				}
			} catch (IOException e) {
				// Unreadable directory: skip it (and therefore its subtree) without failing the walk.
				return;
			}

			if (fileCount > 0) {
				leafDirs.add(new LeafDirInfo(dir, fileCount, totalBytes, dirMediaCount));
				mediaCount.addAndGet(dirMediaCount);
			}
			invokeAll(subtasks);
		}
	}

	/**
	 * Associates a leaf directory with the library folder and root path it belongs to, plus the
	 * per-directory tallies captured during the Phase 1 walk.
	 * Used to carry the context Phase 2 and Phase 3 need without re-stating the directory.
	 *
	 * @param leafDir       the leaf directory containing media files
	 * @param root          the library folder root path
	 * @param libraryFolder the library folder entity this leaf directory belongs to
	 * @param fileCount     number of direct regular files in the directory
	 * @param totalBytes    total byte size of the direct regular files (for the change-detection hash)
	 * @param mediaCount    number of direct files whose extension maps to a known {@link MediaKind}
	 */
	private record LeafDirEntry(Path leafDir, Path root, LibraryFolder libraryFolder,
								int fileCount, long totalBytes, int mediaCount) {}

	/**
	 * Intermediate container used during Phase 2 to associate a persisted
	 * {@link MediaFile} with its original {@link Path} on disk.
	 * The library folder is accessible via {@link MediaFile#getLibraryFolder()}.
	 *
	 * @param mediaFile the saved media file entity
	 * @param filePath  the file's location on disk
	 */
	private record FileScanEntry(MediaFile mediaFile, Path filePath) {}
}
