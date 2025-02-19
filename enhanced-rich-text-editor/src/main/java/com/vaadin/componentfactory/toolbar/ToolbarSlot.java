package com.vaadin.componentfactory.toolbar;

/**
 * <p>
 * A list of available slots, where custom toolbar components can be added.
 * </p><p>
 * There are two special slots
 * {@link #START} and {@link #END}. These will be always the first and last slot where
 * elements can be added.
 * </p><p>
 * All other GROUP slots are placed before and after the respective button group. If the respective button
 * group is moved in future, their slots will be moved accordingly.
 * </p>
 */
public enum ToolbarSlot {

    /**
     * The very first / starting slot of the toolbar.
     */
    START("toolbar-start"),

    /**
     * Slot before the history group (contains "undo" etc.).
     */
    BEFORE_GROUP_HISTORY("toolbar-before-group-history"),

    /**
     * Slot after the history group (contains "undo" etc.).
     */
    AFTER_GROUP_HISTORY("toolbar-after-group-history"),

    /**
     * Slot before the emphasis group (contains "bold" etc.).
     */
    BEFORE_GROUP_EMPHASIS("toolbar-before-group-emphasis"),

    /**
     * Slot after the emphasis group (contains "bold" etc.).
     */
    AFTER_GROUP_EMPHASIS("toolbar-after-group-emphasis"),

    /**
     * Slot before the heading group.
     */
    BEFORE_GROUP_HEADING("toolbar-before-group-heading"),

    /**
     * Slot after the heading group.
     */
    AFTER_GROUP_HEADING("toolbar-after-group-heading"),

    /**
     * Slot before the glyph transformation group (contains "subscript" etc.).
     */
    BEFORE_GROUP_GLYPH_TRANSFORMATION("toolbar-before-group-glyph-transformation"),

    /**
     * Slot after the glyph transformation group (contains "subscript" etc.).
     */
    AFTER_GROUP_GLYPH_TRANSFORMATION("toolbar-after-group-glyph-transformation"),

    /**
     * Slot before the list group.
     */
    BEFORE_GROUP_LIST("toolbar-before-group-list"),

    /**
     * Slot after the list group.
     */
    AFTER_GROUP_LIST("toolbar-after-group-list"),

    /**
     * Slot before the indent group.
     */
    BEFORE_GROUP_INDENT("toolbar-before-group-indent"),

    /**
     * Slot after the indent group.
     */
    AFTER_GROUP_INDENT("toolbar-after-group-indent"),

    /**
     * Slot before the alignment group.
     */
    BEFORE_GROUP_ALIGNMENT("toolbar-before-group-alignment"),

    /**
     * Slot after the alignment group.
     */
    AFTER_GROUP_ALIGNMENT("toolbar-after-group-alignment"),

    /**
     * Slot before the rich text group (contains "image" etc.).
     */
    BEFORE_GROUP_RICH_TEXT("toolbar-before-group-rich-text"),

    /**
     * Slot after the rich text group (contains "image" etc.).
     */
    AFTER_GROUP_RICH_TEXT("toolbar-after-group-rich-text"),

    /**
     * Slot before the block group (contains "quote" etc.).
     */
    BEFORE_GROUP_BLOCK("toolbar-before-group-block"),

    /**
     * Slot after the block group (contains "quote" etc.).
     */
    AFTER_GROUP_BLOCK("toolbar-after-group-block"),

    /**
     * Slot before the format group (contains "readonly" etc.).
     */
    BEFORE_GROUP_FORMAT("toolbar-before-group-format"),

    /**
     * Slot after the format group (contains "readonly" etc.).
     */
    AFTER_GROUP_FORMAT("toolbar-after-group-format"),

    /**
     * Slot before the custom group. See {@link #GROUP_CUSTOM}.
     */
    BEFORE_GROUP_CUSTOM("toolbar-before-group-custom"),

    /**
     * The custom button slots. The custom group is a legacy group from previous versions, that allowed
     * adding items at the end of the toolbar. Might be used by extensions (like the table extension).
     */
    GROUP_CUSTOM("toolbar"),

    /**
     * Slot after the custom group. See {@link #GROUP_CUSTOM}.
     */
    AFTER_GROUP_CUSTOM("toolbar-after-group-custom"),

    /**
     * The very last / ending slot of the toolbar.
     */
    END("toolbar-end");

    private final String slotName;

    ToolbarSlot(String slotName) {
        this.slotName = slotName;
    }

    /**
     * Returns the string value of the slot name.
     * @return The slot name string.
     */
    public String getSlotName() {
        return slotName;
    }
}
