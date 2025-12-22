package com.vaadin.componentfactory;


import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.theme.Theme;
import com.vaadin.flow.theme.lumo.Lumo;
import com.vaadin.flow.theme.lumo.LumoUtility;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

/**
 * The entry point of the Spring Boot application.
 * <p>
 * Use the @PWA annotation make the application installable on phones, tablets
 * and some desktop browsers.
 *
 */
@SpringBootApplication
@PageTitle("VCF Enhanced RTE Demo")
@Theme(themeClass = Lumo.class, variant = Lumo.DARK)
public class Application extends SpringBootServletInitializer implements AppShellConfigurator {


    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}