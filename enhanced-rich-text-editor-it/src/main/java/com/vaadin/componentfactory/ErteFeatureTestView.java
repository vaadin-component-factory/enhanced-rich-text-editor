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

import java.util.ArrayList;
import java.util.List;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.richtexteditor.RichTextEditor;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.Route;

@Route(value = "erte-test/features", layout = ErteTestLayout.class)
public class ErteFeatureTestView extends VerticalLayout {

    private final Div eventLog;

    public ErteFeatureTestView() {
        setSizeFull();

        EnhancedRichTextEditor editor = new EnhancedRichTextEditor();
        editor.setId("test-editor");
        editor.setValueChangeMode(ValueChangeMode.EAGER);
        editor.setMaxHeight("400px");

        // Set tabstops for tab-related tests
        List<TabStop> tabStops = new ArrayList<>();
        tabStops.add(new TabStop(TabStop.Direction.LEFT, 150));
        tabStops.add(new TabStop(TabStop.Direction.RIGHT, 350));
        editor.setTabStops(tabStops);

        // Set placeholders for round-trip tests
        List<Placeholder> placeholders = new ArrayList<>();
        Placeholder p1 = new Placeholder();
        p1.setText("TestPlaceholder");
        placeholders.add(p1);
        editor.setPlaceholders(placeholders);
        editor.addPlaceholderBeforeInsertListener(e -> e.insert());

        eventLog = new Div();
        eventLog.setId("event-log");
        eventLog.getStyle()
                .set("max-height", "100px")
                .set("overflow", "auto")
                .set("font-size", "var(--lumo-font-size-xs)")
                .set("background", "var(--lumo-shade-5pct)")
                .set("padding", "var(--lumo-space-s)")
                .set("white-space", "pre-wrap");

        // Control buttons
        Button addTextAtCursor = new Button("Add Text at Cursor");
        addTextAtCursor.setId("add-text-cursor");
        addTextAtCursor.addClickListener(e -> {
            editor.addText("INSERTED");
            logEvent("addText: Inserted 'INSERTED' at cursor");
        });

        Button addTextAtPos = new Button("Add Text at Position 0");
        addTextAtPos.setId("add-text-pos");
        addTextAtPos.addClickListener(e -> {
            editor.addText("PREFIX", 0);
            logEvent("addText(0): Inserted 'PREFIX' at position 0");
        });

        Button toggleDisabled = new Button("Toggle Disabled");
        toggleDisabled.setId("toggle-disabled");
        toggleDisabled.addClickListener(e -> {
            boolean current = editor.getElement().getProperty("disabled", false);
            editor.getElement().setProperty("disabled", !current);
        });

        Button toggleReadonly = new Button("Toggle Readonly");
        toggleReadonly.setId("toggle-readonly");
        toggleReadonly.addClickListener(e -> {
            boolean current = editor.getElement().getProperty("readonly", false);
            editor.getElement().setProperty("readonly", !current);
        });

        Button toggleNoRulers = new Button("Toggle No Rulers");
        toggleNoRulers.setId("toggle-no-rulers");
        toggleNoRulers.addClickListener(e -> {
            boolean current = editor.getElement().getProperty("noRulers", false);
            editor.setNoRulers(!current);
        });

        Button focusEditor = new Button("Focus");
        focusEditor.setId("focus-editor");
        focusEditor.addClickListener(e ->
                editor.getElement().callJsFunction("focus"));

        Button getTextLength = new Button("Get Text Length");
        getTextLength.setId("get-text-length");
        getTextLength.addClickListener(e -> {
            editor.getTextLength(length ->
                logEvent("TextLength: " + length));
        });

        // Set German I18n (with ERTE-specific labels)
        Button setGermanI18n = new Button("Set German I18n");
        setGermanI18n.setId("set-german-i18n");
        setGermanI18n.addClickListener(e -> {
            EnhancedRichTextEditor.EnhancedRichTextEditorI18n i18n =
                    new EnhancedRichTextEditor.EnhancedRichTextEditorI18n();
            // Standard RTE 2 labels
            i18n.setBold("Fett");
            i18n.setItalic("Kursiv");
            i18n.setUnderline("Unterstreichen");
            i18n.setUndo("Rückgängig");
            i18n.setRedo("Wiederholen");
            // ERTE-specific labels
            i18n.setReadonly("Schreibschutz");
            i18n.setWhitespace("Leerzeichen anzeigen");
            i18n.setPlaceholder("Platzhalter");
            i18n.setPlaceholderAppearance("Platzhalter-Darstellung");
            i18n.setPlaceholderDialogTitle("Platzhalter");
            i18n.setPlaceholderComboBoxLabel("Platzhalter wählen");
            i18n.setPlaceholderAppearanceLabel1("Normal");
            i18n.setPlaceholderAppearanceLabel2("Wert");
            editor.setI18n(i18n);
        });

        // Load delta with tabs
        Button loadTabDelta = new Button("Load Tab Delta");
        loadTabDelta.setId("load-tab-delta");
        loadTabDelta.addClickListener(e -> editor.asDelta().setValue(
            "[{\"insert\":\"Hello\"},{\"insert\":{\"tab\":true}},{\"insert\":\"World\\n\"}]"
        ));

        // Load delta with readonly
        Button loadReadonlyDelta = new Button("Load Readonly Delta");
        loadReadonlyDelta.setId("load-readonly-delta");
        loadReadonlyDelta.addClickListener(e -> editor.asDelta().setValue(
            "[{\"insert\":\"Before \"},{\"insert\":\"Protected\",\"attributes\":{\"readonly\":true}},{\"insert\":\" After\\n\"}]"
        ));

        // Load delta with placeholders
        Button loadPlaceholderDelta = new Button("Load Placeholder Delta");
        loadPlaceholderDelta.setId("load-placeholder-delta");
        loadPlaceholderDelta.addClickListener(e -> editor.asDelta().setValue(
            "[{\"insert\":\"Hello \"},{\"insert\":{\"placeholder\":{\"text\":\"TestPlaceholder\"}}},{\"insert\":\"!\\n\"}]"
        ));

        // Get HTML value
        Button getHtml = new Button("Get HTML");
        getHtml.setId("get-html");
        getHtml.addClickListener(e -> {
            Div htmlOutput = new Div();
            htmlOutput.setId("html-output");
            htmlOutput.setText(editor.getValue());
            // Replace if exists
            getChildren()
                .filter(c -> "html-output".equals(c.getId().orElse(null)))
                .findFirst()
                .ifPresent(this::remove);
            add(htmlOutput);
        });

        // Delta output
        Pre deltaOutput = new Pre();
        deltaOutput.setId("delta-output");
        deltaOutput.getStyle()
                .set("max-height", "150px")
                .set("overflow", "auto")
                .set("font-size", "var(--lumo-font-size-xs)")
                .set("background", "var(--lumo-shade-5pct)")
                .set("padding", "var(--lumo-space-s)")
                .set("white-space", "pre-wrap")
                .set("word-break", "break-all");

        // Client-side text-change listener for delta/html output
        editor.getElement().executeJs(
            "const el = $0;" +
            "const deltaOut = document.getElementById('delta-output');" +
            "const htmlOut = document.getElementById('html-output');" +
            "const waitForEditor = () => {" +
            "  if (el._editor) {" +
            "    el._editor.on('text-change', () => {" +
            "      deltaOut.textContent = JSON.stringify(el._editor.getContents());" +
            "      if (htmlOut) htmlOut.textContent = el._editor.root.innerHTML;" +
            "    });" +
            "    deltaOut.textContent = JSON.stringify(el._editor.getContents());" +
            "  } else { setTimeout(waitForEditor, 50); }" +
            "};" +
            "waitForEditor();",
            editor.getElement()
        );

        // Ready indicator
        Div readyIndicator = new Div();
        readyIndicator.setId("test-ready");
        readyIndicator.getElement().setAttribute("data-ready", "true");
        readyIndicator.getStyle().set("display", "none");

        HorizontalLayout controls1 = new HorizontalLayout(
                addTextAtCursor, addTextAtPos, getTextLength, focusEditor);
        HorizontalLayout controls2 = new HorizontalLayout(
                toggleDisabled, toggleReadonly, toggleNoRulers);
        HorizontalLayout controls3 = new HorizontalLayout(
                loadTabDelta, loadReadonlyDelta, loadPlaceholderDelta, getHtml);
        // Set HTML from TextArea (for sanitizer round-trip testing)
        TextArea htmlInput = new TextArea("HTML Input");
        htmlInput.setId("html-input");
        htmlInput.setWidthFull();
        htmlInput.setMaxHeight("100px");

        Button setHtml = new Button("Set HTML");
        setHtml.setId("set-html");
        setHtml.addClickListener(e ->
                editor.setValue(htmlInput.getValue()));

        HorizontalLayout controls4 = new HorizontalLayout(setGermanI18n, setHtml);

        add(controls1, controls2, controls3, controls4,
                htmlInput, editor, deltaOutput, eventLog, readyIndicator);
    }

    private void logEvent(String message) {
        String timestamp = java.time.LocalTime.now().toString().substring(0, 12);
        eventLog.getElement().executeJs(
            "$0.textContent += $1 + '\\n'",
            eventLog.getElement(),
            "[" + timestamp + "] " + message
        );
    }
}
