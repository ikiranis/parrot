package com.parrottunes.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "votes")
public class Vote extends BaseEntity {
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id")
    private MediaFile file;
    
    @Size(max = 20)
    @Column(name = "voter_ip")
    private String voterIp;

    // Constructors
    public Vote() {}

    public Vote(MediaFile file, String voterIp) {
        this.file = file;
        this.voterIp = voterIp;
    }

    // Getters and Setters
    public MediaFile getFile() {
        return file;
    }

    public void setFile(MediaFile file) {
        this.file = file;
    }

    public String getVoterIp() {
        return voterIp;
    }

    public void setVoterIp(String voterIp) {
        this.voterIp = voterIp;
    }
}
