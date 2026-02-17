package com.vaadin.componentfactory.erte.tables.templates.ruleformparts;

import com.vaadin.componentfactory.erte.tables.TablesI18n.TemplatesI18n;
import com.vaadin.componentfactory.erte.tables.templates.TemplateDialog;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.internal.JacksonUtils;
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
    public void readTemplate(ObjectNode template, Binder<ObjectNode> binder) {
        ObjectNode table = template.has(TABLE) ? (ObjectNode) template.get(TABLE) : null;
        if (table == null) {
            table = JacksonUtils.createObjectNode();
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

        HorizontalLayout row = createRow(textColorField, backgroundColorField, tableOutlineBorderField, tableCellsBorderField);
        add(row);
    }

    public TextField getTextColorField() { return textColorField; }
    public TextField getBackgroundColorField() { return backgroundColorField; }
    public TextField getTableOutlineBorderField() { return tableOutlineBorderField; }
    public TextField getTableCellsBorderField() { return tableCellsBorderField; }
}
