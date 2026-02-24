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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import com.vaadin.componentfactory.erte.tables.EnhancedRichTextEditorTables;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;
import com.vaadin.componentfactory.toolbar.ToolbarDialog;
import com.vaadin.componentfactory.toolbar.ToolbarPopover;
import com.vaadin.componentfactory.toolbar.ToolbarSelectPopup;
import com.vaadin.componentfactory.toolbar.ToolbarSlot;
import com.vaadin.componentfactory.toolbar.ToolbarSwitch;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/**
 * Interactive playground for the Enhanced Rich Text Editor.
 * <p>
 * Demonstrates tabstops, placeholders, readonly sections, toolbar slots,
 * toolbar helper classes (ToolbarSwitch, ToolbarPopover, ToolbarSelectPopup,
 * ToolbarDialog), keyboard shortcuts, whitespace indicators, and tables.
 * Delta output is shown in a side panel.
 */
@Route("")
@PageTitle("Enhanced RTE Playground")
public class ErtePlaygroundView extends HorizontalLayout {

    public ErtePlaygroundView() {
        setSizeFull();
        setPadding(false);
        setSpacing(false);
        getStyle().set("gap", "var(--lumo-space-m)");

        // --- Editor ---
        var editor = new EnhancedRichTextEditor();
        editor.setWidthFull();
        editor.setValueChangeMode(ValueChangeMode.TIMEOUT);

        // Tables addon
        var tables = EnhancedRichTextEditorTables.enable(editor);

        // Load sample templates for table styling
        try (InputStream is = getClass().getClassLoader()
                .getResourceAsStream("table-sample-templates.json")) {
            if (is != null) {
                tables.setTemplates(
                        (ObjectNode) JsonMapper.builder().build().readTree(is));
            }
        } catch (IOException ignored) {
            // Template loading is non-critical
        }

        // Tabstops
        editor.setTabStops(List.of(
                new TabStop(TabStop.Direction.LEFT, 150),
                new TabStop(TabStop.Direction.RIGHT, 350),
                new TabStop(TabStop.Direction.MIDDLE, 550)));

        // Placeholders
        List<Placeholder> placeholders = new ArrayList<>();

        Placeholder p1 = new Placeholder();
        p1.setText("V-1=Vaadin Ltd");
        p1.getFormat().put("italic", true);
        p1.getAltFormat().put("bold", true);
        placeholders.add(p1);

        Placeholder p2 = new Placeholder();
        p2.setText("V-2=Turku, Finland");
        p2.getAltFormat().put("link", "https://vaadin.com");
        placeholders.add(p2);

        Placeholder p3 = new Placeholder();
        p3.setText("V-3=2000");
        placeholders.add(p3);

        editor.setPlaceholders(placeholders);
        editor.setPlaceholderTags("@", "");
        editor.setPlaceholderAltAppearancePattern("(?<=\\=).*$");
        editor.addPlaceholderBeforeInsertListener(e -> e.insert());

        // Keyboard shortcuts
        editor.addStandardToolbarButtonShortcut(
                EnhancedRichTextEditor.ToolbarButton.ALIGN_CENTER,
                "F9", false, true, false); // Shift+F9
        editor.addToolbarFocusShortcut(
                "F10", false, true, false); // Shift+F10

        // --- Toolbar custom components ---

        // START slot
        var startBtn = new Button("S");
        startBtn.getElement().setAttribute("title", "Start slot button");
        editor.addToolbarComponents(ToolbarSlot.START, startBtn);

        // END slot
        var endBtn = new Button("E");
        endBtn.getElement().setAttribute("title", "End slot button");
        editor.addToolbarComponents(ToolbarSlot.END, endBtn);

        // AFTER_GROUP_HEADING — font ComboBox
        var fontCombo = new ComboBox<String>();
        fontCombo.setItems("Arial", "Courier", "Georgia", "Times New Roman");
        fontCombo.setPlaceholder("Font");
        fontCombo.setWidth("130px");
        fontCombo.setClearButtonVisible(true);
        editor.addToolbarComponents(ToolbarSlot.AFTER_GROUP_HEADING, fontCombo);

        // GROUP_CUSTOM — search TextField
        var searchField = new TextField();
        searchField.setPlaceholder("Search...");
        searchField.setWidth("120px");
        editor.addCustomToolbarComponents(searchField);

        // GROUP_CUSTOM — whitespace ToolbarSwitch
        var wsSwitch = new ToolbarSwitch("WS");
        wsSwitch.getElement().setAttribute("title", "Whitespace indicators");
        wsSwitch.getElement().setAttribute("tabindex", "0");
        editor.addCustomToolbarComponents(wsSwitch);

        // ToolbarPopover — color picker
        var colorSwitch = new ToolbarSwitch(VaadinIcon.PAINTBRUSH);
        colorSwitch.getElement().setAttribute("title",
                "Color picker (ToolbarPopover)");
        var colorField = new TextField("Color");
        colorField.setPlaceholder("#000000");
        colorField.setWidth("120px");
        var applyColorBtn = new Button("Apply");
        applyColorBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY,
                ButtonVariant.LUMO_SMALL);
        var colorPopover = ToolbarPopover.vertical(colorSwitch, colorField,
                applyColorBtn);
        colorPopover.setFocusOnOpenTarget(colorField);
        editor.addToolbarComponents(ToolbarSlot.GROUP_CUSTOM, colorSwitch);

