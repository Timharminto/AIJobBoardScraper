package com.example.demo.service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
        
        // ADDED: A Set to track jobs we've already parsed so OpenAI's double-links don't cause duplicates
        Set<String> processedTitles = new HashSet<>(); 
        
        String[] knownDepartments = {
            "AI Research","Engineering", "Research", "Product", "Design", "Sales", "Marketing", 
            "Operations", "Finance", "Legal", "HR", "Robotics", "Applied AI", "Security",
            "Datacenter Design", "Intelligence & Investigations", "Hardware"
        };
        
        try {
            // LAYER 1: Jsoup fetch HTML 
            // (Upgraded User-Agent to look like a real Chrome browser to avoid basic bot blocks)
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .timeout(15_000)
                    .get(); 
            
            // Grab ANY link that might be related to a job
            Elements links = doc.select("a[href*=/careers/], a[href*=/jobs/], a[href*=/role/], a[class*=job], a:matchesOwn((?i)(apply|view job))");
            
            if (links.isEmpty()) {
                throw new Exception("No job links found in Layer 1");
            }
            
            for (org.jsoup.nodes.Element link : links) {
                org.jsoup.nodes.Element card = link;
                org.jsoup.nodes.Element titleEl = card.selectFirst("h1, h2, h3, h4, h5, h6, strong, b, [class*=title], [class*=jobRole]");
                
                // --- SMART DOM CLIMBING ---
                // Only climb if the link itself DOES NOT contain the title (Solves the Anthropic vs OpenAI issue)
                int climbLimit = 4;
                while (titleEl == null && card.parent() != null && climbLimit > 0) {
                    card = card.parent();
                    titleEl = card.selectFirst("h1, h2, h3, h4, h5, h6, strong, b, [class*=title], [class*=jobRole]");
                    climbLimit--;
                }
                
                // Extract Title
                String title = "";
                if (titleEl != null) {
                    title = titleEl.text().trim();
                } else {
                    title = link.text().trim();
                }

                // Clean up title
                title = title.replaceAll("(?i)(apply now|apply|view job|learn more).*", "").trim();
                if (title.isEmpty() || title.length() < 4 || title.equalsIgnoreCase("careers") || title.equalsIgnoreCase("jobs")) {
                    continue; 
                }

                // --- DEDUPLICATION (Crucial for OpenAI) ---
                // If we already saved this job from the other link in the row, skip it!
                if (processedTitles.contains(title.toLowerCase())) {
                    continue;
                }
                processedTitles.add(title.toLowerCase());

                JobListing job = new JobListing();
                job.setUrl(link.absUrl("href")); // Always use the direct link we found
                job.setTitle(title);
                
                // --- ISOLATED ELEMENT SCANNER (Location & Dept) ---
                List<String> foundLocations = new ArrayList<>();
                String foundDept = null;
                
                Pattern locationPattern = Pattern.compile("([A-Z][a-zA-Z]+\\s?){1,3},\\s[A-Z]{2}");
                Pattern multiLocationPattern = Pattern.compile("(?i)\\d+\\s+locations?");

                for (org.jsoup.nodes.Element child : card.getAllElements()) {
                    String elementText = child.ownText().trim();
                    if (elementText.isEmpty() || elementText.equals(job.getTitle())) continue;

                    // Dept Check
                    if (foundDept == null) {
                        for (String dept : knownDepartments) {
                            if (elementText.equalsIgnoreCase(dept) || 
                               (elementText.contains(dept) && !job.getTitle().contains(dept))) {
                                foundDept = dept;
                                break;
                            }
                        }
                    }

                    // Location Regex Check
                    Matcher matcher = locationPattern.matcher(elementText);
                    while (matcher.find()) {
                        String matchedLoc = matcher.group(0).trim();
                        if (!foundLocations.contains(matchedLoc)) foundLocations.add(matchedLoc);
                    }

                    // OpenAI "2 locations" Check
                    if (multiLocationPattern.matcher(elementText).find()) {
                        if (!foundLocations.contains("Multiple Locations")) foundLocations.add("Multiple Locations");
                    }

                    // Explicit Keywords Fallback
                    String lowerEl = elementText.toLowerCase();
                    if (lowerEl.contains("remote") && !foundLocations.contains("Remote")) foundLocations.add("Remote");
                    else if ((lowerEl.contains("san francisco") || lowerEl.equals("sf")) && !foundLocations.contains("San Francisco, CA")) foundLocations.add("San Francisco, CA");
                    else if ((lowerEl.contains("new york") || lowerEl.contains("nyc")) && !foundLocations.contains("New York, NY")) foundLocations.add("New York, NY");
                    else if (lowerEl.contains("london") && !foundLocations.contains("London, UK")) foundLocations.add("London, UK");
                    else if (lowerEl.contains("seattle") && !foundLocations.contains("Seattle, WA")) foundLocations.add("Seattle, WA");
                }

                // Anthropic Dept Fallback
                if (foundDept == null) {
                    org.jsoup.nodes.Element parent = card.parent();
                    int deptClimb = 3;
                    while (parent != null && deptClimb > 0) {
                        org.jsoup.nodes.Element sectionHeader = parent.selectFirst("h2, h3, h4");
                        if (sectionHeader != null && !sectionHeader.text().equals(job.getTitle())) {
                            foundDept = sectionHeader.text();
                            break;
                        }
                        parent = parent.parent();
                        deptClimb--;
                    }
                }
                
                job.setDepartment(foundDept != null ? foundDept : "General");
                job.setLocation(!foundLocations.isEmpty() ? String.join(" | ", foundLocations) : "Unknown");

                // Job Type
                String cardTextLower = card.text().toLowerCase();
                if (cardTextLower.contains("contract") || cardTextLower.contains("contractor")) job.setEmploymentType("Contract");
                else if (cardTextLower.contains("intern") || cardTextLower.contains("internship")) job.setEmploymentType("Internship");
                else if (cardTextLower.contains("part-time") || cardTextLower.contains("part time")) job.setEmploymentType("Part-time");
                else if (cardTextLower.contains("fellow") || cardTextLower.contains("fellowship")) job.setEmploymentType("Fellowship");
                else job.setEmploymentType("Full-time");
                
                job.setTargetId(savedTarget.getId()); 
                jobs.add(job);
            }
            
        } catch (Exception e) {
            System.out.println("Layer 1 heuristics failed: " + e.getMessage() + ". Triggering Layer 2 (Scrapling)...");
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