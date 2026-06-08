package eu.apps4net.parrotApp.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * JPA entity representing a media file entry in the library.
 * Stores the file system path, filename, optional hash, and media kind.
 */
@Entity(name = "MediaFile")
@Table(name = "media_file")
public class MediaFile {

	/** Auto-generated primary key. */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/** Absolute directory path of the file on the server. */
	@NotBlank
	@Size(max = 1024)
	@Column(name = "path", nullable = false, length = 1024)
	private String path;

	/** Name of the file including its extension. */
	@NotBlank
	@Size(max = 512)
	@Column(name = "filename", nullable = false, length = 512)
	private String filename;

	/** Optional content hash used for de-duplication. */
	@Size(max = 100)
	@Column(name = "hash")
	private String hash;

	/** Classifies the media file as image, video, audio, or document. */
	@Enumerated(EnumType.STRING)
	@Column(name = "kind")
	private MediaKind kind;

	/** Required no-arg constructor for JPA. */
	public MediaFile() {
	}

	/**
	 * Constructs a fully-initialised {@code MediaFile}.
	 *
	 * @param path     the directory path containing the file
	 * @param filename the file name
	 * @param hash     optional content hash, may be {@code null}
	 * @param kind     the media kind
	 */
	public MediaFile(String path, String filename, String hash, MediaKind kind) {
		this.path = path;
		this.filename = filename;
		this.hash = hash;
		this.kind = kind;
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
	 * Returns the directory path.
	 *
	 * @return the path
	 */
	public String getPath() {
		return path;
	}

	/**
	 * Sets the directory path.
	 *
	 * @param path the path to set
	 */
	public void setPath(String path) {
		this.path = path;
	}

	/**
	 * Returns the file name.
	 *
	 * @return the filename
	 */
	public String getFilename() {
		return filename;
	}

	/**
	 * Sets the file name.
	 *
	 * @param filename the filename to set
	 */
	public void setFilename(String filename) {
		this.filename = filename;
	}

	/**
	 * Returns the optional content hash.
	 *
	 * @return the hash, or {@code null} if not computed
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
	 * Returns the media kind.
	 *
	 * @return the kind
	 */
	public MediaKind getKind() {
		return kind;
	}

	/**
	 * Sets the media kind.
	 *
	 * @param kind the kind to set
	 */
	public void setKind(MediaKind kind) {
		this.kind = kind;
	}
}
