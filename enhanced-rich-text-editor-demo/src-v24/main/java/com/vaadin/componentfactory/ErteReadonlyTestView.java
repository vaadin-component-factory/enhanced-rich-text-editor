package com.vaadin.componentfactory;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.Route;

@Route("erte-test/readonly")
public class ErteReadonlyTestView extends VerticalLayout {

    public ErteReadonlyTestView() {
        setSizeFull();

        EnhancedRichTextEditor editor = new EnhancedRichTextEditor();
        editor.setId("test-editor");
        editor.setValueChangeMode(ValueChangeMode.EAGER);
        editor.setMaxHeight("400px");

        // Pre-populate with readonly sections
        editor.setValue("[" +
            "{\"insert\":\"Editable text before. \"}," +
            "{\"insert\":\"This is readonly content.\",\"attributes\":{\"readonly\":true}}," +
            "{\"insert\":\" Editable text after.\\n\"}," +
            "{\"insert\":\"Another readonly section with \",\"attributes\":{\"readonly\":true}}," +
            "{\"insert\":\"bold formatting\",\"attributes\":{\"readonly\":true,\"bold\":true}}," +
            "{\"insert\":\".\\n\"}" +
        "]");

        // Toggle whole-editor readonly (via element property since setReadonly is protected)
        Button toggleReadonly = new Button("Toggle Readonly");
        toggleReadonly.setId("toggle-readonly");
        toggleReadonly.addClickListener(e -> {
            boolean current = editor.getElement().getProperty("readonly", false);
            editor.getElement().setProperty("readonly", !current);
        });

        // Toggle disabled (via element property since setDisabled is protected)
        Button toggleDisabled = new Button("Toggle Disabled");
        toggleDisabled.setId("toggle-disabled");
        toggleDisabled.addClickListener(e -> {
            boolean current = editor.getElement().getProperty("disabled", false);
            editor.getElement().setProperty("disabled", !current);
        });

        // Set value with readonly sections programmatically
        Button loadReadonly = new Button("Load Readonly Delta");
        loadReadonly.setId("load-readonly");
        loadReadonly.addClickListener(e -> editor.setValue("[" +
            "{\"insert\":\"Start \"}," +
            "{\"insert\":\"Protected Section\",\"attributes\":{\"readonly\":true}}," +
            "{\"insert\":\" End\\n\"}" +
        "]"));

        // Delta output
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
        htmlOutput.getStyle().set("display", "none");

        // Event log
        Div eventLog = new Div();
        eventLog.setId("event-log");
        eventLog.getStyle().set("display", "none");

        // Client-side listener for delta/html output
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

        HorizontalLayout controls = new HorizontalLayout(toggleReadonly, toggleDisabled, loadReadonly);
        add(controls, editor, deltaOutput, htmlOutput, eventLog, readyIndicator);
    }
}
