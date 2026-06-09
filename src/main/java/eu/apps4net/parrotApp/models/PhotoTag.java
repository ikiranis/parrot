package eu.apps4net.parrotApp.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import java.time.LocalDateTime;

/**
 * JPA entity storing photo-specific metadata for a {@link MediaFile}.
 * Extended tag information including dimensions, GPS coordinates, camera data,
 * and user-assigned metadata such as name, album, and rating.
 */
@Entity(name = "PhotoTag")
@Table(name = "photo_tag")
public class PhotoTag {

	/** Auto-generated primary key. */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/** The associated media file (one-to-one, unique per file). */
	@OneToOne
	@JoinColumn(name = "media_file_id", nullable = false, unique = true)
	private MediaFile mediaFile;

	/** User-assigned display name for the photo. */
	@Size(max = 255)
	@Column(name = "name")
	private String name;

	/** Optional free-text description. */
	@Size(max = 1000)
	@Column(name = "description", length = 1000)
	private String description;

	/** Album the photo belongs to. */
	@Size(max = 255)
	@Column(name = "album")
	private String album;

	/** File size in bytes. */
	@Column(name = "filesize")
	private Long filesize;

	/** Image width in pixels. */
	@Column(name = "width")
	private Integer width;

	/** Image height in pixels. */
	@Column(name = "height")
	private Integer height;

	/** Number of times this photo has been viewed. */
	@Column(name = "view_count")
	private Long viewCount = 0L;

	/** User rating between 1 and 5 (inclusive). */
	@Min(1)
	@Max(5)
	@Column(name = "rating")
	private Integer rating;

	/** Date and time the photo was originally taken. */
	@Column(name = "date_taken")
	private LocalDateTime dateTaken;

	/** GPS latitude coordinate. */
	@Column(name = "latitude")
	private Double latitude;

	/** GPS longitude coordinate. */
	@Column(name = "longitude")
	private Double longitude;

	/** Camera manufacturer extracted from EXIF data. */
	@Size(max = 100)
	@Column(name = "camera_make")
	private String cameraMake;

	/** Camera model extracted from EXIF data. */
	@Size(max = 100)
	@Column(name = "camera_model")
	private String cameraModel;

	/** MIME type of the image file (e.g. {@code "image/jpeg"}). */
	@Size(max = 100)
	@Column(name = "mime_type")
	private String mimeType;

	/** Timestamp when this record was first persisted. */
	@Column(name = "date_created")
	private LocalDateTime dateCreated;

	/** Timestamp of the most recent update to this record. */
	@Column(name = "date_updated")
	private LocalDateTime dateUpdated;

	/** Required no-arg constructor for JPA. */
	public PhotoTag() {
	}

	/**
	 * Sets {@link #dateCreated} and {@link #dateUpdated} to the current time
	 * before the entity is first persisted.
	 */
	@PrePersist
	protected void onCreate() {
		if (dateCreated == null) dateCreated = LocalDateTime.now();
		dateUpdated = LocalDateTime.now();
	}

