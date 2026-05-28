package com.example.demo.view;

import com.example.demo.entity.JobListing;
import com.example.demo.service.JobService;
import com.example.demo.service.ScraperService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.dataview.GridListDataView;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;

@Route(value = "scrape", layout = MainLayout.class)
@PageTitle("Scraper | AI Job Board")
public class ScrapeView extends VerticalLayout {

    private final ScraperService scraperService;
    private final JobService jobService;
    
    private Grid<JobListing> grid;
    private GridListDataView<JobListing> dataView;

    private TextField searchField;
    private TextField deptFilter;
    private TextField typeFilter;
    private TextField locationFilter;

    public ScrapeView(ScraperService scraperService, JobService jobService) {
        this.scraperService = scraperService;
        this.jobService = jobService;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        add(new H2("Run Web Scraper"));
        add(createScrapeForm());

        add(new H2("Scraped Job Listings"));

        createGrid(); 

        add(createToolbar()); 

        add(grid);

        refreshGrid();
    }

    private HorizontalLayout createScrapeForm() {
        TextField urlField = new TextField("Careers URL");
        urlField.setWidth("300px");
        urlField.setPlaceholder("https://openai.com/careers...");

        TextField companyField = new TextField("Company Name");
        companyField.setWidth("200px");
        companyField.setPlaceholder("OpenAI");

        Button scrapeBtn = new Button("Start Scraping", VaadinIcon.DOWNLOAD.create());
        scrapeBtn.addThemeName("primary");
        scrapeBtn.addClickListener(e -> {
            if (urlField.isEmpty() || companyField.isEmpty()) {
                Notification.show("Please enter both URL and Company Name", 3000, Notification.Position.MIDDLE);
                return;
            }
            
            try {
                scraperService.scrape(urlField.getValue(), companyField.getValue());
                Notification.show("Scraping successful!", 3000, Notification.Position.BOTTOM_END);
                refreshGrid();
            } catch (Exception ex) {
                Notification.show("Scraping failed: " + ex.getMessage(), 5000, Notification.Position.MIDDLE);
            }
        });

        HorizontalLayout layout = new HorizontalLayout(urlField, companyField, scrapeBtn);
        layout.setAlignItems(Alignment.BASELINE);
        return layout;
    }

    private HorizontalLayout createToolbar() {
        // --- SEARCH BAR (Title & Location) ---
        searchField = new TextField();
        searchField.setPlaceholder("Search titles, locations...");
        searchField.setPrefixComponent(VaadinIcon.SEARCH.create());
        searchField.setClearButtonVisible(true);

        // --- FILTER: DEPARTMENT (Free Text) ---
        deptFilter = new TextField("Department");
        deptFilter.setPlaceholder("e.g. Engineering...");
        deptFilter.setClearButtonVisible(true);

        // --- FILTER: EMPLOYMENT TYPE (Free Text) ---
        typeFilter = new TextField("Job Type");
        typeFilter.setPlaceholder("e.g. Full-time...");
        typeFilter.setClearButtonVisible(true);

        // --- FILTER: LOCATION TYPE (Free Text) ---
        locationFilter = new TextField("Location");
        locationFilter.setPlaceholder("e.g. London...");
        locationFilter.setClearButtonVisible(true);

        // --- EXPLICIT SEARCH BUTTON ---
        Button searchBtn = new Button("Search & Filter", VaadinIcon.FILTER.create());
        searchBtn.addThemeName("primary");
        
        searchBtn.addClickListener(e -> refreshGrid());

        Button clearBtn = new Button("Clear", VaadinIcon.CLOSE.create());
        clearBtn.addClickListener(e -> {
            searchField.clear();
            deptFilter.clear();
            typeFilter.clear();
            if (dataView != null) {
                dataView.refreshAll(); 
            }
        });

        HorizontalLayout toolbar = new HorizontalLayout(searchField, deptFilter, typeFilter, locationFilter, searchBtn, clearBtn);
        toolbar.setDefaultVerticalComponentAlignment(Alignment.BASELINE);
        toolbar.addClassNames(LumoUtility.Margin.Bottom.MEDIUM);
        return toolbar;
    }

    private void createGrid() {
        grid = new Grid<>(JobListing.class, false);
        grid.setSizeFull();
        grid.addThemeNames("row-stripes", "borders");

        // --- COLUMNS & SORTING ---
        grid.addColumn(JobListing::getTitle)
            .setHeader("Job Title")
            .setSortable(true)
            .setAutoWidth(true).setFlexGrow(1);

        grid.addColumn(JobListing::getDepartment)
            .setHeader("Department")
            .setSortable(true)
            .setAutoWidth(true);

        grid.addColumn(JobListing::getLocation)
            .setHeader("Location")
            .setSortable(true)
            .setAutoWidth(true);

        grid.addColumn(JobListing::getEmploymentType)
            .setHeader("Type")
            .setSortable(true);

        grid.addColumn(JobListing::getUrl)
            .setHeader("Url")
            .setSortable(true);
    }

    private void refreshGrid() {
        try{
        // Safely get the text from the UI boxes
        String keyword = searchField != null ? searchField.getValue() : "";
        String dept = deptFilter != null ? deptFilter.getValue() : "";
        String type = typeFilter != null ? typeFilter.getValue() : "";
        String location = locationFilter != null ? locationFilter.getValue() : "";

        java.util.List<com.example.demo.entity.JobListing> results = jobService.searchJobs(keyword, dept, type, location);
        
        grid.setItems(results);
        Notification.show("Success! Found " + results.size() + " jobs.", 3000, Notification.Position.MIDDLE);
        }catch (Exception e) {
            e.printStackTrace();
            Notification.show("ERROR: " + e.getMessage(), 5000, Notification.Position.MIDDLE);
        }    
    }
}