package com.parrottunes.controller;

import com.parrottunes.entity.MediaFile;
import com.parrottunes.service.MediaFileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

@RestController
@RequestMapping("/media")
public class StreamController {

    @Autowired
    private MediaFileService mediaFileService;

    @Value("${app.media.upload-dir}")
    private String uploadDir;

    @GetMapping("/stream/{id}")
    public ResponseEntity<Resource> streamFile(@PathVariable Long id,
                                              @RequestHeader(value = "Range", required = false) String rangeHeader,
                                              HttpServletRequest request) {
        try {
            Optional<MediaFile> fileOpt = mediaFileService.getFileById(id);
            if (!fileOpt.isPresent()) {
                return ResponseEntity.notFound().build();
            }

            MediaFile mediaFile = fileOpt.get();
            Path filePath = Paths.get(mediaFile.getFullPath());
            
            if (!Files.exists(filePath)) {
                return ResponseEntity.notFound().build();
            }

            File file = filePath.toFile();
            long fileLength = file.length();
            
            // Detect content type
            String contentType = Files.probeContentType(filePath);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            Resource resource = new FileSystemResource(file);
            
            // Handle range requests for video/audio streaming
            if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
                String[] ranges = rangeHeader.substring(6).split("-");
                long start = Long.parseLong(ranges[0]);
                long end = ranges.length > 1 && !ranges[1].isEmpty() ? 
                          Long.parseLong(ranges[1]) : fileLength - 1;
                
                if (start >= fileLength || end >= fileLength || start > end) {
                    return ResponseEntity.status(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE).build();
                }
                
                long contentLength = end - start + 1;
                
                HttpHeaders headers = new HttpHeaders();
                headers.add("Content-Type", contentType);
                headers.add("Accept-Ranges", "bytes");
                headers.add("Content-Range", String.format("bytes %d-%d/%d", start, end, fileLength));
                headers.add("Content-Length", String.valueOf(contentLength));
                
                // For partial content, we'd need to implement a custom Resource
                // For now, return the full file with partial content status
                return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                        .headers(headers)
                        .body(resource);
            } else {
                // Return full file
                HttpHeaders headers = new HttpHeaders();
                headers.add("Content-Type", contentType);
                headers.add("Content-Length", String.valueOf(fileLength));
                headers.add("Accept-Ranges", "bytes");
                
                return ResponseEntity.ok()
                        .headers(headers)
                        .body(resource);
            }
            
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/image/{id}")
    public ResponseEntity<Resource> getAlbumArt(@PathVariable Long id) {
        try {
            // This would be implemented when we have album art functionality
            // For now, return a placeholder or 404
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/download/{id}")
    public ResponseEntity<Resource> downloadFile(@PathVariable Long id) {
        try {
            Optional<MediaFile> fileOpt = mediaFileService.getFileById(id);
            if (!fileOpt.isPresent()) {
                return ResponseEntity.notFound().build();
            }

            MediaFile mediaFile = fileOpt.get();
            Path filePath = Paths.get(mediaFile.getFullPath());
            
            if (!Files.exists(filePath)) {
                return ResponseEntity.notFound().build();
            }

            Resource resource = new FileSystemResource(filePath.toFile());
            
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, 
                       "attachment; filename=\"" + mediaFile.getFilename() + "\"");
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(resource);
                    
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
