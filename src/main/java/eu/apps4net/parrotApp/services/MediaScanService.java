package eu.apps4net.parrotApp.services;

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
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Service that recursively scans a server-side directory for media files and
 * persists them in three phases:
 *
 * 1. Folder scan (COLLECTING) – collects every leaf directory (a directory containing
 *    at least one direct file) across all configured library folders.
 * 2. File scan (SCANNING) – for each changed leaf directory, detects media files by
 *    extension, determines their {@link MediaKind}, and saves a {@link MediaFile} record
 *    for each new file.  Already-indexed files are skipped.
 * 3. Tag scan (TAGGING) – dispatches each saved {@link MediaFile} to the appropriate
 *    {@link MediaTagScanner} implementation based on its {@link MediaKind}.
 *
 * In background scans, phases 2 and 3 run concurrently: the tag scanner starts in a
 * separate thread alongside the file scanner and polls the database for newly added files.
 * If no untagged records are found while the file scan is still in progress, the tag
 * scanner sleeps for 3 minutes before retrying.
 *
 * When called with a {@link ScanJobState}, phase transitions and every counter increment
 * are mirrored into the job state so that background scans expose live progress and error
 * logs over the REST API.
 */
@Service
public class MediaScanService {

	/** Number of MediaFile records fetched from the DB per batch during Phase 3 (tag scanning). */
	private static final int TAG_SCAN_BATCH_SIZE = 1000;

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
	 * still in progress, the tag scanner sleeps for 3 minutes before retrying.
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
			// Record pre-scan DB count so the frontend can show context for re-scans
			state.setInitialFilesCount((int) mediaFileRepository.count());

			// Phase 1: collect all leaf directories across all library folders
			state.setPhase(ScanPhase.COLLECTING);
			List<LeafDirEntry> allLeafDirs = collectAllLeafDirs(libraryFolders, state);
			state.setTotalFolders(allLeafDirs.size());
			state.setTotalMediaFilesInLibrary(countAllMediaFiles(allLeafDirs));

			// Phases 2 and 3 run concurrently.
			// fileScanDone is set in a finally block so the tag thread always gets a termination signal.
			AtomicBoolean fileScanDone = new AtomicBoolean(false);
			ExecutorService phaseExecutor = Executors.newFixedThreadPool(2);

			Future<?> scanFuture = phaseExecutor.submit(() -> {
				try {
					state.setPhase(ScanPhase.SCANNING);
					scanAllLeafDirs(allLeafDirs, state);
					allLeafDirs.clear();
					System.gc();
				} finally {
					fileScanDone.set(true);
				}
			});

			Future<?> tagFuture = phaseExecutor.submit(() -> {
				runTagScannersParallel(state, fileScanDone);
				System.gc();
			});

			phaseExecutor.shutdown();
			awaitAll(List.of(scanFuture, tagFuture), state);

