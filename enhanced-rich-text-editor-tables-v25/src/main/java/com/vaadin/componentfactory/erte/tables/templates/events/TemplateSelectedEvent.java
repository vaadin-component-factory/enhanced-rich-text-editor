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

public class TemplateSelectedEvent extends EnhancedRichTextEditorTablesComponentEvent {
    private final String templateId;

    public TemplateSelectedEvent(EnhancedRichTextEditorTables source, boolean fromClient, String templateId) {
        super(source, fromClient);
        this.templateId = templateId;
    }

    public String getTemplateId() {
        return templateId;
    }
}
