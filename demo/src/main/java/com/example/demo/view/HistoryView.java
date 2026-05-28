package com.example.demo.view;

import java.time.format.DateTimeFormatter;

import org.springframework.data.domain.Sort;

import com.example.demo.entity.ScrapeHistory;
import com.example.demo.repository.ScrapeHistoryRepository;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@Route(value = "history", layout = MainLayout.class)
@PageTitle("Scraping History | AI Job Board")
public class HistoryView extends VerticalLayout {

    public HistoryView(ScrapeHistoryRepository historyRepo) {
        setSizeFull();
        setPadding(true);
        setSpacing(true);

        add(new H2("Scraping History Log"));

        Grid<ScrapeHistory> grid = new Grid<>(ScrapeHistory.class, false);
        grid.setSizeFull();
        grid.addThemeNames("row-stripes", "borders");

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");

        grid.addColumn(history -> history.getScrapedAt() != null ? history.getScrapedAt().format(formatter) : "")
            .setHeader("Time Scraped")
            .setSortable(true)
            .setAutoWidth(true);

        grid.addColumn(ScrapeHistory::getCompanyName)
            .setHeader("Company")
            .setSortable(true)
            .setAutoWidth(true);

        grid.addColumn(ScrapeHistory::getTargetUrl)
            .setHeader("URL")
            .setAutoWidth(true)
            .setFlexGrow(1); // Give the URL column the most space

        grid.addColumn(ScrapeHistory::getJobsFound)
            .setHeader("Jobs Found")
            .setSortable(true);

        grid.setItems(historyRepo.findAll(Sort.by(Sort.Direction.DESC, "scrapedAt")));

        add(grid);
    }
}