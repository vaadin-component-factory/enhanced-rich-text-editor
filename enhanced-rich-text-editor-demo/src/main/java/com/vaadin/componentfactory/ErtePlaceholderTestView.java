package com.vaadin.componentfactory;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.Route;

import java.util.ArrayList;
import java.util.List;

@Route("erte-test/placeholders")
public class ErtePlaceholderTestView extends VerticalLayout {

    private final Div eventLog;
    private boolean autoConfirmInserts = true;
    private boolean autoConfirmRemoves = true;

    public ErtePlaceholderTestView() {
        setSizeFull();

        EnhancedRichTextEditor editor = new EnhancedRichTextEditor();
        editor.setId("test-editor");
        editor.setValueChangeMode(ValueChangeMode.EAGER);
        editor.setMaxHeight("400px");

        // Configure 3 placeholders
        List<Placeholder> placeholders = new ArrayList<>();

        Placeholder p1 = new Placeholder();
        p1.setText("N-1=Company Name");
        p1.getFormat().put("italic", true);
        p1.getAltFormat().put("bold", true);
        placeholders.add(p1);

        Placeholder p2 = new Placeholder();
        p2.setText("A-1=Street Address");
        p2.getAltFormat().put("link", "https://example.com");
        placeholders.add(p2);

        Placeholder p3 = new Placeholder();
        p3.setText("D-1=2024-01-01");
        placeholders.add(p3);

        editor.setPlaceholders(placeholders);
        editor.setPlaceholderAltAppearancePattern("(?<=\\=).*$");

        // Auto-confirm toggles
        Checkbox autoInsertToggle = new Checkbox("Auto-confirm inserts", true);
        autoInsertToggle.setId("auto-confirm-inserts");
        autoInsertToggle.addValueChangeListener(e -> autoConfirmInserts = e.getValue());

        Checkbox autoRemoveToggle = new Checkbox("Auto-confirm removes", true);
        autoRemoveToggle.setId("auto-confirm-removes");
        autoRemoveToggle.addValueChangeListener(e -> autoConfirmRemoves = e.getValue());

        // Event log
        eventLog = new Div();
        eventLog.setId("event-log");
        eventLog.getStyle()
                .set("max-height", "150px")
                .set("overflow", "auto")
                .set("font-size", "var(--lumo-font-size-xs)")
                .set("background", "var(--lumo-shade-5pct)")
                .set("padding", "var(--lumo-space-s)")
                .set("white-space", "pre-wrap");

        // Register placeholder event listeners
        editor.addPlaceholderButtonClickedListener(event -> {
            logEvent("PlaceholderButtonClicked: position=" + event.getPosition());
        });

        editor.addPlaceholderBeforeInsertListener(event -> {
            StringBuilder texts = new StringBuilder();
            event.getPlaceholders().forEach(p -> texts.append(p.getText()).append(" "));
            logEvent("PlaceholderBeforeInsert: " + texts.toString().trim());
            if (autoConfirmInserts) {
                event.insert();
            }
        });

        editor.addPlaceholderInsertedListener(event -> {
            StringBuilder texts = new StringBuilder();
            event.getPlaceholders().forEach(p -> texts.append(p.getText()).append(" "));
            logEvent("PlaceholderInserted: " + texts.toString().trim());
        });

        editor.addPlaceholderBeforeRemoveListener(event -> {
            StringBuilder texts = new StringBuilder();
            event.getPlaceholders().forEach(p -> texts.append(p.getText()).append(" "));
            logEvent("PlaceholderBeforeRemove: " + texts.toString().trim());
            if (autoConfirmRemoves) {
                event.remove();
            }
        });

        editor.addPlaceholderRemovedListener(event -> {
            StringBuilder texts = new StringBuilder();
            event.getPlaceholders().forEach(p -> texts.append(p.getText()).append(" "));
            logEvent("PlaceholderRemoved: " + texts.toString().trim());
        });

        editor.addPlaceholderSelectedListener(event -> {
            StringBuilder texts = new StringBuilder();
            event.getPlaceholders().forEach(p -> texts.append(p.getText()).append(" "));
            logEvent("PlaceholderSelected: " + texts.toString().trim());
        });

        editor.addPlaceholderLeaveListener(event -> {
            logEvent("PlaceholderLeave");
        });

        editor.addPlaceholderAppearanceChangedListener(event -> {
            if (event.isFromClient()) {
                logEvent("PlaceholderAppearanceChanged: alt=" + event.getAltAppearance()
                        + " label=" + event.getAppearanceLabel());
            }
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

        // HTML output
        Div htmlOutput = new Div();
        htmlOutput.setId("html-output");
        htmlOutput.getStyle().set("display", "none");

        // Client-side listener for delta/html output
        editor.getElement().executeJs(
            "const el = $0;" +
            "const deltaOut = document.getElementById('delta-output');" +
            "const htmlOut = document.getElementById('html-output');" +
            "const waitForEditor = () => {" +
            "  if (el._editor) {" +
            "    el._editor.on('text-change', () => {" +
            "      deltaOut.textContent = JSON.stringify(el._editor.getContents());" +
            "      htmlOut.textContent = el._editor.root.innerHTML;" +
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

        HorizontalLayout controls = new HorizontalLayout(autoInsertToggle, autoRemoveToggle);
        add(controls, editor, deltaOutput, eventLog, htmlOutput, readyIndicator);
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
