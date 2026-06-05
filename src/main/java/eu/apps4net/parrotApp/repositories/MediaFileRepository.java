package eu.apps4net.parrotApp.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import eu.apps4net.parrotApp.models.MediaFile;
import eu.apps4net.parrotApp.models.MediaKind;

import java.util.Optional;

/**
 * Spring Data JPA repository for {@link MediaFile} entities.
 */
@Repository
public interface MediaFileRepository extends JpaRepository<MediaFile, Long> {

	/**
	 * Finds a media file by its directory path and filename.
	 *
	 * @param path     the directory path
	 * @param filename the file name
	 * @return an {@link Optional} containing the matching file, or empty if not found
	 */
	Optional<MediaFile> findByPathAndFilename(String path, String filename);

	/**
	 * Returns a paginated list of media files of the given kind.
	 *
	 * @param kind     the media kind to filter by
	 * @param pageable pagination and sorting parameters
	 * @return a {@link Page} of matching {@link MediaFile} records
	 */
	Page<MediaFile> findByKind(MediaKind kind, Pageable pageable);

	/**
	 * Returns a single randomly selected media file of the given kind.
	 * Uses Derby's RANDOM() to assign a per-row random value and picks the top result.
	 *
	 * @param kind the media kind string (e.g. {@code "IMAGE"})
	 * @return an {@link Optional} containing a random file, or empty if none exist
	 */
	@Query(value = "SELECT * FROM media_file WHERE kind = ?1 ORDER BY RANDOM() FETCH FIRST 1 ROWS ONLY",
		nativeQuery = true)
	Optional<MediaFile> findRandomByKind(String kind);

	/**
	 * Deletes all media files of the given kind.
	 *
	 * @param kind the media kind to delete
	 */
	void deleteAllByKind(MediaKind kind);
}
