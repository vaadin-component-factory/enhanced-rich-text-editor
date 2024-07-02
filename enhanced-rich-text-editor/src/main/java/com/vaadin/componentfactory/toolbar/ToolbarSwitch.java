package com.vaadin.componentfactory.toolbar;

import com.vaadin.componentfactory.SlotUtil;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.shared.Registration;

public class ToolbarSwitch extends Button {

    private boolean active;

    public ToolbarSwitch() {
        init();
    }

    public ToolbarSwitch(String text) {
        super(text);
        init();
    }

    public ToolbarSwitch(Component icon) {
        super(icon);
        init();
    }

    public ToolbarSwitch(VaadinIcon icon, VaadinIcon suffixIcon) {
        super(icon.create());
        init();
        SlotUtil.addSuffixIcon(this, suffixIcon);
    }

    public ToolbarSwitch(String text, Component icon) {
        super(text, icon);
        init();
    }

    private void init() {
        addClickListener(event -> updateActivate(!active, event.isFromClient()));
    }

    public boolean toggle() {
        setActive(!active);
        return active;
    }

    public void setActive(boolean active) {
        updateActivate(active, false);
    }

    private void updateActivate(boolean active, boolean fromClient) {
        if (this.active != active) {
            this.active = active;

            if (active) {
                getElement().setAttribute("on", "on");
            } else {
                getElement().removeAttribute("on");
            }

            fireEvent(new ActiveChangedEvent(this, fromClient));
        }
    }

    public boolean isActive() {
        return active;
    }

    public Registration addActiveChangedListener(ComponentEventListener<ActiveChangedEvent> listener) {
        return addListener(ActiveChangedEvent.class, listener);
    }

    public static class ActiveChangedEvent extends ComponentEvent<ToolbarSwitch> {
        public ActiveChangedEvent(ToolbarSwitch source, boolean fromClient) {
            super(source, fromClient);
        }

        public boolean isActive() {
            return getSource().isActive();
        }
    }

}