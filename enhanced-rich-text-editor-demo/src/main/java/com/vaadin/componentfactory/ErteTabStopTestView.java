package com.vaadin.componentfactory;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.Route;

import java.util.ArrayList;
import java.util.List;

@Route("erte-test/tabstops")
public class ErteTabStopTestView extends VerticalLayout {

    public ErteTabStopTestView() {
        setSizeFull();

        EnhancedRichTextEditor editor = new EnhancedRichTextEditor();
        editor.setId("test-editor");
        editor.setValueChangeMode(ValueChangeMode.EAGER);
        editor.setMaxHeight("400px");

        // Default tabstops: LEFT@150, RIGHT@350, MIDDLE@550
        List<TabStop> tabStops = new ArrayList<>();
        tabStops.add(new TabStop(TabStop.Direction.LEFT, 150));
        tabStops.add(new TabStop(TabStop.Direction.RIGHT, 350));
        tabStops.add(new TabStop(TabStop.Direction.MIDDLE, 550));
        editor.setTabStops(tabStops);

        // Delta output — updated client-side on every text-change
        Pre deltaOutput = new Pre();
        deltaOutput.setId("delta-output");
        deltaOutput.getStyle()
                .set("max-height", "200px")
                .set("overflow", "auto")
                .set("font-size", "var(--lumo-font-size-xs)")
                .set("background", "var(--lumo-shade-5pct)")
                .set("padding", "var(--lumo-space-s)")
                .set("white-space", "pre-wrap")
                .set("word-break", "break-all");

        // HTML output
        Div htmlOutput = new Div();
        htmlOutput.setId("html-output");
        htmlOutput.getStyle()
                .set("max-height", "100px")
                .set("overflow", "auto")
                .set("font-size", "var(--lumo-font-size-xs)")
                .set("display", "none");

        // Event log
        Div eventLog = new Div();
        eventLog.setId("event-log");
        eventLog.getStyle().set("display", "none");

        // Whitespace toggle
        Checkbox showWhitespace = new Checkbox("Show Whitespace");
        showWhitespace.setId("whitespace-toggle");
        showWhitespace.addValueChangeListener(e -> editor.setShowWhitespace(e.getValue()));

        // Client-side listener to update delta output on every change
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

        add(showWhitespace, editor, deltaOutput, htmlOutput, eventLog, readyIndicator);
    }
}
