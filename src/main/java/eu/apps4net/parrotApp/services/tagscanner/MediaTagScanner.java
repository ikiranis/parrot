package eu.apps4net.parrotApp.services.tagscanner;

import eu.apps4net.parrotApp.models.MediaFile;
import eu.apps4net.parrotApp.models.MediaKind;

import java.nio.file.Path;

/**
 * Strategy interface for scanning and persisting metadata tags for a specific
 * {@link MediaKind}.
 *
 * <p>Implementations are registered as Spring beans and collected by
 * {@link eu.apps4net.parrotApp.services.MediaScanService}, which dispatches each
 * saved {@link MediaFile} to the implementation whose {@link #getSupportedKind()}
 * matches the file's kind.</p>
 *
 * <p>To support a new media type, create a Spring {@code @Component} that implements
 * this interface and returns the appropriate {@link MediaKind}.</p>
 */
public interface MediaTagScanner {

	/**
	 * Returns the {@link MediaKind} this scanner handles.
	 *
	 * @return the supported media kind
	 */
	MediaKind getSupportedKind();

	/**
	 * Scans the file at {@code filePath} and persists the media-type-specific
	 * metadata tags for the given {@link MediaFile}.
	 *
	 * @param mediaFile the already-persisted media file record
	 * @param filePath  the path to the actual file on disk
	 */
	void scanTags(MediaFile mediaFile, Path filePath);
}
