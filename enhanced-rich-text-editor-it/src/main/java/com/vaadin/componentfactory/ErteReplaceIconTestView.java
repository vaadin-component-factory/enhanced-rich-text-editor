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

import com.vaadin.componentfactory.EnhancedRichTextEditor.ToolbarButton;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.richtexteditor.RichTextEditor;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.Route;

@Route(value = "erte-test/replace-icons", layout = ErteTestLayout.class)
public class ErteReplaceIconTestView extends VerticalLayout {

    private final Div eventLog;
    private final EnhancedRichTextEditor editor;

    public ErteReplaceIconTestView() {
        setSizeFull();

        editor = new EnhancedRichTextEditor();
        editor.setId("test-editor");
        editor.setValueChangeMode(ValueChangeMode.EAGER);
        editor.setMaxHeight("400px");

        // Pre-configure 5 custom icons for automated testing
        editor.replaceStandardToolbarButtonIcon(ToolbarButton.BOLD,
                new Icon(VaadinIcon.STAR));
        editor.replaceStandardToolbarButtonIcon(ToolbarButton.ITALIC,
                new Icon(VaadinIcon.FLAG));
        editor.replaceStandardToolbarButtonIcon(ToolbarButton.UNDO,
                new Icon(VaadinIcon.ARROW_LEFT));
        editor.replaceStandardToolbarButtonIcon(ToolbarButton.REDO,
                new Icon(VaadinIcon.ARROW_RIGHT));
        editor.replaceStandardToolbarButtonIcon(ToolbarButton.ALIGN_LEFT,
                new Icon(VaadinIcon.AIRPLANE));

        eventLog = new Div();
        eventLog.setId("event-log");
        eventLog.getStyle()
                .set("max-height", "100px")
                .set("overflow", "auto")
                .set("font-size", "var(--lumo-font-size-xs)")
                .set("background", "var(--lumo-shade-5pct)")
                .set("padding", "var(--lumo-space-s)")
                .set("white-space", "pre-wrap");

        // Control buttons
        Button clearBoldIcon = new Button("Clear Bold Icon");
        clearBoldIcon.setId("clear-bold");
        clearBoldIcon.addClickListener(e -> {
            editor.replaceStandardToolbarButtonIcon(ToolbarButton.BOLD, null);
            logEvent("Cleared Bold icon (restored default)");
        });

        Button replaceAlignCenter = new Button("Replace Align Center");
        replaceAlignCenter.setId("replace-align-center");
        replaceAlignCenter.addClickListener(e -> {
            editor.replaceStandardToolbarButtonIcon(ToolbarButton.ALIGN_CENTER,
                    new Icon(VaadinIcon.CLOUD));
            logEvent("Replaced Align Center icon with Cloud");
        });

        Button setGermanI18n = new Button("Set German I18n");
        setGermanI18n.setId("set-german");
        setGermanI18n.addClickListener(e -> {
            RichTextEditor.RichTextEditorI18n i18n = new RichTextEditor.RichTextEditorI18n();
            i18n.setUndo("Rückgängig");
            i18n.setRedo("Wiederholen");
            i18n.setBold("Fett");
            editor.setI18n(i18n);
            logEvent("Set German i18n");
        });

        HorizontalLayout controls = new HorizontalLayout(clearBoldIcon,
                replaceAlignCenter, setGermanI18n);
        controls.setSpacing(true);

        // Output: Delta
        TextArea deltaOutput = new TextArea("Delta JSON");
        deltaOutput.setId("delta-output");
        deltaOutput.setWidthFull();
        deltaOutput.setHeight("150px");
        deltaOutput.setReadOnly(true);
        editor.asDelta().addValueChangeListener(e -> {
            if (e.getValue() != null) {
                deltaOutput.setValue(e.getValue());
            }
        });

        // Ready indicator for Playwright
        Div ready = new Div();
        ready.setId("test-ready");
        ready.getStyle().set("display", "none");
        ready.getElement().setAttribute("data-ready", "true");

        add(editor, controls, eventLog, deltaOutput, ready);
    }

    private void logEvent(String message) {
        Div entry = new Div();
        entry.setText(message);
        eventLog.add(entry);
    }
}
