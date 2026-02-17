package com.vaadin.componentfactory;

import java.util.Optional;
import java.util.stream.Stream;

import com.vaadin.componentfactory.toolbar.ToolbarSlot;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.richtexteditor.EnhancedRichTextEditor;
import com.vaadin.flow.dom.Element;

public class SlotUtil {

    public static final String CUSTOM_GROUP_SLOTNAME = ToolbarSlot.GROUP_CUSTOM.getSlotName();

    private static Stream<Element> getElementsInSlot(HasElement target,
                                                     String slot) {
        return target.getElement().getChildren()
                .filter(child -> slot.equals(child.getAttribute("slot")));
    }

    /**
     * Adds a button to the toolbar slot.
     * @param target editor instance
     * @param component button to add
     * @deprecated use {@link #addComponent(EnhancedRichTextEditor, Component)} instead
     */
    @Deprecated
    public static void addButton(EnhancedRichTextEditor target, Button component) {
        addComponent(target, component);
    }

    /**
     * Adds a component to the toolbar slot {@link ToolbarSlot#GROUP_CUSTOM}.
     * @param target editor instance
     * @param component component to add
     */
    public static void addComponent(EnhancedRichTextEditor target, Component component) {
        addComponent(target, CUSTOM_GROUP_SLOTNAME, component);
    }

    /**
     * Adds a component to the toolbar slot {@link ToolbarSlot#GROUP_CUSTOM}.
     * @param target editor instance
     * @param component component to add
     */
    public static void addComponentAtIndex(EnhancedRichTextEditor target, Component component, int index) {
        addComponentAtIndex(target, CUSTOM_GROUP_SLOTNAME, component, index);
    }

    /**
     * Adds a component to the toolbar slot {@link ToolbarSlot#GROUP_CUSTOM}.
     * @param target editor instance
     * @param slot slot name to place the component in
     * @param component component to add
     */
    public static void addComponent(EnhancedRichTextEditor target, String slot, Component component) {
        if (component != null) {
            component.getElement().setAttribute("slot", slot);
            target.getElement().appendChild(component.getElement());
        }
    }

    /**
     * Adds a component to the toolbar slot {@link ToolbarSlot#GROUP_CUSTOM}.
     * @param target editor instance
     * @param slot slot name to place the component in
     * @param component component to add
     * @param index relative index inside the slot
     */
    public static void addComponentAtIndex(EnhancedRichTextEditor target, String slot, Component component, int index) {
        if (component != null) {
            component.getElement().setAttribute("slot", slot);
            target.getElement().insertChild(index, component.getElement());
        }
    }

    public static void addSuffixIcon(Button button, VaadinIcon icon) {
        Icon i = icon.create();
        addSuffixIcon(button, i);
    }

    public static void addSuffixIcon(Button button, Component icon) {
        icon.getElement().setAttribute("slot", "suffix");
        button.getElement().appendChild(icon.getElement());
        button.addClassName("suffix-icon");
    }

    private static void clearSlot(EnhancedRichTextEditor target, String slot) {
        getElementsInSlot(target, slot).forEach(target.getElement()::removeChild);
    }

    private static Stream<Component> getComponentsInSlot(HasElement target, String slot) {
        return getElementsInSlot(target, slot)
                .map(Element::getComponent)
                .filter(Optional::isPresent)
                .map(Optional::get);
    }

    public static Component getComponent(EnhancedRichTextEditor target, String slot, String id) {
        return getComponentsInSlot(target, slot)
                .filter(component -> id.equals(component.getId().orElse(null)))
                .findFirst()
                .orElse(null);
    }

    public static void removeComponent(EnhancedRichTextEditor target, String slot, String id) {
        Component component = getComponent(target, slot, id);
        if (component != null) {
            component.getElement().removeFromParent();
        }
    }

    public static void removeComponent(EnhancedRichTextEditor target, String slot, Component component) {
        getComponentsInSlot(target, slot)
                .filter(c -> c.equals(component))
                .findFirst()
                .map(Component::getElement)
                .ifPresent(Element::removeFromParent);
    }

    public static void removeButton(EnhancedRichTextEditor target, String id) {
        removeComponent(target, CUSTOM_GROUP_SLOTNAME, id);
    }

    public static void removeButton(EnhancedRichTextEditor target, Button button) {
        removeComponent(target, CUSTOM_GROUP_SLOTNAME, button);
    }

    public static Button getButton(EnhancedRichTextEditor target, String id) {
        return (Button) getComponent(target, CUSTOM_GROUP_SLOTNAME, id);
    }

    public static void replaceStandardButtonIcon(EnhancedRichTextEditor target, Icon icon, String iconSlotName) {
        if (icon != null) {
            clearSlot(target, iconSlotName);
            icon.getElement().setAttribute("slot", iconSlotName);
            target.getElement().appendChild(icon.getElement());
        }
    }
}
