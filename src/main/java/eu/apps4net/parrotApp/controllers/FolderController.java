package eu.apps4net.parrotApp.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import eu.apps4net.parrotApp.models.Folder;
import eu.apps4net.parrotApp.services.FolderService;

import java.util.List;

/**
 * REST controller for folder operations.
 * Exposes endpoints to retrieve the scanned folder records from the library.
 */
@RestController
@RequestMapping("api/folders")
public class FolderController {

	/** Service for folder data access. */
	private final FolderService folderService;

	/**
	 * Constructs a new {@code FolderController}.
	 *
	 * @param folderService the folder service
	 */
	public FolderController(FolderService folderService) {
		this.folderService = folderService;
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
}
