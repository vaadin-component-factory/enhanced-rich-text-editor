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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.vaadin.componentfactory.toolbar.ToolbarSlot;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.DomEvent;
import com.vaadin.flow.component.EventData;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.richtexteditor.RteExtensionBase;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.shared.Registration;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * Enhanced Rich Text Editor — V25 / Quill 2.
 * <p>
 * Extends {@link RteExtensionBase} which bridges package-private access to
 * RTE 2. All ERTE-specific logic lives in this class and package.
 */
@Tag("vcf-enhanced-rich-text-editor")
@JsModule("./vcf-enhanced-rich-text-editor.js")
public class EnhancedRichTextEditor extends RteExtensionBase {

    private List<Placeholder> placeholders;

    // ---- Toolbar component API ----

    /**
     * Adds components to the given toolbar slot (appended).
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
     * Adds components to the given toolbar slot at the specified index.
     */
    public void addToolbarComponentsAtIndex(ToolbarSlot toolbarSlot,
            int index, Component... components) {
        Objects.requireNonNull(components);
        for (Component component : components) {
            Objects.requireNonNull(component);
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
     * Sets the list of available placeholders for the editor.
     *
     * @param placeholders the placeholder definitions
     */
    public void setPlaceholders(List<Placeholder> placeholders) {
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

    public Registration addPlaceholderButtonClickedListener(
            ComponentEventListener<PlaceholderButtonClickedEvent> listener) {
        return addListener(PlaceholderButtonClickedEvent.class, listener);
    }

    public Registration addPlaceholderBeforeInsertListener(
            ComponentEventListener<PlaceholderBeforeInsertEvent> listener) {
        return addListener(PlaceholderBeforeInsertEvent.class, listener);
    }

    public Registration addPlaceholderInsertedListener(
            ComponentEventListener<PlaceholderInsertedEvent> listener) {
        return addListener(PlaceholderInsertedEvent.class, listener);
    }

    public Registration addPlaceholderBeforeRemoveListener(
            ComponentEventListener<PlaceholderBeforeRemoveEvent> listener) {
        return addListener(PlaceholderBeforeRemoveEvent.class, listener);
    }

    public Registration addPlaceholderRemovedListener(
            ComponentEventListener<PlaceholderRemovedEvent> listener) {
        return addListener(PlaceholderRemovedEvent.class, listener);
    }

    public Registration addPlaceholderSelectedListener(
            ComponentEventListener<PlaceholderSelectedEvent> listener) {
        return addListener(PlaceholderSelectedEvent.class, listener);
    }

    public Registration addPlaceholderLeaveListener(
            ComponentEventListener<PlaceholderLeaveEvent> listener) {
        return addListener(PlaceholderLeaveEvent.class, listener);
    }

    public Registration addPlaceholderAppearanceChangedListener(
            ComponentEventListener<PlaceholderAppearanceChangedEvent> listener) {
        return addListener(PlaceholderAppearanceChangedEvent.class, listener);
    }

    // ========================================================================
    // Placeholder Event Classes
    // ========================================================================

    /**
     * Abstract base for events that carry a list of placeholders.
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

        public int getPosition() {
            return position;
        }

        /**
         * Insert a placeholder at the current cursor position.
         */
        public void insert(Placeholder placeholder) {
            insert(placeholder, position);
        }

        /**
         * Insert a placeholder at the given position.
         */
        public void insert(Placeholder placeholder, int position) {
            Objects.requireNonNull(placeholder, "Placeholder cannot be null");
            EnhancedRichTextEditor s = (EnhancedRichTextEditor) source;
            s.getElement().executeJs(
                    "this._confirmInsertPlaceholders([{$0,index: $1}])",
                    placeholder.toJson(), position);
        }
    }

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
         * Confirm insertion of the placeholders. If this method is not called,
         * the placeholders will not be inserted.
         */
        public void insert() {
            EnhancedRichTextEditor s = (EnhancedRichTextEditor) source;
            s.getElement().executeJs("this._confirmInsertPlaceholders()");
        }
    }

    @DomEvent("placeholder-insert")
    public static class PlaceholderInsertedEvent
            extends AbstractMultiPlaceholderEvent {

        public PlaceholderInsertedEvent(EnhancedRichTextEditor source,
                boolean fromClient,
                @EventData("event.detail") ObjectNode detail) {
            super(source, fromClient, detail.get("placeholders"));
        }
    }

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
         * Confirm removal of the placeholders. If this method is not called,
         * the placeholders will not be removed.
         */
        public void remove() {
            EnhancedRichTextEditor s = (EnhancedRichTextEditor) source;
            s.getElement().executeJs("this._confirmRemovePlaceholders()");
        }
    }

    @DomEvent("placeholder-delete")
    public static class PlaceholderRemovedEvent
            extends AbstractMultiPlaceholderEvent {

        public PlaceholderRemovedEvent(EnhancedRichTextEditor source,
                boolean fromClient,
                @EventData("event.detail") ObjectNode detail) {
            super(source, fromClient, detail.get("placeholders"));
        }
    }

    @DomEvent("placeholder-select")
    public static class PlaceholderSelectedEvent
            extends AbstractMultiPlaceholderEvent {

        public PlaceholderSelectedEvent(EnhancedRichTextEditor source,
                boolean fromClient,
                @EventData("event.detail") ObjectNode detail) {
            super(source, fromClient, detail.get("placeholders"));
        }
    }

    @DomEvent("placeholder-leave")
    public static class PlaceholderLeaveEvent
            extends ComponentEvent<EnhancedRichTextEditor> {

        public PlaceholderLeaveEvent(EnhancedRichTextEditor source,
                boolean fromClient) {
            super(source, fromClient);
        }
    }

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

        public Boolean getAltAppearance() {
            return altAppearance;
        }

        public String getAppearanceLabel() {
            return appearanceLabel;
        }
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
