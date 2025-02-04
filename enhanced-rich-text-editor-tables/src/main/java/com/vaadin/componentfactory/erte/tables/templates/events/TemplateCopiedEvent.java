package com.vaadin.componentfactory.erte.tables.templates.events;

import com.vaadin.componentfactory.erte.tables.EnhancedRichTextEditorTables;
import elemental.json.JsonObject;

import java.util.Optional;

/**
 * Fired, when a new template has been created. Contains the id of the created template.
 * <p/>
 * This event is also fired, when a copy of an existing template is created. In that case the {@link #copiedTemplateId}
 * contains the origin template id.
 */
public class TemplateCopiedEvent extends TemplateCreatedEvent {

    private final String copiedTemplateId;

    public TemplateCopiedEvent(EnhancedRichTextEditorTables source, boolean fromClient, String templateId, String copiedTemplateId, JsonObject template) {
        super(source, fromClient, templateId, template);
        this.copiedTemplateId = copiedTemplateId;
    }

    /**
     * If the new template is a copy of an existing template, this method returns the origin template id.
     * @return origin template id (empty if no copy)
     */
    public Optional<String> getCopiedTemplateId() {
        return Optional.ofNullable(copiedTemplateId);
    }
}