package com.parrottunes.service;

import com.parrottunes.entity.MediaFile;
import com.parrottunes.entity.MusicTag;
import org.apache.tika.Tika;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class FileUploadService {

    @Autowired
    private MediaFileService mediaFileService;

    @Value("${app.media.upload-dir}")
    private String uploadDir;

    private final Tika tika = new Tika();

    public MediaFile uploadFile(MultipartFile file) throws IOException {
        // Validate file
        if (file.isEmpty()) {
            throw new RuntimeException("Please select a file to upload");
        }

        // Check if it's a media file
        String contentType = tika.detect(file.getInputStream());
        if (!isMediaFile(contentType)) {
            throw new RuntimeException("File type not supported. Please upload audio or video files only.");
        }

        // Create upload directory if it doesn't exist
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Generate unique filename
        String originalFilename = file.getOriginalFilename();
        String fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        String uniqueFilename = UUID.randomUUID().toString() + fileExtension;
        
        // Save file
        Path filePath = uploadPath.resolve(uniqueFilename);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        // Calculate file hash
        String fileHash = calculateFileHash(filePath.toFile());

        // Check if file already exists
        if (mediaFileService.getFileByHash(fileHash).isPresent()) {
            // Delete the uploaded file since it's a duplicate
            Files.deleteIfExists(filePath);
            throw new RuntimeException("This file already exists in the library");
        }

        // Determine media kind
        MediaFile.MediaKind kind = contentType.startsWith("video/") ? 
                MediaFile.MediaKind.MUSIC_VIDEO : MediaFile.MediaKind.MUSIC;

        // Extract metadata
        MusicTag musicTag = extractMetadata(filePath.toFile());
        if (musicTag != null) {
            musicTag.setDateAdded(LocalDateTime.now());
            musicTag.setFileSize(file.getSize());
        }

        // Create and save media file
        MediaFile mediaFile = mediaFileService.createFileWithTags(
                uploadDir + "/", 
                uniqueFilename, 
                fileHash, 
                kind, 
                musicTag
        );

        return mediaFile;
    }

    private boolean isMediaFile(String contentType) {
        return contentType != null && (
                contentType.startsWith("audio/") || 
                contentType.startsWith("video/")
        );
    }

    private String calculateFileHash(File file) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (FileInputStream fis = new FileInputStream(file)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    digest.update(buffer, 0, bytesRead);
                }
            }
            
            byte[] hashBytes = digest.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IOException("Error calculating file hash", e);
        }
    }

    private MusicTag extractMetadata(File file) {
        try {
            AutoDetectParser parser = new AutoDetectParser();
            BodyContentHandler handler = new BodyContentHandler();
            Metadata metadata = new Metadata();
            ParseContext context = new ParseContext();

            try (FileInputStream inputstream = new FileInputStream(file)) {
                parser.parse(inputstream, handler, metadata, context);
            }

            MusicTag musicTag = new MusicTag();
            
            // Extract basic metadata
            musicTag.setSongName(metadata.get("title"));
            musicTag.setArtist(metadata.get("xmpDM:artist"));
            musicTag.setAlbum(metadata.get("xmpDM:album"));
            musicTag.setGenre(metadata.get("xmpDM:genre"));
            
            // Try alternative metadata fields if the first ones are null
            if (musicTag.getArtist() == null) {
                musicTag.setArtist(metadata.get("creator"));
            }
            if (musicTag.getSongName() == null) {
                musicTag.setSongName(file.getName());
            }

            // Extract year
            String year = metadata.get("xmpDM:releaseDate");
            if (year != null && year.length() >= 4) {
                try {
                    musicTag.setSongYear(Integer.parseInt(year.substring(0, 4)));
                } catch (NumberFormatException ignored) {}
            }

            // Extract duration
            String duration = metadata.get("xmpDM:duration");
            if (duration != null) {
                try {
                    musicTag.setTrackTime(Float.parseFloat(duration));
                } catch (NumberFormatException ignored) {}
            }

            // Extract video dimensions if it's a video
            String width = metadata.get("tiff:ImageWidth");
            String height = metadata.get("tiff:ImageLength");
            if (width != null && height != null) {
                try {
                    musicTag.setVideoWidth(Integer.parseInt(width));
                    musicTag.setVideoHeight(Integer.parseInt(height));
                } catch (NumberFormatException ignored) {}
            }

            return musicTag;
        } catch (Exception e) {
            // If metadata extraction fails, create a basic tag with filename
            MusicTag basicTag = new MusicTag();
            basicTag.setSongName(file.getName());
            return basicTag;
        }
    }
}
