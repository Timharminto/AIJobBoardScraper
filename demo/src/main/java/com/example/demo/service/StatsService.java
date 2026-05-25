package com.example.demo.service;

import com.example.demo.entity.JobListing;
import com.example.demo.repository.JobListingRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class StatsService {

    private final JobListingRepository jobRepo;

    public StatsService(JobListingRepository jobRepo) {
        this.jobRepo = jobRepo;
    }

    public long getTotalJobsCount() {
        return jobRepo.count();
    }

    // Groups jobs by Department (e.g., "Engineering" -> 45, "Sales" -> 12)
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
}