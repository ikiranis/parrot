package com.parrottunes.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class PlaylistRequest {
    
    @NotBlank
    @Size(max = 50)
    private String playlistName;
    
    private String playlistData; // For smart playlists

    // Getters and Setters
    public String getPlaylistName() {
        return playlistName;
    }

    public void setPlaylistName(String playlistName) {
        this.playlistName = playlistName;
    }

    public String getPlaylistData() {
        return playlistData;
    }

    public void setPlaylistData(String playlistData) {
        this.playlistData = playlistData;
    }
}
