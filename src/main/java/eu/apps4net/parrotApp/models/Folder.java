package eu.apps4net.parrotApp.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * JPA entity representing a scanned folder in the media library.
 * Tracks the folder path relative to its {@link LibraryFolder} root, a content hash
 * derived from file count and total size, the nesting level relative to the library
 * root, a flag indicating whether all files have been fully indexed, and the last
 * time a change was detected.
 */
@Entity(name = "Folder")
@Table(name = "folder",
		uniqueConstraints = @UniqueConstraint(name = "uk_folder_library_path",
				columnNames = {"library_folder_id", "path"}))
public class Folder {

	/** Auto-generated primary key. */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/** Library folder this folder belongs to. */
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "library_folder_id", nullable = false)
	private LibraryFolder libraryFolder;

	/** Path of the folder relative to the library folder root. Empty string means the root itself. */
	@NotNull
	@Size(max = 512)
	@Column(name = "path", nullable = false)
	private String path;

	/**
	 * Content hash computed from the file count and total size of files in the
	 * folder, used to detect changes between scans.
	 */
	@Size(max = 100)
	@Column(name = "hash")
	private String hash;

	/**
	 * Nesting level relative to the library root.
	 * The root folder itself is level 0; each subdirectory adds 1.
	 */
	@Column(name = "level", nullable = false, columnDefinition = "INTEGER DEFAULT 0")
	private int level;

	/**
	 * Whether all files in this folder have been fully indexed.
	 * Remains {@code false} until every file has been processed, allowing
	 * interrupted scans to be resumed safely.
	 */
	@Column(name = "finished", nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
	private boolean finished = false;

	/** Timestamp of the last detected change in this folder. */
	@Column(name = "last_update")
	private LocalDateTime lastUpdate;

	/** Optional thumbnail for this folder. Null when no thumbnail has been generated yet. */
	@OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinColumn(name = "thumbnail_id")
	private Thumbnail thumbnail;

	/** Required no-arg constructor for JPA. */
	public Folder() {
	}

	/**
	 * Constructs a fully-initialised {@code Folder}.
	 * {@code finished} is always {@code false} on construction and must be set
	 * explicitly once all files in the folder have been indexed.
	 *
	 * @param libraryFolder the library folder this folder belongs to
	 * @param path          the path relative to the library folder root; empty string for the root itself
	 * @param hash          the content hash, may be {@code null}
	 * @param level         the nesting level relative to the library root (0 = root)
	 * @param lastUpdate    the timestamp of the last detected change
	 */
	public Folder(LibraryFolder libraryFolder, String path, String hash, int level, LocalDateTime lastUpdate) {
		this.libraryFolder = libraryFolder;
		this.path = path;
		this.hash = hash;
		this.level = level;
		this.lastUpdate = lastUpdate;
	}

	/**
	 * Returns the primary key.
	 *
	 * @return the id
	 */
	public Long getId() {
		return id;
	}

	/**
	 * Sets the primary key.
	 *
	 * @param id the id to set
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Returns the library folder this folder belongs to.
	 *
	 * @return the library folder
	 */
	public LibraryFolder getLibraryFolder() {
		return libraryFolder;
	}

	/**
	 * Sets the library folder this folder belongs to.
	 *
	 * @param libraryFolder the library folder to set
	 */
	public void setLibraryFolder(LibraryFolder libraryFolder) {
		this.libraryFolder = libraryFolder;
	}

	/**
	 * Returns the path relative to the library folder root.
	 * An empty string means this folder is the library root itself.
	 *
	 * @return the relative path
	 */
	public String getPath() {
		return path;
	}

	/**
	 * Sets the path relative to the library folder root.
	 *
	 * @param path the relative path to set
	 */
	public void setPath(String path) {
		this.path = path;
	}

	/**
	 * Returns the content hash.
	 *
	 * @return the hash, or {@code null} if not yet computed
	 */
	public String getHash() {
		return hash;
	}

	/**
	 * Sets the content hash.
	 *
	 * @param hash the hash to set
	 */
	public void setHash(String hash) {
		this.hash = hash;
	}

	/**
	 * Returns the nesting level of the folder relative to the library root.
	 *
	 * @return the level (0 = root)
	 */
	public int getLevel() {
		return level;
	}

	/**
	 * Sets the nesting level of the folder relative to the library root.
	 *
	 * @param level the level to set (0 = root)
	 */
	public void setLevel(int level) {
		this.level = level;
	}

	/**
	 * Returns whether all files in this folder have been fully indexed.
	 *
	 * @return {@code true} if indexing is complete, {@code false} otherwise
	 */
	public boolean isFinished() {
		return finished;
	}

	/**
	 * Sets whether all files in this folder have been fully indexed.
	 *
	 * @param finished {@code true} if indexing is complete
	 */
	public void setFinished(boolean finished) {
		this.finished = finished;
	}

	/**
	 * Returns the timestamp of the last detected change.
	 *
	 * @return the lastUpdate timestamp
	 */
	public LocalDateTime getLastUpdate() {
		return lastUpdate;
	}

	/**
	 * Sets the timestamp of the last detected change.
	 *
	 * @param lastUpdate the lastUpdate to set
	 */
	public void setLastUpdate(LocalDateTime lastUpdate) {
		this.lastUpdate = lastUpdate;
	}

	/**
	 * Returns the thumbnail for this folder, or {@code null} if none has been generated yet.
	 *
	 * @return the thumbnail, or {@code null}
	 */
	public Thumbnail getThumbnail() {
		return thumbnail;
	}

	/**
	 * Sets the thumbnail for this folder.
	 *
	 * @param thumbnail the thumbnail to set, or {@code null} to clear it
	 */
	public void setThumbnail(Thumbnail thumbnail) {
		this.thumbnail = thumbnail;
	}
}
