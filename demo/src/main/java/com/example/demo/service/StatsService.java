package com.example.demo.service;

import com.example.demo.entity.JobListing;
import com.example.demo.repository.JobListingRepository;
import com.example.demo.repository.ScrapeTargetRepository;
import com.example.demo.entity.ScrapeTarget;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.stream.Collectors;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class StatsService {

    private final JobListingRepository jobRepo;
    private final ScrapeTargetRepository targetRepo;

    public StatsService(JobListingRepository jobRepo, ScrapeTargetRepository targetRepo) {
        this.jobRepo = jobRepo;
        this.targetRepo = targetRepo;
    }

    public long getTotalJobsCount() {
        return jobRepo.count();
    }

    public Map<String, Long> getJobCountByDepartment() {
        List<JobListing> allJobs = jobRepo.findAll();
        
        return allJobs.stream()
                .collect(Collectors.groupingBy(
                        job -> job.getDepartment() != null ? job.getDepartment() : "Unknown",
                        Collectors.counting()
                ));
    }

    // Groups jobs by Employment Type (e.g., "Full-time" -> 100, "Internship" -> 5)
    public Map<String, Long> getJobCountByType() {
        List<JobListing> allJobs = jobRepo.findAll();
        
        return allJobs.stream()
                .collect(Collectors.groupingBy(
                        job -> job.getEmploymentType() != null ? job.getEmploymentType() : "Unknown",
                        Collectors.counting()
                ));
    }

    public long getTotalCompaniesCount() {
        return jobRepo.findAll().stream()
                .map(job -> job.getTargetId())
                .distinct()
                .count();
    }

    public String getLastScrapingDate() {
        return jobRepo.findAll().stream()
                .map(job -> job.getScrapedAt())
                .filter(date -> date != null)
                .max(LocalDateTime::compareTo)
                .map(date -> date.format(DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm")))
                .orElse("Belum ada data");
    }

    public Map<String, Long> getJobCountByCompany() {
        Map<Long, String> companyNames = targetRepo.findAll().stream()
                .collect(Collectors.toMap(
                        target -> target.getId(),
                        target -> target.getCompany() != null ? target.getCompany() : "Unknown Company"
                ));

        // Group the jobs, but swap the raw Target ID for the actual Company Name!
        return jobRepo.findAll().stream()
                .collect(Collectors.groupingBy(
                        job -> companyNames.getOrDefault(job.getTargetId(), "Unknown Company"),
                        Collectors.counting()
                ));
    }

    public Map<String, Long> getTop10JobTitles() {
        return jobRepo.findAll().stream()
                .collect(Collectors.groupingBy(
                        job -> job.getTitle() != null ? job.getTitle() : "Unknown", 
                        Collectors.counting()
                ))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed()) // Sort highest to lowest
                .limit(10) // Keep only top 10
                .collect(Collectors.toMap(
                        Map.Entry::getKey, 
                        Map.Entry::getValue, 
                        (e1, e2) -> e1, 
                        LinkedHashMap::new // Maintains the sorted order!
                ));
    }
}