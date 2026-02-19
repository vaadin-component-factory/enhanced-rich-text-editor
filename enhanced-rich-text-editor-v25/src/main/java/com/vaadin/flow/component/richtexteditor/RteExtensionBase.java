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
            "ql-readonly", "ql-tab", "ql-soft-break");

    private static final Pattern CLASS_ATTR_PATTERN = Pattern
            .compile("class=\"([^\"]*)\"");

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
                .addTags("img", "h1", "h2", "h3", "s")
                .addAttributes("img", "align", "alt", "height", "src",
                        "title", "width")
                .addAttributes(":all", "style")
                .addProtocols("img", "src", "data")
                // ERTE additions
                .addAttributes("span", "class", "contenteditable",
                        "aria-readonly");

        String safe = Jsoup.clean(html, "", safelist, settings);

        // Post-filter: only allow known ERTE classes, strip unknown ones
        safe = filterErteClasses(safe);

        // Post-filter: only allow contenteditable="false"
        safe = safe.replace("contenteditable=\"true\"", "");

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
     * Override server→client HTML path to use ERTE sanitizer instead of
     * parent's package-private {@code sanitize()}.
     * <p>
     * Replicates the parent's debounce pattern (own flag since parent's
     * {@code pendingPresentationUpdate} is private).
     */
    @Override
    protected void setPresentationValue(String newPresentationValue) {
        String sanitized = erteSanitize(newPresentationValue);
        getElement().setProperty("htmlValue", sanitized);
        if (!ertePendingPresentationUpdate) {
            ertePendingPresentationUpdate = true;
            runBeforeClientResponse(ui -> {
                getElement().callJsFunction("dangerouslySetHtmlValue",
                        getElement().getProperty("htmlValue"));
                ertePendingPresentationUpdate = false;
            });
        }
    }
}
