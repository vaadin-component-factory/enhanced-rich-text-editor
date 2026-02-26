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
import com.vaadin.componentfactory.erte.tables.templates.TemplateParser;
import com.vaadin.flow.data.binder.Binder;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import static com.vaadin.componentfactory.erte.tables.templates.TemplateJsonConstants.*;

public abstract class AbstractIndexedFormPart extends DefaultPropertiesFormPart {

    public AbstractIndexedFormPart(TemplateDialog templateDialog) {
        super(templateDialog);
    }

    @Override
    protected void readTemplate(ObjectNode template, Binder<ObjectNode> binder) {
        ArrayNode array;
        if (template.has(getKey())) {
            array = (ArrayNode) template.get(getKey());
        } else {
            array = JsonNodeFactory.instance.arrayNode();
            template.set(getKey(), array);
        }

        String index = getSelectedIndex();
        boolean indexFromBottom = isIndexFromBottom();
        ObjectNode rowObject = TemplateParser.searchForIndexedObject(array, index, indexFromBottom); // css nth child are 1 based
        ObjectNode rowDeclarations;
        if (rowObject == null) {
            rowObject = JsonNodeFactory.instance.objectNode();
            rowObject.put(INDEX, index);
            if (indexFromBottom) {
                rowObject.put(FROM_BOTTOM, true);
            }
            rowDeclarations = JsonNodeFactory.instance.objectNode();
            rowObject.set(DECLARATIONS, rowDeclarations);
            array.add(rowObject);
        } else {
            rowDeclarations = (ObjectNode) rowObject.get(DECLARATIONS);
        }
        binder.setBean(rowDeclarations); // null automatically clears the binder
    }

    protected abstract String getKey();

    protected abstract String getSelectedIndex();

    protected boolean isIndexFromBottom() {
        return false;
    }
}
