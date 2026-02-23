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
package com.vaadin.componentfactory.erte.tables.templates;

import tools.jackson.databind.node.ObjectNode;

public final class TemplateParser {

    private TemplateParser() {
    }

    // TODO Phase 4.3: Implement CSS generation from templates
    public static String convertToCss(ObjectNode templates) {
        // Stub — returns empty string
        return "";
    }

    public static boolean isValidTemplateId(String templateId) {
        return TemplateJsonConstants.PATTERN_TEMPLATE_ID.asMatchPredicate().test(templateId);
    }

    public static ObjectNode clone(ObjectNode templateToClone) {
        return templateToClone != null ? templateToClone.deepCopy() : null;
    }

    // TODO Phase 4.3: re-enable isValidPropertyValue() (SECURITY.md)
    // TODO Phase 4.3: removeEmptyChildren, searchForIndexedObject, parseJson, full toCss()
}
