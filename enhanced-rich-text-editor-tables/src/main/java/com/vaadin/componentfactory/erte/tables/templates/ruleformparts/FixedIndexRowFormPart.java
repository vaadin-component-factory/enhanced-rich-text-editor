package com.vaadin.componentfactory.erte.tables.templates.ruleformparts;

import com.vaadin.componentfactory.erte.tables.templates.TemplateDialog;

public class FixedIndexRowFormPart extends AbstractRowFormPart {
    private final String index;
    private final boolean fromBottom;

    public FixedIndexRowFormPart(TemplateDialog templateDialog, String title, String index) {
        this(templateDialog, title, index, false);
    }

    public FixedIndexRowFormPart(TemplateDialog templateDialog, String title, String index, boolean fromBottom) {
        super(templateDialog, title);
        this.index = index;
        this.fromBottom = fromBottom;
    }

    @Override
    protected String getSelectedIndex() {
        return index;
    }

    @Override
    protected boolean isIndexFromBottom() {
        return fromBottom;
    }
}