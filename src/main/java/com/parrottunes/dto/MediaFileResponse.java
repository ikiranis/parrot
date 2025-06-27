package com.parrottunes.dto;

import com.parrottunes.entity.MediaFile;

import java.time.LocalDateTime;

public class MediaFileResponse {
    
    private Long id;
    private String path;
    private String filename;
    private String hash;
    private MediaFile.MediaKind kind;
    private LocalDateTime createdAt;
    private MusicTagResponse musicTag;

    // Constructors
    public MediaFileResponse() {}

    public MediaFileResponse(MediaFile file) {
        this.id = file.getId();
        this.path = file.getPath();
        this.filename = file.getFilename();
        this.hash = file.getHash();
        this.kind = file.getKind();
        this.createdAt = file.getCreatedAt();
        if (file.getMusicTag() != null) {
            this.musicTag = new MusicTagResponse(file.getMusicTag());
        }
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public MediaFile.MediaKind getKind() {
        return kind;
    }

    public void setKind(MediaFile.MediaKind kind) {
        this.kind = kind;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public MusicTagResponse getMusicTag() {
        return musicTag;
    }

    public void setMusicTag(MusicTagResponse musicTag) {
        this.musicTag = musicTag;
    }

    public String getFullPath() {
        return path + filename;
    }
}
