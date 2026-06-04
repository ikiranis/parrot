package eu.apps4net.parrotApp.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * JPA entity representing a scanned folder in the media library.
 * Tracks the folder path, a content hash derived from file count and total
 * size, an optional thumbnail image path, and the last time a change was
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

	/** Full path to a representative thumbnail image from this folder, if any. */
	@Size(max = 512)
	@Column(name = "thumbnail")
	private String thumbnail;

	/** Timestamp of the last detected change in this folder. */
	@Column(name = "last_update")
	private LocalDateTime lastUpdate;

	/** Required no-arg constructor for JPA. */
	public Folder() {
	}

	/**
	 * Constructs a fully-initialised {@code Folder}.
	 *
	 * @param path       the full absolute path of the folder
	 * @param hash       the content hash, may be {@code null}
	 * @param thumbnail  full path to a thumbnail image, may be {@code null}
	 * @param lastUpdate the timestamp of the last detected change
	 */
	public Folder(String path, String hash, String thumbnail, LocalDateTime lastUpdate) {
		this.path = path;
		this.hash = hash;
		this.thumbnail = thumbnail;
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
	 * Returns the full path to the thumbnail image.
	 *
	 * @return the thumbnail path, or {@code null} if none has been assigned
	 */
	public String getThumbnail() {
		return thumbnail;
	}

	/**
	 * Sets the full path to the thumbnail image.
	 *
	 * @param thumbnail the thumbnail path to set
	 */
	public void setThumbnail(String thumbnail) {
		this.thumbnail = thumbnail;
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
