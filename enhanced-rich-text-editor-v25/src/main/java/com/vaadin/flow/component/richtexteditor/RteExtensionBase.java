/*-
 * #%L
 * Enhanced Rich Text Editor V25
 * %%
 * Copyright (C) 2019 - 2025 Vaadin Ltd
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
package com.vaadin.flow.component.richtexteditor;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.function.SerializableConsumer;

/**
 * Bridge class that lives in RTE 2's package to access package-private members.
 * <p>
 * All real ERTE logic belongs in {@code com.vaadin.componentfactory} — this
 * class only lifts visibility so that {@code EnhancedRichTextEditor} can extend
 * {@code RichTextEditor} without being in the same package.
 * <p>
 * If RTE 2 changes its package-private API, only this class needs updating.
 */
public abstract class RteExtensionBase extends RichTextEditor {

    /**
     * Visibility-widening override: package-private → protected.
     * {@code RichTextEditor.runBeforeClientResponse()} is package-private,
     * invisible to {@code EnhancedRichTextEditor} in
     * {@code com.vaadin.componentfactory}. This override widens it to
     * {@code protected} so subclasses in other packages can use it.
     */
    @Override
    protected void runBeforeClientResponse(
            SerializableConsumer<UI> command) {
        super.runBeforeClientResponse(command);
    }
}
