package com.vaadin.componentfactory.erte.tables.templates.events;

import com.vaadin.componentfactory.erte.tables.EnhancedRichTextEditorTables;
import tools.jackson.databind.node.ObjectNode;

public class TemplateDeletedEvent extends TemplateModificationEvent {

    public TemplateDeletedEvent(EnhancedRichTextEditorTables source, boolean fromClient, String templateId, ObjectNode deletedTemplate) {
        super(source, fromClient, templateId, deletedTemplate);
    }
}
