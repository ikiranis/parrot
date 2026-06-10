package eu.apps4net.parrotApp.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import eu.apps4net.parrotApp.models.LibraryFolder;
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

	/**
	 * Returns {@code (path, filename, rating)} tuples for all rated media files in the
	 * given folder subtree. Used to build a rating-weighted candidate pool for thumbnail selection.
	 *
	 * @param libraryFolder the library folder to restrict the search to
	 * @param path          exact directory path of the folder itself (matches files directly inside it)
	 * @param pathPrefix    LIKE pattern for subdirectory paths — pass {@code folder.path + "/%"},
	 *                      or {@code "%"} when the folder is the library root (empty path)
	 * @return list of {@code Object[]} rows with elements [path (String), filename (String), rating (Integer)]
	 */
	@Query("SELECT pt.mediaFile.path, pt.mediaFile.filename, pt.rating FROM PhotoTag pt WHERE pt.mediaFile.libraryFolder = :libraryFolder AND (pt.mediaFile.path = :path OR pt.mediaFile.path LIKE :pathPrefix) AND pt.rating IS NOT NULL")
	List<Object[]> findRatingsInFolderSubtree(
			@Param("libraryFolder") LibraryFolder libraryFolder,
			@Param("path") String path,
			@Param("pathPrefix") String pathPrefix);

	/**
	 * Returns all photo tags whose media file is in the given collection.
	 * Used to batch-load existing tags during bulk import, avoiding one query per file.
	 *
	 * @param mediaFiles the media files whose tags should be fetched
	 * @return list of matching {@link PhotoTag} records
	 */
	List<PhotoTag> findAllByMediaFileIn(java.util.Collection<MediaFile> mediaFiles);
}
