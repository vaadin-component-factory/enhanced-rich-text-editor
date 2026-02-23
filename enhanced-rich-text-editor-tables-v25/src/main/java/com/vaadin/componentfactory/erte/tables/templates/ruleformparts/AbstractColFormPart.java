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

import com.vaadin.componentfactory.erte.tables.templates.TemplateDialog;
import static com.vaadin.componentfactory.erte.tables.templates.TemplateJsonConstants.COLUMNS;

public abstract class AbstractColFormPart extends AbstractIndexedFormPart {

    public AbstractColFormPart(TemplateDialog templateDialog) {
        this(templateDialog, null);
    }

    public AbstractColFormPart(TemplateDialog templateDialog, String title) {
        super(templateDialog);
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
