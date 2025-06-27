package com.parrottunes.controller;

import com.parrottunes.dto.ApiResponse;
import com.parrottunes.entity.Vote;
import com.parrottunes.service.VoteService;
import com.parrottunes.service.CustomUserDetailsService.UserPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/votes")
public class VoteController {

    @Autowired
    private VoteService voteService;

    @PostMapping
    public ResponseEntity<?> voteForFile(@RequestParam Long fileId,
                                        @AuthenticationPrincipal UserPrincipal userPrincipal,
                                        HttpServletRequest request) {
        try {
            String voterIp = getClientIpAddress(request);
            Vote vote = voteService.voteForFile(fileId, voterIp);
            return ResponseEntity.ok(new ApiResponse(true, "Vote cast successfully", vote));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Error casting vote: " + e.getMessage()));
        }
    }

    @GetMapping("/counts")
    public ResponseEntity<?> getVoteCountsByFile() {
        try {
            List<Object[]> voteCounts = voteService.getVoteCountsByFile();
            return ResponseEntity.ok(new ApiResponse(true, "Vote counts retrieved successfully", voteCounts));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Error retrieving vote counts: " + e.getMessage()));
        }
    }

    @GetMapping("/file/{fileId}")
    public ResponseEntity<?> getVoteCountForFile(@PathVariable Long fileId) {
        try {
            Long voteCount = voteService.getVoteCount(fileId);
            return ResponseEntity.ok(new ApiResponse(true, "Vote count retrieved successfully", voteCount));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Error retrieving vote count: " + e.getMessage()));
        }
    }

    @DeleteMapping
    public ResponseEntity<?> clearAllVotes(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        try {
            voteService.clearAllVotes();
            return ResponseEntity.ok(new ApiResponse(true, "All votes cleared successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Error clearing votes: " + e.getMessage()));
        }
    }

    @DeleteMapping("/file/{fileId}")
    public ResponseEntity<?> clearVotesForFile(@PathVariable Long fileId,
                                              @AuthenticationPrincipal UserPrincipal userPrincipal) {
        try {
            voteService.clearVotesForFile(fileId);
            return ResponseEntity.ok(new ApiResponse(true, "Votes for file cleared successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Error clearing votes for file: " + e.getMessage()));
        }
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedForHeader = request.getHeader("X-Forwarded-For");
        if (xForwardedForHeader == null) {
            return request.getRemoteAddr();
        } else {
            return xForwardedForHeader.split(",")[0].trim();
        }
    }
}
