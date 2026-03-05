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

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.popover.Popover;
import com.vaadin.flow.shared.Registration;

/**
 * A specialized Popover component that integrates with {@link ToolbarSwitch}
 * for opening and closing. The popover automatically syncs its opened state
 * with the switch's active state, and provides convenient factory methods
 * for creating vertical and horizontal layouts.
 *
 * <p><b>Example usage:</b>
 * <pre>{@code
 * ToolbarSwitch switch = new ToolbarSwitch(VaadinIcon.PAINTBRUSH);
 * editor.addComponentToSlot(ToolbarSlot.GROUP_CUSTOM, switch);
 *
 * TextField colorField = new TextField("Color");
 * ToolbarPopover popover = ToolbarPopover.vertical(switch, colorField);
 * }</pre>
 *
 * <p>The popover automatically:
 * <ul>
 *   <li>Opens/closes when the switch is activated/deactivated
 *   <li>Updates the switch state when opened/closed programmatically
 *   <li>Sets focus to the first focusable element (or custom target via {@link #setFocusOnOpenTarget})
 * </ul>
 *
 * @see ToolbarSwitch
 * @see com.vaadin.flow.component.popover.Popover
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
     *
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
     * @param alignment     the vertical alignment of the components
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
     *
     * @param referencedSwitch switch to open the popup
     */
    public ToolbarPopover(ToolbarSwitch referencedSwitch) {
        setTarget(referencedSwitch);
        setAutofocus(true);

        // Sync popover opened state with switch active state
        addOpenedChangeListener(event -> referencedSwitch.setActive(event.isOpened()));

        // Note: V25 Popover handles restoreFocusOnClose internally via the web component
        // Note: V25 Popover handles attach/detach via setTarget() - no manual listeners needed
    }

    /**
     * Allows to define a component, that should be focused initially, when opening this instance.
     * <p>
     * Note: Setting a custom focus target disables the default autofocus behavior to avoid conflicts.
     *
     * @param focusOnOpenTarget initial focus target
     */
    public void setFocusOnOpenTarget(Component focusOnOpenTarget) {
        // Disable autofocus to avoid conflict with custom target
        setAutofocus(false);

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
