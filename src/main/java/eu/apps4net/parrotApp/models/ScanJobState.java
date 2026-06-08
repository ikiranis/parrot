package eu.apps4net.parrotApp.models;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Mutable, thread-safe container for a running background library scan job.
 * Written by {@link eu.apps4net.parrotApp.services.MediaScanService} during scanning
 * and read by {@link eu.apps4net.parrotApp.services.ScanJobService} to serve status responses.
 * Each background scan creates a fresh instance; counters are updated atomically as
 * files and folders are processed so that any concurrent status poll reflects real-time progress.
 */
public class ScanJobState {

	/** Unique identifier assigned at creation time. */
	private final UUID jobId = UUID.randomUUID();

	/** Current lifecycle phase; volatile so status changes are immediately visible. */
	private volatile ScanStatus status = ScanStatus.RUNNING;

	/** Active scanning phase; null until the scan service sets it. */
	private volatile ScanPhase phase = null;

	/** Wall-clock time when this job was created. */
	private final Instant startedAt = Instant.now();

	/** Wall-clock time when the scan finished; null while still running. */
	private volatile Instant completedAt;

	/** Number of new media files added to the database so far. */
	private final AtomicInteger added = new AtomicInteger();

	/** Number of files skipped because they were already indexed. */
	private final AtomicInteger skipped = new AtomicInteger();

	/** Number of files or folders that produced an error. */
	private final AtomicInteger errors = new AtomicInteger();

	/** Number of leaf directories that had changes and were fully scanned. */
	private final AtomicInteger foldersScanned = new AtomicInteger();

	/** Number of leaf directories skipped because their hash was unchanged. */
	private final AtomicInteger foldersSkipped = new AtomicInteger();

	/** Number of media files whose tags have been read and persisted in Phase 3. */
	private final AtomicInteger tagged = new AtomicInteger();

	/** Total leaf directories discovered in Phase 1; set once collecting finishes. */
	private final AtomicInteger totalFolders = new AtomicInteger();

	/** Total new files to tag discovered in Phase 2; set once scanning finishes. */
	private final AtomicInteger totalFiles = new AtomicInteger();

	/** Total media files found in the filesystem across all leaf directories; set after Phase 1. */
	private final AtomicInteger totalMediaFilesInLibrary = new AtomicInteger();

	/** Number of media files already in the database before this scan started. */
	private volatile int initialFilesCount = 0;

	/** Ordered list of error messages collected during the scan; thread-safe. */
	private final List<String> errorLogs = Collections.synchronizedList(new ArrayList<>());

	/** Human-readable status message updated on completion or failure. */
	private volatile String message = "Scan in progress...";

	/**
	 * @return the unique job identifier
	 */
	public UUID getJobId() {
		return jobId;
	}

	/**
	 * @return the current lifecycle phase
	 */
	public ScanStatus getStatus() {
		return status;
	}

	/**
	 * @return the active scanning phase, or {@code null} before the scan service sets it
	 */
	public ScanPhase getPhase() {
		return phase;
	}

	/**
	 * Sets the active scanning phase.
	 *
	 * @param phase the new phase
	 */
	public void setPhase(ScanPhase phase) {
		this.phase = phase;
	}

	/**
	 * @return wall-clock time when the scan started
	 */
	public Instant getStartedAt() {
		return startedAt;
	}

	/**
	 * @return wall-clock time when the scan finished, or {@code null} if still running
	 */
	public Instant getCompletedAt() {
		return completedAt;
	}

	/**
	 * @return number of media files added so far
	 */
	public int getAdded() {
		return added.get();
	}

	/**
	 * @return number of files skipped so far
	 */
	public int getSkipped() {
		return skipped.get();
	}

	/**
	 * @return number of errors encountered so far
	 */
	public int getErrors() {
		return errors.get();
	}

	/**
	 * @return number of folders fully scanned so far
	 */
	public int getFoldersScanned() {
		return foldersScanned.get();
	}

	/**
	 * @return number of folders skipped so far
	 */
	public int getFoldersSkipped() {
		return foldersSkipped.get();
	}

	/**
	 * @return number of files that have been through the tag scanner so far
	 */
	public int getTagged() {
		return tagged.get();
	}

	/**
	 * @return total leaf directories discovered in Phase 1
	 */
	public int getTotalFolders() {
		return totalFolders.get();
	}

	/**
	 * Sets the total number of leaf directories discovered after Phase 1 completes.
	 *
	 * @param n total leaf directory count
	 */
	public void setTotalFolders(int n) {
		totalFolders.set(n);
	}

