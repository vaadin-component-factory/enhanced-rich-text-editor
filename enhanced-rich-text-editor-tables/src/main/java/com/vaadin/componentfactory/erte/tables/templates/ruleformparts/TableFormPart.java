package com.vaadin.componentfactory.erte.tables.templates.ruleformparts;

import com.vaadin.componentfactory.erte.tables.TablesI18n;
import com.vaadin.componentfactory.erte.tables.TablesI18n.TemplatesI18n;
import com.vaadin.componentfactory.erte.tables.templates.TemplateDialog;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.data.binder.Binder;
import elemental.json.Json;
import elemental.json.JsonObject;

import static com.vaadin.componentfactory.erte.tables.templates.TemplateJsonConstants.*;

public class TableFormPart extends RuleFormPart {

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
        HorizontalLayout row = createRow(
                createTextColorField(),
                createBackgroundColorField(),
                createBorderField(getI18nOrDefault(TemplatesI18n::getFormTableBorderFieldLabel, "Table border"), P_BORDER),
                createBorderField(getI18nOrDefault(TemplatesI18n::getFormTableCellsBorderFieldLabel, "Table Cells border"), P_BORDER_CELLS)
        );

        add(row);
    }
}