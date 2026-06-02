package eu.apps4net.parrotApp.services.tagscanner;

import org.springframework.stereotype.Component;

import eu.apps4net.parrotApp.models.MediaFile;
import eu.apps4net.parrotApp.models.MediaKind;
import eu.apps4net.parrotApp.models.PhotoTag;
import eu.apps4net.parrotApp.repositories.PhotoTagRepository;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

/**
 * {@link MediaTagScanner} implementation for image files ({@link MediaKind#IMAGE}).
 *
 * <p>Creates and persists a {@link PhotoTag} record containing the display name,
 * file size, MIME type, and pixel dimensions of the image.</p>
 */
@Component
public class PhotoTagScanner implements MediaTagScanner {

	/** Repository for persisting photo tag records. */
	private final PhotoTagRepository photoTagRepository;

	/**
	 * Constructs a new {@code PhotoTagScanner}.
	 *
	 * @param photoTagRepository repository for photo tag persistence
	 */
	public PhotoTagScanner(PhotoTagRepository photoTagRepository) {
		this.photoTagRepository = photoTagRepository;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @return {@link MediaKind#IMAGE}
	 */
	@Override
	public MediaKind getSupportedKind() {
		return MediaKind.IMAGE;
	}

	/**
	 * Scans the image file at {@code filePath} and persists a {@link PhotoTag} containing
	 * the display name, file size, MIME type, and pixel dimensions.
	 *
	 * @param mediaFile the already-persisted media file record
	 * @param filePath  the path to the image file on disk
	 */
	@Override
	public void scanTags(MediaFile mediaFile, Path filePath) {
		File file = filePath.toFile();
		String filename = filePath.getFileName().toString();

		PhotoTag photoTag = new PhotoTag();
		photoTag.setMediaFile(mediaFile);
		photoTag.setName(stripExtension(filename));
		photoTag.setFilesize(file.length());
		photoTag.setMimeType(resolveMimeType(filename));

		readImageDimensions(file, photoTag);

		photoTagRepository.save(photoTag);
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
