package com.vaadin.componentfactory.demo;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.richtexteditor.EnhancedRichTextEditor;
import com.vaadin.flow.component.richtexteditor.RichTextEditor;
import com.vaadin.flow.router.Route;

@Route("")
public class SpikeView extends VerticalLayout {

    private boolean readOnly = false;
    private boolean disabled = false;
    private final Pre resultArea;

    public SpikeView() {
        setSizeFull();
        setPadding(true);

        add(new H3("ERTE v25 Migration Spike - Phase 3 + Table Spike"));

        EnhancedRichTextEditor editor = new EnhancedRichTextEditor();
        editor.setWidthFull();
        editor.setMaxHeight("400px");

        // Result area for displaying test output
        resultArea = new Pre();
        resultArea.getStyle().set("padding", "8px");
        resultArea.getStyle().set("background", "#f8f8f8");
        resultArea.getStyle().set("border", "1px solid #ddd");
        resultArea.getStyle().set("border-radius", "4px");
        resultArea.getStyle().set("font-size", "12px");
        resultArea.getStyle().set("max-height", "200px");
        resultArea.getStyle().set("overflow", "auto");
        resultArea.getStyle().set("white-space", "pre-wrap");
        resultArea.setText("Test results will appear here...");

        // === Phase 1 buttons (compact) ===
        add(new H4("Phase 1: Core Tests"));

        Button checkTagButton = new Button("Check Tag Name", e -> {
            editor.getElement().executeJs("return $0.tagName")
                .then(String.class, tag -> {
                    log("Tag: " + tag);
                });
        });

        Button toggleReadOnly = new Button("Toggle ReadOnly", e -> {
            readOnly = !readOnly;
            editor.setReadOnly(readOnly);
            log("ReadOnly: " + readOnly);
        });

        Button setValueButton = new Button("Set Value from Java", e -> {
            editor.asHtml().setValue("<p>Content set from <strong>Java</strong> at " +
                System.currentTimeMillis() + "</p>");
            log("Value set from Java");
        });

        FlexLayout phase1Controls = new FlexLayout(
            checkTagButton, toggleReadOnly, setValueButton
        );
        phase1Controls.setWidthFull();
        phase1Controls.setFlexWrap(FlexLayout.FlexWrap.WRAP);
        phase1Controls.getStyle().set("gap", "8px");
        add(phase1Controls);

        // === Phase 3: Item 11 - ThemableMixin Inheritance ===
        add(new H4("Item 11: ThemableMixin Inheritance"));

        Button checkThemeButton = new Button("Analyze Theme Styles", e -> {
            editor.getElement().executeJs("return $0.analyzeThemeInheritance()")
                .then(String.class, result -> {
                    log("Item 11 Theme:\n" + result);
                });
        });

        FlexLayout item11Controls = new FlexLayout(checkThemeButton);
        item11Controls.setWidthFull();
        item11Controls.setFlexWrap(FlexLayout.FlexWrap.WRAP);
        item11Controls.getStyle().set("gap", "8px");
        add(item11Controls);

        // === Phase 3: Item 12 - ::part() Selectors ===
        add(new H4("Item 12: ::part() Selectors"));

        Button testPartSelectorsButton = new Button("Test ::part() Selectors", e -> {
            editor.getElement().executeJs("return $0.testPartSelectors()")
                .then(String.class, result -> {
                    log("Item 12 Parts:\n" + result);
                });
        });

        Button applyPartStyleButton = new Button("Apply Custom Part Style", e -> {
            // Apply CSS targeting our custom element's parts
            editor.getElement().executeJs(
                "const style = document.createElement('style');" +
                "style.textContent = `" +
                "  vcf-enhanced-rich-text-editor::part(toolbar) {" +
                "    background-color: #e8f5e9 !important;" +
                "    border-bottom: 2px solid #4caf50 !important;" +
                "  }" +
                "  vcf-enhanced-rich-text-editor::part(content) {" +
                "    background-color: #fff3e0 !important;" +
                "  }" +
                "`;" +
                "document.head.appendChild(style);" +
                "return 'Styles applied: toolbar=green bg, content=orange bg';"
            ).then(String.class, result -> {
                log("Item 12: " + result);
            });
        });

        Button removePartStyleButton = new Button("Remove Custom Part Style", e -> {
            editor.getElement().executeJs(
                "const styles = document.head.querySelectorAll('style');" +
                "let removed = 0;" +
                "styles.forEach(s => {" +
                "  if (s.textContent.includes('vcf-enhanced-rich-text-editor::part')) {" +
                "    s.remove(); removed++;" +
                "  }" +
                "});" +
                "return 'Removed ' + removed + ' style elements';"
            ).then(String.class, result -> {
                log("Item 12: " + result);
            });
        });

        FlexLayout item12Controls = new FlexLayout(
            testPartSelectorsButton, applyPartStyleButton, removePartStyleButton
        );
        item12Controls.setWidthFull();
        item12Controls.setFlexWrap(FlexLayout.FlexWrap.WRAP);
        item12Controls.getStyle().set("gap", "8px");
        add(item12Controls);

        // === Phase 3: Item 16 - runBeforeClientResponse ===
        add(new H4("Item 16: runBeforeClientResponse"));

        Button testRunBeforeButton = new Button("Test runBeforeClientResponse", e -> {
            String marker = "test-" + System.currentTimeMillis();
            editor.testRunBeforeClientResponse(marker);
            log("Item 16: Called testRunBeforeClientResponse with marker: " + marker);
            // Read the result after a short delay (it's deferred)
            editor.getElement().executeJs(
                "return new Promise(resolve => {" +
                "  setTimeout(() => resolve(JSON.stringify($0.__item16Result || {error: 'not set yet'})), 100);" +
                "});"
            ).then(String.class, result -> {
                log("Item 16 Result: " + result);
            });
        });

        FlexLayout item16Controls = new FlexLayout(testRunBeforeButton);
        item16Controls.setWidthFull();
        item16Controls.setFlexWrap(FlexLayout.FlexWrap.WRAP);
        item16Controls.getStyle().set("gap", "8px");
        add(item16Controls);

        // === Phase 3: Item 19 - HTML Sanitizer ===
        add(new H4("Item 19: HTML Sanitizer vs Custom Markup"));

        Button testSanitizer1 = new Button("Test: <span class='ql-tab'>", e -> {
            String input = "<p>Hello<span class=\"ql-tab\" style=\"display:inline-block;min-width:2em\"></span>World</p>";
            String output = EnhancedRichTextEditor.testSanitizer(input);
            log("Item 19 - Input:  " + input + "\nSanitized: " + output);
        });

        Button testSanitizer2 = new Button("Test: Full ERTE markup", e -> {
            String input = "<p>Text<span class=\"ql-tab\" contenteditable=\"false\" "
                + "style=\"display:inline-block;min-width:50px;background:rgba(0,120,212,0.1)\">"
                + "</span>After tab</p>";
            String output = EnhancedRichTextEditor.testSanitizer(input);
            log("Item 19 - Input:  " + input + "\nSanitized: " + output);
        });

        Button testSanitizer3 = new Button("Test: Basic safe HTML", e -> {
            String input = "<p>Normal <strong>bold</strong> <em>italic</em> text</p>";
            String output = EnhancedRichTextEditor.testSanitizer(input);
            log("Item 19 - Input:  " + input + "\nSanitized: " + output);
        });

        Button testSanitizer4 = new Button("Test: Safelist.basic() tags", e -> {
            // Test what Safelist.basic() allows
            String input = "<p>Paragraph</p><span>Span</span><div>Div</div>"
                + "<blockquote>Quote</blockquote><pre>Pre</pre>"
                + "<a href=\"http://x\">Link</a><code>Code</code>"
                + "<sub>Sub</sub><sup>Sup</sup>";
            String output = EnhancedRichTextEditor.testSanitizer(input);
            log("Item 19 Safelist test:\nInput:  " + input + "\nOutput: " + output);
        });

        Button testRoundTrip = new Button("Test: Set HTML + Read Back", e -> {
            String tabHtml = "<p>Tab<span class=\"ql-tab\"></span>Content</p>";
            log("Item 19 Round-trip:\nSetting HTML: " + tabHtml);
            try {
                editor.asHtml().setValue(tabHtml);
                // Read back immediately (sync value)
                String storedValue = editor.getHtmlValueForTest();
                log("Item 19 Java getValue(): " + storedValue);
                // Also read from Quill on client side
                editor.getElement().executeJs(
                    "const q = $0._editor;" +
                    "if (q) {" +
                    "  const delta = q.getContents();" +
                    "  const html = q.root.innerHTML;" +
                    "  return JSON.stringify({delta: delta.ops, html: html});" +
                    "} else { return '{\"error\":\"no editor\"}'; }"
                ).then(String.class, result -> {
                    log("Item 19 Client-side state: " + result);
                });
            } catch (Exception ex) {
                log("Item 19 ERROR: " + ex.getMessage());
            }
        });

        FlexLayout item19Controls = new FlexLayout(
            testSanitizer1, testSanitizer2, testSanitizer3, testSanitizer4, testRoundTrip
        );
        item19Controls.setWidthFull();
        item19Controls.setFlexWrap(FlexLayout.FlexWrap.WRAP);
        item19Controls.getStyle().set("gap", "8px");
        add(item19Controls);

        // === Phase 3: Item 21 - Clipboard Round-Trip ===
        add(new H4("Item 21: Clipboard / Delta Round-Trip"));

        Button testDeltaRoundTrip = new Button("Test Delta Round-Trip", e -> {
            editor.getElement().executeJs("return $0.testDeltaRoundTrip()")
                .then(String.class, result -> {
                    log("Item 21 Delta Round-Trip:\n" + result);
                });
        });

        FlexLayout item21Controls = new FlexLayout(testDeltaRoundTrip);
        item21Controls.setWidthFull();
        item21Controls.setFlexWrap(FlexLayout.FlexWrap.WRAP);
        item21Controls.getStyle().set("gap", "8px");
        add(item21Controls);

        // === Table Spike Tests ===
        add(new H4("Table Spike: Blot Migration to Quill 2"));

        Button testTableSpike = new Button("Run Table Spike Tests", e -> {
            editor.getElement().executeJs("return $0.testTableSpike()")
                .then(String.class, result -> {
                    log("Table Spike Results:\n" + result);
                });
        });

        FlexLayout tableControls = new FlexLayout(testTableSpike);
        tableControls.setWidthFull();
        tableControls.setFlexWrap(FlexLayout.FlexWrap.WRAP);
        tableControls.getStyle().set("gap", "8px");
        add(tableControls);

        // Info bar
        Div info = new Div(new Span(
            "Phase 3 + Table Spike tests. Item 23 (production build) tested via Maven CLI."
        ));
        info.getStyle().set("padding", "8px");
        info.getStyle().set("background", "#f0f0f0");
        info.getStyle().set("border-radius", "4px");
        add(info);

        // Result area
        add(resultArea);

        // Editor at the bottom
        add(editor);
        setFlexGrow(1, editor);
    }

    private void log(String message) {
        String current = resultArea.getText();
        if ("Test results will appear here...".equals(current)) {
            current = "";
        }
        resultArea.setText(current + message + "\n\n");
        Notification.show(message.split("\n")[0], 3000, Notification.Position.BOTTOM_START);
    }
}
