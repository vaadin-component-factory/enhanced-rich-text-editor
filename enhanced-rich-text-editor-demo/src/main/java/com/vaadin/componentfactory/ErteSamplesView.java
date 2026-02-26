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

import com.vaadin.componentfactory.EnhancedRichTextEditor.EnhancedRichTextEditorI18n;
import com.vaadin.componentfactory.EnhancedRichTextEditor.ToolbarButton;
import com.vaadin.componentfactory.erte.tables.EnhancedRichTextEditorTables;
import com.vaadin.componentfactory.erte.tables.TablesI18n;
import com.vaadin.componentfactory.erte.tables.templates.TemplateParser;
import com.vaadin.componentfactory.toolbar.ToolbarSlot;
import com.vaadin.componentfactory.toolbar.ToolbarSwitch;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * Migrated V24 demo samples view.
 * <p>
 * Contains 14 self-contained feature samples, each demonstrating one ERTE
 * feature with copy-paste-ready code.
 */
@Route("erte-samples")
@PageTitle("Enhanced RTE Samples")
public class ErteSamplesView extends VerticalLayout {

    public ErteSamplesView() {
        setPadding(true);
        setSpacing(true);
        setMaxWidth("1200px");
        getStyle().set("margin", "0 auto");

        add(new H2("Enhanced RTE Samples"));
        add(new Span(
                "Individual feature samples — each card demonstrates one feature with copy-paste-ready code."));

        add(createBasicEditor());
        add(createTabStopsEditor());
        add(createDeltaValueEditor());
        add(createHtmlValueEditor());
        add(createLimitedToolbarEditor());
        add(createReadonlySectionsEditor());
        add(createPlaceholdersEditor());
        add(createAddTextEditor());
        add(createToolbarSlotsEditor());
        add(createCustomShortcutsEditor());
        add(createIconReplacementEditor());
        add(createNoRulersEditor());
        add(createTableEditor());
        add(createTableI18nEditor());
    }

    private Component createBasicEditor() {
        var rte = new EnhancedRichTextEditor();
        rte.setWidthFull();
        return createCard("Basic Editor",
                "Minimal setup — just create an instance.", rte);
    }

    private Component createTabStopsEditor() {
        var rte = new EnhancedRichTextEditor();
        rte.setWidthFull();
        rte.setTabStops(List.of(new TabStop(TabStop.Direction.LEFT, 150),
                new TabStop(TabStop.Direction.RIGHT, 350),
                new TabStop(TabStop.Direction.MIDDLE, 550)));
        // Pre-load content with tab embeds
        rte.asDelta().setValue("[" + "{\"insert\":\"Left-aligned\"},"
                + "{\"insert\":{\"tab\":true}}," + "{\"insert\":\"Right-aligned\"},"
                + "{\"insert\":{\"tab\":true}}," + "{\"insert\":\"Centered\"},"
                + "{\"insert\":\"\\n\"}" + "]");
        return createCard("Tab Stops",
                "Three tab stops: left at 150px, right at 350px, center at 550px. Press Tab to insert tab characters.",
                rte);
    }

    private Component createDeltaValueEditor() {
        var rte = new EnhancedRichTextEditor();
        rte.setWidthFull();
        rte.asDelta().setValue(
                "[{\"insert\":\"Hello, World!\",\"attributes\":{\"bold\":true}},{\"insert\":\"\\n\"}]");

        var output = new Pre();
        output.getStyle().set("white-space", "pre-wrap")
                .set("word-break", "break-all")
                .set("font-size", "var(--lumo-font-size-xs)")
                .set("background", "var(--lumo-shade-5pct)")
                .set("padding", "var(--lumo-space-s)").set("margin", "0")
                .set("max-height", "150px").set("overflow", "auto");
        rte.asDelta().addValueChangeListener(e -> output.setText(e.getValue()));

        var btn = new Button("Get Delta",
                e -> output.setText(rte.asDelta().getValue()));

        return createCard("Save Delta Value",
                "Use asDelta().getValue() / asDelta().setValue() to read/write Quill Delta JSON.",
                rte, btn, output);
    }

