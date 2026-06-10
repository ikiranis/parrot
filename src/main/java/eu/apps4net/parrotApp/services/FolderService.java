package eu.apps4net.parrotApp.services;

import org.springframework.stereotype.Service;

import eu.apps4net.parrotApp.models.Folder;
import eu.apps4net.parrotApp.models.LibraryFolder;
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
	 * Returns all persisted folders with level greater than 0, sorted by level ascending.
	 *
	 * @return list of {@link Folder} records excluding root-level folders
	 */
	public List<Folder> getAllFolders() {
		return folderRepository.findByLevelGreaterThanOrderByLevelAsc(0);
	}

	/**
	 * Returns all folders at the specified nesting level.
	 *
	 * @param level the exact nesting level to filter by (1 = direct children of library root)
	 * @return list of {@link Folder} records at that level
	 */
	public List<Folder> getFoldersByLevel(int level) {
		return folderRepository.findByLevel(level);
	}

	/**
	 * Returns the direct child folders of the given parent folder.
	 *
	 * @param parent the parent folder whose children are requested
	 * @return list of direct child {@link Folder} records
	 */
	public List<Folder> getChildFolders(Folder parent) {
		return folderRepository.findByLibraryFolderAndLevelAndPathStartingWith(
				parent.getLibraryFolder(), parent.getLevel() + 1, parent.getPath() + "/");
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
	 * Finds a folder by its library folder and relative path.
	 *
	 * @param libraryFolder the library folder the folder belongs to
	 * @param path          the path relative to the library folder root
	 * @return an {@link Optional} containing the matching {@link Folder}, or empty if not found
	 */
	public Optional<Folder> getFolderByPath(LibraryFolder libraryFolder, String path) {
		return folderRepository.findByLibraryFolderAndPath(libraryFolder, path);
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
	 * Every ancestor directory between {@code root} and {@code dirPath} is also persisted
	 * (with a null hash and finished=true) so that the folder tree is fully represented
	 * in the database even when intermediate directories contain no direct files.
	 *
	 * @param dirPath       the leaf directory to inspect; must be an existing directory
	 * @param root          the library root used to compute the nesting level and relative path
	 * @param libraryFolder the library folder this directory belongs to
	 * @return {@code true} if the directory is new or its content changed and its files
	 *         should be re-scanned; {@code false} if the directory is unchanged
	 * @throws IOException if the directory cannot be listed or a file size cannot be read
	 */
	public boolean checkAndSaveFolder(Path dirPath, Path root, LibraryFolder libraryFolder) throws IOException {
		ensureParentFolders(dirPath, root, libraryFolder);

		List<Path> files = new ArrayList<>();
		try (Stream<Path> listing = Files.list(dirPath)) {
			listing.filter(Files::isRegularFile).forEach(files::add);
		}

		long totalSize = 0L;
		for (Path file : files) {
			totalSize += Files.size(file);
		}

		String newHash = sha256(files.size() + ":" + totalSize);
		String relativePath = root.relativize(dirPath).toString();
		// An empty relative path means dirPath is the library root itself. Java reports a name
		// count of 1 for the empty path, so guard against that and assign the root level 0.
		int level = relativePath.isEmpty() ? 0 : root.relativize(dirPath).getNameCount();

		Optional<Folder> existing = folderRepository.findByLibraryFolderAndPath(libraryFolder, relativePath);
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

		folderRepository.save(new Folder(libraryFolder, relativePath, newHash, level, LocalDateTime.now()));
		return true;
	}

	/**
	 * Ensures every ancestor directory between {@code root} and {@code dirPath} has a
	 * {@link Folder} record.  Ancestors that contain no direct files are saved with a
	 * null hash and {@code finished=true} because they have no files to index.
	 * If two threads race to insert the same ancestor, the second insert is silently
	 * ignored — the unique constraint on {@code (library_folder_id, path)} guarantees
	 * at-most-one record per path.
	 *
	 * @param dirPath       the leaf directory whose ancestors are to be persisted
	 * @param root          the library root; ancestors at depth 0 (the root itself) are not saved
	 * @param libraryFolder the library folder this directory belongs to
	 */
	private void ensureParentFolders(Path dirPath, Path root, LibraryFolder libraryFolder) {
		Path relative = root.relativize(dirPath);
		for (int i = 1; i < relative.getNameCount(); i++) {
			String ancestorPath = relative.subpath(0, i).toString();
			if (folderRepository.findByLibraryFolderAndPath(libraryFolder, ancestorPath).isEmpty()) {
				try {
					Folder parent = new Folder(libraryFolder, ancestorPath, null, i, LocalDateTime.now());
					parent.setFinished(true);
					folderRepository.save(parent);
				} catch (Exception ignored) {
					// concurrent insert from another thread — record already exists
				}
			}
		}
	}

	/**
	 * Marks the folder at the given relative path as fully indexed by setting its
	 * {@code finished} flag to {@code true}.
	 * Has no effect if no folder record exists for that path.
	 *
	 * @param libraryFolder the library folder the folder belongs to
	 * @param relativePath  the path of the folder relative to the library folder root
	 */
	public void markFinished(LibraryFolder libraryFolder, String relativePath) {
		folderRepository.findByLibraryFolderAndPath(libraryFolder, relativePath).ifPresent(folder -> {
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
