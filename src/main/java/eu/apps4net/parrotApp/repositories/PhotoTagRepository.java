package eu.apps4net.parrotApp.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import eu.apps4net.parrotApp.models.MediaFile;
import eu.apps4net.parrotApp.models.PhotoTag;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link PhotoTag} entities.
 */
@Repository
public interface PhotoTagRepository extends JpaRepository<PhotoTag, Long> {

	/**
	 * Finds the photo tag associated with the given media file.
	 *
	 * @param mediaFile the media file
	 * @return an {@link Optional} containing the matching {@link PhotoTag}, or empty if none
	 */
	Optional<PhotoTag> findByMediaFile(MediaFile mediaFile);

	/**
	 * Deletes all photo tags whose associated media file has the given kind.
	 *
	 * @param kind the media kind to match on the associated {@link MediaFile}
	 */
	void deleteAllByMediaFileKind(eu.apps4net.parrotApp.models.MediaKind kind);

	/**
	 * Returns all photo tags that carry user-assigned data worth exporting:
	 * those with at least one view recorded or a rating set.
	 *
	 * @return list of {@link PhotoTag} records matching the filter
	 */
	@Query("SELECT pt FROM PhotoTag pt WHERE pt.viewCount > 0 OR pt.rating IS NOT NULL")
	List<PhotoTag> findAllWithViewsOrRating();
}
