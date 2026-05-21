package com.example.demo.service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.entity.JobListing;
import com.example.demo.entity.ScrapeTarget;
import com.example.demo.repository.JobListingRepository;
import com.example.demo.repository.ScrapeTargetRepository;

@Service
public class ScraperService {

    private final ScrapeTargetRepository targetRepo;
    private final JobListingRepository jobRepo;

    public ScraperService(ScrapeTargetRepository targetRepo, JobListingRepository jobRepo) {
        this.targetRepo = targetRepo;
        this.jobRepo = jobRepo;
    }

    @Transactional
    public List<JobListing> scrape(String url, String companyName) {
        ScrapeTarget target = targetRepo.findByUrl(url)
                .orElse(new ScrapeTarget());
        target.setUrl(url);
        target.setCompany(companyName);
        target.setLastScrapedAt(LocalDateTime.now());
        
        final ScrapeTarget savedTarget = targetRepo.save(target);

        List<JobListing> jobs = new ArrayList<>();
        
        try {
            // LAYER 1: Jsoup fetch HTML [cite: 114]
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0")
                    .timeout(10_000)
                    .get(); 
            
            // --- NEW ANTHROPIC LOGIC STARTS HERE ---
            // Select elements where the class CONTAINS "jobItem" [cite: 213]
            Elements jobElements = doc.select("a[class*=jobItem]");
            
            if (jobElements.isEmpty()) {
                throw new Exception("No jobs found with Layer 1");
            }
            
            for (org.jsoup.nodes.Element el : jobElements) {
                JobListing job = new JobListing();
                
                // Get the absolute URL from the main <a> tag
                job.setUrl(el.absUrl("href"));
                
                // Find the Title inside the nested div with "jobRole" in the class
                org.jsoup.nodes.Element roleElement = el.selectFirst("div[class*=jobRole]");
                if (roleElement != null) {
                    job.setTitle(roleElement.text());
                } else {
                    job.setTitle("Unknown Title"); 
                }
                
                // Find the Location inside the nested div with "jobLocation" in the class
                org.jsoup.nodes.Element locationElement = el.selectFirst("div[class*=jobLocation]");
                if (locationElement != null) {
                    job.setLocation(locationElement.text());
                }

                job.setTargetId(savedTarget.getId()); 
                jobs.add(job);
            }
            // --- NEW ANTHROPIC LOGIC ENDS HERE ---
            
        } catch (Exception e) {
            System.out.println("Layer 1 failed. Triggering Layer 2 (Scrapling)...");
            jobs = fallbackToScrapling(url, savedTarget.getId());
        }

        return jobRepo.saveAll(jobs);
    }

    // LAYER 2 FALLBACK INTEGRATION [cite: 229-231, 237, 238]
    private List<JobListing> fallbackToScrapling(String url, Long targetId) {
        List<JobListing> jobs = new ArrayList<>();
        try {
            // Execute the python script using Scrapling
            ProcessBuilder pb = new ProcessBuilder("python3", "scrapling_script.py", url);
            Process p = pb.start();
            BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
            
            // In a real scenario, the python script would return JSON.
            // Parse that JSON here into JobListing objects.
            String line;
            while ((line = in.readLine()) != null) {
                // Parsing logic goes here
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return jobs;
    }
}