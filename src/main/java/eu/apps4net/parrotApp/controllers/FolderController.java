package eu.apps4net.parrotApp.controllers;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import eu.apps4net.parrotApp.exceptions.NotFoundException;
import eu.apps4net.parrotApp.exceptions.ProcessingErrorException;
import eu.apps4net.parrotApp.models.Folder;
import eu.apps4net.parrotApp.models.MediaFile;
import eu.apps4net.parrotApp.models.MediaKind;
import eu.apps4net.parrotApp.repositories.MediaFileRepository;
import eu.apps4net.parrotApp.services.FolderService;
import eu.apps4net.parrotApp.services.ThumbnailService;
import eu.apps4net.parrotApp.utilities.PhotoSortResolver;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * REST controller for folder operations.
 * Exposes endpoints to retrieve the scanned folder records from the library,
 * browse folder hierarchy by level, navigate into child folders, and list the
 * photos contained directly within a given folder.
 */
@RestController
@RequestMapping("api/folders")
public class FolderController {

	/** Service for folder data access. */
	private final FolderService folderService;

	/** Repository for querying media files within a folder. */
	private final MediaFileRepository mediaFileRepository;

	/** Service for generating folder thumbnails. */
	private final ThumbnailService thumbnailService;

	/**
	 * Constructs a new {@code FolderController}.
	 *
	 * @param folderService       the folder service
	 * @param mediaFileRepository the media file repository
	 * @param thumbnailService    the thumbnail service
	 */
	public FolderController(FolderService folderService, MediaFileRepository mediaFileRepository,
							ThumbnailService thumbnailService) {
		this.folderService = folderService;
		this.mediaFileRepository = mediaFileRepository;
		this.thumbnailService = thumbnailService;
	}

	/**
	 * Returns all scanned folder records.
	 *
	 * @return list of all {@link Folder} entries
	 */
	@GetMapping
	public ResponseEntity<List<Folder>> getAllFolders() {
		return ResponseEntity.ok(folderService.getAllFolders());
	}

	/**
	 * Returns all folders at the specified nesting level.
	 * Level 1 contains direct children of the library root.
	 *
	 * @param level the nesting level to filter by (must be >= 1)
	 * @return list of {@link Folder} records at that level
	 */
	@GetMapping("level/{level}")
	public ResponseEntity<List<Folder>> getFoldersByLevel(@PathVariable int level) {
		return ResponseEntity.ok(folderService.getFoldersByLevel(level));
	}

	/**
	 * Returns the direct child folders of the specified folder.
	 *
	 * @param id the primary key of the parent folder
	 * @return list of direct child {@link Folder} records
	 * @throws NotFoundException if no folder with the given id exists
	 */
	@GetMapping("{id}/children")
	public ResponseEntity<List<Folder>> getChildFolders(@PathVariable Long id) {
		Folder parent = folderService.getFolder(id)
				.orElseThrow(() -> new NotFoundException("Folder not found: " + id));
		return ResponseEntity.ok(folderService.getChildFolders(parent));
	}

