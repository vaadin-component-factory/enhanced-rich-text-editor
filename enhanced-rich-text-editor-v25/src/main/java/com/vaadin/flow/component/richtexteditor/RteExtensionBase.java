/*-
 * #%L
 * Enhanced Rich Text Editor V25
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
package com.vaadin.flow.component.richtexteditor;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.function.SerializableConsumer;

/**
 * Bridge class that lives in RTE 2's package to access package-private members.
 * <p>
 * All real ERTE logic belongs in {@code com.vaadin.componentfactory} — this
 * class only lifts visibility so that {@code EnhancedRichTextEditor} can extend
 * {@code RichTextEditor} without being in the same package.
 * <p>
 * Also provides the ERTE sanitizer ({@link #erteSanitize(String)}) and
 * overrides {@link #setPresentationValue(String)} to use it instead of
 * the parent's package-private {@code sanitize()}.
 * <p>
 * If RTE 2 changes its package-private API, only this class needs updating.
 */
public abstract class RteExtensionBase extends RichTextEditor {

    /**
     * ERTE-specific CSS classes allowed through the sanitizer.
     * Each migration phase adds its classes here.
     */
    private static final Set<String> ALLOWED_ERTE_CLASSES = Set.of(
            "ql-readonly", "ql-tab", "ql-soft-break", "ql-placeholder",
            "ql-nbsp", "td-q", "ql-editor__table--hideBorder");

    private static final Set<String> ALLOWED_CSS_PROPERTIES = Set.of(
            // Text
            "color", "background-color", "background", "font-size",
            "font-family", "font-weight", "font-style",
            // Layout
            "text-align", "text-indent", "text-decoration",
            "text-decoration-line", "text-decoration-style",
            "text-decoration-color", "direction",
            // Spacing
            "line-height", "letter-spacing", "word-spacing",
            // Box
            "margin", "margin-top", "margin-right", "margin-bottom",
            "margin-left", "padding", "padding-top", "padding-right",
            "padding-bottom", "padding-left",
            // Border
            "border", "border-top", "border-right", "border-bottom",
            "border-left", "border-width", "border-style", "border-color",
            "border-collapse", "border-spacing",
            // Display
            "display", "white-space", "vertical-align", "visibility",
            "opacity",
            // Size
            "width", "height", "min-width", "max-width", "min-height",
            "max-height",
            // Position
            "position", "top", "right", "bottom", "left", "float",
            // Other
            "list-style-type", "overflow", "overflow-x", "overflow-y",
            "cursor");

    private static final Set<String> SAFE_CSS_FUNCTIONS = Set.of(
            "rgb(", "rgba(", "hsl(", "hsla(", "calc(");

    private static final Set<String> SAFE_DATA_MIMES = Set.of(
            "image/png", "image/jpeg", "image/jpg", "image/gif",
            "image/webp", "image/bmp", "image/x-icon");

    private static final Pattern CLASS_ATTR_PATTERN = Pattern
            .compile("class=\"([^\"]*)\"");

    private static final Pattern STYLE_ATTR_PATTERN = Pattern
            .compile("style=\"([^\"]*)\"");

    private static final Pattern CSS_COMMENT_PATTERN = Pattern
            .compile("/\\*.*?\\*/");

    private static final Pattern CSS_FUNCTION_PATTERN = Pattern
            .compile("\\w+\\s*\\(");

    private static final Pattern DATA_SRC_PATTERN = Pattern.compile(
            "src=\"data:\\s*([^;\"]+)[^\"]*\"",
            Pattern.CASE_INSENSITIVE);

    private boolean ertePendingPresentationUpdate;

    /**
     * Visibility-widening override: package-private → protected.
     * {@code RichTextEditor.runBeforeClientResponse()} is package-private,
     * invisible to {@code EnhancedRichTextEditor} in
     * {@code com.vaadin.componentfactory}. This override widens it to
     * {@code protected} so subclasses in other packages can use it.
     */
    @Override
    protected void runBeforeClientResponse(
            SerializableConsumer<UI> command) {
        super.runBeforeClientResponse(command);
    }

