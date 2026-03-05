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

import com.vaadin.flow.component.contextmenu.ContextMenu;

/**
 * A specialized ContextMenu that integrates with {@link ToolbarSwitch}
 * for opening and closing. The menu automatically syncs its opened state
 * with the switch's active state.
 *
 * <p>The menu opens on left-click (not the default right-click behavior)
 * and is typically used for custom toolbar extensions that need dropdown
 * menus with multiple options.
 *
 * <p><b>Example usage:</b>
 * <pre>{@code
 * ToolbarSwitch formatSwitch = new ToolbarSwitch(VaadinIcon.COGS);
 * editor.addToolbarComponents(ToolbarSlot.GROUP_CUSTOM, formatSwitch);
 *
 * ToolbarSelectPopup popup = new ToolbarSelectPopup(formatSwitch);
 * popup.addItem("Bold", e -> editor.getEditor().format("bold", true));
 * popup.addItem("Italic", e -> editor.getEditor().format("italic", true));
 * popup.add(new Hr());
 * popup.addItem("Clear Formatting", e -> editor.getEditor().removeFormat(0, editor.getEditor().getLength()));
 * }</pre>
 *
 * @see ToolbarSwitch
 * @see com.vaadin.flow.component.contextmenu.ContextMenu
 */
public class ToolbarSelectPopup extends ContextMenu {

    /**
     * Creates a new context menu that opens when the given switch is clicked.
     * The menu opens on left-click (not the default right-click) and
     * automatically syncs the switch's active state with the menu's opened state.
     *
     * @param referencedSwitch the toolbar switch that triggers the menu
     */
    public ToolbarSelectPopup(ToolbarSwitch referencedSwitch) {
        super(referencedSwitch);
        setOpenOnClick(true);
        addOpenedChangeListener(event -> referencedSwitch.setActive(event.isOpened()));
    }
}