    private Component createHtmlValueEditor() {
        var rte = new EnhancedRichTextEditor();
        rte.setWidthFull();
        rte.setValue(
                "<p><strong>Bold text</strong> and <em>italic text</em></p>");

        var output = new Pre();
        output.getStyle().set("white-space", "pre-wrap")
                .set("word-break", "break-all")
                .set("font-size", "var(--lumo-font-size-xs)")
                .set("background", "var(--lumo-shade-5pct)")
                .set("padding", "var(--lumo-space-s)").set("margin", "0")
                .set("max-height", "150px").set("overflow", "auto");
        rte.addValueChangeListener(e -> output.setText(e.getValue()));

        var btn = new Button("Get HTML", e -> output.setText(rte.getValue()));

        return createCard("HTML Value",
                "In V25, getValue() returns HTML directly (primary format). Use asDelta() for Delta JSON.",
                rte, btn, output);
    }

    private Component createLimitedToolbarEditor() {
        var rte = new EnhancedRichTextEditor();
        rte.setWidthFull();
        rte.setToolbarButtonsVisibility(
                Map.ofEntries(Map.entry(ToolbarButton.BOLD, true),
                        Map.entry(ToolbarButton.ITALIC, true),
                        Map.entry(ToolbarButton.UNDERLINE, true),
                        Map.entry(ToolbarButton.H1, true),
                        Map.entry(ToolbarButton.H2, true),
                        Map.entry(ToolbarButton.CLEAN, true),
                        // Hide everything else
                        Map.entry(ToolbarButton.UNDO, false),
                        Map.entry(ToolbarButton.REDO, false),
                        Map.entry(ToolbarButton.STRIKE, false),
                        Map.entry(ToolbarButton.COLOR, false),
                        Map.entry(ToolbarButton.BACKGROUND, false),
                        Map.entry(ToolbarButton.H3, false),
                        Map.entry(ToolbarButton.SUBSCRIPT, false),
                        Map.entry(ToolbarButton.SUPERSCRIPT, false),
                        Map.entry(ToolbarButton.LIST_ORDERED, false),
                        Map.entry(ToolbarButton.LIST_BULLET, false),
                        Map.entry(ToolbarButton.OUTDENT, false),
                        Map.entry(ToolbarButton.INDENT, false),
                        Map.entry(ToolbarButton.ALIGN_LEFT, false),
                        Map.entry(ToolbarButton.ALIGN_CENTER, false),
                        Map.entry(ToolbarButton.ALIGN_RIGHT, false),
                        Map.entry(ToolbarButton.IMAGE, false),
                        Map.entry(ToolbarButton.LINK, false),
                        Map.entry(ToolbarButton.BLOCKQUOTE, false),
                        Map.entry(ToolbarButton.CODE_BLOCK, false)));
        return createCard("Limited Toolbar",
                "Only Bold, Italic, Underline, H1, H2, and Clean buttons visible. Groups with all buttons hidden auto-collapse.",
                rte);
    }

    private Component createReadonlySectionsEditor() {
        var rte = new EnhancedRichTextEditor();
        rte.setWidthFull();
        rte.asDelta().setValue("[" + "{\"insert\":\"Editable text before. \"},"
                + "{\"insert\":\"This section is protected.\",\"attributes\":{\"readonly\":true}},"
                + "{\"insert\":\" Editable text after.\\n\"}" + "]");
        return createCard("Readonly Sections",
                "Protected text uses the readonly format attribute. Toggle readonly via the lock toolbar button.",
                rte);
    }

    private Component createPlaceholdersEditor() {
        var rte = new EnhancedRichTextEditor();
        rte.setWidthFull();

        var p1 = new Placeholder("{{name}}");
        p1.getFormat().put("bold", true);
        var p2 = new Placeholder("{{company}}");
        p2.getFormat().put("italic", true);
        var p3 = new Placeholder("{{date}}");

        rte.setPlaceholders(List.of(p1, p2, p3));
        rte.setPlaceholderTags("@", "");
        rte.addPlaceholderBeforeInsertListener(e -> {
            Notification.show("Inserting: " + e.getPlaceholders().stream()
                    .map(Placeholder::getText)
                    .reduce((a, b) -> a + ", " + b).orElse(""));
            e.insert();
        });
        rte.addPlaceholderInsertedListener(e -> Notification.show(
                "Inserted: " + e.getPlaceholders().size() + " placeholder(s)"));

        return createCard("Placeholders",
                "Click the placeholder toolbar button to insert. Tags display as @name. Event listeners show notifications.",
                rte);
    }

