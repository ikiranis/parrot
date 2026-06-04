package eu.apps4net.parrotApp.services;

import org.springframework.stereotype.Service;

import eu.apps4net.parrotApp.models.Folder;
import eu.apps4net.parrotApp.repositories.FolderRepository;

import java.util.List;
import java.util.Optional;

/**
 * Service layer for {@link Folder} entities.
 * Provides CRUD operations and look-up helpers for scanned library folders.
 */
@Service
public class FolderService {

	/** Repository used to persist and retrieve folder records. */
	private final FolderRepository folderRepository;

	/**
	 * Constructs a new {@code FolderService}.
	 *
	 * @param folderRepository the folder repository
	 */
	public FolderService(FolderRepository folderRepository) {
		this.folderRepository = folderRepository;
	}

	/**
	 * Returns all persisted folders.
	 *
	 * @return list of all {@link Folder} records
	 */
	public List<Folder> getAllFolders() {
		return folderRepository.findAll();
	}

	/**
	 * Finds a folder by its primary key.
	 *
	 * @param id the folder identifier
	 * @return an {@link Optional} containing the {@link Folder}, or empty if not found
	 */
	public Optional<Folder> getFolder(Long id) {
		return folderRepository.findById(id);
	}

	/**
	 * Finds a folder by its full absolute path.
	 *
	 * @param path the full path of the folder
	 * @return an {@link Optional} containing the matching {@link Folder}, or empty if not found
	 */
	public Optional<Folder> getFolderByPath(String path) {
		return folderRepository.findByPath(path);
	}

	/**
	 * Persists a new or updated folder record.
	 *
	 * @param folder the folder to save
	 * @return the saved {@link Folder} with any generated fields populated
	 */
	public Folder saveFolder(Folder folder) {
		return folderRepository.save(folder);
	}

	/**
	 * Deletes the folder with the given primary key.
	 *
	 * @param id the identifier of the folder to delete
	 */
	public void deleteFolder(Long id) {
		folderRepository.deleteById(id);
	}
}
