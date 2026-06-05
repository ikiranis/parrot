package eu.apps4net.parrotApp.services;

import org.springframework.stereotype.Service;

import eu.apps4net.parrotApp.models.LibraryFolder;
import eu.apps4net.parrotApp.models.MediaFile;
import eu.apps4net.parrotApp.models.MediaKind;
import eu.apps4net.parrotApp.models.ScanJobState;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

/**
 * Service that recursively scans a server-side directory for media files and
 * persists them in three phases:
 *
 * 1. Folder scan – collects every leaf directory (a directory containing no
 *    subdirectories) and delegates to {@link FolderService#checkAndSaveFolder} to
 *    decide whether its files need re-scanning.  Unchanged directories are skipped.
 * 2. File scan – for each changed leaf directory, detects media files by extension,
 *    determines their {@link MediaKind}, and saves a {@link MediaFile} record for each
 *    new file.  Already-indexed files are skipped.
 * 3. Tag scan – dispatches each saved {@link MediaFile} to the appropriate
 *    {@link MediaTagScanner} implementation based on its {@link MediaKind}.
 *
 * When called with a {@link ScanJobState}, each counter increment is mirrored into
 * the job state so that background scans expose live progress over the REST API.
 */
@Service
public class MediaScanService {

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
	 * @param scannerList           all registered {@link MediaTagScanner} implementations
	 */
	public MediaScanService(MediaFileRepository mediaFileRepository,
							FolderService folderService,
							SettingService settingService,
							LibraryFolderService libraryFolderService,
							List<MediaTagScanner> scannerList) {
		this.mediaFileRepository = mediaFileRepository;
		this.folderService = folderService;
		this.settingService = settingService;
		this.libraryFolderService = libraryFolderService;
		this.tagScanners = new EnumMap<>(MediaKind.class);
		for (MediaTagScanner scanner : scannerList) {
			tagScanners.put(scanner.getSupportedKind(), scanner);
		}
	}

	/**
	 * Scans the given folder recursively for media files and saves them to the database.
	 *
	 * Phase 1 collects all leaf directories (directories that contain no subdirectories)
	 * under {@code folderPath} and asks {@link FolderService#checkAndSaveFolder} whether
	 * each one has changed.  Unchanged directories are skipped entirely.
	 * Phase 2 scans the direct files of every changed leaf directory and persists a
	 * {@link MediaFile} for each new file whose extension maps to a known {@link MediaKind}.
	 * Phase 3 iterates over the saved files and invokes the matching
	 * {@link MediaTagScanner} (if one is registered for that kind).
	 *
	 * @param folderPath absolute path to the root folder to scan
	 * @return {@link ScanResult} with counts of added, skipped, and errored files
	 */
	public ScanResult scanFolder(String folderPath) {
		return scanFolder(folderPath, null);
	}

