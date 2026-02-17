package com.vaadin.componentfactory.toolbar;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.popover.Popover;
import com.vaadin.flow.shared.Registration;

/**
 * Popover, that is opened/closed by a toolbar switch.
 */
public class ToolbarPopover extends Popover {
    private Registration focusOnOpenTargetRegistration;

    /**
     * Creates a new instance listing the given components in a vertical order.
     *
     * @param toolbarSwitch switch to open the popover
     * @param components    content
     * @return new instance
     */
    public static ToolbarPopover vertical(ToolbarSwitch toolbarSwitch, Component... components) {
        ToolbarPopover popup = new ToolbarPopover(toolbarSwitch);
        popup.add(new VerticalLayout(components));
        return popup;
    }

    /**
     * Creates a new instance listing the given components in a horizontal order (center aligned).
     * @param toolbarSwitch switch to open the popover
     * @param components content
     * @return new instance
     */
    public static ToolbarPopover horizontal(ToolbarSwitch toolbarSwitch, Component... components) {
        return horizontal(toolbarSwitch, Alignment.CENTER, components);
    }

    /**
     * Creates a new instance listing the given components in a horizontal order with the given alignment.
     *
     * @param toolbarSwitch switch to open the popover
     * @param components    content
     * @return new instance
     */
    public static ToolbarPopover horizontal(ToolbarSwitch toolbarSwitch, Alignment alignment, Component... components) {
        ToolbarPopover popup = new ToolbarPopover(toolbarSwitch);
        HorizontalLayout layout = new HorizontalLayout(components);
        layout.setPadding(true);
        layout.setAlignItems(alignment);
        popup.add(layout);
        return popup;
    }

    /**
     * Creates a new instance, that will open, when the given switch is active.
     * @param referencedSwitch switch to open the popup
     */
    public ToolbarPopover(ToolbarSwitch referencedSwitch) {
        setTarget(referencedSwitch);
        setAutofocus(true);

        //        setRestoreFocusOnClose(true); // not working with 24 anymore, so we set it manually
        getElement().setProperty("restoreFocusOnClose", true);

        addOpenedChangeListener(event -> referencedSwitch.setActive(event.isOpened()));

        referencedSwitch.addAttachListener(event -> {
            event.getSource().getParent().orElseThrow(IllegalStateException::new).getElement().appendChild(getElement());
        });

        referencedSwitch.addDetachListener(event -> {
            getElement().removeFromParent();
        });
    }

    /**
     * Allows to define a component, that should be focused initially, when opening this instance.
     *
     * @param focusOnOpenTarget initial focus target
     */
    public void setFocusOnOpenTarget(Component focusOnOpenTarget) {
        if (focusOnOpenTargetRegistration != null) {
            focusOnOpenTargetRegistration.remove();
        }

        focusOnOpenTargetRegistration = addOpenedChangeListener(event -> {
            if (event.isOpened()) {
                focusOnOpenTarget.getElement().callJsFunction("focus");
            }
        });

    }
}
