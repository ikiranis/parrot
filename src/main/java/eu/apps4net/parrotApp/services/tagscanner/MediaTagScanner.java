package eu.apps4net.parrotApp.services.tagscanner;

import eu.apps4net.parrotApp.models.MediaFile;
import eu.apps4net.parrotApp.models.MediaKind;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Function;

/**
 * Strategy interface for scanning and persisting metadata tags for a specific
 * {@link MediaKind}.
 *
 * Implementations are registered as Spring beans and collected by
 * {@link eu.apps4net.parrotApp.services.MediaScanService}, which dispatches each
 * saved {@link MediaFile} to the implementation whose {@link #getSupportedKind()}
 * matches the file's kind.
 *
 * To support a new media type, create a Spring {@code @Component} that implements
 * this interface and returns the appropriate {@link MediaKind}.
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
	 * @param mediaFile  the already-persisted media file record
	 * @param filePath   the path to the actual file on disk
	 * @param rootPath   the root folder that was originally passed to the scan operation;
	 *                   used to determine whether the file resides directly in the root
	 */
	void scanTags(MediaFile mediaFile, Path filePath, Path rootPath);

	/**
	 * Scans and persists tags for a batch of media files in a single transaction.
	 * Implementations should override this to replace N individual saves with one
	 * {@code saveAll()} call for better throughput.
	 *
	 * The default implementation falls back to calling {@link #scanTags} for each file.
	 *
	 * @param files        the already-persisted media file records to tag
	 * @param rootResolver function that maps each {@link MediaFile} to the library root
	 *                     path used by {@link #scanTags}
	 */
	default void scanTagsBatch(List<MediaFile> files, Function<MediaFile, Path> rootResolver) {
		for (MediaFile mf : files) {
			Path libraryRoot = rootResolver.apply(mf);
			Path filePath = libraryRoot.resolve(mf.getPath()).resolve(mf.getFilename());
			scanTags(mf, filePath, libraryRoot);
		}
	}
}
