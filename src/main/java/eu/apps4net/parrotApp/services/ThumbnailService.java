package eu.apps4net.parrotApp.services;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import eu.apps4net.parrotApp.models.Folder;
import eu.apps4net.parrotApp.models.Thumbnail;
import eu.apps4net.parrotApp.models.ThumbnailType;
import eu.apps4net.parrotApp.repositories.FolderRepository;
import eu.apps4net.parrotApp.repositories.PhotoTagRepository;
import eu.apps4net.parrotApp.repositories.ThumbnailRepository;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Service layer for {@link Thumbnail} entities.
 * Provides CRUD operations, look-up helpers for folder and file thumbnails, and folder
 * thumbnail generation. Generated thumbnails are 150x150-pixel centre-crop JPEGs saved
 * under {@code thumbnails/YYYY/MM/DD/HH/} relative to the application working directory.
 */
@Service
public class ThumbnailService {

	/** Side length in pixels for generated square thumbnails. */
	private static final int THUMBNAIL_SIZE = 150;

	/** Maximum number of folders processed in a single regeneration run. */
	private static final int FOLDER_BATCH_SIZE = 1000;

	/** Number of days after which a folder thumbnail is considered stale and eligible for regeneration. */
	private static final int THUMBNAIL_STALE_DAYS = 15;

	/** Root directory name where generated thumbnail files are stored. */
	private static final String THUMBNAILS_ROOT = "thumbnails";

	/** Image file extensions eligible as thumbnail source images (lower-case, no leading dot). */
	private static final Set<String> IMAGE_EXTENSIONS = Set.of(
			"jpg", "jpeg", "png", "gif", "bmp", "webp", "tiff", "tif"
	);

	/** Repository used to persist and retrieve thumbnail records. */
	private final ThumbnailRepository thumbnailRepository;

	/** Repository used to query and update folder records during thumbnail generation. */
	private final FolderRepository folderRepository;

	/** Repository used to look up per-photo ratings for weighted image selection. */
	private final PhotoTagRepository photoTagRepository;