	/**
	 * Updates {@link #dateUpdated} to the current time before each update.
	 */
	@PreUpdate
	protected void onUpdate() {
		dateUpdated = LocalDateTime.now();
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
	 * Returns the associated media file.
	 *
	 * @return the media file
	 */
	public MediaFile getMediaFile() {
		return mediaFile;
	}

	/**
	 * Sets the associated media file.
	 *
	 * @param mediaFile the media file to associate
	 */
	public void setMediaFile(MediaFile mediaFile) {
		this.mediaFile = mediaFile;
	}

	/**
	 * Returns the display name.
	 *
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Sets the display name.
	 *
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Returns the description.
	 *
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Sets the description.
	 *
	 * @param description the description to set
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * Returns the album name.
	 *
	 * @return the album
	 */
	public String getAlbum() {
		return album;
	}

	/**
	 * Sets the album name.
	 *
	 * @param album the album to set
	 */
	public void setAlbum(String album) {
		this.album = album;
	}

	/**
	 * Returns the file size in bytes.
	 *
	 * @return the filesize
	 */
	public Long getFilesize() {
		return filesize;
	}

	/**
	 * Sets the file size in bytes.
	 *
	 * @param filesize the filesize to set
	 */
	public void setFilesize(Long filesize) {
		this.filesize = filesize;
	}

	/**
	 * Returns the image width in pixels.
	 *
	 * @return the width
	 */
	public Integer getWidth() {
		return width;
	}

	/**
	 * Sets the image width in pixels.
	 *
	 * @param width the width to set
	 */
	public void setWidth(Integer width) {
		this.width = width;
	}

	/**
	 * Returns the image height in pixels.
	 *
	 * @return the height
	 */
	public Integer getHeight() {
		return height;
	}

	/**
	 * Sets the image height in pixels.
	 *
	 * @param height the height to set
	 */
	public void setHeight(Integer height) {
		this.height = height;
	}

	/**
	 * Returns the view count.
	 *
	 * @return the viewCount
	 */
	public Long getViewCount() {
		return viewCount;
	}

	/**
	 * Sets the view count.
	 *
	 * @param viewCount the viewCount to set
	 */
	public void setViewCount(Long viewCount) {
		this.viewCount = viewCount;
	}

	/**
	 * Returns the user rating (1–5).
	 *
	 * @return the rating
	 */
	public Integer getRating() {
		return rating;
	}

	/**
	 * Sets the user rating.
	 *
	 * @param rating the rating to set (must be between 1 and 5)
	 */
	public void setRating(Integer rating) {
		this.rating = rating;
	}

	/**
	 * Returns the date and time the photo was taken.
	 *
	 * @return the dateTaken
	 */
	public LocalDateTime getDateTaken() {
		return dateTaken;
	}

	/**
	 * Sets the date and time the photo was taken.
	 *
	 * @param dateTaken the dateTaken to set
	 */
	public void setDateTaken(LocalDateTime dateTaken) {
		this.dateTaken = dateTaken;
	}

	/**
	 * Returns the GPS latitude.
	 *
	 * @return the latitude
	 */
	public Double getLatitude() {
		return latitude;
	}

	/**
	 * Sets the GPS latitude.
	 *
	 * @param latitude the latitude to set
	 */
	public void setLatitude(Double latitude) {
		this.latitude = latitude;
	}

	/**
	 * Returns the GPS longitude.
	 *
	 * @return the longitude
	 */
	public Double getLongitude() {
		return longitude;
	}

	/**
	 * Sets the GPS longitude.
	 *
	 * @param longitude the longitude to set
	 */
	public void setLongitude(Double longitude) {
		this.longitude = longitude;
	}

	/**
	 * Returns the camera make.
	 *
	 * @return the cameraMake
	 */
	public String getCameraMake() {
		return cameraMake;
	}

	/**
	 * Sets the camera make.
	 *
	 * @param cameraMake the cameraMake to set
	 */
	public void setCameraMake(String cameraMake) {
		this.cameraMake = cameraMake;
	}

	/**
	 * Returns the camera model.
	 *
	 * @return the cameraModel
	 */
	public String getCameraModel() {
		return cameraModel;
	}

	/**
	 * Sets the camera model.
	 *
	 * @param cameraModel the cameraModel to set
	 */
	public void setCameraModel(String cameraModel) {
		this.cameraModel = cameraModel;
	}

	/**
	 * Returns the MIME type.
	 *
	 * @return the mimeType
	 */
	public String getMimeType() {
		return mimeType;
	}

	/**
	 * Sets the MIME type.
	 *
	 * @param mimeType the mimeType to set
	 */
	public void setMimeType(String mimeType) {
		this.mimeType = mimeType;
	}

	/**
	 * Returns the record creation timestamp.
	 *
	 * @return the dateCreated
	 */
	public LocalDateTime getDateCreated() {
		return dateCreated;
	}

	/**
	 * Sets the record creation timestamp.
	 *
	 * @param dateCreated the dateCreated to set
	 */
	public void setDateCreated(LocalDateTime dateCreated) {
		this.dateCreated = dateCreated;
	}

	/**
	 * Returns the record last-updated timestamp.
	 *
	 * @return the dateUpdated
	 */
	public LocalDateTime getDateUpdated() {
		return dateUpdated;
	}

	/**
	 * Sets the record last-updated timestamp.
	 *
	 * @param dateUpdated the dateUpdated to set
	 */
	public void setDateUpdated(LocalDateTime dateUpdated) {
		this.dateUpdated = dateUpdated;
	}
}
