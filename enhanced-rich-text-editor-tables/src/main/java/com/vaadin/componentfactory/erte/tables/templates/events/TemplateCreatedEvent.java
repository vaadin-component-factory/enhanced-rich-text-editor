package com.vaadin.componentfactory.erte.tables.templates.events;

import com.vaadin.componentfactory.erte.tables.EnhancedRichTextEditorTables;
import elemental.json.JsonObject;

/**
 * Fired, when a new template has been created. Contains the id of the created template.
 * <p/>
 * This event is also fired, when a copy of an existing template is created. In that case the {@link #copiedTemplateId}
 * contains the origin template id.
 */
public class TemplateCreatedEvent extends TemplateModificationEvent {

    public TemplateCreatedEvent(EnhancedRichTextEditorTables source, boolean fromClient, String templateId, JsonObject template) {
        super(source, fromClient, templateId, template);
    }
}