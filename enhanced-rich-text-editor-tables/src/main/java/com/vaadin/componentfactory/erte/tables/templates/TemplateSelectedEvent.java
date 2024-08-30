package com.vaadin.componentfactory.erte.tables.templates;

import com.vaadin.componentfactory.erte.tables.EnhancedRichTextEditorTables;
import com.vaadin.componentfactory.erte.tables.EnhancedRichTextEditorTablesComponentEvent;

/**
 * This event is fired, when the active template for the selected template has changed. This includes deselection.
 */
public class TemplateSelectedEvent extends EnhancedRichTextEditorTablesComponentEvent {
    private final String template;

    public TemplateSelectedEvent(EnhancedRichTextEditorTables source, boolean fromClient, String template) {
        super(source, fromClient);
        this.template = template;
    }

    /**
     * Returns the name of the selected template.
     * @return template
     */
    public String getTemplate() {
        return template;
    }
}