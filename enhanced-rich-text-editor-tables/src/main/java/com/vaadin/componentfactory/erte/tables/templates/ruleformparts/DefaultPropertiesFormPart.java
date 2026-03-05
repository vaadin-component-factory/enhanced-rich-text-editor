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
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import tools.jackson.databind.node.ObjectNode;

public abstract class DefaultPropertiesFormPart extends RuleFormPart {

    private TextField textColorField;
    private TextField backgroundColorField;
    private TextField borderField;

    public DefaultPropertiesFormPart(TemplateDialog templateDialog) {
        super(templateDialog);
    }

    @Override
    void initForm(Binder<ObjectNode> binder) {
        textColorField = createTextColorField();
        backgroundColorField = createBackgroundColorField();
        borderField = createBorderField();
        HorizontalLayout row = createRow(
                textColorField,
                backgroundColorField,
                borderField
        );
        if (hasWidthInputs()) {
            row.add(createWidthField());
        }
        if (hasHeightInputs()) {
            row.add(createHeightField());
        }

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
     * Returns the input field for the border.
     * @return border field
     */
    public TextField getBorderField() {
        return borderField;
    }

    @Override
    public boolean hasWidthInputs() {
        return false;
    }

    @Override
    public boolean hasHeightInputs() {
        return false;
    }
}
