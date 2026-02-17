package com.vaadin.componentfactory.toolbar;

import com.vaadin.flow.component.contextmenu.ContextMenu;
import com.vaadin.flow.shared.Registration;

/**
 * A context menu, that opens, when the referenced switch is active.
 */
public class ToolbarSelectPopup extends ContextMenu {
    private Registration focusOnOpenTargetRegistration;

    /**
     * Creates a new instance, opening, when the given switch is active.
     * @param referencedSwitch referenced switch
     */
    public ToolbarSelectPopup(ToolbarSwitch referencedSwitch) {
        super(referencedSwitch);
        setOpenOnClick(true);
        addOpenedChangeListener(event -> referencedSwitch.setActive(event.isOpened()));
    }
}
