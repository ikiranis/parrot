package eu.apps4net.parrotApp.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import eu.apps4net.parrotApp.models.LibraryFolder;

import java.util.Optional;

/**
 * Spring Data JPA repository for {@link LibraryFolder} entities.
 */
@Repository
public interface LibraryFolderRepository extends JpaRepository<LibraryFolder, Long> {

	/**
	 * Finds a library folder by its full absolute path.
	 *
	 * @param path the full path of the folder
	 * @return an {@link Optional} containing the matching {@link LibraryFolder}, or empty if not found
	 */
	Optional<LibraryFolder> findByPath(String path);
}
