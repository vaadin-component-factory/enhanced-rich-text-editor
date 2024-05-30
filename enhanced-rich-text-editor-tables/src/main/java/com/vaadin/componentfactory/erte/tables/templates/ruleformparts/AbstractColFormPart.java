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
        super(templateDialog, false, true);
        this.title = title;
    }

    @Override
    void initForm(Binder<JsonObject> binder) {
        if (title != null) {
            add(createPartTitle(title));
        }

        super.initForm(binder);
    }


    @Override
    protected String getKey() {
        return COLUMNS;
    }
}