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

import com.vaadin.componentfactory.toolbar.ToolbarSelectPopup;
import com.vaadin.componentfactory.toolbar.ToolbarSwitch;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Test view for ToolbarSelectPopup component.
 * Tests context menu integration with ToolbarSwitch.
 */
@Route("erte-test/toolbar-select-popup")
public class ErteToolbarSelectPopupTestView extends VerticalLayout {

    private final VerticalLayout eventLog = new VerticalLayout();
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    public ErteToolbarSelectPopupTestView() {
        setSizeFull();
        setPadding(true);

        add(new H2("ToolbarSelectPopup Test"));

        // Test 1: Simple menu with text items
        add(createSimpleMenuTest());

        // Test 2: Menu with icons
        add(createIconMenuTest());

        // Test 3: Multiple menus
        add(createMultipleMenusTest());

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

    private HorizontalLayout createSimpleMenuTest() {
        HorizontalLayout layout = new HorizontalLayout();
        layout.setAlignItems(Alignment.CENTER);

        Span label = new Span("Simple Menu:");
        label.getStyle().set("margin-right", "var(--lumo-space-m)");

        ToolbarSwitch formatSwitch = new ToolbarSwitch(VaadinIcon.COGS);
        formatSwitch.setId("format-switch");

        ToolbarSelectPopup formatMenu = new ToolbarSelectPopup(formatSwitch);
        formatMenu.setId("format-menu");
        formatMenu.addItem("Bold", e -> log("Selected: Bold"));
        formatMenu.addItem("Italic", e -> log("Selected: Italic"));
        formatMenu.addItem("Underline", e -> log("Selected: Underline"));
        formatMenu.addComponent(new Hr());
        formatMenu.addItem("Clear Formatting", e -> log("Selected: Clear Formatting"));

        formatMenu.addOpenedChangeListener(e ->
            log("Format menu opened: " + e.isOpened())
        );

        layout.add(label, formatSwitch);
        return layout;
    }

    private HorizontalLayout createIconMenuTest() {
        HorizontalLayout layout = new HorizontalLayout();
        layout.setAlignItems(Alignment.CENTER);

        Span label = new Span("Icon Menu:");
        label.getStyle().set("margin-right", "var(--lumo-space-m)");

        ToolbarSwitch alignSwitch = new ToolbarSwitch(VaadinIcon.ALIGN_LEFT);
        alignSwitch.setId("align-switch");

        ToolbarSelectPopup alignMenu = new ToolbarSelectPopup(alignSwitch);
        alignMenu.setId("align-menu");

        // Create menu items with icon + text using HorizontalLayout
        alignMenu.addItem(createIconText(VaadinIcon.ALIGN_LEFT, "Align Left"),
            e -> log("Selected: Align Left"));
        alignMenu.addItem(createIconText(VaadinIcon.ALIGN_CENTER, "Align Center"),
            e -> log("Selected: Align Center"));
        alignMenu.addItem(createIconText(VaadinIcon.ALIGN_RIGHT, "Align Right"),
            e -> log("Selected: Align Right"));
        alignMenu.addItem(createIconText(VaadinIcon.ALIGN_JUSTIFY, "Justify"),
            e -> log("Selected: Justify"));

        alignMenu.addOpenedChangeListener(e ->
            log("Align menu opened: " + e.isOpened())
        );

        layout.add(label, alignSwitch);
        return layout;
    }

    private HorizontalLayout createIconText(VaadinIcon vaadinIcon, String text) {
        Icon icon = new Icon(vaadinIcon);
        icon.getStyle().set("margin-right", "var(--lumo-space-s)");
        Span textSpan = new Span(text);
        HorizontalLayout iconText = new HorizontalLayout(icon, textSpan);
        iconText.setAlignItems(Alignment.CENTER);
        iconText.setPadding(false);
        iconText.setSpacing(false);
        return iconText;
    }

    private HorizontalLayout createMultipleMenusTest() {
        HorizontalLayout layout = new HorizontalLayout();
        layout.setAlignItems(Alignment.CENTER);

        Span label = new Span("Multiple Menus:");
        label.getStyle().set("margin-right", "var(--lumo-space-m)");

        ToolbarSwitch switch1 = new ToolbarSwitch(VaadinIcon.EDIT);
        switch1.setId("switch-1");
        ToolbarSelectPopup menu1 = new ToolbarSelectPopup(switch1);
        menu1.setId("menu-1");
        menu1.addItem("Option A1", e -> log("Menu 1: Option A1"));
        menu1.addItem("Option A2", e -> log("Menu 1: Option A2"));
        menu1.addOpenedChangeListener(e -> log("Menu 1 opened: " + e.isOpened()));

        ToolbarSwitch switch2 = new ToolbarSwitch(VaadinIcon.OPTIONS);
        switch2.setId("switch-2");
        ToolbarSelectPopup menu2 = new ToolbarSelectPopup(switch2);
        menu2.setId("menu-2");
        menu2.addItem("Option B1", e -> log("Menu 2: Option B1"));
        menu2.addItem("Option B2", e -> log("Menu 2: Option B2"));
        menu2.addOpenedChangeListener(e -> log("Menu 2 opened: " + e.isOpened()));

        layout.add(label, switch1, switch2);
        return layout;
    }

    private void log(String message) {
        String timestamp = LocalTime.now().format(timeFormatter);
        Div logEntry = new Div(new Span(timestamp + " — " + message));
        logEntry.getStyle().set("margin", "2px 0");
        eventLog.addComponentAtIndex(0, logEntry);
    }
}
