package eu.apps4net.parrotApp.utilities;

import org.springframework.data.domain.Sort;

import java.util.Map;

/**
 * Translates a client-supplied sort field name and direction into a Spring Data {@link Sort}
 * for paginated photo queries.
 *
 * Photos are returned as MediaFile records, but most sortable attributes (rating, date taken,
 * file size, camera, etc.) live on the associated PhotoTag entity. The photo queries therefore
 * join PhotoTag under the alias {@code pt}, and the sort properties produced here reference either
 * a MediaFile column directly (e.g. {@code filename}) or a PhotoTag column through that alias
 * (e.g. {@code pt.rating}).
 *
 * Only the field names in the internal whitelist are accepted; any unknown or blank value falls
 * back to sorting by filename. This both guards against invalid-property errors at query time and
 * keeps the set of sortable columns explicit.
 *
 * Every resolved sort appends an ascending {@code id} tiebreaker so that records with equal sort
 * keys (for example many photos sharing a rating) keep a stable, repeatable order across the pages
 * fetched by the grid's infinite scroll.
 */
public final class PhotoSortResolver {

	/**
	 * Maps an accepted client field name to the JPQL sort property used in the photo queries.
	 * MediaFile columns are unqualified; PhotoTag columns are prefixed with the join alias {@code pt}.
	 */
	private static final Map<String, String> SORT_PROPERTIES = Map.ofEntries(
			Map.entry("filename", "filename"),
			Map.entry("name", "pt.name"),
			Map.entry("album", "pt.album"),
			Map.entry("rating", "pt.rating"),
			Map.entry("viewCount", "pt.viewCount"),
			Map.entry("dateTaken", "pt.dateTaken"),
			Map.entry("filesize", "pt.filesize"),
			Map.entry("width", "pt.width"),
			Map.entry("height", "pt.height"),
			Map.entry("cameraMake", "pt.cameraMake"),
			Map.entry("cameraModel", "pt.cameraModel"),
			Map.entry("dateCreated", "pt.dateCreated"),
			Map.entry("dateUpdated", "pt.dateUpdated"));

	/** Sort property applied when the requested field is unknown or blank. */
	private static final String DEFAULT_PROPERTY = "filename";

	/** Utility class; not instantiable. */
	private PhotoSortResolver() {
	}

	/**
	 * Builds a {@link Sort} from a client-supplied field name and direction.
	 *
	 * @param sortBy    the field to sort by; one of the accepted names, otherwise filename is used
	 * @param direction the sort direction; "desc" (case-insensitive) sorts descending, anything else ascending
	 * @return a {@link Sort} on the resolved property followed by an ascending id tiebreaker
	 */
	public static Sort resolve(String sortBy, String direction) {
		String property = SORT_PROPERTIES.getOrDefault(sortBy, DEFAULT_PROPERTY);
		Sort.Direction dir = "desc".equalsIgnoreCase(direction) ? Sort.Direction.DESC : Sort.Direction.ASC;
		return Sort.by(dir, property).and(Sort.by(Sort.Direction.ASC, "id"));
	}
}
