package com.vaadin.componentfactory.erte.tables.templates.events;

import com.vaadin.componentfactory.erte.tables.EnhancedRichTextEditorTables;
import com.vaadin.componentfactory.erte.tables.EnhancedRichTextEditorTablesComponentEvent;
import com.vaadin.componentfactory.erte.tables.templates.TemplateParser;
import elemental.json.JsonObject;

/**
 * Base class of all events, that are to be fired, when a specific template has been modified in any way. This includes
 * the creation of a new template.
 */
public abstract class TemplateModificationEvent extends EnhancedRichTextEditorTablesComponentEvent {

    private final String templateId;
    private final JsonObject template;

    /**
     * Creates a new event using the given source and indicator whether the
     * event originated from the client side or the server side.
     *
     * @param source     the source component
     * @param fromClient <code>true</code> if the event originated from the client
     *                   side, <code>false</code> otherwise
     */
    public TemplateModificationEvent(EnhancedRichTextEditorTables source, boolean fromClient, String templateId, JsonObject template) {
        super(source, fromClient);
        this.templateId = templateId;
        this.template = TemplateParser.clone(template);
    }

    /**
     * Returns the affected template id.
     * @return template id
     */
    public String getTemplateId() {
        return templateId;
    }

    /**
     * Returns the template object resulting from the modification. Modifications to this object will NOT affect
     * the source.
     * @return template object
     */
    public JsonObject getTemplate() {
        return template;
    }
}