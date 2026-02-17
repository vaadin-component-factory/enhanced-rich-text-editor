package com.vaadin.componentfactory.demo25;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.richtexteditor.EnhancedRichTextEditor;
import com.vaadin.flow.router.Route;

/**
 * V25 Demo view for the Enhanced Rich Text Editor.
 * <p>
 * Shows the basic ERTE on V25 with feature verification controls.
 * Additional feature demos will be added as features are migrated
 * from ERTE 1 to ERTE 2.
 */
@Route("")
public class EnhancedRichTextEditorDemo25View extends VerticalLayout {

    private final Pre resultArea;

    public EnhancedRichTextEditorDemo25View() {
        setSizeFull();
        setPadding(true);

        add(new H3("ERTE v25 Demo"));

        // --- Editor ---
        EnhancedRichTextEditor editor = new EnhancedRichTextEditor();
        editor.setWidthFull();
        editor.setMaxHeight("400px");

        // --- Result area ---
        resultArea = new Pre();
        resultArea.getStyle()
                .set("padding", "8px")
                .set("background", "#f8f8f8")
                .set("border", "1px solid #ddd")
                .set("border-radius", "4px")
                .set("font-size", "12px")
                .set("max-height", "200px")
                .set("overflow", "auto")
                .set("white-space", "pre-wrap");
        resultArea.setText("Results will appear here...");

        // --- Verification buttons ---
        add(new H4("Basic Verification"));

        Button checkTagButton = new Button("Check Tag Name", e ->
            editor.getElement().executeJs("return $0.tagName")
                .then(String.class, tag -> log("Tag: " + tag))
        );

        Button toggleReadOnly = new Button("Toggle ReadOnly", e -> {
            boolean current = editor.isReadOnly();
            editor.setReadOnly(!current);
            log("ReadOnly: " + !current);
        });

        Button setValueButton = new Button("Set HTML Value", e -> {
            editor.asHtml().setValue("<p>Content set from <strong>Java</strong> at " +
                System.currentTimeMillis() + "</p>");
            log("Value set from Java");
        });

        Button getValueButton = new Button("Get HTML Value", e ->
            log("Value: " + editor.getValue())
        );

        Div buttons = new Div(checkTagButton, toggleReadOnly, setValueButton, getValueButton);
        buttons.getStyle().set("display", "flex").set("gap", "8px").set("flex-wrap", "wrap");

        add(buttons, editor, resultArea);
    }

    private void log(String message) {
        String current = resultArea.getText();
        resultArea.setText(message + "\n" + current);
    }
}
