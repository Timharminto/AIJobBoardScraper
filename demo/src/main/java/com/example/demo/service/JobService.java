package com.example.demo.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.example.demo.entity.JobListing;
import com.example.demo.repository.JobListingRepository;

@Service
public class JobService {

    private final JobListingRepository jobRepo;

    public JobService(JobListingRepository jobRepo) {
        this.jobRepo = jobRepo;
    }

    // 1. Get all jobs
    public List<JobListing> getAllJobs() {
        return jobRepo.findAll();
    }

    // 2. Delete a job
    public void deleteJob(Long id) {
        jobRepo.deleteById(id);
    }

    // 3. Simple in-memory search (Great for Vaadin Grid filtering)
    public List<JobListing> searchJobs(String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return getAllJobs();
        }
        
        String lowerCaseTerm = searchTerm.toLowerCase();
        
        return jobRepo.findAll().stream()
                .filter(job -> 
                    (job.getTitle() != null && job.getTitle().toLowerCase().contains(lowerCaseTerm)) ||
                    (job.getLocation() != null && job.getLocation().toLowerCase().contains(lowerCaseTerm)) ||
                    (job.getDepartment() != null && job.getDepartment().toLowerCase().contains(lowerCaseTerm)) ||
                    (job.getEmploymentType() != null && job.getEmploymentType().toLowerCase().contains(lowerCaseTerm))
                )
                .collect(Collectors.toList());
    }
}