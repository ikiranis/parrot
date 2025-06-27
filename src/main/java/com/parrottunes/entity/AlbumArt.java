package com.parrottunes.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "album_arts")
public class AlbumArt extends BaseEntity {
    
    @NotBlank
    @Size(max = 255)
    @Column(name = "path")
    private String path;
    
    @NotBlank
    @Size(max = 255)
    @Column(name = "filename")
    private String filename;
    
    @Size(max = 100)
    @Column(name = "hash")
    private String hash;

    // Constructors
    public AlbumArt() {}

    public AlbumArt(String path, String filename, String hash) {
        this.path = path;
        this.filename = filename;
        this.hash = hash;
    }

    // Getters and Setters
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

    public String getFullPath() {
        return path + filename;
    }
}
