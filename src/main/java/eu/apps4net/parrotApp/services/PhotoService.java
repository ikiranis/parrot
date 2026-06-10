package eu.apps4net.parrotApp.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import eu.apps4net.parrotApp.exceptions.NotFoundException;
import eu.apps4net.parrotApp.models.MediaFile;
import eu.apps4net.parrotApp.models.Thumbnail;
import eu.apps4net.parrotApp.repositories.MediaFileRepository;
import eu.apps4net.parrotApp.repositories.PhotoTagRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Service layer for photo operations.
 * Handles business logic for creating, updating, and deleting photo records.
 */
@Service
public class PhotoService {

	/** Repository for querying and persisting media file records. */
	private final MediaFileRepository mediaFileRepository;

	/** Repository for photo tag records linked to media files. */
	private final PhotoTagRepository photoTagRepository;

	/** Service used to resolve thumbnail file paths. */
	private final ThumbnailService thumbnailService;

	/**
	 * Constructs a new {@code PhotoService}.
	 *
	 * @param mediaFileRepository the media file repository
	 * @param photoTagRepository  the photo tag repository
	 * @param thumbnailService    the thumbnail service
	 */
	public PhotoService(MediaFileRepository mediaFileRepository,
						PhotoTagRepository photoTagRepository,
						ThumbnailService thumbnailService) {
		this.mediaFileRepository = mediaFileRepository;
		this.photoTagRepository = photoTagRepository;
		this.thumbnailService = thumbnailService;
	}

	/**
	 * Deletes the photo record with the given id from the database and removes
	 * both the original image file and its thumbnail from disk.
	 *
	 * The deletion order is:
	 * - 1. {@link eu.apps4net.parrotApp.models.PhotoTag} (foreign key to {@link MediaFile})
	 * - 2. {@link MediaFile} (cascade via {@code orphanRemoval} removes the linked {@link Thumbnail} record)
	 * - 3. Thumbnail file on disk (if present)
	 * - 4. Original image file on disk
	 *
	 * Physical file deletions are best-effort: a missing or unreadable file is silently
	 * skipped rather than causing the overall operation to fail.
	 *
	 * @param id the primary key of the media file to delete
	 * @throws NotFoundException if no media file with the given id exists
	 */
	@Transactional
	public void deletePhoto(Long id) {
		MediaFile mediaFile = mediaFileRepository.findById(id)
				.orElseThrow(() -> new NotFoundException("Photo not found: " + id));

		Path originalFile = Paths.get(mediaFile.getLibraryFolder().getPath())
				.resolve(mediaFile.getPath())
				.resolve(mediaFile.getFilename());

		Thumbnail thumbnail = mediaFile.getThumbnail();
		Path thumbnailFile = (thumbnail != null) ? thumbnailService.resolveThumbnailPath(thumbnail) : null;

		photoTagRepository.findByMediaFile(mediaFile).ifPresent(photoTagRepository::delete);
		mediaFileRepository.delete(mediaFile);

		if (thumbnailFile != null) deleteFileQuietly(thumbnailFile);
		deleteFileQuietly(originalFile);
	}

	/**
	 * Deletes the file at the given path, ignoring any errors.
	 * Missing files are treated as a no-op.
	 *
	 * @param path the file to delete
	 */
	private void deleteFileQuietly(Path path) {
		try {
			Files.deleteIfExists(path);
		} catch (IOException e) {
			// best-effort — log but do not propagate
			System.err.println("PhotoService: could not delete file " + path + " — " + e.getMessage());
		}
	}
}
