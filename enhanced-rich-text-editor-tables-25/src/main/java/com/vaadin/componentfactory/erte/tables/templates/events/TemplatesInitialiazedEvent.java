package com.vaadin.componentfactory.erte.tables.templates.events;

import com.vaadin.componentfactory.erte.tables.EnhancedRichTextEditorTables;
import com.vaadin.componentfactory.erte.tables.EnhancedRichTextEditorTablesComponentEvent;
import tools.jackson.databind.node.ObjectNode;

public class TemplatesInitialiazedEvent extends EnhancedRichTextEditorTablesComponentEvent {
    private final ObjectNode templates;
    private final String cssString;

    public TemplatesInitialiazedEvent(EnhancedRichTextEditorTables source, boolean fromClient, ObjectNode templates, String cssString) {
        super(source, fromClient);
        this.templates = templates;
        this.cssString = cssString;
    }

    public ObjectNode getTemplates() { return templates; }
    public String getCssString() { return cssString; }
}
