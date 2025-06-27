package com.parrottunes.repository;

import com.parrottunes.entity.SmartPlaylist;
import com.parrottunes.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SmartPlaylistRepository extends JpaRepository<SmartPlaylist, Long> {
    
    List<SmartPlaylist> findByUser(User user);
    
    List<SmartPlaylist> findByUserOrderByCreatedAtDesc(User user);
    
    List<SmartPlaylist> findByPlaylistNameContainingIgnoreCase(String name);
}
