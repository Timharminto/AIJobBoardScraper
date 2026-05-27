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
import com.vaadin.flow.data.value.ValueChangeMode;
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
        TextField searchField = new TextField();
        searchField.setPlaceholder("Search titles, locations...");
        searchField.setPrefixComponent(VaadinIcon.SEARCH.create());
        searchField.setClearButtonVisible(true);
        searchField.setValueChangeMode(ValueChangeMode.LAZY); 
        searchField.addValueChangeListener(e -> dataView.refreshAll());

        // --- FILTER: DEPARTMENT (Free Text) ---
        TextField deptFilter = new TextField("Department");
        deptFilter.setPlaceholder("e.g. Engineering...");
        deptFilter.setClearButtonVisible(true);
        deptFilter.setValueChangeMode(ValueChangeMode.LAZY); // Filters as you type!
        deptFilter.addValueChangeListener(e -> dataView.refreshAll());

        // --- FILTER: EMPLOYMENT TYPE (Free Text) ---
        TextField typeFilter = new TextField("Job Type");
        typeFilter.setPlaceholder("e.g. Full-time...");
        typeFilter.setClearButtonVisible(true);
        typeFilter.setValueChangeMode(ValueChangeMode.LAZY); // Filters as you type!
        typeFilter.addValueChangeListener(e -> dataView.refreshAll());

        // --- FILTER: LOCATION TYPE (Free Text) ---
        TextField locationFilter = new TextField("Location");
        locationFilter.setPlaceholder("e.g. London...");
        locationFilter.setClearButtonVisible(true);
        locationFilter.setValueChangeMode(ValueChangeMode.LAZY); // Filters as you type!
        locationFilter.addValueChangeListener(e -> dataView.refreshAll());

        // Link all free-text filters to the Grid's DataView logic
        dataView = grid.setItems(jobService.getAllJobs()); 
        dataView.addFilter(job -> {
            String searchTerm = searchField.getValue().trim().toLowerCase();
            String deptTerm = deptFilter.getValue().trim().toLowerCase();
            String typeTerm = typeFilter.getValue().trim().toLowerCase();
            String locationTerm = locationFilter.getValue().trim().toLowerCase();

            // 1. Check Search Box
            boolean matchesTerm = searchTerm.isEmpty() || 
                    (job.getTitle() != null && job.getTitle().toLowerCase().contains(searchTerm)) ||
                    (job.getLocation() != null && job.getLocation().toLowerCase().contains(searchTerm));

            // 2. Check Department Text
            boolean matchesDept = deptTerm.isEmpty() || 
                    (job.getDepartment() != null && job.getDepartment().toLowerCase().contains(deptTerm));

            // 3. Check Job Type Text
            boolean matchesType = typeTerm.isEmpty() || 
                    (job.getEmploymentType() != null && job.getEmploymentType().toLowerCase().contains(typeTerm));

             // 4. Check Job location Text
            boolean matchesLocation = locationTerm.isEmpty() || 
                    (job.getLocation() != null && job.getLocation().toLowerCase().contains(locationTerm));

            // Must match ALL active filters to show up in the grid
            return matchesTerm && matchesDept && matchesType && matchesLocation;
        });

        HorizontalLayout toolbar = new HorizontalLayout(searchField, deptFilter, typeFilter);
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
        if (dataView != null) {
            dataView = grid.setItems(jobService.getAllJobs()); 
        }
    }
}