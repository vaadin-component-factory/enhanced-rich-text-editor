package com.vaadin.componentfactory.erte.tables;

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

    /**
     * Returns the current column index (0-based). Returns null, if the table has been deselected.
     * @return column index
     */
    public Integer getColIndex() {
        return colIndex;
    }

    /**
     * Returns the current row index (0-based). Returns null, if the table has been deselected.
     * @return row index
     */
    public Integer getRowIndex() {
        return rowIndex;
    }

    /**
     * Returns the previous column index (0-based). Returns null, if no table had been selected before.
     * @return old column index
     */
    public Integer getOldColIndex() {
        return oldColIndex;
    }

    /**
     * Returns the previous row index (0-based). Returns null, if no table had been selected before.
     * @return old row index
     */
    public Integer getOldRowIndex() {
        return oldRowIndex;
    }
}