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
package com.vaadin.componentfactory;

import java.io.Serializable;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.vaadin.componentfactory.toolbar.ToolbarSlot;
import com.vaadin.flow.component.AbstractField;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.DomEvent;
import com.vaadin.flow.component.EventData;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.richtexteditor.RichTextEditor;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.shared.Registration;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * Enhanced Rich Text Editor — V25 / Quill 2.
 * <p>
 * Extends {@link RichTextEditor} directly. All ERTE-specific logic —
 * including the sanitizer, value overrides, toolbar, placeholders, and
 * tabstops — lives in this class.
 *
 * <h2>Placeholder Workflow</h2>
 * <p>
 * Placeholders are configured via three related methods that work together:
 * <ol>
 *   <li><b>{@link #setPlaceholders(List)}</b> — Defines the master list of
 *       available placeholders shown in the placeholder dialog. Each placeholder
 *       has a text (e.g., "{{name}}"), optional tag for grouping, and optional
 *       format/altFormat for appearance customization.</li>
 *   <li><b>{@link #setPlaceholderTags(List)}</b> — (Optional) Defines tags
 *       for grouping placeholders in the dialog. If not set, all placeholders
 *       appear in a single list.</li>
 *   <li><b>{@link #setPlaceholderAltAppearancePattern(String)}</b> — (Optional)
 *       Defines a regex pattern to automatically switch placeholder appearance
 *       based on surrounding text. When the text matching this pattern is found
 *       before the placeholder, the altFormat is used instead of format.</li>
 * </ol>
 * <p>
 * <b>Example usage:</b>
 * <pre>{@code
 * // 1. Define placeholders with default and alternative formatting
 * editor.setPlaceholders(List.of(
 *     new Placeholder("{{name}}", "user",
 *         Map.of("bold", true),           // default format
 *         Map.of("bold", true, "italic", true))  // altFormat
 * ));
 *
 * // 2. (Optional) Define tags for dialog grouping
 * editor.setPlaceholderTags(List.of("user", "system", "date"));
 *
 * // 3. (Optional) Auto-switch to altFormat when preceded by "Dear "
 * editor.setPlaceholderAltAppearancePattern("Dear\\s+$");
 * }</pre>
 * <p>
 * When a user types "Dear {{name}}", the placeholder will use its altFormat
 * (bold + italic) instead of the default format (bold only).
 */
@Tag("vcf-enhanced-rich-text-editor")
@JsModule("./vcf-enhanced-rich-text-editor.js")
public class EnhancedRichTextEditor extends RichTextEditor {

    /**
     * Constructs an empty {@code EnhancedRichTextEditor}.
     */
    public EnhancedRichTextEditor() {
        super();
    }

    /**
     * Constructs a {@code EnhancedRichTextEditor} with the initial value.
     *
     * @param initialValue the initial value in HTML format
     * @see #setValue(String)
     */
    public EnhancedRichTextEditor(String initialValue) {
        this();
        setValue(initialValue);
    }

    /**
     * Constructs an empty {@code EnhancedRichTextEditor} with a value change
     * listener.
     *
     * @param listener the value change listener
     * @see #addValueChangeListener(HasValue.ValueChangeListener)
     */
    public EnhancedRichTextEditor(
            HasValue.ValueChangeListener<? super AbstractField.ComponentValueChangeEvent<
                    RichTextEditor, String>> listener) {
        this();
        addValueChangeListener(listener);
    }

    /**
     * Constructs a {@code EnhancedRichTextEditor} with the initial value and a
     * value change listener.
     *
     * @param initialValue the initial value in HTML format
     * @param listener     the value change listener
     * @see #setValue(String)
     * @see #addValueChangeListener(HasValue.ValueChangeListener)
     */
    public EnhancedRichTextEditor(String initialValue,
            HasValue.ValueChangeListener<? super AbstractField.ComponentValueChangeEvent<
                    RichTextEditor, String>> listener) {
        this();
        setValue(initialValue);
        addValueChangeListener(listener);
    }

    /**
     * Schedules a command to run before the next client response.
     * <p>
     * Reimplements RichTextEditor's package-private method using public
     * Vaadin API, so ERTE can extend RichTextEditor directly without
     * needing a bridge class in the foreign package.
     */
    protected void runBeforeClientResponse(
            SerializableConsumer<UI> command) {
        getElement().getNode().runWhenAttached(ui -> ui
                .beforeClientResponse(this,
                        context -> command.accept(ui)));
    }

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
    private static final Pattern VALID_CLASS_NAME = Pattern
            .compile("[A-Za-z][A-Za-z0-9\\-]*");
    private List<Placeholder> placeholders;

    private boolean ertePendingPresentationUpdate;
    private final Set<String> dynamicAllowedClasses = new LinkedHashSet<>();

    /**
     * Sanitizes HTML with ERTE's extended whitelist using only static allowed
     * classes. Equivalent to {@code erteSanitize(html, Set.of())}.
     *
     * @param html the raw HTML
     * @return sanitized HTML safe for ERTE rendering
     */
    protected static String erteSanitize(String html) {
        return erteSanitize(html, Set.of());
    }

    /**
     * Sanitizes HTML with ERTE's extended whitelist plus additional dynamic
     * classes.
     * <p>
     * The {@code extraClasses} set is trusted (no validation) — callers are
     * responsible for ensuring class names are safe. The public API
     * {@link #addAllowedHtmlClasses(String...)} performs validation.
     *
     * @param html         the raw HTML
     * @param extraClasses additional CSS classes to preserve (may be empty)
     * @return sanitized HTML safe for ERTE rendering
     */
    protected static String erteSanitize(String html,
            Set<String> extraClasses) {
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
        safe = filterErteClasses(safe, extraClasses);

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
     * (ql-align-*, ql-indent-*), known ERTE classes, and dynamic extra
     * classes registered via {@link #addAllowedHtmlClasses(String...)}.
     */
    private static String filterErteClasses(String html,
            Set<String> extraClasses) {
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
                // Keep dynamic extra classes (e.g., template IDs)
                else if (!extraClasses.isEmpty()
                        && extraClasses.contains(cls)) {
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

    // ---- Dynamic Allowed HTML Classes API ----

    /**
     * Registers additional CSS class names to be preserved by the sanitizer.
     * Used by addons (e.g., Tables) to whitelist template classes dynamically.
     * <p>
     * Class names must match {@code [A-Za-z][A-Za-z0-9-]*} (same pattern as
     * template IDs) and must not start with {@code "ql-"} (reserved for Quill
     * internals). Note: underscores are not permitted — this matches the
     * template ID validation in the Tables addon.
     * <p>
     * Must be called from the Vaadin session (UI) thread.
     *
     * @param classNames one or more CSS class names
     * @throws IllegalArgumentException if a class name is invalid
     * @since 6.0.0
     */
    public void addAllowedHtmlClasses(String... classNames) {
        for (String cls : classNames) {
            validateClassName(cls);
            dynamicAllowedClasses.add(cls);
        }
    }

    /**
     * Removes previously registered dynamic CSS class names.
     *
     * @param classNames one or more CSS class names to remove
     * @since 6.0.0
     */
    public void removeAllowedHtmlClasses(String... classNames) {
        for (String cls : classNames) {
            dynamicAllowedClasses.remove(cls);
        }
    }

    /**
     * Returns the currently registered dynamic allowed classes (unmodifiable
     * view).
     *
     * @return unmodifiable set of registered class names
     * @since 6.0.0
     */
    public Set<String> getAllowedHtmlClasses() {
        return Collections.unmodifiableSet(dynamicAllowedClasses);
    }

    /**
     * Validates a CSS class name for use with {@link #addAllowedHtmlClasses}.
     * Package-private for test access.
     */
    static void validateClassName(String cls) {
        if (cls == null || cls.isEmpty()) {
            throw new IllegalArgumentException(
                    "Class name must not be null or empty");
        }
        if (cls.startsWith("ql-")) {
            throw new IllegalArgumentException(
                    "Class name must not start with 'ql-' (reserved for "
                            + "Quill): " + cls);
        }
        if (!VALID_CLASS_NAME.matcher(cls).matches()) {
            throw new IllegalArgumentException(
                    "Invalid class name (must match "
                            + "[A-Za-z][A-Za-z0-9-]*): " + cls);
        }
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
        String sanitized = erteSanitize(newPresentationValue,
                dynamicAllowedClasses);
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

    /**
     * Intercepts the client→server HTML value sync to use ERTE's extended
     * sanitizer instead of the parent's {@code sanitize()}.
     * <p>
     * {@code AbstractSinglePropertyField} registers a property change listener
     * for {@code htmlValue} that converts via
     * {@code RichTextEditor::presentationToModel} which calls the parent's
     * package-private {@code sanitize()}. That safelist does NOT include
     * {@code <table>}, {@code <tr>}, {@code <td>} — so all table elements
     * are stripped, turning tables into plain paragraphs.
     * <p>
     * This override re-reads the raw {@code htmlValue} from the element
     * property and applies {@code erteSanitize()} instead, preserving table
     * structure and ERTE-specific attributes.
     */
    @Override
    protected void setModelValue(String newModelValue, boolean fromClient) {
        // Always re-sanitize using erteSanitize instead of parent's sanitize().
        // The parent's presentationToModel converter (RichTextEditor::sanitize)
        // strips <table>, <tr>, <td> elements. We intercept here and re-read
        // the raw htmlValue from the element property, then apply erteSanitize()
        // which preserves table structure and ERTE-specific attributes.
        //
        // This override fires in two scenarios:
        // 1. fromClient=true: html-value-changed event from the client
        // 2. fromClient=false: property change triggered by setPresentationValue
        //    setting the htmlValue element property (server-side)
        // Both need re-sanitization because the parent's converter always strips
        // tables before passing the value here.
        String rawHtml = getElement().getProperty("htmlValue", "");
        if (rawHtml != null && !rawHtml.isEmpty()) {
            super.setModelValue(erteSanitize(rawHtml,
                    dynamicAllowedClasses), fromClient);
        } else {
            super.setModelValue(newModelValue, fromClient);
        }
    }

    // ---- Toolbar component API ----

    /**
     * Ensures the component has the given part name so that
     * {@code ::slotted([part~='toolbar-custom-component'])} styles apply.
     */
    private void addOrAppendPartAttribute(Component component,
            String partName) {
        String existingPart = component.getElement().getAttribute("part");
        if (existingPart == null || existingPart.isEmpty()) {
            component.getElement().setAttribute("part", partName);
        } else if (!existingPart.contains(partName)) {
            component.getElement().setAttribute("part",
                    existingPart + " " + partName);
        }
    }

    /**
     * Adds components to the given toolbar slot (appended).
     */
    public void addToolbarComponents(ToolbarSlot toolbarSlot,
            Component... components) {
        Objects.requireNonNull(components);
        for (Component component : components) {
            Objects.requireNonNull(component);
            addOrAppendPartAttribute(component, "toolbar-custom-component");
            SlotUtil.addComponent(this, toolbarSlot.getSlotName(), component);
        }
    }

    /**
     * Adds components to the given toolbar slot at the specified index.
     */
    public void addToolbarComponentsAtIndex(ToolbarSlot toolbarSlot,
            int index, Component... components) {
        Objects.requireNonNull(components);
        for (Component component : components) {
            Objects.requireNonNull(component);
            addOrAppendPartAttribute(component, "toolbar-custom-component");
            SlotUtil.addComponentAtIndex(this, toolbarSlot.getSlotName(),
                    component, index);
        }
    }

    /**
     * Returns a toolbar component with the given id from the slot.
     */
    @SuppressWarnings("unchecked")
    public <T extends Component> T getToolbarComponent(
            ToolbarSlot toolbarSlot, String id) {
        Objects.requireNonNull(id);
        return (T) SlotUtil.getComponent(this, toolbarSlot.getSlotName(), id);
    }

    /**
     * Removes a toolbar component by id.
     */
    public void removeToolbarComponent(ToolbarSlot toolbarSlot, String id) {
        Objects.requireNonNull(id);
        SlotUtil.removeComponent(this, toolbarSlot.getSlotName(), id);
    }

    /**
     * Removes a toolbar component by reference.
     */
    public void removeToolbarComponent(ToolbarSlot toolbarSlot,
            Component component) {
        Objects.requireNonNull(component);
        SlotUtil.removeComponent(this, toolbarSlot.getSlotName(), component);
    }

    /**
     * Convenience: adds components to {@link ToolbarSlot#GROUP_CUSTOM}.
     */
    public void addCustomToolbarComponents(Component... components) {
        addToolbarComponents(ToolbarSlot.GROUP_CUSTOM, components);
    }

    /**
     * Convenience: adds components to {@link ToolbarSlot#GROUP_CUSTOM}
     * at the given index.
     */
    public void addCustomToolbarComponentsAtIndex(int index,
            Component... components) {
        addToolbarComponentsAtIndex(ToolbarSlot.GROUP_CUSTOM, index,
                components);
    }

    /**
     * Replaces a standard toolbar button icon via its named slot.
     *
     * @param icon          the replacement icon
     * @param iconSlotName  the slot name of the button to replace (e.g. "undo")
     */
    public void replaceStandardButtonIcon(Icon icon, String iconSlotName) {
        SlotUtil.replaceStandardButtonIcon(this, icon, iconSlotName);
    }

    /**
     * Replaces a standard toolbar button's icon via its enum constant.
     * <p>
     * The icon is projected into the button's light DOM slot, replacing the
     * default SVG icon. Pass {@code null} to clear a previously set icon and
     * restore the default appearance.
     * <p>
     * Example:
     * <pre>
     * editor.replaceStandardToolbarButtonIcon(
     *     ToolbarButton.BOLD,
     *     new Icon(VaadinIcon.STAR)
     * );
     * </pre>
     *
     * @param button the toolbar button to modify (not null)
     * @param icon   the replacement icon, or {@code null} to restore default
     * @throws NullPointerException if button is null
     */
    public void replaceStandardToolbarButtonIcon(ToolbarButton button, Icon icon) {
        Objects.requireNonNull(button, "ToolbarButton cannot be null");
        replaceStandardButtonIcon(icon, button.getPartSuffix());
    }

    // ---- Toolbar button visibility API ----

    /**
     * Toolbar buttons that can be shown or hidden via
     * {@link #setToolbarButtonsVisibility(Map)}.
     * <p>
     * Each constant maps to a shadow DOM button identified by part name
     * {@code toolbar-button-<partSuffix>}.
     */
    public enum ToolbarButton {
        // Standard RTE 2 buttons (25)
        UNDO("undo"), REDO("redo"),
        BOLD("bold"), ITALIC("italic"), UNDERLINE("underline"), STRIKE("strike"),
        COLOR("color"), BACKGROUND("background"),
        H1("h1"), H2("h2"), H3("h3"),
        SUBSCRIPT("subscript"), SUPERSCRIPT("superscript"),
        LIST_ORDERED("list-ordered"), LIST_BULLET("list-bullet"),
        OUTDENT("outdent"), INDENT("indent"),
        ALIGN_LEFT("align-left"), ALIGN_CENTER("align-center"),
        ALIGN_RIGHT("align-right"),
        IMAGE("image"), LINK("link"),
        BLOCKQUOTE("blockquote"), CODE_BLOCK("code-block"),
        CLEAN("clean"),
        // ERTE-specific buttons (5)
        READONLY("readonly"),
        PLACEHOLDER("placeholder"),
        PLACEHOLDER_APPEARANCE("placeholder-display"),
        WHITESPACE("whitespace"),
        ALIGN_JUSTIFY("align-justify");

        private final String partSuffix;

        ToolbarButton(String partSuffix) {
            this.partSuffix = partSuffix;
        }

        /** Returns the suffix portion (e.g. {@code "bold"}). */
        public String getPartSuffix() {
            return partSuffix;
        }

        /**
         * Returns the button name.
         * @deprecated Use {@link #getPartSuffix()} instead.
         */
        @Deprecated
        public String getButtonName() {
            return partSuffix;
        }

        /** Returns the full part name (e.g. {@code "toolbar-button-bold"}). */
        public String getPartName() {
            return "toolbar-button-" + partSuffix;
        }
    }

    private Map<ToolbarButton, Boolean> toolbarButtonsVisibility;

    /**
     * Shows or hides individual toolbar buttons. Pass {@code false} for a
     * button to hide it; pass {@code true} (or omit) to show it. Groups
     * whose <em>all</em> buttons are hidden are auto-hidden.
     * <p>
     * Pass {@code null} to reset all buttons to visible.
     *
     * @param visibility the visibility map, or {@code null} to reset
     */
    public void setToolbarButtonsVisibility(
            Map<ToolbarButton, Boolean> visibility) {
        this.toolbarButtonsVisibility = visibility;
        runBeforeClientResponse(ui -> {
            ObjectNode json = JacksonUtils.getMapper().createObjectNode();
            if (visibility != null) {
                for (var entry : visibility.entrySet()) {
                    json.put(entry.getKey().getPartSuffix(),
                            entry.getValue());
                }
            }
            getElement().executeJs(
                    "this.setToolbarButtonsVisibility($0)", json);
        });
    }

    /**
     * Returns the current toolbar button visibility map, or {@code null}
     * if no visibility has been set.
     *
     * @return the visibility map
     */
    public Map<ToolbarButton, Boolean> getToolbarButtonsVisibility() {
        return toolbarButtonsVisibility;
    }

    // ---- Keyboard Shortcut API ----

    /**
     * Binds a keyboard shortcut to a standard toolbar button. When the
     * shortcut is pressed, the button is clicked, triggering its native
     * handler (format toggle, dialog open, undo/redo, etc.).
     *
     * @param toolbarButton the toolbar button to trigger
     * @param key           Quill 2 key name (e.g. {@code "F9"}, {@code "b"})
     * @param shortKey      {@code true} for Ctrl (Win/Linux) or Cmd (Mac)
     * @param shiftKey      {@code true} for Shift modifier
     * @param altKey        {@code true} for Alt modifier
     */
    public void addStandardToolbarButtonShortcut(ToolbarButton toolbarButton,
            String key, boolean shortKey, boolean shiftKey, boolean altKey) {
        getElement().executeJs(
                "this.addStandardToolbarButtonShortcut($0, $1, $2, $3, $4)",
                toolbarButton.getPartSuffix(), key, shortKey, shiftKey, altKey);
    }

    /**
     * Binds a keyboard shortcut that moves focus from the editor to the
     * toolbar. The first visible toolbar button receives focus.
     *
     * @param key      Quill 2 key name (e.g. {@code "F10"})
     * @param shortKey {@code true} for Ctrl (Win/Linux) or Cmd (Mac)
     * @param shiftKey {@code true} for Shift modifier
     * @param altKey   {@code true} for Alt modifier
     */
    public void addToolbarFocusShortcut(String key, boolean shortKey,
            boolean shiftKey, boolean altKey) {
        getElement().executeJs(
                "this.addToolbarFocusShortcut($0, $1, $2, $3)",
                key, shortKey, shiftKey, altKey);
    }

    // ---- Whitespace Indicators API ----

    /**
     * Sets whether whitespace indicators are shown in the editor.
     * When enabled, special characters are displayed: → (tab), ↵ (soft-break),
     * ¶ (paragraph end), ⮐→ (auto-wrap).
     *
     * @param show true to show whitespace indicators
     */
    public void setShowWhitespace(boolean show) {
        getElement().setProperty("showWhitespace", show);
    }

    /**
     * Returns whether whitespace indicators are currently shown.
     *
     * @return true if whitespace indicators are visible
     */
    public boolean isShowWhitespace() {
        return getElement().getProperty("showWhitespace", false);
    }

    // ---- TabStop API ----

    /**
     * Sets tabstop positions and alignments on the ruler.
     *
     * @param tabStops the list of tab stops to set
     */
    public void setTabStops(List<TabStop> tabStops) {
        ArrayNode array = JacksonUtils.getMapper().createArrayNode();
        for (TabStop ts : tabStops) {
            ObjectNode obj = array.addObject();
            obj.put("direction", ts.getDirection().name().toLowerCase());
            obj.put("position", ts.getPosition());
        }
        getElement().setPropertyJson("tabStops", array);
    }

    /**
     * Returns the current tabstop configuration.
     *
     * @return list of tab stops, never null
     */
    public List<TabStop> getTabStops() {
        ArrayNode raw = (ArrayNode) getElement().getPropertyRaw("tabStops");
        if (raw == null) {
            return List.of();
        }
        List<TabStop> result = new ArrayList<>();
        for (int i = 0; i < raw.size(); i++) {
            var obj = raw.get(i);
            result.add(new TabStop(
                    TabStop.Direction.valueOf(
                            obj.get("direction").asText().toUpperCase()),
                    obj.get("position").asDouble()));
        }
        return result;
    }

    /**
     * When true, the rulers are not visible.
     *
     * @param noRulers true to hide rulers, false to show them
     */
    public void setNoRulers(boolean noRulers) {
        getElement().setProperty("noRulers", noRulers);
    }

    /**
     * Returns whether rulers are hidden.
     *
     * @return true if rulers are hidden
     */
    public boolean isNoRulers() {
        return getElement().getProperty("noRulers", false);
    }

    // ---- Placeholder API ----

    /**
     * Sets the available placeholders for the editor.
     *
     * @param placeholders the placeholder definitions
     */
    public void setPlaceholders(Collection<Placeholder> placeholders) {
        this.placeholders = new ArrayList<>(placeholders);
        ArrayNode array = JacksonUtils.getMapper().createArrayNode();
        for (Placeholder p : placeholders) {
            array.add(p.toJson());
        }
        getElement().setPropertyJson("placeholders", array);
    }

    /**
     * Returns the current placeholder configuration.
     *
     * @return list of placeholders, never null
     */
    public List<Placeholder> getPlaceholders() {
        return placeholders != null ? List.copyOf(placeholders) : List.of();
    }

    /**
     * Sets the start and end tags displayed around placeholder text.
     *
     * @param start the start tag (e.g. "@", "[")
     * @param end   the end tag (e.g. "", "]"), may be null
     */
    public void setPlaceholderTags(String start, String end) {
        ObjectNode tags = JacksonUtils.getMapper().createObjectNode();
        tags.put("start", start);
        tags.put("end", end != null ? end : "");
        getElement().setPropertyJson("placeholderTags", tags);
    }

    /**
     * Sets the regex pattern used to extract alt appearance text from
     * placeholder text. Groups matched by this pattern are shown in alt mode.
     *
     * @param pattern the regex pattern
     */
    public void setPlaceholderAltAppearancePattern(String pattern) {
        getElement().setProperty("placeholderAltAppearancePattern", pattern);
    }

    /**
     * Returns the regex pattern used for alt appearance matching.
     *
     * @return the pattern, or {@code null} if not set
     */
    public String getPlaceholderAltAppearancePattern() {
        return getElement().getProperty("placeholderAltAppearancePattern");
    }

    /**
     * Toggles between normal and alternative placeholder appearance.
     *
     * @param alt true for alt appearance, false for normal
     */
    public void setPlaceholderAltAppearance(boolean alt) {
        getElement().setProperty("placeholderAltAppearance", alt);
    }

    /**
     * Returns whether the editor is currently showing alt placeholder
     * appearance.
     *
     * @return true if alt appearance is active
     */
    public boolean isPlaceholderAltAppearance() {
        return getElement().getProperty("placeholderAltAppearance", false);
    }

    /**
     * Look up a full Placeholder from the master list by text match.
     *
     * @param placeholder the placeholder to look up (matched by text)
     * @return the matching placeholder from the master list, or the input
     *         placeholder if not found
     */
    protected Placeholder getPlaceholder(Placeholder placeholder) {
        if (placeholders == null || placeholder == null) return placeholder;
        return placeholders.stream()
                .filter(p -> p.getText().equals(placeholder.getText()))
                .findFirst().orElse(placeholder);
    }

    // ---- Placeholder event listeners ----

    /**
     * Adds a listener for when the placeholder toolbar button is clicked.
     * <p>
     * Fired when the user clicks the placeholder button in the toolbar. The event
     * provides the current cursor position and an {@code insert()} method to
     * programmatically insert a placeholder at that position.
     *
     * @param listener the listener to add
     * @return a registration object for removing the listener
     */
    public Registration addPlaceholderButtonClickedListener(
            ComponentEventListener<PlaceholderButtonClickedEvent> listener) {
        return addListener(PlaceholderButtonClickedEvent.class, listener);
    }

    /**
     * Adds a listener for when placeholders are about to be inserted.
     * <p>
     * Fired before placeholders are inserted into the editor (via dialog or
     * programmatically). The event can be used to validate or modify the
     * placeholders before insertion. If {@link PlaceholderBeforeInsertEvent#insert()}
     * is not called, the insertion is cancelled.
     *
     * @param listener the listener to add
     * @return a registration object for removing the listener
     */
    public Registration addPlaceholderBeforeInsertListener(
            ComponentEventListener<PlaceholderBeforeInsertEvent> listener) {
        return addListener(PlaceholderBeforeInsertEvent.class, listener);
    }

    /**
     * Adds a listener for when placeholders have been inserted.
     * <p>
     * Fired after placeholders are successfully inserted into the editor.
     * This is a notification event (read-only) — use
     * {@link #addPlaceholderBeforeInsertListener} to prevent or modify insertion.
     *
     * @param listener the listener to add
     * @return a registration object for removing the listener
     */
    public Registration addPlaceholderInsertedListener(
            ComponentEventListener<PlaceholderInsertedEvent> listener) {
        return addListener(PlaceholderInsertedEvent.class, listener);
    }

    /**
     * Adds a listener for when placeholders are about to be removed.
     * <p>
     * Fired before placeholders are removed from the editor (via Delete/Backspace
     * key). The event can be used to prevent removal of protected placeholders.
     * If {@link PlaceholderBeforeRemoveEvent#remove()} is not called, the
     * removal is cancelled.
     *
     * @param listener the listener to add
     * @return a registration object for removing the listener
     */
    public Registration addPlaceholderBeforeRemoveListener(
            ComponentEventListener<PlaceholderBeforeRemoveEvent> listener) {
        return addListener(PlaceholderBeforeRemoveEvent.class, listener);
    }

    /**
     * Adds a listener for when placeholders have been removed.
     * <p>
     * Fired after placeholders are successfully removed from the editor.
     * This is a notification event (read-only) — use
     * {@link #addPlaceholderBeforeRemoveListener} to prevent removal.
     *
     * @param listener the listener to add
     * @return a registration object for removing the listener
     */
    public Registration addPlaceholderRemovedListener(
            ComponentEventListener<PlaceholderRemovedEvent> listener) {
        return addListener(PlaceholderRemovedEvent.class, listener);
    }

    /**
     * Adds a listener for when a placeholder is selected.
     * <p>
     * Fired when the user clicks on a placeholder in the editor or navigates
     * to it via keyboard (arrow keys, Tab). The event provides the selected
     * placeholder(s).
     *
     * @param listener the listener to add
     * @return a registration object for removing the listener
     */
    public Registration addPlaceholderSelectedListener(
            ComponentEventListener<PlaceholderSelectedEvent> listener) {
        return addListener(PlaceholderSelectedEvent.class, listener);
    }

    /**
     * Adds a listener for when a placeholder loses selection.
     * <p>
     * Fired when the cursor moves away from a placeholder (e.g., user clicks
     * elsewhere or presses an arrow key). Useful for cleaning up UI state
     * related to placeholder selection.
     *
     * @param listener the listener to add
     * @return a registration object for removing the listener
     */
    public Registration addPlaceholderLeaveListener(
            ComponentEventListener<PlaceholderLeaveEvent> listener) {
        return addListener(PlaceholderLeaveEvent.class, listener);
    }

    /**
     * Adds a listener for when placeholder appearance changes.
     * <p>
     * Fired when a placeholder switches between its default format and altFormat,
     * based on the {@link #setPlaceholderAltAppearancePattern(String)} regex.
     * The event provides the new appearance state (altAppearance boolean) and
     * the appearance label (if set).
     *
     * @param listener the listener to add
     * @return a registration object for removing the listener
     */
    public Registration addPlaceholderAppearanceChangedListener(
            ComponentEventListener<PlaceholderAppearanceChangedEvent> listener) {
        return addListener(PlaceholderAppearanceChangedEvent.class, listener);
    }

    // ========================================================================
    // Placeholder Event Classes
    // ========================================================================

    /**
     * Abstract base for events that carry a list of placeholders.
     * <p>
     * This base class handles deserialization of placeholder data from the client
     * and provides a {@link #getPlaceholders()} method that returns the full
     * placeholder objects from the master list (via {@link #setPlaceholders(List)}),
     * not just the lightweight event payload.
     */
    public static abstract class AbstractMultiPlaceholderEvent
            extends ComponentEvent<EnhancedRichTextEditor> {

        private final List<Placeholder> placeholders = new ArrayList<>();

        public AbstractMultiPlaceholderEvent(EnhancedRichTextEditor source,
                boolean fromClient, JsonNode placeholderJson) {
            super(source, fromClient);
            if (placeholderJson != null && placeholderJson.isArray()) {
                for (JsonNode node : placeholderJson) {
                    JsonNode pNode = node;
                    int idx = -1;
                    if (node.has("placeholder") && node.has("index")) {
                        idx = node.get("index").asInt();
                        pNode = node.get("placeholder");
                    }
                    Placeholder p = new Placeholder(pNode);
                    if (idx != -1) p.setIndex(idx);
                    placeholders.add(p);
                }
            }
        }

        /**
         * Returns the placeholders involved in this event.
         * <p>
         * The returned placeholders are looked up from the master list set via
         * {@link EnhancedRichTextEditor#setPlaceholders(List)}, so they contain
         * all configured properties (text, tag, format, altFormat), not just the
         * minimal data sent from the client. The {@code index} property is set
         * to the placeholder's position in the editor's delta.
         *
         * @return the list of placeholders, never null
         */
        public List<Placeholder> getPlaceholders() {
            List<Placeholder> actual = new ArrayList<>();
            for (Placeholder p : placeholders) {
                Placeholder found = ((EnhancedRichTextEditor) source)
                        .getPlaceholder(p);
                if (found != null) {
                    if (p.getIndex() != -1) found.setIndex(p.getIndex());
                    actual.add(found);
                }
            }
            return actual;
        }
    }

    /**
     * Event fired when the placeholder toolbar button is clicked.
     * <p>
     * This event is fired before the placeholder dialog opens. You can use it
     * to programmatically insert a placeholder without showing the dialog, or
     * to customize the dialog behavior.
     */
    @DomEvent("placeholder-button-click")
    public static class PlaceholderButtonClickedEvent
            extends ComponentEvent<EnhancedRichTextEditor> {

        private final int position;

        public PlaceholderButtonClickedEvent(EnhancedRichTextEditor source,
                boolean fromClient,
                @EventData("event.preventDefault()") Object ignored,
                @EventData("event.detail.position") int position) {
            super(source, fromClient);
            this.position = position;
        }

        /**
         * Returns the cursor position where the placeholder would be inserted.
         *
         * @return the zero-based index in the editor's delta
         */
        public int getPosition() {
            return position;
        }

        /**
         * Inserts a placeholder at the current cursor position.
         * <p>
         * This bypasses the placeholder dialog and inserts the placeholder directly.
         * The placeholder must be defined in the master list via
         * {@link EnhancedRichTextEditor#setPlaceholders(List)}.
         *
         * @param placeholder the placeholder to insert
         */
        public void insert(Placeholder placeholder) {
            insert(placeholder, position);
        }

        /**
         * Inserts a placeholder at the specified position.
         * <p>
         * This bypasses the placeholder dialog and inserts the placeholder directly.
         * The placeholder must be defined in the master list via
         * {@link EnhancedRichTextEditor#setPlaceholders(List)}.
         *
         * @param placeholder the placeholder to insert
         * @param position the zero-based index in the editor's delta where to insert
         */
        public void insert(Placeholder placeholder, int position) {
            Objects.requireNonNull(placeholder, "Placeholder cannot be null");
            EnhancedRichTextEditor s = (EnhancedRichTextEditor) source;
            s.getElement().executeJs(
                    "this._confirmInsertPlaceholders([{$0,index: $1}])",
                    placeholder.toJson(), position);
        }
    }

    /**
     * Event fired before placeholders are inserted into the editor.
     * <p>
     * This is a cancellable event. The insertion will only proceed if
     * {@link #insert()} is called. If {@code insert()} is not called, the
     * placeholders are NOT inserted and no {@link PlaceholderInsertedEvent}
     * will be fired.
     * <p>
     * Use this event to validate placeholders before insertion, modify them,
     * or cancel the operation entirely.
     */
    @DomEvent("placeholder-before-insert")
    public static class PlaceholderBeforeInsertEvent
            extends AbstractMultiPlaceholderEvent {

        public PlaceholderBeforeInsertEvent(EnhancedRichTextEditor source,
                boolean fromClient,
                @EventData("event.preventDefault()") Object ignored,
                @EventData("event.detail") ObjectNode detail) {
            super(source, fromClient, detail.get("placeholders"));
        }

        /**
         * Confirms insertion of the placeholders.
         * <p>
         * <b>IMPORTANT:</b> If this method is not called, the placeholders will
         * NOT be inserted into the editor. This allows validation or cancellation
         * of the insertion operation.
         * <p>
         * Example:
         * <pre>{@code
         * editor.addPlaceholderBeforeInsertListener(e -> {
         *     if (isValid(e.getPlaceholders())) {
         *         e.insert(); // Proceed with insertion
         *     }
         *     // Otherwise, do nothing to cancel
         * });
         * }</pre>
         */
        public void insert() {
            EnhancedRichTextEditor s = (EnhancedRichTextEditor) source;
            s.getElement().executeJs("this._confirmInsertPlaceholders()");
        }
    }

    /**
     * Event fired after placeholders have been successfully inserted.
     * <p>
     * This is a notification event fired after the insertion is complete.
     * Use {@link PlaceholderBeforeInsertEvent} if you need to prevent or
     * modify the insertion.
     */
    @DomEvent("placeholder-insert")
    public static class PlaceholderInsertedEvent
            extends AbstractMultiPlaceholderEvent {

        public PlaceholderInsertedEvent(EnhancedRichTextEditor source,
                boolean fromClient,
                @EventData("event.detail") ObjectNode detail) {
            super(source, fromClient, detail.get("placeholders"));
        }
    }

    /**
     * Event fired before placeholders are removed from the editor.
     * <p>
     * This is a cancellable event. The removal will only proceed if
     * {@link #remove()} is called. If {@code remove()} is not called, the
     * placeholders are NOT removed and no {@link PlaceholderRemovedEvent}
     * will be fired.
     * <p>
     * Use this event to prevent deletion of protected placeholders or to
     * prompt the user for confirmation.
     */
    @DomEvent("placeholder-before-delete")
    public static class PlaceholderBeforeRemoveEvent
            extends AbstractMultiPlaceholderEvent {

        public PlaceholderBeforeRemoveEvent(EnhancedRichTextEditor source,
                boolean fromClient,
                @EventData("event.preventDefault()") Object ignored,
                @EventData("event.detail") ObjectNode detail) {
            super(source, fromClient, detail.get("placeholders"));
        }

        /**
         * Confirms removal of the placeholders.
         * <p>
         * <b>IMPORTANT:</b> If this method is not called, the placeholders will
         * NOT be removed from the editor. This allows preventing deletion of
         * protected placeholders.
         * <p>
         * Example:
         * <pre>{@code
         * editor.addPlaceholderBeforeRemoveListener(e -> {
         *     if (!isProtected(e.getPlaceholders())) {
         *         e.remove(); // Allow removal
         *     }
         *     // Otherwise, do nothing to cancel
         * });
         * }</pre>
         */
        public void remove() {
            EnhancedRichTextEditor s = (EnhancedRichTextEditor) source;
            s.getElement().executeJs("this._confirmRemovePlaceholders()");
        }
    }

    /**
     * Event fired after placeholders have been successfully removed.
     * <p>
     * This is a notification event fired after the removal is complete.
     * Use {@link PlaceholderBeforeRemoveEvent} if you need to prevent removal.
     */
    @DomEvent("placeholder-delete")
    public static class PlaceholderRemovedEvent
            extends AbstractMultiPlaceholderEvent {

        public PlaceholderRemovedEvent(EnhancedRichTextEditor source,
                boolean fromClient,
                @EventData("event.detail") ObjectNode detail) {
            super(source, fromClient, detail.get("placeholders"));
        }
    }

    /**
     * Event fired when a placeholder is selected in the editor.
     * <p>
     * This event is fired when the user clicks on a placeholder or navigates
     * to it via keyboard (arrow keys, Tab). The cursor is positioned at the
     * placeholder.
     */
    @DomEvent("placeholder-select")
    public static class PlaceholderSelectedEvent
            extends AbstractMultiPlaceholderEvent {

        public PlaceholderSelectedEvent(EnhancedRichTextEditor source,
                boolean fromClient,
                @EventData("event.detail") ObjectNode detail) {
            super(source, fromClient, detail.get("placeholders"));
        }
    }

    /**
     * Event fired when a placeholder loses selection.
     * <p>
     * This event is fired when the cursor moves away from a placeholder
     * (e.g., user clicks elsewhere, presses an arrow key, or types text).
     * Useful for cleaning up UI state related to placeholder selection.
     */
    @DomEvent("placeholder-leave")
    public static class PlaceholderLeaveEvent
            extends ComponentEvent<EnhancedRichTextEditor> {

        public PlaceholderLeaveEvent(EnhancedRichTextEditor source,
                boolean fromClient) {
            super(source, fromClient);
        }
    }

    /**
     * Event fired when a placeholder's appearance changes.
     * <p>
     * This event is fired when a placeholder switches between its default
     * format and altFormat, based on the regex pattern set via
     * {@link EnhancedRichTextEditor#setPlaceholderAltAppearancePattern(String)}.
     * <p>
     * For example, if the pattern is "Dear\\s+$" and the user types "Dear ",
     * any following placeholder will switch to its altFormat. When the user
     * deletes "Dear ", the placeholder switches back to its default format.
     */
    @DomEvent("placeholder-appearance-change")
    public static class PlaceholderAppearanceChangedEvent
            extends ComponentEvent<EnhancedRichTextEditor> {

        private final Boolean altAppearance;
        private final String appearanceLabel;

        public PlaceholderAppearanceChangedEvent(
                EnhancedRichTextEditor source, boolean fromClient,
                @EventData("event.detail") ObjectNode detail) {
            super(source, fromClient);
            altAppearance = detail.has("altAppearance")
                    ? detail.get("altAppearance").asBoolean() : null;
            appearanceLabel = detail.has("appearanceLabel")
                    ? detail.get("appearanceLabel").asText() : null;
        }

        /**
         * Returns whether the placeholder is using its alternative appearance.
         * <p>
         * {@code true} if the placeholder is using its altFormat, {@code false}
         * if using the default format, {@code null} if appearance state is unknown.
         *
         * @return the alternative appearance state, or null if unknown
         */
        public Boolean getAltAppearance() {
            return altAppearance;
        }

        /**
         * Returns the appearance label (if set on the placeholder).
         * <p>
         * This is a custom label that can be set on placeholders to identify
         * different appearance states. Returns {@code null} if no label is set.
         *
         * @return the appearance label, or null if not set
         */
        public String getAppearanceLabel() {
            return appearanceLabel;
        }
    }

    // ========================================================================
    // Programmatic Text Insertion (Feature 14)
    // ========================================================================

    /**
     * Asynchronously retrieves the editor's text length.
     * <p>
     * The callback is invoked once the length is available from the browser.
     * Quill's internal trailing newline is excluded from the count to match
     * user expectations (e.g., "Hello" returns 5, not 6).
     * </p>
     * <p>
     * <strong>Breaking Change from V24:</strong> V24's synchronous API
     * ({@code int getTextLength()}) cannot be preserved in V25 due to
     * Vaadin's deadlock detection. This async callback pattern matches
     * Vaadin's {@code WebStorage.getItem()} API.
     * </p>
     *
     * @param callback Consumer that receives the text length (never null)
     * @throws NullPointerException if callback is null
     */
    public void getTextLength(SerializableConsumer<Integer> callback) {
        Objects.requireNonNull(callback, "Callback cannot be null");
        getElement()
            .executeJs(
                "return Math.max(0, ($0._editor ? $0._editor.getLength() : 0) - 1)",
                getElement()
            )
            .then(Integer.class, callback::accept);
    }

    /**
     * Inserts text at the specified position.
     * <p>
     * Position is clamped to valid range [0, length-1] on the client side.
     * No insertion occurs if the editor is disabled.
     * </p>
     * <p>
     * <strong>Behavior Change from V24:</strong> V24 rejected out-of-bounds
     * positions silently. V25 clamps to nearest valid position.
     * </p>
     *
     * @param text Text to be inserted (not null)
     * @param position Position where text should be inserted (0-based index)
     * @throws NullPointerException if text is null
     */
    public void addText(String text, int position) {
        Objects.requireNonNull(text, "Text cannot be null");
        getElement().executeJs(
            "if ($0._editor && $0._editor.isEnabled()) {" +
            "  const len = Math.max(1, $0._editor.getLength());" +
            "  const pos = Math.max(0, Math.min($1, len - 1));" +
            "  $0._editor.insertText(pos, $2);" +
            "}",
            getElement(), position, text
        );
    }

    /**
     * Inserts text at the current cursor position.
     * <p>
     * If no selection exists, editor is not focused, or editor is disabled,
     * no insertion occurs.
     * </p>
     *
     * @param text Text to be inserted (not null)
     * @throws NullPointerException if text is null
     */
    public void addText(String text) {
        Objects.requireNonNull(text, "Text cannot be null");
        getElement().executeJs(
            "if ($0._editor && $0._editor.isEnabled() && $0._editor.getSelection()) {" +
            "  $0._editor.insertText($0._editor.getSelection().index, $1);" +
            "}",
            getElement(), text
        );
    }

    // ========================================================================
    // I18n
    // ========================================================================

    /**
     * Extended i18n for ERTE — adds labels for ERTE-specific toolbar
     * buttons and the placeholder dialog.
     * <p>
     * Inherits all standard RTE 2 labels. Only set ERTE-specific fields
     * if you need to translate them; defaults are English.
     * <p>
     * Usage:
     * <pre>
     * editor.setI18n(new EnhancedRichTextEditorI18n()
     *     .setBold("Fett")
     *     .setReadonly("Schreibschutz")
     *     .setPlaceholder("Platzhalter"));
     * </pre>
     */
    public static class EnhancedRichTextEditorI18n
            extends com.vaadin.flow.component.richtexteditor
                    .RichTextEditor.RichTextEditorI18n
            implements Serializable {

        private String readonly;
        private String whitespace;
        private String placeholder;
        private String placeholderAppearance;
        private String placeholderDialogTitle;
        private String placeholderComboBoxLabel;
        private String placeholderAppearanceLabel1;
        private String placeholderAppearanceLabel2;
        private String alignJustify;

        public String getReadonly() {
            return readonly;
        }

        public EnhancedRichTextEditorI18n setReadonly(String readonly) {
            this.readonly = readonly;
            return this;
        }

        public String getWhitespace() {
            return whitespace;
        }

        public EnhancedRichTextEditorI18n setWhitespace(String whitespace) {
            this.whitespace = whitespace;
            return this;
        }

        public String getPlaceholder() {
            return placeholder;
        }

        public EnhancedRichTextEditorI18n setPlaceholder(
                String placeholder) {
            this.placeholder = placeholder;
            return this;
        }

        public String getPlaceholderAppearance() {
            return placeholderAppearance;
        }

        public EnhancedRichTextEditorI18n setPlaceholderAppearance(
                String placeholderAppearance) {
            this.placeholderAppearance = placeholderAppearance;
            return this;
        }

        public String getPlaceholderDialogTitle() {
            return placeholderDialogTitle;
        }

        public EnhancedRichTextEditorI18n setPlaceholderDialogTitle(
                String placeholderDialogTitle) {
            this.placeholderDialogTitle = placeholderDialogTitle;
            return this;
        }

        public String getPlaceholderComboBoxLabel() {
            return placeholderComboBoxLabel;
        }

        public EnhancedRichTextEditorI18n setPlaceholderComboBoxLabel(
                String placeholderComboBoxLabel) {
            this.placeholderComboBoxLabel = placeholderComboBoxLabel;
            return this;
        }

        public String getPlaceholderAppearanceLabel1() {
            return placeholderAppearanceLabel1;
        }

        public EnhancedRichTextEditorI18n setPlaceholderAppearanceLabel1(
                String placeholderAppearanceLabel1) {
            this.placeholderAppearanceLabel1 = placeholderAppearanceLabel1;
            return this;
        }

        public String getPlaceholderAppearanceLabel2() {
            return placeholderAppearanceLabel2;
        }

        public EnhancedRichTextEditorI18n setPlaceholderAppearanceLabel2(
                String placeholderAppearanceLabel2) {
            this.placeholderAppearanceLabel2 = placeholderAppearanceLabel2;
            return this;
        }

        /**
         * Gets the label for the justify alignment button.
         *
         * @return the label, or null if not set
         */
        public String getAlignJustify() {
            return alignJustify;
        }

        /**
         * Sets the label for the justify alignment button. RTE 2 only provides
         * left, center, right alignment buttons; the justify button is
         * ERTE-specific.
         *
         * @param alignJustify the label
         * @return this instance for fluent chaining
         */
        public EnhancedRichTextEditorI18n setAlignJustify(
                String alignJustify) {
            this.alignJustify = alignJustify;
            return this;
        }

        // Covariant return type overrides for fluent chaining

        @Override
        public EnhancedRichTextEditorI18n setUndo(String undo) {
            super.setUndo(undo);
            return this;
        }

        @Override
        public EnhancedRichTextEditorI18n setRedo(String redo) {
            super.setRedo(redo);
            return this;
        }

        @Override
        public EnhancedRichTextEditorI18n setBold(String bold) {
            super.setBold(bold);
            return this;
        }

        @Override
        public EnhancedRichTextEditorI18n setItalic(String italic) {
            super.setItalic(italic);
            return this;
        }

        @Override
        public EnhancedRichTextEditorI18n setUnderline(String underline) {
            super.setUnderline(underline);
            return this;
        }

        @Override
        public EnhancedRichTextEditorI18n setStrike(String strike) {
            super.setStrike(strike);
            return this;
        }

        @Override
        public EnhancedRichTextEditorI18n setH1(String h1) {
            super.setH1(h1);
            return this;
        }

        @Override
        public EnhancedRichTextEditorI18n setH2(String h2) {
            super.setH2(h2);
            return this;
        }

        @Override
        public EnhancedRichTextEditorI18n setH3(String h3) {
            super.setH3(h3);
            return this;
        }

        @Override
        public EnhancedRichTextEditorI18n setColor(String color) {
            super.setColor(color);
            return this;
        }

        @Override
        public EnhancedRichTextEditorI18n setBackground(
                String background) {
            super.setBackground(background);
            return this;
        }

        @Override
        public EnhancedRichTextEditorI18n setSubscript(String subscript) {
            super.setSubscript(subscript);
            return this;
        }

        @Override
        public EnhancedRichTextEditorI18n setSuperscript(
                String superscript) {
            super.setSuperscript(superscript);
            return this;
        }

        @Override
        public EnhancedRichTextEditorI18n setListOrdered(
                String listOrdered) {
            super.setListOrdered(listOrdered);
            return this;
        }

        @Override
        public EnhancedRichTextEditorI18n setListBullet(
                String listBullet) {
            super.setListBullet(listBullet);
            return this;
        }

        @Override
        public EnhancedRichTextEditorI18n setOutdent(String outdent) {
            super.setOutdent(outdent);
            return this;
        }

        @Override
        public EnhancedRichTextEditorI18n setIndent(String indent) {
            super.setIndent(indent);
            return this;
        }

        @Override
        public EnhancedRichTextEditorI18n setAlignLeft(String alignLeft) {
            super.setAlignLeft(alignLeft);
            return this;
        }

        @Override
        public EnhancedRichTextEditorI18n setAlignCenter(
                String alignCenter) {
            super.setAlignCenter(alignCenter);
            return this;
        }

        @Override
        public EnhancedRichTextEditorI18n setAlignRight(
                String alignRight) {
            super.setAlignRight(alignRight);
            return this;
        }

        @Override
        public EnhancedRichTextEditorI18n setImage(String image) {
            super.setImage(image);
            return this;
        }

        @Override
        public EnhancedRichTextEditorI18n setLink(String link) {
            super.setLink(link);
            return this;
        }

        @Override
        public EnhancedRichTextEditorI18n setBlockquote(
                String blockquote) {
            super.setBlockquote(blockquote);
            return this;
        }

        @Override
        public EnhancedRichTextEditorI18n setCodeBlock(String codeBlock) {
            super.setCodeBlock(codeBlock);
            return this;
        }

        @Override
        public EnhancedRichTextEditorI18n setClean(String clean) {
            super.setClean(clean);
            return this;
        }
    }
}
