package eu.apps4net.parrotApp.models;

/** Lifecycle states for a background library scan job. */
public enum ScanStatus {

	/** No scan has been started since the application launched. */
	IDLE,

	/** A scan is currently in progress. */
	RUNNING,

	/** The last scan finished successfully. */
	COMPLETED,

	/** The last scan terminated with an unrecoverable error. */
	FAILED,

	/** The last scan was cancelled by the user before it finished. */
	CANCELLED
}
