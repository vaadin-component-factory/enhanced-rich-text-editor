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

}
