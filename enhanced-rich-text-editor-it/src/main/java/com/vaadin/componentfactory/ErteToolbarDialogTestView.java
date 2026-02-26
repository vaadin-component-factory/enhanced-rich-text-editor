/*-
 * #%L
 * Enhanced Rich Text Editor V25 Demo
 * %%
 * Copyright (C) 2019 - 2025 Vaadin Ltd
 * %%
 * This program is available under Commercial Vaadin Add-On License 3.0
 * (CVALv3).
 *
 * See the file license.html distributed with this software for more
 * information about licensing.
 *
 * You should have received a copy of the CVALv3 along with this program.
 * If not, see <http://vaadin.com/license/cval-3>.
 * #L%
 */
package com.vaadin.componentfactory;

import com.vaadin.componentfactory.toolbar.ToolbarDialog;
import com.vaadin.componentfactory.toolbar.ToolbarSwitch;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Test view for ToolbarDialog component.
 * Tests dialog integration with ToolbarSwitch and positioning.
 */
@Route("erte-test/toolbar-dialog")
public class ErteToolbarDialogTestView extends VerticalLayout {

    private final VerticalLayout eventLog = new VerticalLayout();
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    public ErteToolbarDialogTestView() {
        setSizeFull();
        setPadding(true);

        add(new H2("ToolbarDialog Test"));

        // Test 1: Center-positioned dialog (vertical layout)
        add(new H3("1. Center-positioned (vertical layout)"));
        add(createCenterVerticalTest());

        // Test 2: Center-positioned dialog (horizontal layout)
        add(new H3("2. Center-positioned (horizontal layout)"));
        add(createCenterHorizontalTest());

        // Test 3: Positioned at switch
        add(new H3("3. Positioned at switch"));
        add(createPositionedAtSwitchTest());

        // Test 4: Focus management
        add(new H3("4. Focus management"));
        add(createFocusManagementTest());

        // Event log
        add(new H2("Event Log"));
        eventLog.setId("event-log");
        eventLog.setPadding(false);
        eventLog.getStyle()
                .set("border", "1px solid var(--lumo-contrast-20pct)")
                .set("padding", "var(--lumo-space-s)")
                .set("max-height", "300px")
                .set("overflow-y", "auto")
                .set("font-family", "monospace")
                .set("font-size", "var(--lumo-font-size-s)");
        add(eventLog);

        // Ready indicator
        Div ready = new Div();
        ready.setId("test-ready");
        ready.getStyle().set("display", "none");
        add(ready);

        log("Test view ready");
    }

    private HorizontalLayout createCenterVerticalTest() {
        HorizontalLayout layout = new HorizontalLayout();
        layout.setAlignItems(Alignment.CENTER);

        Span label = new Span("Vertical Dialog:");
        label.getStyle().set("margin-right", "var(--lumo-space-m)");

        ToolbarSwitch settingsSwitch = new ToolbarSwitch(VaadinIcon.COG);
        settingsSwitch.setId("settings-switch");

        ToolbarDialog settingsDialog = ToolbarDialog.vertical(settingsSwitch,
                new Checkbox("Show rulers"),
                new Checkbox("Show whitespace"),
                new Checkbox("Auto-save")
        );
        settingsDialog.setId("settings-dialog");

        settingsDialog.addOpenedChangeListener(e -> {
            log("Settings dialog opened: " + e.isOpened());
            if (e.isOpened()) {
                log("Dialog position: CENTER (default)");
            }
        });

        layout.add(label, settingsSwitch);
        return layout;
    }