    private Component createAddTextEditor() {
        var rte = new EnhancedRichTextEditor();
        rte.setWidthFull();
        rte.asDelta().setValue(
                "[{\"insert\":\"Click a button to insert text.\"},{\"insert\":\"\\n\"}]");

        var btnCursor = new Button("Insert at cursor",
                e -> rte.addText("INSERTED "));
        var btnPos = new Button("Insert at position 0",
                e -> rte.addText("START ", 0));

        return createCard("Programmatic Text Insertion",
                "addText(text) inserts at cursor, addText(text, pos) at a specific position.",
                rte, btnCursor, btnPos);
    }

    private Component createToolbarSlotsEditor() {
        var rte = new EnhancedRichTextEditor();
        rte.setWidthFull();

        // START slot
        var startBtn = new Button("S");
        startBtn.getElement().setAttribute("title", "Start slot button");
        rte.addToolbarComponents(ToolbarSlot.START, startBtn);

        // AFTER_GROUP_HEADING slot
        var headingBtn = new Button("H+");
        headingBtn.getElement().setAttribute("title", "After heading group");
        rte.addToolbarComponents(ToolbarSlot.AFTER_GROUP_HEADING, headingBtn);

        // GROUP_CUSTOM
        var wsSwitch = new ToolbarSwitch("WS");
        wsSwitch.getElement().setAttribute("title", "Whitespace indicators");
        rte.addCustomToolbarComponents(wsSwitch);

        // END slot
        var endBtn = new Button("E");
        endBtn.getElement().setAttribute("title", "End slot button");
        rte.addToolbarComponents(ToolbarSlot.END, endBtn);

        return createCard("Toolbar Slots",
                "Custom components in 4 slots: START (S), AFTER_GROUP_HEADING (H+), GROUP_CUSTOM (WS toggle), END (E).",
                rte);
    }

    private Component createCustomShortcutsEditor() {
        var rte = new EnhancedRichTextEditor();
        rte.setWidthFull();

        // F9 triggers center alignment
        rte.addStandardToolbarButtonShortcut(ToolbarButton.ALIGN_CENTER, "F9",
                false, false, false);
        // Ctrl+G triggers bold
        rte.addStandardToolbarButtonShortcut(ToolbarButton.BOLD, "G", true,
                false, false);
        // Shift+F10 focuses toolbar
        rte.addToolbarFocusShortcut("F10", false, true, false);

        return createCard("Custom Keyboard Shortcuts",
                "F9 = center align, Ctrl+G = bold, Shift+F10 = focus toolbar. Uses string key names (V25 API).",
                rte);
    }

    private Component createIconReplacementEditor() {
        var rte = new EnhancedRichTextEditor();
        rte.setWidthFull();
        rte.replaceStandardToolbarButtonIcon(ToolbarButton.BOLD,
                new Icon(VaadinIcon.BOLD));
        rte.replaceStandardToolbarButtonIcon(ToolbarButton.ITALIC,
                new Icon(VaadinIcon.ITALIC));
        rte.replaceStandardToolbarButtonIcon(ToolbarButton.UNDO,
                new Icon(VaadinIcon.ARROW_BACKWARD));
        rte.replaceStandardToolbarButtonIcon(ToolbarButton.REDO,
                new Icon(VaadinIcon.ARROW_FORWARD));

        return createCard("Replace Toolbar Button Icons",
                "replaceStandardToolbarButtonIcon() replaces default icons with custom Vaadin Icons.",
                rte);
    }

    private Component createNoRulersEditor() {
        var rte = new EnhancedRichTextEditor();
        rte.setWidthFull();
        rte.setNoRulers(true);
        return createCard("No Rulers",
                "setNoRulers(true) hides the horizontal and vertical rulers.", rte);
    }

    private Component createTableEditor() {
        var rte = new EnhancedRichTextEditor();
        rte.setWidthFull();

        var tables = EnhancedRichTextEditorTables.enable(rte);

        try (InputStream is = getClass().getClassLoader()
                .getResourceAsStream("table-sample-templates.json")) {
            if (is != null) {
                String json = new String(is.readAllBytes(),
                        java.nio.charset.StandardCharsets.UTF_8);
                tables.setTemplates(TemplateParser.parseJson(json));
            }
        } catch (IOException ignored) {
        }

        // Pre-load a simple table
        rte.asDelta()
                .setValue("["
                        + "{\"insert\":\"Name\",\"attributes\":{\"bold\":true}},"
                        + "{\"insert\":\"\\n\",\"attributes\":{\"td\":\"t1|r1|c1||||\"}},"
                        + "{\"insert\":\"Value\",\"attributes\":{\"bold\":true}},"
                        + "{\"insert\":\"\\n\",\"attributes\":{\"td\":\"t1|r1|c2||||\"}},"
                        + "{\"insert\":\"Feature A\"},"
                        + "{\"insert\":\"\\n\",\"attributes\":{\"td\":\"t1|r2|c1||||\"}},"
                        + "{\"insert\":\"Enabled\"},"
                        + "{\"insert\":\"\\n\",\"attributes\":{\"td\":\"t1|r2|c2||||\"}},"
                        + "{\"insert\":\"\\n\"}" + "]");

        return createCard("Table Addon",
                "EnhancedRichTextEditorTables.enable(editor) adds table support with templates.",
                rte);
    }

