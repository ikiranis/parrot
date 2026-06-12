package eu.apps4net.parrotApp.controllers;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import eu.apps4net.parrotApp.exceptions.NotFoundException;
import eu.apps4net.parrotApp.exceptions.ProcessingErrorException;
import eu.apps4net.parrotApp.models.Folder;
import eu.apps4net.parrotApp.models.MediaFile;
import eu.apps4net.parrotApp.models.MediaKind;
import eu.apps4net.parrotApp.models.PhotoDetailDTO;
import eu.apps4net.parrotApp.models.PhotoTag;
import eu.apps4net.parrotApp.models.ScanResult;
import eu.apps4net.parrotApp.models.TagExportItemDTO;
import eu.apps4net.parrotApp.models.Thumbnail;
import eu.apps4net.parrotApp.repositories.MediaFileRepository;
import eu.apps4net.parrotApp.repositories.PhotoTagRepository;
import eu.apps4net.parrotApp.services.FolderService;
import eu.apps4net.parrotApp.services.MediaScanService;
import eu.apps4net.parrotApp.services.PhotoService;
import eu.apps4net.parrotApp.services.ThumbnailService;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

/**
 * REST controller for photo media file operations.
 * Exposes endpoints to scan folders for new photos and retrieve paginated photo listings.
 */
@RestController
@RequestMapping("api/photos")
public class PhotoController {

	/** Service responsible for scanning directories for media files. */
	private final MediaScanService mediaScanService;

	/** Repository for querying and persisting media file records. */
	private final MediaFileRepository mediaFileRepository;

	/** Repository for photo tag records linked to media files. */
	private final PhotoTagRepository photoTagRepository;

	/** Service for generating and retrieving thumbnails. */
	private final ThumbnailService thumbnailService;

	/** Service encapsulating photo business logic such as deletion. */
	private final PhotoService photoService;

	/** Service for resolving folder records when scoping a photo batch to a folder. */
	private final FolderService folderService;

	/** JDBC template used for bulk import writes, bypassing JPA dirty-checking overhead. */
	private final JdbcTemplate jdbcTemplate;

	/**
	 * Constructs a new {@code PhotoController}.
	 *
	 * @param mediaScanService    the media scanning service
	 * @param mediaFileRepository the media file repository
	 * @param photoTagRepository  the photo tag repository
	 * @param thumbnailService    the thumbnail service
	 * @param photoService        the photo service
	 * @param folderService       the folder service
	 * @param jdbcTemplate        the JDBC template
	 */
	public PhotoController(MediaScanService mediaScanService,
						   MediaFileRepository mediaFileRepository,
						   PhotoTagRepository photoTagRepository,
						   ThumbnailService thumbnailService,
						   PhotoService photoService,
						   FolderService folderService,
						   JdbcTemplate jdbcTemplate) {
		this.mediaScanService = mediaScanService;
		this.mediaFileRepository = mediaFileRepository;
		this.photoTagRepository = photoTagRepository;
		this.thumbnailService = thumbnailService;
		this.photoService = photoService;
		this.folderService = folderService;
		this.jdbcTemplate = jdbcTemplate;
	}

	/**
	 * Triggers a recursive folder scan for image files.
	 * The given folder path must match a configured library folder.
	 *
	 * @param request JSON body containing a {@code "folderPath"} key with the absolute server path
	 * @return {@link ScanResult} with counts of added, skipped, and errored files
	 * @throws ProcessingErrorException if {@code folderPath} is blank or does not match a library folder
	 */
	@PostMapping("scan")
	public ScanResult scanFolder(@RequestBody Map<String, String> request) {
		String folderPath = request.get("folderPath");

		if (folderPath == null || folderPath.isBlank()) {
			throw new ProcessingErrorException("folderPath must not be empty");
		}

		return mediaScanService.scanFolder(folderPath.trim());
	}

	/**
	 * Triggers a recursive scan of all configured library folders for image files.
	 *
	 * @return aggregated {@link ScanResult} across all library folders
	 */
	@PostMapping("scan-library")
	public ScanResult scanLibraryFolders() {
		return mediaScanService.scanLibraryFolders();
	}

