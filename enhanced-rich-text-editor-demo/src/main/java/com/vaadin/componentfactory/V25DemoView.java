package com.vaadin.componentfactory;

import java.util.ArrayList;
import java.util.List;

import com.vaadin.componentfactory.erte.tables.EnhancedRichTextEditorTables;
import com.vaadin.componentfactory.toolbar.ToolbarDialog;
import com.vaadin.componentfactory.toolbar.ToolbarPopover;
import com.vaadin.componentfactory.toolbar.ToolbarSelectPopup;
import com.vaadin.componentfactory.toolbar.ToolbarSlot;
import com.vaadin.componentfactory.toolbar.ToolbarSwitch;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.page.ColorScheme;
import com.vaadin.flow.component.richtexteditor.RichTextEditor;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/**
 * All-in-one ERTE V25 feature showcase.
 * <p>
 * Shows a stock RTE for comparison, then an ERTE demonstrating tabstops,
 * placeholders, readonly sections, toolbar slots, toolbar helper classes
 * (ToolbarSwitch, ToolbarPopover, ToolbarSelectPopup, ToolbarDialog),
 * keyboard shortcuts, whitespace indicators, and dark/light mode toggle.
 */
@Route("")
@PageTitle("ERTE V25 Demo")
public class V25DemoView extends VerticalLayout {

    private boolean darkMode = false;

    public V25DemoView() {
        setSizeFull();
        setPadding(true);
        setSpacing(true);

        // --- Header bar ---
        var title = new H2("ERTE V25 Demo");
        title.getStyle().set("margin", "0");

        var themeToggle = new Button(VaadinIcon.MOON.create(), e -> toggleColorScheme(e.getSource()));
        themeToggle.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        themeToggle.getElement().setAttribute("aria-label", "Toggle dark/light mode");

        var header = new HorizontalLayout(title, themeToggle);
        header.setWidthFull();
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);

        // --- Stock RTE (reference) ---
        var rte = new RichTextEditor();
        rte.setWidthFull();
        rte.setValue("<p>Stock <b>RTE</b> from Vaadin 25 (for comparison)</p>");

        // --- ERTE ---
        var editor = new EnhancedRichTextEditor();
        editor.setWidthFull();

        // Tables addon
        EnhancedRichTextEditorTables.enable(editor);

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

        // START slot — button
        var startBtn = new Button("S");
        startBtn.getElement().setAttribute("title", "Start slot button");
        editor.addToolbarComponents(ToolbarSlot.START, startBtn);

        // END slot — button
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

        // --- ToolbarPopover example ---
        // A color picker popover triggered by a ToolbarSwitch
        var colorSwitch = new ToolbarSwitch(VaadinIcon.PAINTBRUSH);
        colorSwitch.getElement().setAttribute("title", "Color picker (ToolbarPopover)");
        var colorField = new TextField("Color");
        colorField.setPlaceholder("#000000");
        colorField.setWidth("120px");
        var applyColorBtn = new Button("Apply");
        applyColorBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
        var colorPopover = ToolbarPopover.vertical(colorSwitch, colorField, applyColorBtn);
        colorPopover.setFocusOnOpenTarget(colorField);
        editor.addToolbarComponents(ToolbarSlot.GROUP_CUSTOM, colorSwitch);

        // --- ToolbarSelectPopup example ---
        // A dropdown menu triggered by a ToolbarSwitch
        var insertSwitch = new ToolbarSwitch(VaadinIcon.PLUS);
        insertSwitch.getElement().setAttribute("title", "Insert menu (ToolbarSelectPopup)");
        var insertMenu = new ToolbarSelectPopup(insertSwitch);
        insertMenu.addItem("Horizontal Rule", e -> { /* no-op demo */ });
        insertMenu.addItem("Page Break", e -> { /* no-op demo */ });
        insertMenu.addComponent(new Hr());
        insertMenu.addItem("Special Character...", e -> { /* no-op demo */ });
        editor.addToolbarComponents(ToolbarSlot.GROUP_CUSTOM, insertSwitch);

        // --- ToolbarDialog example ---
        // A settings dialog triggered by a ToolbarSwitch, positioned at the switch
        var settingsSwitch = new ToolbarSwitch(VaadinIcon.COG);
        settingsSwitch.getElement().setAttribute("title", "Settings (ToolbarDialog)");
        var showRulers = new Checkbox("Show rulers");
        var showWhitespace = new Checkbox("Show whitespace");
        var autoSave = new Checkbox("Auto-save");
        ToolbarDialog.vertical(settingsSwitch, showRulers, showWhitespace, autoSave)
                .openAtSwitch();
        editor.addToolbarComponents(ToolbarSlot.GROUP_CUSTOM, settingsSwitch);

        // --- Pre-loaded content ---
        editor.asDelta().setValue(
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
                "{\"insert\":\"\\n\",\"attributes\":{\"align\":\"justify\"}}" +
            "]"
        );

        // --- Delta output ---
        var deltaOutput = new Pre();
        deltaOutput.getStyle()
                .set("white-space", "pre-wrap")
                .set("word-break", "break-all")
                .set("font-size", "var(--lumo-font-size-xs)")
                .set("max-height", "150px")
                .set("overflow", "auto")
                .set("background", "var(--lumo-shade-5pct)")
                .set("padding", "var(--lumo-space-s)")
                .set("margin", "0")
                .set("border-radius", "var(--lumo-border-radius-m)");

        editor.asDelta().addValueChangeListener(e ->
                deltaOutput.setText(e.getValue()));

        // --- Toolbar class legend ---
        var legend = new Span(
                "Toolbar helpers: WS = ToolbarSwitch, " +
                VaadinIcon.PAINTBRUSH.name() + " = ToolbarPopover, " +
                VaadinIcon.PLUS.name() + " = ToolbarSelectPopup, " +
                VaadinIcon.COG.name() + " = ToolbarDialog");
        legend.getStyle()
                .set("font-size", "var(--lumo-font-size-xs)")
                .set("color", "var(--lumo-secondary-text-color)");

        // --- Layout ---
        add(header,
                new H3("Stock RTE (reference)"), rte,
                new H3("Enhanced RTE"), editor,
                legend, deltaOutput);
        setFlexGrow(1, editor);
        setFlexGrow(0, rte);
        setFlexGrow(0, deltaOutput);
    }

    private void toggleColorScheme(Button button) {
        darkMode = !darkMode;
        var page = UI.getCurrent().getPage();
        if (darkMode) {
            page.setColorScheme(ColorScheme.Value.DARK);
            button.setIcon(VaadinIcon.SUN_O.create());
        } else {
            page.setColorScheme(ColorScheme.Value.LIGHT);
            button.setIcon(VaadinIcon.MOON.create());
        }
    }
}
