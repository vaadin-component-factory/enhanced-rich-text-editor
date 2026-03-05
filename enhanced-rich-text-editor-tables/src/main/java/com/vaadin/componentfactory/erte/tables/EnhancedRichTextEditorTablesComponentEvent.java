/*-
 * #%L
 * Enhanced Rich Text Editor Tables Extension V25
 * %%
 * Copyright (C) 2025 Vaadin Ltd
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
package com.vaadin.componentfactory.erte.tables;

import com.vaadin.componentfactory.EnhancedRichTextEditor;
import com.vaadin.flow.component.ComponentEvent;

/**
 * Base class for all table extension events. Extends {@code ComponentEvent<EnhancedRichTextEditor>}
 * to allow registration on the editor component via {@code ComponentUtil}. Use {@link #getTableExtension()}
 * to access the {@link EnhancedRichTextEditorTables} instance that fired the event.
 */
public abstract class EnhancedRichTextEditorTablesComponentEvent extends ComponentEvent<EnhancedRichTextEditor> {
    private final EnhancedRichTextEditorTables tables;

    public EnhancedRichTextEditorTablesComponentEvent(EnhancedRichTextEditorTables source, boolean fromClient) {
        super(source.getRte(), fromClient);
        tables = source;
    }

    /**
     * Returns the table extension instance that fired this event. Use this to access table-specific API
     * methods from within event handlers.
     *
     * @return the table extension instance
     */
    public EnhancedRichTextEditorTables getTableExtension() {
        return tables;
    }
}
