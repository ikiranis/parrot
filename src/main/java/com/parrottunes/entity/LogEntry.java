package com.parrottunes.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

@Entity
@Table(name = "logs")
public class LogEntry extends BaseEntity {
    
    @Size(max = 255)
    @Column(name = "message")
    private String message;
    
    @Size(max = 15)
    @Column(name = "ip")
    private String ip;
    
    @Size(max = 15)
    @Column(name = "user_name")
    private String userName;
    
    @Column(name = "log_date")
    private LocalDateTime logDate;
    
    @Size(max = 70)
    @Column(name = "browser")
    private String browser;

    // Constructors
    public LogEntry() {}

    public LogEntry(String message, String ip, String userName, String browser) {
        this.message = message;
        this.ip = ip;
        this.userName = userName;
        this.browser = browser;
        this.logDate = LocalDateTime.now();
    }

    // Getters and Setters
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public LocalDateTime getLogDate() {
        return logDate;
    }

    public void setLogDate(LocalDateTime logDate) {
        this.logDate = logDate;
    }

    public String getBrowser() {
        return browser;
    }

    public void setBrowser(String browser) {
        this.browser = browser;
    }
}
