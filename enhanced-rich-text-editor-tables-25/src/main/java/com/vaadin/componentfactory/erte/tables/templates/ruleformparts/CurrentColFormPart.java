package com.vaadin.componentfactory.erte.tables.templates.ruleformparts;

import com.vaadin.componentfactory.erte.tables.templates.TemplateDialog;

public class CurrentColFormPart extends AbstractColFormPart {
    private int selectedCol;

    public CurrentColFormPart(TemplateDialog templateDialog) {
        super(templateDialog);
    }

    @Override
    protected String getSelectedIndex() {
        return String.valueOf(selectedCol + 1);
    }

    public void setSelectedColumn(int column) {
        if (column < 0) { throw new IllegalArgumentException("Column must not be negative"); }
        if (this.selectedCol != column) { this.selectedCol = column; }
    }

    public int getSelectedCol() { return selectedCol; }
}
