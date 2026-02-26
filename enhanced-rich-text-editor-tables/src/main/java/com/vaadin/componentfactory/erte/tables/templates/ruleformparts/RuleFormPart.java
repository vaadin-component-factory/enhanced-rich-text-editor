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

import com.vaadin.componentfactory.erte.tables.TablesI18n.TemplatesI18n;
import com.vaadin.componentfactory.erte.tables.templates.Dimension;
import com.vaadin.componentfactory.erte.tables.templates.DimensionField;
import com.vaadin.componentfactory.erte.tables.templates.TemplateDialog;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.textfield.TextFieldVariant;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.Setter;
import com.vaadin.flow.function.ValueProvider;
import com.vaadin.flow.shared.Registration;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

import static com.vaadin.componentfactory.erte.tables.templates.TemplateJsonConstants.*;

public abstract class RuleFormPart extends VerticalLayout {
    private final Binder<ObjectNode> binder;
    private final TemplateDialog templateDialog;

    public RuleFormPart(TemplateDialog templateDialog) {
        this.templateDialog = templateDialog;
        this.binder = new Binder<>();

        setPadding(false);

        initForm(binder);
        addClassName("form-part");
    }

    abstract void initForm(Binder<ObjectNode> binder);

    protected TextField createBorderField(String label, String key) {
        TextField field = createTextField(label, key);
        field.setTooltipText(getI18nOrDefault(TemplatesI18n::getFormBorderFieldTooltip, "Expects a valid css border definition, e.g. 1px solid black"));
        return field;
    }

    protected TextField createBorderField() {
        return createBorderField(getI18nOrDefault(TemplatesI18n::getFormBorderFieldLabel, "Border"), P_BORDER);
    }

    protected TextField createTextColorField() {
        return createColorField(getI18nOrDefault(TemplatesI18n::getFormTextColorFieldLabel, "Text color"), P_COLOR);
    }

    protected TextField createBackgroundColorField() {
        return createColorField(getI18nOrDefault(TemplatesI18n::getFormBackgroundColorFieldLabel, "Background color"), P_BACKGROUND);
    }

    protected TextField createColorField(String label, String key) {
        TextField field = createTextField(label, key);
        field.setTooltipText(getI18nOrDefault(TemplatesI18n::getFormColorFieldTooltip, "Expects a valid css color definition, e.g. #123456 or red"));
        return field;
    }

    protected TextField createTextField(String label, String key) {
        TextField field = new TextField(label);
        field.setClearButtonVisible(true);
        field.addThemeVariants(TextFieldVariant.LUMO_SMALL);

        binder.forField(field).bind(getter(key), setter(key));

        return field;
    }

    protected DimensionField createSizeField(String label, String key) {
        DimensionField field = new DimensionField(label);
        field.setStepButtonsVisible(true);
        String dimensionUnit = templateDialog.getDefaults().getDimensionUnit();

        field.setDefaultUnit(dimensionUnit);
        updateDimensionFieldTooltip(field, dimensionUnit);

        templateDialog.getDefaults().addDimensionUnitChangedListener(event -> {
            String newDimensionUnit = event.getNewValue();
            field.setDefaultUnit(newDimensionUnit);
            updateDimensionFieldTooltip(field, newDimensionUnit);
        });

        binder.forField(field)
                .withValidator(dimension -> dimension == null || dimension.getValue() >= 1,
                        getI18nOrDefault(TemplatesI18n::getFormNumberFieldLessOrEqualZeroError, "Value must be larger than 0"))
                .withConverter(
                        dimension -> dimension != null
                                        ? dimension.toDimensionString()
                                        : null,
                        s -> s != null ?
                                new Dimension(s)
                                : null
                )
                .bind(getter(key), setter(key));


        return field;
    }

    private void updateDimensionFieldTooltip(DimensionField field, String dimensionUnit) {
        field.setTooltipText(getI18nOrDefault(TemplatesI18n::getFormSizeFieldTooltip,
                "Expects a positive integer value. Will be interpreted with the css unit '%s' (global font size)"
                        .formatted(dimensionUnit)));
    }

    protected static Component createPartTitle(String label) {
        Span section = new Span(label);
        section.getStyle().set("font-weight", "bold").set("margin-top","0.8rem");
        return section;
    }

    protected static HorizontalLayout createRow(Component... components) {
        HorizontalLayout row = new HorizontalLayout(components);
        row.setAlignItems(Alignment.CENTER);
        row.getStyle().set("flex-wrap", "wrap");
        return row;
    }

    protected DimensionField createWidthField() {
        return createSizeField(getI18nOrDefault(TemplatesI18n::getFormWidthFieldLabel, "Width"), P_WIDTH);
    }

    protected DimensionField createHeightField() {
        return createSizeField(getI18nOrDefault(TemplatesI18n::getFormHeightFieldLabel, "Height"), P_HEIGHT);
    }

    protected static ValueProvider<ObjectNode, String> getter(String key) {
        return obj -> {
            if (!obj.has(key)) return null;
            JsonNode node = obj.get(key);
            return (node != null && !node.isNull()) ? node.asText() : null;
        };
    }

    protected static Setter<ObjectNode, String> setter(String key) {
        return (obj, val) -> {
            if (val != null && !val.isEmpty()) {
                obj.put(key, val);
            } else {
                obj.remove(key);
            }
        };
    }

    public void readTemplate(ObjectNode template) {
        readTemplate(template, binder);
    }

    protected abstract void readTemplate(ObjectNode template, Binder<ObjectNode> binder);

    public void setBean(ObjectNode object) {
        binder.setBean(object);
    }

    public ObjectNode getBean() {
        return binder.getBean();
    }

    public Registration addValueChangeListener(HasValue.ValueChangeListener<? super HasValue.ValueChangeEvent<?>> listener) {
        return binder.addValueChangeListener(event -> {
            if (binder.validate().isOk()) {
                listener.valueChanged(event);
            }
        });
    }

    public String getI18nOrDefault(ValueProvider<TemplatesI18n, String> valueProvider, String defaultValue) {
        return templateDialog.getI18nOrDefault(valueProvider, defaultValue);
    }

    public void clearValues() {
        binder.removeBean();
    }

    public boolean hasWidthInputs() {
        return false;
    }

    public boolean hasHeightInputs() {
        return false;
    }
}
