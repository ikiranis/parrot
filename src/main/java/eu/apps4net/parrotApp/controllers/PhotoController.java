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
import eu.apps4net.parrotApp.models.MediaFile;
import eu.apps4net.parrotApp.models.MediaKind;
import eu.apps4net.parrotApp.models.PhotoDetailDTO;
import eu.apps4net.parrotApp.models.ScanResult;
import eu.apps4net.parrotApp.repositories.MediaFileRepository;
import eu.apps4net.parrotApp.repositories.PhotoTagRepository;
import eu.apps4net.parrotApp.services.MediaScanService;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

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

	/**
	 * Constructs a new {@code PhotoController}.
	 *
	 * @param mediaScanService    the media scanning service
	 * @param mediaFileRepository the media file repository
	 * @param photoTagRepository  the photo tag repository
	 */
	public PhotoController(MediaScanService mediaScanService,
						   MediaFileRepository mediaFileRepository,
						   PhotoTagRepository photoTagRepository) {
		this.mediaScanService = mediaScanService;
		this.mediaFileRepository = mediaFileRepository;
		this.photoTagRepository = photoTagRepository;
	}

	/**
	 * Triggers a recursive folder scan for image files.
	 *
	 * @param request JSON body containing a {@code "folderPath"} key with the absolute server path
	 * @return {@link ScanResult} with counts of added, skipped, and errored files
	 * @throws ProcessingErrorException if {@code folderPath} is blank
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
	 * Returns up to {@code count} randomly selected photos in a single query.
	 *
	 * @param count number of photos to return (1–50, default 10)
	 * @return 200 with a {@link List} of {@link MediaFile} records, or 204 No Content if the library is empty
	 */
	@GetMapping("random")
	public ResponseEntity<List<MediaFile>> getRandomPhotos(
			@RequestParam(defaultValue = "10") int count) {
		List<MediaFile> photos = mediaFileRepository.findRandomPhotos(
				MediaKind.IMAGE.name(),
				PageRequest.of(0, Math.min(Math.max(count, 1), 50)));
		return photos.isEmpty()
				? ResponseEntity.noContent().build()
				: ResponseEntity.ok(photos);
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
	 * The file is read from the path stored in the {@link MediaFile} record.
	 *
	 * @param id the primary key of the media file
	 * @return the image as a binary response with the appropriate content type
	 * @throws NotFoundException if the media file record or the physical file does not exist
	 */
	@GetMapping("{id}/image")
	public ResponseEntity<Resource> getPhotoImage(@PathVariable Long id) {
		MediaFile mediaFile = mediaFileRepository.findById(id)
				.orElseThrow(() -> new NotFoundException("Photo not found: " + id));

		Path filePath = Paths.get(mediaFile.getPath(), mediaFile.getFilename());
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
}
