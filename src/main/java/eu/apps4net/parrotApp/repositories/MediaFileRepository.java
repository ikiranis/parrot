package eu.apps4net.parrotApp.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import eu.apps4net.parrotApp.models.LibraryFolder;
import eu.apps4net.parrotApp.models.MediaFile;
import eu.apps4net.parrotApp.models.MediaKind;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link MediaFile} entities.
 */
@Repository
public interface MediaFileRepository extends JpaRepository<MediaFile, Long> {

	/**
	 * Finds a media file by its library folder, relative directory path, and filename.
	 *
	 * @param libraryFolder the library folder the file belongs to
	 * @param path          the directory path relative to the library folder root
	 * @param filename      the file name
	 * @return an {@link Optional} containing the matching file, or empty if not found
	 */
	Optional<MediaFile> findByLibraryFolderAndPathAndFilename(LibraryFolder libraryFolder, String path, String filename);

	/**
	 * Returns all media files in the given library folder and relative directory path.
	 *
	 * @param libraryFolder the library folder the files belong to
	 * @param path          the directory path relative to the library folder root
	 * @return list of {@link MediaFile} records for that directory
	 */
	List<MediaFile> findByLibraryFolderAndPath(LibraryFolder libraryFolder, String path);

	/**
	 * Returns all media files of the given kind in the given library folder and relative directory path.
	 *
	 * @param libraryFolder the library folder the files belong to
	 * @param path          the directory path relative to the library folder root
	 * @param kind          the media kind to filter by
	 * @return list of matching {@link MediaFile} records
	 */
	List<MediaFile> findByLibraryFolderAndPathAndKind(LibraryFolder libraryFolder, String path, MediaKind kind);

	/**
	 * Returns only the filenames of media files in the given library folder and relative directory path.
	 * Prefer this over {@link #findByLibraryFolderAndPath} when only filenames are needed,
	 * to avoid loading full entities into the persistence context.
	 *
	 * @param libraryFolder the library folder the files belong to
	 * @param path          the directory path relative to the library folder root
	 * @return list of filenames in that directory
	 */
	@Query("SELECT mf.filename FROM MediaFile mf WHERE mf.libraryFolder = ?1 AND mf.path = ?2")
	List<String> findFilenamesByLibraryFolderAndPath(LibraryFolder libraryFolder, String path);

	/**
	 * Returns the media files in the given directory whose filename is in the provided set.
	 * Prefer this over {@link #findByLibraryFolderAndPath} when only specific filenames are needed,
	 * to avoid loading the entire directory into the persistence context.
	 *
	 * @param lf        the library folder the files belong to
	 * @param path      the directory path relative to the library folder root
	 * @param filenames the filenames to look up
	 * @return list of matching {@link MediaFile} records
	 */
	@Query("SELECT mf FROM MediaFile mf WHERE mf.libraryFolder = :lf AND mf.path = :path AND mf.filename IN :filenames")
	List<MediaFile> findByLibraryFolderAndPathAndFilenameIn(
			@org.springframework.data.repository.query.Param("lf") LibraryFolder lf,
			@org.springframework.data.repository.query.Param("path") String path,
			@org.springframework.data.repository.query.Param("filenames") java.util.Collection<String> filenames);

	/**
	 * Returns a paginated list of media files of the given kind.
	 *
	 * @param kind     the media kind to filter by
	 * @param pageable pagination and sorting parameters
	 * @return a {@link Page} of matching {@link MediaFile} records
	 */
	Page<MediaFile> findByKind(MediaKind kind, Pageable pageable);

	/**
	 * Counts the media files of the given kind.
	 *
	 * Backs the random-photo selection: a cheap aggregate count lets the caller
	 * pick a random page to fetch, avoiding a full-table {@code ORDER BY RANDOM()}
	 * scan-and-sort on every request.
	 *
	 * @param kind the media kind to count
	 * @return the total number of records of that kind
	 */
	long countByKind(MediaKind kind);

	/**
	 * Deletes all media files of the given kind.
	 *
	 * @param kind the media kind to delete
	 */
	void deleteAllByKind(MediaKind kind);

	/**
	 * Returns a paginated batch of media files of the given kind that have no
	 * corresponding {@link eu.apps4net.parrotApp.models.PhotoTag} record.
	 * Always pass {@code PageRequest.of(0, batchSize)} so that already-tagged
	 * records fall out of the result set as the repair phase progresses.
	 *
	 * @param kind     the media kind to filter by
	 * @param pageable pagination parameters (use page 0 each call)
	 * @return a page of untagged {@link MediaFile} records
	 */
	@Query("SELECT mf FROM MediaFile mf WHERE mf.kind = ?1 AND NOT EXISTS (SELECT pt FROM PhotoTag pt WHERE pt.mediaFile = mf)")
	Page<MediaFile> findByKindWithoutPhotoTag(MediaKind kind, Pageable pageable);

	/**
	 * Counts media files of the given kind that have no corresponding
	 * {@link eu.apps4net.parrotApp.models.PhotoTag} record.
	 *
	 * @param kind the media kind to filter by
	 * @return total count of untagged records for that kind
	 */
	@Query("SELECT COUNT(mf) FROM MediaFile mf WHERE mf.kind = ?1 AND NOT EXISTS (SELECT pt FROM PhotoTag pt WHERE pt.mediaFile = mf)")
	long countByKindWithoutPhotoTag(MediaKind kind);
}
