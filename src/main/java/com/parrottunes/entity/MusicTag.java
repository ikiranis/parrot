package com.parrottunes.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

@Entity
@Table(name = "music_tags")
public class MusicTag extends BaseEntity {
    
    @OneToOne
    @JoinColumn(name = "file_id", referencedColumnName = "id")
    @MapsId
    private MediaFile file;
    
    @Size(max = 255)
    @Column(name = "song_name")
    private String songName;
    
    @Size(max = 255)
    @Column(name = "artist")
    private String artist;
    
    @Size(max = 20)
    @Column(name = "genre")
    private String genre;
    
    @Column(name = "date_added")
    private LocalDateTime dateAdded;
    
    @Column(name = "play_count")
    private Integer playCount = 0;
    
    @Column(name = "date_last_played")
    private LocalDateTime dateLastPlayed;
    
    @Min(0)
    @Max(100)
    @Column(name = "rating")
    private Integer rating;
    
    @Size(max = 255)
    @Column(name = "album")
    private String album;
    
    @Column(name = "album_artwork_id")
    private Long albumArtworkId;
    
    @Column(name = "video_width")
    private Integer videoWidth;
    
    @Column(name = "video_height")
    private Integer videoHeight;
    
    @Column(name = "filesize")
    private Long fileSize;
    
    @Column(name = "track_time")
    private Float trackTime;
    
    @Column(name = "song_year")
    private Integer songYear;
    
    @Column(name = "live")
    private Boolean live;

    // Constructors
    public MusicTag() {}

    // Getters and Setters
    public MediaFile getFile() {
        return file;
    }

    public void setFile(MediaFile file) {
        this.file = file;
    }

    public String getSongName() {
        return songName;
    }

    public void setSongName(String songName) {
        this.songName = songName;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public LocalDateTime getDateAdded() {
        return dateAdded;
    }

    public void setDateAdded(LocalDateTime dateAdded) {
        this.dateAdded = dateAdded;
    }

    public Integer getPlayCount() {
        return playCount;
    }

    public void setPlayCount(Integer playCount) {
        this.playCount = playCount;
    }

    public LocalDateTime getDateLastPlayed() {
        return dateLastPlayed;
    }

    public void setDateLastPlayed(LocalDateTime dateLastPlayed) {
        this.dateLastPlayed = dateLastPlayed;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public String getAlbum() {
        return album;
    }

    public void setAlbum(String album) {
        this.album = album;
    }

    public Long getAlbumArtworkId() {
        return albumArtworkId;
    }

    public void setAlbumArtworkId(Long albumArtworkId) {
        this.albumArtworkId = albumArtworkId;
    }

    public Integer getVideoWidth() {
        return videoWidth;
    }

    public void setVideoWidth(Integer videoWidth) {
        this.videoWidth = videoWidth;
    }

    public Integer getVideoHeight() {
        return videoHeight;
    }

    public void setVideoHeight(Integer videoHeight) {
        this.videoHeight = videoHeight;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public Float getTrackTime() {
        return trackTime;
    }

    public void setTrackTime(Float trackTime) {
        this.trackTime = trackTime;
    }

    public Integer getSongYear() {
        return songYear;
    }

    public void setSongYear(Integer songYear) {
        this.songYear = songYear;
    }

    public Boolean getLive() {
        return live;
    }

    public void setLive(Boolean live) {
        this.live = live;
    }
}
