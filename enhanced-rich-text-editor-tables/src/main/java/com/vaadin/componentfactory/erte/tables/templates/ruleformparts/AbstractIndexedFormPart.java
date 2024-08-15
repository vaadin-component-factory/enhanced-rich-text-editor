package com.vaadin.componentfactory.erte.tables.templates.ruleformparts;

import com.vaadin.componentfactory.erte.tables.templates.TemplateDialog;
import com.vaadin.componentfactory.erte.tables.templates.TemplateParser;
import com.vaadin.flow.data.binder.Binder;
import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;

import static com.vaadin.componentfactory.erte.tables.templates.TemplateJsonConstants.*;

public abstract class AbstractIndexedFormPart extends DefaultPropertiesFormPart {

    public AbstractIndexedFormPart(TemplateDialog templateDialog) {
        super(templateDialog);
    }

    @Override
    protected void readTemplate(JsonObject template, Binder<JsonObject> binder) {
        JsonArray array = template.getArray(getKey());
        if (array == null) {
            array = Json.createArray();
            template.put(getKey(), array);
        }

        String index = getSelectedIndex();
        boolean indexFromBottom = isIndexFromBottom();
        JsonObject rowObject = TemplateParser.searchForIndexedObject(array, index, indexFromBottom); // css nth child are 1 based
        JsonObject rowDeclarations;
        if (rowObject == null) {
            rowObject = Json.createObject();
            rowObject.put(INDEX, index);
            if (indexFromBottom) {
                rowObject.put(FROM_BOTTOM, true);
            }
            rowDeclarations = Json.createObject();
            rowObject.put(DECLARATIONS, rowDeclarations);
            array.set(array.length(), rowObject);
        } else {
            rowDeclarations = rowObject.getObject(DECLARATIONS);
        }
        binder.setBean(rowDeclarations); // null automatically clears the binder
    }

    protected abstract String getKey();

    protected abstract String getSelectedIndex();

    protected boolean isIndexFromBottom() {
        return false;
    }
}