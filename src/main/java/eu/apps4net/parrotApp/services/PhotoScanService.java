package eu.apps4net.parrotApp.services;

import org.springframework.stereotype.Service;

import eu.apps4net.parrotApp.models.MediaFile;
import eu.apps4net.parrotApp.models.MediaKind;
import eu.apps4net.parrotApp.models.PhotoTag;
import eu.apps4net.parrotApp.models.ScanResult;
import eu.apps4net.parrotApp.repositories.MediaFileRepository;
import eu.apps4net.parrotApp.repositories.PhotoTagRepository;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

/**
 * Service that recursively scans a server-side directory for image files and
 * persists new entries as {@link MediaFile} + {@link PhotoTag} pairs.
 * Already-indexed files are skipped using a path+filename lookup.
 */
@Service
public class PhotoScanService {

	/** Supported image file extensions (lower-case, without the leading dot). */
	private static final Set<String> IMAGE_EXTENSIONS = Set.of(
			"jpg", "jpeg", "png", "gif", "bmp", "webp", "tiff", "tif"
	);

	/** Repository for querying and persisting media file records. */
	private final MediaFileRepository mediaFileRepository;

	/** Repository for querying and persisting photo tag records. */
	private final PhotoTagRepository photoTagRepository;

	/**
	 * Constructs a new {@code PhotoScanService}.
	 *
	 * @param mediaFileRepository repository for media file persistence
	 * @param photoTagRepository  repository for photo tag persistence
	 */
	public PhotoScanService(MediaFileRepository mediaFileRepository,
							PhotoTagRepository photoTagRepository) {
		this.mediaFileRepository = mediaFileRepository;
		this.photoTagRepository = photoTagRepository;
	}

	/**
	 * Scans the given folder recursively for image files and saves them to the database.
	 *
	 * @param folderPath absolute path to the folder to scan
	 * @return ScanResult with counts of added, skipped, and errored files
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

		try (Stream<Path> stream = Files.walk(root)) {
			stream.filter(Files::isRegularFile)
					.filter(this::isImageFile)
					.forEach(filePath -> processFile(filePath, added, skipped, errors));
		} catch (IOException e) {
			return new ScanResult(added.get(), skipped.get(), errors.get(),
					"Error walking directory: " + e.getMessage());
		}

		return new ScanResult(added.get(), skipped.get(), errors.get(),
				"Scan complete. Added: " + added.get() +
				", Skipped: " + skipped.get() +
				", Errors: " + errors.get());
	}

	/**
	 * Processes a single image file: skips it if already indexed, otherwise persists
	 * a new {@link MediaFile} and {@link PhotoTag} record.
	 *
	 * @param filePath path to the image file to process
	 * @param added    counter incremented when a file is successfully persisted
	 * @param skipped  counter incremented when a file is already in the database
	 * @param errors   counter incremented when an exception occurs during processing
	 */
	private void processFile(Path filePath, AtomicInteger added, AtomicInteger skipped, AtomicInteger errors) {
		String filename = filePath.getFileName().toString();
		String parentPath = filePath.getParent() != null
				? filePath.getParent().toString()
				: "";

		// Skip if already in database
		if (mediaFileRepository.findByPathAndFilename(parentPath, filename).isPresent()) {
			skipped.incrementAndGet();
			return;
		}

		try {
			File file = filePath.toFile();

			MediaFile mediaFile = new MediaFile(parentPath, filename, null, MediaKind.IMAGE);
			mediaFileRepository.save(mediaFile);

			PhotoTag photoTag = new PhotoTag();
			photoTag.setMediaFile(mediaFile);
			photoTag.setName(stripExtension(filename));
			photoTag.setFilesize(file.length());
			photoTag.setMimeType(resolveMimeType(filename));

			readImageDimensions(file, photoTag);

			photoTagRepository.save(photoTag);
			added.incrementAndGet();
		} catch (Exception e) {
			errors.incrementAndGet();
		}
	}

	/**
	 * Reads the pixel width and height of an image file and sets them on the given
	 * {@link PhotoTag}. Silently ignores any {@link IOException} as dimensions are
	 * considered optional metadata.
	 *
	 * @param file     the image file to read
	 * @param photoTag the tag whose width and height will be populated
	 */
	private void readImageDimensions(File file, PhotoTag photoTag) {
		try {
			BufferedImage image = ImageIO.read(file);
			if (image != null) {
				photoTag.setWidth(image.getWidth());
				photoTag.setHeight(image.getHeight());
			}
		} catch (IOException ignored) {
			// Dimensions are optional — skip on failure
		}
	}

	/**
	 * Returns {@code true} if the given path has a file extension that belongs to
	 * {@link #IMAGE_EXTENSIONS}.
	 *
	 * @param path the file path to inspect
	 * @return {@code true} if the file is a recognised image type, {@code false} otherwise
	 */
	private boolean isImageFile(Path path) {
		String name = path.getFileName().toString().toLowerCase();
		int dot = name.lastIndexOf('.');
		if (dot < 0) return false;
		return IMAGE_EXTENSIONS.contains(name.substring(dot + 1));
	}

	/**
	 * Removes the file extension from a filename (everything from the last {@code .}
	 * onwards). Returns the original string unchanged if no dot is found.
	 *
	 * @param filename the filename to process
	 * @return the filename without its extension
	 */
	private String stripExtension(String filename) {
		int dot = filename.lastIndexOf('.');
		return dot > 0 ? filename.substring(0, dot) : filename;
	}

	/**
	 * Resolves the MIME type for an image file based on its extension.
	 * Falls back to {@code image/*} for unrecognised extensions.
	 *
	 * @param filename the filename whose extension is used for MIME-type resolution
	 * @return the MIME type string, e.g. {@code "image/jpeg"}
	 */
	private String resolveMimeType(String filename) {
		String ext = filename.toLowerCase();
		if (ext.endsWith(".jpg") || ext.endsWith(".jpeg")) return "image/jpeg";
		if (ext.endsWith(".png")) return "image/png";
		if (ext.endsWith(".gif")) return "image/gif";
		if (ext.endsWith(".bmp")) return "image/bmp";
		if (ext.endsWith(".webp")) return "image/webp";
		if (ext.endsWith(".tiff") || ext.endsWith(".tif")) return "image/tiff";
		return "image/*";
	}
}
