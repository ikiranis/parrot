package com.parrottunes.repository;

import com.parrottunes.entity.QueueItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QueueItemRepository extends JpaRepository<QueueItem, Long> {
    
    List<QueueItem> findAllByOrderByCreatedAtAsc();
    
    List<QueueItem> findAllByOrderByCreatedAtDesc();
}
