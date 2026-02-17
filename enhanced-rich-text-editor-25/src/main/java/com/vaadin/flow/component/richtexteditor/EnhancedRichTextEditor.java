/*-
 * #%L
 * Enhanced Rich Text Editor (V25)
 * %%
 * Copyright (C) 2025 Vaadin Ltd
 * %%
 * This program is available under Commercial Vaadin Add-On License 3.0
 * (CVALv3).
 *
 * See the file licensing.txt distributed with this software for more
 * information about licensing.
 *
 * You should have received a copy of the CVALv3 along with this program.
 * If not, see <http://vaadin.com/license/cval-3>.
 * #L%
 */
package com.vaadin.flow.component.richtexteditor;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;

/**
 * Enhanced Rich Text Editor for Vaadin 25.
 * <p>
 * Extends Vaadin's built-in RichTextEditor (RTE 2, Quill 2.0.3) with
 * additional features: tabstops, read-only sections, placeholders,
 * custom toolbar slots, rulers, and more.
 * <p>
 * Uses a custom tag to avoid conflicts with the standard RTE. The JS module
 * extends the RTE 2 Lit class via render() override (toolbar) and ready()
 * hook (Quill access).
 * <p>
 * Package: com.vaadin.flow.component.richtexteditor — same as RTE 2, giving
 * access to package-private methods (runBeforeClientResponse, sanitize, etc.)
 */
@Tag("vcf-enhanced-rich-text-editor")
@JsModule("./src/vcf-enhanced-rich-text-editor.js")
public class EnhancedRichTextEditor extends RichTextEditor {

    public EnhancedRichTextEditor() {
        super();
    }

    /**
     * Access to runBeforeClientResponse (package-private in RichTextEditor).
     * Used for deferred JS execution in the same pattern as RTE 2 internally.
     */
    protected void executeBeforeClientResponse(
            com.vaadin.flow.function.SerializableConsumer<com.vaadin.flow.component.UI> command) {
        runBeforeClientResponse(command);
    }

    /**
     * Access to the HTML sanitizer (package-private static in RichTextEditor).
     * May need overriding to whitelist ERTE-specific attributes (e.g., class on span).
     */
    public static String sanitizeHtml(String html) {
        return RichTextEditor.sanitize(html);
    }
}
