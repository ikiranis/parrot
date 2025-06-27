package com.parrottunes.repository;

import com.parrottunes.entity.MusicTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MusicTagRepository extends JpaRepository<MusicTag, Long> {
    
    Optional<MusicTag> findByFileId(Long fileId);
    
    List<MusicTag> findByArtist(String artist);
    
    List<MusicTag> findByAlbum(String album);
    
    List<MusicTag> findByGenre(String genre);
    
    @Query("SELECT t FROM MusicTag t WHERE t.songYear = :year")
    List<MusicTag> findBySongYear(@Param("year") Integer year);
    
    @Query("SELECT t FROM MusicTag t ORDER BY t.playCount DESC")
    List<MusicTag> findMostPlayed();
    
    @Query("SELECT t FROM MusicTag t WHERE t.rating >= :minRating ORDER BY t.rating DESC")
    List<MusicTag> findHighlyRated(@Param("minRating") Integer minRating);
    
    @Query("SELECT t FROM MusicTag t ORDER BY t.dateAdded DESC")
    List<MusicTag> findRecentlyAdded();
    
    @Query("SELECT t FROM MusicTag t WHERE t.dateLastPlayed IS NOT NULL ORDER BY t.dateLastPlayed DESC")
    List<MusicTag> findRecentlyPlayed();
}
