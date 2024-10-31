package com.vaadin.componentfactory.erte.tables.templates.events;

import com.vaadin.componentfactory.erte.tables.EnhancedRichTextEditorTables;
import com.vaadin.componentfactory.erte.tables.EnhancedRichTextEditorTablesComponentEvent;
import elemental.json.JsonObject;

/**
 * This event is fired, when the templates of the ERTE table instance have changed, for instance, when a new template
 * has been added or styles have been modified.
 */
public class TemplatesChangedEvent extends EnhancedRichTextEditorTablesComponentEvent {
    private final JsonObject templates;
    private final String cssString;

    public TemplatesChangedEvent(EnhancedRichTextEditorTables source, boolean fromClient, JsonObject templates, String cssString) {
        super(source, fromClient);
        this.templates = templates;
        this.cssString = cssString;
    }

    /**
     * Checks if this event originated from the client side.
     *
     * @return <code>true</code> if the event originated from the client side,
     * <code>false</code> otherwise
     */
    public JsonObject getTemplates() {
        return templates;
    }

    public String getCssString() {
        return cssString;
    }
}