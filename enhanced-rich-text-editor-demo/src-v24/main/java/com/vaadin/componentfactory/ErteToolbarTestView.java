package com.vaadin.componentfactory;

import com.vaadin.componentfactory.EnhancedRichTextEditor.ToolbarButton;
import com.vaadin.componentfactory.toolbar.ToolbarSlot;
import com.vaadin.componentfactory.toolbar.ToolbarSwitch;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.Route;

import java.util.HashMap;
import java.util.Map;

@Route("erte-test/toolbar")
public class ErteToolbarTestView extends VerticalLayout {

    private final Div eventLog;

    public ErteToolbarTestView() {
        setSizeFull();

        EnhancedRichTextEditor editor = new EnhancedRichTextEditor();
        editor.setId("test-editor");
        editor.setValueChangeMode(ValueChangeMode.EAGER);
        editor.setMaxHeight("400px");

        eventLog = new Div();
        eventLog.setId("event-log");
        eventLog.getStyle()
                .set("max-height", "150px")
                .set("overflow", "auto")
                .set("font-size", "var(--lumo-font-size-xs)")
                .set("background", "var(--lumo-shade-5pct)")
                .set("padding", "var(--lumo-space-s)")
                .set("white-space", "pre-wrap");

        // Add test components to slots
        Button startButton = new Button("START");
        startButton.setId("slot-start-btn");
        startButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        startButton.addClickListener(e -> logEvent("StartButtonClicked"));
        editor.addToolbarComponents(ToolbarSlot.START, startButton);

        Button endButton = new Button("END");
        endButton.setId("slot-end-btn");
        endButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        editor.addToolbarComponents(ToolbarSlot.END, endButton);

        Button beforeHistoryBtn = new Button("BH");
        beforeHistoryBtn.setId("slot-before-history-btn");
        beforeHistoryBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        editor.addToolbarComponents(ToolbarSlot.BEFORE_GROUP_HISTORY, beforeHistoryBtn);

        Button afterEmphasisBtn = new Button("AE");
        afterEmphasisBtn.setId("slot-after-emphasis-btn");
        afterEmphasisBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        editor.addToolbarComponents(ToolbarSlot.AFTER_GROUP_EMPHASIS, afterEmphasisBtn);

        // Custom group component
        Button customGroupBtn = new Button(VaadinIcon.STAR.create());
        customGroupBtn.setId("custom-group-btn");
        customGroupBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        customGroupBtn.addClickListener(e -> logEvent("CustomGroupBtnClicked"));
        editor.addCustomToolbarComponents(customGroupBtn);

        // ToolbarSwitch
        ToolbarSwitch toolbarSwitch = new ToolbarSwitch(VaadinIcon.EYE.create());
        toolbarSwitch.setId("toolbar-switch");
        toolbarSwitch.addActiveChangedListener(e -> logEvent("ToolbarSwitchChanged: active=" + e.isActive()));
        editor.addToolbarComponents(ToolbarSlot.GROUP_CUSTOM, toolbarSwitch);

        // Control buttons
        Button hideButtons = new Button("Hide CLEAN+BLOCKQUOTE");
        hideButtons.setId("hide-buttons");
        hideButtons.addClickListener(e -> {
            Map<ToolbarButton, Boolean> visibility = new HashMap<>();
            visibility.put(ToolbarButton.CLEAN, false);
            visibility.put(ToolbarButton.BLOCKQUOTE, false);
            editor.setToolbarButtonsVisibility(visibility);
        });

        Button showButtons = new Button("Show All");
        showButtons.setId("show-buttons");
        showButtons.addClickListener(e -> {
            Map<ToolbarButton, Boolean> visibility = new HashMap<>();
            visibility.put(ToolbarButton.CLEAN, true);
            visibility.put(ToolbarButton.BLOCKQUOTE, true);
            editor.setToolbarButtonsVisibility(visibility);
        });

        Button hideErteButtons = new Button("Hide WHITESPACE+READONLY");
        hideErteButtons.setId("hide-erte-buttons");
        hideErteButtons.addClickListener(e -> {
            Map<ToolbarButton, Boolean> visibility = new HashMap<>();
            visibility.put(ToolbarButton.WHITESPACE, false);
            visibility.put(ToolbarButton.READONLY, false);
            editor.setToolbarButtonsVisibility(visibility);
        });

        Button replaceIcon = new Button("Replace Undo Icon");
        replaceIcon.setId("replace-icon");
        replaceIcon.addClickListener(e -> {
            Icon newIcon = new Icon(VaadinIcon.ARROW_BACKWARD);
            newIcon.setColor("red");
            newIcon.setSize("1.25em");
            editor.replaceStandardToolbarButtonIcon(ToolbarButton.UNDO, newIcon);
        });

        Button removeStartBtn = new Button("Remove START btn");
        removeStartBtn.setId("remove-start-btn");
        removeStartBtn.addClickListener(e ->
            editor.removeToolbarComponent(ToolbarSlot.START, startButton));

        // Add keyboard shortcut (Shift+F9 for align center)
        editor.addStandardToolbarButtonShortcut(ToolbarButton.ALIGN_CENTER, 120, false, true, false);

        // Change i18n to German for a few labels
        Button setGermanI18n = new Button("Set German I18n");
        setGermanI18n.setId("set-german-i18n");
        setGermanI18n.addClickListener(e -> {
            EnhancedRichTextEditor.RichTextEditorI18n i18n = new EnhancedRichTextEditor.RichTextEditorI18n();
            i18n.setBold("Fett");
            i18n.setItalic("Kursiv");
            i18n.setUndo("Rückgängig");
            i18n.setRedo("Wiederholen");
            editor.setI18n(i18n);
        });

        // Delta output
        Pre deltaOutput = new Pre();
        deltaOutput.setId("delta-output");
        deltaOutput.getStyle()
                .set("max-height", "150px")
                .set("overflow", "auto")
                .set("font-size", "var(--lumo-font-size-xs)")
                .set("background", "var(--lumo-shade-5pct)")
                .set("padding", "var(--lumo-space-s)")
                .set("white-space", "pre-wrap")
                .set("word-break", "break-all");

        // HTML output
        Div htmlOutput = new Div();
        htmlOutput.setId("html-output");
        htmlOutput.getStyle().set("display", "none");

        // Client-side listener
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

        HorizontalLayout controls1 = new HorizontalLayout(hideButtons, showButtons, hideErteButtons);
        HorizontalLayout controls2 = new HorizontalLayout(replaceIcon, removeStartBtn, setGermanI18n);
        add(controls1, controls2, editor, deltaOutput, eventLog, htmlOutput, readyIndicator);
    }

    private void logEvent(String message) {
        String timestamp = java.time.LocalTime.now().toString().substring(0, 12);
        eventLog.getElement().executeJs(
            "$0.textContent += $1 + '\\n'",
            eventLog.getElement(),
            "[" + timestamp + "] " + message
        );
    }
}
