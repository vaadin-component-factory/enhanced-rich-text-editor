package com.vaadin.componentfactory.erte.tables.templates.events;

import com.vaadin.componentfactory.erte.tables.EnhancedRichTextEditorTables;
import com.vaadin.componentfactory.erte.tables.EnhancedRichTextEditorTablesComponentEvent;
import elemental.json.JsonObject;

/**
 * This event is fired, when the templates of the ERTE table instance are initialized or resetted. This means,
 * that the whole set of templates are changed.
 */
public class TemplatesInitialiazedEvent extends EnhancedRichTextEditorTablesComponentEvent {
    private final JsonObject templates;
    private final String cssString;

    public TemplatesInitialiazedEvent(EnhancedRichTextEditorTables source, boolean fromClient, JsonObject templates, String cssString) {
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

    /**
     * Returns the css string representing the templates.
     * @return css
     */
    public String getCssString() {
        return cssString;
    }
}