    /**
     * Sanitizes HTML with ERTE's extended whitelist. Extends RTE 2's safelist
     * with {@code span[class]}, {@code span[contenteditable]} and then
     * post-filters to only allow known ERTE classes and
     * {@code contenteditable="false"}.
     *
     * @param html the raw HTML
     * @return sanitized HTML safe for ERTE rendering
     */
    protected static String erteSanitize(String html) {
        if (html == null || html.isEmpty()) {
            return html;
        }
        var settings = new org.jsoup.nodes.Document.OutputSettings();
        settings.prettyPrint(false);

        // Start from RTE 2's safelist and extend for ERTE
        Safelist safelist = Safelist.basic()
                .addTags("img", "h1", "h2", "h3", "s",
                         "table", "tbody", "tr", "td", "th",
                         "colgroup", "col")
                .addAttributes("img", "align", "alt", "height", "src",
                        "title", "width")
                .addAttributes(":all", "style", "class")
                .addProtocols("img", "src", "data", "http", "https")
                // ERTE additions
                .addAttributes("span", "contenteditable",
                        "aria-readonly", "data-placeholder")
                // Table additions
                .addAttributes("td", "table_id", "row_id", "cell_id",
                        "merge_id", "colspan", "rowspan", "table-class")
                .addAttributes("tr", "row_id")
                .addAttributes("table", "table_id");

        String safe = Jsoup.clean(html, "", safelist, settings);

        // Post-filter: only allow known ERTE classes, strip unknown ones
        safe = filterErteClasses(safe);

        // Post-filter: restrict style attributes to safe CSS properties
        safe = filterStyleAttributes(safe);

        // Post-filter: restrict data: URLs to safe image MIME types
        safe = filterDataUrls(safe);

        // Post-filter: only allow contenteditable="false"
        safe = safe.replace("contenteditable=\"true\"", "");
        safe = safe.replace("contenteditable=\"\"", "");

        return safe;
    }

