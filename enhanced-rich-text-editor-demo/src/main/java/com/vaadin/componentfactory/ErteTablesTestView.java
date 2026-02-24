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

import com.vaadin.componentfactory.erte.tables.EnhancedRichTextEditorTables;
import com.vaadin.componentfactory.erte.tables.templates.TemplateParser;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.Route;
import tools.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Test view for the Tables addon (Phase 4).
 */
@Route(value = "erte-test/tables", layout = ErteTestLayout.class)
public class ErteTablesTestView extends VerticalLayout {

    public ErteTablesTestView() {
        setSizeFull();

        EnhancedRichTextEditor editor = new EnhancedRichTextEditor();
        editor.setId("test-editor");
        editor.setValueChangeMode(ValueChangeMode.EAGER);
        editor.setMaxHeight("400px");

        // Event log
        Div eventLog = new Div();
        eventLog.setId("event-log");
        eventLog.getStyle()
                .set("font-size", "var(--lumo-font-size-xs)")
                .set("max-height", "100px")
                .set("overflow", "auto");

        // Enable tables addon
        EnhancedRichTextEditorTables tables = EnhancedRichTextEditorTables.enable(editor);

        // Load sample templates from classpath
        ObjectNode sampleTemplates = loadJsonResource("table-sample-templates.json");
        if (sampleTemplates != null) {
            tables.setTemplates(sampleTemplates);
        }

        // Load sample delta from classpath
        String sampleDelta = loadTextResource("table-sample-delta.json");
        if (sampleDelta != null) {
            editor.asDelta().setValue(sampleDelta);
        }

        // Event listeners
        tables.addTableSelectedListener(e -> {
            String msg = String.format("TableSelected: selected=%s, cellSelection=%s, template=%s",
                e.isSelected(), e.isCellSelectionActive(), e.getTemplate());
            eventLog.add(new Div(new com.vaadin.flow.component.html.Span(msg)));
        });

        tables.addTableCellChangedListener(e -> {
            String msg = String.format("CellChanged: row=%s, col=%s, oldRow=%s, oldCol=%s",
                e.getRowIndex(), e.getColIndex(), e.getOldRowIndex(), e.getOldColIndex());
            eventLog.add(new Div(new com.vaadin.flow.component.html.Span(msg)));
        });

        // HTML output
        Pre htmlOutput = new Pre();
        htmlOutput.setId("html-output");
        htmlOutput.getStyle()
                .set("white-space", "pre-wrap")
                .set("font-size", "var(--lumo-font-size-xs)")
                .set("max-height", "150px")
                .set("overflow", "auto")
                .set("background", "var(--lumo-contrast-5pct)")
                .set("padding", "var(--lumo-space-s)");
        editor.addValueChangeListener(e -> htmlOutput.setText(e.getValue()));

        // Delta input area
        TextArea deltaInput = new TextArea("Delta JSON");
        deltaInput.setId("delta-input");
        deltaInput.setWidthFull();
        deltaInput.setMaxHeight("200px");
        deltaInput.setPlaceholder("Paste Delta JSON here, then click 'Load Delta'");

        Button loadDelta = new Button("Load Delta", e -> {
            String json = deltaInput.getValue();
            if (json != null && !json.isBlank()) {
                // Accept both {"ops":[...]} and plain [...] array format
                String trimmed = json.strip();
                if (trimmed.startsWith("{")) {
                    // Extract ops array from {"ops":[...]}
                    int idx = trimmed.indexOf('[');
                    int lastIdx = trimmed.lastIndexOf(']');
                    if (idx >= 0 && lastIdx > idx) {
                        trimmed = trimmed.substring(idx, lastIdx + 1);
                    }
                }
                editor.asDelta().setValue(trimmed);
            }
        });
        loadDelta.setId("load-delta-btn");

        Button readDelta = new Button("Read Delta", e -> {
            deltaInput.setValue(editor.asDelta().getValue());
        });
        readDelta.setId("read-delta-btn");

        // Template JSON display
        TextArea templateOutput = new TextArea("Template JSON");
        templateOutput.setId("template-output");
        templateOutput.setWidthFull();
        templateOutput.setMaxHeight("200px");

        Button readTemplates = new Button("Read Templates", e -> {
            ObjectNode t = tables.getTemplates();
            templateOutput.setValue(t != null ? t.toString() : "null");
        });
        readTemplates.setId("read-templates-btn");

        Button loadTemplates = new Button("Load Templates", e -> {
            String json = templateOutput.getValue();
            if (json != null && !json.isBlank()) {
                try {
                    tables.setTemplates(TemplateParser.parseJson(json));
                    eventLog.add(new Div(new com.vaadin.flow.component.html.Span("Templates loaded successfully")));
                } catch (Exception ex) {
                    eventLog.add(new Div(new com.vaadin.flow.component.html.Span("Error loading templates: " + ex.getMessage())));
                }
            }
        });
        loadTemplates.setId("load-templates-btn");

        HorizontalLayout deltaButtons = new HorizontalLayout(loadDelta, readDelta, readTemplates, loadTemplates);

        add(editor, deltaInput, deltaButtons, templateOutput, htmlOutput, eventLog);

        // Hidden ready indicator
        Div ready = new Div();
        ready.setId("test-ready");
        ready.getStyle().set("display", "none");
        add(ready);
    }

    private ObjectNode loadJsonResource(String resourceName) {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(resourceName)) {
            if (is == null) return null;
            String json = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            return TemplateParser.parseJson(json);
        } catch (IOException e) {
            return null;
        }
    }

    private String loadTextResource(String resourceName) {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(resourceName)) {
            if (is == null) return null;
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return null;
        }
    }
}
