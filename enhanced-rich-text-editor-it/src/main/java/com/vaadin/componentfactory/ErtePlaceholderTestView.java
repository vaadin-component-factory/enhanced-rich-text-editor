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

import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.Route;

@Route("erte-test/placeholders")
public class ErtePlaceholderTestView extends VerticalLayout {

    private final Div eventLog;
    private boolean autoConfirmInserts = true;
    private boolean autoConfirmRemoves = true;

    public ErtePlaceholderTestView() {
        setSizeFull();
        setPadding(true);
        setSpacing(true);

        var editor = new EnhancedRichTextEditor();
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
        editor.setPlaceholderTags("@", "");
        editor.setPlaceholderAltAppearancePattern("(?<=\\=).*$");

        // Auto-confirm toggles
        var autoInsertToggle = new Checkbox("Auto-confirm inserts", true);
        autoInsertToggle.setId("auto-confirm-inserts");
        autoInsertToggle.addValueChangeListener(
                e -> autoConfirmInserts = e.getValue());

        var autoRemoveToggle = new Checkbox("Auto-confirm removes", true);
        autoRemoveToggle.setId("auto-confirm-removes");
        autoRemoveToggle.addValueChangeListener(
                e -> autoConfirmRemoves = e.getValue());

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

        // Register all 8 placeholder event listeners
        editor.addPlaceholderButtonClickedListener(event -> {
            logEvent("PlaceholderButtonClicked: position="
                    + event.getPosition());
            // Re-open the built-in JS dialog (preventDefault() in @DomEvent
            // cancels it)
            editor.getElement()
                    .executeJs("this._placeholderEditing = true");
        });

        editor.addPlaceholderBeforeInsertListener(event -> {
            StringBuilder texts = new StringBuilder();
            event.getPlaceholders()
                    .forEach(p -> texts.append(p.getText()).append(" "));
            logEvent("PlaceholderBeforeInsert: "
                    + texts.toString().trim());
            if (autoConfirmInserts) {
                event.insert();
            }
        });

        editor.addPlaceholderInsertedListener(event -> {
            StringBuilder texts = new StringBuilder();
            event.getPlaceholders()
                    .forEach(p -> texts.append(p.getText()).append(" "));
            logEvent("PlaceholderInserted: " + texts.toString().trim());
        });

        editor.addPlaceholderBeforeRemoveListener(event -> {
            StringBuilder texts = new StringBuilder();
            event.getPlaceholders()
                    .forEach(p -> texts.append(p.getText()).append(" "));
            logEvent("PlaceholderBeforeRemove: "
                    + texts.toString().trim());
            if (autoConfirmRemoves) {
                event.remove();
            }
        });

        editor.addPlaceholderRemovedListener(event -> {
            StringBuilder texts = new StringBuilder();
            event.getPlaceholders()
                    .forEach(p -> texts.append(p.getText()).append(" "));
            logEvent("PlaceholderRemoved: " + texts.toString().trim());
        });

        editor.addPlaceholderSelectedListener(event -> {
            StringBuilder texts = new StringBuilder();
            event.getPlaceholders()
                    .forEach(p -> texts.append(p.getText()).append(" "));
            logEvent("PlaceholderSelected: " + texts.toString().trim());
        });

        editor.addPlaceholderLeaveListener(event -> {
            logEvent("PlaceholderLeave");
        });

        editor.addPlaceholderAppearanceChangedListener(event -> {
            if (event.isFromClient()) {
                logEvent("PlaceholderAppearanceChanged: alt="
                        + event.getAltAppearance() + " label="
                        + event.getAppearanceLabel());
            }
        });

        // Delta output (client-side text-change)
        var deltaOutput = new Pre();
        deltaOutput.setId("delta-output");
        deltaOutput.getStyle()
                .set("max-height", "150px")
                .set("overflow", "auto")
                .set("font-size", "var(--lumo-font-size-xs)")
                .set("background", "var(--lumo-shade-5pct)")
                .set("padding", "var(--lumo-space-s)")
                .set("white-space", "pre-wrap")
                .set("word-break", "break-all");

        // HTML output (hidden)
        var htmlOutput = new Div();
        htmlOutput.setId("html-output");
        htmlOutput.getStyle().set("display", "none");

        // Client-side listener for delta/html output
        editor.getElement().executeJs(
                "const el = this;"
                + "const deltaOut = document.getElementById('delta-output');"
                + "const htmlOut = document.getElementById('html-output');"
                + "const waitForEditor = () => {"
                + "  if (el._editor) {"
                + "    el._editor.on('text-change', () => {"
                + "      deltaOut.textContent = JSON.stringify(el._editor.getContents());"
                + "      htmlOut.textContent = el._editor.root.innerHTML;"
                + "    });"
                + "    deltaOut.textContent = JSON.stringify(el._editor.getContents());"
                + "  } else { setTimeout(waitForEditor, 50); }"
                + "};"
                + "waitForEditor();");

        // Ready indicator
        var readyIndicator = new Span("ready");
        readyIndicator.setId("test-ready");
        readyIndicator.getStyle().set("display", "none");
        readyIndicator.getElement().setAttribute("data-ready", "true");

        var controls = new HorizontalLayout(autoInsertToggle,
                autoRemoveToggle);
        controls.setSpacing(true);

        add(controls, editor, deltaOutput, eventLog, htmlOutput,
                readyIndicator);
    }

    private void logEvent(String message) {
        String timestamp = java.time.LocalTime.now().toString()
                .substring(0, 12);
        eventLog.getElement().executeJs(
                "$0.textContent += $1 + '\\n'",
                eventLog.getElement(),
                "[" + timestamp + "] " + message);
    }
}
