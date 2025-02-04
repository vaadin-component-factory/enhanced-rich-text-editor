package com.vaadin.componentfactory.toolbar;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.dialog.DialogVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.shared.Registration;

/**
 * A dialog, that can be opened/closed using a toolbar switch.
 */
public class ToolbarDialog extends Dialog {
    private final ToolbarSwitch toolbarSwitch;
    private Registration focusOnOpenTargetRegistration;
    private boolean openAtSwitch;
    private boolean ignoreNextEvent;

    /**
     * Creates a new instance, that opens/closes based on the given switch state.
     * @param toolbarSwitch switch
     */
    public ToolbarDialog(ToolbarSwitch toolbarSwitch) {
        this(toolbarSwitch, false);
    }

    /**
     * Creates a new instance, that opens/closes based on the given switch state. The boolean parameter defines, if
     * the dialog shall be opened aligned to the switch or not.
     * @param toolbarSwitch switch
     * @param openAtSwitch should be aligned to the switch
     */
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

    /**
     * Creates a new instance listing the given components in a vertical order.
     * @param toolbarSwitch switch to open the popover
     * @param components content
     * @return new instance
     */
    public static ToolbarDialog vertical(ToolbarSwitch toolbarSwitch, Component... components) {
        ToolbarDialog dialog = new ToolbarDialog(toolbarSwitch);
        dialog.add(new VerticalLayout(components));
        return dialog;
    }

    /**
     * Creates a new instance listing the given components in a horizontal order (center aligned).
     * @param toolbarSwitch switch to open the dialog
     * @param components content
     * @return new instance
     */
    public static ToolbarDialog horizontal(ToolbarSwitch toolbarSwitch, Component... components) {
        return horizontal(toolbarSwitch, FlexComponent.Alignment.CENTER, components);
    }


    /**
     * Creates a new instance listing the given components in a horizontal order with the given alignment.
     * @param toolbarSwitch switch to open the dialog
     * @param components content
     * @return new instance
     */
    public static ToolbarDialog horizontal(ToolbarSwitch toolbarSwitch, FlexComponent.Alignment alignment, Component... components) {
        ToolbarDialog dialog = new ToolbarDialog(toolbarSwitch);
        HorizontalLayout layout = new HorizontalLayout(components);
        layout.setPadding(true);
        layout.setAlignItems(alignment);
        dialog.add(layout);
        return dialog;
    }

    /**
     * Allows to define a component, that should be focused initially, when opening this instance.
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