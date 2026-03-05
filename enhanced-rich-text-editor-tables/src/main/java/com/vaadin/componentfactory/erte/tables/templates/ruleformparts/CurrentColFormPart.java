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
        if (column < 0) {
            throw new IllegalArgumentException("Column must not be negative");
        }
        this.selectedCol = column;
    }

    public int getSelectedCol() {
        return selectedCol;
    }
}
