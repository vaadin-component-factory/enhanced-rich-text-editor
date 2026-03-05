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

import com.vaadin.componentfactory.toolbar.ToolbarPopover;
import com.vaadin.componentfactory.toolbar.ToolbarSlot;
import com.vaadin.componentfactory.toolbar.ToolbarSwitch;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;

/**
 * Test view for ToolbarPopover helper class (Phase 3.4g).
 * <p>
 * Provides:
 * <ul>
 *   <li>Three popovers with different configurations (vertical, horizontal, custom focus)</li>
 *   <li>Event log for popover and switch state changes</li>
 *   <li>Control buttons for programmatic open/close</li>
 *   <li>Ready indicator for Playwright</li>
 * </ul>
 */
@Route("erte-test/toolbar-popover")
public class ErteToolbarPopoverTestView extends VerticalLayout {

    private final EnhancedRichTextEditor editor;
    private final Pre eventLog;

    private final ToolbarSwitch colorSwitch;
    private final ToolbarPopover colorPopover;

    private final ToolbarSwitch alignSwitch;
    private final ToolbarPopover alignPopover;

    private final ToolbarSwitch inputSwitch;
    private final ToolbarPopover inputPopover;
    private final TextField focusField;

    public ErteToolbarPopoverTestView() {
        setSizeFull();
        setPadding(true);
        setSpacing(true);

        // --- Editor ---
        editor = new EnhancedRichTextEditor();
        editor.setId("test-editor");
        editor.setValue("<p>ToolbarPopover test content</p>");

        // --- Test 1: Vertical popover with color picker ---
        colorSwitch = new ToolbarSwitch(VaadinIcon.PAINTBRUSH);
        colorSwitch.setId("color-switch");

        TextField colorField = new TextField("Color");
        colorField.setId("color-field");
        colorField.setPlaceholder("#000000");
        Button applyColorBtn = new Button("Apply");
        applyColorBtn.setId("apply-color-btn");

        colorPopover = ToolbarPopover.vertical(colorSwitch, colorField, applyColorBtn);
        colorPopover.setId("color-popover");

        editor.addToolbarComponents(ToolbarSlot.GROUP_CUSTOM, colorSwitch);

        // --- Test 2: Horizontal popover with alignment buttons ---
        alignSwitch = new ToolbarSwitch(VaadinIcon.ALIGN_LEFT);
        alignSwitch.setId("align-switch");

        Button leftBtn = new Button("Left");
        leftBtn.setId("align-left-btn");
        Button centerBtn = new Button("Center");
        centerBtn.setId("align-center-btn");
        Button rightBtn = new Button("Right");
        rightBtn.setId("align-right-btn");

        alignPopover = ToolbarPopover.horizontal(alignSwitch,
                FlexComponent.Alignment.CENTER, leftBtn, centerBtn, rightBtn);
        alignPopover.setId("align-popover");

        editor.addToolbarComponents(ToolbarSlot.GROUP_CUSTOM, alignSwitch);

        // --- Test 3: Custom focus target ---
        inputSwitch = new ToolbarSwitch(VaadinIcon.EDIT);
        inputSwitch.setId("input-switch");

        focusField = new TextField("Focus me");
        focusField.setId("focus-field");
        Button okBtn = new Button("OK");
        okBtn.setId("ok-btn");

        inputPopover = ToolbarPopover.vertical(inputSwitch, focusField, okBtn);
        inputPopover.setId("input-popover");
        inputPopover.setFocusOnOpenTarget(focusField);

        editor.addToolbarComponents(ToolbarSlot.GROUP_CUSTOM, inputSwitch);

        // --- Event log ---
        eventLog = new Pre();
        eventLog.setId("event-log");
        eventLog.getStyle().set("max-height", "200px").set("overflow", "auto");

        // Listen to all popover and switch events
        colorPopover.addOpenedChangeListener(e ->
            log("color-popover opened=" + e.isOpened()));
        colorSwitch.addActiveChangedListener(e ->
            log("color-switch active=" + e.isActive()));

        alignPopover.addOpenedChangeListener(e ->
            log("align-popover opened=" + e.isOpened()));
        alignSwitch.addActiveChangedListener(e ->
            log("align-switch active=" + e.isActive()));

        inputPopover.addOpenedChangeListener(e ->
            log("input-popover opened=" + e.isOpened()));
        inputSwitch.addActiveChangedListener(e ->
            log("input-switch active=" + e.isActive()));

        // --- Control buttons ---
        Button openColorBtn = new Button("Open Color Popover", e -> {
            colorPopover.setOpened(true);
            log("Programmatically opened color-popover");
        });
        openColorBtn.setId("open-color-btn");

        Button closeColorBtn = new Button("Close Color Popover", e -> {
            colorPopover.setOpened(false);
            log("Programmatically closed color-popover");
        });
        closeColorBtn.setId("close-color-btn");

        Button clearLogBtn = new Button("Clear Log", e -> eventLog.setText(""));
        clearLogBtn.setId("clear-log-btn");

        // --- Ready indicator ---
        Div ready = new Div();
        ready.setId("test-ready");
        ready.getStyle().set("display", "none");

        // --- Layout ---
        add(
            new H3("ToolbarPopover Test View (Phase 3.4g)"),
            editor,
            new H3("Event Log"),
            eventLog,
            new H3("Controls"),
            new Div(openColorBtn, closeColorBtn, clearLogBtn),
            ready
        );
    }

    private void log(String message) {
        String current = eventLog.getText();
        eventLog.setText(current + message + "\n");
    }
}
