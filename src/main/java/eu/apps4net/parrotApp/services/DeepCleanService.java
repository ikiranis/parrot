package eu.apps4net.parrotApp.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import eu.apps4net.parrotApp.repositories.FolderRepository;
import eu.apps4net.parrotApp.repositories.LibraryFolderRepository;
import eu.apps4net.parrotApp.repositories.MediaFileRepository;
import eu.apps4net.parrotApp.repositories.PhotoTagRepository;
import eu.apps4net.parrotApp.repositories.ThumbnailRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.stream.Stream;

/**
 * Service that performs a full reset of the library database and removes all generated thumbnails.
 *
 * All scanned media records, folder records, library folder configuration, and thumbnail metadata
 * are removed from the database. The thumbnails directory is then deleted from disk.
 */
@Service
public class DeepCleanService {

	/** Root directory name where generated thumbnail files are stored. */
	private static final String THUMBNAILS_ROOT = "thumbnails";

	/** Repository for photo tag records linked to media files. */
	private final PhotoTagRepository photoTagRepository;

	/** Repository for querying and persisting media file records. */
	private final MediaFileRepository mediaFileRepository;

	/** Repository for scanned folder records. */
	private final FolderRepository folderRepository;

	/** Repository for configured library root folder records. */
	private final LibraryFolderRepository libraryFolderRepository;

	/** Repository for thumbnail metadata records. */
	private final ThumbnailRepository thumbnailRepository;

	/**
	 * Constructs a new {@code DeepCleanService}.
	 *
	 * @param photoTagRepository      the photo tag repository
	 * @param mediaFileRepository     the media file repository
	 * @param folderRepository        the folder repository
	 * @param libraryFolderRepository the library folder repository
	 * @param thumbnailRepository     the thumbnail repository
	 */
	public DeepCleanService(PhotoTagRepository photoTagRepository,
							MediaFileRepository mediaFileRepository,
							FolderRepository folderRepository,
							LibraryFolderRepository libraryFolderRepository,
							ThumbnailRepository thumbnailRepository) {
		this.photoTagRepository = photoTagRepository;
		this.mediaFileRepository = mediaFileRepository;
		this.folderRepository = folderRepository;
		this.libraryFolderRepository = libraryFolderRepository;
		this.thumbnailRepository = thumbnailRepository;
	}

	/**
	 * Deletes all library data from the database and removes the thumbnails directory from disk.
	 *
	 * Deletion order respects foreign-key dependencies:
	 * - 1. PhotoTag records (FK to MediaFile)
	 * - 2. MediaFile records (FK to LibraryFolder; cascade removes linked Thumbnail records)
	 * - 3. Folder records (FK to LibraryFolder; cascade removes linked Thumbnail records)
	 * - 4. LibraryFolder records
	 * - 5. Any remaining Thumbnail records
	 * - 6. Thumbnails directory on disk (best-effort)
	 */
	@Transactional
	public void deepClean() {
		photoTagRepository.deleteAll();
		mediaFileRepository.deleteAll();
		folderRepository.deleteAll();
		libraryFolderRepository.deleteAll();
		thumbnailRepository.deleteAll();

		deleteDirectoryQuietly(Paths.get(THUMBNAILS_ROOT));
	}

	/**
	 * Deletes a directory and all its contents recursively, ignoring any errors.
	 *
	 * @param path the root directory to delete
	 */
	private void deleteDirectoryQuietly(Path path) {
		if (!Files.exists(path)) return;
		try (Stream<Path> walk = Files.walk(path)) {
			walk.sorted(Comparator.reverseOrder())
				.forEach(p -> {
					try {
						Files.delete(p);
					} catch (IOException e) {
						System.err.println("DeepCleanService: could not delete " + p + " — " + e.getMessage());
					}
				});
		} catch (IOException e) {
			System.err.println("DeepCleanService: could not walk thumbnails directory — " + e.getMessage());
		}
	}
}
