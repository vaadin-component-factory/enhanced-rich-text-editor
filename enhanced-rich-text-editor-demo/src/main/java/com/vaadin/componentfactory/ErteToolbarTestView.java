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

import com.vaadin.componentfactory.toolbar.ToolbarSlot;
import com.vaadin.componentfactory.toolbar.ToolbarSwitch;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

/**
 * Test view for toolbar slot system (Phase 3.1a).
 * <p>
 * Provides:
 * <ul>
 *   <li>Buttons in START, END, BEFORE_GROUP_HISTORY, AFTER_GROUP_EMPHASIS, GROUP_CUSTOM slots</li>
 *   <li>A ToolbarSwitch in GROUP_CUSTOM</li>
 *   <li>Control buttons: remove-start-btn, set-german-i18n</li>
 *   <li>Delta output, event log, ready indicator</li>
 * </ul>
 */
@Route("erte-test/toolbar")
public class ErteToolbarTestView extends VerticalLayout {

    private final EnhancedRichTextEditor editor;
    private final Pre deltaOutput;
    private final Pre eventLog;
    private final Button startBtn;

    public ErteToolbarTestView() {
        setSizeFull();
        setPadding(true);
        setSpacing(true);

        editor = new EnhancedRichTextEditor();
        editor.setId("test-editor");
        editor.setValue("<p>Toolbar test content</p>");

        // --- Slot components ---

        // START slot
        startBtn = new Button("S");
        startBtn.setId("slot-start-btn");
        startBtn.getElement().setAttribute("title", "Start slot");
        startBtn.addClickListener(e -> appendEvent("StartButtonClicked"));
        editor.addToolbarComponents(ToolbarSlot.START, startBtn);

        // END slot
        var endBtn = new Button("E");
        endBtn.setId("slot-end-btn");
        endBtn.getElement().setAttribute("title", "End slot");
        editor.addToolbarComponents(ToolbarSlot.END, endBtn);

        // BEFORE_GROUP_HISTORY
        var beforeHistoryBtn = new Button("BH");
        beforeHistoryBtn.setId("slot-before-history-btn");
        beforeHistoryBtn.getElement().setAttribute("title",
                "Before history slot");
        editor.addToolbarComponents(ToolbarSlot.BEFORE_GROUP_HISTORY,
                beforeHistoryBtn);

        // AFTER_GROUP_EMPHASIS
        var afterEmphasisBtn = new Button("AE");
        afterEmphasisBtn.setId("slot-after-emphasis-btn");
        afterEmphasisBtn.getElement().setAttribute("title",
                "After emphasis slot");
        editor.addToolbarComponents(ToolbarSlot.AFTER_GROUP_EMPHASIS,
                afterEmphasisBtn);

        // GROUP_CUSTOM — button
        var customGroupBtn = new Button("C");
        customGroupBtn.setId("custom-group-btn");
        customGroupBtn.getElement().setAttribute("title",
                "Custom group button");
        customGroupBtn.addClickListener(
                e -> appendEvent("CustomGroupBtnClicked"));
        editor.addCustomToolbarComponents(customGroupBtn);

        // GROUP_CUSTOM — ToolbarSwitch
        var toolbarSwitch = new ToolbarSwitch("SW");
        toolbarSwitch.setId("toolbar-switch");
        toolbarSwitch.getElement().setAttribute("title", "Toolbar switch");
        toolbarSwitch.addActiveChangedListener(e -> appendEvent(
                "ToolbarSwitchChanged active=" + e.isActive()));
        editor.addCustomToolbarComponents(toolbarSwitch);

        // --- Delta output (client-side text-change) ---
        deltaOutput = new Pre();
        deltaOutput.setId("delta-output");
        deltaOutput.getStyle()
                .set("white-space", "pre-wrap")
                .set("font-size", "var(--lumo-font-size-xs)")
                .set("max-height", "120px")
                .set("overflow", "auto");

        // Client-side: update delta output on every text-change
        editor.getElement().executeJs(
            "const el = this;" +
            "const out = document.getElementById('delta-output');" +
            "if (el._editor) {" +
            "  el._editor.on('text-change', function() {" +
            "    out.textContent = JSON.stringify(el._editor.getContents());" +
            "  });" +
            "}"
        );

        // --- Event log ---
        eventLog = new Pre();
        eventLog.setId("event-log");
        eventLog.getStyle()
                .set("white-space", "pre-wrap")
                .set("font-size", "var(--lumo-font-size-xs)")
                .set("max-height", "120px")
                .set("overflow", "auto");

        // --- Control buttons ---
        var removeStartBtn = new Button("Remove START btn", e -> {
            editor.removeToolbarComponent(ToolbarSlot.START, startBtn);
        });
        removeStartBtn.setId("remove-start-btn");

        var setGermanI18n = new Button("Set German i18n",
                e -> setGermanI18n());
        setGermanI18n.setId("set-german-i18n");

        var controls = new HorizontalLayout(removeStartBtn, setGermanI18n);
        controls.setSpacing(true);

        // --- Ready indicator ---
        var readyIndicator = new Span("ready");
        readyIndicator.setId("test-ready");
        readyIndicator.getStyle().set("display", "none");
        readyIndicator.getElement().setAttribute("data-ready", "true");

        add(editor, controls,
                new Html("<b>Delta:</b>"), deltaOutput,
                new Html("<b>Events:</b>"), eventLog,
                readyIndicator);

        setFlexGrow(1, editor);
    }

    private void appendEvent(String message) {
        eventLog.getElement().executeJs(
            "this.textContent = this.textContent + $0 + '\\n'", message);
    }

    private void setGermanI18n() {
        editor.getElement().executeJs(
            "this.i18n = {" +
            "  undo: 'Rückgängig'," +
            "  redo: 'Wiederholen'," +
            "  bold: 'Fett'," +
            "  italic: 'Kursiv'," +
            "  underline: 'Unterstrichen'," +
            "  strike: 'Durchgestrichen'," +
            "  color: 'Textfarbe'," +
            "  background: 'Hintergrund'," +
            "  h1: 'Überschrift 1'," +
            "  h2: 'Überschrift 2'," +
            "  h3: 'Überschrift 3'," +
            "  subscript: 'Tiefgestellt'," +
            "  superscript: 'Hochgestellt'," +
            "  listOrdered: 'Nummerierte Liste'," +
            "  listBullet: 'Aufzählung'," +
            "  alignLeft: 'Links'," +
            "  alignCenter: 'Mitte'," +
            "  alignRight: 'Rechts'," +
            "  image: 'Bild'," +
            "  link: 'Link'," +
            "  blockquote: 'Zitat'," +
            "  codeBlock: 'Quellcode'," +
            "  clean: 'Formatierung entfernen'," +
            "  indent: 'Einrücken'," +
            "  outdent: 'Ausrücken'" +
            "}"
        );
    }
}
