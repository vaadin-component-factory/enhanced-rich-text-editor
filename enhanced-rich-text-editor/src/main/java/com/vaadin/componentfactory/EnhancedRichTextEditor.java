package com.vaadin.componentfactory;

/*
 * #%L
 * EnhancedRichTextEditor for Vaadin 10
 * %%
 * Copyright (C) 2017 - 2019 Vaadin Ltd
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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;

import com.vaadin.flow.component.CompositionNotifier;
import com.vaadin.flow.component.HasSize;
import com.vaadin.flow.component.InputNotifier;
import com.vaadin.flow.component.KeyNotifier;
import com.vaadin.flow.component.Synchronize;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dependency.JavaScript;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.value.HasValueChangeMode;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.internal.JsonSerializer;

import elemental.json.JsonArray;
import elemental.json.JsonObject;
import elemental.json.impl.JreJsonArray;
import elemental.json.impl.JreJsonFactory;

/**
 * Server-side component for the {@code <vcf-enhanced-rich-text-editor>}
 * component.
 *
 * @author Vaadin Ltd
 */
@Tag("vcf-enhanced-rich-text-editor")
@JsModule("./richTextEditorConnector-npm.js")
@JavaScript("frontend://richTextEditorConnector.js")
public class EnhancedRichTextEditor
        extends GeneratedEnhancedRichTextEditor<EnhancedRichTextEditor, String>
        implements HasSize, HasValueChangeMode, InputNotifier, KeyNotifier,
        CompositionNotifier {

    private ValueChangeMode currentMode;
    private RichTextEditorI18n i18n;
    private Map<ToolbarButton, Boolean> toolbarButtonsVisibility;
    private Collection<Placeholder> placeholders;

    /**
     * Gets the internationalization object previously set for this component.
     * <p>
     * Note: updating the object content that is gotten from this method will
     * not update the lang on the component if not set back using
     * {@link EnhancedRichTextEditor#setI18n(RichTextEditorI18n)}
     *
     * @return the i18n object. It will be <code>null</code>, If the i18n
     *         properties weren't set.
     */
    public RichTextEditorI18n getI18n() {
        return i18n;
    }

    /**
     * Sets the internationalization properties for this component.
     *
     * @param i18n
     *            the internationalized properties, not <code>null</code>
     */
    public void setI18n(RichTextEditorI18n i18n) {
        Objects.requireNonNull(i18n,
                "The I18N properties object should not be null");
        this.i18n = i18n;
        runBeforeClientResponse(ui -> {
            if (i18n == this.i18n) {
                JsonObject i18nObject = (JsonObject) JsonSerializer
                        .toJson(this.i18n);
                for (String key : i18nObject.keys()) {
                    ui.getPage().executeJavaScript(
                            "$0.set('i18n." + key + "', $1)", getElement(),
                            i18nObject.get(key));
                }
            }
        });
    }

    public Map<ToolbarButton, Boolean> getToolbarButtonsVisibility() {
        return toolbarButtonsVisibility;
    }

    /**
     * Set which toolbar buttons are visible.
     * 
     * @param toolbarButtonsVisibility
     *            Map of button and boolean value. Boolean value false
     *            associated with the button means that button will be hidden.
     */
    public void setToolbarButtonsVisibility(
            Map<ToolbarButton, Boolean> toolbarButtonsVisibility) {
        this.toolbarButtonsVisibility = toolbarButtonsVisibility;
        runBeforeClientResponse(ui -> {
            String str = toolbarButtonsVisibility.toString();
            str = str.replaceAll("=", ":");
            ui.getPage().executeJavaScript("setToolbarButtons($0, $1)",
                    getElement(), str);
        });
    }

    void runBeforeClientResponse(SerializableConsumer<UI> command) {
        getElement().getNode().runWhenAttached(ui -> ui
                .beforeClientResponse(this, context -> command.accept(ui)));
    }

    /**
     * Constructs an empty {@code EnhancedRichTextEditor}.
     */
    public EnhancedRichTextEditor() {
        super("", "", false);
        setValueChangeMode(ValueChangeMode.ON_CHANGE);
    }

    /**
     * Constructs a {@code EnhancedRichTextEditor} with the initial value
     *
     * @param initialValue
     *            the initial value
     * @see #setValue(Object)
     */
    public EnhancedRichTextEditor(String initialValue) {
        this();
        setValue(initialValue);
    }

    /**
     * Constructs an empty {@code TextField} with a value change listener.
     *
     * @param listener
     *            the value change listener
     * @see #addValueChangeListener(com.vaadin.flow.component.HasValue.ValueChangeListener)
     */
    public EnhancedRichTextEditor(
            ValueChangeListener<? super ComponentValueChangeEvent<EnhancedRichTextEditor, String>> listener) {
        this();
        addValueChangeListener(listener);
    }

    /**
     * Constructs an empty {@code EnhancedRichTextEditor} with a value change
     * listener and an initial value.
     *
     * @param initialValue
     *            the initial value
     * @param listener
     *            the value change listener
     * @see #setValue(Object)
     * @see #addValueChangeListener(com.vaadin.flow.component.HasValue.ValueChangeListener)
     */
    public EnhancedRichTextEditor(String initialValue,
            ValueChangeListener<? super ComponentValueChangeEvent<EnhancedRichTextEditor, String>> listener) {
        this();
        setValue(initialValue);
        addValueChangeListener(listener);
    }

    /**
     * {@inheritDoc}
     * <p>
     * The default value is {@link ValueChangeMode#ON_CHANGE}.
     */
    @Override
    public ValueChangeMode getValueChangeMode() {
        return currentMode;
    }

    @Override
    public void setValueChangeMode(ValueChangeMode valueChangeMode) {
        currentMode = valueChangeMode;
        setSynchronizedEvent(
                ValueChangeMode.eventForMode(valueChangeMode, "value-changed"));
    }

    /**
     * Sets the value of this editor. Should be in
     * <a href="https://github.com/quilljs/delta">Delta</a> format. If the new
     * value is not equal to {@code getValue()}, fires a value change event.
     * Throws {@code NullPointerException}, if the value is null.
     * <p>
     * Note: {@link Binder} will take care of the {@code null} conversion when
     * integrates with the editor, as long as no new converter is defined.
     *
     * @param value
     *            the new value in Delta format, not {@code null}
     */
    @Override
    public void setValue(String value) {
        super.setValue(value);
    }

    /**
     * Returns the current value of the text editor in
     * <a href="https://github.com/quilljs/delta">Delta</a> format. By default,
     * the empty editor will return an empty string.
     *
     * @return the current value.
     */
    @Override
    public String getValue() {
        return super.getValue();
    }

    /**
     * Value of the editor presented as HTML string.
     *
     * @return the sanitized {@code htmlValue} property from the webcomponent.
     */
    public String getHtmlValue() {
        // Using basic whitelist and adding img tag with data protocol enabled.
        return sanitize(getHtmlValueString());
    }

    String sanitize(String html) {
        return org.jsoup.Jsoup.clean(html,
                org.jsoup.safety.Whitelist.basic()
                        .addTags("img", "h1", "h2", "h3", "s")
                        .addAttributes("img", "align", "alt", "height", "src",
                                "title", "width")
                        .addAttributes(":all", "style")
                        .addProtocols("img", "src", "data"));
    }

    /**
     * Set placeholders shown in the Placeholder drop down menu.
     * 
     * @param placeholders
     *            Collection of Placeholder objects
     */
    public void setPlaceholders(Collection<Placeholder> placeholders) {
        Objects.requireNonNull(placeholders, "placeholders cannot be null");
        JreJsonFactory factory = new JreJsonFactory();
        JsonArray jsonArray = new JreJsonArray(factory);

        int index = 0;
        for (Placeholder placeholder : placeholders) {
            jsonArray.set(index++, placeholder.toJson());
        }

        this.placeholders = placeholders;
        getElement().setPropertyJson("placeholders", jsonArray);
    }

    @Synchronize(property = "placeholders", value = "placeholders-changed")
    public Collection<Placeholder> getPlaceholders() {
        ArrayList<Placeholder> placeholders = new ArrayList<>();
        JsonArray rawArray = (JsonArray) getElement()
                .getPropertyRaw("placeholders");

        if (rawArray == null) {
            return placeholders;
        }

        for (int i = 0; i < rawArray.length(); i++) {
            JsonObject obj = rawArray.getObject(i);
            try {
                Placeholder placeholder = new Placeholder(obj);
                placeholders.add(placeholder);

            } catch (IllegalArgumentException e) {
            }
        }

        return placeholders;
    }

    public void setPlaceholderAltAppearancePattern(String pattern) {
        getElement().setProperty("placeholderAltAppearancePattern", pattern);
    }

    @Synchronize(property = "placeholderAltAppearancePattern", value = "placeholder-alt-appearance-pattern-changed")
    public String getPlaceholderAltAppearancePattern() {
        return getElement().getProperty("placeholderAltAppearancePattern");
    }

    public void setPlacehoderAltAppearance(boolean altAppearance) {
        getElement().setProperty("placeholderAltAppearance", altAppearance);
    }

    @Synchronize(property = "placeholderAltAppearance", value = "placeholder-alt-appearance-changed")
    public boolean isPlacehoderAltAppearance() {
        return getElement().getProperty("placeholderAltAppearance", false);
    }

    /**
     * For internal use only. Return Placeholder from the master list matching
     * the given Placeholder by getText.
     * 
     * @param placeholder
     *            The Placeholder to be searched.
     * @return A Placeholder
     */
    protected Placeholder getPlaceholder(Placeholder placeholder) {
        Objects.requireNonNull(placeholder, "Placeholder cannot be null");
        Objects.requireNonNull(placeholders,
                "getPlaceholder cannot be called before placeholders are set");
        return placeholders.stream()
                .filter(p -> p.getText().equals(placeholder.getText()))
                .findFirst().orElse(null);
    }

    /**
     * The internationalization properties for {@link EnhancedRichTextEditor}.
     */
    public static class RichTextEditorI18n implements Serializable {
        private String undo;
        private String redo;
        private String bold;
        private String italic;
        private String underline;
        private String strike;
        private String h1;
        private String h2;
        private String h3;
        private String subscript;
        private String superscript;
        private String listOrdered;
        private String listBullet;
        private String alignLeft;
        private String alignCenter;
        private String alignRight;
        private String image;
        private String link;
        private String blockquote;
        private String codeBlock;
        private String readonly;
        private String placeholder;
        private String placeholderAppearance;
        private String placeholderComboBoxLabel;
        private String placeholderAppearanceLabel1;
        private String placeholderAppearanceLabel2;
        private String placeholderDialogTitle;
        private String clean;

        /**
         * Gets the translated word for {@code undo}
         *
         * @return the translated word for undo
         */
        public String getUndo() {
            return undo;
        }

        /**
         * Sets the translated word for {@code undo}.
         *
         * @param undo
         *            the translated word for undo
         * @return this instance for method chaining
         */
        public RichTextEditorI18n setUndo(String undo) {
            this.undo = undo;
            return this;
        }

        /**
         * Gets the translated word for {@code redo}
         *
         * @return the translated word for redo
         */
        public String getRedo() {
            return redo;
        }

        /**
         * Sets the translated word for {@code redo}.
         *
         * @param redo
         *            the translated word for redo
         * @return this instance for method chaining
         */
        public RichTextEditorI18n setRedo(String redo) {
            this.redo = redo;
            return this;
        }

        /**
         * Gets the translated word for {@code bold}
         *
         * @return the translated word for bold
         */
        public String getBold() {
            return bold;
        }

        /**
         * Sets the translated word for {@code bold}.
         *
         * @param bold
         *            the translated word for bold
         * @return this instance for method chaining
         */
        public RichTextEditorI18n setBold(String bold) {
            this.bold = bold;
            return this;
        }

        /**
         * Gets the translated word for {@code italic}
         *
         * @return the translated word for italic
         */
        public String getItalic() {
            return italic;
        }

        /**
         * Sets the translated word for {@code italic}.
         *
         * @param italic
         *            the translated word for italic
         * @return this instance for method chaining
         */
        public RichTextEditorI18n setItalic(String italic) {
            this.italic = italic;
            return this;
        }

        /**
         * Gets the translated word for {@code underline}
         *
         * @return the translated word for underline
         */
        public String getUnderline() {
            return underline;
        }

        /**
         * Sets the translated word for {@code underline}.
         *
         * @param underline
         *            the translated word for underline
         * @return this instance for method chaining
         */
        public RichTextEditorI18n setUnderline(String underline) {
            this.underline = underline;
            return this;
        }

        /**
         * Gets the translated word for {@code strike}
         *
         * @return the translated word for strike
         */
        public String getStrike() {
            return strike;
        }

        /**
         * Sets the translated word for {@code strike}.
         *
         * @param strike
         *            the translated word for strike
         * @return this instance for method chaining
         */
        public RichTextEditorI18n setStrike(String strike) {
            this.strike = strike;
            return this;
        }

        /**
         * Gets the translated word for {@code h1}
         *
         * @return the translated word for h1
         */
        public String getH1() {
            return h1;
        }

        /**
         * Sets the translated word for {@code h1}.
         *
         * @param h1
         *            the translated word for h1
         * @return this instance for method chaining
         */
        public RichTextEditorI18n setH1(String h1) {
            this.h1 = h1;
            return this;
        }

        /**
         * Gets the translated word for {@code h2}
         *
         * @return the translated word for h2
         */
        public String getH2() {
            return h2;
        }

        /**
         * Sets the translated word for {@code h2}.
         *
         * @param h2
         *            the translated word for h2
         * @return this instance for method chaining
         */
        public RichTextEditorI18n setH2(String h2) {
            this.h2 = h2;
            return this;
        }

        /**
         * Gets the translated word for {@code h3}
         *
         * @return the translated word for h3
         */
        public String getH3() {
            return h3;
        }

        /**
         * Sets the translated word for {@code h3}.
         *
         * @param h3
         *            the translated word for h3
         * @return this instance for method chaining
         */
        public RichTextEditorI18n setH3(String h3) {
            this.h3 = h3;
            return this;
        }

        /**
         * Gets the translated word for {@code subscript}
         *
         * @return the translated word for subscript
         */
        public String getSubscript() {
            return subscript;
        }

        /**
         * Sets the translated word for {@code subscript}.
         *
         * @param subscript
         *            the translated word for subscript
         * @return this instance for method chaining
         */
        public RichTextEditorI18n setSubscript(String subscript) {
            this.subscript = subscript;
            return this;
        }

        /**
         * Gets the translated word for {@code superscript}
         *
         * @return the translated word for superscript
         */
        public String getSuperscript() {
            return superscript;
        }

        /**
         * Sets the translated word for {@code superscript}.
         *
         * @param superscript
         *            the translated word for superscript
         * @return this instance for method chaining
         */
        public RichTextEditorI18n setSuperscript(String superscript) {
            this.superscript = superscript;
            return this;
        }

        /**
         * Gets the translated word for {@code listOrdered}
         *
         * @return the translated word for listOrdered
         */
        public String getListOrdered() {
            return listOrdered;
        }

        /**
         * Sets the translated word for {@code listOrdered}.
         *
         * @param listOrdered
         *            the translated word for listOrdered
         * @return this instance for method chaining
         */
        public RichTextEditorI18n setListOrdered(String listOrdered) {
            this.listOrdered = listOrdered;
            return this;
        }

        /**
         * Gets the translated word for {@code listBullet}
         *
         * @return the translated word for listBullet
         */
        public String getListBullet() {
            return listBullet;
        }

        /**
         * Sets the translated word for {@code listBullet}.
         *
         * @param listBullet
         *            the translated word for listBullet
         * @return this instance for method chaining
         */
        public RichTextEditorI18n setListBullet(String listBullet) {
            this.listBullet = listBullet;
            return this;
        }

        /**
         * Gets the translated word for {@code alignLeft}
         *
         * @return the translated word for alignLeft
         */
        public String getAlignLeft() {
            return alignLeft;
        }

        /**
         * Sets the translated word for {@code alignLeft}.
         *
         * @param alignLeft
         *            the translated word for alignLeft
         * @return this instance for method chaining
         */
        public RichTextEditorI18n setAlignLeft(String alignLeft) {
            this.alignLeft = alignLeft;
            return this;
        }

        /**
         * Gets the translated word for {@code alignCenter}
         *
         * @return the translated word for alignCenter
         */
        public String getAlignCenter() {
            return alignCenter;
        }

        /**
         * Sets the translated word for {@code alignCenter}.
         *
         * @param alignCenter
         *            the translated word for alignCenter
         * @return this instance for method chaining
         */
        public RichTextEditorI18n setAlignCenter(String alignCenter) {
            this.alignCenter = alignCenter;
            return this;
        }

        /**
         * Gets the translated word for {@code alignRight}
         *
         * @return the translated word for alignRight
         */
        public String getAlignRight() {
            return alignRight;
        }

        /**
         * Sets the translated word for {@code alignRight}.
         *
         * @param alignRight
         *            the translated word for alignRight
         * @return this instance for method chaining
         */
        public RichTextEditorI18n setAlignRight(String alignRight) {
            this.alignRight = alignRight;
            return this;
        }

        /**
         * Gets the translated word for {@code image}
         *
         * @return the translated word for image
         */
        public String getImage() {
            return image;
        }

        /**
         * Sets the translated word for {@code image}.
         *
         * @param image
         *            the translated word for image
         * @return this instance for method chaining
         */
        public RichTextEditorI18n setImage(String image) {
            this.image = image;
            return this;
        }

        /**
         * Gets the translated word for {@code link}
         *
         * @return the translated word for link
         */
        public String getLink() {
            return link;
        }

        /**
         * Sets the translated word for {@code link}.
         *
         * @param link
         *            the translated word for link
         * @return this instance for method chaining
         */
        public RichTextEditorI18n setLink(String link) {
            this.link = link;
            return this;
        }

        /**
         * Gets the translated word for {@code blockquote}
         *
         * @return the translated word for blockquote
         */
        public String getBlockquote() {
            return blockquote;
        }

        /**
         * Sets the translated word for {@code blockquote}.
         *
         * @param blockquote
         *            the translated word for blockquote
         * @return this instance for method chaining
         */
        public RichTextEditorI18n setBlockquote(String blockquote) {
            this.blockquote = blockquote;
            return this;
        }

        /**
         * Gets the translated word for {@code codeBlock}
         *
         * @return the translated word for codeBlock
         */
        public String getCodeBlock() {
            return codeBlock;
        }

        /**
         * Sets the translated word for {@code codeBlock}.
         *
         * @param codeBlock
         *            the translated word for codeBlock
         * @return this instance for method chaining
         */
        public RichTextEditorI18n setCodeBlock(String codeBlock) {
            this.codeBlock = codeBlock;
            return this;
        }

        /**
         * Gets the translated word for {@code readonly}
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
        public RichTextEditorI18n setReadonly(String readonly) {
            this.readonly = readonly;
            return this;
        }

        /**
         * Gets the translated word for {@code placeholder}
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
        public RichTextEditorI18n setPlaceholder(String placeholder) {
            this.placeholder = placeholder;
            return this;
        }

        /**
         * Gets the translated word for {@code placeholderAppearance}
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
        public RichTextEditorI18n setPlaceholderAppeance(
                String placeholderAppearance) {
            this.placeholderAppearance = placeholderAppearance;
            return this;
        }

        /**
         * Gets the translated word for {@code placeholderComboBoxLabel}
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
        public RichTextEditorI18n setPlaceholderComboBoxLabel(
                String placeholderComboBoxLabel) {
            this.placeholderComboBoxLabel = placeholderComboBoxLabel;
            return this;
        }

        /**
         * Gets the translated word for {@code placeholderAppearanceLabel1}
         *
         * @return the translated word for placeholderAppearanceLabel1
         */
        public String setPlaceholderAppearanceLabel1() {
            return placeholderAppearanceLabel1;
        }

        /**
         * Sets the translated word for {@code placeholderAppearanceLabel1}.
         *
         * @param placeholderAppearanceLabel1
         *            the translated word for placeholderAppearanceLabel1
         * @return this instance for method chaining
         */
        public RichTextEditorI18n getPlaceholderAppearanceLabel1(
                String placeholderAppearanceLabel1) {
            this.placeholderAppearanceLabel1 = placeholderAppearanceLabel1;
            return this;
        }

        /**
         * Gets the translated word for {@code placeholderAppearanceLabel2}
         *
         * @return the translated word for placeholderAppearanceLabel2
         */
        public String setPlaceholderAppearanceLabel2() {
            return placeholderAppearanceLabel2;
        }

        /**
         * Sets the translated word for {@code placeholderAppearanceLabel2}.
         *
         * @param placeholderAppearanceLabel2
         *            the translated word for placeholderAppearanceLabel2
         * @return this instance for method chaining
         */
        public RichTextEditorI18n getPlaceholderAppearanceLabel2(
                String placeholderAppearanceLabel2) {
            this.placeholderAppearanceLabel2 = placeholderAppearanceLabel2;
            return this;
        }

        /**
         * Gets the translated word for {@code placeholderDialogTitle}
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
        public RichTextEditorI18n getPlaceholderDialogTitle(
                String placeholderDialogTitle) {
            this.placeholderDialogTitle = placeholderDialogTitle;
            return this;
        }

        /**
         * Gets the translated word for {@code clean}
         *
         * @return the translated word for clean
         */
        public String getClean() {
            return clean;
        }

        /**
         * Sets the translated word for {@code clean}.
         *
         * @param clean
         *            the translated word for clean
         * @return this instance for method chaining
         */
        public RichTextEditorI18n setClean(String clean) {
            this.clean = clean;
            return this;
        }

        /**
         * Gets the stringified values of the tooltips.
         *
         * @return stringified values of the tooltips
         */
        @Override
        public String toString() {
            return "[" + undo + ", " + redo + ", " + bold + ", " + italic + ", "
                    + underline + ", " + strike + ", " + h1 + ", " + h2 + ", "
                    + h3 + ", " + subscript + ", " + superscript + ", "
                    + listOrdered + ", " + listBullet + ", " + alignLeft + ", "
                    + alignCenter + ", " + alignRight + ", " + image + ", "
                    + link + ", " + blockquote + ", " + codeBlock + ", "
                    + readonly + ", " + placeholder + ", " + placeholderAppearance
                    + ", " + placeholderComboBoxLabel + ", "
                    + placeholderAppearanceLabel1 + ", "
                    + placeholderAppearanceLabel2 + ", "
                    + placeholderDialogTitle + ", " + clean + "]";
        }
    }

    public enum ToolbarButton {
        UNDO, REDO, BOLD, ITALIC, UNDERLINE, STRIKE, H1, H2, H3, SUBSCRIPT, SUPERSCRIPT, LIST_ORDERED, LIST_BULLET, ALIGN_LEFT, ALIGN_CENTER, ALIGN_RIGHT, IMAGE, LINK, BLOCKQUOTE, CODE_BLOCK, READONLY, CLEAN, PLACEHOLDER, PLACEHOLDER_APPEARANCE;

        @Override
        public String toString() {
            String str = this.name().toLowerCase();
            String[] parts = str.split("_");
            if (parts.length == 1)
                return "\"" + str + "\"";

            for (int i = 1; i < parts.length; i++)
                parts[i] = Character.toUpperCase(parts[i].charAt(0))
                        + parts[i].substring(1);

            return "\"" + String.join("", parts) + "\"";
        }
    }
}
