package eu.apps4net.parrotApp.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
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
}
