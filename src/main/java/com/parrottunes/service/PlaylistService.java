package com.parrottunes.service;

import com.parrottunes.entity.ManualPlaylist;
import com.parrottunes.entity.PlaylistItem;
import com.parrottunes.entity.MediaFile;
import com.parrottunes.entity.User;
import com.parrottunes.repository.ManualPlaylistRepository;
import com.parrottunes.repository.PlaylistItemRepository;
import com.parrottunes.repository.MediaFileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class PlaylistService {

    @Autowired
    private ManualPlaylistRepository manualPlaylistRepository;

    @Autowired
    private PlaylistItemRepository playlistItemRepository;

    @Autowired
    private MediaFileRepository mediaFileRepository;

    public List<ManualPlaylist> getUserPlaylists(User user) {
        return manualPlaylistRepository.findByUserOrderByCreatedAtDesc(user);
    }

    public Optional<ManualPlaylist> getPlaylistById(Long id) {
        return manualPlaylistRepository.findById(id);
    }

    public ManualPlaylist createPlaylist(String playlistName, User user) {
        ManualPlaylist playlist = new ManualPlaylist(playlistName, user);
        return manualPlaylistRepository.save(playlist);
    }

    public ManualPlaylist updatePlaylist(Long playlistId, String playlistName) {
        Optional<ManualPlaylist> playlistOpt = manualPlaylistRepository.findById(playlistId);
        if (playlistOpt.isPresent()) {
            ManualPlaylist playlist = playlistOpt.get();
            playlist.setPlaylistName(playlistName);
            return manualPlaylistRepository.save(playlist);
        }
        throw new RuntimeException("Playlist not found with id: " + playlistId);
    }

    public void deletePlaylist(Long playlistId) {
        manualPlaylistRepository.deleteById(playlistId);
    }

    public List<PlaylistItem> getPlaylistItems(Long playlistId) {
        Optional<ManualPlaylist> playlistOpt = manualPlaylistRepository.findById(playlistId);
        if (playlistOpt.isPresent()) {
            return playlistItemRepository.findByPlaylistOrderByOrderIndex(playlistOpt.get());
        }
        throw new RuntimeException("Playlist not found with id: " + playlistId);
    }

    public PlaylistItem addToPlaylist(Long playlistId, Long fileId) {
        Optional<ManualPlaylist> playlistOpt = manualPlaylistRepository.findById(playlistId);
        Optional<MediaFile> fileOpt = mediaFileRepository.findById(fileId);
        
        if (playlistOpt.isPresent() && fileOpt.isPresent()) {
            ManualPlaylist playlist = playlistOpt.get();
            MediaFile file = fileOpt.get();
            
            // Check if file is already in playlist
            Optional<PlaylistItem> existingItem = playlistItemRepository.findByPlaylistAndFile(playlist, file);
            if (existingItem.isPresent()) {
                throw new RuntimeException("File is already in the playlist");
            }
            
            Integer maxOrder = playlistItemRepository.findMaxOrderIndex(playlist);
            Integer nextOrder = (maxOrder != null) ? maxOrder + 1 : 1;
            
            PlaylistItem item = new PlaylistItem(playlist, file, nextOrder);
            return playlistItemRepository.save(item);
        }
        throw new RuntimeException("Playlist or file not found");
    }

    public void removeFromPlaylist(Long playlistId, Long fileId) {
        Optional<ManualPlaylist> playlistOpt = manualPlaylistRepository.findById(playlistId);
        Optional<MediaFile> fileOpt = mediaFileRepository.findById(fileId);
        
        if (playlistOpt.isPresent() && fileOpt.isPresent()) {
            ManualPlaylist playlist = playlistOpt.get();
            MediaFile file = fileOpt.get();
            
            Optional<PlaylistItem> itemOpt = playlistItemRepository.findByPlaylistAndFile(playlist, file);
            if (itemOpt.isPresent()) {
                playlistItemRepository.delete(itemOpt.get());
                return;
            }
        }
        throw new RuntimeException("Playlist item not found");
    }

    public void reorderPlaylistItem(Long playlistId, Long itemId, Integer newOrder) {
        Optional<PlaylistItem> itemOpt = playlistItemRepository.findById(itemId);
        if (itemOpt.isPresent()) {
            PlaylistItem item = itemOpt.get();
            if (item.getPlaylist().getId().equals(playlistId)) {
                item.setOrderIndex(newOrder);
                playlistItemRepository.save(item);
                return;
            }
        }
        throw new RuntimeException("Playlist item not found");
    }

    public Long getPlaylistItemCount(Long playlistId) {
        Optional<ManualPlaylist> playlistOpt = manualPlaylistRepository.findById(playlistId);
        if (playlistOpt.isPresent()) {
            return playlistItemRepository.countByPlaylist(playlistOpt.get());
        }
        return 0L;
    }

    public List<ManualPlaylist> searchPlaylists(String query) {
        return manualPlaylistRepository.findByPlaylistNameContainingIgnoreCase(query);
    }
}
