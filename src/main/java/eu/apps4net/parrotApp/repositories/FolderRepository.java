package eu.apps4net.parrotApp.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import eu.apps4net.parrotApp.models.Folder;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link Folder} entities.
 */
@Repository
public interface FolderRepository extends JpaRepository<Folder, Long> {

	/**
	 * Finds a folder by its full absolute path.
	 *
	 * @param path the full path of the folder
	 * @return an {@link Optional} containing the matching {@link Folder}, or empty if not found
	 */
	Optional<Folder> findByPath(String path);

	/**
	 * Returns all folders whose level is greater than the given value, sorted by level ascending.
	 *
	 * @param level the exclusive lower bound for the level filter
	 * @return list of matching {@link Folder} records ordered by level
	 */
	List<Folder> findByLevelGreaterThanOrderByLevelAsc(int level);
}
