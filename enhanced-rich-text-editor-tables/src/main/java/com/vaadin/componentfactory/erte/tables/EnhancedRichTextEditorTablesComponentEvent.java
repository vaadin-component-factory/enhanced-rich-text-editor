package com.vaadin.componentfactory.erte.tables;

import com.vaadin.componentfactory.EnhancedRichTextEditor;
import com.vaadin.flow.component.ComponentEvent;

/**
 * Base class for any component events, that are fired by the ERTE Table Extension. Provides additional information
 * regarding the Table Extension isntance.
 */
public abstract class EnhancedRichTextEditorTablesComponentEvent extends ComponentEvent<EnhancedRichTextEditor> {
    private final EnhancedRichTextEditorTables tables;

    /**
     * Creates a new event using the given source and indicator whether the
     * event originated from the client side or the server side.
     *
     * @param source     the source component
     * @param fromClient <code>true</code> if the event originated from the client
     *                   side, <code>false</code> otherwise
     */
    public EnhancedRichTextEditorTablesComponentEvent(EnhancedRichTextEditorTables source, boolean fromClient) {
        super(source.getRte(), fromClient);
        tables = source;
    }

    /**
     * Returns the table extension, that is applied to the editor source returned by this event.
     * @return
     */
    public EnhancedRichTextEditorTables getTableExtension() {
        return tables;
    }
}