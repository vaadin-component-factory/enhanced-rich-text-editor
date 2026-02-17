package com.vaadin.componentfactory.erte.tables.events;

import com.vaadin.componentfactory.erte.tables.EnhancedRichTextEditorTables;
import com.vaadin.componentfactory.erte.tables.EnhancedRichTextEditorTablesComponentEvent;

/**
 * This event is fired, when the table cell indices change (including deselection).
 */
public class TableCellChangedEvent extends EnhancedRichTextEditorTablesComponentEvent {
    private final Integer rowIndex;
    private final Integer colIndex;
    private final Integer oldRowIndex;
    private final Integer oldColIndex;

    public TableCellChangedEvent(
            EnhancedRichTextEditorTables source,
            boolean fromClient,
            Integer rowIndex,
            Integer colIndex,
            Integer oldRowIndex,
            Integer oldColIndex
    ) {
        super(source, fromClient);

        if (rowIndex != null && rowIndex < 0) {
            throw new IllegalArgumentException("Row index must not be negative");
        }
        if (colIndex != null && colIndex < 0) {
            throw new IllegalArgumentException("Col index must not be negative");
        }
        if (oldRowIndex != null && oldRowIndex < 0) {
            throw new IllegalArgumentException("Old row index must not be negative");
        }
        if (oldColIndex != null && oldColIndex < 0) {
            throw new IllegalArgumentException("Old col index must not be negative");
        }
        this.oldRowIndex = oldRowIndex;
        this.oldColIndex = oldColIndex;
        this.rowIndex = rowIndex;
        this.colIndex = colIndex;
    }

    public Integer getColIndex() { return colIndex; }
    public Integer getRowIndex() { return rowIndex; }
    public Integer getOldColIndex() { return oldColIndex; }
    public Integer getOldRowIndex() { return oldRowIndex; }
}
