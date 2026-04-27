package eu.apps4net.parrotApp.controllers;

import org.springframework.beans.factory.annotation.Autowired;
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

@RestController
@RequestMapping("api/photo")
public class PhotoController {

	private final PhotoScanService photoScanService;
	private final MediaFileRepository mediaFileRepository;

	@Autowired
	public PhotoController(PhotoScanService photoScanService,
						   MediaFileRepository mediaFileRepository) {
		this.photoScanService = photoScanService;
		this.mediaFileRepository = mediaFileRepository;
	}

	/**
	 * Trigger a folder scan for photos.
	 *
	 * @param request JSON body with "folderPath" key
	 * @return ScanResult with added/skipped/errors counts
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
	 * Get a paginated list of all photo media files.
	 *
	 * @param page page index (0-based)
	 * @param size page size
	 * @return Page of MediaFile
	 */
	@GetMapping("all")
	public Page<MediaFile> getPhotos(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return mediaFileRepository.findByKind(MediaKind.IMAGE,
				PageRequest.of(page, size, Sort.by("id").descending()));
	}
}
