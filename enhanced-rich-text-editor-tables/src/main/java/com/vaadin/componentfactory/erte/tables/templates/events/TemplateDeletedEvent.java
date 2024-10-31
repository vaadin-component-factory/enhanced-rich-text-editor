package com.vaadin.componentfactory.erte.tables.templates.events;

import com.vaadin.componentfactory.erte.tables.EnhancedRichTextEditorTables;
import elemental.json.JsonObject;

/**
 * Fired, when a template has been deleted. Contains the id of the deleted template and its content.
 */
public class TemplateDeletedEvent extends TemplateModificationEvent {


    public TemplateDeletedEvent(EnhancedRichTextEditorTables source, boolean fromClient, String templateId, JsonObject deletedTemplate) {
        super(source, fromClient, templateId, deletedTemplate);
    }

}