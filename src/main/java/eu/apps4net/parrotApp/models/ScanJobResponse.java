package eu.apps4net.parrotApp.models;

import java.time.Instant;
import java.util.UUID;

/**
 * Immutable REST response describing the current state of a library scan job.
 * Built from a live {@link ScanJobState} or as a static idle placeholder when
 * no scan has been started.
 *
 * @param jobId          unique job identifier; {@code null} when no scan has run
 * @param status         current lifecycle phase
 * @param startedAt      wall-clock time when the scan started; {@code null} for idle
 * @param completedAt    wall-clock time when the scan finished; {@code null} if running
 * @param added          number of media files added so far
 * @param skipped        number of files already indexed and skipped
 * @param errors         number of files or folders that produced an error
 * @param foldersScanned number of leaf directories fully scanned
 * @param foldersSkipped number of leaf directories skipped as unchanged
 * @param tagged         number of files whose tags have been read in Phase 3
 * @param message        human-readable status message
 */
public record ScanJobResponse(
		UUID jobId,
		ScanStatus status,
		Instant startedAt,
		Instant completedAt,
		int added,
		int skipped,
		int errors,
		int foldersScanned,
		int foldersSkipped,
		int tagged,
		String message
) {

	/**
	 * Creates an idle placeholder response for use when no scan has been run.
	 *
	 * @return an idle {@link ScanJobResponse} with zero counters
	 */
	public static ScanJobResponse idle() {
		return new ScanJobResponse(null, ScanStatus.IDLE, null, null,
				0, 0, 0, 0, 0, 0, "No scan has been run yet");
	}

	/**
	 * Builds a response snapshot from a live {@link ScanJobState}.
	 *
	 * @param state the running or completed job state
	 * @return a response reflecting the current counters and status
	 */
	public static ScanJobResponse from(ScanJobState state) {
		return new ScanJobResponse(
				state.getJobId(),
				state.getStatus(),
				state.getStartedAt(),
				state.getCompletedAt(),
				state.getAdded(),
				state.getSkipped(),
				state.getErrors(),
				state.getFoldersScanned(),
				state.getFoldersSkipped(),
				state.getTagged(),
				state.getMessage()
		);
	}
}
