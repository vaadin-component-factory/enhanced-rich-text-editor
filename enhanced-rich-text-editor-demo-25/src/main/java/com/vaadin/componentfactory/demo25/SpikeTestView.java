package com.vaadin.componentfactory.demo25;

import com.vaadin.componentfactory.Placeholder;
import com.vaadin.componentfactory.TabStop;
import com.vaadin.componentfactory.erte.tables.EnhancedRichTextEditorTables;
import com.vaadin.componentfactory.toolbar.ToolbarSlot;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.richtexteditor.EnhancedRichTextEditor;
import com.vaadin.flow.component.richtexteditor.EnhancedRichTextEditor.EnhancedRichTextEditorI18n;
import com.vaadin.flow.component.richtexteditor.EnhancedRichTextEditor.ToolbarButton;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.router.Route;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * V25 Demo view for the Enhanced Rich Text Editor.
 * <p>
 * Comprehensive feature verification view for V25 migration Phase 3.
 */
@Route("test")
public class SpikeTestView extends VerticalLayout {

    private final Pre resultArea;
    private final EnhancedRichTextEditor editor;
    private EnhancedRichTextEditorTables tables;

    public SpikeTestView() {
        setSizeFull();
        setPadding(true);

        add(new H3("ERTE v25 Demo"));

        // --- Editor ---
        editor = new EnhancedRichTextEditor();
        editor.setWidthFull();
        editor.setMaxHeight("400px");

        // --- Tab stops ---
        editor.setTabStops(List.of(
            new TabStop(TabStop.Direction.LEFT, 150),
            new TabStop(TabStop.Direction.MIDDLE, 300),
            new TabStop(TabStop.Direction.RIGHT, 450)
        ));

        // --- Result area ---
        resultArea = new Pre();
        resultArea.getStyle()
                .set("padding", "8px")
                .set("background", "#f8f8f8")
                .set("border", "1px solid #ddd")
                .set("border-radius", "4px")
                .set("font-size", "12px")
                .set("max-height", "200px")
                .set("overflow", "auto")
                .set("white-space", "pre-wrap");
        resultArea.setText("Results will appear here...");

        // --- Section: Basic ---
        add(new H4("Basic Verification"));
        add(createBasicButtons());

        // --- Section: Placeholders ---
        add(new H4("Placeholders"));
        add(createPlaceholderButtons());

        // --- Section: Toolbar ---
        add(new H4("Toolbar Visibility & Custom Components"));
        add(createToolbarButtons());

        // --- Section: Tables ---
        add(new H4("Tables Extension"));
        add(createTableButtons());

        // --- Section: i18n ---
        add(new H4("i18n / Edge Cases"));
        add(createI18nButtons());

        add(editor, resultArea);
    }

    // ================================================================
    // Basic
    // ================================================================

    private Div createBasicButtons() {
        Button checkTagButton = new Button("Check Tag Name", e ->
            editor.getElement().executeJs("return $0.tagName")
                .then(String.class, tag -> log("Tag: " + tag))
        );

        Button toggleReadOnly = new Button("Toggle ReadOnly", e -> {
            boolean current = editor.isReadOnly();
            editor.setReadOnly(!current);
            log("ReadOnly: " + !current);
        });

        Button setValueButton = new Button("Set HTML Value", e -> {
            editor.asHtml().setValue("<p>Content set from <strong>Java</strong> at " +
                System.currentTimeMillis() + "</p>");
            log("Value set from Java");
        });

        Button getValueButton = new Button("Get HTML Value", e ->
            log("Value: " + editor.getValue())
        );

        Button toggleWhitespace = new Button("Toggle Whitespace", e -> {
            boolean current = editor.isShowWhitespace();
            editor.setShowWhitespace(!current);
            log("Whitespace: " + !current);
        });

        Button checkTabStops = new Button("Check TabStops", e ->
            log("TabStops: " + editor.getTabStops().size() + " defined: " +
                editor.getTabStops().stream()
                    .map(t -> t.getDirection() + "@" + (int) t.getPosition())
                    .reduce((a, b) -> a + ", " + b).orElse("none"))
        );

        Div buttons = new Div(checkTagButton, toggleReadOnly, setValueButton,
                getValueButton, toggleWhitespace, checkTabStops);
        buttons.getStyle().set("display", "flex").set("gap", "8px").set("flex-wrap", "wrap");
        return buttons;
    }