    private HorizontalLayout createCenterHorizontalTest() {
        HorizontalLayout layout = new HorizontalLayout();
        layout.setAlignItems(Alignment.CENTER);

        Span label = new Span("Horizontal Dialog:");
        label.getStyle().set("margin-right", "var(--lumo-space-m)");

        ToolbarSwitch formatSwitch = new ToolbarSwitch(VaadinIcon.TEXT_HEIGHT);
        formatSwitch.setId("format-switch");

        Button boldBtn = new Button("Bold");
        boldBtn.addThemeVariants(ButtonVariant.LUMO_SMALL);
        boldBtn.addClickListener(e -> log("Bold clicked"));

        Button italicBtn = new Button("Italic");
        italicBtn.addThemeVariants(ButtonVariant.LUMO_SMALL);
        italicBtn.addClickListener(e -> log("Italic clicked"));

        Button underlineBtn = new Button("Underline");
        underlineBtn.addThemeVariants(ButtonVariant.LUMO_SMALL);
        underlineBtn.addClickListener(e -> log("Underline clicked"));

        ToolbarDialog formatDialog = ToolbarDialog.horizontal(formatSwitch,
                FlexComponent.Alignment.CENTER,
                boldBtn, italicBtn, underlineBtn
        );
        formatDialog.setId("format-dialog");

        formatDialog.addOpenedChangeListener(e -> {
            log("Format dialog opened: " + e.isOpened());
            if (e.isOpened()) {
                log("Dialog position: CENTER (default)");
            }
        });

        layout.add(label, formatSwitch);
        return layout;
    }

    private HorizontalLayout createPositionedAtSwitchTest() {
        HorizontalLayout layout = new HorizontalLayout();
        layout.setAlignItems(Alignment.CENTER);

        Span label = new Span("Positioned at Switch:");
        label.getStyle().set("margin-right", "var(--lumo-space-m)");

        ToolbarSwitch menuSwitch = new ToolbarSwitch(VaadinIcon.MENU);
        menuSwitch.setId("menu-switch");

        Button option1 = new Button("Option 1");
        option1.addThemeVariants(ButtonVariant.LUMO_SMALL);
        option1.addClickListener(e -> log("Option 1 clicked"));

        Button option2 = new Button("Option 2");
        option2.addThemeVariants(ButtonVariant.LUMO_SMALL);
        option2.addClickListener(e -> log("Option 2 clicked"));

        Button option3 = new Button("Option 3");
        option3.addThemeVariants(ButtonVariant.LUMO_SMALL);
        option3.addClickListener(e -> log("Option 3 clicked"));

        ToolbarDialog menuDialog = ToolbarDialog.horizontal(menuSwitch,
                FlexComponent.Alignment.CENTER,
                option1, option2, option3
        ).openAtSwitch();
        menuDialog.setId("menu-dialog");

        menuDialog.addOpenedChangeListener(e -> {
            log("Menu dialog opened: " + e.isOpened());
            if (e.isOpened()) {
                log("Dialog position: AT SWITCH (below toolbar button)");
            }
        });

        layout.add(label, menuSwitch);
        return layout;
    }

    private HorizontalLayout createFocusManagementTest() {
        HorizontalLayout layout = new HorizontalLayout();
        layout.setAlignItems(Alignment.CENTER);

        Span label = new Span("Focus Management:");
        label.getStyle().set("margin-right", "var(--lumo-space-m)");

        ToolbarSwitch inputSwitch = new ToolbarSwitch(VaadinIcon.EDIT);
        inputSwitch.setId("input-switch");

        TextField nameField = new TextField("Name");
        nameField.setId("name-field");
        nameField.setWidth("200px");

        Button submitBtn = new Button("Submit");
        submitBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
        submitBtn.addClickListener(e -> {
            log("Submitted: " + nameField.getValue());
            nameField.clear();
        });

        ToolbarDialog inputDialog = ToolbarDialog.vertical(inputSwitch,
                nameField,
                submitBtn
        );
        inputDialog.setId("input-dialog");
        inputDialog.setFocusOnOpenTarget(nameField);

        inputDialog.addOpenedChangeListener(e -> {
            log("Input dialog opened: " + e.isOpened());
            if (e.isOpened()) {
                log("Focus set to: name field");
            }
        });

        layout.add(label, inputSwitch);
        return layout;
    }

    private void log(String message) {
        String timestamp = LocalTime.now().format(timeFormatter);
        Div logEntry = new Div(new Span(timestamp + " — " + message));
        logEntry.getStyle().set("margin", "2px 0");
        eventLog.addComponentAtIndex(0, logEntry);
    }
}
