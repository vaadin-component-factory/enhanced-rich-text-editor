package com.vaadin.componentfactory;

import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.richtexteditor.RichTextEditor;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@Route("")
@PageTitle("ERTE V25 - Phase 1")
public class V25DemoView extends VerticalLayout {

    public V25DemoView() {
        var rte = new RichTextEditor();
        rte.setWidthFull();
        rte.setMaxHeight("400px");
        add(new H3("ERTE V25 — Phase 1 (Project Base)"), rte);
    }

}
