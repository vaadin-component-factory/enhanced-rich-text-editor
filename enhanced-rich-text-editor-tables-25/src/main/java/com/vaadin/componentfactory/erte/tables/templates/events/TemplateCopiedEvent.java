package com.vaadin.componentfactory.erte.tables.templates.events;

import com.vaadin.componentfactory.erte.tables.EnhancedRichTextEditorTables;
import tools.jackson.databind.node.ObjectNode;

import java.util.Optional;

public class TemplateCopiedEvent extends TemplateCreatedEvent {

    private final String copiedTemplateId;

    public TemplateCopiedEvent(EnhancedRichTextEditorTables source, boolean fromClient, String templateId, String copiedTemplateId, ObjectNode template) {
        super(source, fromClient, templateId, template);
        this.copiedTemplateId = copiedTemplateId;
    }

    public Optional<String> getCopiedTemplateId() {
        return Optional.ofNullable(copiedTemplateId);
    }
}
