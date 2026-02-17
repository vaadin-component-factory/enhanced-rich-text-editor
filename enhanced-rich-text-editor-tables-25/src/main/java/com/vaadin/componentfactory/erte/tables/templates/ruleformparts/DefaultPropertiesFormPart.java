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
        HorizontalLayout row = createRow(textColorField, backgroundColorField, borderField);
        if (hasWidthInputs()) { row.add(createWidthField()); }
        if (hasHeightInputs()) { row.add(createHeightField()); }
        add(row);
    }

    public TextField getTextColorField() { return textColorField; }
    public TextField getBackgroundColorField() { return backgroundColorField; }
    public TextField getBorderField() { return borderField; }
    public boolean hasWidthInputs() { return false; }
    public boolean hasHeightInputs() { return false; }
}
