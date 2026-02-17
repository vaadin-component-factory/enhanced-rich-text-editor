package com.vaadin.flow.component.richtexteditor;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.function.SerializableConsumer;

/**
 * ERTE v25 Spike: Validate that extending RichTextEditor with a custom @Tag
 * produces a web component with the correct tag name and inherits all
 * RTE 2 functionality.
 *
 * Phase 3 additions:
 * - Item 16: runBeforeClientResponse access test
 * - Item 19: HTML sanitizer investigation
 *
 * Table Spike:
 * - T1-T7: Table blot migration to Quill 2 (tests in JS, triggered from SpikeView)
 */
@Tag("vcf-enhanced-rich-text-editor")
@JsModule("./vcf-enhanced-rich-text-editor.js")
public class EnhancedRichTextEditor extends RichTextEditor {

    public EnhancedRichTextEditor() {
        super();
    }

    // ================================================================
    // Item 16: Test runBeforeClientResponse access
    // ================================================================

    /**
     * Tests that runBeforeClientResponse (package-private in RichTextEditor)
     * is accessible from the subclass in the same package.
     *
     * Executes a JS snippet via the deferred execution pattern used by
     * RTE 2 internally (e.g., in setI18n, setPresentationValue).
     */
    public void testRunBeforeClientResponse(String testMarker) {
        // runBeforeClientResponse is package-private in RichTextEditor.
        // Since we're in the same package, we can call it directly.
        runBeforeClientResponse(ui -> {
            getElement().executeJs(
                "$0.__item16Result = { marker: $1, timestamp: Date.now(), source: 'runBeforeClientResponse' };"
                + "console.log('[ERTE Item 16] runBeforeClientResponse executed:', $0.__item16Result);",
                testMarker
            );
        });
    }

    /**
     * Reads the result set by testRunBeforeClientResponse.
     */
    public void readItem16Result(SerializableConsumer<String> callback) {
        getElement().executeJs(
            "return JSON.stringify($0.__item16Result || {error: 'not set'})"
        ).then(String.class, callback);
    }

    // ================================================================
    // Item 19: HTML Sanitizer Investigation
    // ================================================================

    /**
     * Tests how the RTE 2 sanitizer handles custom ERTE markup.
     * The sanitize() method is static package-private in RichTextEditor.
     */
    public static String testSanitizer(String html) {
        // sanitize() is static package-private in RichTextEditor.
        // Since we're in the same package, we can call it directly.
        return RichTextEditor.sanitize(html);
    }

    /**
     * Sets HTML value via the standard asHtml().setValue() path,
     * which goes through the sanitizer.
     */
    public void setHtmlValueForTest(String html) {
        asHtml().setValue(html);
    }

    /**
     * Reads HTML value back from the editor component (the sanitized value
     * that was actually stored).
     */
    public String getHtmlValueForTest() {
        return getValue();
    }
}
