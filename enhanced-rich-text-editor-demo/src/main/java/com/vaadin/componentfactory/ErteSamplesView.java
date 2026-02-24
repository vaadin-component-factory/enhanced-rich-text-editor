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

import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/**
 * Placeholder view for migrated V24 demo samples.
 * <p>
 * Will contain the individual feature samples from the original
 * {@code EnhancedRichTextEditorView} once the demo migration (Phase 5) is
 * completed.
 */
@Route("erte-samples")
@PageTitle("Enhanced RTE Samples")
public class ErteSamplesView extends VerticalLayout {

    public ErteSamplesView() {
        setPadding(true);
        setSpacing(true);

        add(new H2("Enhanced RTE Samples"));
        add(new Paragraph("V24 demo samples will be migrated here in Phase 5."));
    }
}