	/**
	 * Returns a paginated list of all photo media files, newest first.
	 *
	 * @param page zero-based page index (default {@code 0})
	 * @param size number of items per page (default {@code 20})
	 * @return a {@link Page} of {@link MediaFile} records
	 */
	@GetMapping("all")
	public Page<MediaFile> getPhotos(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return mediaFileRepository.findByKind(MediaKind.IMAGE,
				PageRequest.of(page, size, Sort.by("id").descending()));
	}

	/**
	 * Returns up to {@code count} photos for the slideshow, generating any missing
	 * thumbnails before returning so that callers always receive a populated
	 * {@code thumbnailId}.
	 *
	 * Selection loads all photo IDs (a lightweight query) and then picks {@code count}
	 * of them. When {@code doShuffle} is true the subset is chosen via a partial
	 * Fisher-Yates shuffle, so every photo in the library is equally likely to appear
	 * in any given batch regardless of its position in the table. When {@code doShuffle}
	 * is false the photos are returned in sequence (ordered by id ascending) as stored
	 * in the database, continuing from the photo immediately after {@code afterId} so
	 * that consecutive calls page forward through the library instead of repeating the
	 * same head records. Either way this avoids both a full-table {@code ORDER BY RANDOM()}
	 * scan and the page-clustering problem of picking a single random page.
	 *
	 * When {@code folderId} is supplied the selection is scoped to that folder's subtree:
	 * only photos located directly in the folder or in any of its nested subfolders are
	 * considered. An unknown {@code folderId} falls back to the whole library.
	 *
	 * @param count     number of photos to return (1–50, default 10)
	 * @param folderId  id of the folder whose subtree to scope the selection to, or null for the
	 *                  whole library
	 * @param doShuffle true to return a random subset, false to return photos in sequence (default true)
	 * @param afterId   in sequential mode, the id to resume after; null starts from the first photo
	 *                  (ignored in shuffle mode)
	 * @return 200 with a {@link List} of {@link MediaFile} records (possibly empty at the end of the
	 *         sequence), or 204 No Content if the library is empty
	 */
	@GetMapping("batch")
	@Transactional
	public ResponseEntity<List<MediaFile>> getPhotos(
			@RequestParam(defaultValue = "10") int count,
			@RequestParam(required = false) Long folderId,
			@RequestParam(defaultValue = "true") boolean doShuffle,
			@RequestParam(required = false) Long afterId) {
		int size = Math.min(Math.max(count, 1), 50);
		Optional<Folder> folder = folderId != null ? folderService.getFolder(folderId) : Optional.empty();
		List<Long> allIds = folder
				.map(f -> mediaFileRepository.findIdsByKindAndFolderSubtree(
						MediaKind.IMAGE, f.getLibraryFolder(), f.getPath()))
				.orElseGet(() -> mediaFileRepository.findIdsByKind(MediaKind.IMAGE));
		if (allIds.isEmpty()) {
			return ResponseEntity.noContent().build();
		}

		List<Long> pickIds;
		if (doShuffle) {
			int pickCount = Math.min(size, allIds.size());
			ThreadLocalRandom rng = ThreadLocalRandom.current();
			for (int i = 0; i < pickCount; i++) {
				int j = rng.nextInt(i, allIds.size());
				Long tmp = allIds.get(i);
				allIds.set(i, allIds.get(j));
				allIds.set(j, tmp);
			}
			pickIds = allIds.subList(0, pickCount);
		} else {
			// Sequential mode: ids are sorted ascending, so resume right after afterId.
			int start = 0;
			if (afterId != null) {
				while (start < allIds.size() && allIds.get(start) <= afterId) {
					start++;
				}
			}
			int end = Math.min(start + size, allIds.size());
			pickIds = allIds.subList(start, end);
		}

		List<MediaFile> photos = new ArrayList<>(mediaFileRepository.findAllById(pickIds));
		if (!doShuffle) {
			// findAllById does not guarantee ordering, so restore the database sequence.
			photos.sort(Comparator.comparing(MediaFile::getId));
		}
		ensureThumbnails(photos);
		return ResponseEntity.ok(photos);
	}