        // ToolbarSelectPopup — insert menu
        var insertSwitch = new ToolbarSwitch(VaadinIcon.PLUS);
        insertSwitch.getElement().setAttribute("title",
                "Insert menu (ToolbarSelectPopup)");
        var insertMenu = new ToolbarSelectPopup(insertSwitch);
        insertMenu.addItem("Horizontal Rule", e -> { /* no-op demo */ });
        insertMenu.addItem("Page Break", e -> { /* no-op demo */ });
        insertMenu.addComponent(new Hr());
        insertMenu.addItem("Special Character...", e -> { /* no-op demo */ });
        editor.addToolbarComponents(ToolbarSlot.GROUP_CUSTOM, insertSwitch);

        // ToolbarDialog — settings
        var settingsSwitch = new ToolbarSwitch(VaadinIcon.COG);
        settingsSwitch.getElement().setAttribute("title",
                "Settings (ToolbarDialog)");
        var showRulers = new Checkbox("Show rulers");
        var showWhitespace = new Checkbox("Show whitespace");
        var autoSave = new Checkbox("Auto-save");
        ToolbarDialog.vertical(settingsSwitch, showRulers, showWhitespace,
                autoSave).openAtSwitch();
        editor.addToolbarComponents(ToolbarSlot.GROUP_CUSTOM, settingsSwitch);

