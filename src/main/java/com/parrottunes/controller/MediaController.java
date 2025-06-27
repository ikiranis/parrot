package com.parrottunes.controller;

import com.parrottunes.dto.ApiResponse;
import com.parrottunes.dto.MediaFileResponse;
import com.parrottunes.entity.MediaFile;
import com.parrottunes.service.MediaFileService;
import com.parrottunes.service.CustomUserDetailsService.UserPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/media")
public class MediaController {

    @Autowired
    private MediaFileService mediaFileService;

    @GetMapping("/files")
    public ResponseEntity<?> getAllFiles(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) String kind) {
        
        Sort sort = sortDir.equalsIgnoreCase("desc") ? 
                Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<MediaFile> files;
        if (kind != null) {
            MediaFile.MediaKind mediaKind = MediaFile.MediaKind.valueOf(kind.toUpperCase());
            files = mediaFileService.getFilesByKind(mediaKind).stream()
                    .collect(Collectors.collectingAndThen(
                            Collectors.toList(),
                            list -> new org.springframework.data.domain.PageImpl<>(
                                    list.subList(Math.min(page * size, list.size()),
                                            Math.min((page + 1) * size, list.size())),
                                    pageable, list.size())
                    ));
        } else {
            files = mediaFileService.getAllFiles(pageable);
        }
        
        Page<MediaFileResponse> response = files.map(MediaFileResponse::new);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/files/{id}")
    public ResponseEntity<?> getFileById(@PathVariable Long id) {
        Optional<MediaFile> file = mediaFileService.getFileById(id);
        if (file.isPresent()) {
            return ResponseEntity.ok(new MediaFileResponse(file.get()));
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchFiles(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String kind) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<MediaFile> files;
        
        if (kind != null) {
            MediaFile.MediaKind mediaKind = MediaFile.MediaKind.valueOf(kind.toUpperCase());
            files = mediaFileService.searchFilesByKind(query, mediaKind, pageable);
        } else {
            files = mediaFileService.searchFiles(query, pageable);
        }
        
        Page<MediaFileResponse> response = files.map(MediaFileResponse::new);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/artists")
    public ResponseEntity<?> getAllArtists() {
        List<String> artists = mediaFileService.getAllArtists();
        return ResponseEntity.ok(new ApiResponse(true, "Artists retrieved successfully", artists));
    }

    @GetMapping("/albums")
    public ResponseEntity<?> getAllAlbums() {
        List<String> albums = mediaFileService.getAllAlbums();
        return ResponseEntity.ok(new ApiResponse(true, "Albums retrieved successfully", albums));
    }

    @GetMapping("/genres")
    public ResponseEntity<?> getAllGenres() {
        List<String> genres = mediaFileService.getAllGenres();
        return ResponseEntity.ok(new ApiResponse(true, "Genres retrieved successfully", genres));
    }

    @GetMapping("/artist/{artist}")
    public ResponseEntity<?> getFilesByArtist(@PathVariable String artist) {
        List<MediaFile> files = mediaFileService.getFilesByArtist(artist);
        List<MediaFileResponse> response = files.stream()
                .map(MediaFileResponse::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/album/{album}")
    public ResponseEntity<?> getFilesByAlbum(@PathVariable String album) {
        List<MediaFile> files = mediaFileService.getFilesByAlbum(album);
        List<MediaFileResponse> response = files.stream()
                .map(MediaFileResponse::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/genre/{genre}")
    public ResponseEntity<?> getFilesByGenre(@PathVariable String genre) {
        List<MediaFile> files = mediaFileService.getFilesByGenre(genre);
        List<MediaFileResponse> response = files.stream()
                .map(MediaFileResponse::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/files/{id}/play")
    public ResponseEntity<?> incrementPlayCount(@PathVariable Long id,
                                               @AuthenticationPrincipal UserPrincipal userPrincipal) {
        try {
            mediaFileService.incrementPlayCount(id);
            return ResponseEntity.ok(new ApiResponse(true, "Play count incremented"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Error updating play count: " + e.getMessage()));
        }
    }

    @PutMapping("/files/{id}/rating")
    public ResponseEntity<?> updateRating(@PathVariable Long id,
                                         @RequestParam Integer rating,
                                         @AuthenticationPrincipal UserPrincipal userPrincipal) {
        try {
            if (rating < 0 || rating > 100) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse(false, "Rating must be between 0 and 100"));
            }
            mediaFileService.updateRating(id, rating);
            return ResponseEntity.ok(new ApiResponse(true, "Rating updated successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Error updating rating: " + e.getMessage()));
        }
    }

    @DeleteMapping("/files/{id}")
    public ResponseEntity<?> deleteFile(@PathVariable Long id,
                                       @AuthenticationPrincipal UserPrincipal userPrincipal) {
        try {
            if (!mediaFileService.existsById(id)) {
                return ResponseEntity.notFound().build();
            }
            mediaFileService.deleteFile(id);
            return ResponseEntity.ok(new ApiResponse(true, "File deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Error deleting file: " + e.getMessage()));
        }
    }

    @GetMapping("/stats")
    public ResponseEntity<?> getMediaStats() {
        long totalFiles = mediaFileService.getFileCount();
        long musicFiles = mediaFileService.getFilesByKind(MediaFile.MediaKind.MUSIC).size();
        long videoFiles = mediaFileService.getFilesByKind(MediaFile.MediaKind.MUSIC_VIDEO).size();
        
        return ResponseEntity.ok(new ApiResponse(true, "Media statistics", 
                java.util.Map.of(
                        "totalFiles", totalFiles,
                        "musicFiles", musicFiles,
                        "videoFiles", videoFiles,
                        "artists", mediaFileService.getAllArtists().size(),
                        "albums", mediaFileService.getAllAlbums().size(),
                        "genres", mediaFileService.getAllGenres().size()
                )));
    }
}
