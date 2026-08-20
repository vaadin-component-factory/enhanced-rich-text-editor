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
import java.util.stream.Collectors;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

/**
 * Test view for server-side tabstop synchronization.
 * <p>
 * Same editor setup as {@link ErteTabStopTestView}, plus a button that reads
 * {@link EnhancedRichTextEditor#getTabStops()} on the server and renders the
 * result. This proves that tabstop changes made by the user in the ruler
 * actually reach the server side (see the {@code tab-stops-changed} event and
 * the {@code @Synchronize} binding of the {@code tabStops} property).
 * <p>
 * Kept separate from {@link ErteTabStopTestView} so the additional controls do
 * not change that view's layout (visual regression baselines).
 */
@Route("erte-test/tabstops-sync")
public class ErteTabStopSyncTestView extends VerticalLayout {

    public ErteTabStopSyncTestView() {
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

        // --- Server-side tabstop readout ---
        var serverTabStopsOutput = new Pre();
        serverTabStopsOutput.setId("server-tabstops-output");
        serverTabStopsOutput.getStyle().set("white-space", "pre-wrap")
                .set("font-size", "var(--lumo-font-size-xs)");

        var readServerTabStops = new Button("Read server tabstops",
                e -> serverTabStopsOutput.setText(editor.getTabStops().stream()
                        .map(ts -> ts.getDirection() + "@" + ts.getPosition())
                        .collect(Collectors.joining(", "))));
        readServerTabStops.setId("read-server-tabstops");

        // --- Ready indicator ---
        var readyIndicator = new Span("ready");
        readyIndicator.setId("test-ready");
        readyIndicator.getStyle().set("display", "none");
        readyIndicator.getElement().setAttribute("data-ready", "true");

        // --- Label ---
        var serverTabStopsLabel = new Div();
        serverTabStopsLabel.getElement().setProperty("innerHTML",
                "<b>Server tabstops:</b>");

        add(editor, serverTabStopsLabel, readServerTabStops,
                serverTabStopsOutput, readyIndicator);
        setFlexGrow(1, editor);
    }
}
