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
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.dialog.DialogVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.shared.Registration;

/**
 * A specialized Dialog that integrates with {@link ToolbarSwitch} for opening
 * and closing. The dialog automatically syncs its opened state with the
 * switch's active state.
 *
 * <p>The dialog can be configured to open at the switch's position using
 * {@link #openAtSwitch()}, which positions the dialog directly below the
 * toolbar button. This is useful for toolbar-attached controls that need
 * more space than a popover.
 *
 * <p><b>Default settings:</b>
 * <ul>
 *   <li>Non-modal (doesn't block interaction with the rest of the page)</li>
 *   <li>Resizable (can be resized by the user)</li>
 *   <li>Draggable (can be moved by the user)</li>
 *   <li>No padding (use layouts to control spacing)</li>
 *   <li>Closes on ESC key</li>
 *   <li>Does NOT close on outside click (to allow toolbar interaction)</li>
 * </ul>
 *
 * <p><b>Example usage (center-positioned):</b>
 * <pre>{@code
 * ToolbarSwitch settingsSwitch = new ToolbarSwitch(VaadinIcon.COG);
 * editor.addToolbarComponents(ToolbarSlot.GROUP_CUSTOM, settingsSwitch);
 *
 * ToolbarDialog settingsDialog = ToolbarDialog.vertical(settingsSwitch,
 *     new Checkbox("Show rulers"),
 *     new Checkbox("Show whitespace"),
 *     new Button("Close", e -> settingsDialog.close())
 * );
 * }</pre>
 *
 * <p><b>Example usage (positioned at switch):</b>
 * <pre>{@code
 * ToolbarSwitch formatSwitch = new ToolbarSwitch(VaadinIcon.TEXT_HEIGHT);
 * editor.addToolbarComponents(ToolbarSlot.GROUP_CUSTOM, formatSwitch);
 *
 * ToolbarDialog formatDialog = ToolbarDialog.horizontal(formatSwitch,
 *     new Button("Bold", e -> editor.getEditor().format("bold", true)),
 *     new Button("Italic", e -> editor.getEditor().format("italic", true))
 * ).openAtSwitch();
 * }</pre>
 *
 * @see ToolbarSwitch
 * @see ToolbarPopover
 * @see com.vaadin.flow.component.dialog.Dialog
 */
public class ToolbarDialog extends Dialog {
    private final ToolbarSwitch toolbarSwitch;
    private Registration focusOnOpenTargetRegistration;
    private boolean openAtSwitch;
    private boolean ignoreNextEvent;

    /**
     * Creates a new dialog that opens/closes based on the given switch state.
     * The dialog is center-positioned by default. Use {@link #openAtSwitch()}
     * to position it at the switch instead.
     *
     * @param toolbarSwitch the toolbar switch that controls the dialog
     */
    public ToolbarDialog(ToolbarSwitch toolbarSwitch) {
        this(toolbarSwitch, false);
    }

    /**
     * Creates a new dialog that opens/closes based on the given switch state.
     *
     * @param toolbarSwitch the toolbar switch that controls the dialog
     * @param openAtSwitch whether to position the dialog at the switch (true)
     *                     or center it (false)
     */
    public ToolbarDialog(ToolbarSwitch toolbarSwitch, boolean openAtSwitch) {
        this.toolbarSwitch = toolbarSwitch;
        this.openAtSwitch = openAtSwitch;

        // Bidirectional state sync between switch and dialog
        toolbarSwitch.addActiveChangedListener(event -> {
            if (!ignoreNextEvent) {
                ignoreNextEvent = true;
                setOpened(event.isActive());
                ignoreNextEvent = false;
            }
        });

        // Initialize opened state if switch is already active
        if (toolbarSwitch.isActive()) {
            setOpened(true);
        }

        addOpenedChangeListener(event -> {
            if (!ignoreNextEvent) {
                ignoreNextEvent = true;
                toolbarSwitch.setActive(event.isOpened());
                ignoreNextEvent = false;
            }
            // Return focus to switch when dialog closes
            if (!event.isOpened()) {
                toolbarSwitch.focus();
            }
        });

        // Default settings
        setCloseOnOutsideClick(false);
        setCloseOnEsc(true);
        setModal(false);
        setResizable(true);
        setDraggable(true);
        addThemeVariants(DialogVariant.LUMO_NO_PADDING);

        // Position dialog at switch if requested
        addOpenedChangeListener(event -> {
            if (this.openAtSwitch && event.isOpened()) {
                getElement().executeJs("""
                                const {left, top, width, height} = $0.getBoundingClientRect();
                                this.$.overlay.$.overlay.style.position = 'absolute';
                                this.$.overlay.$.overlay.style.left = left + 'px';
                                this.$.overlay.$.overlay.style.top = (top + height) + 'px';""",
                        getToolbarSwitch());
            }
        });
    }

    /**
     * Creates a new dialog with the given components arranged vertically.
     * The dialog is center-positioned by default.
     *
     * @param toolbarSwitch the toolbar switch that controls the dialog
     * @param components content components to arrange vertically
     * @return new dialog instance
     */
    public static ToolbarDialog vertical(ToolbarSwitch toolbarSwitch, Component... components) {
        ToolbarDialog dialog = new ToolbarDialog(toolbarSwitch);
        dialog.add(new VerticalLayout(components));
        return dialog;
    }

    /**
     * Creates a new dialog with the given components arranged horizontally
     * (center-aligned). The dialog is center-positioned by default.
     *
     * @param toolbarSwitch the toolbar switch that controls the dialog
     * @param components content components to arrange horizontally
     * @return new dialog instance
     */
    public static ToolbarDialog horizontal(ToolbarSwitch toolbarSwitch, Component... components) {
        return horizontal(toolbarSwitch, FlexComponent.Alignment.CENTER, components);
    }

    /**
     * Creates a new dialog with the given components arranged horizontally
     * with the specified alignment. The dialog is center-positioned by default.
     *
     * @param toolbarSwitch the toolbar switch that controls the dialog
     * @param alignment vertical alignment of components
     * @param components content components to arrange horizontally
     * @return new dialog instance
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
     * Sets a component to receive focus when the dialog opens.
     *
     * @param focusOnOpenTarget component to focus on open
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

    /**
     * Returns the toolbar switch that controls this dialog.
     *
     * @return the toolbar switch
     */
    public ToolbarSwitch getToolbarSwitch() {
        return toolbarSwitch;
    }

    /**
     * Configures the dialog to open at the toolbar switch's position instead
     * of being center-positioned. The dialog will appear directly below the
     * switch button.
     *
     * @return this dialog for method chaining
     */
    public ToolbarDialog openAtSwitch() {
        this.openAtSwitch = true;
        return this;
    }
}
