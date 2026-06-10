package eu.apps4net.parrotApp.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

/**
 * JPA entity representing a thumbnail image for a folder or media file.
 * The path is stored relative to the application's thumbnails root folder,
 * and the type discriminates between folder and file thumbnails.
 */
@Entity(name = "Thumbnail")
@Table(name = "thumbnail")
public class Thumbnail {

	/** Auto-generated primary key. */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/** Thumbnail image path relative to the thumbnails root folder. */
	@NotBlank
	@Size(max = 1024)
	@Column(name = "path", nullable = false, length = 1024)
	private String path;

	/** Whether this thumbnail belongs to a folder or a media file. */
	@NotNull
	@Enumerated(EnumType.STRING)
	@Column(name = "type", nullable = false)
	private ThumbnailType type;

	/** Timestamp of the last thumbnail generation, set automatically on insert and update. */
	@Column(name = "date_update", nullable = false)
	private LocalDateTime dateUpdate;

	/** Required no-arg constructor for JPA. */
	public Thumbnail() {
	}

	/**
	 * Constructs a fully-initialised {@code Thumbnail}.
	 *
	 * @param path the thumbnail image path relative to the thumbnails root folder
	 * @param type whether this thumbnail belongs to a folder or a media file
	 */
	public Thumbnail(String path, ThumbnailType type) {
		this.path = path;
		this.type = type;
	}

	/** Sets {@code dateUpdate} to the current timestamp before insert or update. */
	@PrePersist
	@PreUpdate
	private void stampDate() {
		this.dateUpdate = LocalDateTime.now();
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
	 * Returns the thumbnail image path relative to the thumbnails root folder.
	 *
	 * @return the relative path
	 */
	public String getPath() {
		return path;
	}

	/**
	 * Sets the thumbnail image path relative to the thumbnails root folder.
	 *
	 * @param path the relative path to set
	 */
	public void setPath(String path) {
		this.path = path;
	}

	/**
	 * Returns whether this thumbnail belongs to a folder or a media file.
	 *
	 * @return the thumbnail type
	 */
	public ThumbnailType getType() {
		return type;
	}

	/**
	 * Sets whether this thumbnail belongs to a folder or a media file.
	 *
	 * @param type the thumbnail type to set
	 */
	public void setType(ThumbnailType type) {
		this.type = type;
	}

	/**
	 * Returns the timestamp of the last thumbnail generation.
	 *
	 * @return the date and time when this thumbnail was last generated
	 */
	public LocalDateTime getDateUpdate() {
		return dateUpdate;
	}

	/**
	 * Sets the timestamp of the last thumbnail generation.
	 *
	 * @param dateUpdate the date and time to set
	 */
	public void setDateUpdate(LocalDateTime dateUpdate) {
		this.dateUpdate = dateUpdate;
	}
}
