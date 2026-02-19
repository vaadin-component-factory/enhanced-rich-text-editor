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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.vaadin.componentfactory.toolbar.ToolbarSlot;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.richtexteditor.RteExtensionBase;
import com.vaadin.flow.internal.JacksonUtils;

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
}
