package com.vaadin.componentfactory;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

/**
 * Minimal test view for ERTE shell smoke tests (Phase 2).
 * Provides a single editor with "ERTE V25" content for shell verification.
 */
@Route("erte-test/shell")
public class ErteShellTestView extends VerticalLayout {

    public ErteShellTestView() {
        setSizeFull();
        setPadding(true);

        var editor = new EnhancedRichTextEditor();
        editor.setId("test-editor");
        editor.setWidthFull();
        editor.setHeight("400px");
        editor.setValue("<p>ERTE V25 Shell Test</p>");

        add(editor);

        // Ready indicator (hidden, used by Playwright)
        var ready = new Div();
        ready.setId("test-ready");
        ready.getElement().setAttribute("data-ready", "true");
        ready.getStyle().set("display", "none");
        add(ready);
    }
}
