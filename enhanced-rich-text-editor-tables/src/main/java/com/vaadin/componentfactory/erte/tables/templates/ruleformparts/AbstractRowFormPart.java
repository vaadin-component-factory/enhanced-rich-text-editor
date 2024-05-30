package com.vaadin.componentfactory.erte.tables.templates.ruleformparts;

import com.vaadin.componentfactory.erte.tables.templates.TemplateDialog;
import com.vaadin.flow.data.binder.Binder;
import elemental.json.JsonObject;

import static com.vaadin.componentfactory.erte.tables.templates.TemplateJsonConstants.ROWS;

public abstract class AbstractRowFormPart extends AbstractIndexedFormPart {
    private final String title;

    public AbstractRowFormPart(TemplateDialog templateDialog) {
        this(templateDialog, null);
    }

    public AbstractRowFormPart(TemplateDialog templateDialog, String title) {
        super(templateDialog, true, false);
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
        return ROWS;
    }
}