	/**
	 * Returns a paginated list of image files directly inside the specified folder.
	 *
	 * Results are ordered by the requested {@code sortBy} field and {@code direction}; the field may
	 * be a MediaFile column (e.g. filename) or a PhotoTag column (e.g. rating, dateTaken). Unknown
	 * fields fall back to filename. See {@link eu.apps4net.parrotApp.utilities.PhotoSortResolver}.
	 *
	 * @param id        the primary key of the folder
	 * @param page      zero-based page index (default 0)
	 * @param size      number of records per page (default 50)
	 * @param sortBy    the field to sort by (default filename)
	 * @param direction the sort direction, "asc" or "desc" (default asc)
	 * @return paginated {@link MediaFile} records of kind IMAGE within the folder
	 * @throws NotFoundException if no folder with the given id exists
	 */
	@GetMapping("{id}/photos")
	public ResponseEntity<Page<MediaFile>> getFolderPhotos(
			@PathVariable Long id,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "50") int size,
			@RequestParam(defaultValue = "filename") String sortBy,
			@RequestParam(defaultValue = "asc") String direction) {
		Folder folder = folderService.getFolder(id)
				.orElseThrow(() -> new NotFoundException("Folder not found: " + id));
		PageRequest pageable = PageRequest.of(page, size, PhotoSortResolver.resolve(sortBy, direction));
		return ResponseEntity.ok(mediaFileRepository.findFolderImagesSorted(
				folder.getLibraryFolder(), folder.getPath(), MediaKind.IMAGE, pageable));
	}

	/**
	 * Returns the ancestor-to-target folder chain for the folder that directly
	 * contains the given photo.
	 *
	 * The chain is ordered from the top-level ancestor (a direct child of the library
	 * root) down to the folder holding the photo, mirroring the breadcrumb trail the
	 * photo grid builds when navigating by hand. It lets a caller jump straight to a
	 * photo's folder without walking the hierarchy.
	 *
	 * @param photoId the primary key of the photo whose folder chain is requested
	 * @return ordered list of folders from top-level ancestor to the photo's folder;
	 *         empty when the photo sits directly in the library root
	 * @throws NotFoundException if no photo with the given id exists
	 */
	@GetMapping("by-photo/{photoId}")
	public ResponseEntity<List<Folder>> getFolderChainByPhoto(@PathVariable Long photoId) {
		MediaFile photo = mediaFileRepository.findById(photoId)
				.orElseThrow(() -> new NotFoundException("Photo not found: " + photoId));
		return ResponseEntity.ok(folderService.getFolderChain(photo.getLibraryFolder(), photo.getPath()));
	}

	/**
	 * Returns the ancestor-to-target folder chain for the given folder.
	 *
	 * The chain is ordered from the top-level ancestor (a direct child of the library
	 * root) down to and including the folder itself, mirroring the breadcrumb trail the
	 * photo grid builds when navigating into the folder by hand. It lets the grid rebuild
	 * its breadcrumb when opened directly on a deep folder.
	 *
	 * @param id the primary key of the folder whose chain is requested
	 * @return ordered list of folders from top-level ancestor to the folder itself
	 * @throws NotFoundException if no folder with the given id exists
	 */
	@GetMapping("{id}/chain")
	public ResponseEntity<List<Folder>> getFolderChain(@PathVariable Long id) {
		Folder folder = folderService.getFolder(id)
				.orElseThrow(() -> new NotFoundException("Folder not found: " + id));
		return ResponseEntity.ok(folderService.getFolderChain(folder.getLibraryFolder(), folder.getPath()));
	}

	/**
	 * Generates a thumbnail for the specified folder and returns its id.
	 * If the folder already has a thumbnail, returns the existing id without regenerating.
	 * Returns a 404 if no image files are found in the folder directory tree.
	 *
	 * @param id the primary key of the folder
	 * @return a map with key {@code thumbnailId} set to the thumbnail primary key
	 * @throws NotFoundException        if no folder with the given id exists, or the folder has no images
	 * @throws ProcessingErrorException if thumbnail generation fails
	 */
	@PostMapping("{id}/thumbnail")
	@Transactional
	public Map<String, Long> generateThumbnail(@PathVariable Long id) {
		Folder folder = folderService.getFolder(id)
				.orElseThrow(() -> new NotFoundException("Folder not found: " + id));

		if (folder.getThumbnailId() != null) {
			return Map.of("thumbnailId", folder.getThumbnailId());
		}

		try {
			Long thumbnailId = thumbnailService.generateSingleFolderThumbnail(folder);
			return Map.of("thumbnailId", thumbnailId);
		} catch (IOException e) {
			throw new ProcessingErrorException("Thumbnail generation failed: " + e.getMessage());
		}
	}

	/**
	 * Deletes all folder records from the database.
	 *
	 * @return empty 204 No Content response
	 */
	@DeleteMapping
	public ResponseEntity<Void> clearFolders() {
		folderService.deleteAllFolders();
		return ResponseEntity.noContent().build();
	}
}
