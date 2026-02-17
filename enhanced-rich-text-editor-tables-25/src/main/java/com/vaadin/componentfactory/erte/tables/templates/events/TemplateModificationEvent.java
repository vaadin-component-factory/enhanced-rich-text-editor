package com.vaadin.componentfactory.erte.tables.templates.events;

import com.vaadin.componentfactory.erte.tables.EnhancedRichTextEditorTables;
import com.vaadin.componentfactory.erte.tables.EnhancedRichTextEditorTablesComponentEvent;
import com.vaadin.componentfactory.erte.tables.templates.TemplateParser;
import tools.jackson.databind.node.ObjectNode;

/**
 * Base class of all events, that are to be fired, when a specific template has been modified in any way.
 */
public abstract class TemplateModificationEvent extends EnhancedRichTextEditorTablesComponentEvent {

    private final String templateId;
    private final ObjectNode template;

    public TemplateModificationEvent(EnhancedRichTextEditorTables source, boolean fromClient, String templateId, ObjectNode template) {
        super(source, fromClient);
        this.templateId = templateId;
        this.template = TemplateParser.clone(template);
    }

    public String getTemplateId() { return templateId; }
    public ObjectNode getTemplate() { return template; }
}
