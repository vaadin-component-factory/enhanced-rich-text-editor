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
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.Route;

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

        // Enable tables addon
        EnhancedRichTextEditorTables tables = EnhancedRichTextEditorTables.enable(editor);

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

        // Event log
        Div eventLog = new Div();
        eventLog.setId("event-log");
        eventLog.getStyle()
                .set("font-size", "var(--lumo-font-size-xs)")
                .set("max-height", "100px")
                .set("overflow", "auto");

        add(editor, htmlOutput, eventLog);

        // Hidden ready indicator
        Div ready = new Div();
        ready.setId("test-ready");
        ready.getStyle().set("display", "none");
        add(ready);
    }
}
