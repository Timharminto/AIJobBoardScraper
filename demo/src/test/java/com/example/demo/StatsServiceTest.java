package com.example.demo;

import com.example.demo.entity.JobListing;
import com.example.demo.repository.JobListingRepository;
import com.example.demo.service.StatsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Arrays;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@SpringBootTest
class StatsServiceTest {

    @MockitoBean
    private JobListingRepository jobRepo;

    @Autowired
    private StatsService statsService;

    private JobListing job1, job2, job3;

    @BeforeEach
    void setUp() {
        job1 = new JobListing();
        job1.setDepartment("Engineering");
        job1.setEmploymentType("Full-time");

        job2 = new JobListing();
        job2.setDepartment("Engineering");
        job2.setEmploymentType("Contract");

        job3 = new JobListing();
        job3.setDepartment("Sales");
        job3.setEmploymentType("Full-time");
    }

    @Test
    void getJobCountByDepartment_GroupsCorrectly() {
        when(jobRepo.findAll()).thenReturn(Arrays.asList(job1, job2, job3));

        Map<String, Long> stats = statsService.getJobCountByDepartment();

        assertEquals(2, stats.size()); 
        assertEquals(2L, stats.get("Engineering")); 
        assertEquals(1L, stats.get("Sales")); 
    }

    @Test
    void getJobCountByType_GroupsCorrectly() {
        when(jobRepo.findAll()).thenReturn(Arrays.asList(job1, job2, job3));

        Map<String, Long> stats = statsService.getJobCountByType();

        assertEquals(2L, stats.get("Full-time")); 
        assertEquals(1L, stats.get("Contract")); 
    }
}