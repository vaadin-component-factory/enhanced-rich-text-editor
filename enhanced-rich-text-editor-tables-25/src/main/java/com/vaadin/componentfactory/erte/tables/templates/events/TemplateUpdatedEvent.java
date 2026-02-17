package com.vaadin.componentfactory.erte.tables.templates.events;

import com.vaadin.componentfactory.erte.tables.EnhancedRichTextEditorTables;
import tools.jackson.databind.node.ObjectNode;

public class TemplateUpdatedEvent extends TemplateModificationEvent {

    public TemplateUpdatedEvent(EnhancedRichTextEditorTables source, boolean fromClient, String templateId, ObjectNode updatedTemplate) {
        super(source, fromClient, templateId, updatedTemplate);
    }
}
