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

import com.vaadin.componentfactory.erte.tables.TablesI18n.TemplatesI18n;
import com.vaadin.componentfactory.erte.tables.templates.TemplateDialog;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import static com.vaadin.componentfactory.erte.tables.templates.TemplateJsonConstants.*;

public class TableFormPart extends RuleFormPart {

    private TextField textColorField;
    private TextField backgroundColorField;
    private TextField tableOutlineBorderField;
    private TextField tableCellsBorderField;

    public TableFormPart(TemplateDialog templateDialog) {
        super(templateDialog);
    }

    @Override
    protected void readTemplate(ObjectNode template, Binder<ObjectNode> binder) {
        ObjectNode table;
        if (template.has(TABLE)) {
            table = (ObjectNode) template.get(TABLE);
        } else {
            table = JsonNodeFactory.instance.objectNode();
            template.set(TABLE, table);
        }
        binder.setBean(table);
    }

    @Override
    void initForm(Binder<ObjectNode> binder) {
        textColorField = createTextColorField();
        backgroundColorField = createBackgroundColorField();
        tableOutlineBorderField = createBorderField(getI18nOrDefault(TemplatesI18n::getFormTableBorderFieldLabel, "Table border"), P_BORDER);
        tableCellsBorderField = createBorderField(getI18nOrDefault(TemplatesI18n::getFormTableCellsBorderFieldLabel, "Table Cells border"), P_BORDER_CELLS);

        HorizontalLayout row = createRow(
                textColorField,
                backgroundColorField,
                tableOutlineBorderField,
                tableCellsBorderField
        );

        add(row);
    }

    /**
     * Returns the input field for the text color.
     * @return text color field
     */
    public TextField getTextColorField() {
        return textColorField;
    }

    /**
     * Returns the input field for the background color.
     * @return background color field
     */
    public TextField getBackgroundColorField() {
        return backgroundColorField;
    }

    /**
     * Returns the input field for the table's outline border.
     * @return table's outline border field
     */
    public TextField getTableOutlineBorderField() {
        return tableOutlineBorderField;
    }

    /**
     * Returns the input field for the table cells border.
     * @return table cells border field
     */
    public TextField getTableCellsBorderField() {
        return tableCellsBorderField;
    }
}
