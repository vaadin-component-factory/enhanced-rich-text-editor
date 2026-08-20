package com.vaadin.componentfactory;

import com.vaadin.flow.component.Synchronize;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies that the {@code tabStops} property is bound to the client-side
 * {@code tab-stops-changed} event. Without that binding the server never learns
 * about tabstop changes made by the user in the ruler, so
 * {@link EnhancedRichTextEditor#getTabStops()} returns a stale value.
 * <p>
 * This is the headless half of the guard; that the event actually fires in the
 * browser is covered by the {@code Server Synchronization} tests in
 * {@code erte/tabstops.spec.ts}.
 */
class TabStopSynchronizationTest {

    @Test
    void getTabStopsIsSynchronizedOnTabStopsChangedEvent() throws Exception {
        Synchronize synchronize = EnhancedRichTextEditor.class
                .getMethod("getTabStops").getAnnotation(Synchronize.class);

        assertNotNull(synchronize,
                "getTabStops() must be annotated with @Synchronize, otherwise "
                        + "ruler changes never reach the server");
        assertEquals("tabStops", synchronize.property());
        assertArrayEquals(new String[] { "tab-stops-changed" },
                synchronize.value());
    }
}
