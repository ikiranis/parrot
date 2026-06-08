package eu.apps4net.parrotApp.services.tagscanner;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import eu.apps4net.parrotApp.models.MediaFile;
import eu.apps4net.parrotApp.models.MediaKind;
import eu.apps4net.parrotApp.models.PhotoTag;
import eu.apps4net.parrotApp.repositories.PhotoTagRepository;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

/**
 * {@link MediaTagScanner} implementation for image files ({@link MediaKind#IMAGE}).
 *
 * Creates and persists a {@link PhotoTag} record containing the display name,
 * file size, MIME type, and pixel dimensions of the image.
 */
@Component
public class PhotoTagScanner implements MediaTagScanner {

	/** Repository for persisting photo tag records (used by single-file scan path). */
	private final PhotoTagRepository photoTagRepository;

	/** JDBC template for bulk inserts in batch scan path, bypassing the JPA persistence context. */
	private final JdbcTemplate jdbcTemplate;

	/**
	 * Constructs a new {@code PhotoTagScanner}.
	 *
	 * @param photoTagRepository repository for photo tag persistence
	 * @param jdbcTemplate       JDBC template for batch inserts
	 */
	public PhotoTagScanner(PhotoTagRepository photoTagRepository, JdbcTemplate jdbcTemplate) {
		this.photoTagRepository = photoTagRepository;
		this.jdbcTemplate = jdbcTemplate;
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
	 * the display name, album, file size, MIME type, and pixel dimensions.
	 *
	 * @param mediaFile the already-persisted media file record
	 * @param filePath  the path to the image file on disk
	 * @param rootPath  the root scan folder; used to suppress the album for top-level files
	 */
	@Override
	public void scanTags(MediaFile mediaFile, Path filePath, Path rootPath) {
		photoTagRepository.save(buildPhotoTag(mediaFile, filePath, rootPath));
	}

	/**
	 * Builds {@link PhotoTag} records for a batch of image files then inserts them via
	 * JDBC in a single batch statement, bypassing the JPA persistence context so that
	 * neither the tags nor their associated {@link MediaFile} entities accumulate in
	 * Hibernate's first-level cache.
	 *
	 * @param files        the already-persisted media file records to tag
	 * @param rootResolver function that maps each {@link MediaFile} to the library root path
	 */
	@Override
	public void scanTagsBatch(List<MediaFile> files, Function<MediaFile, Path> rootResolver) {
		List<PhotoTag> tags = new ArrayList<>(files.size());
		for (MediaFile mf : files) {
			Path filePath = Paths.get(mf.getPath()).resolve(mf.getFilename());
			tags.add(buildPhotoTag(mf, filePath, rootResolver.apply(mf)));
		}
		if (tags.isEmpty()) return;

		Timestamp now = Timestamp.valueOf(LocalDateTime.now());
		jdbcTemplate.batchUpdate(
				"INSERT INTO photo_tag (media_file_id, name, album, filesize, mime_type, width, height," +
				" view_count, date_created, date_updated) VALUES (?, ?, ?, ?, ?, ?, ?, 0, ?, ?)",
				tags,
				tags.size(),
				(ps, tag) -> {
					ps.setLong(1, tag.getMediaFile().getId());
					ps.setString(2, tag.getName());
					ps.setString(3, tag.getAlbum());
					if (tag.getFilesize() != null) {
						ps.setLong(4, tag.getFilesize());
					} else {
						ps.setNull(4, Types.BIGINT);
					}
					ps.setString(5, tag.getMimeType());
					if (tag.getWidth() != null) {
						ps.setInt(6, tag.getWidth());
					} else {
						ps.setNull(6, Types.INTEGER);
					}
					if (tag.getHeight() != null) {
						ps.setInt(7, tag.getHeight());
					} else {
						ps.setNull(7, Types.INTEGER);
					}
					ps.setTimestamp(8, now);
					ps.setTimestamp(9, now);
				}
		);
	}

	/**
	 * Builds a {@link PhotoTag} for the given image file without persisting it.
	 *
	 * @param mediaFile the media file record
	 * @param filePath  the path to the image file on disk
	 * @param rootPath  the root scan folder
	 * @return a fully populated, unsaved {@link PhotoTag}
	 */
	@NonNull
	private PhotoTag buildPhotoTag(MediaFile mediaFile, Path filePath, Path rootPath) {
		File file = filePath.toFile();
		String filename = filePath.getFileName().toString();

		PhotoTag photoTag = new PhotoTag();
		photoTag.setMediaFile(mediaFile);
		photoTag.setName(stripExtension(filename));
		photoTag.setAlbum(resolveAlbum(filePath, rootPath));
		photoTag.setFilesize(file.length());
		photoTag.setMimeType(resolveMimeType(filename));
		readImageDimensions(file, photoTag);
		return photoTag;
	}

	/**
	 * Reads the pixel width and height from the image file header without decoding
	 * any pixel data. Uses an {@link ImageReader} positioned on an
	 * {@link ImageInputStream} so that only the format-specific header bytes are
	 * consumed, keeping memory usage near zero regardless of image resolution.
	 *
	 * @param file     the image file to inspect
	 * @param photoTag the tag whose width and height will be populated
	 */
	private void readImageDimensions(File file, PhotoTag photoTag) {
		try (ImageInputStream iis = ImageIO.createImageInputStream(file)) {
			if (iis == null) return;
			Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
			if (!readers.hasNext()) return;
			ImageReader reader = readers.next();
			try {
				reader.setInput(iis, true, true);
				photoTag.setWidth(reader.getWidth(0));
				photoTag.setHeight(reader.getHeight(0));
			} finally {
				reader.dispose();
			}
		} catch (IOException ignored) {
			// Dimensions are optional — skip on failure
		}
	}

	/**
	 * Resolves the album name for a photo from its parent directory.
	 * Returns {@code null} if the photo is directly inside {@code rootPath}.
	 *
	 * @param filePath the full path to the photo file
	 * @param rootPath the root scan folder
	 * @return the parent folder name, or {@code null} if the photo is in the root
	 */
	private String resolveAlbum(Path filePath, Path rootPath) {
		Path parent = filePath.getParent();
		if (parent == null || parent.equals(rootPath)) {
			return null;
		}
		return parent.getFileName().toString();
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
