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

import com.vaadin.componentfactory.SlotUtil;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.shared.Registration;

/**
 * A toolbar toggle button. Clicking toggles between active (on) and inactive
 * states. The active state is reflected via an {@code on} HTML attribute.
 */
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

    public ToolbarSwitch(VaadinIcon icon) {
        super(icon.create());
        init();
    }

    /**
     * Creates a new instance with the given icons. The second icon is displayed
     * smaller and elevated as an addition to the first icon.
     *
     * @param icon       main icon
     * @param suffixIcon suffix / overlay icon
     */
    public ToolbarSwitch(VaadinIcon icon, VaadinIcon suffixIcon) {
        this(icon.create(), suffixIcon.create());
    }

    /**
     * Creates a new instance with the given icons. The second icon is displayed
     * smaller and elevated as an addition to the first icon.
     *
     * @param icon       main icon
     * @param suffixIcon suffix / overlay icon
     */
    public ToolbarSwitch(Component icon, Component suffixIcon) {
        super(icon);
        init();
        SlotUtil.addSuffixIcon(this, suffixIcon);
    }

    public ToolbarSwitch(String text, Component icon) {
        super(text, icon);
        init();
    }

    private void init() {
        addClickListener(event -> updateActive(!active, event.isFromClient()));
    }

    /**
     * Toggles the active state.
     *
     * @return the new active state
     */
    public boolean toggle() {
        setActive(!active);
        return active;
    }

    /**
     * Sets the active state programmatically.
     */
    public void setActive(boolean active) {
        updateActive(active, false);
    }

    private void updateActive(boolean active, boolean fromClient) {
        if (this.active != active) {
            this.active = active;
            if (active) {
                getElement().setAttribute("on", true );
            } else {
                getElement().removeAttribute("on");
            }
            fireEvent(new ActiveChangedEvent(this, fromClient));
        }
    }

    /**
     * Returns whether this switch is currently active.
     */
    public boolean isActive() {
        return active;
    }

    /**
     * Adds a listener for active-state changes.
     */
    public Registration addActiveChangedListener(
            ComponentEventListener<ActiveChangedEvent> listener) {
        return addListener(ActiveChangedEvent.class, listener);
    }

    /**
     * Fired when the switch's active state changes.
     */
    public static class ActiveChangedEvent extends ComponentEvent<ToolbarSwitch> {

        public ActiveChangedEvent(ToolbarSwitch source, boolean fromClient) {
            super(source, fromClient);
        }

        public boolean isActive() {
            return getSource().isActive();
        }
    }
}
