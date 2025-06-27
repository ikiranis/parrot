package com.parrottunes.controller;

import com.parrottunes.dto.ApiResponse;
import com.parrottunes.entity.User;
import com.parrottunes.repository.UserRepository;
import com.parrottunes.service.CustomUserDetailsService.UserPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        Optional<User> userOpt = userRepository.findById(userPrincipal.getId());
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            // Don't expose password
            user.setPassword(null);
            return ResponseEntity.ok(new ApiResponse(true, "User details retrieved successfully", user));
        }
        return ResponseEntity.notFound().build();
    }

    @PutMapping("/me")
    public ResponseEntity<?> updateCurrentUser(@RequestBody User userUpdate,
                                              @AuthenticationPrincipal UserPrincipal userPrincipal) {
        try {
            Optional<User> userOpt = userRepository.findById(userPrincipal.getId());
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                
                // Update allowed fields
                if (userUpdate.getFirstName() != null) {
                    user.setFirstName(userUpdate.getFirstName());
                }
                if (userUpdate.getLastName() != null) {
                    user.setLastName(userUpdate.getLastName());
                }
                if (userUpdate.getEmail() != null && !userUpdate.getEmail().equals(user.getEmail())) {
                    // Check if email is already taken
                    if (userRepository.existsByEmail(userUpdate.getEmail())) {
                        return ResponseEntity.badRequest()
                                .body(new ApiResponse(false, "Email is already in use"));
                    }
                    user.setEmail(userUpdate.getEmail());
                }
                
                User savedUser = userRepository.save(user);
                savedUser.setPassword(null); // Don't expose password
                return ResponseEntity.ok(new ApiResponse(true, "User updated successfully", savedUser));
            }
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Error updating user: " + e.getMessage()));
        }
    }

    @PutMapping("/me/password")
    public ResponseEntity<?> changePassword(@RequestParam String currentPassword,
                                          @RequestParam String newPassword,
                                          @AuthenticationPrincipal UserPrincipal userPrincipal) {
        try {
            Optional<User> userOpt = userRepository.findById(userPrincipal.getId());
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                
                // Verify current password
                if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
                    return ResponseEntity.badRequest()
                            .body(new ApiResponse(false, "Current password is incorrect"));
                }
                
                // Update password
                user.setPassword(passwordEncoder.encode(newPassword));
                userRepository.save(user);
                
                return ResponseEntity.ok(new ApiResponse(true, "Password changed successfully"));
            }
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Error changing password: " + e.getMessage()));
        }
    }
}
