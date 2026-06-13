package eu.apps4net.parrotApp.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import eu.apps4net.parrotApp.models.Thumbnail;
import eu.apps4net.parrotApp.models.ThumbnailType;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Spring Data JPA repository for {@link Thumbnail} entities.
 */
@Repository
public interface ThumbnailRepository extends JpaRepository<Thumbnail, Long> {

	/**
	 * Returns all thumbnails of the given type.
	 *
	 * @param type the thumbnail type to filter by
	 * @return list of matching {@link Thumbnail} records
	 */
	List<Thumbnail> findByType(ThumbnailType type);

	/**
	 * Returns all thumbnails of the given type whose last-update timestamp is before the given cutoff.
	 *
	 * @param type   the thumbnail type to filter by
	 * @param cutoff records with {@code dateUpdate} strictly before this value are returned
	 * @return list of matching {@link Thumbnail} records
	 */
	List<Thumbnail> findByTypeAndDateUpdateBefore(ThumbnailType type, LocalDateTime cutoff);
}
