package eu.apps4net.parrotApp.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * JPA entity representing a media file entry in the library.
 * Stores the directory path relative to the {@link LibraryFolder} root, filename,
 * optional hash, and media kind.
 */
@Entity(name = "MediaFile")
@Table(name = "media_file")
public class MediaFile {

	/** Auto-generated primary key. */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/** Library folder this file belongs to. */
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "library_folder_id", nullable = false)
	private LibraryFolder libraryFolder;

	/** Directory path of the file relative to the library folder root. Empty string means the root directory. */
	@NotNull
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

	/**
	 * Primary key of the linked thumbnail, readable without loading the lazy entity.
	 * Mirrors the {@code thumbnail_id} FK column; managed exclusively by the ORM via
	 * {@link #thumbnail} — never set this field directly.
	 */
	@Column(name = "thumbnail_id", insertable = false, updatable = false)
	private Long thumbnailId;

	/** Optional thumbnail for this media file. Null when no thumbnail has been generated yet. */
	@OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinColumn(name = "thumbnail_id")
	private Thumbnail thumbnail;

	/** Required no-arg constructor for JPA. */
	public MediaFile() {
	}

	/**
	 * Constructs a fully-initialised {@code MediaFile}.
	 *
	 * @param libraryFolder the library folder this file belongs to
	 * @param path          the directory path relative to the library folder root; empty string for the root
	 * @param filename      the file name
	 * @param hash          optional content hash, may be {@code null}
	 * @param kind          the media kind
	 */
	public MediaFile(LibraryFolder libraryFolder, String path, String filename, String hash, MediaKind kind) {
		this.libraryFolder = libraryFolder;
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
	 * Returns the library folder this file belongs to.
	 *
	 * @return the library folder
	 */
	public LibraryFolder getLibraryFolder() {
		return libraryFolder;
	}

	/**
	 * Sets the library folder this file belongs to.
	 *
	 * @param libraryFolder the library folder to set
	 */
	public void setLibraryFolder(LibraryFolder libraryFolder) {
		this.libraryFolder = libraryFolder;
	}

	/**
	 * Returns the directory path relative to the library folder root.
	 * An empty string means the file resides directly in the library root.
	 *
	 * @return the relative directory path
	 */
	public String getPath() {
		return path;
	}

	/**
	 * Sets the directory path relative to the library folder root.
	 *
	 * @param path the relative path to set
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

	/**
	 * Returns the primary key of the linked thumbnail without triggering a lazy load,
	 * or {@code null} if no thumbnail has been generated yet.
	 *
	 * @return the thumbnail id, or {@code null}
	 */
	public Long getThumbnailId() {
		return thumbnailId;
	}

	/**
	 * Returns the thumbnail for this media file, or {@code null} if none has been generated yet.
	 *
	 * @return the thumbnail, or {@code null}
	 */
	@JsonIgnore
	public Thumbnail getThumbnail() {
		return thumbnail;
	}

	/**
	 * Sets the thumbnail for this media file.
	 *
	 * @param thumbnail the thumbnail to set, or {@code null} to clear it
	 */
	public void setThumbnail(Thumbnail thumbnail) {
		this.thumbnail = thumbnail;
	}
}
