package eu.apps4net.parrotApp.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import eu.apps4net.parrotApp.models.ScanJobResponse;
import eu.apps4net.parrotApp.services.ScanJobService;

/**
 * REST controller for background library scan operations.
 * Exposes endpoints to start a non-blocking scan and poll its live progress.
 */
@RestController
@RequestMapping("api/scan")
public class ScanController {

	/** Service that manages the background scan job lifecycle. */
	private final ScanJobService scanJobService;

	/**
	 * Constructs a new {@code ScanController}.
	 *
	 * @param scanJobService the background scan job service
	 */
	public ScanController(ScanJobService scanJobService) {
		this.scanJobService = scanJobService;
	}

	/**
	 * Starts a background library scan across all configured library folders.
	 * Returns immediately with 202 Accepted once the job has been queued.
	 * Returns 409 Conflict if a scan is already running.
	 *
	 * @return the initial {@link ScanJobResponse} for the newly started job
	 */
	@PostMapping("start")
	public ResponseEntity<ScanJobResponse> start() {
		return ResponseEntity.accepted().body(scanJobService.startScan());
	}

	/**
	 * Returns the current status of the most recent background scan job.
	 * If no scan has been started since the application launched, returns an idle placeholder.
	 *
	 * @return a {@link ScanJobResponse} with live counter values and status
	 */
	@GetMapping("status")
	public ResponseEntity<ScanJobResponse> status() {
		return ResponseEntity.ok(scanJobService.getStatus());
	}
}
