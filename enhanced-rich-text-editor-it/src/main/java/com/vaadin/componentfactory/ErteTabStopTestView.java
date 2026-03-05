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

import java.util.List;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

/**
 * Test view for tabstops, rulers, and soft-break (Phase 3.1c).
 * <p>
 * Provides:
 * <ul>
 *   <li>Editor with 3 tabstops (LEFT@150, RIGHT@350, MIDDLE@550)</li>
 *   <li>Delta output (client-side text-change listener)</li>
 *   <li>HTML output</li>
 *   <li>Ready indicator</li>
 * </ul>
 */
@Route("erte-test/tabstops")
public class ErteTabStopTestView extends VerticalLayout {

    public ErteTabStopTestView() {
        setSizeFull();
        setPadding(true);
        setSpacing(true);

        var editor = new EnhancedRichTextEditor();
        editor.setId("test-editor");
        editor.setWidthFull();
        editor.setMinHeight("300px");

        // Set 3 tabstops via Java API
        editor.setTabStops(List.of(
                new TabStop(TabStop.Direction.LEFT, 150),
                new TabStop(TabStop.Direction.RIGHT, 350),
                new TabStop(TabStop.Direction.MIDDLE, 550)));

        // --- Delta output (client-side text-change) ---
        var deltaOutput = new Pre();
        deltaOutput.setId("delta-output");
        deltaOutput.getStyle()
                .set("white-space", "pre-wrap")
                .set("font-size", "var(--lumo-font-size-xs)")
                .set("max-height", "120px")
                .set("overflow", "auto");

        // --- HTML output ---
        var htmlOutput = new Pre();
        htmlOutput.setId("html-output");
        htmlOutput.getStyle()
                .set("white-space", "pre-wrap")
                .set("font-size", "var(--lumo-font-size-xs)")
                .set("max-height", "120px")
                .set("overflow", "auto");

        // Client-side: update delta + HTML output on every text-change + initial
        editor.getElement().executeJs(
                "const el = this;"
                        + "const deltaOut = document.getElementById('delta-output');"
                        + "const htmlOut = document.getElementById('html-output');"
                        + "if (el._editor) {"
                        + "  deltaOut.textContent = JSON.stringify(el._editor.getContents());"
                        + "  htmlOut.textContent = el._editor.root.innerHTML;"
                        + "  el._editor.on('text-change', function() {"
                        + "    deltaOut.textContent = JSON.stringify(el._editor.getContents());"
                        + "    htmlOut.textContent = el._editor.root.innerHTML;"
                        + "  });"
                        + "}");

        // --- Ready indicator ---
        var readyIndicator = new Span("ready");
        readyIndicator.setId("test-ready");
        readyIndicator.getStyle().set("display", "none");
        readyIndicator.getElement().setAttribute("data-ready", "true");

        // --- Labels ---
        var deltaLabel = new Div();
        deltaLabel.getElement().setProperty("innerHTML", "<b>Delta:</b>");

        var htmlLabel = new Div();
        htmlLabel.getElement().setProperty("innerHTML", "<b>HTML:</b>");

        add(editor, deltaLabel, deltaOutput, htmlLabel, htmlOutput,
                readyIndicator);
        setFlexGrow(1, editor);
    }
}
