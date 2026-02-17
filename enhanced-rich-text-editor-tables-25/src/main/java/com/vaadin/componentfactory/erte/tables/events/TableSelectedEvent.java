package com.vaadin.componentfactory.erte.tables.events;

import com.vaadin.componentfactory.erte.tables.EnhancedRichTextEditorTables;
import com.vaadin.componentfactory.erte.tables.EnhancedRichTextEditorTablesComponentEvent;

/**
 * This event is fired, when a table has been (de-) selected in an ERTE instance.
 */
public class TableSelectedEvent extends EnhancedRichTextEditorTablesComponentEvent {
    private final boolean selected;
    private final boolean cellSelectionActive;
    private final String template;

    public TableSelectedEvent(
            EnhancedRichTextEditorTables source,
            boolean fromClient,
            boolean selected,
            boolean cellSelectionActive,
            String template
    ) {
        super(source, fromClient);

        if (template != null && !template.isBlank() && !template.matches("[A-Za-z0-9\\-]+")) {
            throw new IllegalArgumentException("Illegal template name: " + template);
        }

        this.selected = selected;
        this.cellSelectionActive = cellSelectionActive;
        this.template = template != null ? template.trim() : null;
    }

    public boolean isSelected() { return selected; }
    public boolean isCellSelectionActive() { return cellSelectionActive; }
    public String getTemplate() { return template; }
}
