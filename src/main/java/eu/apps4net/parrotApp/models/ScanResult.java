package eu.apps4net.parrotApp.models;

/**
 * Immutable summary of a folder-scan operation performed by
 * {@link eu.apps4net.parrotApp.services.MediaScanService}.
 *
 * @param added          number of new media files added to the database
 * @param skipped        number of files already present and therefore skipped
 * @param errors         number of files or folders that could not be processed
 * @param foldersScanned number of leaf directories that had changes and were scanned
 * @param foldersSkipped number of leaf directories whose hash was unchanged and were skipped
 * @param message        human-readable summary of the scan outcome
 */
public record ScanResult(
		int added,
		int skipped,
		int errors,
		int foldersScanned,
		int foldersSkipped,
		String message
) {
}