	/**
	 * @return total new files to tag discovered in Phase 2
	 */
	public int getTotalFiles() {
		return totalFiles.get();
	}

	/**
	 * Sets the total number of new files to tag after Phase 2 completes.
	 *
	 * @param n total new file count
	 */
	public void setTotalFiles(int n) {
		totalFiles.set(n);
	}

	/**
	 * @return number of media files that were already in the database before this scan started
	 */
	public int getInitialFilesCount() {
		return initialFilesCount;
	}

	/**
	 * @return total media files found in the filesystem across all leaf directories
	 */
	public int getTotalMediaFilesInLibrary() {
		return totalMediaFilesInLibrary.get();
	}

	/**
	 * Sets the total number of media files found in the filesystem after Phase 1 completes.
	 * Used as the denominator for the Files progress card.
	 *
	 * @param n total media file count on disk
	 */
	public void setTotalMediaFilesInLibrary(int n) {
		totalMediaFilesInLibrary.set(n);
	}

	/**
	 * Records the number of media files already in the database before scanning begins.
	 * Called once at the start of each background scan to provide context for re-scans.
	 *
	 * @param n pre-scan file count
	 */
	public void setInitialFilesCount(int n) {
		this.initialFilesCount = n;
	}

	/**
	 * @return unmodifiable snapshot of all error log messages
	 */
	public List<String> getErrorLogs() {
		return Collections.unmodifiableList(errorLogs);
	}

	/**
	 * Appends an error message to the log.
	 *
	 * @param msg human-readable description of the error
	 */
	public void addErrorLog(String msg) {
		errorLogs.add(msg);
	}

	/**
	 * Computes an estimated progress percentage based on the current phase and counters.
	 * Returns 100 when the scan completes successfully, and tracks partial progress
	 * when the scan is running or has failed mid-way.
	 *
	 * - COLLECTING: 5% (indeterminate; total folders not yet known)
	 * - SCANNING:   10-55% proportional to folders processed / totalFolders
	 * - TAGGING:    55-99% proportional to files tagged / totalFiles
	 * - COMPLETED:  100%
	 *
	 * @return progress estimate in the range [0, 100]
	 */
	public int getProgressPercent() {
		if (status == ScanStatus.COMPLETED) return 100;
		if (phase == null) return 0;
		return switch (phase) {
			case COLLECTING -> 5;
			case SCANNING -> {
				int total = totalFolders.get();
				if (total == 0) yield 10;
				int done = foldersScanned.get() + foldersSkipped.get();
				yield 10 + (int) (done * 45.0 / total);
			}
			case TAGGING -> {
				int total = totalFiles.get();
				if (total == 0) yield 60;
				int done = tagged.get();
				yield 55 + (int) (done * 44.0 / total);
			}
		};
	}

	/**
	 * @return current human-readable status message
	 */
	public String getMessage() {
		return message;
	}

	/** Increments the added-files counter by one. */
	public void incrementAdded() {
		added.incrementAndGet();
	}

	/** Increments the skipped-files counter by one. */
	public void incrementSkipped() {
		skipped.incrementAndGet();
	}

	/**
	 * Adds {@code n} to the skipped-files counter atomically.
	 *
	 * @param n number of skipped files to add
	 */
	public void addSkipped(int n) {
		skipped.addAndGet(n);
	}

	/** Increments the error counter by one. */
	public void incrementErrors() {
		errors.incrementAndGet();
	}

	/** Increments the scanned-folders counter by one. */
	public void incrementFoldersScanned() {
		foldersScanned.incrementAndGet();
	}

	/** Increments the skipped-folders counter by one. */
	public void incrementFoldersSkipped() {
		foldersSkipped.incrementAndGet();
	}

	/** Increments the tagged-files counter by one. */
	public void incrementTagged() {
		tagged.incrementAndGet();
	}

	/**
	 * Marks the scan as successfully completed with a summary message.
	 *
	 * @param msg human-readable completion summary
	 */
	public void complete(String msg) {
		this.message = msg;
		this.completedAt = Instant.now();
		this.status = ScanStatus.COMPLETED;
	}

	/**
	 * Marks the scan as failed with a description of the error.
	 *
	 * @param msg human-readable error description
	 */
	public void fail(String msg) {
		this.message = msg;
		this.completedAt = Instant.now();
		this.status = ScanStatus.FAILED;
	}
}
