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

import java.util.Objects;

import com.vaadin.componentfactory.toolbar.ToolbarSlot;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.richtexteditor.RteExtensionBase;

/**
 * Enhanced Rich Text Editor — V25 / Quill 2.
 * <p>
 * Extends {@link RteExtensionBase} which bridges package-private access to
 * RTE 2. All ERTE-specific logic lives in this class and package.
 */
@Tag("vcf-enhanced-rich-text-editor")
@JsModule("./vcf-enhanced-rich-text-editor.js")
public class EnhancedRichTextEditor extends RteExtensionBase {

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
}
