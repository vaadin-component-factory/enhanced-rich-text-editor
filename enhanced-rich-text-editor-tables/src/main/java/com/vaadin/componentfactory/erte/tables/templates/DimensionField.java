package com.vaadin.componentfactory.erte.tables.templates;

import com.vaadin.flow.component.customfield.CustomField;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextFieldVariant;
import com.vaadin.flow.theme.lumo.LumoUtility;
import org.apache.commons.lang3.StringUtils;

import java.time.format.TextStyle;

import static com.vaadin.flow.theme.lumo.LumoUtility.*;

public class DimensionField extends CustomField<Dimension> {

    private final IntegerField valueField;
    private final Span unitField; // might be replaced with a combobox or select later
    private String defaultUnit = TemplateDialog.Defaults.DIMENSION_UNIT;

    public DimensionField(String label) {
        valueField = new IntegerField(label);
        valueField.setMin(1);

        valueField.addThemeVariants(TextFieldVariant.LUMO_SMALL);
        valueField.setWidth("6rem");

        unitField = new Span();
        unitField.getStyle()
                .set("margin-left", "var(--lumo-space-xs)")
                .set("font-size", "var(--lumo-font-size-s)");
        add(valueField, unitField);
    }

    @Override
    protected Dimension generateModelValue() {
        Integer value = valueField.getValue();
        if (value == null || value == 0) {
            return null;
        }

        return new Dimension(value, StringUtils.trimToEmpty(unitField.getText()));
    }

    @Override
    protected void setPresentationValue(Dimension newPresentationValue) {
        unitField.getStyle().remove("font-style").remove("font-weight");

        if (newPresentationValue != null) {
            valueField.setValue((int) newPresentationValue.getValue());
            String unit = newPresentationValue.getUnit();
            unitField.setText(unit);

            if(!unit.equals(defaultUnit)) {
                unitField.getStyle()
                        .set("font-style", "italic")
                        .set("font-weight", "bold");
            }
        } else {
            valueField.clear();
            unitField.setText(defaultUnit);
        }
    }

    public void setDefaultUnit(String defaultUnit) {
        this.defaultUnit = defaultUnit;
        unitField.setText(defaultUnit);
    }

    public void setStepButtonsVisible(boolean stepButtonsVisible) {
        valueField.setStepButtonsVisible(stepButtonsVisible);
    }
}

