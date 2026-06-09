package eu.apps4net.parrotApp.services;

import org.springframework.stereotype.Service;

import eu.apps4net.parrotApp.models.Thumbnail;
import eu.apps4net.parrotApp.models.ThumbnailType;
import eu.apps4net.parrotApp.repositories.ThumbnailRepository;

import java.util.List;
import java.util.Optional;

/**
 * Service layer for {@link Thumbnail} entities.
 * Provides CRUD operations and look-up helpers for folder and file thumbnails.
 */
@Service
public class ThumbnailService {

	/** Repository used to persist and retrieve thumbnail records. */
	private final ThumbnailRepository thumbnailRepository;

	/**
	 * Constructs a new {@code ThumbnailService}.
	 *
	 * @param thumbnailRepository the thumbnail repository
	 */
	public ThumbnailService(ThumbnailRepository thumbnailRepository) {
		this.thumbnailRepository = thumbnailRepository;
	}

	/**
	 * Returns all persisted thumbnails.
	 *
	 * @return list of all {@link Thumbnail} records
	 */
	public List<Thumbnail> getAllThumbnails() {
		return thumbnailRepository.findAll();
	}

	/**
	 * Returns all thumbnails of the given type.
	 *
	 * @param type the thumbnail type to filter by
	 * @return list of matching {@link Thumbnail} records
	 */
	public List<Thumbnail> getThumbnailsByType(ThumbnailType type) {
		return thumbnailRepository.findByType(type);
	}

	/**
	 * Finds a thumbnail by its primary key.
	 *
	 * @param id the thumbnail identifier
	 * @return an {@link Optional} containing the {@link Thumbnail}, or empty if not found
	 */
	public Optional<Thumbnail> getThumbnail(Long id) {
		return thumbnailRepository.findById(id);
	}

	/**
	 * Persists a new or updated thumbnail record.
	 *
	 * @param thumbnail the thumbnail to save
	 * @return the saved {@link Thumbnail} with any generated fields populated
	 */
	public Thumbnail saveThumbnail(Thumbnail thumbnail) {
		return thumbnailRepository.save(thumbnail);
	}

	/**
	 * Deletes the thumbnail with the given primary key.
	 *
	 * @param id the identifier of the thumbnail to delete
	 */
	public void deleteThumbnail(Long id) {
		thumbnailRepository.deleteById(id);
	}
}