			state.complete("Scan complete. Added: " + state.getAdded() +
					", Skipped: " + state.getSkipped() +
					", Tagged: " + state.getTagged() +
					", Errors: " + state.getErrors() +
					", Folders scanned: " + state.getFoldersScanned() +
					", Folders skipped: " + state.getFoldersSkipped());
		} catch (Exception e) {
			state.fail("Scan failed: " + e.getMessage());
		}
	}

	/**
	 * Phase 1 (background) — walks every configured library folder and collects all leaf
	 * directories into a flat list paired with their library root for later use in tag scanning.
	 * Errors accessing individual library folders are logged to {@code state} and skipped.
	 *
	 * @param libraryFolders configured library folders to walk
	 * @param state          job state for error logging
	 * @return flat list of all leaf directory entries across all library folders
	 */
	private List<LeafDirEntry> collectAllLeafDirs(List<LibraryFolder> libraryFolders, ScanJobState state) {
		List<LeafDirEntry> allLeafDirs = new ArrayList<>();
		for (LibraryFolder lf : libraryFolders) {
			Path root = Paths.get(lf.getPath());
			if (!Files.exists(root) || !Files.isDirectory(root)) {
				state.incrementErrors();
				state.addErrorLog("Library folder not accessible: " + lf.getPath());
				continue;
			}
			try {
				for (Path leafDir : collectLeafDirs(root)) {
					allLeafDirs.add(new LeafDirEntry(leafDir, root, lf));
				}
			} catch (IOException e) {
				state.incrementErrors();
				state.addErrorLog("Failed to walk " + lf.getPath() + ": " + e.getMessage());
			}
		}
		return allLeafDirs;
	}

	/**
	 * Phase 2 (background) — partitions the leaf directory list into chunks and processes
	 * each chunk in a dedicated thread.  For each directory, delegates to
	 * {@link FolderService#checkAndSaveFolder} to detect changes; changed directories have
	 * their direct regular files scanned and saved as {@link MediaFile} records.
	 *
	 * @param leafDirs all leaf directory entries to process
	 * @param state    job state for live counter updates and error logging
	 */
	private void scanAllLeafDirs(List<LeafDirEntry> leafDirs, ScanJobState state) {
		int total = leafDirs.size();
		if (total == 0) return;

		int threads = Math.min(settingService.getMaxThreads(), total);
		int chunkSize = (int) Math.ceil((double) total / threads);

		ExecutorService executor = Executors.newFixedThreadPool(threads);
		List<Future<?>> futures = new ArrayList<>();

		for (int i = 0; i < total; i += chunkSize) {
			List<LeafDirEntry> chunk = leafDirs.subList(i, Math.min(i + chunkSize, total));
			futures.add(executor.submit(() -> {
				for (LeafDirEntry entry : chunk) {
					boolean hasChanges;
					try {
						hasChanges = folderService.checkAndSaveFolder(entry.leafDir(), entry.root(), entry.libraryFolder());
					} catch (IOException e) {
						state.incrementErrors();
						state.addErrorLog("Folder check failed: " + entry.leafDir() + " — " + e.getMessage());
						continue;
					}
					if (!hasChanges) {
						state.incrementFoldersSkipped();
						state.addSkipped(countMediaFilesInDir(entry.leafDir()));
						continue;
					}
					state.incrementFoldersScanned();
					try (Stream<Path> files = Files.list(entry.leafDir())) {
						List<Path> fileList = files.filter(Files::isRegularFile).collect(Collectors.toList());
						saveDirectoryFiles(fileList, entry.leafDir(), entry.libraryFolder(), entry.root(), state);
						String relativePath = entry.root().relativize(entry.leafDir()).toString();
						folderService.markFinished(entry.libraryFolder(), relativePath);
					} catch (IOException e) {
						state.incrementErrors();
						state.addErrorLog("Failed to list directory: " + entry.leafDir() + " — " + e.getMessage());
					}
				}
			}));
		}

		executor.shutdown();
		awaitAll(futures, state);
	}

	/**
	 * Phase 3 (parallel background) — polls the database for {@link MediaFile} records without
	 * tags and processes them while Phase 2 is still running.
	 *
	 * On each iteration the method counts all untagged records across every registered scanner kind.
	 * If records are found, it transitions the job state to TAGGING and drains them in batches.
	 * If no records are found and the file scan has not yet finished, the thread sleeps for
	 * 3 minutes before retrying. Once the file scan is complete and no untagged records remain,
	 * the method returns.
	 *
	 * The cumulative total passed to {@link ScanJobState#setTotalFiles} grows across iterations
	 * so that the progress percentage never regresses when a second pass finds additional files.
	 *
	 * @param state        job state for live counter updates, phase transitions, and error logging
	 * @param fileScanDone flag set to {@code true} by Phase 2 when all directories have been processed
	 */
	private void runTagScannersParallel(ScanJobState state, AtomicBoolean fileScanDone) {
		long cumulativeTotalToTag = 0;

		while (true) {
			long totalToTag = 0;
			for (MediaKind kind : tagScanners.keySet()) {
				totalToTag += mediaFileRepository.countByKindWithoutPhotoTag(kind);
			}

			if (totalToTag == 0) {
				if (fileScanDone.get()) {
					break;
				}
				try {
					Thread.sleep(3 * 60 * 1000L);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					return;
				}
				continue;
			}

			cumulativeTotalToTag += totalToTag;
			state.setTotalFiles((int) cumulativeTotalToTag);
			if (fileScanDone.get()) {
				state.setPhase(ScanPhase.TAGGING);
			}

			for (Map.Entry<MediaKind, MediaTagScanner> entry : tagScanners.entrySet()) {
				List<MediaFile> batch;
				do {
					batch = mediaFileRepository
							.findByKindWithoutPhotoTag(entry.getKey(), PageRequest.of(0, TAG_SCAN_BATCH_SIZE))
							.getContent();
					if (batch.isEmpty()) break;
					processTagBatch(batch, entry.getValue(), state);
				} while (true);
			}
		}
	}

	/**
	 * Partitions a single tag batch into chunks and processes each chunk in a dedicated thread,
	 * invoking the given {@link MediaTagScanner} for every file.
	 * The library root for each file is resolved directly from its {@link LibraryFolder} association.
	 *
	 * @param batch   MediaFile records to tag
	 * @param scanner the scanner to use for every file in this batch
	 * @param state   job state for live counter updates and error logging
	 */
	private void processTagBatch(List<MediaFile> batch, MediaTagScanner scanner, ScanJobState state) {
		int total = batch.size();
		int threads = Math.min(settingService.getMaxThreads(), total);
		int chunkSize = (int) Math.ceil((double) total / threads);

		ExecutorService executor = Executors.newFixedThreadPool(threads);
		List<Future<?>> futures = new ArrayList<>();

		for (int i = 0; i < total; i += chunkSize) {
			List<MediaFile> chunk = batch.subList(i, Math.min(i + chunkSize, total));
			futures.add(executor.submit(() -> {
				try {
					scanner.scanTagsBatch(chunk, mf -> Paths.get(mf.getLibraryFolder().getPath()));
				} catch (Exception e) {
					state.addErrors(chunk.size());
					state.addErrorLog("Batch tag scan failed: " + e.getMessage());
				}
				state.addTagged(chunk.size());
			}));
		}

		executor.shutdown();
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

		List<Path> leafDirs;
		try {
			leafDirs = collectLeafDirs(root);
		} catch (IOException e) {
			return new ScanResult(0, 0, 0, 0, 0, "Error walking directory: " + e.getMessage());
		}

		scanLeafDirectories(leafDirs, ctx);
		leafDirs.clear();
		runTagScanners(ctx);
		ctx.savedEntries.clear();
		System.gc();

		return new ScanResult(ctx.added.get(), ctx.skipped.get(), ctx.errors.get(),
				ctx.foldersScanned.get(), ctx.foldersSkipped.get(),
				"Scan complete. Added: " + ctx.added.get() +
				", Skipped: " + ctx.skipped.get() +
				", Errors: " + ctx.errors.get() +
				", Folders scanned: " + ctx.foldersScanned.get() +
				", Folders skipped: " + ctx.foldersSkipped.get());
	}

	/**
	 * Phase 1 (synchronous) — walks {@code root} recursively and collects every directory
	 * that contains at least one direct regular file.
	 *
	 * Directories whose names start with {@code #} (e.g. {@code #recycle}) are skipped
	 * entirely, as are any directories that cannot be accessed.
	 *
	 * @param root the root directory to walk
	 * @return list of directories that contain at least one direct regular file
	 * @throws IOException if the directory tree cannot be walked
	 */
	private List<Path> collectLeafDirs(Path root) throws IOException {
		List<Path> leafDirs = new ArrayList<>();
		Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
				String name = dir.getFileName() != null ? dir.getFileName().toString() : "";
				if (name.startsWith("#")) {
					return FileVisitResult.SKIP_SUBTREE;
				}
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitFileFailed(Path file, IOException exc) {
				return FileVisitResult.SKIP_SUBTREE;
			}

			@Override
			public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
				if (exc == null && containsFiles(dir)) {
					leafDirs.add(dir);
				}
				return FileVisitResult.CONTINUE;
			}
		});
		return leafDirs;
	}

	/**
	 * Phase 2 (synchronous) — partitions {@code leafDirs} into chunks and processes each
	 * chunk in a dedicated thread.  For each directory, delegates to
	 * {@link FolderService#checkAndSaveFolder}; changed directories have their direct
	 * regular files scanned and saved.
	 *
	 * @param leafDirs directories to process
	 * @param ctx      mutable scan state shared across all threads
	 */
	private void scanLeafDirectories(List<Path> leafDirs, ScanContext ctx) {
		int total = leafDirs.size();
		if (total == 0) return;

		int threads = Math.min(settingService.getMaxThreads(), total);
		int chunkSize = (int) Math.ceil((double) total / threads);

		ExecutorService executor = Executors.newFixedThreadPool(threads);
		List<Future<?>> futures = new ArrayList<>();

		for (int i = 0; i < total; i += chunkSize) {
			List<Path> chunk = leafDirs.subList(i, Math.min(i + chunkSize, total));
			futures.add(executor.submit(() -> {
				for (Path leafDir : chunk) {
					boolean hasChanges;
					try {
						hasChanges = folderService.checkAndSaveFolder(leafDir, ctx.root, ctx.libraryFolder);
					} catch (IOException e) {
						ctx.incrementErrors("Folder check failed: " + leafDir, e.getMessage());
						continue;
					}
					if (!hasChanges) {
						ctx.incrementFoldersSkipped();
						ctx.addSkipped(countMediaFilesInDir(leafDir));
						continue;
					}
					ctx.incrementFoldersScanned();
					try (Stream<Path> files = Files.list(leafDir)) {
						List<Path> fileList = files.filter(Files::isRegularFile).collect(Collectors.toList());
						scanDirectoryFiles(fileList, leafDir, ctx);
						String relativePath = ctx.root.relativize(leafDir).toString();
						folderService.markFinished(ctx.libraryFolder, relativePath);
					} catch (IOException e) {
						ctx.incrementErrors("Failed to list directory: " + leafDir, e.getMessage());
					}
				}
			}));
		}

		executor.shutdown();
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
	 * Phase 3 (synchronous) — partitions saved entries into chunks and processes each chunk
	 * in a dedicated thread.  For each entry, looks up the registered {@link MediaTagScanner}
	 * for its {@link MediaKind} and invokes tag scanning.
	 *
	 * @param ctx mutable scan state holding the saved entries, scan root, and error counter
	 */
	private void runTagScanners(ScanContext ctx) {
		int total = ctx.savedEntries.size();
		if (total == 0) return;

		int threads = Math.min(settingService.getMaxThreads(), total);
		int chunkSize = (int) Math.ceil((double) total / threads);

		ExecutorService executor = Executors.newFixedThreadPool(threads);
		List<Future<?>> futures = new ArrayList<>();

		for (int i = 0; i < total; i += chunkSize) {
			List<FileScanEntry> chunk = ctx.savedEntries.subList(i, Math.min(i + chunkSize, total));
			futures.add(executor.submit(() -> {
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
			}));
		}

		executor.shutdown();
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
	 * Returns {@code true} when {@code dir} contains at least one direct regular file.
	 *
	 * @param dir the directory to test
	 * @return {@code true} if the directory has at least one direct regular file
	 */
	private boolean containsFiles(Path dir) {
		try (Stream<Path> children = Files.list(dir)) {
			return children.anyMatch(Files::isRegularFile);
		} catch (IOException e) {
			return false;
		}
	}

	/**
	 * Phase 2 (synchronous) — processes all regular files from a single leaf directory
	 * as one batch: fetches existing filenames with one query, builds new
	 * {@link MediaFile} objects for unseen files, and persists them all with one
	 * {@code saveAll()} call so that N files become 1 SELECT + 1 transaction instead of
	 * N individual SELECTs + N individual transactions.
	 *
	 * @param files   regular files in the directory
	 * @param leafDir the directory being scanned
	 * @param ctx     mutable scan state updated as files are processed
	 */
	private void scanDirectoryFiles(List<Path> files, Path leafDir, ScanContext ctx) {
		String relativePath = ctx.root.relativize(leafDir).toString();

		Set<String> existing = new HashSet<>(
				mediaFileRepository.findFilenamesByLibraryFolderAndPath(ctx.libraryFolder, relativePath));

		List<Path> newPaths = new ArrayList<>();
		List<MediaFile> toSave = new ArrayList<>();

		for (Path filePath : files) {
			Optional<MediaKind> kindOpt = resolveKind(filePath);
			if (kindOpt.isEmpty()) continue;
			String filename = filePath.getFileName().toString();
			if (existing.contains(filename)) {
				ctx.incrementSkipped();
				continue;
			}
			newPaths.add(filePath);
			toSave.add(new MediaFile(ctx.libraryFolder, relativePath, filename, null, kindOpt.get()));
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
	 * @param files         regular files in the directory
	 * @param leafDir       the directory being scanned
	 * @param libraryFolder the library folder this directory belongs to
	 * @param root          the library folder root path, used to compute the relative path
	 * @param state         job state for counter updates and error logging
	 */
	private void saveDirectoryFiles(List<Path> files, Path leafDir, LibraryFolder libraryFolder,
									Path root, ScanJobState state) {
		String relativePath = root.relativize(leafDir).toString();

		Set<String> existing = new HashSet<>(
				mediaFileRepository.findFilenamesByLibraryFolderAndPath(libraryFolder, relativePath));

		List<Object[]> rows = new ArrayList<>();
		int skipped = 0;

		for (Path filePath : files) {
			Optional<MediaKind> kindOpt = resolveKind(filePath);
			if (kindOpt.isEmpty()) continue;
			String filename = filePath.getFileName().toString();
			if (existing.contains(filename)) {
				skipped++;
				continue;
			}
			rows.add(new Object[]{libraryFolder.getId(), relativePath, filename, null, kindOpt.get().name()});
		}

		if (skipped > 0) state.addSkipped(skipped);
		if (rows.isEmpty()) return;

		try {
			jdbcTemplate.batchUpdate(
					"INSERT INTO media_file (library_folder_id, path, filename, hash, kind) VALUES (?, ?, ?, ?, ?)",
					rows
			);
			state.addAdded(rows.size());
		} catch (Exception e) {
			state.addErrors(rows.size());
			state.addErrorLog("Batch save failed for: " + relativePath + " — " + e.getMessage());
		}
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
	 * Counts the media files in a single directory (non-recursive) by extension,
	 * without any database access. Used when a folder is skipped to report its files as skipped.
	 *
	 * @param dir the directory to inspect
	 * @return count of files whose extension maps to a known {@link MediaKind}
	 */
	private int countMediaFilesInDir(Path dir) {
		try (Stream<Path> files = Files.list(dir)) {
			return (int) files
					.filter(Files::isRegularFile)
					.filter(f -> resolveKind(f).isPresent())
					.count();
		} catch (IOException e) {
			return 0;
		}
	}

	/**
	 * Counts all media files across every leaf directory in parallel by extension,
	 * without any database access. Used after Phase 1 to set the total-files denominator.
	 *
	 * @param leafDirs all leaf directory entries discovered in Phase 1
	 * @return total count of files whose extension maps to a known {@link MediaKind}
	 */
	private int countAllMediaFiles(List<LeafDirEntry> leafDirs) {
		return leafDirs.parallelStream()
				.mapToInt(entry -> {
					try (Stream<Path> files = Files.list(entry.leafDir())) {
						return (int) files
								.filter(Files::isRegularFile)
								.filter(f -> resolveKind(f).isPresent())
								.count();
					} catch (IOException e) {
						return 0;
					}
				})
				.sum();
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
	 * Associates a leaf directory with the library folder and root path it belongs to.
	 * Used during Phase 1 to preserve the context needed by Phase 2 and Phase 3.
	 *
	 * @param leafDir       the leaf directory containing media files
	 * @param root          the library folder root path
	 * @param libraryFolder the library folder entity this leaf directory belongs to
	 */
	private record LeafDirEntry(Path leafDir, Path root, LibraryFolder libraryFolder) {}

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
