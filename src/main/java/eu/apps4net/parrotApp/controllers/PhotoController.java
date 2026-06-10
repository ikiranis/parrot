package eu.apps4net.parrotApp.controllers;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import eu.apps4net.parrotApp.exceptions.NotFoundException;
import eu.apps4net.parrotApp.exceptions.ProcessingErrorException;
import eu.apps4net.parrotApp.models.LibraryFolder;
import eu.apps4net.parrotApp.models.MediaFile;
import eu.apps4net.parrotApp.models.MediaKind;
import eu.apps4net.parrotApp.models.PhotoDetailDTO;
import eu.apps4net.parrotApp.models.PhotoTag;
import eu.apps4net.parrotApp.models.ScanResult;
import eu.apps4net.parrotApp.models.TagExportItemDTO;
import eu.apps4net.parrotApp.models.Thumbnail;
import eu.apps4net.parrotApp.repositories.MediaFileRepository;
import eu.apps4net.parrotApp.repositories.PhotoTagRepository;
import eu.apps4net.parrotApp.services.LibraryFolderService;
import eu.apps4net.parrotApp.services.MediaScanService;
import eu.apps4net.parrotApp.services.ThumbnailService;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
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

	/** Service for looking up library folders, used to resolve full paths during tag import/export. */
	private final LibraryFolderService libraryFolderService;

	/** Service for generating and retrieving thumbnails. */
	private final ThumbnailService thumbnailService;

	/**
	 * Constructs a new {@code PhotoController}.
	 *
	 * @param mediaScanService    the media scanning service
	 * @param mediaFileRepository the media file repository
	 * @param photoTagRepository  the photo tag repository
	 * @param libraryFolderService the library folder service
	 * @param thumbnailService    the thumbnail service
	 */
	public PhotoController(MediaScanService mediaScanService,
						   MediaFileRepository mediaFileRepository,
						   PhotoTagRepository photoTagRepository,
						   LibraryFolderService libraryFolderService,
						   ThumbnailService thumbnailService) {
		this.mediaScanService = mediaScanService;
		this.mediaFileRepository = mediaFileRepository;
		this.photoTagRepository = photoTagRepository;
		this.libraryFolderService = libraryFolderService;
		this.thumbnailService = thumbnailService;
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
	 * Returns up to {@code count} randomly selected photos, generating any missing thumbnails
	 * before returning so that callers always receive a populated {@code thumbnailId}.
	 *
	 * Selection is done by counting the photos once (a cheap aggregate) and
	 * fetching a single random page of that size. This deliberately avoids a
	 * {@code ORDER BY RANDOM()} query, which forces the database to scan and sort
	 * the entire table on every call and churns large amounts of heap when the
	 * slideshow requests batches continuously. The returned page is shuffled so
	 * the order within the batch is not predictable.
	 *
	 * @param count number of photos to return (1–50, default 10)
	 * @return 200 with a {@link List} of {@link MediaFile} records, or 204 No Content if the library is empty
	 */
	@GetMapping("random")
	@Transactional
	public ResponseEntity<List<MediaFile>> getRandomPhotos(
			@RequestParam(defaultValue = "10") int count) {
		int size = Math.min(Math.max(count, 1), 50);
		long total = mediaFileRepository.countByKind(MediaKind.IMAGE);
		if (total == 0) {
			return ResponseEntity.noContent().build();
		}
		int totalPages = (int) ((total + size - 1) / size);
		int randomPage = ThreadLocalRandom.current().nextInt(totalPages);
		List<MediaFile> photos = new ArrayList<>(mediaFileRepository.findByKind(
				MediaKind.IMAGE,
				PageRequest.of(randomPage, size, Sort.by("id"))).getContent());
		Collections.shuffle(photos);
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
	 * Imports tag data from a JSON payload and updates the matching records in the database.
	 * Each item is matched by its full absolute path and filename. The path is resolved against
	 * the configured library folders to find the relative path stored in the database.
	 * A {@link PhotoTag} is created if none exists.
	 * Only {@code rating} and {@code viewCount} fields are updated; all other metadata is preserved.
	 *
	 * @param items list of {@link TagExportItemDTO} entries to import
	 * @return a map with keys {@code updated} (matched and saved) and {@code notFound} (unmatched entries)
	 */
	@PostMapping("tags/import")
	@Transactional
	public ResponseEntity<Map<String, Integer>> importTags(@RequestBody List<TagExportItemDTO> items) {
		int updated = 0;
		int notFound = 0;
		for (TagExportItemDTO item : items) {
			Optional<LibraryFolder> lfOpt = libraryFolderService.findMatchingForPath(item.getPath());
			if (lfOpt.isEmpty()) {
				notFound++;
				continue;
			}
			LibraryFolder lf = lfOpt.get();
			String relativePath = Paths.get(lf.getPath()).relativize(Paths.get(item.getPath())).toString();
			Optional<MediaFile> mediaFileOpt = mediaFileRepository
					.findByLibraryFolderAndPathAndFilename(lf, relativePath, item.getFilename());
			if (mediaFileOpt.isEmpty()) {
				notFound++;
				continue;
			}
			MediaFile mediaFile = mediaFileOpt.get();
			PhotoTag tag = photoTagRepository.findByMediaFile(mediaFile)
					.orElseGet(() -> {
						PhotoTag t = new PhotoTag();
						t.setMediaFile(mediaFile);
						return t;
					});
			if (item.getRating() != null) {
				tag.setRating(item.getRating());
			}
			if (item.getViewCount() != null) {
				tag.setViewCount(item.getViewCount());
			}
			photoTagRepository.save(tag);
			updated++;
		}
		return ResponseEntity.ok(Map.of("updated", updated, "notFound", notFound));
	}
}
