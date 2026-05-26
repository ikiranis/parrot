package eu.apps4net.parrotApp.controllers;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import eu.apps4net.parrotApp.exceptions.ProcessingErrorException;
import eu.apps4net.parrotApp.models.MediaFile;
import eu.apps4net.parrotApp.models.MediaKind;
import eu.apps4net.parrotApp.models.ScanResult;
import eu.apps4net.parrotApp.repositories.MediaFileRepository;
import eu.apps4net.parrotApp.services.PhotoScanService;

import java.util.Map;

/**
 * REST controller for photo media file operations.
 * Exposes endpoints to scan folders for new photos and retrieve paginated photo listings.
 */
@RestController
@RequestMapping("api/photos")
public class PhotoController {

	/** Service responsible for scanning directories for image files. */
	private final PhotoScanService photoScanService;

	/** Repository for querying and persisting media file records. */
	private final MediaFileRepository mediaFileRepository;

	/**
	 * Constructs a new {@code PhotoController}.
	 *
	 * @param photoScanService      the photo scanning service
	 * @param mediaFileRepository   the media file repository
	 */
	public PhotoController(PhotoScanService photoScanService,
						   MediaFileRepository mediaFileRepository) {
		this.photoScanService = photoScanService;
		this.mediaFileRepository = mediaFileRepository;
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

		return photoScanService.scanFolder(folderPath.trim());
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
}
