package com.parrottunes.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "queue")
public class QueueItem extends BaseEntity {
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id")
    private MediaFile file;

    // Constructors
    public QueueItem() {}

    public QueueItem(MediaFile file) {
        this.file = file;
    }

    // Getters and Setters
    public MediaFile getFile() {
        return file;
    }

    public void setFile(MediaFile file) {
        this.file = file;
    }
}
