package eu.apps4net.parrotApp.controllers;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import eu.apps4net.parrotApp.models.Folder;
import eu.apps4net.parrotApp.models.MediaFile;
import eu.apps4net.parrotApp.models.MediaKind;
import eu.apps4net.parrotApp.repositories.FolderRepository;
import eu.apps4net.parrotApp.repositories.MediaFileRepository;

import java.util.List;

/**
 * REST controller for searching the media library.
 *
 * Exposes endpoints that filter the library by a free-text query matched against folder paths,
 * photo paths, and photo filenames, optionally constrained to photos of a given rating. Folders
 * and photos are searched through separate endpoints so the photo results can be paginated for
 * infinite scroll while the (typically far smaller) set of matching folders is returned in one call.
 */
@RestController
@RequestMapping("api/search")
public class SearchController {

	/** Upper bound on the number of folders returned by a single folder search. */
	private static final int FOLDER_LIMIT = 200;

	/** Repository for querying media files. */
	private final MediaFileRepository mediaFileRepository;

	/** Repository for querying folders. */
	private final FolderRepository folderRepository;

	/**
	 * Constructs a new {@code SearchController}.
	 *
	 * @param mediaFileRepository the media file repository
	 * @param folderRepository    the folder repository
	 */
	public SearchController(MediaFileRepository mediaFileRepository, FolderRepository folderRepository) {
		this.mediaFileRepository = mediaFileRepository;
		this.folderRepository = folderRepository;
	}

	/**
	 * Searches folders whose relative path contains the given query, case-insensitively.
	 *
	 * A blank query yields an empty list rather than every folder, so callers that clear the search
	 * box do not accidentally pull the whole table. Results are capped at {@link #FOLDER_LIMIT}.
	 *
	 * @param query the free-text query matched against folder paths; blank returns an empty list
	 * @return list of matching {@link Folder} records, ordered by path ascending
	 */
	@GetMapping("folders")
	public ResponseEntity<List<Folder>> searchFolders(@RequestParam(defaultValue = "") String query) {
		String trimmed = query.trim();
		if (trimmed.isEmpty()) {
			return ResponseEntity.ok(List.of());
		}
		String pattern = "%" + trimmed.toLowerCase() + "%";
		return ResponseEntity.ok(folderRepository.searchByPath(pattern, PageRequest.of(0, FOLDER_LIMIT)));
	}

	/**
	 * Searches image files whose relative path or filename contains the given query,
	 * case-insensitively, optionally constrained to a single rating.
	 *
	 * When {@code rating} is supplied only photos rated exactly that value are returned; when it is
	 * absent photos of any rating (or none) match. A blank query matches every photo, so the rating
	 * filter can be used on its own to list all photos of a given rating.
	 *
	 * @param query  the free-text query matched against photo paths and filenames
	 * @param rating optional exact rating filter (1–5); null matches all ratings
	 * @param page   zero-based page index (default 0)
	 * @param size   number of records per page (default 50)
	 * @return a {@link Page} of matching {@link MediaFile} records of kind IMAGE
	 */
	@GetMapping("photos")
	public ResponseEntity<Page<MediaFile>> searchPhotos(
			@RequestParam(defaultValue = "") String query,
			@RequestParam(required = false) Integer rating,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "50") int size) {
		if (rating != null && (rating < 1 || rating > 5)) {
			return ResponseEntity.ok(new PageImpl<>(List.<MediaFile>of()));
		}
		String pattern = "%" + query.trim().toLowerCase() + "%";
		PageRequest pageable = PageRequest.of(page, size, Sort.by("filename").ascending());
		Page<MediaFile> result = (rating != null)
				? mediaFileRepository.searchByKindAndTextAndRating(MediaKind.IMAGE, pattern, rating, pageable)
				: mediaFileRepository.searchByKindAndText(MediaKind.IMAGE, pattern, pageable);
		return ResponseEntity.ok(result);
	}
}
