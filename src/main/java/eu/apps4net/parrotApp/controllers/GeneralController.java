package eu.apps4net.parrotApp.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import eu.apps4net.parrotApp.services.DeepCleanService;

/**
 * REST controller that exposes general application health and maintenance endpoints.
 */
@RestController
@RequestMapping("api/general")
public class GeneralController {

	/** Service that performs a full reset of library data and thumbnails. */
	private final DeepCleanService deepCleanService;

	/**
	 * Constructs a new {@code GeneralController}.
	 *
	 * @param deepCleanService the deep-clean service
	 */
	public GeneralController(DeepCleanService deepCleanService) {
		this.deepCleanService = deepCleanService;
	}

	/**
	 * Returns {@code true} to signal that the backend is reachable.
	 *
	 * @return {@code true} always
	 */
	@GetMapping(path = "appAlive")
	public boolean getAppAlive() {
		return true;
	}

	/**
	 * Deletes all library data from the database and removes the thumbnails directory from disk.
	 * This operation is irreversible.
	 *
	 * @return 200 OK with no body on success
	 */
	@DeleteMapping("deep-clean")
	public ResponseEntity<Void> deepClean() {
		deepCleanService.deepClean();
		return ResponseEntity.ok().build();
	}
}
