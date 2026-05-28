package com.example.demo.view;

import java.util.Map;

import com.example.demo.service.StatsService;
import com.github.appreciated.apexcharts.ApexCharts;
import com.github.appreciated.apexcharts.ApexChartsBuilder;
import com.github.appreciated.apexcharts.config.builder.ChartBuilder;
import com.github.appreciated.apexcharts.config.builder.LegendBuilder;
import com.github.appreciated.apexcharts.config.builder.PlotOptionsBuilder;
import com.github.appreciated.apexcharts.config.builder.XAxisBuilder;
import com.github.appreciated.apexcharts.config.chart.Type;
import com.github.appreciated.apexcharts.config.legend.Position;
import com.github.appreciated.apexcharts.config.plotoptions.builder.BarBuilder;
import com.github.appreciated.apexcharts.helper.Series;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.theme.lumo.LumoUtility;

@Route(value = "", layout = MainLayout.class)
@RouteAlias(value = "dashboard", layout = MainLayout.class)
@PageTitle("Dashboard | AI Job Board")
public class DashboardView extends VerticalLayout {

    private final StatsService statsService;

    public DashboardView(StatsService statsService) {
        this.statsService = statsService;
        
        setSizeFull();
        setPadding(true);
        setSpacing(true);

        // 1. Summary Board Table
        add(createSummaryBoard());

        // 2. Charts Container
        VerticalLayout chartsContainer = new VerticalLayout();
        chartsContainer.setWidthFull();
        chartsContainer.setPadding(false);

        // We now wrap each chart in our helper method to add the title below!
        chartsContainer.add(
            createChartWithTitleBelow(createCompanyChart(), "Jobs per Company"),
            createChartWithTitleBelow(createDepartmentChart(), "Department Distribution"),
            createChartWithTitleBelow(createTopJobsChart(), "Top 10 Most Frequent Jobs")
        );

        add(chartsContainer);
    }

    private HorizontalLayout createSummaryBoard() {
        HorizontalLayout board = new HorizontalLayout();
        board.setWidthFull();
        board.setJustifyContentMode(JustifyContentMode.BETWEEN);
        board.setAlignItems(Alignment.CENTER);
        board.addClassNames(LumoUtility.Background.BASE, LumoUtility.Padding.MEDIUM, LumoUtility.BorderRadius.LARGE, LumoUtility.BoxShadow.SMALL);

        VerticalLayout totalJobs = createStatBox("Total Job", String.valueOf(statsService.getTotalJobsCount()));
        VerticalLayout totalCompanies = createStatBox("Jumlah Perusahaan", String.valueOf(statsService.getTotalCompaniesCount()));
        VerticalLayout lastScraped = createStatBox("Tanggal Scraping Terakhir", statsService.getLastScrapingDate());

        Button refreshBtn = new Button("Refresh Data", VaadinIcon.REFRESH.create());
        refreshBtn.addClickListener(e -> com.vaadin.flow.component.UI.getCurrent().getPage().reload());
        refreshBtn.addClassNames(LumoUtility.Margin.Top.MEDIUM);

        board.add(totalJobs, totalCompanies, lastScraped, refreshBtn);
        return board;
    }

    private VerticalLayout createStatBox(String label, String value) {
        Span labelSpan = new Span(label);
        labelSpan.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.FontSize.SMALL);
        
        H2 valueH2 = new H2(value);
        valueH2.addClassNames(LumoUtility.Margin.NONE);

        VerticalLayout box = new VerticalLayout(labelSpan, valueH2);
        box.setSpacing(false);
        box.setPadding(false);
        return box;
    }

    // --- HELPER METHOD: Sizing and Titles Below ---
    private VerticalLayout createChartWithTitleBelow(ApexCharts chart, String titleText) {
        chart.setHeight("280px"); // This forces the charts to be much smaller
        chart.setWidthFull();

        Span title = new Span(titleText);
        title.addClassNames(LumoUtility.FontWeight.BOLD, LumoUtility.FontSize.LARGE, LumoUtility.Margin.Top.SMALL, LumoUtility.Margin.Bottom.MEDIUM);

        VerticalLayout layout = new VerticalLayout(chart, title);
        layout.setAlignItems(Alignment.CENTER);
        layout.setPadding(false);
        layout.setSpacing(false);
        return layout;
    }

    private ApexCharts createCompanyChart() {
        Map<String, Long> data = statsService.getJobCountByCompany();
        String[] categories = data.keySet().toArray(new String[0]);
        Double[] values = data.values().stream().map(Long::doubleValue).toArray(Double[]::new);

        return ApexChartsBuilder.get()
                .withChart(ChartBuilder.get().withType(Type.BAR).build())
                .withXaxis(XAxisBuilder.get().withCategories(categories).build())
                // 'distributed' tells the chart to color each bar differently and show categories in the legend!
                .withPlotOptions(PlotOptionsBuilder.get().withBar(BarBuilder.get().withDistributed(true).build()).build())
                .withLegend(LegendBuilder.get().withShow(true).withPosition(Position.RIGHT).build())
                .withSeries(new Series<>("Jobs", values))
                .build();
    }

    private ApexCharts createDepartmentChart() {
        Map<String, Long> data = statsService.getJobCountByDepartment();
        Double[] values = data.values().stream().map(Long::doubleValue).toArray(Double[]::new);
        String[] labels = data.keySet().toArray(new String[0]);

        return ApexChartsBuilder.get()
                .withChart(ChartBuilder.get().withType(Type.PIE).build())
                .withLabels(labels)
                .withSeries(values)
                .build();
    }

    private ApexCharts createTopJobsChart() {
        Map<String, Long> data = statsService.getTop10JobTitles();
        String[] categories = data.keySet().toArray(new String[0]);
        Double[] values = data.values().stream().map(Long::doubleValue).toArray(Double[]::new);

        return ApexChartsBuilder.get()
                .withChart(ChartBuilder.get().withType(Type.BAR).build())
                // 'isHorizontal' turns this into a horizontal bar chart so long job titles fit nicely
                .withPlotOptions(PlotOptionsBuilder.get().withBar(BarBuilder.get().withHorizontal(true).build()).build())
                .withXaxis(XAxisBuilder.get().withCategories(categories).build())
                .withSeries(new Series<>("Postings", values))
                .build();
    }
}