package com.vaadin.componentfactory.erte.tables.templates.events;

import com.vaadin.componentfactory.erte.tables.EnhancedRichTextEditorTables;
import tools.jackson.databind.node.ObjectNode;

public class TemplateCreatedEvent extends TemplateModificationEvent {

    public TemplateCreatedEvent(EnhancedRichTextEditorTables source, boolean fromClient, String templateId, ObjectNode template) {
        super(source, fromClient, templateId, template);
    }
}
