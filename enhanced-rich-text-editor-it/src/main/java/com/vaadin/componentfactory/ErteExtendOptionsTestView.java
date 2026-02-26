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

import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.Route;

/**
 * Test view for Phase 3.1g: extendQuill / extendEditor hooks.
 * Loads the sample connector which registers a highlight format via
 * extendQuill and sets a verification flag via extendEditor.
 */
@Route("erte-test/extend-options")
@JsModule("./src/sampleEditorExtensionConnector.js")
public class ErteExtendOptionsTestView extends VerticalLayout {

    public ErteExtendOptionsTestView() {
        setSizeFull();
        setPadding(true);
        setSpacing(true);

        var editor = new EnhancedRichTextEditor();
        editor.setId("test-editor");
        editor.setValueChangeMode(ValueChangeMode.EAGER);
        editor.setMaxHeight("400px");

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

        // Event log
        var eventLog = new Div();
        eventLog.setId("event-log");
        eventLog.getStyle()
                .set("max-height", "150px")
                .set("overflow", "auto")
                .set("font-size", "var(--lumo-font-size-xs)")
                .set("background", "var(--lumo-shade-5pct)")
                .set("padding", "var(--lumo-space-s)")
                .set("white-space", "pre-wrap");

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

        add(editor, deltaOutput, eventLog, htmlOutput, readyIndicator);
    }
}
