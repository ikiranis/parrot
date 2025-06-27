package com.parrottunes.repository;

import com.parrottunes.entity.Vote;
import com.parrottunes.entity.MediaFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VoteRepository extends JpaRepository<Vote, Long> {
    
    List<Vote> findByFile(MediaFile file);
    
    @Query("SELECT v.file, COUNT(v) as voteCount FROM Vote v GROUP BY v.file ORDER BY voteCount DESC")
    List<Object[]> findVoteCountsByFile();
    
    @Query("SELECT COUNT(v) FROM Vote v WHERE v.file = :file")
    Long countByFile(@Param("file") MediaFile file);
    
    Boolean existsByFileAndVoterIp(MediaFile file, String voterIp);
}
