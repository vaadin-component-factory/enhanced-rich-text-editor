package com.vaadin.componentfactory;

import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.ColorScheme;
import com.vaadin.flow.theme.lumo.Lumo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Minimal Spring Boot application for ERTE integration tests.
 * Serves only test views on port 8081.
 */
@SpringBootApplication
@StyleSheet(Lumo.STYLESHEET)
@ColorScheme(ColorScheme.Value.DARK)
public class ItApplication implements AppShellConfigurator {

    public static void main(String[] args) {
        SpringApplication.run(ItApplication.class, args);
    }

}
