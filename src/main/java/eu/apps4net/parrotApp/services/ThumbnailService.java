package eu.apps4net.parrotApp.services;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import eu.apps4net.parrotApp.models.Folder;
import eu.apps4net.parrotApp.models.Thumbnail;
import eu.apps4net.parrotApp.models.ThumbnailType;
import eu.apps4net.parrotApp.repositories.FolderRepository;
import eu.apps4net.parrotApp.repositories.ThumbnailRepository;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
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

	/** Maximum number of folders processed in a single generation run. */
	private static final int FOLDER_BATCH_SIZE = 1000;

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

	/**
	 * Constructs a new {@code ThumbnailService}.
	 *
	 * @param thumbnailRepository the thumbnail repository
	 * @param folderRepository    the folder repository
	 */
	public ThumbnailService(ThumbnailRepository thumbnailRepository, FolderRepository folderRepository) {
		this.thumbnailRepository = thumbnailRepository;
		this.folderRepository = folderRepository;
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
	 * Generates thumbnails for up to {@value #FOLDER_BATCH_SIZE} folders that have no
	 * thumbnail yet, processing shallowest folders first (level 1, then 2, etc.).
	 *
	 * For each folder a random image file from its directory is chosen as the source.
	 * A 150x150-pixel centre-crop JPEG is written to
	 * {@code thumbnails/YYYY/MM/DD/HH/<folderId>_<nanos>.jpg}, a {@link Thumbnail} record
	 * is persisted, and the folder is updated to reference it.
	 * Folders whose directory is inaccessible or contains no image files are skipped silently.
	 */
	public void generateFolderThumbnails() {
		List<Folder> folders = folderRepository.findFoldersWithoutThumbnailOrderedByLevel(
				PageRequest.of(0, FOLDER_BATCH_SIZE));

		if (folders.isEmpty()) return;

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
			return;
		}

		String datePath = now.getYear() + "/" +
				String.format("%02d", now.getMonthValue()) + "/" +
				String.format("%02d", now.getDayOfMonth()) + "/" +
				String.format("%02d", now.getHour());

		for (Folder folder : folders) {
			try {
				processFolder(folder, outputDir, datePath);
			} catch (Exception e) {
				System.err.println("ThumbnailService: error generating thumbnail for folder "
						+ folder.getId() + " — " + e.getMessage());
			}
		}
	}

	/**
	 * Picks a random image from the folder, writes the thumbnail file, persists the
	 * {@link Thumbnail} entity, and links it to the folder.
	 *
	 * @param folder    the folder to generate a thumbnail for
	 * @param outputDir absolute path to the directory where the file will be written
	 * @param datePath  relative date path segment used to build the stored thumbnail path
	 * @throws IOException if the folder directory cannot be listed
	 */
	private void processFolder(Folder folder, Path outputDir, String datePath) throws IOException {
		Path folderAbsPath = Paths.get(folder.getLibraryFolder().getPath()).resolve(folder.getPath());

		if (!Files.isDirectory(folderAbsPath)) return;

		Path sourceImage = pickRandomImage(folderAbsPath);
		if (sourceImage == null) return;

		String filename = folder.getId() + "_" + System.nanoTime() + ".jpg";
		Path outputPath = outputDir.resolve(filename);

		if (!writeThumbnail(sourceImage, outputPath)) return;

		Thumbnail thumbnail = thumbnailRepository.save(new Thumbnail(datePath + "/" + filename, ThumbnailType.FOLDER));
		folder.setThumbnail(thumbnail);
		folderRepository.save(folder);
	}

	/**
	 * Lists the direct files of {@code dir} and returns one image file chosen at random,
	 * or {@code null} if the directory contains no supported image files.
	 *
	 * @param dir the directory to search
	 * @return a randomly selected image {@link Path}, or {@code null} if none found
	 * @throws IOException if the directory cannot be listed
	 */
	private Path pickRandomImage(Path dir) throws IOException {
		try (Stream<Path> files = Files.list(dir)) {
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
