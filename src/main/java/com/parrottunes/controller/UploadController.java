package com.parrottunes.controller;

import com.parrottunes.dto.ApiResponse;
import com.parrottunes.dto.MediaFileResponse;
import com.parrottunes.entity.MediaFile;
import com.parrottunes.service.FileUploadService;
import com.parrottunes.service.CustomUserDetailsService.UserPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/upload")
public class UploadController {

    @Autowired
    private FileUploadService fileUploadService;

    @PostMapping("/file")
    public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile file,
                                       @AuthenticationPrincipal UserPrincipal userPrincipal) {
        try {
            MediaFile uploadedFile = fileUploadService.uploadFile(file);
            MediaFileResponse response = new MediaFileResponse(uploadedFile);
            return ResponseEntity.ok(new ApiResponse(true, "File uploaded successfully", response));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Error uploading file: " + e.getMessage()));
        }
    }

    @PostMapping("/files")
    public ResponseEntity<?> uploadMultipleFiles(@RequestParam("files") MultipartFile[] files,
                                                @AuthenticationPrincipal UserPrincipal userPrincipal) {
        try {
            java.util.List<MediaFileResponse> uploadedFiles = new java.util.ArrayList<>();
            java.util.List<String> errors = new java.util.ArrayList<>();

            for (MultipartFile file : files) {
                try {
                    MediaFile uploadedFile = fileUploadService.uploadFile(file);
                    uploadedFiles.add(new MediaFileResponse(uploadedFile));
                } catch (Exception e) {
                    errors.add("Error uploading " + file.getOriginalFilename() + ": " + e.getMessage());
                }
            }

            if (errors.isEmpty()) {
                return ResponseEntity.ok(new ApiResponse(true, "All files uploaded successfully", uploadedFiles));
            } else {
                return ResponseEntity.ok(new ApiResponse(true, 
                        "Some files uploaded with errors", 
                        java.util.Map.of("uploaded", uploadedFiles, "errors", errors)));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Error uploading files: " + e.getMessage()));
        }
    }
}
