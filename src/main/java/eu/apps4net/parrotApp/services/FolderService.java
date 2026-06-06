package eu.apps4net.parrotApp.services;

import org.springframework.stereotype.Service;

import eu.apps4net.parrotApp.models.Folder;
import eu.apps4net.parrotApp.repositories.FolderRepository;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

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

	/**
	 * Deletes all folder records from the database.
	 */
	public void deleteAllFolders() {
		folderRepository.deleteAll();
	}

	/**
	 * Checks whether the given leaf directory has changed since it was last scanned,
	 * then creates or updates its {@link Folder} record accordingly.
	 *
	 * The hash is derived from the count and total byte-size of direct regular files
	 * in the directory (formatted as {@code "count:totalBytes"}).  If no record exists
	 * for the path, one is created and {@code true} is returned.  If a record exists
	 * with an identical hash, no write is performed and {@code false} is returned.
	 * Otherwise the hash and {@code lastUpdate} fields are updated and {@code true}
	 * is returned.
	 *
	 * The folder level is computed as the number of path components between {@code root}
	 * and {@code dirPath}: the root itself is level 0, its direct children are level 1,
	 * and so on.
	 *
	 * @param dirPath the leaf directory to inspect; must be an existing directory
	 * @param root    the library root used to compute the nesting level
	 * @return {@code true} if the directory is new or its content changed and its files
	 *         should be re-scanned; {@code false} if the directory is unchanged
	 * @throws IOException if the directory cannot be listed or a file size cannot be read
	 */
	public boolean checkAndSaveFolder(Path dirPath, Path root) throws IOException {
		List<Path> files = new ArrayList<>();
		try (Stream<Path> listing = Files.list(dirPath)) {
			listing.filter(Files::isRegularFile).forEach(files::add);
		}

		long totalSize = 0L;
		for (Path file : files) {
			totalSize += Files.size(file);
		}

		String newHash = sha256(files.size() + ":" + totalSize);
		String path = dirPath.toString();
		int level = (int) root.relativize(dirPath).getNameCount();

		Optional<Folder> existing = folderRepository.findByPath(path);
		if (existing.isPresent()) {
			Folder folder = existing.get();
			if (newHash.equals(folder.getHash()) && folder.isFinished()) {
				return false;
			}
			folder.setHash(newHash);
			folder.setLevel(level);
			folder.setFinished(false);
			folder.setLastUpdate(LocalDateTime.now());
			folderRepository.save(folder);
			return true;
		}

		folderRepository.save(new Folder(path, newHash, level, LocalDateTime.now()));
		return true;
	}

	/**
	 * Marks the folder at the given path as fully indexed by setting its
	 * {@code finished} flag to {@code true}.
	 * Has no effect if no folder record exists for that path.
	 *
	 * @param path the full absolute path of the folder to mark as finished
	 */
	public void markFinished(String path) {
		folderRepository.findByPath(path).ifPresent(folder -> {
			folder.setFinished(true);
			folderRepository.save(folder);
		});
	}

	/**
	 * Returns the SHA-256 hex digest of the given input string.
	 *
	 * @param input the string to hash
	 * @return lowercase hex-encoded SHA-256 digest
	 */
	private String sha256(String input) {
		try {
			byte[] digest = MessageDigest.getInstance("SHA-256")
					.digest(input.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(digest);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256 not available", e);
		}
	}
}
