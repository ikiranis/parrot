package com.parrottunes.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "manual_playlists")
public class ManualPlaylist extends BaseEntity {
    
    @Size(max = 20)
    @Column(name = "table_name")
    private String tableName;
    
    @NotBlank
    @Size(max = 50)
    @Column(name = "playlist_name", nullable = false)
    private String playlistName;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;
    
    @OneToMany(mappedBy = "playlist", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<PlaylistItem> items = new HashSet<>();

    // Constructors
    public ManualPlaylist() {}

    public ManualPlaylist(String playlistName, User user) {
        this.playlistName = playlistName;
        this.user = user;
        this.tableName = "manual_" + System.currentTimeMillis();
    }

    // Getters and Setters
    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getPlaylistName() {
        return playlistName;
    }

    public void setPlaylistName(String playlistName) {
        this.playlistName = playlistName;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Set<PlaylistItem> getItems() {
        return items;
    }

    public void setItems(Set<PlaylistItem> items) {
        this.items = items;
    }
}
