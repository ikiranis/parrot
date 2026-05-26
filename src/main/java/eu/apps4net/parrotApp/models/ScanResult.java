package eu.apps4net.parrotApp.models;

/**
 * Immutable summary of a folder-scan operation performed by
 * {@link eu.apps4net.parrotApp.services.PhotoScanService}.
 *
 * @param added   number of new media files added to the database
 * @param skipped number of files already present and therefore skipped
 * @param errors  number of files that could not be processed
 * @param message human-readable summary of the scan outcome
 */
public record ScanResult(
		int added,
		int skipped,
		int errors,
		String message
) {
}
