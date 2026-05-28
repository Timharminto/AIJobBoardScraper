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

import com.example.demo.dto.ScrapeResult;
import com.example.demo.entity.JobListing;
import com.example.demo.entity.ScrapeHistory;
import com.example.demo.entity.ScrapeTarget;
import com.example.demo.repository.JobListingRepository;
import com.example.demo.repository.ScrapeHistoryRepository;
import com.example.demo.repository.ScrapeTargetRepository;

@Service
public class ScraperService {

    private final ScrapeTargetRepository targetRepo;
    private final ScrapeHistoryRepository historyRepo;
    private final JobListingRepository jobRepo;

    public ScraperService(ScrapeTargetRepository targetRepo, JobListingRepository jobRepo, ScrapeHistoryRepository historyRepo) {
        this.targetRepo = targetRepo;
        this.jobRepo = jobRepo;
        this.historyRepo = historyRepo;
    }

    @Transactional
    public ScrapeResult scrape(String url, String companyName) {
        ScrapeTarget target = targetRepo.findByCompanyIgnoreCase(companyName)
                .orElseGet(() -> {
                    ScrapeTarget newTarget = new ScrapeTarget();
                    newTarget.setCompany(companyName);
                    newTarget.setUrl(url);
                    return targetRepo.save(newTarget);
                });

        int jobsScrapedCount = 0;

        List<JobListing> jobs = new ArrayList<>();
        
        Set<String> processedTitles = new HashSet<>(); 
        
        String[] knownDepartments = {
            "AI Research","Engineering", "Research", "Product", "Design", "Sales", "Marketing", 
            "Operations", "Finance", "Legal", "HR", "Robotics", "Applied AI", "Security",
            "Datacenter Design", "Intelligence & Investigations", "Hardware"
        };
        
        try {
            // LAYER 1: Jsoup fetch HTML 
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .timeout(15_000)
                    .get(); 
            
            Elements links = doc.select("a[href*=/careers/], a[href*=/jobs/], a[href*=/role/], a[class*=job], a:matchesOwn((?i)(apply|view job))");
            
            if (links.isEmpty()) {
                throw new Exception("No job links found in Layer 1");
            }
            
            for (org.jsoup.nodes.Element link : links) {
                org.jsoup.nodes.Element card = link;
                org.jsoup.nodes.Element titleEl = card.selectFirst("h1, h2, h3, h4, h5, h6, strong, b, [class*=title], [class*=jobRole]");
                
                // --- SMART DOM CLIMBING ---
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
                if (processedTitles.contains(title.toLowerCase())) {
                    continue;
                }
                processedTitles.add(title.toLowerCase());

                JobListing job = new JobListing();
                job.setUrl(link.absUrl("href"));
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
                
                job.setTargetId(target.getId()); 
                jobs.add(job);
                jobsScrapedCount++;
            }
            ScrapeHistory history = new ScrapeHistory();
            history.setScrapedAt(LocalDateTime.now());
            history.setCompanyName(companyName);
            history.setTargetUrl(url);
            history.setJobsFound(jobsScrapedCount);
            historyRepo.save(history);
            List<JobListing> savedJobs = jobRepo.saveAll(jobs);
            return new ScrapeResult(savedJobs, "Layer 1 (Jsoup)", true, null);
        } catch (Exception e) {
            System.out.println("Layer 1 heuristics failed: " + e.getMessage() + ". Triggering Layer 2 (Scrapling)...");
            
            try {
                jobs = fallbackToScrapling(url, target.getId(), companyName);
                List<JobListing> savedJobs = jobRepo.saveAll(jobs);
                
                return new ScrapeResult(savedJobs, "Layer 2 (Scrapling Fallback)", true, null);
            } catch (Exception ex) {
                return new ScrapeResult(new ArrayList<>(), "None", false, ex.getMessage());
            }
        }
    }

    // LAYER 2 FALLBACK INTEGRATION
    private List<JobListing> fallbackToScrapling(String url, Long targetId, String companyName) {
        List<JobListing> jobs = new ArrayList<>();
        Set<String> processedTitles = new HashSet<>(); 
        
        String[] knownDepartments = {
            "AI Research","Engineering", "Research", "Product", "Design", "Sales", "Marketing", 
            "Operations", "Finance", "Legal", "HR", "Robotics", "Applied AI", "Security",
            "Datacenter Design", "Intelligence & Investigations", "Hardware"
        };

        try {
            // 1. Trigger Python Scrapling Script
            ProcessBuilder pb = new ProcessBuilder("python3", "scrapling_script.py", url);
            Process p = pb.start();
            BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
            
            // 2. Stitch the incoming terminal lines into a clean HTML block
            StringBuilder htmlBuilder = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                htmlBuilder.append(line).append("\n");
            }
            p.waitFor();

            // 3. Convert Scrapling's rendered output into a Jsoup Document
            Document doc = Jsoup.parse(htmlBuilder.toString(), url);
            
            Elements links = doc.select("a[href*=/careers/], a[href*=/jobs/], a[href*=/role/], a[class*=job], a:matchesOwn((?i)(apply|view job))");
            
            for (org.jsoup.nodes.Element link : links) {
                org.jsoup.nodes.Element card = link;
                org.jsoup.nodes.Element titleEl = card.selectFirst("h1, h2, h3, h4, h5, h6, strong, b, [class*=title], [class*=jobRole]");
                
                // --- SMART DOM CLIMBING ---
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

                // --- DEDUPLICATION ---
                if (processedTitles.contains(title.toLowerCase())) {
                    continue;
                }
                processedTitles.add(title.toLowerCase());

                JobListing job = new JobListing();
                job.setUrl(link.absUrl("href"));
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
                
                job.setTargetId(targetId); 
                jobs.add(job);
            }

            // 4. Save to ScrapeHistory table for Layer 2 Analytics
            ScrapeHistory history = new ScrapeHistory();
            history.setScrapedAt(LocalDateTime.now());
            history.setCompanyName(companyName);
            history.setTargetUrl(url);
            history.setJobsFound(jobs.size());
            historyRepo.save(history);

        } catch (Exception ex) {
            System.err.println("Layer 2 execution or parsing structural error: " + ex.getMessage());
            ex.printStackTrace();
        }

        return jobs;
    }
}