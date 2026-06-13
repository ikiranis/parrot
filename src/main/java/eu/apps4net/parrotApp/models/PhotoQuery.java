package eu.apps4net.parrotApp.models;

/**
 * Search criteria describing which photos a search or slideshow should match.
 *
 * Carries the free-text query and an optional exact rating filter. It is sent as a single JSON
 * object so the slideshow can play exactly the photos currently shown in the search results.
 *
 * The type is intentionally open to extension: future tag-based filters can be added as additional
 * fields without changing the photo-batch endpoint that consumes it.
 */
public class PhotoQuery {

	/** Free-text query matched against photo paths and filenames; blank or null matches all photos. */
	private String text;

	/** Exact rating filter between 1 and 5, or {@code null} to match all ratings. */
	private Integer rating;

	/** Required no-arg constructor for Jackson deserialization. */
	public PhotoQuery() {
	}

	/**
	 * Reports whether this query carries any active filter.
	 *
	 * A query is active when it has non-blank text or a rating filter. An inactive query imposes no
	 * constraint and is treated by callers as the absence of a search scope.
	 *
	 * @return true if the text is non-blank or a rating is set, false otherwise
	 */
	public boolean isActive() {
		return (text != null && !text.isBlank()) || rating != null;
	}

	/**
	 * Returns the free-text query.
	 *
	 * @return the text, or {@code null} if not set
	 */
	public String getText() {
		return text;
	}

	/**
	 * Sets the free-text query.
	 *
	 * @param text the text to set
	 */
	public void setText(String text) {
		this.text = text;
	}

	/**
	 * Returns the exact rating filter.
	 *
	 * @return the rating, or {@code null} to match all ratings
	 */
	public Integer getRating() {
		return rating;
	}

	/**
	 * Sets the exact rating filter.
	 *
	 * @param rating the rating to set
	 */
	public void setRating(Integer rating) {
		this.rating = rating;
	}
}
