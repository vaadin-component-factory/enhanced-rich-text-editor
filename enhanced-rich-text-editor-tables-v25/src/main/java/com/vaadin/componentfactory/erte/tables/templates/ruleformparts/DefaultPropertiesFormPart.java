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
import tools.jackson.databind.node.ObjectNode;

public abstract class DefaultPropertiesFormPart extends RuleFormPart {

    public DefaultPropertiesFormPart(TemplateDialog templateDialog) {
        super(templateDialog);
    }

    @Override
    protected void readTemplate(ObjectNode template, Object binder) {
        // TODO Phase 4.3
    }

    @Override
    public boolean hasWidthInputs() { return false; }

    @Override
    public boolean hasHeightInputs() { return false; }
}
