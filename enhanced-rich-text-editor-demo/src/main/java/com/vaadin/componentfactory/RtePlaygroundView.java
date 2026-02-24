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

import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.richtexteditor.RichTextEditor;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/**
 * Playground for the stock Vaadin Rich Text Editor (for comparison).
 * <p>
 * Shows the unmodified RTE with a delta output panel on the right.
 */
@Route("rte-playground")
@PageTitle("RTE Playground")
public class RtePlaygroundView extends HorizontalLayout {

    public RtePlaygroundView() {
        setSizeFull();
        setPadding(false);
        setSpacing(false);
        getStyle().set("gap", "var(--lumo-space-m)");

        // --- Stock RTE ---
        var rte = new RichTextEditor();
        rte.setWidthFull();
        rte.setValueChangeMode(ValueChangeMode.TIMEOUT);
        var initialDelta = "[" +
                "{\"insert\":\"Stock \"}," +
                "{\"insert\":\"RTE\",\"attributes\":{\"bold\":true}}," +
                "{\"insert\":\" from Vaadin 25\\n" +
                "This is the unmodified Vaadin Rich Text Editor for comparison.\\n\"}" +
                "]";
        rte.asDelta().setValue(initialDelta);

        // --- Delta output panel ---
        var deltaOutput = new Pre();
        deltaOutput.getStyle()
                .set("white-space", "pre-wrap")
                .set("word-break", "break-all")
                .set("font-size", "var(--lumo-font-size-xs)")
                .set("overflow", "auto")
                .set("background", "var(--lumo-shade-5pct)")
                .set("padding", "var(--lumo-space-s)")
                .set("margin", "0")
                .set("border-radius", "var(--lumo-border-radius-m)");

        deltaOutput.setText(initialDelta);
        rte.asDelta().addValueChangeListener(e ->
                deltaOutput.setText(e.getValue()));

        // --- Layout: editor left (2), delta right (1) ---
        var editorPanel = new VerticalLayout(rte);
        editorPanel.setPadding(true);
        editorPanel.setSpacing(false);
        editorPanel.getStyle()
                .set("flex", "2")
                .set("min-width", "0");
        editorPanel.setFlexGrow(1, rte);

        var deltaPanel = new VerticalLayout(deltaOutput);
        deltaPanel.setPadding(true);
        deltaPanel.setSpacing(false);
        deltaPanel.getStyle()
                .set("flex", "1")
                .set("min-width", "0")
                .set("overflow", "hidden");
        deltaPanel.setFlexGrow(1, deltaOutput);

        add(editorPanel, deltaPanel);
    }
}
