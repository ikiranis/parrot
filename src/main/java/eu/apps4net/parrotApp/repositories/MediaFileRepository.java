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
	 * Returns a paginated list of media files of the given kind in the given library folder and relative directory path.
	 *
	 * @param libraryFolder the library folder the files belong to
	 * @param path          the directory path relative to the library folder root
	 * @param kind          the media kind to filter by
	 * @param pageable      pagination and sorting parameters
	 * @return a {@link Page} of matching {@link MediaFile} records
	 */
	Page<MediaFile> findByLibraryFolderAndPathAndKind(LibraryFolder libraryFolder, String path, MediaKind kind, Pageable pageable);

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
	 * Returns the filenames, among the supplied candidates, that already exist in the given
	 * directory of the given library folder.
	 *
	 * Unlike {@link #findFilenamesByLibraryFolderAndPath}, which filters only on the library
	 * folder and path and therefore scans every row of that library folder, this query is
	 * driven by the {@code (library_folder_id, filename)} index: it probes the index once per
	 * candidate filename and applies the path as a residual filter. This keeps the per-directory
	 * existence check proportional to the number of files in the directory rather than to the
	 * size of the whole library, which matters as the table grows during a large scan.
	 *
	 * @param lf        the library folder the files belong to
	 * @param path      the directory path relative to the library folder root
	 * @param filenames the candidate filenames discovered on disk in that directory
	 * @return the subset of {@code filenames} already present in the database for that directory
	 */
	@Query("SELECT mf.filename FROM MediaFile mf WHERE mf.libraryFolder = :lf AND mf.path = :path AND mf.filename IN :filenames")
	List<String> findExistingFilenames(
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
	 * @param kind the media kind to count
	 * @return the total number of records of that kind
	 */
	long countByKind(MediaKind kind);

	/**
	 * Returns the IDs of all media files of the given kind, ordered by id ascending.
	 *
	 * Used for photo selection: loading IDs alone is cheap regardless of library size,
	 * allowing the caller to pick a subset and then fetch only those records, which
	 * avoids both a full-table {@code ORDER BY RANDOM()} scan and the page-clustering
	 * problem of picking a single random page. The deterministic ordering also lets the
	 * caller return photos in sequence when random selection is not requested.
	 *
	 * @param kind the media kind to filter by
	 * @return list of all matching record IDs, ordered by id ascending
	 */
	@Query("SELECT mf.id FROM MediaFile mf WHERE mf.kind = ?1 ORDER BY mf.id ASC")
	List<Long> findIdsByKind(MediaKind kind);

	/**
	 * Returns the IDs of all media files of the given kind located in the given library folder
	 * at or beneath the given relative directory path, ordered by id ascending.
	 *
	 * Scoping is recursive: a file matches when its path equals {@code path} exactly (files
	 * directly in the folder) or begins with {@code path + "/"} (files in any nested subfolder).
	 * This mirrors the id-only selection of {@link #findIdsByKind} but constrained to a single
	 * folder subtree, so the photo-batch endpoint can drive a folder-scoped slideshow without a
	 * full-library scan.
	 *
	 * @param kind          the media kind to filter by
	 * @param libraryFolder the library folder the files belong to
	 * @param path          the directory path, relative to the library folder root, whose subtree to include
	 * @return list of matching record IDs, ordered by id ascending
	 */
	@Query("SELECT mf.id FROM MediaFile mf WHERE mf.kind = ?1 AND mf.libraryFolder = ?2 " +
			"AND (mf.path = ?3 OR mf.path LIKE CONCAT(?3, '/%')) ORDER BY mf.id ASC")
	List<Long> findIdsByKindAndFolderSubtree(MediaKind kind, LibraryFolder libraryFolder, String path);

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
	 * Returns the next batch of untagged media files of the given kind whose id is greater than
	 * {@code afterId}, ordered by ascending id.
	 *
	 * This is the forward-cursor form used by the tag-scan phase: by advancing {@code afterId}
	 * to the highest id of each returned batch, successive calls never re-examine rows that were
	 * already drained, so draining the whole backlog costs O(n) rather than the O(n squared) of
	 * repeatedly fetching page 0 and skipping a growing prefix of already-tagged rows. Because
	 * identity ids are monotonically increasing, rows inserted concurrently by the file-scan
	 * phase are naturally picked up on a later call. Returns a {@code List} (not a {@code Page})
	 * so that no additional count query is issued per batch.
	 *
	 * Backed by the {@code (kind, id)} index, which provides both the {@code kind} filter and the
	 * ascending-id ordering as an index-only forward scan.
	 *
	 * @param kind     the media kind to filter by
	 * @param afterId  exclusive lower bound on the id; pass 0 to start from the beginning
	 * @param pageable batch-size limit (use {@code PageRequest.of(0, batchSize)})
	 * @return up to {@code batchSize} untagged records of that kind with id greater than {@code afterId}
	 */
	@Query("SELECT mf FROM MediaFile mf WHERE mf.kind = ?1 AND mf.id > ?2 " +
			"AND NOT EXISTS (SELECT pt FROM PhotoTag pt WHERE pt.mediaFile = mf) ORDER BY mf.id ASC")
	List<MediaFile> findByKindWithoutPhotoTagAfterId(MediaKind kind, Long afterId, Pageable pageable);

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
