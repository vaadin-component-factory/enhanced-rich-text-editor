/*-
 * #%L
 * Enhanced Rich Text Editor Tables Extension V25
 * %%
 * Copyright (C) 2025 Vaadin Ltd
 * %%
 * This program is available under Commercial Vaadin Add-On License 3.0
 * (CVALv3).
 *
 * See the file license.html distributed with this software for more
 * information about licensing.
 *
 * You should have received a copy of the CVALv3 along with this program.
 * If not, see <http://vaadin.com/license/cval-3>.
 * #L%
 */
package com.vaadin.componentfactory.erte.tables.templates.events;

import com.vaadin.componentfactory.erte.tables.EnhancedRichTextEditorTables;
import tools.jackson.databind.node.ObjectNode;
import java.util.Optional;

public class TemplateCopiedEvent extends TemplateCreatedEvent {
    private final String copiedTemplateId;

    public TemplateCopiedEvent(EnhancedRichTextEditorTables source, boolean fromClient,
                               String templateId, String copiedTemplateId, ObjectNode template) {
        super(source, fromClient, templateId, template);
        this.copiedTemplateId = copiedTemplateId;
    }

    /**
     * Returns the ID of the original template that was copied, wrapped in an Optional.
     *
     * @return an Optional containing the original template ID, or empty if not available
     */
    public Optional<String> getCopiedTemplateId() {
        return Optional.ofNullable(copiedTemplateId);
    }
}
