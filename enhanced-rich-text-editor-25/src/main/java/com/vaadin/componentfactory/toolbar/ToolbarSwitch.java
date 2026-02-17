package com.vaadin.componentfactory.toolbar;

import com.vaadin.componentfactory.SlotUtil;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.shared.Registration;

/**
 * A button for the toolbar, that provides a "switch" like functionality. Clicking it will toggle it to "active / on",
 * allowing to represent active functionality.
 */
public class ToolbarSwitch extends Button {

    private boolean active;

    /**
     * Creates a new instance.
     */
    public ToolbarSwitch() {
        init();
    }

    /**
     * Creates a new instance with the given text as its label.
     * @param text label text
     */
    public ToolbarSwitch(String text) {
        super(text);
        init();
    }

    /**
     * Creates a new instance with the given icon.
     * @param icon icon
     */
    public ToolbarSwitch(Component icon) {
        super(icon);
        init();
    }

    /**
     * Creates a new instance with the given icon.
     * @param icon icon
     */
    public ToolbarSwitch(VaadinIcon icon) {
        super(icon.create());
        init();
    }

    /**
     * Creates a new instance with the given icons. The second icon is displayed smaller and elevated as an
     * addition to the first icon.
     * @param icon icon
     * @param suffixIcon additonal / suffix icon
     */
    public ToolbarSwitch(VaadinIcon icon, VaadinIcon suffixIcon) {
        this(icon.create(), suffixIcon.create());
    }

    /**
     * Creates a new instance with the given icons. The second icon is displayed smaller and elevated as an
     * addition to the first icon.
     * @param icon icon
     * @param suffixIcon additonal / suffix icon
     */
    public ToolbarSwitch(Component icon, Component suffixIcon) {
        super(icon);
        init();
        SlotUtil.addSuffixIcon(this, suffixIcon);
    }

    /**
     * Creates a new instance with the given label text and icon.
     * @param text label text
     * @param icon icon
     */
    public ToolbarSwitch(String text, Component icon) {
        super(text, icon);
        init();
    }

    private void init() {
        addClickListener(event -> updateActivate(!active, event.isFromClient()));
    }

    /**
     * Toggles the "active" state.
     * @return new active state
     */
    public boolean toggle() {
        setActive(!active);
        return active;
    }

    /**
     * Sets an explicit active state
     * @param active new active state
     */
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

    /**
     * Indicates, if this switch is active or not.
     * @return is active
     */
    public boolean isActive() {
        return active;
    }

    /**
     * Adds a listener, that will be notified, when this switch is activated.
     * @param listener listener
     * @return registration to remove the listener
     */
    public Registration addActiveChangedListener(ComponentEventListener<ActiveChangedEvent> listener) {
        return addListener(ActiveChangedEvent.class, listener);
    }

    /**
     * This event is fired, when a switch's active state changes.
     */
    public static class ActiveChangedEvent extends ComponentEvent<ToolbarSwitch> {
        public ActiveChangedEvent(ToolbarSwitch source, boolean fromClient) {
            super(source, fromClient);
        }

        /**
         * Indicates, if this switch is active or not.
         * @return is active
         */
        public boolean isActive() {
            return getSource().isActive();
        }
    }

}
