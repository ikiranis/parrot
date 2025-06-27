package com.parrottunes.controller;

import com.parrottunes.dto.ApiResponse;
import com.parrottunes.entity.QueueItem;
import com.parrottunes.service.QueueService;
import com.parrottunes.service.CustomUserDetailsService.UserPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/queue")
public class QueueController {

    @Autowired
    private QueueService queueService;

    @GetMapping
    public ResponseEntity<?> getQueue() {
        try {
            List<QueueItem> queue = queueService.getQueue();
            return ResponseEntity.ok(new ApiResponse(true, "Queue retrieved successfully", queue));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Error retrieving queue: " + e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<?> addToQueue(@RequestParam Long fileId,
                                       @AuthenticationPrincipal UserPrincipal userPrincipal) {
        try {
            QueueItem queueItem = queueService.addToQueue(fileId);
            return ResponseEntity.ok(new ApiResponse(true, "File added to queue successfully", queueItem));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Error adding file to queue: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{queueItemId}")
    public ResponseEntity<?> removeFromQueue(@PathVariable Long queueItemId,
                                            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        try {
            queueService.removeFromQueue(queueItemId);
            return ResponseEntity.ok(new ApiResponse(true, "File removed from queue successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Error removing file from queue: " + e.getMessage()));
        }
    }

    @GetMapping("/next")
    public ResponseEntity<?> getNextInQueue() {
        try {
            Optional<QueueItem> nextItem = queueService.getNextInQueue();
            if (nextItem.isPresent()) {
                return ResponseEntity.ok(new ApiResponse(true, "Next item in queue", nextItem.get()));
            } else {
                return ResponseEntity.ok(new ApiResponse(true, "Queue is empty", null));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Error getting next item in queue: " + e.getMessage()));
        }
    }

    @DeleteMapping
    public ResponseEntity<?> clearQueue(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        try {
            queueService.clearQueue();
            return ResponseEntity.ok(new ApiResponse(true, "Queue cleared successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Error clearing queue: " + e.getMessage()));
        }
    }

    @GetMapping("/size")
    public ResponseEntity<?> getQueueSize() {
        try {
            long size = queueService.getQueueSize();
            return ResponseEntity.ok(new ApiResponse(true, "Queue size retrieved successfully", size));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Error getting queue size: " + e.getMessage()));
        }
    }
}
