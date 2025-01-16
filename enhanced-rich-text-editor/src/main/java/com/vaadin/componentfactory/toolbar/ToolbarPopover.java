package com.vaadin.componentfactory.toolbar;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.popover.Popover;
import com.vaadin.flow.shared.Registration;

public class ToolbarPopover extends Popover {
    private Registration focusOnOpenTargetRegistration;

    public static ToolbarPopover vertical(ToolbarSwitch toolbarSwitch, Component... components) {
        ToolbarPopover popup = new ToolbarPopover(toolbarSwitch);
        popup.add(new VerticalLayout(components));
        return popup;
    }

    public static ToolbarPopover horizontal(ToolbarSwitch toolbarSwitch, Component... components) {
        ToolbarPopover popup = new ToolbarPopover(toolbarSwitch);
        HorizontalLayout layout = new HorizontalLayout(components);
        layout.setPadding(true);
        layout.setAlignItems(FlexComponent.Alignment.CENTER);
        popup.add(layout);
        return popup;
    }

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

    public void setFocusOnOpenTarget(Component component) {
        if (focusOnOpenTargetRegistration != null) {
            focusOnOpenTargetRegistration.remove();
        }

        focusOnOpenTargetRegistration = addOpenedChangeListener(event -> {
            if (event.isOpened()) {
                component.getElement().callJsFunction("focus");
            }
        });

    }
}