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
package com.vaadin.componentfactory.toolbar;

/**
 * Named toolbar slots where custom components can be placed.
 * <p>
 * {@link #START} and {@link #END} are always the first and last positions.
 * All other slots are placed before and after their respective button group.
 * <p>
 * V25 has 11 standard groups (the "style" group containing color/background
 * is new compared to V24's 10 groups), yielding 27 enum values total.
 */
public enum ToolbarSlot {

    /** The very first slot in the toolbar. */
    START("toolbar-start"),

    /** Slot before the history group (undo, redo). */
    BEFORE_GROUP_HISTORY("toolbar-before-group-history"),
    /** Slot after the history group. */
    AFTER_GROUP_HISTORY("toolbar-after-group-history"),

    /** Slot before the emphasis group (bold, italic, underline, strike). */
    BEFORE_GROUP_EMPHASIS("toolbar-before-group-emphasis"),
    /** Slot after the emphasis group. */
    AFTER_GROUP_EMPHASIS("toolbar-after-group-emphasis"),

    /** Slot before the style group (color, background). New in V25. */
    BEFORE_GROUP_STYLE("toolbar-before-group-style"),
    /** Slot after the style group. New in V25. */
    AFTER_GROUP_STYLE("toolbar-after-group-style"),

    /** Slot before the heading group (h1, h2, h3). */
    BEFORE_GROUP_HEADING("toolbar-before-group-heading"),
    /** Slot after the heading group. */
    AFTER_GROUP_HEADING("toolbar-after-group-heading"),

    /** Slot before the glyph transformation group (subscript, superscript). */
    BEFORE_GROUP_GLYPH_TRANSFORMATION("toolbar-before-group-glyph-transformation"),
    /** Slot after the glyph transformation group. */
    AFTER_GROUP_GLYPH_TRANSFORMATION("toolbar-after-group-glyph-transformation"),

    /** Slot before the list group (ordered, bullet). */
    BEFORE_GROUP_LIST("toolbar-before-group-list"),
    /** Slot after the list group. */
    AFTER_GROUP_LIST("toolbar-after-group-list"),

    /** Slot before the indent group (outdent, indent). */
    BEFORE_GROUP_INDENT("toolbar-before-group-indent"),
    /** Slot after the indent group. */
    AFTER_GROUP_INDENT("toolbar-after-group-indent"),

    /** Slot before the alignment group (left, center, right). */
    BEFORE_GROUP_ALIGNMENT("toolbar-before-group-alignment"),
    /** Slot after the alignment group. */
    AFTER_GROUP_ALIGNMENT("toolbar-after-group-alignment"),

    /** Slot before the rich text group (image, link). */
    BEFORE_GROUP_RICH_TEXT("toolbar-before-group-rich-text"),
    /** Slot after the rich text group. */
    AFTER_GROUP_RICH_TEXT("toolbar-after-group-rich-text"),

    /** Slot before the block group (blockquote, code-block). */
    BEFORE_GROUP_BLOCK("toolbar-before-group-block"),
    /** Slot after the block group. */
    AFTER_GROUP_BLOCK("toolbar-after-group-block"),

    /** Slot before the format group (clean). */
    BEFORE_GROUP_FORMAT("toolbar-before-group-format"),
    /** Slot after the format group. */
    AFTER_GROUP_FORMAT("toolbar-after-group-format"),

    /** Slot before the custom group. */
    BEFORE_GROUP_CUSTOM("toolbar-before-group-custom"),

    /**
     * The custom group slot. Legacy name "toolbar" for backward compatibility.
     * Used by extensions (e.g. tables addon) to add buttons at the end.
     */
    GROUP_CUSTOM("toolbar"),

    /** Slot after the custom group. */
    AFTER_GROUP_CUSTOM("toolbar-after-group-custom"),

    /** The very last slot in the toolbar. */
    END("toolbar-end");

    private final String slotName;

    ToolbarSlot(String slotName) {
        this.slotName = slotName;
    }

    /**
     * Returns the slot name string used in the DOM {@code slot} attribute.
     *
     * @return slot name
     */
    public String getSlotName() {
        return slotName;
    }
}
