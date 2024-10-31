package com.vaadin.componentfactory.erte.tables.templates.events;

import com.vaadin.componentfactory.erte.tables.EnhancedRichTextEditorTables;
import com.vaadin.componentfactory.erte.tables.EnhancedRichTextEditorTablesComponentEvent;

/**
 * This event is fired, when the active template for the selected template has changed. This includes deselection.
 */
public class TemplateSelectedEvent extends EnhancedRichTextEditorTablesComponentEvent {
    private final String templateId;

    public TemplateSelectedEvent(EnhancedRichTextEditorTables source, boolean fromClient, String templateId) {
        super(source, fromClient);
        this.templateId = templateId;
    }

    /**
     * Returns the id of the selected template.
     * @return template
     */
    public String getTemplateId() {
        return templateId;
    }
}