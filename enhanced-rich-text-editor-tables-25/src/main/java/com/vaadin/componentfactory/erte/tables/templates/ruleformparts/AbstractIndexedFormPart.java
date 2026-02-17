package com.vaadin.componentfactory.erte.tables.templates.ruleformparts;

import com.vaadin.componentfactory.erte.tables.templates.TemplateDialog;
import com.vaadin.componentfactory.erte.tables.templates.TemplateParser;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.internal.JacksonUtils;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import static com.vaadin.componentfactory.erte.tables.templates.TemplateJsonConstants.*;

public abstract class AbstractIndexedFormPart extends DefaultPropertiesFormPart {

    public AbstractIndexedFormPart(TemplateDialog templateDialog) {
        super(templateDialog);
    }

    @Override
    protected void readTemplate(ObjectNode template, Binder<ObjectNode> binder) {
        ArrayNode array = template.has(getKey()) ? (ArrayNode) template.get(getKey()) : null;
        if (array == null) {
            array = JacksonUtils.createArrayNode();
            template.set(getKey(), array);
        }

        String index = getSelectedIndex();
        boolean indexFromBottom = isIndexFromBottom();
        ObjectNode rowObject = TemplateParser.searchForIndexedObject(array, index, indexFromBottom);
        ObjectNode rowDeclarations;
        if (rowObject == null) {
            rowObject = JacksonUtils.createObjectNode();
            rowObject.put(INDEX, index);
            if (indexFromBottom) {
                rowObject.put(FROM_BOTTOM, true);
            }
            rowDeclarations = JacksonUtils.createObjectNode();
            rowObject.set(DECLARATIONS, rowDeclarations);
            array.add(rowObject);
        } else {
            rowDeclarations = (ObjectNode) rowObject.get(DECLARATIONS);
        }
        binder.setBean(rowDeclarations);
    }

    protected abstract String getKey();
    protected abstract String getSelectedIndex();
    protected boolean isIndexFromBottom() { return false; }
}
