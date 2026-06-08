package eu.apps4net.parrotApp.models;

import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * DTO combining {@link MediaFile} and its optional {@link PhotoTag} for the photo-detail endpoint.
 * Fields from {@link PhotoTag} are {@code null} when no tag record exists for the media file.
 */
public class PhotoDetailDTO {

	/** Primary key of the media file. */
	private Long id;

	/** Absolute directory path of the file on the server. */
	private String path;

	/** Name of the file including its extension. */
	private String filename;

	/** Optional content hash used for de-duplication. */
	private String hash;

	/** Media kind (e.g. {@code "IMAGE"}). */
	private String kind;

	/** User-assigned display name. */
	private String name;

	/** Optional free-text description. */
	private String description;

	/** Album the photo belongs to. */
	private String album;

	/** File size in bytes. */
	private Long filesize;

	/** Image width in pixels. */
	private Integer width;

	/** Image height in pixels. */
	private Integer height;

	/** Number of times this photo has been viewed. */
	private Long viewCount;

	/** User rating between 1 and 5 (inclusive). */
	private Integer rating;

	/** Date and time the photo was originally taken. */
	private LocalDateTime dateTaken;

	/** GPS latitude coordinate. */
	private Double latitude;

	/** GPS longitude coordinate. */
	private Double longitude;

	/** Camera manufacturer extracted from EXIF data. */
	private String cameraMake;

	/** Camera model extracted from EXIF data. */
	private String cameraModel;

	/** MIME type of the image file (e.g. {@code "image/jpeg"}). */
	private String mimeType;

	/** Timestamp when the tag record was first persisted. */
	private LocalDateTime dateCreated;

	/** Timestamp of the most recent update to the tag record. */
	private LocalDateTime dateUpdated;

	/** Required no-arg constructor for Jackson serialisation. */
	public PhotoDetailDTO() {
	}

	/**
	 * Factory method that builds a {@code PhotoDetailDTO} from a {@link MediaFile}
	 * and an optional {@link PhotoTag}.
	 *
	 * @param mediaFile the media file entity (must not be {@code null})
	 * @param photoTag  the optional photo tag; may be empty
	 * @return a fully populated {@code PhotoDetailDTO}
	 */
	public static PhotoDetailDTO from(MediaFile mediaFile, Optional<PhotoTag> photoTag) {
		PhotoDetailDTO dto = new PhotoDetailDTO();
		dto.setId(mediaFile.getId());
		dto.setPath(Paths.get(mediaFile.getLibraryFolder().getPath(), mediaFile.getPath()).toString());
		dto.setFilename(mediaFile.getFilename());
		dto.setHash(mediaFile.getHash());
		dto.setKind(mediaFile.getKind() != null ? mediaFile.getKind().name() : null);

		photoTag.ifPresent(tag -> {
			dto.setName(tag.getName());
			dto.setDescription(tag.getDescription());
			dto.setAlbum(tag.getAlbum());
			dto.setFilesize(tag.getFilesize());
			dto.setWidth(tag.getWidth());
			dto.setHeight(tag.getHeight());
			dto.setViewCount(tag.getViewCount());
			dto.setRating(tag.getRating());
			dto.setDateTaken(tag.getDateTaken());
			dto.setLatitude(tag.getLatitude());
			dto.setLongitude(tag.getLongitude());
			dto.setCameraMake(tag.getCameraMake());
			dto.setCameraModel(tag.getCameraModel());
			dto.setMimeType(tag.getMimeType());
			dto.setDateCreated(tag.getDateCreated());
			dto.setDateUpdated(tag.getDateUpdated());
		});

		return dto;
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
	 * Returns the content hash.
	 *
	 * @return the hash
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
	 * Returns the media kind name.
	 *
	 * @return the kind
	 */
	public String getKind() {
		return kind;
	}

	/**
	 * Sets the media kind name.
	 *
	 * @param kind the kind to set
	 */
	public void setKind(String kind) {
		this.kind = kind;
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
	 * Returns the user rating.
	 *
	 * @return the rating
	 */
	public Integer getRating() {
		return rating;
	}

	/**
	 * Sets the user rating.
	 *
	 * @param rating the rating to set
	 */
	public void setRating(Integer rating) {
		this.rating = rating;
	}

	/**
	 * Returns the date the photo was taken.
	 *
	 * @return the dateTaken
	 */
	public LocalDateTime getDateTaken() {
		return dateTaken;
	}

	/**
	 * Sets the date the photo was taken.
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
	 * Returns the camera manufacturer.
	 *
	 * @return the cameraMake
	 */
	public String getCameraMake() {
		return cameraMake;
	}

	/**
	 * Sets the camera manufacturer.
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
