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

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

/**
 * Test view for readonly sections (Phase 3.1b).
 * <p>
 * Provides:
 * <ul>
 *   <li>Editor with initial content containing two readonly sections</li>
 *   <li>Toggle whole-editor readonly button</li>
 *   <li>Load new delta with readonly section button</li>
 *   <li>Delta output, ready indicator</li>
 * </ul>
 */
@Route("erte-test/readonly")
public class ErteReadonlyTestView extends VerticalLayout {

    // Initial delta JSON:
    //   "Editable text before. " + readonly("This is readonly content.")
    //   + " Editable text after.\n"
    //   + "Another readonly section with " + readonly+bold("bold formatting") + ".\n"
    private static final String INITIAL_DELTA = "["
            + "{\"insert\":\"Editable text before. \"},"
            + "{\"insert\":\"This is readonly content.\","
            +   "\"attributes\":{\"readonly\":true}},"
            + "{\"insert\":\" Editable text after.\\n"
            + "Another readonly section with \"},"
            + "{\"insert\":\"bold formatting\","
            +   "\"attributes\":{\"readonly\":true,\"bold\":true}},"
            + "{\"insert\":\".\\n\"}"
            + "]";

    // Delta for load button: "Start " + readonly("Protected Section") + " End\n"
    private static final String LOAD_DELTA = "["
            + "{\"insert\":\"Start \"},"
            + "{\"insert\":\"Protected Section\","
            +   "\"attributes\":{\"readonly\":true}},"
            + "{\"insert\":\" End\\n\"}"
            + "]";

    public ErteReadonlyTestView() {
        setSizeFull();
        setPadding(true);
        setSpacing(true);

        var editor = new EnhancedRichTextEditor();
        editor.setId("test-editor");
        editor.setWidthFull();
        editor.setHeight("300px");

        // Set initial content via Delta
        editor.asDelta().setValue(INITIAL_DELTA);

        // --- Delta output (client-side text-change) ---
        var deltaOutput = new Pre();
        deltaOutput.setId("delta-output");
        deltaOutput.getStyle()
                .set("white-space", "pre-wrap")
                .set("font-size", "var(--lumo-font-size-xs)")
                .set("max-height", "120px")
                .set("overflow", "auto");

        // Client-side: update delta output on every text-change + initial
        editor.getElement().executeJs(
            "const el = this;" +
            "const out = document.getElementById('delta-output');" +
            "if (el._editor) {" +
            "  out.textContent = JSON.stringify(el._editor.getContents());" +
            "  el._editor.on('text-change', function() {" +
            "    out.textContent = JSON.stringify(el._editor.getContents());" +
            "  });" +
            "}"
        );

        // --- Control buttons ---
        var toggleReadonly = new Button("Toggle Readonly", e -> {
            editor.setReadOnly(!editor.isReadOnly());
        });
        toggleReadonly.setId("toggle-readonly");

        var loadReadonly = new Button("Load Readonly Delta", e -> {
            editor.asDelta().setValue(LOAD_DELTA);
        });
        loadReadonly.setId("load-readonly");

        var controls = new HorizontalLayout(toggleReadonly, loadReadonly);
        controls.setSpacing(true);

        // --- Ready indicator ---
        var readyIndicator = new Span("ready");
        readyIndicator.setId("test-ready");
        readyIndicator.getStyle().set("display", "none");
        readyIndicator.getElement().setAttribute("data-ready", "true");

        // --- Labels ---
        var deltaLabel = new Div();
        deltaLabel.getElement().setProperty("innerHTML", "<b>Delta:</b>");

        add(editor, controls, deltaLabel, deltaOutput, readyIndicator);
        setFlexGrow(1, editor);
    }
}