        // --- Pre-loaded content ---
        var initialDelta =
            "[" +
                "{\"insert\":\"Welcome to ERTE V25\",\"attributes\":{\"bold\":true}}," +
                "{\"insert\":\"\\n\"}," +
                "{\"insert\":\"This editor demonstrates all features.\"}," +
                "{\"insert\":\"\\n\"}," +
                "{\"insert\":\"Editable text. \"}," +
                "{\"insert\":\"This section is protected.\",\"attributes\":{\"readonly\":true}}," +
                "{\"insert\":\" More editable text.\"}," +
                "{\"insert\":\"\\n\"}," +
                "{\"insert\":\"Tab stops: \"}," +
                "{\"insert\":{\"tab\":true}}," +
                "{\"insert\":\"Left-aligned\"}," +
                "{\"insert\":{\"tab\":true}}," +
                "{\"insert\":\"Right-aligned\"}," +
                "{\"insert\":{\"tab\":true}}," +
                "{\"insert\":\"Centered\"}," +
                "{\"insert\":\"\\n\"}," +
                "{\"insert\":\"Placeholder: \"}," +
                "{\"insert\":{\"placeholder\":{\"text\":\"V-1=Vaadin Ltd\"}}}," +
                "{\"insert\":\" — founded \"}," +
                "{\"insert\":{\"placeholder\":{\"text\":\"V-3=2000\"}}}," +
                "{\"insert\":\" in \"}," +
                "{\"insert\":{\"placeholder\":{\"text\":\"V-2=Turku, Finland\"}}}," +
                "{\"insert\":\".\"}," +
                "{\"insert\":\"\\n\"}," +
                "{\"insert\":\"\\n\"}," +
                "{\"insert\":\"Lorem ipsum dolor sit amet, consectetur adipiscing elit. " +
                    "Sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. " +
                    "Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris " +
                    "nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in " +
                    "reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla " +
                    "pariatur. Excepteur sint occaecat cupidatat non proident, sunt in " +
                    "culpa qui officia deserunt mollit anim id est laborum.\"}," +
                "{\"insert\":\"\\n\",\"attributes\":{\"align\":\"justify\"}}," +
                // --- Sample Table (3 columns x 3 rows) ---
                "{\"insert\":\"\\n\"}," +
                "{\"insert\":\"Sample Table\",\"attributes\":{\"bold\":true}}," +
                "{\"insert\":\"\\n\"}," +
                // Row 1 — Header
                "{\"insert\":\"Feature\",\"attributes\":{\"bold\":true}}," +
                "{\"insert\":\"\\n\",\"attributes\":{\"td\":\"demo|r1|c1||||\"}}," +
                "{\"insert\":\"Status\",\"attributes\":{\"bold\":true}}," +
                "{\"insert\":\"\\n\",\"attributes\":{\"td\":\"demo|r1|c2||||\"}}," +
                "{\"insert\":\"Notes\",\"attributes\":{\"bold\":true}}," +
                "{\"insert\":\"\\n\",\"attributes\":{\"td\":\"demo|r1|c3||||\"}}," +
                // Row 2
                "{\"insert\":\"Tabstops\"}," +
                "{\"insert\":\"\\n\",\"attributes\":{\"td\":\"demo|r2|c1||||\"}}," +
                "{\"insert\":\"Complete\"}," +
                "{\"insert\":\"\\n\",\"attributes\":{\"td\":\"demo|r2|c2||||\"}}," +
                "{\"insert\":\"Left, right, center alignment\"}," +
                "{\"insert\":\"\\n\",\"attributes\":{\"td\":\"demo|r2|c3||||\"}}," +
                // Row 3
                "{\"insert\":\"Placeholders\"}," +
                "{\"insert\":\"\\n\",\"attributes\":{\"td\":\"demo|r3|c1||||\"}}," +
                "{\"insert\":\"Complete\"}," +
                "{\"insert\":\"\\n\",\"attributes\":{\"td\":\"demo|r3|c2||||\"}}," +
                "{\"insert\":\"Insert via dialog or shortcut\"}," +
                "{\"insert\":\"\\n\",\"attributes\":{\"td\":\"demo|r3|c3||||\"}}" +
            "]";
        editor.asDelta().setValue(initialDelta);

        // --- Delta output panel ---
        var deltaOutput = new Pre();
        deltaOutput.getStyle()
                .set("white-space", "pre-wrap")
                .set("word-break", "break-all")
                .set("font-size", "var(--lumo-font-size-xs)")
                .set("overflow", "auto")
                .set("background", "var(--lumo-shade-5pct)")
                .set("padding", "var(--lumo-space-s)")
                .set("margin", "0")
                .set("border-radius", "var(--lumo-border-radius-m)");

        deltaOutput.setText(initialDelta);
        editor.asDelta().addValueChangeListener(e ->
                deltaOutput.setText(e.getValue()));

        // --- Toolbar legend ---
        var legend = new Span(
                "Toolbar helpers: WS = ToolbarSwitch, " +
                VaadinIcon.PAINTBRUSH.name() + " = ToolbarPopover, " +
                VaadinIcon.PLUS.name() + " = ToolbarSelectPopup, " +
                VaadinIcon.COG.name() + " = ToolbarDialog");
        legend.getStyle()
                .set("font-size", "var(--lumo-font-size-xs)")
                .set("color", "var(--lumo-secondary-text-color)");

        // --- Layout: editor left (2), delta right (1) ---
        var editorPanel = new VerticalLayout(editor, legend);
        editorPanel.setPadding(true);
        editorPanel.setSpacing(false);
        editorPanel.getStyle()
                .set("flex", "2")
                .set("min-width", "0")
                .set("gap", "var(--lumo-space-xs)");
        editorPanel.setFlexGrow(1, editor);

        var deltaPanel = new VerticalLayout(deltaOutput);
        deltaPanel.setPadding(true);
        deltaPanel.setSpacing(false);
        deltaPanel.getStyle()
                .set("flex", "1")
                .set("min-width", "0")
                .set("overflow", "hidden");
        deltaPanel.setFlexGrow(1, deltaOutput);

        add(editorPanel, deltaPanel);
    }
}
