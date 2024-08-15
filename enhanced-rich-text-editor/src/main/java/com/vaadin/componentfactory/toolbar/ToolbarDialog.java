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
    private boolean openAtSwitch;
    private boolean ignoreNextEvent;

    public ToolbarDialog(ToolbarSwitch toolbarSwitch) {
        this(toolbarSwitch, false);
    }

    public ToolbarDialog(ToolbarSwitch toolbarSwitch, boolean openAtSwitch) {
        this.toolbarSwitch = toolbarSwitch;
        this.openAtSwitch = openAtSwitch;

        toolbarSwitch.addActiveChangedListener(event -> {
            if (!ignoreNextEvent) {
                ignoreNextEvent = true;
                setOpened(event.isActive());
                ignoreNextEvent = false;
            }
        });

        if (toolbarSwitch.isActive()) {
            setOpened(true);
        }

        addOpenedChangeListener(event -> {
            if (!ignoreNextEvent) {
                ignoreNextEvent = true;
                toolbarSwitch.setActive(event.isOpened());
                ignoreNextEvent = false;
            }
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

        addOpenedChangeListener(event -> {
            if (this.openAtSwitch && event.isOpened()) {
                getElement().executeJs("""
                                const {left, top, width, height} = $0.getBoundingClientRect();
                                this.$.overlay.$.overlay.style.position = 'absolute';
                                this.$.overlay.$.overlay.style.left = left + 'px';
                                this.$.overlay.$.overlay.style.top = top + height + 'px';""",
                        getToolbarSwitch());
            }
        });
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

    /**
     * Will set this dialog to open at the related toolbar switch.
     * @return open at toolbar swith
     */
    public ToolbarDialog openAtSwitch() {
        this.openAtSwitch = true;
        return this;
    }
}