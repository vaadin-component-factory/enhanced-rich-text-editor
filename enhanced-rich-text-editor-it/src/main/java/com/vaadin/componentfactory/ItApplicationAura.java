package com.vaadin.componentfactory;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.ColorScheme;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.theme.aura.Aura;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot application for ERTE integration tests with Aura theme.
 * Activated via the {@code -Paura} Maven profile (port 8082).
 * <p>
 * Only ONE {@link AppShellConfigurator} may exist on the classpath.
 * The {@code aura} Maven profile excludes {@link ItApplication} so
 * this class is the sole configurator.
 */
@SpringBootApplication
@StyleSheet(Aura.STYLESHEET)
@ColorScheme(ColorScheme.Value.DARK)
public class ItApplicationAura implements AppShellConfigurator {

    public static void main(String[] args) {
        SpringApplication.run(ItApplicationAura.class, args);
    }

}
