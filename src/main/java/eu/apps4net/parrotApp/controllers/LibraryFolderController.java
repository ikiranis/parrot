package eu.apps4net.parrotApp.controllers;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import eu.apps4net.parrotApp.models.LibraryFolder;
import eu.apps4net.parrotApp.services.LibraryFolderService;

import java.util.List;

/**
 * REST controller for library folder configuration.
 * Exposes CRUD endpoints to manage the folders scanned for media files.
 */
@RestController
@RequestMapping("api/library-folders")
public class LibraryFolderController {

	/** Service for library folder data access. */
	private final LibraryFolderService libraryFolderService;

	/**
	 * Constructs a new {@code LibraryFolderController}.
	 *
	 * @param libraryFolderService the library folder service
	 */
	public LibraryFolderController(LibraryFolderService libraryFolderService) {
		this.libraryFolderService = libraryFolderService;
	}

	/**
	 * Returns all configured library folders.
	 *
	 * @return list of all {@link LibraryFolder} entries
	 */
	@GetMapping
	public ResponseEntity<List<LibraryFolder>> getAll() {
		return ResponseEntity.ok(libraryFolderService.getAll());
	}

	/**
	 * Returns a single library folder by its primary key.
	 *
	 * @param id the folder identifier
	 * @return the matching {@link LibraryFolder}
	 */
	@GetMapping("{id}")
	public ResponseEntity<LibraryFolder> getById(@PathVariable Long id) {
		return ResponseEntity.ok(libraryFolderService.getById(id));
	}

	/**
	 * Creates a new library folder.
	 *
	 * @param libraryFolder the folder data; {@code path} must be non-blank
	 * @return the persisted {@link LibraryFolder} with 201 Created status
	 */
	@PostMapping
	public ResponseEntity<LibraryFolder> create(@Valid @RequestBody LibraryFolder libraryFolder) {
		libraryFolder.setId(null);
		return ResponseEntity.status(201).body(libraryFolderService.save(libraryFolder));
	}

	/**
	 * Updates an existing library folder.
	 *
	 * @param id            the identifier of the folder to update
	 * @param libraryFolder the updated folder data
	 * @return the updated {@link LibraryFolder}
	 */
	@PutMapping("{id}")
	public ResponseEntity<LibraryFolder> update(@PathVariable Long id,
												@Valid @RequestBody LibraryFolder libraryFolder) {
		LibraryFolder existing = libraryFolderService.getById(id);
		existing.setPath(libraryFolder.getPath());
		return ResponseEntity.ok(libraryFolderService.save(existing));
	}

	/**
	 * Deletes the library folder with the given primary key.
	 *
	 * @param id the identifier of the folder to delete
	 * @return empty 204 No Content response
	 */
	@DeleteMapping("{id}")
	public ResponseEntity<Void> delete(@PathVariable Long id) {
		libraryFolderService.delete(id);
		return ResponseEntity.noContent().build();
	}
}
