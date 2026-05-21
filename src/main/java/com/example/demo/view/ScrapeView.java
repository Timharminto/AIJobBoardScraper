package com.example.demo.view;

import java.util.List;

import com.example.demo.entity.JobListing;
import com.example.demo.service.ScraperService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;

@Route("scrape")
public class ScrapeView extends VerticalLayout {

    private final ScraperService scraperService;
    private final Grid<JobListing> grid = new Grid<>(JobListing.class, false);

    public ScrapeView(ScraperService scraperService) {
        this.scraperService = scraperService;

        // UI Components [cite: 80-86]
        TextField urlField = new TextField("Target URL");
        urlField.setPlaceholder("https://www.anthropic.com/careers");
        urlField.setWidth("300px");

        TextField companyField = new TextField("Company Name");
        companyField.setPlaceholder("Anthropic");
        
        Button scrapeButton = new Button("Scrape Now");
        ProgressBar spinner = new ProgressBar();
        spinner.setIndeterminate(true);
        spinner.setVisible(false);

        // Configure Grid Columns [cite: 85]
        grid.addColumn(JobListing::getTitle).setHeader("Title");
        grid.addColumn(JobListing::getDepartment).setHeader("Department");
        grid.addColumn(JobListing::getLocation).setHeader("Location");
        grid.addColumn(JobListing::getEmploymentType).setHeader("Type");
        
        // Scraping Logic & UI Thread Handling [cite: 87-96, 201]
        scrapeButton.addClickListener(e -> {
            String url = urlField.getValue();
            String company = companyField.getValue();
            
            if (url.isEmpty()) return;

            spinner.setVisible(true);
            scrapeButton.setEnabled(false);

            UI ui = UI.getCurrent();

            // Run in background thread so UI doesn't freeze [cite: 201, 205]
            new Thread(() -> {
                try {
                    List<JobListing> results = scraperService.scrape(url, company);
                    
                    ui.access(() -> {
                        grid.setItems(results);
                        spinner.setVisible(false);
                        scrapeButton.setEnabled(true);
                        Notification.show("Scraping successful! Found " + results.size() + " jobs.");
                    });
                } catch (Exception ex) {
                    ui.access(() -> {
                        spinner.setVisible(false);
                        scrapeButton.setEnabled(true);
                        Notification.show("Scraping failed: " + ex.getMessage());
                    });
                }
            }).start();
        });

        add(new HorizontalLayout(urlField, companyField, scrapeButton), spinner, grid);
    }
}