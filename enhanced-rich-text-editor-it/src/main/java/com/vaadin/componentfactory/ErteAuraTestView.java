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

import java.util.List;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.richtexteditor.RichTextEditor;
import com.vaadin.flow.router.Route;

/**
 * Aura theme test view for visual verification of ERTE under both themes.
 * <p>
 * Run with the {@code -Paura} Maven profile to see this view under Aura.
 * The theme style proxy clones vaadin-rich-text-editor rules for
 * vcf-enhanced-rich-text-editor, so ERTE should match stock RTE chrome.
 * <p>
 * Features exercised: readonly sections, placeholders, whitespace indicators,
 * tabstops, rulers, and tables (selected/focused cells).
 */
@Route(value = "erte-test/aura", layout = ErteTestLayout.class)
public class ErteAuraTestView extends VerticalLayout {

    public ErteAuraTestView() {
        setSizeFull();
        setSpacing(true);

        // Ready indicator for Playwright
        Div ready = new Div();
        ready.setId("test-ready");
        ready.getStyle().set("display", "none");
        add(ready);

        // --- Stock RTE (reference) ---
        var stockRte = new RichTextEditor();
        stockRte.setId("stock-rte");
        stockRte.setWidthFull();
        stockRte.setMaxHeight("250px");

        // --- ERTE with features ---
        var erte = new EnhancedRichTextEditor();
        erte.setId("test-editor");
        erte.setWidthFull();
        erte.setMaxHeight("400px");

        // Tabstops + rulers
        erte.setTabStops(List.of(
                new TabStop(TabStop.Direction.LEFT, 150),
                new TabStop(TabStop.Direction.RIGHT, 350)));

        // Whitespace indicators
        erte.setShowWhitespace(true);

        // Placeholders
        erte.setPlaceholders(List.of(
                new Placeholder("Customer Name"),
                new Placeholder("Invoice Date")));

        // Readonly sections + placeholders + normal text via Delta
        String delta = "[" +
                "{\"insert\":\"Normal text before readonly. \"}," +
                "{\"insert\":\"This is readonly\",\"attributes\":{\"readonly\":true}}," +
                "{\"insert\":\" and normal after.\\n\"}," +
                "{\"insert\":{\"placeholder\":{\"text\":\"Customer Name\"}}}," +
                "{\"insert\":\" ordered on \"}," +
                "{\"insert\":{\"placeholder\":{\"text\":\"Invoice Date\"}}}," +
                "{\"insert\":\".\\n\"}," +
                "{\"insert\":{\"tab\":{\"width\":\"150px\",\"direction\":\"left\"}}}," +
                "{\"insert\":\"After tab stop\\n\"}" +
                "]";
        erte.asDelta().setValue(delta);

        // Tables editor (separate ERTE with table content)
        var erteTable = new EnhancedRichTextEditor();
        erteTable.setId("table-editor");
        erteTable.setWidthFull();
        erteTable.setMaxHeight("250px");
        // Set a table via Delta
        String tableDelta = "[" +
                "{\"insert\":{\"td\":\"t1|r1|c1||||\"}}," +
                "{\"insert\":\"Cell A1\"}," +
                "{\"insert\":{\"td\":\"t1|r1|c2||||\"}}," +
                "{\"insert\":\"Cell B1\"}," +
                "{\"insert\":{\"td\":\"t1|r2|c3||||\"}}," +
                "{\"insert\":\"Cell A2\"}," +
                "{\"insert\":{\"td\":\"t1|r2|c4||||\"}}," +
                "{\"insert\":\"Cell B2\"}," +
                "{\"insert\":\"\\n\"}" +
                "]";
        erteTable.asDelta().setValue(tableDelta);

        // Layout: side by side comparison
        var stockSection = createSection("Stock RTE (reference)", stockRte);
        var erteSection = createSection("ERTE — Aura Theme", erte);
        var tableSection = createSection("ERTE — Tables", erteTable);

        var comparison = new HorizontalLayout(stockSection, erteSection);
        comparison.setWidthFull();
        comparison.setFlexGrow(1, stockSection, erteSection);

        add(comparison, tableSection);
    }

    private VerticalLayout createSection(String title,
            com.vaadin.flow.component.Component editor) {
        var section = new VerticalLayout();
        section.setPadding(false);
        section.setSpacing(false);
        section.add(new H3(title));
        section.add(editor);
        return section;
    }
}
