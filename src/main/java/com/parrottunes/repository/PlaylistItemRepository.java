package com.parrottunes.repository;

import com.parrottunes.entity.PlaylistItem;
import com.parrottunes.entity.ManualPlaylist;
import com.parrottunes.entity.MediaFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlaylistItemRepository extends JpaRepository<PlaylistItem, Long> {
    
    List<PlaylistItem> findByPlaylistOrderByOrderIndex(ManualPlaylist playlist);
    
    List<PlaylistItem> findByPlaylistOrderByOrderIndexDesc(ManualPlaylist playlist);
    
    Optional<PlaylistItem> findByPlaylistAndFile(ManualPlaylist playlist, MediaFile file);
    
    @Query("SELECT pi FROM PlaylistItem pi WHERE pi.playlist = :playlist AND pi.file = :file")
    Optional<PlaylistItem> findByPlaylistAndFileId(@Param("playlist") ManualPlaylist playlist, 
                                                  @Param("file") MediaFile file);
    
    @Query("SELECT COUNT(pi) FROM PlaylistItem pi WHERE pi.playlist = :playlist")
    Long countByPlaylist(@Param("playlist") ManualPlaylist playlist);
    
    @Query("SELECT MAX(pi.orderIndex) FROM PlaylistItem pi WHERE pi.playlist = :playlist")
    Integer findMaxOrderIndex(@Param("playlist") ManualPlaylist playlist);
}
