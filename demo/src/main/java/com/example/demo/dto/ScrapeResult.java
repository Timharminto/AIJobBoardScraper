package com.example.demo.dto;

import java.util.List;

import com.example.demo.entity.JobListing;

public class ScrapeResult {
    private final List<JobListing> jobs;
    private final String layerUsed;
    private final boolean success;
    private final String errorMessage;

    public ScrapeResult(List<JobListing> jobs, String layerUsed, boolean success, String errorMessage) {
        this.jobs = jobs;
        this.layerUsed = layerUsed;
        this.success = success;
        this.errorMessage = errorMessage;
    }

    public List<JobListing> getJobs() { return jobs; }
    public String getLayerUsed() { return layerUsed; }
    public boolean isSuccess() { return success; }
    public String getErrorMessage() { return errorMessage; }
}