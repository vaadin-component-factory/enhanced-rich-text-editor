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

import java.util.Optional;
import java.util.stream.Stream;

import com.vaadin.componentfactory.toolbar.ToolbarSlot;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.dom.Element;

/**
 * Utility class for managing components in named toolbar slots.
 */
public final class SlotUtil {

    public static final String CUSTOM_GROUP_SLOTNAME =
            ToolbarSlot.GROUP_CUSTOM.getSlotName();

    private SlotUtil() {
        // utility class
    }

    private static Stream<Element> getElementsInSlot(HasElement target,
            String slot) {
        return target.getElement().getChildren()
                .filter(child -> slot.equals(child.getAttribute("slot")));
    }

    private static Stream<Component> getComponentsInSlot(HasElement target,
            String slot) {
        return getElementsInSlot(target, slot)
                .map(Element::getComponent)
                .filter(Optional::isPresent)
                .map(Optional::get);
    }

    /**
     * Adds a component to the given slot (appended at the end).
     */
    public static void addComponent(EnhancedRichTextEditor target,
            String slot, Component component) {
        if (component != null) {
            component.getElement().setAttribute("slot", slot);
            target.getElement().appendChild(component.getElement());
        }
    }

    /**
     * Adds a component to the given slot at a specific index.
     */
    public static void addComponentAtIndex(EnhancedRichTextEditor target,
            String slot, Component component, int index) {
        if (component != null) {
            component.getElement().setAttribute("slot", slot);
            target.getElement().insertChild(index, component.getElement());
        }
    }

    /**
     * Returns a component in the given slot matching the specified id.
     */
    public static Component getComponent(EnhancedRichTextEditor target,
            String slot, String id) {
        return getComponentsInSlot(target, slot)
                .filter(c -> id.equals(c.getId().orElse(null)))
                .findFirst()
                .orElse(null);
    }

    /**
     * Removes a component by id from the given slot.
     */
    public static void removeComponent(EnhancedRichTextEditor target,
            String slot, String id) {
        Component component = getComponent(target, slot, id);
        if (component != null) {
            component.getElement().removeFromParent();
        }
    }

    /**
     * Removes a specific component from the given slot.
     */
    public static void removeComponent(EnhancedRichTextEditor target,
            String slot, Component component) {
        getComponentsInSlot(target, slot)
                .filter(c -> c.equals(component))
                .findFirst()
                .map(Component::getElement)
                .ifPresent(Element::removeFromParent);
    }

    /**
     * Replaces a standard toolbar button icon via its named slot.
     * Pass {@code null} for icon to clear the slot and restore the default icon.
     */
    public static void replaceStandardButtonIcon(
            EnhancedRichTextEditor target, Icon icon, String iconSlotName) {
        // Always clear existing icon first
        clearSlot(target, iconSlotName);

        // If a new icon is provided, add it to the slot
        if (icon != null) {
            icon.getElement().setAttribute("slot", iconSlotName);
            target.getElement().appendChild(icon.getElement());
        }
    }

    /**
     * Adds a suffix icon to a button. The suffix icon is displayed smaller and
     * elevated as an overlay to the main button icon.
     */
    public static void addSuffixIcon(Button button, VaadinIcon icon) {
        Icon i = icon.create();
        addSuffixIcon(button, i);
    }

    /**
     * Adds a suffix icon component to a button. The suffix icon is displayed
     * smaller and elevated as an overlay to the main button icon.
     */
    public static void addSuffixIcon(Button button, Component icon) {
        icon.getElement().setAttribute("slot", "suffix");
        button.getElement().appendChild(icon.getElement());
        button.addClassName("suffix-icon");
    }

    private static void clearSlot(EnhancedRichTextEditor target,
            String slot) {
        getElementsInSlot(target, slot)
                .forEach(target.getElement()::removeChild);
    }
}