	/**
	 * Generates a thumbnail for each photo in the list that does not yet have one.
	 * Photos whose source file is absent or whose thumbnail generation fails are skipped silently.
	 * After generation the in-memory {@code thumbnailId} field is synced so that the entity
	 * serialises correctly without requiring a second database round-trip.
	 *
	 * @param photos the list of media files to process
	 */
	private void ensureThumbnails(List<MediaFile> photos) {
		for (MediaFile photo : photos) {
			if (photo.getThumbnailId() != null) continue;
			Path sourceImage = Paths.get(photo.getLibraryFolder().getPath())
					.resolve(photo.getPath())
					.resolve(photo.getFilename());
			if (!Files.isRegularFile(sourceImage)) continue;
			try {
				Thumbnail thumbnail = thumbnailService.generatePhotoThumbnail(photo.getId(), sourceImage);
				photo.setThumbnail(thumbnail);
				photo.setThumbnailId(thumbnail.getId());
				mediaFileRepository.save(photo);
			} catch (IOException e) {
				// best-effort — skip photos whose thumbnail cannot be generated
			}
		}
	}

	/**
	 * Returns the detail view for a single photo, combining its {@link MediaFile}
	 * record with the associated {@link eu.apps4net.parrotApp.models.PhotoTag} if one exists.
	 *
	 * @param id the primary key of the media file
	 * @return a {@link PhotoDetailDTO} with all available metadata
	 * @throws NotFoundException if no media file with the given id exists
	 */
	@GetMapping("{id}")
	public PhotoDetailDTO getPhotoDetail(@PathVariable Long id) {
		MediaFile mediaFile = mediaFileRepository.findById(id)
				.orElseThrow(() -> new NotFoundException("Photo not found: " + id));
		return PhotoDetailDTO.from(mediaFile, photoTagRepository.findByMediaFile(mediaFile));
	}

	/**
	 * Serves the raw image bytes for the specified photo.
	 * The full file path is reconstructed from the library folder root and the
	 * relative path stored in the {@link MediaFile} record.
	 *
	 * @param id the primary key of the media file
	 * @return the image as a binary response with the appropriate content type
	 * @throws NotFoundException if the media file record or the physical file does not exist
	 */
	@GetMapping("{id}/image")
	public ResponseEntity<Resource> getPhotoImage(@PathVariable Long id) {
		MediaFile mediaFile = mediaFileRepository.findById(id)
				.orElseThrow(() -> new NotFoundException("Photo not found: " + id));

		Path filePath = Paths.get(mediaFile.getLibraryFolder().getPath())
				.resolve(mediaFile.getPath())
				.resolve(mediaFile.getFilename());
		File file = filePath.toFile();

		if (!file.exists() || !file.isFile()) {
			throw new NotFoundException("File not found on disk: " + filePath);
		}

		String contentType;
		try {
			String probed = Files.probeContentType(filePath);
			contentType = (probed != null) ? probed : "application/octet-stream";
		} catch (Exception e) {
			contentType = "application/octet-stream";
		}

		Resource resource = new FileSystemResource(file);
		return ResponseEntity.ok()
				.contentType(MediaType.parseMediaType(contentType))
				.body(resource);
	}

	/**
	 * Sets the rating for the specified photo.
	 * Creates a {@link eu.apps4net.parrotApp.models.PhotoTag} record if one does not yet exist.
	 *
	 * @param id      the primary key of the media file
	 * @param request JSON body containing a {@code "rating"} key with an integer value between 1 and 5
	 * @return the updated {@link PhotoDetailDTO}
	 * @throws NotFoundException        if no media file with the given id exists
	 * @throws ProcessingErrorException if {@code rating} is missing or out of the 1–5 range
	 */
	@PatchMapping("{id}/rating")
	@Transactional
	public PhotoDetailDTO setRating(@PathVariable Long id, @RequestBody Map<String, Integer> request) {
		Integer rating = request.get("rating");
		if (rating == null || rating < 1 || rating > 5) {
			throw new ProcessingErrorException("rating must be between 1 and 5");
		}
		MediaFile mediaFile = mediaFileRepository.findById(id)
				.orElseThrow(() -> new NotFoundException("Photo not found: " + id));
		PhotoTag tag = photoTagRepository.findByMediaFile(mediaFile)
				.orElseGet(() -> {
					PhotoTag t = new PhotoTag();
					t.setMediaFile(mediaFile);
					return t;
				});
		tag.setRating(rating);
		photoTagRepository.save(tag);
		return PhotoDetailDTO.from(mediaFile, Optional.of(tag));
	}