    private Component createTableI18nEditor() {
        var rte = new EnhancedRichTextEditor();
        rte.setWidthFull();

        // German ERTE I18n
        rte.setI18n(new EnhancedRichTextEditorI18n().setBold("Fett")
                .setItalic("Kursiv").setUnderline("Unterstrichen")
                .setStrike("Durchgestrichen").setH1("Überschrift 1")
                .setH2("Überschrift 2").setH3("Überschrift 3")
                .setSubscript("Tiefgestellt").setSuperscript("Hochgestellt")
                .setListOrdered("Nummerierte Liste").setListBullet("Aufzählung")
                .setAlignLeft("Linksbündig").setAlignCenter("Zentriert")
                .setAlignRight("Rechtsbündig").setAlignJustify("Blocksatz")
                .setOutdent("Einzug verkleinern").setIndent("Einzug vergrößern")
                .setImage("Bild").setLink("Link").setBlockquote("Zitat")
                .setCodeBlock("Code-Block")
                .setClean("Formatierung entfernen").setUndo("Rückgängig")
                .setRedo("Wiederherstellen").setReadonly("Schreibschutz")
                .setPlaceholder("Platzhalter")
                .setWhitespace("Leerzeichen anzeigen"));

        // German Tables I18n
        var tablesI18n = new TablesI18n();
        tablesI18n.setInsertTableRowsFieldLabel("Zeilen");
        tablesI18n.setInsertTableColumnsFieldLabel("Spalten");
        tablesI18n.setInsertTableAddButtonTooltip("Tabelle einfügen");
        tablesI18n.setInsertTableToolbarSwitchTooltip("Tabelle einfügen");
        tablesI18n.setModifyTableToolbarSwitchTooltip("Tabelle bearbeiten");
        tablesI18n.setModifyTableAppendRowAboveItemLabel(
                "Zeile darüber einfügen");
        tablesI18n
                .setModifyTableAppendRowBelowItemLabel("Zeile darunter einfügen");
        tablesI18n.setModifyTableRemoveRowItemLabel("Zeile entfernen");
        tablesI18n.setModifyTableAppendColumnBeforeItemLabel(
                "Spalte davor einfügen");
        tablesI18n.setModifyTableAppendColumnAfterItemLabel(
                "Spalte danach einfügen");
        tablesI18n.setModifyTableRemoveColumnItemLabel("Spalte entfernen");
        tablesI18n.setModifyTableRemoveTableItemLabel("Tabelle entfernen");
        tablesI18n.setModifyTableMergeCellsItemLabel("Zellen verbinden");
        tablesI18n.setModifyTableSplitCellItemLabel("Zelle teilen");
        tablesI18n
                .setTableTemplatesToolbarSwitchTooltip("Tabellenvorlagen");

        EnhancedRichTextEditorTables.enable(rte, tablesI18n);

        return createCard("Table I18n (German)",
                "Full German localization of ERTE labels and table addon labels using TablesI18n.",
                rte);
    }

    private Div createCard(String title, String description,
            Component... content) {
        var card = new Div();
        card.getStyle().set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("border-radius", "var(--lumo-border-radius-l)")
                .set("padding", "var(--lumo-space-l)")
                .set("background", "var(--lumo-base-color)")
                .set("display", "flex").set("flex-direction", "column")
                .set("gap", "var(--lumo-space-s)");

        var h3 = new H3(title);
        h3.getStyle().set("margin", "0");

        var desc = new Span(description);
        desc.getStyle()
                .set("color", "var(--lumo-secondary-text-color)")
                .set("font-size", "var(--lumo-font-size-s)");

        card.add(h3, desc);
        for (Component c : content) {
            card.add(c);
        }
        return card;
    }
}
