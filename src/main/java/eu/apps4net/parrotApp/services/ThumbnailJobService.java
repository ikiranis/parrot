package eu.apps4net.parrotApp.services;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Scheduled service that generates folder thumbnails at a fixed interval.
 * Every 10 minutes (measured from the end of the previous run) it asks
 * {@link ThumbnailService#generateFolderThumbnails()} to process the next batch of
 * folders that have no thumbnail yet, starting from the shallowest nesting level.
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
	 * Generates folder thumbnails in a fixed-delay cycle of 10 minutes.
	 * If a media library scan is currently running, this execution is skipped entirely
	 * and the next attempt begins 10 minutes after the skip returns.
	 */
	@Scheduled(fixedDelay = 600_000)
	public void generateThumbnails() {
		if (scanJobService.isScanning()) {
			return;
		}
		thumbnailService.generateFolderThumbnails();
	}
}
