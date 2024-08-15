package com.vaadin.componentfactory.erte.tables.templates.ruleformparts;

import com.vaadin.componentfactory.erte.tables.templates.TemplateDialog;
import com.vaadin.flow.data.binder.Binder;
import elemental.json.JsonObject;

import static com.vaadin.componentfactory.erte.tables.templates.TemplateJsonConstants.*;

public abstract class AbstractColFormPart extends AbstractIndexedFormPart {
    private final String title;

    public AbstractColFormPart(TemplateDialog templateDialog) {
        this(templateDialog, null);
    }

    public AbstractColFormPart(TemplateDialog templateDialog, String title) {
        super(templateDialog);
        this.title = title;
        if (title != null) {
            addComponentAsFirst(createPartTitle(title));
        }
    }

    @Override
    protected String getKey() {
        return COLUMNS;
    }

    @Override
    public boolean hasWidthInputs() {
        return true;
    }
}