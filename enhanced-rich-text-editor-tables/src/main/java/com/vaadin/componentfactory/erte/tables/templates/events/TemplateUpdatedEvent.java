package com.vaadin.componentfactory.erte.tables.templates.events;

import com.vaadin.componentfactory.erte.tables.EnhancedRichTextEditorTables;
import com.vaadin.componentfactory.erte.tables.templates.TemplateParser;
import elemental.json.JsonObject;

/**
 * Fired, when template content has been updated. Contains the id of the modified template and its content.
 */
public class TemplateUpdatedEvent extends TemplateModificationEvent {

    public TemplateUpdatedEvent(EnhancedRichTextEditorTables source, boolean fromClient, String templateId, JsonObject updatedTemplate) {
        super(source, fromClient, templateId, updatedTemplate);
    }

}