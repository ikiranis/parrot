package eu.apps4net.parrotApp.models;

/**
 * DTO representing a single tag entry for export and import operations.
 * Carries only the user-assigned fields: path, filename, rating, and view count.
 */
public class TagExportItemDTO {

	/** Absolute directory path of the media file on the server. */
	private String path;

	/** File name including its extension. */
	private String filename;

	/** User rating between 1 and 5, or {@code null} if not set. */
	private Integer rating;

	/** Number of times the file has been viewed; {@code null} is treated as zero on import. */
	private Long viewCount;

	/** Required no-arg constructor for Jackson deserialization. */
	public TagExportItemDTO() {
	}

	/**
	 * Constructs a fully-initialised {@code TagExportItemDTO}.
	 *
	 * @param path      the directory path containing the file
	 * @param filename  the file name including extension
	 * @param rating    the user rating, may be {@code null}
	 * @param viewCount the view count, may be {@code null}
	 */
	public TagExportItemDTO(String path, String filename, Integer rating, Long viewCount) {
		this.path = path;
		this.filename = filename;
		this.rating = rating;
		this.viewCount = viewCount;
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
	 * Returns the user rating.
	 *
	 * @return the rating, or {@code null} if not set
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
	 * Returns the view count.
	 *
	 * @return the viewCount, or {@code null} if not present
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
}
