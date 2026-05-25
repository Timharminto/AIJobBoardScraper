package com.example.demo.view;

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Header;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.theme.lumo.LumoUtility;

public class MainLayout extends AppLayout {

    public MainLayout() {
        createHeader();
        createDrawer();
    }

    private void createHeader() {
        // The hamburger menu button that toggles the side drawer
        DrawerToggle toggle = new DrawerToggle();

        // The title of your application in the navbar
        H1 title = new H1("AI Job Board");
        
        // Using Lumo utility classes to style the title nicely
        title.addClassNames(
            LumoUtility.FontSize.LARGE, 
            LumoUtility.Margin.MEDIUM
        );

        Header header = new Header(toggle, title);
        header.addClassNames(
            LumoUtility.AlignItems.CENTER, 
            LumoUtility.Display.FLEX, 
            LumoUtility.Width.FULL
        );

        addToNavbar(header);
    }

    private void createDrawer() {
        SideNav nav = new SideNav();

        // Adds the actual clickable tab to your sidebar!
        nav.addItem(new SideNavItem("Web Scraper", ScrapeView.class, VaadinIcon.DOWNLOAD.create()));

        Scroller scroller = new Scroller(nav);
        scroller.setClassName(LumoUtility.Padding.SMALL);

        addToDrawer(scroller);
    }
}
