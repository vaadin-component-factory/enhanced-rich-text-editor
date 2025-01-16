package com.vaadin.componentfactory.toolbar;

import com.vaadin.flow.component.contextmenu.ContextMenu;
import com.vaadin.flow.shared.Registration;

public class ToolbarSelectPopup extends ContextMenu {
    private Registration focusOnOpenTargetRegistration;

    public ToolbarSelectPopup(ToolbarSwitch referencedSwitch) {
        super(referencedSwitch);
        setOpenOnClick(true);
        addOpenedChangeListener(event -> referencedSwitch.setActive(event.isOpened()));
    }
}