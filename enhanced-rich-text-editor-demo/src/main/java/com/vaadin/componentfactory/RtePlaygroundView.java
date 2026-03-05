/*-
 * #%L
 * Enhanced Rich Text Editor V25 Demo
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
package com.vaadin.componentfactory;

import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.richtexteditor.RichTextEditor;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/**
 * Playground for the stock Vaadin Rich Text Editor (for comparison).
 * <p>
 * Shows the unmodified RTE with a delta output panel on the right.
 */
@Route("rte-playground")
@PageTitle("RTE Playground")
public class RtePlaygroundView extends PlaygroundView<RichTextEditor> {

    public static final String INITIAL_DELTA = "[" +
            "{\"insert\":\"Stock \"}," +
            "{\"insert\":\"RTE\",\"attributes\":{\"bold\":true}}," +
            "{\"insert\":\" from Vaadin 25\\n" +
            "This is the unmodified Vaadin Rich Text Editor for comparison.\\n\"}" +
            "]";


    @Override
    protected RichTextEditor createEditor() {
        return new RichTextEditor();
    }

    @Override
    protected String getInitialContent() {
        return INITIAL_DELTA;
    }
}
