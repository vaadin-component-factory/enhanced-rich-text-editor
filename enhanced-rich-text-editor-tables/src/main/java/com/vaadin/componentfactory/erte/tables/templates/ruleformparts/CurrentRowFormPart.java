package com.vaadin.componentfactory.erte.tables.templates.ruleformparts;

import com.vaadin.componentfactory.erte.tables.templates.TemplateDialog;

public class CurrentRowFormPart extends AbstractRowFormPart {
    private int selectedRow;

    public CurrentRowFormPart(TemplateDialog templateDialog) {
        super(templateDialog);
    }

    @Override
    protected String getSelectedIndex() {
        return String.valueOf(selectedRow + 1);
    }

    public void setSelectedRow(int row) {
        if (row < 0) {
            throw new IllegalArgumentException("Row must not be negative");
        }

        if (this.selectedRow != row) {
            this.selectedRow = row;
        }
    }

    public int getSelectedRow() {
        return selectedRow;
    }
}