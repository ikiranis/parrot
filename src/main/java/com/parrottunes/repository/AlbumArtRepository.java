package com.parrottunes.repository;

import com.parrottunes.entity.AlbumArt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AlbumArtRepository extends JpaRepository<AlbumArt, Long> {
    
    Optional<AlbumArt> findByHash(String hash);
}