	/**
	 * Increments the view counter for the specified photo by one.
	 * Creates a {@link eu.apps4net.parrotApp.models.PhotoTag} record if one does not yet exist.
	 *
	 * @param id the primary key of the media file
	 * @return the updated {@link PhotoDetailDTO}
	 * @throws NotFoundException if no media file with the given id exists
	 */
	@PostMapping("{id}/view")
	@Transactional
	public PhotoDetailDTO incrementView(@PathVariable Long id) {
		MediaFile mediaFile = mediaFileRepository.findById(id)
				.orElseThrow(() -> new NotFoundException("Photo not found: " + id));
		PhotoTag tag = photoTagRepository.findByMediaFile(mediaFile)
				.orElseGet(() -> {
					PhotoTag t = new PhotoTag();
					t.setMediaFile(mediaFile);
					return t;
				});
		long current = tag.getViewCount() != null ? tag.getViewCount() : 0L;
		tag.setViewCount(current + 1);
		photoTagRepository.save(tag);
		return PhotoDetailDTO.from(mediaFile, Optional.of(tag));
	}

	/**
	 * Generates a thumbnail for the specified photo and returns its id.
	 * If the photo already has a thumbnail, returns the existing id without regenerating.
	 * The physical image file must exist on disk; a 404 is returned if it does not.
	 *
	 * @param id the primary key of the media file
	 * @return a map with key {@code thumbnailId} set to the thumbnail primary key
	 * @throws NotFoundException        if no media file with the given id exists, or the file is not on disk
	 * @throws ProcessingErrorException if thumbnail generation fails
	 */
	@PostMapping("{id}/thumbnail")
	@Transactional
	public Map<String, Long> generateThumbnail(@PathVariable Long id) {
		MediaFile mediaFile = mediaFileRepository.findById(id)
				.orElseThrow(() -> new NotFoundException("Photo not found: " + id));

		if (mediaFile.getThumbnailId() != null) {
			return Map.of("thumbnailId", mediaFile.getThumbnailId());
		}

		Path sourceImage = Paths.get(mediaFile.getLibraryFolder().getPath())
				.resolve(mediaFile.getPath())
				.resolve(mediaFile.getFilename());

		if (!Files.isRegularFile(sourceImage)) {
			throw new NotFoundException("Image file not found on disk: " + sourceImage);
		}

		try {
			Thumbnail thumbnail = thumbnailService.generatePhotoThumbnail(id, sourceImage);
			mediaFile.setThumbnail(thumbnail);
			return Map.of("thumbnailId", thumbnail.getId());
		} catch (IOException e) {
			throw new ProcessingErrorException("Thumbnail generation failed: " + e.getMessage());
		}
	}

	/**
	 * Deletes a single photo record from the database by its primary key.
	 * The associated {@link eu.apps4net.parrotApp.models.PhotoTag}, if any, is removed first.
	 * The physical file on disk is not affected.
	 *
	 * @param id the primary key of the media file to delete
	 * @return an empty 204 No Content response
	 * @throws NotFoundException if no media file with the given id exists
	 */
	@DeleteMapping("{id}")
	public ResponseEntity<Void> deletePhoto(@PathVariable Long id) {
		photoService.deletePhoto(id);
		return ResponseEntity.noContent().build();
	}

	/**
	 * Deletes all photo records from the database. The actual files are not affected.
	 * Dependent {@code PhotoTag} rows are removed first to satisfy the foreign key constraint.
	 *
	 * @return an empty 204 No Content response
	 */
	@DeleteMapping("all")
	@Transactional
	public ResponseEntity<Void> clearLibrary() {
		photoTagRepository.deleteAllByMediaFileKind(MediaKind.IMAGE);
		mediaFileRepository.deleteAllByKind(MediaKind.IMAGE);
		return ResponseEntity.noContent().build();
	}

