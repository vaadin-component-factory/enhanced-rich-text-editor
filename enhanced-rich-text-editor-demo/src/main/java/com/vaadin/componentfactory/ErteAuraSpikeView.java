package com.vaadin.componentfactory;

import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.aura.Aura;

/**
 * Spike test view for Aura Style Proxy prototype (Phase 3.4j).
 * Tests runtime stylesheet scanning and rule injection.
 * Uses Aura theme to validate style proxy behavior.
 */
@Route("erte-spike/aura-proxy")
@StyleSheet(Aura.STYLESHEET)
public class ErteAuraSpikeView extends VerticalLayout {
    public ErteAuraSpikeView() {
        add(new H2("Aura Style Proxy Spike"));
        add(new Paragraph("Testing runtime stylesheet scanning and rule cloning."));

        // Stock RTE (receives Aura styles normally)
        var stockRte = new com.vaadin.flow.component.richtexteditor.RichTextEditor();
        stockRte.setId("stock-rte");
        stockRte.setWidthFull();

        // ERTE (should receive cloned Aura styles via proxy)
        var erteRte = new EnhancedRichTextEditor();
        erteRte.setId("erte-rte");
        erteRte.setWidthFull();

        add(new H2("Stock RTE (vaadin-rich-text-editor)"), stockRte);
        add(new H2("ERTE (vcf-enhanced-rich-text-editor)"), erteRte);

        // Debug output div
        Div debugOutput = new Div();
        debugOutput.setId("debug-output");
        debugOutput.getStyle()
            .set("margin-top", "2em")
            .set("padding", "1em")
            .set("border", "1px solid var(--lumo-contrast-20pct)")
            .set("background", "var(--lumo-contrast-5pct)");
        add(new H2("Debug Output"), debugOutput);
    }
}
