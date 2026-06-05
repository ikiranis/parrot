package eu.apps4net.parrotApp.models;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Immutable REST response describing the current state of a library scan job.
 * Built from a live {@link ScanJobState} or as a static idle placeholder when
 * no scan has been started.
 *
 * @param jobId          unique job identifier; null when no scan has run
 * @param status         current lifecycle phase
 * @param phase          active scanning phase (COLLECTING, SCANNING, TAGGING); null when idle
 * @param startedAt      wall-clock time when the scan started; null for idle
 * @param completedAt    wall-clock time when the scan finished; null if running
 * @param added          number of media files added so far
 * @param skipped        number of files already indexed and skipped
 * @param errors         number of files or folders that produced an error
 * @param foldersScanned number of leaf directories fully scanned
 * @param foldersSkipped number of leaf directories skipped as unchanged
 * @param tagged         number of files whose tags have been read in Phase 3
 * @param totalFolders             total leaf directories discovered in Phase 1
 * @param totalFiles               total new files to tag discovered in Phase 2
 * @param totalMediaFilesInLibrary total media files found in the filesystem after Phase 1
 * @param progressPercent          estimated scan progress in the range [0, 100]
 * @param errorLogs                ordered list of error messages collected during the scan
 * @param initialFilesCount        media files already in the database before this scan started
 * @param message                  human-readable status message
 */
public record ScanJobResponse(
		UUID jobId,
		ScanStatus status,
		ScanPhase phase,
		Instant startedAt,
		Instant completedAt,
		int added,
		int skipped,
		int errors,
		int foldersScanned,
		int foldersSkipped,
		int tagged,
		int totalFolders,
		int totalFiles,
		int totalMediaFilesInLibrary,
		int progressPercent,
		List<String> errorLogs,
		int initialFilesCount,
		String message
) {

	/**
	 * Creates an idle placeholder response for use when no scan has been run.
	 *
	 * @return an idle {@link ScanJobResponse} with zero counters
	 */
	public static ScanJobResponse idle() {
		return new ScanJobResponse(null, ScanStatus.IDLE, null, null, null,
				0, 0, 0, 0, 0, 0, 0, 0, 0, 0, List.of(), 0, "No scan has been run yet");
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
				state.getPhase(),
				state.getStartedAt(),
				state.getCompletedAt(),
				state.getAdded(),
				state.getSkipped(),
				state.getErrors(),
				state.getFoldersScanned(),
				state.getFoldersSkipped(),
				state.getTagged(),
				state.getTotalFolders(),
				state.getTotalFiles(),
				state.getTotalMediaFilesInLibrary(),
				state.getProgressPercent(),
				state.getErrorLogs(),
				state.getInitialFilesCount(),
				state.getMessage()
		);
	}
}
