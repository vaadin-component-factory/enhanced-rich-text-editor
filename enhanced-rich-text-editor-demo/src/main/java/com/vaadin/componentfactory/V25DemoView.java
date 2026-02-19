package com.vaadin.componentfactory;

import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@Route("")
@PageTitle("ERTE V25 - Phase 2")
public class V25DemoView extends VerticalLayout {

    public V25DemoView() {
        var erte = new EnhancedRichTextEditor();
        erte.setWidthFull();
        erte.setMaxHeight("400px");
        // setValue() accepts HTML. Use asDelta().setValue() for Delta JSON.
        erte.setValue("<p>ERTE V25 — Phase 2 (Shell)</p>");

        // Delta output — verifies asDelta() round-trip (AC #4)
        var deltaOutput = new Pre();
        deltaOutput.setId("delta-output");
        deltaOutput.getStyle().set("white-space", "pre-wrap")
                .set("font-size", "var(--lumo-font-size-xs)")
                .set("max-height", "200px")
                .set("overflow", "auto");
        deltaOutput.getElement().setAttribute("tabindex", "0");
        deltaOutput.getElement().setAttribute("aria-label", "Delta output");
        erte.asDelta().addValueChangeListener(e ->
                deltaOutput.setText(e.getValue()));

        add(
            new H2("ERTE V25 — Phase 2 (ERTE Shell)"),
            erte,
            new H3("Delta Output"),
            deltaOutput
        );
    }

}
