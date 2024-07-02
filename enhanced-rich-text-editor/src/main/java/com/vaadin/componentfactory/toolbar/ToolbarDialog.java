package com.vaadin.componentfactory.toolbar;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.dialog.DialogVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.shared.Registration;

public class ToolbarDialog extends Dialog {
    private static final String SET_PROPERTY_IN_OVERLAY_JS = "this.$.overlay.$.overlay.style[$0]=$1";

    private final ToolbarSwitch toolbarSwitch;
    private Registration focusOnOpenTargetRegistration;

    public ToolbarDialog(ToolbarSwitch toolbarSwitch) {
        this.toolbarSwitch = toolbarSwitch;
        toolbarSwitch.addActiveChangedListener(event -> {
            if (event.isFromClient()) {
                setOpened(event.isActive());
            }
        });

        if (toolbarSwitch.isActive()) {
            setOpened(true);
        }

        addOpenedChangeListener(event -> {
            toolbarSwitch.setActive(event.isOpened());
            if (!event.isOpened()) {
                toolbarSwitch.focus();
            }
        });

        // default settings
        setCloseOnOutsideClick(false);
        setCloseOnEsc(true); // not sure?
        setModal(false);
        setResizable(true);
        setDraggable(true);
        addThemeVariants(DialogVariant.LUMO_NO_PADDING);

    }

    public static ToolbarDialog vertical(ToolbarSwitch toolbarSwitch, Component... components) {
        ToolbarDialog dialog = new ToolbarDialog(toolbarSwitch);
        dialog.add(new VerticalLayout(components));
        return dialog;
    }

    public static ToolbarDialog horizontal(ToolbarSwitch toolbarSwitch, Component... components) {
        ToolbarDialog dialog = new ToolbarDialog(toolbarSwitch);
        HorizontalLayout layout = new HorizontalLayout(components);
        layout.setPadding(true);
        layout.setAlignItems(FlexComponent.Alignment.CENTER);
        dialog.add(layout);
        return dialog;
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

    public ToolbarSwitch getToolbarSwitch() {
        return toolbarSwitch;
    }
}