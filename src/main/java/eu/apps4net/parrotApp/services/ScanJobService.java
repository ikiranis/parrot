package eu.apps4net.parrotApp.services;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import eu.apps4net.parrotApp.models.ScanJobResponse;
import eu.apps4net.parrotApp.models.ScanJobState;
import eu.apps4net.parrotApp.models.ScanStatus;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Manages the lifecycle of a single background library scan job.
 * At most one scan may run at a time; attempting to start a second scan while
 * one is already running throws a 409 Conflict.
 * The current or most recent job state is retained in memory and served as a
 * {@link ScanJobResponse} via {@link #getStatus()}.
 */
@Service
public class ScanJobService {

	/** The media scan service that performs the actual scan work. */
	private final MediaScanService mediaScanService;

	/** Single-threaded executor that runs background scan jobs one at a time. */
	private final ExecutorService executor = Executors.newSingleThreadExecutor();

	/** Reference to the most recent scan job state; {@code null} if no scan has run. */
	private final AtomicReference<ScanJobState> currentJob = new AtomicReference<>();

	/**
	 * Constructs a new {@code ScanJobService}.
	 *
	 * @param mediaScanService the service used to execute the scan
	 */
	public ScanJobService(MediaScanService mediaScanService) {
		this.mediaScanService = mediaScanService;
	}

	/**
	 * Starts a background library scan if no scan is currently running.
	 * Returns immediately with a {@link ScanJobResponse} describing the newly created job.
	 *
	 * @return response for the newly started job with {@link ScanStatus#RUNNING}
	 * @throws ResponseStatusException with 409 Conflict if a scan is already running
	 */
	public ScanJobResponse startScan() {
		ScanJobState existing = currentJob.get();
		if (existing != null && existing.getStatus() == ScanStatus.RUNNING) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "A scan is already running");
		}

		ScanJobState state = new ScanJobState();
		currentJob.set(state);
		executor.submit(() -> mediaScanService.scanLibraryFolders(state));

		return ScanJobResponse.from(state);
	}

	/**
	 * Requests cancellation of the currently running scan.
	 * The scan stops claiming new work at the next safe point and is then marked as cancelled;
	 * work already completed is retained. Returns the current status either way.
	 *
	 * @return the {@link ScanJobResponse} for the current job
	 * @throws ResponseStatusException with 409 Conflict if no scan is currently running
	 */
	public ScanJobResponse cancelScan() {
		ScanJobState state = currentJob.get();
		if (state == null || state.getStatus() != ScanStatus.RUNNING) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "No scan is currently running");
		}
		state.requestCancel();
		return ScanJobResponse.from(state);
	}

	/**
	 * Returns the current scan job status.
	 * If no scan has been started since the application launched, returns an idle placeholder.
	 *
	 * @return a {@link ScanJobResponse} reflecting the most recent or current scan, never {@code null}
	 */
	public ScanJobResponse getStatus() {
		ScanJobState state = currentJob.get();
		return state == null ? ScanJobResponse.idle() : ScanJobResponse.from(state);
	}

	/**
	 * Returns {@code true} if a scan job is currently running.
	 *
	 * @return {@code true} when the current job's status is {@link ScanStatus#RUNNING}
	 */
	public boolean isScanning() {
		ScanJobState state = currentJob.get();
		return state != null && state.getStatus() == ScanStatus.RUNNING;
	}
}
