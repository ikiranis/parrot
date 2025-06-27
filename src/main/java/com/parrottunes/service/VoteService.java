package com.parrottunes.service;

import com.parrottunes.entity.Vote;
import com.parrottunes.entity.MediaFile;
import com.parrottunes.repository.VoteRepository;
import com.parrottunes.repository.MediaFileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class VoteService {

    @Autowired
    private VoteRepository voteRepository;

    @Autowired
    private MediaFileRepository mediaFileRepository;

    public Vote voteForFile(Long fileId, String voterIp) {
        Optional<MediaFile> fileOpt = mediaFileRepository.findById(fileId);
        if (fileOpt.isPresent()) {
            MediaFile file = fileOpt.get();
            
            // Check if this IP already voted for this file
            if (voteRepository.existsByFileAndVoterIp(file, voterIp)) {
                throw new RuntimeException("You have already voted for this file");
            }
            
            Vote vote = new Vote(file, voterIp);
            return voteRepository.save(vote);
        }
        throw new RuntimeException("File not found with id: " + fileId);
    }

    public List<Object[]> getVoteCountsByFile() {
        return voteRepository.findVoteCountsByFile();
    }

    public Long getVoteCount(Long fileId) {
        Optional<MediaFile> fileOpt = mediaFileRepository.findById(fileId);
        if (fileOpt.isPresent()) {
            return voteRepository.countByFile(fileOpt.get());
        }
        return 0L;
    }

    public void clearAllVotes() {
        voteRepository.deleteAll();
    }

    public void clearVotesForFile(Long fileId) {
        Optional<MediaFile> fileOpt = mediaFileRepository.findById(fileId);
        if (fileOpt.isPresent()) {
            List<Vote> votes = voteRepository.findByFile(fileOpt.get());
            voteRepository.deleteAll(votes);
        }
    }
}
