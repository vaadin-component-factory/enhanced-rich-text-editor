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
package com.vaadin.componentfactory.erte.tables.templates.events;

import com.vaadin.componentfactory.erte.tables.EnhancedRichTextEditorTables;
import com.vaadin.componentfactory.erte.tables.EnhancedRichTextEditorTablesComponentEvent;
import tools.jackson.databind.node.ObjectNode;

public class TemplatesInitializedEvent extends EnhancedRichTextEditorTablesComponentEvent {
    private final ObjectNode templates;
    private final String cssString;

    public TemplatesInitializedEvent(EnhancedRichTextEditorTables source, boolean fromClient,
                                     ObjectNode templates, String cssString) {
        super(source, fromClient);
        this.templates = templates;
        this.cssString = cssString;
    }

    public ObjectNode getTemplates() {
        return templates;
    }

    public String getCssString() {
        return cssString;
    }
}
