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

import com.vaadin.componentfactory.erte.tables.templates.Dimension;
import com.vaadin.componentfactory.erte.tables.templates.DimensionField;
import com.vaadin.componentfactory.erte.tables.templates.TemplateDialog;
import com.vaadin.componentfactory.erte.tables.TablesI18n.TemplatesI18n;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.textfield.TextFieldVariant;
import com.vaadin.flow.function.ValueProvider;
import com.vaadin.flow.shared.Registration;
import tools.jackson.databind.node.ObjectNode;

public abstract class RuleFormPart extends VerticalLayout {
    private final TemplateDialog templateDialog;

    public RuleFormPart(TemplateDialog templateDialog) {
        this.templateDialog = templateDialog;
        setPadding(false);
        addClassName("form-part");
        // TODO Phase 4.3: binder, initForm
    }

    public void readTemplate(ObjectNode template) {
        // TODO Phase 4.3
    }

    protected abstract void readTemplate(ObjectNode template, Object binder);

    public void clearValues() {
        // TODO Phase 4.3
    }

    public Registration addValueChangeListener(
            HasValue.ValueChangeListener<? super HasValue.ValueChangeEvent<?>> listener) {
        return Registration.once(() -> {}); // TODO Phase 4.3: real binder registration
    }

    public String getI18nOrDefault(ValueProvider<TemplatesI18n, String> valueProvider, String defaultValue) {
        return templateDialog.getI18nOrDefault(valueProvider, defaultValue);
    }

    protected static Component createPartTitle(String label) {
        Span section = new Span(label);
        section.getStyle().set("font-weight", "bold").set("margin-top", "0.8rem");
        return section;
    }

    protected static HorizontalLayout createRow(Component... components) {
        HorizontalLayout row = new HorizontalLayout(components);
        row.setAlignItems(Alignment.CENTER);
        row.getStyle().set("flex-wrap", "wrap");
        return row;
    }

    // Template methods needed for compilation — Phase 4.3 fills in real binder logic
    public boolean hasWidthInputs() { return false; }
    public boolean hasHeightInputs() { return false; }
}
