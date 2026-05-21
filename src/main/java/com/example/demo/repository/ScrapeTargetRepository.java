package com.example.demo.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.entity.ScrapeTarget;

@Repository
public interface ScrapeTargetRepository extends JpaRepository<ScrapeTarget, Long> {
    
    /**
     * Finds a ScrapeTarget by its unique URL.
     * This is used in the ScraperService to determine if a URL has been scraped before.
     *
     * @param url the target URL
     * @return an Optional containing the ScrapeTarget if found, or empty if not
     */
    Optional<ScrapeTarget> findByUrl(String url);
    
}