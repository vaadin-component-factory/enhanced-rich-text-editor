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
        this.selectedRow = row;
    }

    public int getSelectedRow() {
        return selectedRow;
    }
}
