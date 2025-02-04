package com.vaadin.componentfactory.erte.tables.templates.ruleformparts;

import com.vaadin.componentfactory.erte.tables.TablesI18n.TemplatesI18n;
import com.vaadin.componentfactory.erte.tables.templates.TemplateDialog;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import elemental.json.Json;
import elemental.json.JsonObject;

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
    public void readTemplate(JsonObject template, Binder<JsonObject> binder) {
        JsonObject table = template.getObject(TABLE);
        if (table == null) {
            table = Json.createObject();
            template.put(TABLE, table);
        }
        binder.setBean(table);
    }

    @Override
    void initForm(Binder<JsonObject> binder) {
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