package com.parrottunes.repository;

import com.parrottunes.entity.ManualPlaylist;
import com.parrottunes.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ManualPlaylistRepository extends JpaRepository<ManualPlaylist, Long> {
    
    List<ManualPlaylist> findByUser(User user);
    
    List<ManualPlaylist> findByUserOrderByCreatedAtDesc(User user);
    
    List<ManualPlaylist> findByPlaylistNameContainingIgnoreCase(String name);
}
