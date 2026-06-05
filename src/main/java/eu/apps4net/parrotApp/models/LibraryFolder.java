package eu.apps4net.parrotApp.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * JPA entity representing a folder configured as a media library scan source.
 * Library folders are the root paths that will be recursively scanned for media files.
 */
@Entity(name = "LibraryFolder")
@Table(name = "library_folder")
public class LibraryFolder {

	/** Auto-generated primary key. */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/** Full absolute path of the folder on the server. */
	@NotBlank
	@Size(max = 512)
	@Column(name = "path", nullable = false, unique = true)
	private String path;

	/** Required no-arg constructor for JPA. */
	public LibraryFolder() {
	}

	/**
	 * Constructs a new {@code LibraryFolder} with the given path.
	 *
	 * @param path the full absolute path of the folder
	 */
	public LibraryFolder(String path) {
		this.path = path;
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
}
