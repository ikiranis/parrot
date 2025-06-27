package com.parrottunes.entity;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "playlist_items")
public class PlaylistItem extends BaseEntity {
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "playlist_id", nullable = false)
    @JsonIgnore
    private ManualPlaylist playlist;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id", nullable = false)
    private MediaFile file;
    
    @Column(name = "order_index")
    private Integer orderIndex;

    // Constructors
    public PlaylistItem() {}

    public PlaylistItem(ManualPlaylist playlist, MediaFile file, Integer orderIndex) {
        this.playlist = playlist;
        this.file = file;
        this.orderIndex = orderIndex;
    }

    // Getters and Setters
    public ManualPlaylist getPlaylist() {
        return playlist;
    }

    public void setPlaylist(ManualPlaylist playlist) {
        this.playlist = playlist;
    }

    public MediaFile getFile() {
        return file;
    }

    public void setFile(MediaFile file) {
        this.file = file;
    }

    public Integer getOrderIndex() {
        return orderIndex;
    }

    public void setOrderIndex(Integer orderIndex) {
        this.orderIndex = orderIndex;
    }
}
