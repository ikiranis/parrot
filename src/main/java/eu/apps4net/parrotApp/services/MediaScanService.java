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
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

/**
 * Service that recursively scans a server-side directory for media files and
 * persists them in two phases:
 * <ol>
 *   <li><b>File scan</b> – detects media files by extension, determines their
 *       {@link MediaKind}, and saves a {@link MediaFile} record for each new file.</li>
 *   <li><b>Tag scan</b> – dispatches each saved {@link MediaFile} to the appropriate
 *       {@link MediaTagScanner} implementation based on its {@link MediaKind}.</li>
 * </ol>
 * Already-indexed files are skipped using a path + filename look-up.
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
	 * @param scannerList         all registered {@link MediaTagScanner} implementations
	 */
	public MediaScanService(MediaFileRepository mediaFileRepository,
							List<MediaTagScanner> scannerList) {
		this.mediaFileRepository = mediaFileRepository;
		this.tagScanners = new EnumMap<>(MediaKind.class);
		for (MediaTagScanner scanner : scannerList) {
			tagScanners.put(scanner.getSupportedKind(), scanner);
		}
	}

	/**
	 * Scans the given folder recursively for media files and saves them to the database.
	 *
	 * <p>Phase 1 walks the directory and persists a {@link MediaFile} for every new file
	 * whose extension maps to a known {@link MediaKind}.
	 * Phase 2 iterates over the saved files and invokes the matching
	 * {@link MediaTagScanner} (if one is registered for that kind).</p>
	 *
	 * @param folderPath absolute path to the folder to scan
	 * @return {@link ScanResult} with counts of added, skipped, and errored files
	 */
	public ScanResult scanFolder(String folderPath) {
		Path root = Paths.get(folderPath);

		if (!Files.exists(root)) {
			return new ScanResult(0, 0, 0, "Folder does not exist: " + folderPath);
		}

		if (!Files.isDirectory(root)) {
			return new ScanResult(0, 0, 0, "Path is not a directory: " + folderPath);
		}

		AtomicInteger added = new AtomicInteger(0);
		AtomicInteger skipped = new AtomicInteger(0);
		AtomicInteger errors = new AtomicInteger(0);

		List<FileScanEntry> savedEntries = new ArrayList<>();

		// Phase 1 — scan files and save MediaFile records
		try (Stream<Path> stream = Files.walk(root)) {
			stream.filter(Files::isRegularFile)
					.forEach(filePath -> scanMediaFile(filePath, savedEntries, added, skipped, errors));
		} catch (IOException e) {
			return new ScanResult(added.get(), skipped.get(), errors.get(),
					"Error walking directory: " + e.getMessage());
		}

		// Phase 2 — dispatch each saved MediaFile to the appropriate MediaTagScanner
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

		return new ScanResult(added.get(), skipped.get(), errors.get(),
				"Scan complete. Added: " + added.get() +
				", Skipped: " + skipped.get() +
				", Errors: " + errors.get());
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
