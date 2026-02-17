package com.vaadin.componentfactory.demo25;

import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.theme.lumo.Lumo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * V25 Demo application entry point.
 * <p>
 * Note: Vaadin 25 requires explicit Lumo loading via @StyleSheet
 * (no longer auto-loaded as in V24).
 */
@SpringBootApplication
@StyleSheet(Lumo.STYLESHEET)
public class Application implements AppShellConfigurator {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
