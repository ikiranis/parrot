package eu.apps4net.parrotApp.services;

import org.springframework.stereotype.Service;

import eu.apps4net.parrotApp.models.MediaFile;
import eu.apps4net.parrotApp.models.MediaKind;
import eu.apps4net.parrotApp.models.ScanResult;
import eu.apps4net.parrotApp.repositories.MediaFileRepository;
import eu.apps4net.parrotApp.services.tagscanner.MediaTagScanner;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
	 * @param mediaFileRepository repository for media file persistence
	 * @param folderService       service for folder change detection and persistence
	 * @param settingService      service for reading application settings
	 * @param scannerList         all registered {@link MediaTagScanner} implementations
	 */
	public MediaScanService(MediaFileRepository mediaFileRepository,
							FolderService folderService,
							SettingService settingService,
							List<MediaTagScanner> scannerList) {
		this.mediaFileRepository = mediaFileRepository;
		this.folderService = folderService;
		this.settingService = settingService;
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
		Path root = Paths.get(folderPath);

		if (!Files.exists(root)) {
			return new ScanResult(0, 0, 0, 0, 0, "Folder does not exist: " + folderPath);
		}

		if (!Files.isDirectory(root)) {
			return new ScanResult(0, 0, 0, 0, 0, "Path is not a directory: " + folderPath);
		}

		AtomicInteger added = new AtomicInteger(0);
		AtomicInteger skipped = new AtomicInteger(0);
		AtomicInteger errors = new AtomicInteger(0);
		AtomicInteger foldersScanned = new AtomicInteger(0);
		AtomicInteger foldersSkipped = new AtomicInteger(0);
		List<FileScanEntry> savedEntries = Collections.synchronizedList(new ArrayList<>());

		List<Path> leafDirs;
		try {
			leafDirs = collectLeafDirs(root);
		} catch (IOException e) {
			return new ScanResult(0, 0, 0, 0, 0, "Error walking directory: " + e.getMessage());
		}

		scanLeafDirectories(leafDirs, savedEntries, added, skipped, errors, foldersScanned, foldersSkipped);
		runTagScanners(savedEntries, root, errors);

		return new ScanResult(added.get(), skipped.get(), errors.get(),
				foldersScanned.get(), foldersSkipped.get(),
				"Scan complete. Added: " + added.get() +
				", Skipped: " + skipped.get() +
				", Errors: " + errors.get() +
				", Folders scanned: " + foldersScanned.get() +
				", Folders skipped: " + foldersSkipped.get());
	}

	/**
	 * Phase 1 — walks {@code root} recursively and collects every directory that
	 * contains at least one direct regular file.
	 *
	 * @param root the root directory to walk
	 * @return list of directories that contain at least one direct regular file
	 * @throws IOException if the directory tree cannot be walked
	 */
	private List<Path> collectLeafDirs(Path root) throws IOException {
		List<Path> leafDirs = new ArrayList<>();
		try (Stream<Path> stream = Files.walk(root)) {
			stream.filter(Files::isDirectory)
					.filter(this::containsFiles)
					.forEach(leafDirs::add);
		}
		return leafDirs;
	}

	/**
	 * Phase 2 — partitions {@code leafDirs} into chunks and processes each chunk in a
	 * dedicated thread, using up to {@code maxThreads} threads as configured in settings.
	 * For each directory, delegates to {@link FolderService#checkAndSaveFolder} to detect
	 * changes; changed directories have their direct regular files scanned and saved.
	 *
	 * @param leafDirs       directories to process
	 * @param savedEntries   thread-safe accumulator for successfully saved file entries
	 * @param added          counter incremented when a {@link MediaFile} is persisted
	 * @param skipped        counter incremented when a file is already indexed
	 * @param errors         counter incremented on any exception
	 * @param foldersScanned counter incremented when a folder has changes and is scanned
	 * @param foldersSkipped counter incremented when a folder is unchanged
	 */
	private void scanLeafDirectories(List<Path> leafDirs, List<FileScanEntry> savedEntries,
			AtomicInteger added, AtomicInteger skipped, AtomicInteger errors,
			AtomicInteger foldersScanned, AtomicInteger foldersSkipped) {
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
						errors.incrementAndGet();
						continue;
					}
					if (!hasChanges) {
						foldersSkipped.incrementAndGet();
						continue;
					}
					foldersScanned.incrementAndGet();
					try (Stream<Path> files = Files.list(leafDir)) {
						files.filter(Files::isRegularFile)
								.forEach(filePath -> scanMediaFile(filePath, savedEntries, added, skipped, errors));
					} catch (IOException e) {
						errors.incrementAndGet();
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
				errors.incrementAndGet();
			} catch (ExecutionException e) {
				errors.incrementAndGet();
			}
		}
	}

	/**
	 * Phase 3 — for each saved entry, looks up the registered {@link MediaTagScanner}
	 * for its {@link MediaKind} and invokes tag scanning.
	 *
	 * @param savedEntries entries collected during phase 2
	 * @param root         the scan root passed through to the scanner for relative path resolution
	 * @param errors       counter incremented when tag scanning throws
	 */
	private void runTagScanners(List<FileScanEntry> savedEntries, Path root, AtomicInteger errors) {
		for (FileScanEntry entry : savedEntries) {
			MediaTagScanner scanner = tagScanners.get(entry.mediaFile().getKind());
			if (scanner != null) {
				try {
					scanner.scanTags(entry.mediaFile(), entry.filePath(), root);
				} catch (Exception e) {
					errors.incrementAndGet();
				}
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
	 * Processes a single file during Phase 1: resolves its {@link MediaKind} by
	 * extension, skips it if already indexed, otherwise saves a {@link MediaFile}
	 * record and adds the entry to {@code savedEntries} for Phase 2 processing.
	 *
	 * @param filePath     the path to the file
	 * @param savedEntries accumulator for successfully saved entries
	 * @param added        counter incremented when a {@link MediaFile} is persisted
	 * @param skipped      counter incremented when the file is already in the database
	 * @param errors       counter incremented when an exception occurs
	 */
	private void scanMediaFile(Path filePath, List<FileScanEntry> savedEntries,
							   AtomicInteger added, AtomicInteger skipped, AtomicInteger errors) {
		Optional<MediaKind> kindOpt = resolveKind(filePath);
		if (kindOpt.isEmpty()) {
			return; // not a recognised media type
		}

		String filename = filePath.getFileName().toString();
		String parentPath = filePath.getParent() != null
				? filePath.getParent().toString()
				: "";

		if (mediaFileRepository.findByPathAndFilename(parentPath, filename).isPresent()) {
			skipped.incrementAndGet();
			return;
		}

		try {
			MediaFile mediaFile = new MediaFile(parentPath, filename, null, kindOpt.get());
			mediaFileRepository.save(mediaFile);
			savedEntries.add(new FileScanEntry(mediaFile, filePath));
			added.incrementAndGet();
		} catch (Exception e) {
			errors.incrementAndGet();
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
	 * Intermediate container used during Phase 2 to associate a persisted
	 * {@link MediaFile} with its original {@link Path} on disk.
	 *
	 * @param mediaFile the saved media file entity
	 * @param filePath  the file's location on disk
	 */
	private record FileScanEntry(MediaFile mediaFile, Path filePath) {}
}