    /**
     * Filters class attributes to only keep standard Quill classes
     * (ql-align-*, ql-indent-*) and known ERTE classes.
     */
    private static String filterErteClasses(String html) {
        Matcher m = CLASS_ATTR_PATTERN.matcher(html);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String classValue = m.group(1);
            String[] classes = classValue.split("\\s+");
            StringBuilder filtered = new StringBuilder();
            for (String cls : classes) {
                if (cls.isEmpty()) continue;
                // Keep standard Quill classes for alignment/indent
                if (cls.startsWith("ql-align")
                        || cls.startsWith("ql-indent")) {
                    if (filtered.length() > 0) filtered.append(' ');
                    filtered.append(cls);
                }
                // Keep known ERTE classes
                else if (ALLOWED_ERTE_CLASSES.contains(cls)) {
                    if (filtered.length() > 0) filtered.append(' ');
                    filtered.append(cls);
                }
                // Strip everything else
            }
            m.appendReplacement(sb,
                    "class=\"" + Matcher.quoteReplacement(filtered.toString())
                            + "\"");
        }
        m.appendTail(sb);
        return sb.toString();
    }

    /**
     * Filters style attributes to only allow safe CSS properties. Strips
     * dangerous CSS functions (only whitelisted ones like rgb/calc allowed),
     * {@code @import} directives, and CSS comments.
     */
    private static String filterStyleAttributes(String html) {
        Matcher m = STYLE_ATTR_PATTERN.matcher(html);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String styleValue = m.group(1);
            // Strip CSS comments first
            styleValue = CSS_COMMENT_PATTERN.matcher(styleValue).replaceAll("");
            String[] declarations = styleValue.split(";");
            StringBuilder filtered = new StringBuilder();
            for (String decl : declarations) {
                decl = decl.trim();
                if (decl.isEmpty()) continue;
                int colon = decl.indexOf(':');
                if (colon < 0) continue;
                String property = decl.substring(0, colon).trim()
                        .toLowerCase(Locale.ROOT);
                String value = decl.substring(colon + 1).trim();
                // Skip unknown properties
                if (!ALLOWED_CSS_PROPERTIES.contains(property)) continue;
                // Skip values containing @import
                if (value.toLowerCase(Locale.ROOT).contains("@import"))
                    continue;
                // Check for CSS function calls — only allow whitelisted ones
                Matcher funcMatcher = CSS_FUNCTION_PATTERN.matcher(
                        value.toLowerCase(Locale.ROOT));
                boolean hasDangerousFunction = false;
                while (funcMatcher.find()) {
                    String func = funcMatcher.group();
                    if (!SAFE_CSS_FUNCTIONS.contains(func)) {
                        hasDangerousFunction = true;
                        break;
                    }
                }
                if (hasDangerousFunction) continue;
                if (filtered.length() > 0) filtered.append("; ");
                filtered.append(property).append(": ").append(value);
            }
            if (filtered.length() > 0) {
                m.appendReplacement(sb, "style=\""
                        + Matcher.quoteReplacement(filtered.toString())
                        + "\"");
            } else {
                // Remove empty style attribute entirely
                m.appendReplacement(sb, "");
            }
        }
        m.appendTail(sb);
        return sb.toString();
    }

    /**
     * Filters {@code data:} URLs in {@code src} attributes to only allow
     * safe image MIME types. SVG is excluded (can contain scripts).
     */
    private static String filterDataUrls(String html) {
        Matcher m = DATA_SRC_PATTERN.matcher(html);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String mime = m.group(1).trim().toLowerCase(Locale.ROOT);
            if (SAFE_DATA_MIMES.contains(mime)) {
                // Keep safe data URL as-is
                m.appendReplacement(sb,
                        Matcher.quoteReplacement(m.group()));
            } else {
                // Strip entire src attribute for unsafe MIME types
                m.appendReplacement(sb, "");
            }
        }
        m.appendTail(sb);
        return sb.toString();
    }

    /**
     * Override server→client HTML path to use ERTE sanitizer instead of
     * parent's package-private {@code sanitize()}.
     * <p>
     * Replicates the parent's debounce pattern (own flag since parent's
     * {@code pendingPresentationUpdate} is private).
     * <p>
     * When the delta value property is set (via {@code asDelta().setValue()}),
     * the client-side {@code _valueChanged} observer already applies the content
     * directly via {@code setContents()}. In that case, we skip the
     * {@code dangerouslySetHtmlValue} call to avoid an HTML roundtrip that would
     * lose table structure data (template classes, cell IDs via clipboard
     * re-conversion).
     */
    @Override
    protected void setPresentationValue(String newPresentationValue) {
        String sanitized = erteSanitize(newPresentationValue);
        getElement().setProperty("htmlValue", sanitized);
        if (!ertePendingPresentationUpdate) {
            ertePendingPresentationUpdate = true;
            runBeforeClientResponse(ui -> {
                // If a non-empty delta value is set, the client-side _valueChanged
                // observer will have already applied the content via setContents().
                // Skip dangerouslySetHtmlValue to avoid overwriting with the HTML
                // roundtrip, which loses table template classes and can corrupt
                // table structure through clipboard re-conversion.
                String deltaValue = getElement().getProperty("value");
                if (deltaValue != null && !deltaValue.isEmpty()
                        && !"[{\"insert\":\"\\n\"}]".equals(deltaValue)) {
                    ertePendingPresentationUpdate = false;
                    return;
                }
                getElement().callJsFunction("dangerouslySetHtmlValue",
                        getElement().getProperty("htmlValue"));
                ertePendingPresentationUpdate = false;
            });
        }
    }
}
