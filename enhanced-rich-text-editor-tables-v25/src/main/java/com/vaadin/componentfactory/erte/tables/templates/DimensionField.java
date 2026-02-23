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
package com.vaadin.componentfactory.erte.tables.templates;

import com.vaadin.flow.component.customfield.CustomField;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextFieldVariant;

public class DimensionField extends CustomField<Dimension> {

    private final IntegerField valueField;
    private final Span unitField;
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
        return new Dimension(value, unitField.getText().strip());
    }

    @Override
    protected void setPresentationValue(Dimension newPresentationValue) {
        unitField.getStyle().remove("font-style").remove("font-weight");
        if (newPresentationValue != null) {
            valueField.setValue((int) newPresentationValue.getValue());
            String unit = newPresentationValue.getUnit();
            unitField.setText(unit);
            if (!unit.equals(defaultUnit)) {
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
