package com.parrottunes.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "smart_playlists")
public class SmartPlaylist extends BaseEntity {
    
    @NotBlank
    @Size(max = 50)
    @Column(name = "playlist_name", nullable = false)
    private String playlistName;
    
    @Column(name = "playlist_data", length = 4000)
    private String playlistData;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    // Constructors
    public SmartPlaylist() {}

    public SmartPlaylist(String playlistName, String playlistData, User user) {
        this.playlistName = playlistName;
        this.playlistData = playlistData;
        this.user = user;
    }

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

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
