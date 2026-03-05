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
package com.vaadin.componentfactory.erte.tables.events;

import com.vaadin.componentfactory.erte.tables.EnhancedRichTextEditorTables;
import com.vaadin.componentfactory.erte.tables.EnhancedRichTextEditorTablesComponentEvent;
import com.vaadin.componentfactory.erte.tables.templates.TemplateParser;

/**
 * This event is fired, when a table has been (de-) selected in an ERTE instance. Contains additional information,
 * if the user uses "cell selection" and which template is applied to the selected table.
 * <p/>
 * Cell selection means, that one or multiple cells has been selected (using the Ctrl key) instead of just placing
 * the text cursor inside one cell. When active, cell merging is possible.
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

        if (template != null && !template.isBlank() && !TemplateParser.isValidTemplateId(template)) {
            throw new IllegalArgumentException("Illegal template name: " + template);
        }

        this.selected = selected;
        this.cellSelectionActive = cellSelectionActive;
        this.template = template != null ? template.strip() : null;
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
     * The template of the current selected table. This value is an empty string, if no table is selected
     * or no template has been set. It is null, when no template information has been passed. The latter is only
     * the case for new tables to prevent overriding
     *
     * @return template
     */
    public String getTemplate() {
        return template;
    }

}
