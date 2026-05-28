package com.example.demo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.example.demo.entity.JobListing;

public interface JobListingRepository extends JpaRepository<JobListing, Long> {
    List<JobListing> findByTargetId(Long targetId);
    
    // For DashboardView Analytics [cite: 123]
    @Query("SELECT j.department, COUNT(j) FROM JobListing j GROUP BY j.department")
    List<Object[]> countByDepartment();
    
    @Query("SELECT j.title, COUNT(j) as count FROM JobListing j GROUP BY j.title ORDER BY count DESC LIMIT 10")
    List<Object[]> findTopJobTitles();
}