	/**
	 * Scans all configured {@link LibraryFolder} paths and aggregates the results
	 * into a single {@link ScanResult}.
	 * Each library folder is scanned independently via {@link #scanFolder(String)}.
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
			ScanResult result = scanFolder(libraryFolder.getPath());
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
	 * Marks the job state as completed or failed when the scan finishes.
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
			for (LibraryFolder libraryFolder : libraryFolders) {
				scanFolder(libraryFolder.getPath(), state);
			}
			state.complete("Scan complete. Added: " + state.getAdded() +
					", Skipped: " + state.getSkipped() +
					", Errors: " + state.getErrors() +
					", Folders scanned: " + state.getFoldersScanned() +
					", Folders skipped: " + state.getFoldersSkipped());
		} catch (Exception e) {
			state.fail("Scan failed: " + e.getMessage());
		}
	}

	/**
	 * Scans the given folder recursively, mirroring each counter increment into
	 * {@code jobState} when provided so that background scans expose live progress.
	 *
	 * @param folderPath absolute path to the root folder to scan
	 * @param jobState   job state to update in real time, or {@code null} for synchronous scans
	 * @return {@link ScanResult} with counts of added, skipped, and errored files
	 */
	private ScanResult scanFolder(String folderPath, ScanJobState jobState) {
		Path root = Paths.get(folderPath);

		if (!Files.exists(root)) {
			return new ScanResult(0, 0, 0, 0, 0, "Folder does not exist: " + folderPath);
		}

		if (!Files.isDirectory(root)) {
			return new ScanResult(0, 0, 0, 0, 0, "Path is not a directory: " + folderPath);
		}

		ScanContext ctx = new ScanContext(root, jobState);

		List<Path> leafDirs;
		try {
			leafDirs = collectLeafDirs(root);
		} catch (IOException e) {
			return new ScanResult(0, 0, 0, 0, 0, "Error walking directory: " + e.getMessage());
		}

		scanLeafDirectories(leafDirs, ctx);
		runTagScanners(ctx);

		return new ScanResult(ctx.added.get(), ctx.skipped.get(), ctx.errors.get(),
				ctx.foldersScanned.get(), ctx.foldersSkipped.get(),
				"Scan complete. Added: " + ctx.added.get() +
				", Skipped: " + ctx.skipped.get() +
				", Errors: " + ctx.errors.get() +
				", Folders scanned: " + ctx.foldersScanned.get() +
				", Folders skipped: " + ctx.foldersSkipped.get());
	}

