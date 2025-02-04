package com.vaadin.componentfactory.erte.tables.templates.ruleformparts;

import com.vaadin.componentfactory.erte.tables.templates.TemplateDialog;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import elemental.json.JsonObject;

public abstract class DefaultPropertiesFormPart extends RuleFormPart {

    private TextField textColorField;
    private TextField backgroundColorField;
    private TextField borderField;

    public DefaultPropertiesFormPart(TemplateDialog templateDialog) {
        super(templateDialog);
    }

    @Override
    void initForm(Binder<JsonObject> binder) {
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

    public boolean hasWidthInputs() {
        return false;
    }

    public boolean hasHeightInputs() {
        return false;
    }
}