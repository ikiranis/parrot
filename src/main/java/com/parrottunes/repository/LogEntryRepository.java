package com.parrottunes.repository;

import com.parrottunes.entity.LogEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface LogEntryRepository extends JpaRepository<LogEntry, Long> {
    
    Page<LogEntry> findAllByOrderByLogDateDesc(Pageable pageable);
    
    List<LogEntry> findByUserName(String userName);
    
    List<LogEntry> findByLogDateBetween(LocalDateTime start, LocalDateTime end);
    
    List<LogEntry> findByIp(String ip);
}
