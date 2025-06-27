package com.parrottunes.repository;

import com.parrottunes.entity.MediaFile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MediaFileRepository extends JpaRepository<MediaFile, Long> {
    
    Optional<MediaFile> findByHash(String hash);
    
    List<MediaFile> findByKind(MediaFile.MediaKind kind);
    
    @Query("SELECT f FROM MediaFile f JOIN f.musicTag t WHERE " +
           "LOWER(t.songName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(t.artist) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(t.album) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(t.genre) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<MediaFile> searchByMetadata(@Param("query") String query, Pageable pageable);
    
    @Query("SELECT f FROM MediaFile f JOIN f.musicTag t WHERE f.kind = :kind AND (" +
           "LOWER(t.songName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(t.artist) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(t.album) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(t.genre) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<MediaFile> searchByMetadataAndKind(@Param("query") String query, 
                                           @Param("kind") MediaFile.MediaKind kind, 
                                           Pageable pageable);
    
    @Query("SELECT f FROM MediaFile f JOIN f.musicTag t WHERE t.artist = :artist")
    List<MediaFile> findByArtist(@Param("artist") String artist);
    
    @Query("SELECT f FROM MediaFile f JOIN f.musicTag t WHERE t.album = :album")
    List<MediaFile> findByAlbum(@Param("album") String album);
    
    @Query("SELECT f FROM MediaFile f JOIN f.musicTag t WHERE t.genre = :genre")
    List<MediaFile> findByGenre(@Param("genre") String genre);
    
    @Query("SELECT DISTINCT t.artist FROM MediaFile f JOIN f.musicTag t WHERE t.artist IS NOT NULL ORDER BY t.artist")
    List<String> findAllArtists();
    
    @Query("SELECT DISTINCT t.album FROM MediaFile f JOIN f.musicTag t WHERE t.album IS NOT NULL ORDER BY t.album")
    List<String> findAllAlbums();
    
    @Query("SELECT DISTINCT t.genre FROM MediaFile f JOIN f.musicTag t WHERE t.genre IS NOT NULL ORDER BY t.genre")
    List<String> findAllGenres();
}
