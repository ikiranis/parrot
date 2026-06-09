package eu.apps4net.parrotApp.models;

/**
 * Indicates whether a {@link Thumbnail} belongs to a folder or a media file.
 */
public enum ThumbnailType {

	/** The thumbnail represents a {@link Folder}. */
	FOLDER,

	/** The thumbnail represents a {@link MediaFile}. */
	FILE
}
