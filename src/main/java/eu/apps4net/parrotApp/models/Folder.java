package eu.apps4net.parrotApp.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * JPA entity representing a scanned folder in the media library.
 * Tracks the folder path, a content hash derived from file count and total
 * size, the nesting level relative to the library root, a flag indicating
 * whether all files have been fully indexed, and the last time a change was
 * detected.
 */
@Entity(name = "Folder")
@Table(name = "folder")
public class Folder {

	/** Auto-generated primary key. */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/** Full absolute path of the folder on the server. */
	@NotBlank
	@Size(max = 512)
	@Column(name = "path", nullable = false, unique = true)
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

	/** Required no-arg constructor for JPA. */
	public Folder() {
	}

	/**
	 * Constructs a fully-initialised {@code Folder}.
	 * {@code finished} is always {@code false} on construction and must be set
	 * explicitly once all files in the folder have been indexed.
	 *
	 * @param path       the full absolute path of the folder
	 * @param hash       the content hash, may be {@code null}
	 * @param level      the nesting level relative to the library root (0 = root)
	 * @param lastUpdate the timestamp of the last detected change
	 */
	public Folder(String path, String hash, int level, LocalDateTime lastUpdate) {
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
	 * Returns the full path of the folder.
	 *
	 * @return the path
	 */
	public String getPath() {
		return path;
	}

	/**
	 * Sets the full path of the folder.
	 *
	 * @param path the path to set
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
}
