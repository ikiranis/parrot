package com.parrottunes.dto;

import com.parrottunes.entity.MusicTag;

import java.time.LocalDateTime;

public class MusicTagResponse {
    
    private Long id;
    private String songName;
    private String artist;
    private String genre;
    private LocalDateTime dateAdded;
    private Integer playCount;
    private LocalDateTime dateLastPlayed;
    private Integer rating;
    private String album;
    private Long albumArtworkId;
    private Integer videoWidth;
    private Integer videoHeight;
    private Long fileSize;
    private Float trackTime;
    private Integer songYear;
    private Boolean live;

    // Constructors
    public MusicTagResponse() {}

    public MusicTagResponse(MusicTag tag) {
        this.id = tag.getId();
        this.songName = tag.getSongName();
        this.artist = tag.getArtist();
        this.genre = tag.getGenre();
        this.dateAdded = tag.getDateAdded();
        this.playCount = tag.getPlayCount();
        this.dateLastPlayed = tag.getDateLastPlayed();
        this.rating = tag.getRating();
        this.album = tag.getAlbum();
        this.albumArtworkId = tag.getAlbumArtworkId();
        this.videoWidth = tag.getVideoWidth();
        this.videoHeight = tag.getVideoHeight();
        this.fileSize = tag.getFileSize();
        this.trackTime = tag.getTrackTime();
        this.songYear = tag.getSongYear();
        this.live = tag.getLive();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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
