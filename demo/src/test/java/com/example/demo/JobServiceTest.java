package com.example.demo;

import com.example.demo.entity.JobListing;
import com.example.demo.repository.JobListingRepository;
import com.example.demo.service.JobService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@SpringBootTest
class JobServiceTest {

    @MockitoBean
    private JobListingRepository jobRepo;

    @Autowired
    private JobService jobService;

    private JobListing job1;
    private JobListing job2;

    @BeforeEach
    void setUp() {
        job1 = new JobListing();
        job1.setTitle("Software Engineer");
        job1.setLocation("San Francisco, CA");
        job1.setDepartment("Engineering");

        job2 = new JobListing();
        job2.setTitle("Data Scientist");
        job2.setLocation("Remote");
        job2.setDepartment("Research");
    }

    @Test
    void searchJobs_WithValidKeyword_ReturnsMatchedJobs() {
        when(jobRepo.findAll()).thenReturn(Arrays.asList(job1, job2));

        List<JobListing> results = jobService.searchJobs("software");

        assertEquals(1, results.size());
        assertEquals("Software Engineer", results.get(0).getTitle());
    }

    @Test
    void searchJobs_WithEmptyKeyword_ReturnsAllJobs() {
        when(jobRepo.findAll()).thenReturn(Arrays.asList(job1, job2));

        List<JobListing> results = jobService.searchJobs("");

        assertEquals(2, results.size());
        verify(jobRepo, times(1)).findAll(); 
    }
}