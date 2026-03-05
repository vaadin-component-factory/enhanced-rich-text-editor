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
