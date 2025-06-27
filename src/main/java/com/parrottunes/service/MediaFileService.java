package com.parrottunes.service;

import com.parrottunes.entity.MediaFile;
import com.parrottunes.entity.MusicTag;
import com.parrottunes.repository.MediaFileRepository;
import com.parrottunes.repository.MusicTagRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class MediaFileService {

    @Autowired
    private MediaFileRepository mediaFileRepository;

    @Autowired
    private MusicTagRepository musicTagRepository;

    public List<MediaFile> getAllFiles() {
        return mediaFileRepository.findAll();
    }

    public Page<MediaFile> getAllFiles(Pageable pageable) {
        return mediaFileRepository.findAll(pageable);
    }

    public Optional<MediaFile> getFileById(Long id) {
        return mediaFileRepository.findById(id);
    }

    public Optional<MediaFile> getFileByHash(String hash) {
        return mediaFileRepository.findByHash(hash);
    }

    public List<MediaFile> getFilesByKind(MediaFile.MediaKind kind) {
        return mediaFileRepository.findByKind(kind);
    }

    public Page<MediaFile> searchFiles(String query, Pageable pageable) {
        return mediaFileRepository.searchByMetadata(query, pageable);
    }

    public Page<MediaFile> searchFilesByKind(String query, MediaFile.MediaKind kind, Pageable pageable) {
        return mediaFileRepository.searchByMetadataAndKind(query, kind, pageable);
    }

    public List<MediaFile> getFilesByArtist(String artist) {
        return mediaFileRepository.findByArtist(artist);
    }

    public List<MediaFile> getFilesByAlbum(String album) {
        return mediaFileRepository.findByAlbum(album);
    }

    public List<MediaFile> getFilesByGenre(String genre) {
        return mediaFileRepository.findByGenre(genre);
    }

    public List<String> getAllArtists() {
        return mediaFileRepository.findAllArtists();
    }

    public List<String> getAllAlbums() {
        return mediaFileRepository.findAllAlbums();
    }

    public List<String> getAllGenres() {
        return mediaFileRepository.findAllGenres();
    }

    public MediaFile saveFile(MediaFile file) {
        return mediaFileRepository.save(file);
    }

    public MediaFile createFileWithTags(String path, String filename, String hash, 
                                       MediaFile.MediaKind kind, MusicTag musicTag) {
        MediaFile file = new MediaFile(path, filename, hash, kind);
        file = mediaFileRepository.save(file);
        
        if (musicTag != null) {
            musicTag.setFile(file);
            musicTag.setDateAdded(LocalDateTime.now());
            musicTagRepository.save(musicTag);
            file.setMusicTag(musicTag);
        }
        
        return file;
    }

    public void deleteFile(Long id) {
        mediaFileRepository.deleteById(id);
    }

    public boolean existsById(Long id) {
        return mediaFileRepository.existsById(id);
    }

    public long getFileCount() {
        return mediaFileRepository.count();
    }

    public void incrementPlayCount(Long fileId) {
        Optional<MusicTag> tagOpt = musicTagRepository.findByFileId(fileId);
        if (tagOpt.isPresent()) {
            MusicTag tag = tagOpt.get();
            tag.setPlayCount(tag.getPlayCount() != null ? tag.getPlayCount() + 1 : 1);
            tag.setDateLastPlayed(LocalDateTime.now());
            musicTagRepository.save(tag);
        }
    }

    public void updateRating(Long fileId, Integer rating) {
        Optional<MusicTag> tagOpt = musicTagRepository.findByFileId(fileId);
        if (tagOpt.isPresent()) {
            MusicTag tag = tagOpt.get();
            tag.setRating(rating);
            musicTagRepository.save(tag);
        }
    }

    public List<MusicTag> getMostPlayed() {
        return musicTagRepository.findMostPlayed();
    }

    public List<MusicTag> getRecentlyAdded() {
        return musicTagRepository.findRecentlyAdded();
    }

    public List<MusicTag> getRecentlyPlayed() {
        return musicTagRepository.findRecentlyPlayed();
    }

    public List<MusicTag> getHighlyRated(Integer minRating) {
        return musicTagRepository.findHighlyRated(minRating);
    }
}