	/**
	 * Exports all tag entries that have at least one view or a rating set.
	 * Each entry includes the full absolute file path, filename, rating, and view count.
	 *
	 * @return list of {@link TagExportItemDTO} for tags with user-assigned data
	 */
	@GetMapping("tags/export")
	public List<TagExportItemDTO> exportTags() {
		return photoTagRepository.findAllWithViewsOrRating().stream()
				.map(tag -> {
					MediaFile mf = tag.getMediaFile();
					String fullPath = Paths.get(mf.getLibraryFolder().getPath(), mf.getPath()).toString();
					return new TagExportItemDTO(fullPath, mf.getFilename(), tag.getRating(), tag.getViewCount());
				})
				.toList();
	}

	/**
	 * Imports tag data from a JSON payload and updates the matching {@link PhotoTag} records.
	 * Each item is matched by its full absolute path and filename.
	 * Items with no existing {@link PhotoTag} are counted as not found and skipped.
	 * Only {@code rating} and {@code viewCount} fields are updated; all other metadata is preserved.
	 *
	 * Performance strategy:
	 * - A single JOIN query loads all existing photo tags into an in-memory map keyed by full path.
	 * - Each item is resolved by a single O(1) map lookup — no per-file or per-directory DB round-trips.
	 * - Writes bypass JPA entirely via {@link JdbcTemplate} batch UPDATE.
	 *
	 * @param items list of {@link TagExportItemDTO} entries to import
	 * @return a map with keys {@code updated} (matched and saved) and {@code notFound} (unmatched entries)
	 */
	@PostMapping("tags/import")
	@Transactional
	public ResponseEntity<Map<String, Integer>> importTags(@RequestBody List<TagExportItemDTO> items) {
		if (items.isEmpty()) {
			return ResponseEntity.ok(Map.of("updated", 0, "notFound", 0));
		}

		// Load existing photo tags once: fullAbsolutePath → tagId
		Map<String, Long> view = new HashMap<>();
		jdbcTemplate.query(
			"SELECT lf.path, mf.path, mf.filename, pt.id"
				+ " FROM photo_tag pt"
				+ " JOIN media_file mf ON pt.media_file_id = mf.id"
				+ " JOIN library_folder lf ON mf.library_folder_id = lf.id",
			rs -> {
				String lfPath = rs.getString(1);
				String mfPath = rs.getString(2);
				String filename = rs.getString(3);
				view.put(Paths.get(lfPath, mfPath, filename).toString(), rs.getLong(4));
			}
		);

		record TagWrite(Long tagId, Integer rating, Long viewCount) {}
		List<TagWrite> toUpdate = new ArrayList<>();
		int notFound = 0;

		for (TagExportItemDTO item : items) {
			Long tagId = view.get(Paths.get(item.getPath(), item.getFilename()).toString());
			if (tagId == null) {
				notFound++;
				continue;
			}
			toUpdate.add(new TagWrite(tagId, item.getRating(), item.getViewCount()));
		}

		if (!toUpdate.isEmpty()) {
			jdbcTemplate.batchUpdate(
				"UPDATE photo_tag SET rating = ?, view_count = ?, date_updated = CURRENT_TIMESTAMP WHERE id = ?",
				new BatchPreparedStatementSetter() {
					@Override
					public void setValues(PreparedStatement ps, int i) throws SQLException {
						TagWrite tw = toUpdate.get(i);
						if (tw.rating() != null) ps.setInt(1, tw.rating()); else ps.setNull(1, Types.INTEGER);
						if (tw.viewCount() != null) ps.setLong(2, tw.viewCount()); else ps.setNull(2, Types.BIGINT);
						ps.setLong(3, tw.tagId());
					}
					@Override
					public int getBatchSize() { return toUpdate.size(); }
				}
			);
		}

		return ResponseEntity.ok(Map.of("updated", toUpdate.size(), "notFound", notFound));
	}
}
