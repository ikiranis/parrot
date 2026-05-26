package eu.apps4net.parrotApp.models;

/**
 * Classifies the type of a {@link MediaFile}.
 */
public enum MediaKind {

	/** A still image (JPEG, PNG, GIF, etc.). */
	IMAGE,

	/** A video file (MP4, MKV, etc.). */
	VIDEO,

	/** An audio file (MP3, FLAC, etc.). */
	AUDIO,

	/** A document file (PDF, DOCX, etc.). */
	DOCUMENT
}
