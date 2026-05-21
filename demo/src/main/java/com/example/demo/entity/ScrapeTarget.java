package com.example.demo.entity; // Adjust this to match your actual package

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "scrape_targets")
public class ScrapeTarget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String url;
    
    private String company;
    
    @Column(name = "last_scraped_at")
    private LocalDateTime lastScrapedAt;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    // --- Constructors ---
    public ScrapeTarget() {
    }

    // --- Getters and Setters ---
    
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public LocalDateTime getLastScrapedAt() {
        return lastScrapedAt;
    }

    public void setLastScrapedAt(LocalDateTime lastScrapedAt) {
        this.lastScrapedAt = lastScrapedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}