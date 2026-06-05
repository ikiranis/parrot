package eu.apps4net.parrotApp.models;

import java.time.Instant;
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

	/** Wall-clock time when this job was created. */
	private final Instant startedAt = Instant.now();

	/** Wall-clock time when the scan finished; {@code null} while still running. */
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
