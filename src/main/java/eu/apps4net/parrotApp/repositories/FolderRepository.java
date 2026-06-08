package eu.apps4net.parrotApp.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import eu.apps4net.parrotApp.models.Folder;
import eu.apps4net.parrotApp.models.LibraryFolder;

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
}
