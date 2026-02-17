package com.vaadin.componentfactory.erte.tables.templates.ruleformparts;

import com.vaadin.componentfactory.erte.tables.templates.TemplateDialog;

import static com.vaadin.componentfactory.erte.tables.templates.TemplateJsonConstants.ROWS;

public abstract class AbstractRowFormPart extends AbstractIndexedFormPart {
    private final String title;

    public AbstractRowFormPart(TemplateDialog templateDialog) {
        this(templateDialog, null);
    }

    public AbstractRowFormPart(TemplateDialog templateDialog, String title) {
        super(templateDialog);
        this.title = title;
        if (title != null) {
            addComponentAsFirst(createPartTitle(title));
        }
    }

    @Override
    protected String getKey() { return ROWS; }

    @Override
    public boolean hasHeightInputs() { return true; }
}
