package eu.apps4net.parrotApp.services;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Scheduled service that regenerates stale folder thumbnails at a fixed interval.
 * Every hour (measured from the end of the previous run) it asks
 * {@link ThumbnailService#generateFolderThumbnails()} to process the next batch of
 * folders whose thumbnail is older than 15 days, starting from the shallowest nesting level.
 * Each run logs the start timestamp and the number of thumbnails regenerated.
 *
 * The task is suppressed while a media library scan is running so that thumbnail
 * I/O does not compete with the scanner. The next scheduled tick after the scan
 * finishes will resume generation automatically.
 */
@Service
public class ThumbnailJobService {

	/** Service that performs the actual thumbnail generation work. */
	private final ThumbnailService thumbnailService;

	/** Service used to detect whether a media library scan is currently in progress. */
	private final ScanJobService scanJobService;

	/**
	 * Constructs a new {@code ThumbnailJobService}.
	 *
	 * @param thumbnailService the service that generates folder thumbnails
	 * @param scanJobService   the service that manages the background scan lifecycle
	 */
	public ThumbnailJobService(ThumbnailService thumbnailService, ScanJobService scanJobService) {
		this.thumbnailService = thumbnailService;
		this.scanJobService = scanJobService;
	}

	/**
	 * Regenerates stale folder thumbnails in a fixed-delay cycle of 1 hour.
	 * A thumbnail is considered stale when it was generated more than 15 days ago.
	 * If a media library scan is currently running, this execution is skipped entirely
	 * and the next attempt begins 1 hour after the skip returns.
	 * Logs the run timestamp and the number of thumbnails regenerated.
	 */
	@Scheduled(fixedDelay = 3_600_000)
	public void generateThumbnails() {
		if (scanJobService.isScanning()) {
			return;
		}
		LocalDateTime start = LocalDateTime.now();
		int count = thumbnailService.generateFolderThumbnails();
		System.out.println("ThumbnailJobService: run at " + start + " — " + count + " folder thumbnail(s) regenerated");
	}
}
