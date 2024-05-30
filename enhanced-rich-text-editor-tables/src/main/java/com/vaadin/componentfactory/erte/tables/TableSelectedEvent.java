package com.vaadin.componentfactory.erte.tables;

import org.apache.commons.lang3.StringUtils;

/**
 * This event is fired, when a table has been (de-) selected in an ERTE instance. Contains additional information,
 * if the user uses "cell selection" and which template is applied to the selected table.
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

        if (StringUtils.isNotBlank(template) && !template.matches("[A-Za-z0-9\\-]+")) {
            throw new IllegalArgumentException("Illegal template name: " + template);
        }

        this.selected = selected;
        this.cellSelectionActive = cellSelectionActive;
        this.template = StringUtils.trimToNull(template);
    }

    /**
     * Is a table selected (normal cursor placement inside the table or cell selection)?
     * @see #isCellSelectionActive()
     * @return table is selected
     */
    public boolean isSelected() {
        return selected;
    }

    /**
     * Indicates, if the user used cell selection for the current table. Cell selection means, that there is
     * no active cursor inside the table, but the cells themselves have been selected (for instance to merge them).
     * This information is mainly relevant for available operations on the current table.
     * @return cell selection is active
     */
    public boolean isCellSelectionActive() {
        return cellSelectionActive;
    }

    /**
     * The template of the current selected table or null, if none is selected or the table has no template
     * assigned.
     *
     * @return template
     */
    public String getTemplate() {
        return template;
    }

}