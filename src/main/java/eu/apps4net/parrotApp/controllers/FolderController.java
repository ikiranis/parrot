package eu.apps4net.parrotApp.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import eu.apps4net.parrotApp.exceptions.NotFoundException;
import eu.apps4net.parrotApp.models.Folder;
import eu.apps4net.parrotApp.models.MediaFile;
import eu.apps4net.parrotApp.models.MediaKind;
import eu.apps4net.parrotApp.repositories.MediaFileRepository;
import eu.apps4net.parrotApp.services.FolderService;

import java.util.List;

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

	/**
	 * Constructs a new {@code FolderController}.
	 *
	 * @param folderService       the folder service
	 * @param mediaFileRepository the media file repository
	 */
	public FolderController(FolderService folderService, MediaFileRepository mediaFileRepository) {
		this.folderService = folderService;
		this.mediaFileRepository = mediaFileRepository;
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
	 * Returns all image files directly inside the specified folder.
	 *
	 * @param id the primary key of the folder
	 * @return list of {@link MediaFile} records of kind IMAGE within the folder
	 * @throws NotFoundException if no folder with the given id exists
	 */
	@GetMapping("{id}/photos")
	public ResponseEntity<List<MediaFile>> getFolderPhotos(@PathVariable Long id) {
		Folder folder = folderService.getFolder(id)
				.orElseThrow(() -> new NotFoundException("Folder not found: " + id));
		return ResponseEntity.ok(mediaFileRepository.findByLibraryFolderAndPathAndKind(
				folder.getLibraryFolder(), folder.getPath(), MediaKind.IMAGE));
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
