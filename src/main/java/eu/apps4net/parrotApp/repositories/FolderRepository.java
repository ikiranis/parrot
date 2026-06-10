package eu.apps4net.parrotApp.repositories;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import eu.apps4net.parrotApp.models.Folder;
import eu.apps4net.parrotApp.models.LibraryFolder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link Folder} entities.
 */
@Repository
public interface FolderRepository extends JpaRepository<Folder, Long> {

	/**
	 * Finds a folder by its library folder and relative path.
	 *
	 * @param libraryFolder the library folder the folder belongs to
	 * @param path          the path relative to the library folder root
	 * @return an {@link Optional} containing the matching {@link Folder}, or empty if not found
	 */
	Optional<Folder> findByLibraryFolderAndPath(LibraryFolder libraryFolder, String path);

	/**
	 * Returns all folders whose level is greater than the given value, sorted by level ascending.
	 *
	 * @param level the exclusive lower bound for the level filter
	 * @return list of matching {@link Folder} records ordered by level
	 */
	List<Folder> findByLevelGreaterThanOrderByLevelAsc(int level);

	/**
	 * Returns up to {@code pageable.getPageSize()} {@link Folder} records that have no thumbnail
	 * assigned yet, ordered by nesting level ascending so that shallow folders (level 1, then 2,
	 * etc.) are processed before deeper ones.
	 *
	 * @param pageable controls the result-set size; page index must be 0
	 * @return {@link Folder} records without a thumbnail, shallowest first
	 */
	@Query(value = "SELECT * FROM folder WHERE thumbnail_id IS NULL ORDER BY level ASC", nativeQuery = true)
	List<Folder> findFoldersWithoutThumbnailOrderedByLevel(Pageable pageable);

	/**
	 * Returns up to {@code pageable.getPageSize()} folders whose thumbnail was generated
	 * before {@code cutoff}, ordered by nesting level ascending so that shallow folders are
	 * refreshed before deeper ones.
	 * The thumbnail entity is eagerly joined so it is accessible without a live session.
	 *
	 * @param cutoff  folders with a thumbnail whose {@code dateUpdate} is before this instant qualify
	 * @param pageable controls the result-set size; page index must be 0
	 * @return folders with a stale thumbnail, shallowest first
	 */
	@Query("SELECT f FROM Folder f JOIN FETCH f.thumbnail t WHERE t.dateUpdate < :cutoff ORDER BY f.level ASC")
	List<Folder> findFoldersWithOldThumbnailsOrderedByLevel(@Param("cutoff") LocalDateTime cutoff, Pageable pageable);

	/**
	 * Returns all folders at the given nesting level.
	 *
	 * @param level the exact level to filter by
	 * @return list of {@link Folder} records at that level, unordered
	 */
	List<Folder> findByLevel(int level);

	/**
	 * Returns all direct child folders of the given parent.
	 * Children are identified by belonging to the same library folder, being exactly one
	 * level deeper than the parent, and having paths that start with the parent path prefix.
	 *
	 * @param libraryFolder the library folder the children belong to
	 * @param childLevel    the expected level of child folders (parent.level + 1)
	 * @param pathPrefix    the path prefix used to match children ({@code parent.path + "/"})
	 * @return list of direct child {@link Folder} records
	 */
	List<Folder> findByLibraryFolderAndLevelAndPathStartingWith(
			LibraryFolder libraryFolder, int childLevel, String pathPrefix);
}