    // ================================================================
    // Placeholders
    // ================================================================

    private Div createPlaceholderButtons() {
        // Define placeholders
        Placeholder p1 = new Placeholder();
        p1.setText("FirstName");
        var fmt1 = JacksonUtils.createObjectNode();
        fmt1.put("bold", true);
        p1.setFormat(fmt1);

        Placeholder p2 = new Placeholder();
        p2.setText("LastName");
        var fmt2 = JacksonUtils.createObjectNode();
        fmt2.put("italic", true);
        p2.setFormat(fmt2);

        Placeholder p3 = new Placeholder();
        p3.setText("Company");

        // Register listener once (not per click) to avoid listener leak
        editor.addPlaceholderButtonClickedListener(evt -> {
            evt.insert(p1);
            log("Placeholder inserted: " + p1.getText() + " at pos " + evt.getPosition());
        });

        Button setupPlaceholders = new Button("Setup Placeholders", e -> {
            editor.setPlaceholders(List.of(p1, p2, p3));
            log("Placeholders set: FirstName (bold), LastName (italic), Company");
        });

        Button toggleAltAppearance = new Button("Toggle Alt Appearance", e -> {
            boolean current = editor.isPlaceholderAltAppearance();
            editor.setPlaceholderAltAppearance(!current);
            log("Alt appearance: " + !current);
        });

        Button setAltPattern = new Button("Set Alt Pattern", e -> {
            editor.setPlaceholderAltAppearancePattern("^First.*");
            log("Alt pattern set: ^First.*");
        });

        Div buttons = new Div(setupPlaceholders, toggleAltAppearance, setAltPattern);
        buttons.getStyle().set("display", "flex").set("gap", "8px").set("flex-wrap", "wrap");
        return buttons;
    }

    // ================================================================
    // Toolbar Visibility & Custom Components
    // ================================================================

    private Div createToolbarButtons() {
        Button hideHeadings = new Button("Hide H1/H2/H3", e -> {
            Map<ToolbarButton, Boolean> vis = new LinkedHashMap<>();
            vis.put(ToolbarButton.H1, false);
            vis.put(ToolbarButton.H2, false);
            vis.put(ToolbarButton.H3, false);
            editor.setToolbarButtonsVisibility(vis);
            log("Headings hidden");
        });

        Button showAll = new Button("Show All Buttons", e -> {
            editor.setToolbarButtonsVisibility(Map.of());
            log("All buttons shown");
        });

        Button addCustomComponent = new Button("Add Custom Component", e -> {
            Button customBtn = new Button("My Custom");
            customBtn.setId("my-custom-btn");
            customBtn.addClickListener(click -> log("Custom button clicked!"));
            editor.addToolbarComponents(ToolbarSlot.END, customBtn);
            log("Custom component added to toolbar END slot");
        });

        Button addStartSlot = new Button("Add START Slot", e -> {
            Span label = new Span("ERTE");
            label.getStyle().set("font-weight", "bold").set("padding", "0 8px");
            editor.addToolbarComponents(ToolbarSlot.START, label);
            log("Label added to START slot");
        });

        Div buttons = new Div(hideHeadings, showAll, addCustomComponent, addStartSlot);
        buttons.getStyle().set("display", "flex").set("gap", "8px").set("flex-wrap", "wrap");
        return buttons;
    }

    // ================================================================
    // Tables
    // ================================================================

