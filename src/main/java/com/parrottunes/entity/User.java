package com.parrottunes.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users", 
       uniqueConstraints = {
           @UniqueConstraint(columnNames = "username"),
           @UniqueConstraint(columnNames = "email")
       })
public class User extends BaseEntity {
    
    @NotBlank
    @Size(max = 15)
    @Column(name = "username", nullable = false, unique = true)
    private String username;
    
    @NotBlank
    @Size(max = 255)
    @Email
    @Column(name = "email", nullable = false, unique = true)
    private String email;
    
    @NotBlank
    @Size(max = 255)
    @Column(name = "password", nullable = false)
    @JsonIgnore
    private String password;
    
    @Size(max = 15)
    @Column(name = "first_name")
    private String firstName;
    
    @Size(max = 25)
    @Column(name = "last_name")
    private String lastName;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "user_group")
    private UserGroup userGroup = UserGroup.USER;
    
    @Column(name = "is_active")
    private Boolean isActive = true;
    
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private Set<ManualPlaylist> manualPlaylists = new HashSet<>();
    
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private Set<SmartPlaylist> smartPlaylists = new HashSet<>();

    // Constructors
    public User() {}

    public User(String username, String email, String password) {
        this.username = username;
        this.email = email;
        this.password = password;
    }

    // Getters and Setters
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public UserGroup getUserGroup() {
        return userGroup;
    }

    public void setUserGroup(UserGroup userGroup) {
        this.userGroup = userGroup;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public Set<ManualPlaylist> getManualPlaylists() {
        return manualPlaylists;
    }

    public void setManualPlaylists(Set<ManualPlaylist> manualPlaylists) {
        this.manualPlaylists = manualPlaylists;
    }

    public Set<SmartPlaylist> getSmartPlaylists() {
        return smartPlaylists;
    }

    public void setSmartPlaylists(Set<SmartPlaylist> smartPlaylists) {
        this.smartPlaylists = smartPlaylists;
    }

    public enum UserGroup {
        ADMIN, USER
    }
}
