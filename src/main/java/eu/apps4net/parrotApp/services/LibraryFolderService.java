package eu.apps4net.parrotApp.services;

import org.springframework.stereotype.Service;

import eu.apps4net.parrotApp.exceptions.NotFoundException;
import eu.apps4net.parrotApp.models.LibraryFolder;
import eu.apps4net.parrotApp.repositories.LibraryFolderRepository;

import java.util.List;

/**
 * Service layer for {@link LibraryFolder} entities.
 * Provides CRUD operations for folders configured as media library scan sources.
 */
@Service
public class LibraryFolderService {

	/** Repository used to persist and retrieve library folder records. */
	private final LibraryFolderRepository libraryFolderRepository;

	/**
	 * Constructs a new {@code LibraryFolderService}.
	 *
	 * @param libraryFolderRepository the library folder repository
	 */
	public LibraryFolderService(LibraryFolderRepository libraryFolderRepository) {
		this.libraryFolderRepository = libraryFolderRepository;
	}

	/**
	 * Returns all configured library folders.
	 *
	 * @return list of all {@link LibraryFolder} records
	 */
	public List<LibraryFolder> getAll() {
		return libraryFolderRepository.findAll();
	}

	/**
	 * Finds a library folder by its primary key.
	 *
	 * @param id the folder identifier
	 * @return the {@link LibraryFolder}
	 * @throws NotFoundException if no folder exists with the given id
	 */
	public LibraryFolder getById(Long id) {
		return libraryFolderRepository.findById(id)
				.orElseThrow(() -> new NotFoundException("Library folder not found: " + id));
	}

	/**
	 * Persists a new or updated library folder record.
	 *
	 * @param libraryFolder the folder to save
	 * @return the saved {@link LibraryFolder} with any generated fields populated
	 */
	public LibraryFolder save(LibraryFolder libraryFolder) {
		return libraryFolderRepository.save(libraryFolder);
	}

	/**
	 * Deletes the library folder with the given primary key.
	 *
	 * @param id the identifier of the folder to delete
	 * @throws NotFoundException if no folder exists with the given id
	 */
	public void delete(Long id) {
		if (!libraryFolderRepository.existsById(id)) {
			throw new NotFoundException("Library folder not found: " + id);
		}
		libraryFolderRepository.deleteById(id);
	}
}
