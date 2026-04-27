package eu.apps4net.parrotApp.models;

public record ScanResult(
		int added,
		int skipped,
		int errors,
		String message
) {
}
