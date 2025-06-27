package com.parrottunes.controller;

import com.parrottunes.dto.ApiResponse;
import com.parrottunes.dto.PlaylistRequest;
import com.parrottunes.entity.ManualPlaylist;
import com.parrottunes.entity.PlaylistItem;
import com.parrottunes.entity.User;
import com.parrottunes.repository.UserRepository;
import com.parrottunes.service.PlaylistService;
import com.parrottunes.service.CustomUserDetailsService.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/playlists")
public class PlaylistController {

    @Autowired
    private PlaylistService playlistService;

    @Autowired
    private UserRepository userRepository;

    @GetMapping
    public ResponseEntity<?> getUserPlaylists(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        try {
            Optional<User> userOpt = userRepository.findById(userPrincipal.getId());
            if (userOpt.isPresent()) {
                List<ManualPlaylist> playlists = playlistService.getUserPlaylists(userOpt.get());
                return ResponseEntity.ok(new ApiResponse(true, "Playlists retrieved successfully", playlists));
            }
            return ResponseEntity.badRequest().body(new ApiResponse(false, "User not found"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Error retrieving playlists: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getPlaylistById(@PathVariable Long id) {
        try {
            Optional<ManualPlaylist> playlist = playlistService.getPlaylistById(id);
            if (playlist.isPresent()) {
                return ResponseEntity.ok(new ApiResponse(true, "Playlist retrieved successfully", playlist.get()));
            }
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Error retrieving playlist: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}/items")
    public ResponseEntity<?> getPlaylistItems(@PathVariable Long id) {
        try {
            List<PlaylistItem> items = playlistService.getPlaylistItems(id);
            return ResponseEntity.ok(new ApiResponse(true, "Playlist items retrieved successfully", items));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Error retrieving playlist items: " + e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<?> createPlaylist(@Valid @RequestBody PlaylistRequest request,
                                          @AuthenticationPrincipal UserPrincipal userPrincipal) {
        try {
            Optional<User> userOpt = userRepository.findById(userPrincipal.getId());
            if (userOpt.isPresent()) {
                ManualPlaylist playlist = playlistService.createPlaylist(request.getPlaylistName(), userOpt.get());
                return ResponseEntity.ok(new ApiResponse(true, "Playlist created successfully", playlist));
            }
            return ResponseEntity.badRequest().body(new ApiResponse(false, "User not found"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Error creating playlist: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updatePlaylist(@PathVariable Long id,
                                          @Valid @RequestBody PlaylistRequest request,
                                          @AuthenticationPrincipal UserPrincipal userPrincipal) {
        try {
            ManualPlaylist playlist = playlistService.updatePlaylist(id, request.getPlaylistName());
            return ResponseEntity.ok(new ApiResponse(true, "Playlist updated successfully", playlist));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Error updating playlist: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletePlaylist(@PathVariable Long id,
                                          @AuthenticationPrincipal UserPrincipal userPrincipal) {
        try {
            playlistService.deletePlaylist(id);
            return ResponseEntity.ok(new ApiResponse(true, "Playlist deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Error deleting playlist: " + e.getMessage()));
        }
    }

    @PostMapping("/{id}/items")
    public ResponseEntity<?> addToPlaylist(@PathVariable Long id,
                                         @RequestParam Long fileId,
                                         @AuthenticationPrincipal UserPrincipal userPrincipal) {
        try {
            PlaylistItem item = playlistService.addToPlaylist(id, fileId);
            return ResponseEntity.ok(new ApiResponse(true, "File added to playlist successfully", item));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Error adding file to playlist: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}/items")
    public ResponseEntity<?> removeFromPlaylist(@PathVariable Long id,
                                              @RequestParam Long fileId,
                                              @AuthenticationPrincipal UserPrincipal userPrincipal) {
        try {
            playlistService.removeFromPlaylist(id, fileId);
            return ResponseEntity.ok(new ApiResponse(true, "File removed from playlist successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Error removing file from playlist: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}/items/{itemId}/order")
    public ResponseEntity<?> reorderPlaylistItem(@PathVariable Long id,
                                                @PathVariable Long itemId,
                                                @RequestParam Integer newOrder,
                                                @AuthenticationPrincipal UserPrincipal userPrincipal) {
        try {
            playlistService.reorderPlaylistItem(id, itemId, newOrder);
            return ResponseEntity.ok(new ApiResponse(true, "Playlist item reordered successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Error reordering playlist item: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}/count")
    public ResponseEntity<?> getPlaylistItemCount(@PathVariable Long id) {
        try {
            Long count = playlistService.getPlaylistItemCount(id);
            return ResponseEntity.ok(new ApiResponse(true, "Playlist item count retrieved successfully", count));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Error getting playlist item count: " + e.getMessage()));
        }
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchPlaylists(@RequestParam String query) {
        try {
            List<ManualPlaylist> playlists = playlistService.searchPlaylists(query);
            return ResponseEntity.ok(new ApiResponse(true, "Playlists found", playlists));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Error searching playlists: " + e.getMessage()));
        }
    }
}
