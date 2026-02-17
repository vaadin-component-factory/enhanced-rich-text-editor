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

import com.vaadin.componentfactory.EnhancedRichTextEditorVariant;
import com.vaadin.componentfactory.Placeholder;
import com.vaadin.componentfactory.SlotUtil;
import com.vaadin.componentfactory.TabStop;
import com.vaadin.componentfactory.toolbar.ToolbarSlot;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.DomEvent;
import com.vaadin.flow.component.EventData;
import com.vaadin.flow.component.Synchronize;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.shared.Registration;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Enhanced Rich Text Editor for Vaadin 25.
 * <p>
 * Extends Vaadin's built-in {@link RichTextEditor} (RTE 2, Quill 2.0.3) with
 * additional features: tabstops, read-only sections, placeholders,
 * custom toolbar slots, rulers, whitespace indicators, and more.
 * <p>
 * Uses a custom tag ({@code <vcf-enhanced-rich-text-editor>}) to avoid conflicts
 * with the standard RTE. The JS module extends the RTE 2 Lit class via
 * {@code render()} override (toolbar) and {@code ready()} hook (Quill access).
 * <p>
 * Package: {@code com.vaadin.flow.component.richtexteditor} -- same as RTE 2,
 * giving access to package-private methods ({@code runBeforeClientResponse},
 * {@code sanitize}, etc.)
 * <p>
 * Value format: HTML-primary (same as RTE 2). Use {@link #asDelta()} for Delta
 * format access.
 *
 * @author Vaadin Ltd
 */
@Tag("vcf-enhanced-rich-text-editor")
@JsModule("./src/vcf-enhanced-rich-text-editor.js")
public class EnhancedRichTextEditor extends RichTextEditor {

    private EnhancedRichTextEditorI18n enhancedI18n;
    private Map<ToolbarButton, Boolean> toolbarButtonsVisibility;
    private Collection<Placeholder> placeholders;
    private boolean pendingHtmlUpdate;

    // ================================================================
    // Constructors
    // ================================================================

    /**
     * Constructs an empty {@code EnhancedRichTextEditor}.
     */
    public EnhancedRichTextEditor() {
        super();
    }

    /**
     * Constructs a {@code EnhancedRichTextEditor} with the initial value.
     *
     * @param initialValue
     *            the initial value (HTML format)
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
     * @param listener
     *            the value change listener
     * @see #addValueChangeListener(com.vaadin.flow.component.HasValue.ValueChangeListener)
     */
    public EnhancedRichTextEditor(
            ValueChangeListener<? super com.vaadin.flow.component.AbstractField.ComponentValueChangeEvent<RichTextEditor, String>> listener) {
        this();
        addValueChangeListener(listener);
    }

    /**
     * Constructs an empty {@code EnhancedRichTextEditor} with a value change
     * listener and an initial value.
     *
     * @param initialValue
     *            the initial value (HTML format)
     * @param listener
     *            the value change listener
     * @see #setValue(String)
     * @see #addValueChangeListener(com.vaadin.flow.component.HasValue.ValueChangeListener)
     */
    public EnhancedRichTextEditor(String initialValue,
            ValueChangeListener<? super com.vaadin.flow.component.AbstractField.ComponentValueChangeEvent<RichTextEditor, String>> listener) {
        this();
        setValue(initialValue);
        addValueChangeListener(listener);
    }

    // ================================================================
    // Theme Variants
    // ================================================================

    /**
     * Adds theme variants to the component.
     *
     * @param variants
     *            theme variants to add
     */
    public void addThemeVariants(EnhancedRichTextEditorVariant... variants) {
        getThemeNames().addAll(Stream.of(variants)
                .map(EnhancedRichTextEditorVariant::getVariantName)
                .collect(Collectors.toList()));
    }

    /**
     * Removes theme variants from the component.
     *
     * @param variants
     *            theme variants to remove
     */
    public void removeThemeVariants(EnhancedRichTextEditorVariant... variants) {
        getThemeNames().removeAll(Stream.of(variants)
                .map(EnhancedRichTextEditorVariant::getVariantName)
                .collect(Collectors.toList()));
    }

    // ================================================================
    // Value handling (HTML sanitization override)
    // ================================================================

    /**
     * Returns the current editor value as sanitized HTML.
     * <p>
     * Overrides the parent's {@code getValue()} to use ERTE's extended
     * sanitizer that preserves ERTE-specific CSS classes (ql-tab,
     * ql-soft-break, ql-readonly, ql-placeholder) on span elements.
     * The parent's sanitizer strips all class attributes.
     *
     * @return the sanitized HTML value with ERTE classes preserved
     */
    @Override
    public String getValue() {
        String raw = getHtmlValueString();
        if (raw == null || raw.isEmpty()) {
            return getEmptyValue();
        }
        return sanitizeErte(raw);
    }

    /**
     * Sends the HTML value to the client, using ERTE's extended sanitizer
     * instead of the parent's default sanitizer.
     * <p>
     * The parent's {@code modelToPresentation} strips ERTE-specific CSS
     * classes (ql-tab, ql-placeholder, etc.) which prevents Quill from
     * reconstructing ERTE-specific blots from HTML. This override ensures
     * those classes are preserved in the round-trip.
     *
     * @param newPresentationValue the value to send to the client
     */
    @Override
    protected void setPresentationValue(String newPresentationValue) {
        String presentationValue = newPresentationValue == null
                ? "" : sanitizeErte(newPresentationValue);
        getElement().setProperty("htmlValue", presentationValue);
        if (!pendingHtmlUpdate) {
            pendingHtmlUpdate = true;
            runBeforeClientResponse(ui -> {
                getElement().callJsFunction("dangerouslySetHtmlValue",
                        getElement().getProperty("htmlValue"));
                pendingHtmlUpdate = false;
            });
        }
    }

    /**
     * Value of the editor presented as sanitized HTML string.
     * <p>
     * Uses ERTE's extended sanitizer that whitelists ERTE-specific CSS classes
     * (ql-tab, ql-soft-break, ql-readonly, ql-placeholder) on span elements.
     *
     * @return the sanitized HTML value
     * @deprecated Use {@link #getValue()} instead. This method is provided
     *             for backwards compatibility with ERTE v1.
     */
    @Deprecated
    public String getHtmlValue() {
        return getValue();
    }

    /**
     * Returns the raw HTML value string from the web component without
     * sanitization.
     *
     * @return the raw {@code htmlValue} property from the webcomponent
     */
    @Synchronize(property = "htmlValue", value = "html-value-changed")
    protected String getHtmlValueString() {
        return getElement().getProperty("htmlValue");
    }

    /**
     * Return the length of the content stripped as text.
     *
     * @return The length of the text content.
     */
    public int getTextLength() {
        String rawHtml = getHtmlValueString();
        if (rawHtml == null) {
            return 0;
        }
        return org.jsoup.Jsoup.clean(rawHtml,
                org.jsoup.safety.Safelist.none()).length();
    }

    // ================================================================
    // ERTE-specific HTML sanitizer
    // ================================================================

    /**
     * Sanitizes HTML with ERTE-specific whitelist additions.
     * <p>
     * Extends the base RTE 2 sanitizer to allow:
     * <ul>
     *   <li>{@code span} elements with ERTE-specific class values
     *       (ql-tab, ql-soft-break, ql-readonly, ql-placeholder)</li>
     *   <li>{@code contenteditable="false"} on span elements</li>
     *   <li>Table elements ({@code table}, {@code tr}, {@code td},
     *       {@code colgroup}, {@code col}) with table metadata attributes</li>
     * </ul>
     *
     * @param html the HTML string to sanitize
     * @return the sanitized HTML string
     */
    String sanitizeErte(String html) {
        var settings = new org.jsoup.nodes.Document.OutputSettings();
        settings.prettyPrint(false);
        String cleaned = org.jsoup.Jsoup.clean(html, "",
                org.jsoup.safety.Safelist.basic()
                        .addTags("img", "h1", "h2", "h3", "s", "span", "br",
                                "table", "tr", "td", "colgroup", "col")
                        .addAttributes("img", "align", "alt", "height", "src",
                                "title", "width")
                        .addAttributes("span", "class", "contenteditable")
                        .addAttributes("table", "table_id", "class")
                        .addAttributes("tr", "row_id")
                        .addAttributes("td", "table_id", "row_id", "cell_id",
                                "merge_id", "colspan", "rowspan", "class")
                        .addAttributes(":all", "style")
                        .addProtocols("img", "src", "data"),
                settings);

        // Post-sanitization: restrict span class values to known safe classes
        // and contenteditable to only "false", to prevent CSS injection and
        // unintended editing behavior in contexts where HTML output is reused.
        org.jsoup.nodes.Document doc = org.jsoup.Jsoup.parse(cleaned);
        doc.outputSettings().prettyPrint(false);
        java.util.Set<String> allowedClasses = java.util.Set.of(
                "ql-tab", "ql-soft-break", "ql-readonly", "ql-placeholder");
        for (org.jsoup.nodes.Element span : doc.select("span[class]")) {
            String[] classes = span.attr("class").split("\\s+");
            StringBuilder filtered = new StringBuilder();
            for (String cls : classes) {
                if (allowedClasses.contains(cls)) {
                    if (filtered.length() > 0) filtered.append(' ');
                    filtered.append(cls);
                }
            }
            if (filtered.length() > 0) {
                span.attr("class", filtered.toString());
            } else {
                span.removeAttr("class");
            }
        }
        for (org.jsoup.nodes.Element span : doc.select("span[contenteditable]")) {
            if (!"false".equals(span.attr("contenteditable"))) {
                span.removeAttr("contenteditable");
            }
        }
        return doc.body().html();
    }

    // ================================================================
    // TabStops
    // ================================================================

    /**
     * Sets the tab stops for the editor ruler and tab alignment.
     *
     * @param tabStops
     *            list of tab stop definitions
     */
    public void setTabStops(List<TabStop> tabStops) {
        ArrayNode arrayTabStops = JacksonUtils.createArrayNode();

        for (TabStop tab : tabStops) {
            ObjectNode obj = JacksonUtils.createObjectNode();
            obj.put("direction", tab.getDirection().name().toLowerCase());
            obj.put("position", tab.getPosition());
            arrayTabStops.add(obj);
        }

        getElement().setPropertyJson("tabStops", arrayTabStops);
    }

    /**
     * Gets the current tab stops.
     *
     * @return list of tab stop definitions
     */
    public List<TabStop> getTabStops() {
        List<TabStop> tabStops = new ArrayList<>();
        JsonNode rawNode = (JsonNode) getElement()
                .getPropertyRaw("tabStops");

        if (rawNode == null || !rawNode.isArray()) {
            return tabStops;
        }

        ArrayNode rawArray = (ArrayNode) rawNode;
        for (int i = 0; i < rawArray.size(); i++) {
            ObjectNode obj = (ObjectNode) rawArray.get(i);
            try {
                TabStop tab = new TabStop(
                        TabStop.Direction.valueOf(
                                obj.get("direction").asText().toUpperCase()),
                        obj.get("position").asDouble());
                tabStops.add(tab);
            } catch (IllegalArgumentException e) {
                // Skip invalid tab stops
            }
        }

        return tabStops;
    }

    // ================================================================
    // Rulers
    // ================================================================

    /**
     * Sets whether the horizontal and vertical rulers are hidden.
     *
     * @param noRulers
     *            {@code true} to hide rulers, {@code false} to show them
     */
    public void setNoRulers(boolean noRulers) {
        getElement().setProperty("noRulers", noRulers);
    }

    /**
     * Returns whether rulers are hidden.
     *
     * @return {@code true} if rulers are hidden
     */
    public boolean isNoRulers() {
        return getElement().getProperty("noRulers", false);
    }

    // ================================================================
    // Whitespace indicators
    // ================================================================

    /**
     * Sets whether whitespace indicators are shown in the editor.
     * When true, special characters are displayed: tab arrow, soft-break,
     * paragraph mark, auto-wrap indicator.
     *
     * @param show {@code true} to show whitespace indicators
     */
    public void setShowWhitespace(boolean show) {
        getElement().setProperty("showWhitespace", show);
    }

    /**
     * Returns whether whitespace indicators are currently shown.
     *
     * @return {@code true} if whitespace indicators are visible
     */
    public boolean isShowWhitespace() {
        return getElement().getProperty("showWhitespace", false);
    }

    // ================================================================
    // I18n (ERTE-specific extension)
    // ================================================================

    /**
     * Gets the ERTE-specific internationalization object previously set for
     * this component.
     * <p>
     * Note: updating the returned object will not update the component unless
     * set back using {@link #setEnhancedI18n(EnhancedRichTextEditorI18n)}.
     *
     * @return the ERTE i18n object, or {@code null} if not set
     */
    public EnhancedRichTextEditorI18n getEnhancedI18n() {
        return enhancedI18n;
    }

    /**
     * Sets the ERTE-specific internationalization properties for this
     * component. This includes all standard RTE 2 i18n properties plus
     * ERTE-specific ones (readonly, placeholder, etc.).
     * <p>
     * Delegates to the parent
     * {@link RichTextEditor#setI18n(RichTextEditorI18n)} which serializes
     * the full bean (including ERTE-specific fields) via
     * {@code JacksonUtils.beanToJson()} and sets it as the {@code i18n}
     * JSON property. The {@code I18nMixin} on the JS side merges all keys
     * (including ERTE-specific extras) into {@code __effectiveI18n}.
     *
     * @param i18n
     *            the internationalized properties, not {@code null}
     */
    public void setEnhancedI18n(EnhancedRichTextEditorI18n i18n) {
        Objects.requireNonNull(i18n,
                "The I18N properties object should not be null");
        this.enhancedI18n = i18n;

        // Delegate to parent -- setI18n uses JacksonUtils.beanToJson(i18n)
        // which serializes ALL getters, including ERTE-specific fields
        // (alignJustify, deindent, readonly, placeholder*, etc.).
        // The I18nMixin on the JS side merges these into __effectiveI18n,
        // preserving extra keys beyond the standard RTE 2 defaults.
        super.setI18n(i18n);
    }

    // ================================================================
    // Toolbar button visibility
    // ================================================================

    /**
     * Gets the current toolbar button visibility map.
     *
     * @return the visibility map, or {@code null} if not set
     */
    public Map<ToolbarButton, Boolean> getToolbarButtonsVisibility() {
        return toolbarButtonsVisibility;
    }

    /**
     * Set which toolbar buttons are visible.
     *
     * @param toolbarButtonsVisibility
     *            Map of button and boolean value. Boolean value {@code false}
     *            associated with the button means that button will be hidden.
     */
    public void setToolbarButtonsVisibility(
            Map<ToolbarButton, Boolean> toolbarButtonsVisibility) {
        this.toolbarButtonsVisibility = toolbarButtonsVisibility;
        ObjectNode json = JacksonUtils.createObjectNode();
        if (toolbarButtonsVisibility != null) {
            for (Map.Entry<ToolbarButton, Boolean> entry :
                    toolbarButtonsVisibility.entrySet()) {
                json.put(entry.getKey().getButtonName(),
                        entry.getValue());
            }
        }
        getElement().setPropertyJson("toolbarButtons", json);
    }

    // ================================================================
    // Placeholders
    // ================================================================

    /**
     * Set placeholders shown in the Placeholder drop down menu.
     *
     * @param placeholders
     *            Collection of Placeholder objects
     */
    public void setPlaceholders(Collection<Placeholder> placeholders) {
        Objects.requireNonNull(placeholders, "placeholders cannot be null");
        ArrayNode jsonArray = JacksonUtils.createArrayNode();

        for (Placeholder placeholder : placeholders) {
            jsonArray.add(placeholder.toJson());
        }

        this.placeholders = placeholders;
        getElement().setPropertyJson("placeholders", jsonArray);
    }

    /**
     * Gets the current placeholders.
     *
     * @return collection of Placeholder objects
     */
    public Collection<Placeholder> getPlaceholders() {
        ArrayList<Placeholder> result = new ArrayList<>();
        JsonNode rawNode = (JsonNode) getElement()
                .getPropertyRaw("placeholders");

        if (rawNode == null || !rawNode.isArray()) {
            return result;
        }

        ArrayNode rawArray = (ArrayNode) rawNode;
        for (int i = 0; i < rawArray.size(); i++) {
            ObjectNode obj = (ObjectNode) rawArray.get(i);
            try {
                Placeholder placeholder = new Placeholder(obj);
                result.add(placeholder);
            } catch (IllegalArgumentException e) {
                // Skip invalid placeholders
            }
        }

        return result;
    }

    /**
     * Sets the pattern for placeholder alternative appearance.
     *
     * @param pattern the regex pattern
     */
    public void setPlaceholderAltAppearancePattern(String pattern) {
        getElement().setProperty("placeholderAltAppearancePattern", pattern);
    }

    /**
     * Gets the pattern for placeholder alternative appearance.
     *
     * @return the regex pattern
     */
    public String getPlaceholderAltAppearancePattern() {
        return getElement().getProperty("placeholderAltAppearancePattern");
    }

    /**
     * Sets whether placeholders use their alternative appearance.
     *
     * @param altAppearance {@code true} for alternative appearance
     */
    public void setPlaceholderAltAppearance(boolean altAppearance) {
        getElement().setProperty("placeholderAltAppearance", altAppearance);
    }

    /**
     * Returns whether placeholders use their alternative appearance.
     *
     * @return {@code true} if alternative appearance is active
     */
    @Synchronize(property = "placeholderAltAppearance", value = "placeholder-appearance-change")
    public boolean isPlaceholderAltAppearance() {
        return getElement().getProperty("placeholderAltAppearance", false);
    }

    /**
     * For internal use only. Return Placeholder from the master list matching
     * the given Placeholder by getText.
     *
     * @param placeholder
     *            The Placeholder to be searched.
     * @return A Placeholder, or {@code null} if not found
     */
    protected Placeholder getPlaceholder(Placeholder placeholder) {
        Objects.requireNonNull(placeholder, "Placeholder cannot be null");
        Objects.requireNonNull(placeholders,
                "getPlaceholder cannot be called before placeholders are set");
        return placeholders.stream()
                .filter(p -> p.getText().equals(placeholder.getText()))
                .findFirst().orElse(null);
    }

    // ================================================================
    // Text manipulation
    // ================================================================

    /**
     * Add a text to the position. Text will be added if position
     * is within 0 .. length of the current value of the text area.
     *
     * @param text Text to be inserted
     * @param position Position
     */
    public void addText(String text, int position) {
        Objects.requireNonNull(text, "Text can't be null");
        if (position >= 0 && position <= getTextLength()) {
            getElement().executeJs("$0._editor.insertText($1,$2)",
                    getElement(), position, text);
        }
    }

    /**
     * Add text to the caret position, when the focus is in the text area.
     *
     * @param text Text to be inserted
     */
    public void addText(String text) {
        Objects.requireNonNull(text, "Text can't be null");
        getElement().executeJs(
                "if ($0._editor.getSelection()) "
                + "$0._editor.insertText($0._editor.getSelection().index,$1)",
                getElement(), text);
    }

    // ================================================================
    // Custom toolbar components
    // ================================================================

    /**
     * Add a custom button to the toolbar.
     * This method does NOT apply any toolbar styling to the button,
     * but will keep it "Vaadin native".
     *
     * @param button A custom button to be added, not null
     * @deprecated use {@link #addCustomToolbarComponents(Component...)} instead
     */
    @Deprecated
    public void addCustomButton(Button button) {
        Objects.requireNonNull(button, "Button can't be null");
        addCustomToolbarComponents(button);
    }

    /**
     * A convenience method to add multiple custom buttons at one call.
     * This method does NOT apply any toolbar styling to the button,
     * but will keep it "Vaadin native".
     *
     * @param buttons Custom buttons to be added.
     * @deprecated use {@link #addCustomToolbarComponents(Component...)} instead
     */
    @Deprecated
    public void addCustomButtons(Button... buttons) {
        addCustomToolbarComponents(buttons);
    }

    /**
     * A convenience method to add multiple custom components at one call. Uses
     * the {@link ToolbarSlot#GROUP_CUSTOM}.
     *
     * @param components Custom components to be added.
     */
    public void addCustomToolbarComponents(Component... components) {
        addToolbarComponents(ToolbarSlot.GROUP_CUSTOM, components);
    }

    /**
     * A convenience method to add multiple custom components at one call. Uses
     * the {@link ToolbarSlot#GROUP_CUSTOM}. The index allows to define the
     * position of the newly added components relative to already existing ones.
     *
     * @param index index
     * @param components Custom components to be added.
     */
    public void addCustomToolbarComponentsAtIndex(int index,
            Component... components) {
        addToolbarComponentsAtIndex(ToolbarSlot.GROUP_CUSTOM, index,
                components);
    }

    /**
     * Adds the components to the toolbar slot. Appends the components to
     * existing ones.
     *
     * @param toolbarSlot slot to add the components to
     * @param components Components to be added
     */
    public void addToolbarComponents(ToolbarSlot toolbarSlot,
            Component... components) {
        Objects.requireNonNull(components);
        for (Component component : components) {
            Objects.requireNonNull(component);
            SlotUtil.addComponent(this, toolbarSlot.getSlotName(), component);
        }
    }

    /**
     * Adds the components to the toolbar slot. The index allows to define the
     * position of the newly added components relative to already existing ones.
     *
     * @param toolbarSlot slot to add the components to
     * @param index the position index within the slot
     * @param components Components to be added
     */
    public void addToolbarComponentsAtIndex(ToolbarSlot toolbarSlot, int index,
            Component... components) {
        Objects.requireNonNull(components);
        for (Component component : components) {
            Objects.requireNonNull(component);
            SlotUtil.addComponentAtIndex(this, toolbarSlot.getSlotName(),
                    component, index);
        }
    }

    /**
     * Returns a toolbar component with the given id from the toolbar slot. The
     * component must have been added using one of the
     * {@code addToolbarComponents} methods beforehand.
     *
     * @param toolbarSlot toolbar slot
     * @param id component id
     * @return component
     * @param <T> return type
     */
    @SuppressWarnings("unchecked")
    public <T extends Component> T getToolbarComponent(
            ToolbarSlot toolbarSlot, String id) {
        Objects.requireNonNull(id, "Id can't be null");
        return (T) SlotUtil.getComponent(this, toolbarSlot.getSlotName(), id);
    }

    /**
     * Remove the given component from the toolbar by id. The component must
     * have been added using one of the {@code addToolbarComponents} methods
     * beforehand.
     *
     * @param toolbarSlot toolbar slot
     * @param id component id
     */
    public void removeToolbarComponent(ToolbarSlot toolbarSlot, String id) {
        Objects.requireNonNull(id, "Id can't be null");
        SlotUtil.removeComponent(this, toolbarSlot.getSlotName(), id);
    }

    /**
     * Remove a custom component from the toolbar. The component must have been
     * added using one of the {@code addToolbarComponents} methods beforehand.
     *
     * @param toolbarSlot toolbar slot
     * @param component The component to be removed.
     */
    public void removeToolbarComponent(ToolbarSlot toolbarSlot,
            Component component) {
        Objects.requireNonNull(component, "Component can't be null");
        SlotUtil.removeComponent(this, toolbarSlot.getSlotName(), component);
    }

    /**
     * Get the custom button using its id.
     *
     * @param id Id as a string
     * @return A button
     * @deprecated use {@link #getToolbarComponent(ToolbarSlot, String)} instead
     *             with {@link ToolbarSlot#GROUP_CUSTOM}
     */
    @Deprecated
    public Button getCustomButton(String id) {
        Objects.requireNonNull(id, "Id can't be null");
        return SlotUtil.getButton(this, id);
    }

    /**
     * Remove the given button from the toolbar.
     *
     * @param id Id as a string.
     * @deprecated use {@link #removeToolbarComponent(ToolbarSlot, String)}
     *             instead with {@link ToolbarSlot#GROUP_CUSTOM}
     */
    @Deprecated
    public void removeCustomButton(String id) {
        Objects.requireNonNull(id, "Id can't be null");
        SlotUtil.removeButton(this, id);
    }

    /**
     * Remove a custom button from the toolbar.
     *
     * @param button The button to be removed.
     * @deprecated use {@link #removeToolbarComponent(ToolbarSlot, Component)}
     *             instead with {@link ToolbarSlot#GROUP_CUSTOM}
     */
    @Deprecated
    public void removeCustomButton(Button button) {
        Objects.requireNonNull(button, "Button can't be null");
        SlotUtil.removeButton(this, button);
    }

    // ================================================================
    // Keyboard shortcuts
    // ================================================================

    /**
     * Adds a custom shortcut to a specific toolbar standard button.
     *
     * @param toolbarButton The toolbar button to add the shortcut to.
     * @param keyCode The key code for the new shortcut.
     * @param shortKey True if modifier ctrl is part of the shortcut.
     * @param shiftKey True if modifier shift is part of the shortcut.
     * @param altKey True if modifier alt is part of the shortcut.
     */
    public void addStandardToolbarButtonShortcut(ToolbarButton toolbarButton,
            Number keyCode, Boolean shortKey, Boolean shiftKey,
            Boolean altKey) {
        getElement().executeJs(
                "$0.addStandardButtonBinding($1, $2, $3, $4, $5)",
                getElement(), toolbarButton.getButtonName(), keyCode, shortKey,
                shiftKey, altKey);
    }

    /**
     * Adds a custom shortcut to focus the editor toolbar.
     *
     * @param keyCode The key code for the new shortcut.
     * @param shortKey True if modifier ctrl is part of the shortcut.
     * @param shiftKey True if modifier shift is part of the shortcut.
     * @param altKey True if modifier alt is part of the shortcut.
     */
    public void addToolbarFocusShortcut(Number keyCode, Boolean shortKey,
            Boolean shiftKey, Boolean altKey) {
        getElement().executeJs(
                "$0.addToolbarFocusBinding($1, $2, $3, $4)",
                getElement(), keyCode, shortKey, shiftKey, altKey);
    }

    // ================================================================
    // Icon replacement
    // ================================================================

    /**
     * Allows to replace the icon of a standard {@link ToolbarButton toolbar
     * button}.
     *
     * @param toolbarButton toolbar button to replace icon
     * @param icon replacement icon
     */
    public void replaceStandardToolbarButtonIcon(ToolbarButton toolbarButton,
            Icon icon) {
        Objects.requireNonNull(icon, "Icon can't be null");
        SlotUtil.replaceStandardButtonIcon(this, icon,
                toolbarButton.getButtonName());
    }

    // ================================================================
    // Placeholder event listeners (public API)
    // ================================================================

    /**
     * Adds a listener for {@code PlaceholderBeforeRemoveEvent} events fired by
     * the webcomponent.
     *
     * @param listener the listener
     * @return a {@link Registration} for removing the event listener
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public Registration addPlaceholderBeforeRemoveListener(
            ComponentEventListener<PlaceholderBeforeRemoveEvent> listener) {
        return addListener(PlaceholderBeforeRemoveEvent.class,
                (ComponentEventListener) listener);
    }

    /**
     * Adds a listener for {@code PlaceholderBeforeInsertEvent} events fired by
     * the webcomponent.
     *
     * @param listener the listener
     * @return a {@link Registration} for removing the event listener
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public Registration addPlaceholderBeforeInsertListener(
            ComponentEventListener<PlaceholderBeforeInsertEvent> listener) {
        return addListener(PlaceholderBeforeInsertEvent.class,
                (ComponentEventListener) listener);
    }

    /**
     * Adds a listener for {@code PlaceholderRemovedEvent} events fired by the
     * webcomponent.
     *
     * @param listener the listener
     * @return a {@link Registration} for removing the event listener
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public Registration addPlaceholderRemovedListener(
            ComponentEventListener<PlaceholderRemovedEvent> listener) {
        return addListener(PlaceholderRemovedEvent.class,
                (ComponentEventListener) listener);
    }

    /**
     * Adds a listener for {@code PlaceholderButtonClickedEvent} events fired by
     * the webcomponent.
     *
     * @param listener the listener
     * @return a {@link Registration} for removing the event listener
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public Registration addPlaceholderButtonClickedListener(
            ComponentEventListener<PlaceholderButtonClickedEvent> listener) {
        return addListener(PlaceholderButtonClickedEvent.class,
                (ComponentEventListener) listener);
    }

    /**
     * Adds a listener for {@code PlaceholderInsertedEvent} events fired by the
     * webcomponent.
     *
     * @param listener the listener
     * @return a {@link Registration} for removing the event listener
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public Registration addPlaceholderInsertedListener(
            ComponentEventListener<PlaceholderInsertedEvent> listener) {
        return addListener(PlaceholderInsertedEvent.class,
                (ComponentEventListener) listener);
    }

    /**
     * Adds a listener for {@code PlaceholderSelectedEvent} events fired by the
     * webcomponent.
     *
     * @param listener the listener
     * @return a {@link Registration} for removing the event listener
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public Registration addPlaceholderSelectedListener(
            ComponentEventListener<PlaceholderSelectedEvent> listener) {
        return addListener(PlaceholderSelectedEvent.class,
                (ComponentEventListener) listener);
    }

    /**
     * Adds a listener for {@code PlaceholderLeaveEvent} events fired by the
     * webcomponent.
     *
     * @param listener the listener
     * @return a {@link Registration} for removing the event listener
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public Registration addPlaceholderLeaveListener(
            ComponentEventListener<PlaceholderLeaveEvent> listener) {
        return addListener(PlaceholderLeaveEvent.class,
                (ComponentEventListener) listener);
    }

    /**
     * Adds a listener for {@code PlaceholderAppearanceChangedEvent} events
     * fired by the webcomponent.
     *
     * @param listener the listener
     * @return a {@link Registration} for removing the event listener
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public Registration addPlaceholderAppearanceChangedListener(
            ComponentEventListener<PlaceholderAppearanceChangedEvent> listener) {
        return addListener(PlaceholderAppearanceChangedEvent.class,
                (ComponentEventListener) listener);
    }

    /**
     * Adds a listener for {@code change} events fired by the webcomponent.
     *
     * @param listener the listener
     * @return a {@link Registration} for removing the event listener
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public Registration addChangeListener(
            ComponentEventListener<ChangeEvent> listener) {
        return addListener(ChangeEvent.class,
                (ComponentEventListener) listener);
    }

    // ================================================================
    // Inner class: ToolbarButton enum
    // ================================================================

    /**
     * Enum of standard toolbar buttons. Used for visibility control,
     * keyboard shortcut assignment, and icon replacement.
     */
    public enum ToolbarButton {
        UNDO,
        REDO,
        BOLD,
        ITALIC,
        UNDERLINE,
        STRIKE,
        H1,
        H2,
        H3,
        SUBSCRIPT,
        SUPERSCRIPT,
        LIST_ORDERED,
        LIST_BULLET,
        DEINDENT,
        INDENT,
        ALIGN_LEFT,
        ALIGN_CENTER,
        ALIGN_RIGHT,
        ALIGN_JUSTIFY,
        IMAGE,
        LINK,
        BLOCKQUOTE,
        CODE_BLOCK,
        WHITESPACE,
        READONLY,
        CLEAN,
        PLACEHOLDER,
        PLACEHOLDER_APPEARANCE;

        @Override
        public String toString() {
            return "\"" + getButtonName() + "\"";
        }

        /**
         * Returns the camelCase button name used in JS toolbar button
         * identification.
         *
         * @return the camelCase button name
         */
        public String getButtonName() {
            String str = this.name().toLowerCase();
            String[] parts = str.split("_");
            if (parts.length == 1) {
                return str;
            }

            for (int i = 1; i < parts.length; i++) {
                parts[i] = Character.toUpperCase(parts[i].charAt(0))
                        + parts[i].substring(1);
            }

            return String.join("", parts);
        }
    }

    // ================================================================
    // Inner class: EnhancedRichTextEditorI18n
    // ================================================================

    /**
     * The internationalization properties for
     * {@link EnhancedRichTextEditor}.
     * <p>
     * Extends RTE 2's {@link RichTextEditorI18n} with ERTE-specific
     * properties: readonly button, placeholder, placeholder appearance,
     * and alignment justify.
     */
    public static class EnhancedRichTextEditorI18n
            extends RichTextEditor.RichTextEditorI18n {

        /**
         * Keys that are ERTE-specific and not handled by the parent
         * {@link RichTextEditor#setI18n(RichTextEditorI18n)} method.
         * These are sent via JS execution.
         */
        static final String[] ERTE_SPECIFIC_KEYS = {
                "readonly", "placeholder", "placeholderAppearance",
                "placeholderComboBoxLabel", "placeholderAppearanceLabel1",
                "placeholderAppearanceLabel2", "placeholderDialogTitle",
                "alignJustify", "deindent"
        };

        private String alignJustify;
        private String deindent;
        private String readonly;
        private String placeholder;
        private String placeholderAppearance;
        private String placeholderComboBoxLabel;
        private String placeholderAppearanceLabel1;
        private String placeholderAppearanceLabel2;
        private String placeholderDialogTitle;

        // -- alignJustify --

        /**
         * Gets the translated word for {@code alignJustify}.
         *
         * @return the translated word for alignJustify
         */
        public String getAlignJustify() {
            return alignJustify;
        }

        /**
         * Sets the translated word for {@code alignJustify}.
         *
         * @param alignJustify
         *            the translated word for alignJustify
         * @return this instance for method chaining
         */
        public EnhancedRichTextEditorI18n setAlignJustify(String alignJustify) {
            this.alignJustify = alignJustify;
            return this;
        }

        // -- deindent --

        /**
         * Gets the translated word for {@code deindent} (outdent).
         *
         * @return the translated word for deindent
         */
        public String getDeindent() {
            return deindent;
        }

        /**
         * Sets the translated word for {@code deindent}.
         *
         * @param deindent
         *            the translated word for deindent
         * @return this instance for method chaining
         */
        public EnhancedRichTextEditorI18n setDeindent(String deindent) {
            this.deindent = deindent;
            return this;
        }

        // -- readonly --

        /**
         * Gets the translated word for {@code readonly}.
         *
         * @return the translated word for readonly
         */
        public String getReadonly() {
            return readonly;
        }

        /**
         * Sets the translated word for {@code readonly}.
         *
         * @param readonly
         *            the translated word for readonly
         * @return this instance for method chaining
         */
        public EnhancedRichTextEditorI18n setReadonly(String readonly) {
            this.readonly = readonly;
            return this;
        }

        // -- placeholder --

        /**
         * Gets the translated word for {@code placeholder}.
         *
         * @return the translated word for placeholder
         */
        public String getPlaceholder() {
            return placeholder;
        }

        /**
         * Sets the translated word for {@code placeholder}.
         *
         * @param placeholder
         *            the translated word for placeholder
         * @return this instance for method chaining
         */
        public EnhancedRichTextEditorI18n setPlaceholder(String placeholder) {
            this.placeholder = placeholder;
            return this;
        }

        // -- placeholderAppearance --

        /**
         * Gets the translated word for {@code placeholderAppearance}.
         *
         * @return the translated word for placeholderAppearance
         */
        public String getPlaceholderAppearance() {
            return placeholderAppearance;
        }

        /**
         * Sets the translated word for {@code placeholderAppearance}.
         *
         * @param placeholderAppearance
         *            the translated word for placeholderAppearance
         * @return this instance for method chaining
         */
        public EnhancedRichTextEditorI18n setPlaceholderAppearance(
                String placeholderAppearance) {
            this.placeholderAppearance = placeholderAppearance;
            return this;
        }

        // -- placeholderComboBoxLabel --

        /**
         * Gets the translated word for {@code placeholderComboBoxLabel}.
         *
         * @return the translated word for placeholderComboBoxLabel
         */
        public String getPlaceholderComboBoxLabel() {
            return placeholderComboBoxLabel;
        }

        /**
         * Sets the translated word for {@code placeholderComboBoxLabel}.
         *
         * @param placeholderComboBoxLabel
         *            the translated word for placeholderComboBoxLabel
         * @return this instance for method chaining
         */
        public EnhancedRichTextEditorI18n setPlaceholderComboBoxLabel(
                String placeholderComboBoxLabel) {
            this.placeholderComboBoxLabel = placeholderComboBoxLabel;
            return this;
        }

        // -- placeholderAppearanceLabel1 --

        /**
         * Gets the translated word for {@code placeholderAppearanceLabel1}.
         *
         * @return the translated word for placeholderAppearanceLabel1
         */
        public String getPlaceholderAppearanceLabel1() {
            return placeholderAppearanceLabel1;
        }

        /**
         * Sets the translated word for {@code placeholderAppearanceLabel1}.
         *
         * @param placeholderAppearanceLabel1
         *            the translated word for placeholderAppearanceLabel1
         * @return this instance for method chaining
         */
        public EnhancedRichTextEditorI18n setPlaceholderAppearanceLabel1(
                String placeholderAppearanceLabel1) {
            this.placeholderAppearanceLabel1 = placeholderAppearanceLabel1;
            return this;
        }

        // -- placeholderAppearanceLabel2 --

        /**
         * Gets the translated word for {@code placeholderAppearanceLabel2}.
         *
         * @return the translated word for placeholderAppearanceLabel2
         */
        public String getPlaceholderAppearanceLabel2() {
            return placeholderAppearanceLabel2;
        }

        /**
         * Sets the translated word for {@code placeholderAppearanceLabel2}.
         *
         * @param placeholderAppearanceLabel2
         *            the translated word for placeholderAppearanceLabel2
         * @return this instance for method chaining
         */
        public EnhancedRichTextEditorI18n setPlaceholderAppearanceLabel2(
                String placeholderAppearanceLabel2) {
            this.placeholderAppearanceLabel2 = placeholderAppearanceLabel2;
            return this;
        }

        // -- placeholderDialogTitle --

        /**
         * Gets the translated word for {@code placeholderDialogTitle}.
         *
         * @return the translated word for placeholderDialogTitle
         */
        public String getPlaceholderDialogTitle() {
            return placeholderDialogTitle;
        }

        /**
         * Sets the translated word for {@code placeholderDialogTitle}.
         *
         * @param placeholderDialogTitle
         *            the translated word for placeholderDialogTitle
         * @return this instance for method chaining
         */
        public EnhancedRichTextEditorI18n setPlaceholderDialogTitle(
                String placeholderDialogTitle) {
            this.placeholderDialogTitle = placeholderDialogTitle;
            return this;
        }

        // -- Builder-pattern overrides for parent setters --
        // These override parent methods to return EnhancedRichTextEditorI18n
        // for fluent chaining.

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
        public EnhancedRichTextEditorI18n setSubscript(String subscript) {
            super.setSubscript(subscript);
            return this;
        }

        @Override
        public EnhancedRichTextEditorI18n setSuperscript(String superscript) {
            super.setSuperscript(superscript);
            return this;
        }

        @Override
        public EnhancedRichTextEditorI18n setListOrdered(String listOrdered) {
            super.setListOrdered(listOrdered);
            return this;
        }

        @Override
        public EnhancedRichTextEditorI18n setListBullet(String listBullet) {
            super.setListBullet(listBullet);
            return this;
        }

        @Override
        public EnhancedRichTextEditorI18n setAlignLeft(String alignLeft) {
            super.setAlignLeft(alignLeft);
            return this;
        }

        @Override
        public EnhancedRichTextEditorI18n setAlignCenter(String alignCenter) {
            super.setAlignCenter(alignCenter);
            return this;
        }

        @Override
        public EnhancedRichTextEditorI18n setAlignRight(String alignRight) {
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
        public EnhancedRichTextEditorI18n setBlockquote(String blockquote) {
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

        /**
         * Gets the stringified values of the tooltips.
         *
         * @return stringified values of the tooltips
         */
        @Override
        public String toString() {
            return "[" + getUndo() + ", " + getRedo() + ", " + getBold()
                    + ", " + getItalic() + ", " + getUnderline() + ", "
                    + getStrike() + ", " + getH1() + ", " + getH2() + ", "
                    + getH3() + ", " + getSubscript() + ", "
                    + getSuperscript() + ", " + getListOrdered() + ", "
                    + getListBullet() + ", " + deindent + ", "
                    + getIndent() + ", " + getAlignLeft() + ", "
                    + getAlignCenter() + ", " + getAlignRight() + ", "
                    + alignJustify + ", " + getImage() + ", " + getLink()
                    + ", " + getBlockquote() + ", " + getCodeBlock() + ", "
                    + readonly + ", " + placeholder + ", "
                    + placeholderAppearance + ", " + placeholderComboBoxLabel
                    + ", " + placeholderAppearanceLabel1 + ", "
                    + placeholderAppearanceLabel2 + ", "
                    + placeholderDialogTitle + ", " + getClean() + "]";
        }
    }

    // ================================================================
    // Inner class: ChangeEvent
    // ================================================================

    /**
     * Event fired when the editor content changes (DOM "change" event).
     */
    @DomEvent("change")
    public static class ChangeEvent
            extends ComponentEvent<EnhancedRichTextEditor> {
        public ChangeEvent(EnhancedRichTextEditor source,
                boolean fromClient) {
            super(source, fromClient);
        }
    }

    // ================================================================
    // Placeholder event classes
    // ================================================================

    /**
     * Abstract base class for placeholder events that carry a single
     * placeholder reference.
     */
    public static abstract class AbstractPlaceholderEvent
            extends ComponentEvent<EnhancedRichTextEditor> {
        private Placeholder placeholder;

        public AbstractPlaceholderEvent(EnhancedRichTextEditor source,
                boolean fromClient, ObjectNode placeholderJson) {
            super(source, fromClient);
            if (placeholderJson != null) {
                placeholder = new Placeholder(placeholderJson);
            }
        }

        /**
         * Get the Placeholder that was target of the event.
         *
         * @return A Placeholder
         */
        public Placeholder getPlaceholder() {
            return ((EnhancedRichTextEditor) source)
                    .getPlaceholder(placeholder);
        }
    }

    /**
     * Abstract base class for placeholder events that carry multiple
     * placeholder references.
     */
    public static abstract class AbstractMultiPlaceholderEvent
            extends ComponentEvent<EnhancedRichTextEditor> {
        private List<Placeholder> placeholders = new ArrayList<>();

        public AbstractMultiPlaceholderEvent(
                EnhancedRichTextEditor source, boolean fromClient,
                ArrayNode placeholderJson) {
            super(source, fromClient);
            if (placeholderJson != null) {
                for (int i = 0; i < placeholderJson.size(); i++) {
                    ObjectNode pHolder = (ObjectNode) placeholderJson.get(i);
                    int index = -1;
                    if (pHolder.has("placeholder")
                            && pHolder.has("index")) {
                        double ind = pHolder.get("index").asDouble();
                        index = (int) ind;
                        pHolder = (ObjectNode) pHolder.get("placeholder");
                    }
                    Placeholder placeholder = new Placeholder(pHolder);
                    if (index != -1) {
                        placeholder.setIndex(index);
                    }
                    placeholders.add(placeholder);
                }
            }
        }

        /**
         * Get the Placeholders that were target of the event.
         *
         * @return A list of Placeholders
         */
        public List<Placeholder> getPlaceholders() {
            List<Placeholder> actualPlaceholders = new ArrayList<>();
            for (Placeholder placeholder : placeholders) {
                Placeholder actualPlaceholder = ((EnhancedRichTextEditor) source)
                        .getPlaceholder(placeholder);
                if (actualPlaceholder != null
                        && placeholder.getIndex() != -1) {
                    actualPlaceholder.setIndex(placeholder.getIndex());
                }
                if (actualPlaceholder != null) {
                    actualPlaceholders.add(actualPlaceholder);
                }
            }
            return actualPlaceholders;
        }
    }

    /**
     * Event fired before placeholders are removed. Call {@link #remove()} to
     * confirm removal; if not called, the placeholders will not be removed.
     */
    @DomEvent("placeholder-before-delete")
    public static class PlaceholderBeforeRemoveEvent
            extends AbstractMultiPlaceholderEvent {
        public PlaceholderBeforeRemoveEvent(EnhancedRichTextEditor source,
                boolean fromClient,
                @EventData("event.preventDefault()") Object ignored,
                @EventData("event.detail") ObjectNode detail) {
            super(source, fromClient,
                    (ArrayNode) detail.get("placeholders"));
        }

        /**
         * Confirm removal of the Placeholders. If this method is not called
         * in the event, the Placeholders will not be removed.
         * {@link PlaceholderRemovedEvent} will be emitted after removal.
         */
        public void remove() {
            ((EnhancedRichTextEditor) source).getElement()
                    .executeJs("this._confirmRemovePlaceholders()");
        }
    }

    /**
     * Event fired before placeholders are inserted. Call {@link #insert()} to
     * confirm insertion; if not called, the placeholders will not be inserted.
     */
    @DomEvent("placeholder-before-insert")
    public static class PlaceholderBeforeInsertEvent
            extends AbstractMultiPlaceholderEvent {
        public PlaceholderBeforeInsertEvent(EnhancedRichTextEditor source,
                boolean fromClient,
                @EventData("event.preventDefault()") Object ignored,
                @EventData("event.detail") ObjectNode detail) {
            super(source, fromClient,
                    (ArrayNode) detail.get("placeholders"));
        }

        /**
         * Confirm insertion of the Placeholders. If this method is not called
         * in the event, the Placeholders will not be inserted.
         * {@link PlaceholderInsertedEvent} will be emitted after insertion.
         */
        public void insert() {
            ((EnhancedRichTextEditor) source).getElement()
                    .executeJs("this._confirmInsertPlaceholders()");
        }
    }

    /**
     * Event fired after placeholders have been removed.
     */
    @DomEvent("placeholder-delete")
    public static class PlaceholderRemovedEvent
            extends AbstractMultiPlaceholderEvent {
        public PlaceholderRemovedEvent(EnhancedRichTextEditor source,
                boolean fromClient,
                @EventData("event.detail") ObjectNode detail) {
            super(source, fromClient,
                    (ArrayNode) detail.get("placeholders"));
        }
    }

    /**
     * Event fired when the placeholder button in the toolbar is clicked.
     * Call {@link #insert(Placeholder)} to insert a placeholder at the
     * current position.
     */
    @DomEvent("placeholder-button-click")
    public static class PlaceholderButtonClickedEvent
            extends ComponentEvent<EnhancedRichTextEditor> {
        private int position;

        public PlaceholderButtonClickedEvent(EnhancedRichTextEditor source,
                boolean fromClient,
                @EventData("event.preventDefault()") Object ignored,
                @EventData("event.detail.position") int position) {
            super(source, fromClient);
            this.position = position;
        }

        /**
         * Get current position.
         *
         * @return int value
         */
        public int getPosition() {
            return position;
        }

        /**
         * Add text as placeholder to current position.
         *
         * @param placeholder Placeholder to insert
         */
        public void insert(Placeholder placeholder) {
            insert(placeholder, position);
        }

        /**
         * Add text as placeholder to the given position.
         *
         * @param placeholder Placeholder to insert
         * @param position Position where to insert, not validated
         */
        public void insert(Placeholder placeholder, int position) {
            Objects.requireNonNull(placeholder, "Placeholder cannot be null");
            ((EnhancedRichTextEditor) source).getElement().executeJs(
                    "this._confirmInsertPlaceholders([{placeholder: $0, index: $1}])",
                    placeholder.toJson(), position);
        }
    }

    /**
     * Event fired after placeholders have been inserted.
     */
    @DomEvent("placeholder-insert")
    public static class PlaceholderInsertedEvent
            extends AbstractMultiPlaceholderEvent {
        public PlaceholderInsertedEvent(EnhancedRichTextEditor source,
                boolean fromClient,
                @EventData("event.detail") ObjectNode detail) {
            super(source, fromClient,
                    (ArrayNode) detail.get("placeholders"));
        }
    }

    /**
     * Event fired when placeholders are selected.
     */
    @DomEvent("placeholder-select")
    public static class PlaceholderSelectedEvent
            extends AbstractMultiPlaceholderEvent {
        public PlaceholderSelectedEvent(EnhancedRichTextEditor source,
                boolean fromClient,
                @EventData("event.detail") ObjectNode detail) {
            super(source, fromClient,
                    (ArrayNode) detail.get("placeholders"));
        }
    }

    /**
     * Event fired when the cursor leaves a placeholder.
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
     * Event fired when placeholder appearance changes.
     */
    @DomEvent("placeholder-appearance-change")
    public static class PlaceholderAppearanceChangedEvent
            extends ComponentEvent<EnhancedRichTextEditor> {
        private Boolean altAppearance;
        private String appearanceLabel;

        public PlaceholderAppearanceChangedEvent(
                EnhancedRichTextEditor source, boolean fromClient,
                @EventData("event.detail") ObjectNode detail) {
            super(source, fromClient);
            altAppearance = detail.has("altAppearance")
                    ? detail.get("altAppearance").asBoolean()
                    : null;
            appearanceLabel = detail.has("appearanceLabel")
                    ? detail.get("appearanceLabel").asText()
                    : null;
        }

        /**
         * Gets whether alternative appearance is active.
         *
         * @return the alternative appearance flag
         */
        public Boolean getAltAppearance() {
            return altAppearance;
        }

        /**
         * Gets the appearance label.
         *
         * @return the appearance label
         */
        public String getAppearanceLabel() {
            return appearanceLabel;
        }
    }
}
