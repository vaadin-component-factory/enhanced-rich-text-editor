package com.vaadin.componentfactory;

import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.richtexteditor.RichTextEditor;
import com.vaadin.flow.data.value.ValueChangeMode;

public abstract class PlaygroundView<T extends RichTextEditor> extends VerticalLayout {

    public PlaygroundView() {
        setSizeFull();
        setAlignItems(Alignment.STRETCH);

        // --- Editor ---
        var editor = createEditor();

        editor.setWidthFull();
        editor.setValueChangeMode(ValueChangeMode.TIMEOUT);

        // --- Pre-loaded content ---
        String initialContent = getInitialContent();


        // workaround for https://github.com/vaadin/flow-components/issues/8854
        editor.getElement().executeJs("").then(_unused -> editor.asDelta().setValue(initialContent));

        // --- Output panel ---
        var deltaOutput = createOutput();
        deltaOutput.setText(initialContent);
        editor.asDelta().addValueChangeListener(e -> deltaOutput.setText(e.getValue()));

        var htmlOutput = createOutput();
        editor.addValueChangeListener(e -> htmlOutput.setText(e.getValue()));

        // --- Toolbar legend ---
        var legend = new Span(
                "Toolbar helpers: WS = ToolbarSwitch, " +
                        VaadinIcon.PAINTBRUSH.name() + " = ToolbarPopover, " +
                        VaadinIcon.PLUS.name() + " = ToolbarSelectPopup, " +
                        VaadinIcon.COG.name() + " = ToolbarDialog");
        legend.getStyle()
                .set("font-size", "var(--lumo-font-size-xs)")
                .set("color", "var(--lumo-secondary-text-color)");

        // --- Layout: editor left (2), delta right (1) ---
        var editorPanel = new VerticalLayout(editor, legend);
        editorPanel.setPadding(false);
        editorPanel.getStyle()
                .set("flex", "2")
                .set("min-width", "0")
                .set("gap", "var(--lumo-space-xs)");
        editorPanel.setFlexGrow(1, editor);

        var htmlDetails = new Details("HTML-Output", htmlOutput);
        var deltaDetails = new Details("Delta-Output", deltaOutput);

        add(editorPanel, htmlDetails, deltaDetails);

        setFlexGrow(1, editorPanel);
    }

    protected abstract T createEditor();

    protected abstract String getInitialContent();

    protected Pre createOutput() {
        var output  = new Pre();
        output.getStyle()
                .set("white-space", "pre-wrap")
                .set("word-break", "break-all")
                .set("font-size", "var(--lumo-font-size-xs)")
                .set("overflow", "auto")
                .set("background", "var(--lumo-shade-5pct)")
                .set("padding", "var(--lumo-space-s)")
                .set("margin", "0")
                .set("border-radius", "var(--lumo-border-radius-m)");

        return output;
    }
}