	/**
	 * Constructs a new {@code ThumbnailService}.
	 *
	 * @param thumbnailRepository the thumbnail repository
	 * @param folderRepository    the folder repository
	 * @param photoTagRepository  the photo-tag repository
	 */
	public ThumbnailService(ThumbnailRepository thumbnailRepository, FolderRepository folderRepository,
			PhotoTagRepository photoTagRepository) {
		this.thumbnailRepository = thumbnailRepository;
		this.folderRepository = folderRepository;
		this.photoTagRepository = photoTagRepository;
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

	/**
	 * Returns the absolute file-system path for the given thumbnail.
	 * The path is resolved from the application working directory.
	 *
	 * @param thumbnail the thumbnail whose file path to resolve
	 * @return the absolute {@link Path} to the thumbnail file on disk
	 */
	public Path resolveThumbnailPath(Thumbnail thumbnail) {
		return Paths.get(THUMBNAILS_ROOT).resolve(thumbnail.getPath());
	}

	/**
	 * Generates a thumbnail for a single media file and returns the saved {@link Thumbnail} entity.
	 * The thumbnail is a 150x150-pixel centre-crop JPEG written to
	 * {@code thumbnails/YYYY/MM/DD/HH/<fileId>_<nanos>.jpg} relative to the application working directory.
	 * The caller is responsible for linking the returned thumbnail to the {@link eu.apps4net.parrotApp.models.MediaFile}
	 * and persisting the updated entity.
	 *
	 * @param fileId      the primary key of the media file; used as a prefix in the output filename
	 * @param sourceImage absolute path to the source image file
	 * @return the persisted {@link Thumbnail} entity
	 * @throws IOException if the source image cannot be read or the output file cannot be written
	 */
	public Thumbnail generatePhotoThumbnail(Long fileId, Path sourceImage) throws IOException {
		LocalDateTime now = LocalDateTime.now();
		Path outputDir = Paths.get(
				THUMBNAILS_ROOT,
				String.valueOf(now.getYear()),
				String.format("%02d", now.getMonthValue()),
				String.format("%02d", now.getDayOfMonth()),
				String.format("%02d", now.getHour()));

		Files.createDirectories(outputDir);

		String datePath = now.getYear() + "/" +
				String.format("%02d", now.getMonthValue()) + "/" +
				String.format("%02d", now.getDayOfMonth()) + "/" +
				String.format("%02d", now.getHour());

		String filename = fileId + "_" + System.nanoTime() + ".jpg";
		Path outputPath = outputDir.resolve(filename);

		if (!writeThumbnail(sourceImage, outputPath)) {
			throw new IOException("Failed to write thumbnail for: " + sourceImage);
		}

		return thumbnailRepository.save(new Thumbnail(datePath + "/" + filename, ThumbnailType.FILE));
	}

	/**
	 * Generates a thumbnail for a single folder and returns the saved thumbnail id.
	 * A random image from the folder directory tree is used as the source.
	 * The caller is responsible for checking whether the folder already has a thumbnail
	 * before calling this method to avoid duplicate generation.
	 *
	 * @param folder the folder to generate a thumbnail for
	 * @return the id of the persisted {@link Thumbnail}
	 * @throws IOException if the folder directory cannot be read, no image files are found,
	 *                     or the thumbnail file cannot be written
	 */
	public Long generateSingleFolderThumbnail(Folder folder) throws IOException {
		Path folderAbsPath = Paths.get(folder.getLibraryFolder().getPath()).resolve(folder.getPath());

		if (!Files.isDirectory(folderAbsPath)) {
			throw new IOException("Path is not a directory: " + folderAbsPath);
		}

		Path sourceImage = pickRandomImage(folderAbsPath);
		if (sourceImage == null) {
			throw new IOException("No image files found in folder: " + folderAbsPath);
		}

		LocalDateTime now = LocalDateTime.now();
		Path outputDir = Paths.get(
				THUMBNAILS_ROOT,
				String.valueOf(now.getYear()),
				String.format("%02d", now.getMonthValue()),
				String.format("%02d", now.getDayOfMonth()),
				String.format("%02d", now.getHour()));

		Files.createDirectories(outputDir);

		String datePath = now.getYear() + "/" +
				String.format("%02d", now.getMonthValue()) + "/" +
				String.format("%02d", now.getDayOfMonth()) + "/" +
				String.format("%02d", now.getHour());

		String filename = folder.getId() + "_" + System.nanoTime() + ".jpg";
		Path outputPath = outputDir.resolve(filename);

		if (!writeThumbnail(sourceImage, outputPath)) {
			throw new IOException("Failed to write thumbnail for folder: " + folder.getId());
		}

		Thumbnail thumbnail = thumbnailRepository.save(new Thumbnail(datePath + "/" + filename, ThumbnailType.FOLDER));
		folder.setThumbnail(thumbnail);
		folderRepository.save(folder);
		return thumbnail.getId();
	}

	/**
	 * Regenerates thumbnails for up to {@value #FOLDER_BATCH_SIZE} folders whose existing thumbnail
	 * is older than {@value #THUMBNAIL_STALE_DAYS} days, processing shallowest folders first.
	 *
	 * For each qualifying folder a rating-weighted random image from its directory tree is used as
	 * the source: images with a higher user rating get proportionally more chances to be selected.
	 * Unrated images are treated as weight 1 (the minimum).
	 * The old thumbnail file is deleted from disk after the new one is written successfully.
	 * Folders whose directory is inaccessible or contains no image files are skipped silently.
	 *
	 * @return the number of folder thumbnails successfully regenerated in this run
	 */
	public int generateFolderThumbnails() {
		LocalDateTime cutoff = LocalDateTime.now().minusDays(THUMBNAIL_STALE_DAYS);
		List<Folder> folders = folderRepository.findFoldersWithOldThumbnailsOrderedByLevel(
				cutoff, PageRequest.of(0, FOLDER_BATCH_SIZE));

		if (folders.isEmpty()) return 0;

		LocalDateTime now = LocalDateTime.now();
		Path outputDir = Paths.get(
				THUMBNAILS_ROOT,
				String.valueOf(now.getYear()),
				String.format("%02d", now.getMonthValue()),
				String.format("%02d", now.getDayOfMonth()),
				String.format("%02d", now.getHour()));

		try {
			Files.createDirectories(outputDir);
		} catch (IOException e) {
			System.err.println("ThumbnailService: cannot create output directory " + outputDir + " — " + e.getMessage());
			return 0;
		}

		String datePath = now.getYear() + "/" +
				String.format("%02d", now.getMonthValue()) + "/" +
				String.format("%02d", now.getDayOfMonth()) + "/" +
				String.format("%02d", now.getHour());

		int count = 0;
		for (Folder folder : folders) {
			try {
				if (processFolder(folder, outputDir, datePath)) {
					count++;
				}
			} catch (Exception e) {
				System.err.println("ThumbnailService: error regenerating thumbnail for folder "
						+ folder.getId() + " — " + e.getMessage());
			}
		}
		return count;
	}

	/**
	 * Picks a rating-weighted random image from the folder tree, writes a new thumbnail,
	 * persists the {@link Thumbnail} entity, links it to the folder, and deletes the old
	 * thumbnail file from disk.
	 *
	 * @param folder    the folder to regenerate a thumbnail for
	 * @param outputDir absolute path to the directory where the new file will be written
	 * @param datePath  relative date path segment used to build the stored thumbnail path
	 * @return {@code true} if a new thumbnail was written successfully, {@code false} if skipped
	 * @throws IOException if the folder directory cannot be listed
	 */
	private boolean processFolder(Folder folder, Path outputDir, String datePath) throws IOException {
		Path folderAbsPath = Paths.get(folder.getLibraryFolder().getPath()).resolve(folder.getPath());

		if (!Files.isDirectory(folderAbsPath)) return false;

		List<Path> images = collectImageFiles(folderAbsPath);
		if (images.isEmpty()) return false;

		Map<String, Integer> ratingMap = buildRatingMap(folder);
		Path sourceImage = pickWeightedImage(images, ratingMap, folder.getLibraryFolder().getPath());

		String filename = folder.getId() + "_" + System.nanoTime() + ".jpg";
		Path outputPath = outputDir.resolve(filename);

		if (!writeThumbnail(sourceImage, outputPath)) return false;

		Thumbnail oldThumbnail = folder.getThumbnail();
		String oldThumbnailPath = (oldThumbnail != null) ? oldThumbnail.getPath() : null;

		Thumbnail newThumbnail = thumbnailRepository.save(new Thumbnail(datePath + "/" + filename, ThumbnailType.FOLDER));
		folder.setThumbnail(newThumbnail);
		folderRepository.save(folder);

		if (oldThumbnailPath != null) {
			try {
				Files.deleteIfExists(Paths.get(THUMBNAILS_ROOT).resolve(oldThumbnailPath));
			} catch (IOException e) {
				System.err.println("ThumbnailService: could not delete old thumbnail file "
						+ oldThumbnailPath + " — " + e.getMessage());
			}
		}

		return true;
	}

	/**
	 * Builds a map from {@code "mediaFilePath||filename"} to rating for all rated images
	 * in the given folder's directory tree, queried from the database.
	 * Used to weight random image selection during thumbnail generation.
	 *
	 * @param folder the folder whose subtree is queried
	 * @return map of composite key to rating (1–5); empty if no rated images exist in the subtree
	 */
	private Map<String, Integer> buildRatingMap(Folder folder) {
		String path = folder.getPath();
		String pathPrefix = path.isEmpty() ? "%" : path + "/%";
		List<Object[]> rows = photoTagRepository.findRatingsInFolderSubtree(
				folder.getLibraryFolder(), path, pathPrefix);
		Map<String, Integer> map = new HashMap<>();
		for (Object[] row : rows) {
			map.put(row[0] + "||" + row[1], (Integer) row[2]);
		}
		return map;
	}

	/**
	 * Selects one image from {@code images} using a rating-weighted random draw.
	 * Each image is entered into the pool a number of times equal to its rating (1–5);
	 * images without a rating entry in {@code ratingMap} get weight 1.
	 *
	 * @param images     candidate image paths (must not be empty)
	 * @param ratingMap  map of {@code "mediaFilePath||filename"} to rating built by {@link #buildRatingMap}
	 * @param libRootPath absolute path of the library folder root, used to derive DB-style relative paths
	 * @return the selected image path
	 */
	private Path pickWeightedImage(List<Path> images, Map<String, Integer> ratingMap, String libRootPath) {
		Path libRoot = Paths.get(libRootPath);
		List<Path> pool = new ArrayList<>();
		for (Path img : images) {
			Path relDir = libRoot.relativize(img.getParent());
			String dbPath = relDir.toString().replace(File.separatorChar, '/');
			String key = dbPath + "||" + img.getFileName().toString();
			int weight = ratingMap.getOrDefault(key, 1);
			for (int i = 0; i < weight; i++) {
				pool.add(img);
			}
		}
		return pool.get(ThreadLocalRandom.current().nextInt(pool.size()));
	}

	/**
	 * Walks {@code dir} recursively and returns all supported image files found.
	 *
	 * @param dir the root directory to search
	 * @return list of image {@link Path} objects; empty if none found
	 * @throws IOException if the directory tree cannot be walked
	 */
	private List<Path> collectImageFiles(Path dir) throws IOException {
		try (Stream<Path> files = Files.walk(dir)) {
			return files
					.filter(Files::isRegularFile)
					.filter(f -> {
						String name = f.getFileName().toString().toLowerCase();
						int dot = name.lastIndexOf('.');
						return dot >= 0 && IMAGE_EXTENSIONS.contains(name.substring(dot + 1));
					})
					.collect(Collectors.toList());
		}
	}

	/**
	 * Walks {@code dir} recursively and returns one image file chosen at random,
	 * or {@code null} if no supported image files are found anywhere in the tree.
	 *
	 * @param dir the root directory to search
	 * @return a randomly selected image {@link Path}, or {@code null} if none found
	 * @throws IOException if the directory tree cannot be walked
	 */
	private Path pickRandomImage(Path dir) throws IOException {
		try (Stream<Path> files = Files.walk(dir)) {
			List<Path> images = files
					.filter(Files::isRegularFile)
					.filter(f -> {
						String name = f.getFileName().toString().toLowerCase();
						int dot = name.lastIndexOf('.');
						return dot >= 0 && IMAGE_EXTENSIONS.contains(name.substring(dot + 1));
					})
					.collect(Collectors.toList());
			if (images.isEmpty()) return null;
			return images.get(ThreadLocalRandom.current().nextInt(images.size()));
		}
	}

	/**
	 * Reads the image at {@code source}, scales it so the shorter side equals
	 * {@value #THUMBNAIL_SIZE} pixels, crops the centre to a
	 * {@value #THUMBNAIL_SIZE}x{@value #THUMBNAIL_SIZE} square, and writes it as a
	 * JPEG to {@code dest}.
	 * Transparent source images (e.g. PNG with alpha) are composited over a white background
	 * before encoding, since JPEG does not support transparency.
	 *
	 * @param source path to the source image file
	 * @param dest   path where the JPEG thumbnail will be written
	 * @return {@code true} if the thumbnail was written successfully, {@code false} otherwise
	 */
	private boolean writeThumbnail(Path source, Path dest) {
		try {
			BufferedImage src = ImageIO.read(source.toFile());
			if (src == null) return false;

			int srcW = src.getWidth();
			int srcH = src.getHeight();

			double scale = (double) THUMBNAIL_SIZE / Math.min(srcW, srcH);
			int scaledW = (int) Math.ceil(srcW * scale);
			int scaledH = (int) Math.ceil(srcH * scale);

			BufferedImage scaled = new BufferedImage(scaledW, scaledH, BufferedImage.TYPE_INT_RGB);
			Graphics2D g = scaled.createGraphics();
			g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
			g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			g.setColor(Color.WHITE);
			g.fillRect(0, 0, scaledW, scaledH);
			g.drawImage(src, 0, 0, scaledW, scaledH, null);
			g.dispose();

			int x = (scaledW - THUMBNAIL_SIZE) / 2;
			int y = (scaledH - THUMBNAIL_SIZE) / 2;

			BufferedImage out = new BufferedImage(THUMBNAIL_SIZE, THUMBNAIL_SIZE, BufferedImage.TYPE_INT_RGB);
			Graphics2D g2 = out.createGraphics();
			g2.drawImage(scaled.getSubimage(x, y, THUMBNAIL_SIZE, THUMBNAIL_SIZE), 0, 0, null);
			g2.dispose();

			return ImageIO.write(out, "jpg", dest.toFile());
		} catch (IOException e) {
			return false;
		}
	}
}
