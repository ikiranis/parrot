package eu.apps4net.parrotApp.models;

/**
 * Describes the active phase of a running background library scan.
 *
 * - COLLECTING: walking the filesystem to discover all leaf directories.
 * - SCANNING:   inspecting each leaf directory for new media files.
 * - TAGGING:    reading metadata tags from MediaFile records that have none yet.
 */
public enum ScanPhase {
	COLLECTING,
	SCANNING,
	TAGGING
}