	/**
	 * Phase 1 — walks {@code root} recursively and collects every directory that
	 * contains at least one direct regular file.
	 *
	 * Directories whose names start with {@code #} (e.g. {@code #recycle}, {@code #snapshot})
	 * are skipped entirely, as are any directories that cannot be accessed.
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
	 * Phase 2 — partitions {@code leafDirs} into chunks and processes each chunk in a
	 * dedicated thread, using up to {@code maxThreads} threads as configured in settings.
	 * For each directory, delegates to {@link FolderService#checkAndSaveFolder} to detect
	 * changes; changed directories have their direct regular files scanned and saved.
	 *
	 * @param leafDirs directories to process
	 * @param ctx      mutable scan state shared across all threads
	 */
	private void scanLeafDirectories(List<Path> leafDirs, ScanContext ctx) {
		int total = leafDirs.size();
		if (total == 0) {
			return;
		}

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
						hasChanges = folderService.checkAndSaveFolder(leafDir);
					} catch (IOException e) {
						ctx.incrementErrors();
						continue;
					}
					if (!hasChanges) {
						ctx.incrementFoldersSkipped();
						continue;
					}
					ctx.incrementFoldersScanned();
					try (Stream<Path> files = Files.list(leafDir)) {
						files.filter(Files::isRegularFile)
								.forEach(filePath -> scanMediaFile(filePath, ctx));
					} catch (IOException e) {
						ctx.incrementErrors();
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
	 * Phase 3 — partitions saved entries into chunks and processes each chunk in a
	 * dedicated thread, using up to {@code maxThreads} threads as configured in settings.
	 * For each entry, looks up the registered {@link MediaTagScanner} for its {@link MediaKind}
	 * and invokes tag scanning.
	 *
	 * @param ctx mutable scan state holding the saved entries, scan root, and error counter
	 */
	private void runTagScanners(ScanContext ctx) {
		int total = ctx.savedEntries.size();
		if (total == 0) {
			return;
		}

		int threads = Math.min(settingService.getMaxThreads(), total);
		int chunkSize = (int) Math.ceil((double) total / threads);

		ExecutorService executor = Executors.newFixedThreadPool(threads);
		List<Future<?>> futures = new ArrayList<>();

		for (int i = 0; i < total; i += chunkSize) {
			List<FileScanEntry> chunk = ctx.savedEntries.subList(i, Math.min(i + chunkSize, total));
			futures.add(executor.submit(() -> {
				for (FileScanEntry entry : chunk) {
					MediaTagScanner scanner = tagScanners.get(entry.mediaFile().getKind());
					if (scanner != null) {
						try {
							scanner.scanTags(entry.mediaFile(), entry.filePath(), ctx.root);
						} catch (Exception e) {
							ctx.incrementErrors();
						}
					}
					ctx.incrementTagged();
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
	 * Returns {@code true} when {@code dir} contains at least one direct regular file,
	 * meaning its files should be scanned regardless of whether it also has subdirectories.
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
	 * Processes a single file during Phase 2: resolves its {@link MediaKind} by
	 * extension, skips it if already indexed, otherwise saves a {@link MediaFile}
	 * record and adds the entry to the context for Phase 3 processing.
	 *
	 * @param filePath the path to the file
	 * @param ctx      mutable scan state updated as files are processed
	 */
	private void scanMediaFile(Path filePath, ScanContext ctx) {
		Optional<MediaKind> kindOpt = resolveKind(filePath);
		if (kindOpt.isEmpty()) {
			return; // not a recognised media type
		}

		String filename = filePath.getFileName().toString();
		String parentPath = filePath.getParent() != null
				? filePath.getParent().toString()
				: "";

		if (mediaFileRepository.findByPathAndFilename(parentPath, filename).isPresent()) {
			ctx.incrementSkipped();
			return;
		}

		try {
			MediaFile mediaFile = new MediaFile(parentPath, filename, null, kindOpt.get());
			mediaFileRepository.save(mediaFile);
			ctx.savedEntries.add(new FileScanEntry(mediaFile, filePath));
			ctx.incrementAdded();
		} catch (Exception e) {
			ctx.incrementErrors();
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
		if (dot < 0) {
			return Optional.empty();
		}
		String ext = name.substring(dot + 1);
		if (IMAGE_EXTENSIONS.contains(ext)) return Optional.of(MediaKind.IMAGE);
		if (VIDEO_EXTENSIONS.contains(ext)) return Optional.of(MediaKind.VIDEO);
		if (AUDIO_EXTENSIONS.contains(ext)) return Optional.of(MediaKind.AUDIO);
		return Optional.empty();
	}

	/**
	 * Holds all mutable state for a single scan run.
	 * A new instance is created per {@link #scanFolder(String, ScanJobState)} call, keeping
	 * concurrent scans isolated from one another.
	 * When a {@link ScanJobState} is supplied, every counter increment is mirrored into
	 * it so that background scans expose live progress over the REST API.
	 */
	private static class ScanContext {

		/** The root directory of this scan, used for relative path resolution in tag scanners. */
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
		 * @param root     the root directory of the scan
		 * @param jobState optional job state to mirror increments into; may be {@code null}
		 */
		ScanContext(Path root, ScanJobState jobState) {
			this.root = root;
			this.jobState = jobState;
		}

		void incrementAdded() {
			added.incrementAndGet();
			if (jobState != null) jobState.incrementAdded();
		}

		void incrementSkipped() {
			skipped.incrementAndGet();
			if (jobState != null) jobState.incrementSkipped();
		}

		void incrementErrors() {
			errors.incrementAndGet();
			if (jobState != null) jobState.incrementErrors();
		}

		void incrementFoldersScanned() {
			foldersScanned.incrementAndGet();
			if (jobState != null) jobState.incrementFoldersScanned();
		}

		void incrementFoldersSkipped() {
			foldersSkipped.incrementAndGet();
			if (jobState != null) jobState.incrementFoldersSkipped();
		}

		void incrementTagged() {
			if (jobState != null) jobState.incrementTagged();
		}
	}

	/**
	 * Intermediate container used during Phase 2 to associate a persisted
	 * {@link MediaFile} with its original {@link Path} on disk.
	 *
	 * @param mediaFile the saved media file entity
	 * @param filePath  the file's location on disk
	 */
	private record FileScanEntry(MediaFile mediaFile, Path filePath) {}
}
