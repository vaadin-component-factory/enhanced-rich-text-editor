package com.vaadin.componentfactory.erte.tables.templates.events;

import com.vaadin.componentfactory.erte.tables.EnhancedRichTextEditorTables;
import com.vaadin.componentfactory.erte.tables.EnhancedRichTextEditorTablesComponentEvent;

public class TemplateSelectedEvent extends EnhancedRichTextEditorTablesComponentEvent {
    private final String templateId;

    public TemplateSelectedEvent(EnhancedRichTextEditorTables source, boolean fromClient, String templateId) {
        super(source, fromClient);
        this.templateId = templateId;
    }

    public String getTemplateId() { return templateId; }
}