    private Div createTableButtons() {
        Button enableTables = new Button("Enable Tables", e -> {
            if (tables == null) {
                tables = EnhancedRichTextEditorTables.enable(editor);
                log("Tables extension enabled");
            } else {
                log("Tables already enabled");
            }
        });

        Button insertTable = new Button("Insert 2x2 Table", e -> {
            if (tables != null) {
                tables.insertTableAtCurrentPosition(2, 2);
                log("2x2 table inserted");
            } else {
                log("ERROR: Enable tables first!");
            }
        });

        Button insertTable3x3 = new Button("Insert 3x3 Table", e -> {
            if (tables != null) {
                tables.insertTableAtCurrentPosition(3, 3);
                log("3x3 table inserted");
            } else {
                log("ERROR: Enable tables first!");
            }
        });

        Div buttons = new Div(enableTables, insertTable, insertTable3x3);
        buttons.getStyle().set("display", "flex").set("gap", "8px").set("flex-wrap", "wrap");
        return buttons;
    }

    // ================================================================
    // i18n & Edge Cases
    // ================================================================

    private Div createI18nButtons() {
        Button setGermanI18n = new Button("Set German i18n", e -> {
            EnhancedRichTextEditorI18n i18n = new EnhancedRichTextEditorI18n();
            i18n.setUndo("Rückgängig")
                .setRedo("Wiederholen")
                .setBold("Fett")
                .setItalic("Kursiv")
                .setUnderline("Unterstrichen")
                .setStrike("Durchgestrichen")
                .setH1("Überschrift 1")
                .setH2("Überschrift 2")
                .setH3("Überschrift 3")
                .setSubscript("Tiefgestellt")
                .setSuperscript("Hochgestellt")
                .setListOrdered("Nummerierte Liste")
                .setListBullet("Aufzählung")
                .setAlignLeft("Linksbündig")
                .setAlignCenter("Zentriert")
                .setAlignRight("Rechtsbündig")
                .setImage("Bild")
                .setLink("Link")
                .setBlockquote("Zitat")
                .setCodeBlock("Code-Block")
                .setClean("Formatierung entfernen");
            i18n.setReadonly("Schreibgeschützt");
            i18n.setPlaceholder("Platzhalter");
            i18n.setDeindent("Einzug verringern");
            editor.setEnhancedI18n(i18n);
            log("German i18n set");
        });

        Button setLargeContent = new Button("Set Large Content", e -> {
            StringBuilder sb = new StringBuilder();
            for (int i = 1; i <= 50; i++) {
                sb.append("<p>Paragraph ").append(i)
                  .append(": Lorem ipsum dolor sit amet, consectetur adipiscing elit.</p>");
            }
            editor.asHtml().setValue(sb.toString());
            log("50 paragraphs set");
        });

        Button toggleNoRulers = new Button("Toggle noRulers", e -> {
            boolean current = editor.isNoRulers();
            editor.setNoRulers(!current);
            log("noRulers: " + !current);
        });

        Button setTabsContent = new Button("Set Tab Content", e -> {
            // Set content with tabs via HTML -- tabs will be rendered
            // by Quill after the HTML is parsed
            editor.asHtml().setValue(
                "<p>Before tab<span class=\"ql-tab\"></span>After first tab" +
                "<span class=\"ql-tab\"></span>After second tab" +
                "<span class=\"ql-tab\"></span>After third tab</p>" +
                "<p>Line 2<span class=\"ql-tab\"></span>Col2</p>");
            log("Content with tabs set");
        });

        Div buttons = new Div(setGermanI18n, setLargeContent, toggleNoRulers, setTabsContent);
        buttons.getStyle().set("display", "flex").set("gap", "8px").set("flex-wrap", "wrap");
        return buttons;
    }

    // ================================================================
    // Logging
    // ================================================================

    private void log(String message) {
        String current = resultArea.getText();
        resultArea.setText(message + "\n" + current);
    }
}
