package eu.apps4net.parrotApp.controllers;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import eu.apps4net.parrotApp.exceptions.NotFoundException;
import eu.apps4net.parrotApp.models.Thumbnail;
import eu.apps4net.parrotApp.services.ThumbnailService;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * REST controller for thumbnail image serving.
 * Exposes a single endpoint that streams the generated thumbnail JPEG by its
 * primary key; the image file is resolved from the application working directory.
 */
@RestController
@RequestMapping("api/thumbnails")
public class ThumbnailController {

	/** Root directory name where generated thumbnail files are stored. */
	private static final String THUMBNAILS_ROOT = "thumbnails";

	/** Service for thumbnail data access. */
	private final ThumbnailService thumbnailService;

	/**
	 * Constructs a new {@code ThumbnailController}.
	 *
	 * @param thumbnailService the thumbnail service
	 */
	public ThumbnailController(ThumbnailService thumbnailService) {
		this.thumbnailService = thumbnailService;
	}

	/**
	 * Serves the thumbnail image for the given id as a JPEG.
	 *
	 * @param id the primary key of the thumbnail record
	 * @return the thumbnail image bytes with {@code image/jpeg} content type
	 * @throws NotFoundException if no thumbnail record exists for the given id,
	 *                           or the physical file is not found on disk
	 */
	@GetMapping("{id}")
	public ResponseEntity<Resource> getThumbnailImage(@PathVariable Long id) {
		Thumbnail thumbnail = thumbnailService.getThumbnail(id)
				.orElseThrow(() -> new NotFoundException("Thumbnail not found: " + id));

		Path filePath = Paths.get(THUMBNAILS_ROOT).resolve(thumbnail.getPath());
		if (!filePath.toFile().exists()) {
			throw new NotFoundException("Thumbnail file not found on disk: " + filePath);
		}

		return ResponseEntity.ok()
				.contentType(MediaType.IMAGE_JPEG)
				.body(new FileSystemResource(filePath));
	}
}
