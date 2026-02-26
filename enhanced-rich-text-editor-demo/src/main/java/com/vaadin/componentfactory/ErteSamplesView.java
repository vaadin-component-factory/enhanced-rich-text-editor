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

import com.vaadin.componentfactory.EnhancedRichTextEditor.ToolbarButton;
import com.vaadin.componentfactory.erte.tables.EnhancedRichTextEditorTables;
import com.vaadin.componentfactory.erte.tables.TablesI18n;
import com.vaadin.componentfactory.erte.tables.templates.TemplateParser;
import com.vaadin.componentfactory.toolbar.ToolbarSlot;
import com.vaadin.componentfactory.toolbar.ToolbarSwitch;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * V25 demo samples view matching the V24 original demo.
 * <p>
 * Contains 14 self-contained feature samples, each demonstrating one ERTE
 * feature with rich pre-loaded content, interactive components, and copy-paste-ready code snippets.
 */
@Route("erte-samples")
@PageTitle("Enhanced RTE Samples")
public class ErteSamplesView extends VerticalLayout {

    public ErteSamplesView() {
        setPadding(true);
        setSpacing(true);
        setMaxWidth("1200px");
        getStyle().set("margin", "0 auto");

        add(createDefaultEditor());
        add(createEditorWithTabstops());
        add(createGetValue());
        add(createGetHtmlValue());
        add(createEditorWithLimitedToolbar());
        add(createEditorWithReadonlySections());
        add(createEditorWithPlaceholders());
        add(createEditorWithCustomButtons());
        add(createEditorWithCustomButtonsExtended());
        add(createEditorWithCustomShortcuts());
        add(createEditorWithIconReplacement());
        add(createEditorWithNoRulers());
        add(createEditorWithTableSample());
        add(createEditorWithTableI18nSample());

        // Load Prism.js for Java syntax highlighting
        UI.getCurrent().getPage().executeJs(
                "if (!document.getElementById('prism-css')) {"
                        + "  var link = document.createElement('link');"
                        + "  link.id = 'prism-css';"
                        + "  link.rel = 'stylesheet';"
                        + "  link.href = 'https://cdnjs.cloudflare.com/ajax/libs/prism/1.29.0/themes/prism-okaidia.min.css';"
                        + "  document.head.appendChild(link);"
                        + "}"
                        + "function loadPrism() {"
                        + "  if (window.Prism) { Prism.highlightAll(); return; }"
                        + "  var script = document.createElement('script');"
                        + "  script.src = 'https://cdnjs.cloudflare.com/ajax/libs/prism/1.29.0/prism.min.js';"
                        + "  script.onload = function() {"
                        + "    var java = document.createElement('script');"
                        + "    java.src = 'https://cdnjs.cloudflare.com/ajax/libs/prism/1.29.0/components/prism-java.min.js';"
                        + "    java.onload = function() { Prism.highlightAll(); };"
                        + "    document.head.appendChild(java);"
                        + "  };"
                        + "  document.head.appendChild(script);"
                        + "}"
                        + "loadPrism();");
    }

    private Component createDefaultEditor() {
        var rte = new EnhancedRichTextEditor();
        rte.setMaxHeight("200px");

        return createCard("Basic Rich Text Editor", rte,
                createSourceCode(
                        "EnhancedRichTextEditor rte = new EnhancedRichTextEditor();\n"
                                + "rte.setMaxHeight(\"200px\");"));
    }

    private Component createEditorWithTabstops() {
        var rte = new EnhancedRichTextEditor();
        rte.setTabStops(List.of(new TabStop(TabStop.Direction.LEFT, 150),
                new TabStop(TabStop.Direction.RIGHT, 350),
                new TabStop(TabStop.Direction.MIDDLE, 550)));

        // Rich content: 3rd tab stop demo + product table
        rte.asDelta().setValue("["
                + "{\"insert\":{\"tab\":true}},{\"insert\":{\"tab\":true}},{\"insert\":{\"tab\":true}},"
                + "{\"insert\":\"3rd tab-stop\",\"attributes\":{\"underline\":true}},"
                + "{\"insert\":\"\\n\\nThis line is just a normal text. Tab-stops are not affecting it.\\n\\n\"},"
                // Product table header
                + "{\"insert\":{\"tab\":true}},{\"insert\":\"Product\",\"attributes\":{\"bold\":true}},"
                + "{\"insert\":{\"tab\":true}},{\"insert\":\"Price\",\"attributes\":{\"bold\":true}},"
                + "{\"insert\":{\"tab\":true}},{\"insert\":\"Quantity\",\"attributes\":{\"bold\":true}},"
                + "{\"insert\":\"\\n\"},"
                // Row 1
                + "{\"insert\":{\"tab\":true}},{\"insert\":\"Apples\"},"
                + "{\"insert\":{\"tab\":true}},{\"insert\":\"2.00\"},"
                + "{\"insert\":{\"tab\":true}},{\"insert\":\"5\"},"
                + "{\"insert\":\"\\n\"},"
                // Row 2
                + "{\"insert\":{\"tab\":true}},{\"insert\":\"Salmon\"},"
                + "{\"insert\":{\"tab\":true}},{\"insert\":\"25.00\"},"
                + "{\"insert\":{\"tab\":true}},{\"insert\":\"2\"},"
                + "{\"insert\":\"\\n\"}" + "]");

        return createCard("Basic Rich Text Editor with Tab-stops", rte,
                createSourceCode("EnhancedRichTextEditor rte = new EnhancedRichTextEditor();\n"
                        + "List<TabStop> tabStops = new ArrayList<>();\n"
                        + "tabStops.add(new TabStop(TabStop.Direction.LEFT, 150));\n"
                        + "tabStops.add(new TabStop(TabStop.Direction.RIGHT, 350));\n"
                        + "tabStops.add(new TabStop(TabStop.Direction.MIDDLE, 550));\n"
                        + "rte.setTabStops(tabStops);\n"
                        + "rte.asDelta().setValue(\"...\"); // Delta with tab embeds"));
    }

    private Component createGetValue() {
        var valueBlock = new TextArea();
        valueBlock.setWidthFull();
        var rte = new EnhancedRichTextEditor();
        var saveBtn = new Button("Save value",
                e -> valueBlock.setValue(rte.asDelta().getValue()));
        var setBtn = new Button("Set value",
                e -> rte.asDelta().setValue(valueBlock.getValue()));
        var buttonRow = new HorizontalLayout(saveBtn, setBtn);
        buttonRow.setSpacing(true);
        buttonRow.setPadding(false);

        return createCard("Save Rich Text Editor value", rte, buttonRow, valueBlock,
                createSourceCode("TextArea valueBlock = new TextArea();\n"
                        + "EnhancedRichTextEditor rte = new EnhancedRichTextEditor();\n"
                        + "Button saveBtn = new Button(\"Save value\",\n"
                        + "    e -> valueBlock.setValue(rte.asDelta().getValue()));\n"
                        + "Button setBtn = new Button(\"Set value\",\n"
                        + "    e -> rte.asDelta().setValue(valueBlock.getValue()));"));
    }

    private Component createGetHtmlValue() {
        var htmlBlock = new Div();
        var rte = new EnhancedRichTextEditor();
        var showHtmlValue = new Button("Show html value", e -> {
            String exsValue = htmlBlock.getElement().getProperty("innerHTML");
            if (exsValue == null || !exsValue.equals(rte.getValue())) {
                htmlBlock.getElement().setProperty("innerHTML", rte.getValue());
            }
        });
        showHtmlValue.getStyle().set("align-self", "flex-start");

        return createCard("Save Rich Text Editor htmlValue", rte, showHtmlValue, htmlBlock,
                createSourceCode("Div htmlBlock = new Div();\n"
                        + "EnhancedRichTextEditor rte = new EnhancedRichTextEditor();\n"
                        + "Button showHtmlValue = new Button(\"Show html value\", e -> {\n"
                        + "    htmlBlock.getElement().setProperty(\"innerHTML\",\n"
                        + "        rte.getValue());\n" + "});"));
    }

    private Component createEditorWithLimitedToolbar() {
        var rte = new EnhancedRichTextEditor();
        Map<ToolbarButton, Boolean> buttons = new HashMap<>();
        buttons.put(ToolbarButton.CLEAN, false);
        buttons.put(ToolbarButton.BLOCKQUOTE, false);
        buttons.put(ToolbarButton.CODE_BLOCK, false);
        buttons.put(ToolbarButton.IMAGE, false);
        buttons.put(ToolbarButton.LINK, false);
        buttons.put(ToolbarButton.STRIKE, false);
        buttons.put(ToolbarButton.READONLY, false);
        rte.setToolbarButtonsVisibility(buttons);

        return createCard("Rich Text Editor with limited toolbar", rte,
                createSourceCode("EnhancedRichTextEditor rte = new EnhancedRichTextEditor();\n"
                        + "Map<ToolbarButton, Boolean> buttons = new HashMap<>();\n"
                        + "buttons.put(ToolbarButton.CLEAN, false);\n"
                        + "buttons.put(ToolbarButton.BLOCKQUOTE, false);\n"
                        + "buttons.put(ToolbarButton.CODE_BLOCK, false);\n"
                        + "buttons.put(ToolbarButton.IMAGE, false);\n"
                        + "buttons.put(ToolbarButton.LINK, false);\n"
                        + "buttons.put(ToolbarButton.STRIKE, false);\n"
                        + "buttons.put(ToolbarButton.READONLY, false);\n"
                        + "rte.setToolbarButtonsVisibility(buttons);"));
    }

    private Component createEditorWithReadonlySections() {
        var rte = new EnhancedRichTextEditor();
        rte.asDelta().setValue("["
                + "{\"insert\":\"Some text\\n\"},"
                + "{\"insert\":\"Some readonly text\\n\",\"attributes\":{\"readonly\":true}},"
                + "{\"insert\":\"More text\\n\"},"
                + "{\"insert\":\"More readonly text\\n\",\"attributes\":{\"readonly\":true}}"
                + "]");

        return createCard("Basic Rich Text Editor with readonly sections", rte,
                createSourceCode("EnhancedRichTextEditor rte = new EnhancedRichTextEditor();\n"
                        + "rte.asDelta().setValue(\"[\"\n"
                        + "    + \"{\\\"insert\\\":\\\"Some text\\\\n\\\"},\"\n"
                        + "    + \"{\\\"insert\\\":\\\"Some readonly text\\\\n\\\",\\\"attributes\\\":{\\\"readonly\\\":true}},\"\n"
                        + "    + \"{\\\"insert\\\":\\\"More text\\\\n\\\"},\"\n"
                        + "    + \"{\\\"insert\\\":\\\"More readonly text\\\\n\\\",\\\"attributes\\\":{\\\"readonly\\\":true}}\"\n"
                        + "    + \"]\");"));
    }

    private Component createEditorWithPlaceholders() {
        var rte = new EnhancedRichTextEditor();

        var p1 = new Placeholder();
        p1.setText("N-1=Vaadin");
        p1.getFormat().put("italic", true);
        p1.getAltFormat().put("italic", false);
        p1.getAltFormat().put("bold", true);

        var p2 = new Placeholder();
        p2.setText("A-1=Turku, 20540");
        p2.getAltFormat().put("link", "https://goo.gl/maps/EX8RTEMUWeEAdkNN8");

        var p3 = new Placeholder();
        p3.setText("D-1=01-01-2000");

        rte.setPlaceholders(List.of(p1, p2, p3));
        rte.setPlaceholderAltAppearance(true);
        rte.setPlaceholderAltAppearancePattern("(?<=\\=).*$");

        rte.addPlaceholderBeforeInsertListener(event -> {
            StringBuilder texts = new StringBuilder();
            for (Placeholder ph : event.getPlaceholders()) {
                texts.append(" ").append(ph.getText());
                texts.append(" at ").append(ph.getIndex());
            }
            Notification.show(texts + " to be inserted");
            event.insert();
        });

        rte.addPlaceholderInsertedListener(event -> {
            StringBuilder texts = new StringBuilder();
            for (Placeholder ph : event.getPlaceholders()) {
                texts.append(" ").append(ph.getText());
                texts.append(" at ").append(ph.getIndex());
            }
            Notification.show(texts + " inserted");
        });

        rte.addPlaceholderBeforeRemoveListener(event -> {
            StringBuilder texts = new StringBuilder();
            for (Placeholder ph : event.getPlaceholders()) {
                texts.append(" ").append(ph.getText());
            }
            Notification.show(texts + " to be removed");
            if (!texts.toString().contains("Turku"))
                event.remove();
        });

        rte.addPlaceholderSelectedListener(event -> {
            StringBuilder texts = new StringBuilder();
            for (Placeholder ph : event.getPlaceholders()) {
                texts.append(" ").append(ph.getText());
            }
            Notification.show(texts + " selected");
        });

        rte.addPlaceholderLeaveListener(event -> Notification.show("Placeholder leaved"));

        rte.addPlaceholderRemovedListener(event -> {
            StringBuilder texts = new StringBuilder();
            for (Placeholder ph : event.getPlaceholders()) {
                texts.append(" ").append(ph.getText());
            }
            Notification.show(texts + " removed");
        });

        rte.addPlaceholderAppearanceChangedListener(event -> {
            if (event.isFromClient())
                Notification.show("Appearance changed to " + event.getAppearanceLabel());
        });

        // Pre-load content with placeholders (V25 delta format)
        rte.asDelta().setValue("["
                + "{\"insert\":\"The company \"},"
                + "{\"insert\":{\"placeholder\":{\"text\":\"N-1=Vaadin\",\"format\":{\"italic\":true},\"altFormat\":{\"italic\":false,\"bold\":true}}}},"
                + "{\"insert\":\", located in \"},"
                + "{\"insert\":{\"placeholder\":{\"text\":\"A-1=Turku, 20540\",\"altFormat\":{\"link\":\"https://goo.gl/maps/EX8RTEMUWeEAdkNN8\"}}}},"
                + "{\"insert\":\", was founded in \"},"
                + "{\"insert\":{\"placeholder\":{\"text\":\"D-1=01-01-2000\"}}},"
                + "{\"insert\":\".\\n\"}" + "]");

        var htmlHolder = new Div();
        rte.addValueChangeListener(event -> {
            htmlHolder.removeAll();
            String html = rte.getValue();
            if (html != null && !html.isEmpty()) {
                htmlHolder.add(new Html("<div>" + html + "</div>"));
            }
        });

        var button = new Button("Click me", event -> Notification.show("Clicked"));
        button.getStyle().set("align-self", "flex-start");

        return createCard("Rich Text Editor with Placeholders", rte, button, htmlHolder,
                createSourceCode("EnhancedRichTextEditor rte = new EnhancedRichTextEditor();\n"
                        + "Placeholder p1 = new Placeholder();\n"
                        + "p1.setText(\"N-1=Vaadin\");\n"
                        + "p1.getFormat().put(\"italic\", true);\n"
                        + "p1.getAltFormat().put(\"bold\", true);\n"
                        + "// ... more placeholders\n"
                        + "rte.setPlaceholders(List.of(p1, p2, p3));\n"
                        + "rte.setPlaceholderAltAppearance(true);\n"
                        + "rte.setPlaceholderAltAppearancePattern(\"(?<=\\\\=).*$\");\n"
                        + "rte.addPlaceholderBeforeInsertListener(e -> { e.insert(); });\n"
                        + "rte.addPlaceholderBeforeRemoveListener(e -> {\n"
                        + "    if (!texts.contains(\"Turku\")) e.remove();\n" + "});"));
    }

    private Component createEditorWithCustomButtons() {
        var rte = new EnhancedRichTextEditor();

        var textButton1 = new Button("");
        textButton1.setIcon(VaadinIcon.AIRPLANE.create());
        textButton1.addClickShortcut(Key.F8);
        textButton1.getElement().setProperty("title", "Airplanes are flying machines.");
        textButton1.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        textButton1.addClickListener(event -> rte.addText("Airplanes are flying machines. "));

        var textButton2 = new Button("");
        textButton2.setIcon(VaadinIcon.DENTAL_CHAIR.create());
        textButton2.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        textButton2.getElement().setProperty("title", "Dentists are drilling people.");
        textButton2.addClickShortcut(Key.F9);
        textButton2.addClickListener(event -> rte.addText("Dentists are drilling people. "));

        rte.addCustomToolbarComponents(textButton1, textButton2);

        var removeBtn = new Button("Remove airplane",
                event -> rte.removeToolbarComponent(ToolbarSlot.GROUP_CUSTOM, textButton1));
        removeBtn.getStyle().set("align-self", "flex-start");

        return createCard("Rich Text Editor With Custom Buttons", rte, removeBtn,
                createSourceCode("EnhancedRichTextEditor rte = new EnhancedRichTextEditor();\n"
                        + "Button textButton1 = new Button(\"\");\n"
                        + "textButton1.setIcon(VaadinIcon.AIRPLANE.create());\n"
                        + "textButton1.addClickShortcut(Key.F8);\n"
                        + "textButton1.addClickListener(event ->\n"
                        + "    rte.addText(\"Airplanes are flying machines. \"));\n"
                        + "rte.addCustomToolbarComponents(textButton1, textButton2);"));
    }

    private Component createEditorWithCustomButtonsExtended() {
        var rte = new EnhancedRichTextEditor();

        var presets = new ComboBox<String>("", "Preset 1", "Preset 2", "Preset 3");
        presets.setValue("Preset 1");
        presets.setTooltipText("Custom component in '" + ToolbarSlot.START.getSlotName() + "' slot");
        rte.addToolbarComponents(ToolbarSlot.START, presets);

        var colors = new Select<String>();
        colors.setItems("Red", "Green", "Blue");
        colors.setValue("Red");
        colors.setTooltipText("Custom component in '"
                + ToolbarSlot.BEFORE_GROUP_GLYPH_TRANSFORMATION.getSlotName() + "' slot");
        rte.addToolbarComponents(ToolbarSlot.BEFORE_GROUP_GLYPH_TRANSFORMATION, colors);

        List<Button> slottedButtons = new LinkedList<>();
        for (ToolbarSlot slot : ToolbarSlot.values()) {
            if (slot == ToolbarSlot.GROUP_CUSTOM)
                continue;

            var btn = new Button(VaadinIcon.CIRCLE_THIN.create(),
                    event -> Notification.show("Button in '" + slot.getSlotName() + "' slot"));
            btn.setTooltipText("Button in '" + slot.getSlotName() + "' slot");
            btn.setVisible(false);
            btn.getStyle().set("color", "red");
            slottedButtons.add(btn);
            rte.addToolbarComponents(slot, btn);
        }

        var toolbarSwitch = new ToolbarSwitch(VaadinIcon.EYE.create());
        toolbarSwitch.addActiveChangedListener(
                event -> slottedButtons.forEach(b -> b.setVisible(event.isActive())));
        toolbarSwitch.setTooltipText("Toggle slot buttons visibility");
        rte.addToolbarComponents(ToolbarSlot.GROUP_CUSTOM, toolbarSwitch);

        var info = new Span(
                "Click the EYE Button to show/hide additional toolbar components between the standard button groups");

        return createCard("Rich Text Editor With Custom Buttons (extended)", info, rte,
                createSourceCode("EnhancedRichTextEditor rte = new EnhancedRichTextEditor();\n"
                        + "ComboBox<String> presets = new ComboBox<>(\"\", ...);\n"
                        + "rte.addToolbarComponents(ToolbarSlot.START, presets);\n\n"
                        + "ToolbarSwitch toolbarSwitch = new ToolbarSwitch(VaadinIcon.EYE.create());\n"
                        + "toolbarSwitch.addActiveChangedListener(event ->\n"
                        + "    slottedButtons.forEach(b -> b.setVisible(event.isActive())));\n"
                        + "rte.addToolbarComponents(ToolbarSlot.GROUP_CUSTOM, toolbarSwitch);"));
    }

    private Component createEditorWithCustomShortcuts() {
        var rte = new EnhancedRichTextEditor();

        // Shift+F9 for align center
        rte.addStandardToolbarButtonShortcut(ToolbarButton.ALIGN_CENTER, "F9", false, true, false);
        // Shift+P for superscript
        rte.addStandardToolbarButtonShortcut(ToolbarButton.SUPERSCRIPT, "P", false, true, false);
        // Ctrl+B for header 1
        rte.addStandardToolbarButtonShortcut(ToolbarButton.H1, "B", true, false, false);
        // F9 to load an image
        rte.addStandardToolbarButtonShortcut(ToolbarButton.IMAGE, "F9", false, false, false);
        // Alt+G for code block
        rte.addStandardToolbarButtonShortcut(ToolbarButton.CODE_BLOCK, "G", false, false, true);
        // Shift+J to focus toolbar
        rte.addToolbarFocusShortcut("J", false, true, false);

        return createCard("Rich Text Editor with custom shortcuts", rte,
                createSourceCode("EnhancedRichTextEditor rte = new EnhancedRichTextEditor();\n"
                        + "// Shift+F9 for align center\n"
                        + "rte.addStandardToolbarButtonShortcut(\n"
                        + "    ToolbarButton.ALIGN_CENTER, \"F9\", false, true, false);\n"
                        + "// Shift+P for superscript\n"
                        + "rte.addStandardToolbarButtonShortcut(\n"
                        + "    ToolbarButton.SUPERSCRIPT, \"P\", false, true, false);\n"
                        + "// Ctrl+B for header 1\n"
                        + "rte.addStandardToolbarButtonShortcut(\n"
                        + "    ToolbarButton.H1, \"B\", true, false, false);\n"
                        + "// Shift+J to focus toolbar\n"
                        + "rte.addToolbarFocusShortcut(\"J\", false, true, false);"));
    }

    private Component createEditorWithIconReplacement() {
        var rte = new EnhancedRichTextEditor();

        var newUndoIcon = new Icon(VaadinIcon.ARROW_BACKWARD);
        newUndoIcon.setColor("grey");
        newUndoIcon.setSize("1.25em");
        rte.replaceStandardToolbarButtonIcon(ToolbarButton.UNDO, newUndoIcon);

        var newRedoIcon = new Icon(VaadinIcon.ARROW_FORWARD);
        newRedoIcon.setColor("grey");
        newRedoIcon.setSize("1.25em");
        rte.replaceStandardToolbarButtonIcon(ToolbarButton.REDO, newRedoIcon);

        var imageIcon = new Icon(VaadinIcon.PICTURE);
        imageIcon.setSize("1.25em");
        rte.replaceStandardToolbarButtonIcon(ToolbarButton.IMAGE, imageIcon);

        return createCard("Rich Text Editor with icon replacement", rte,
                createSourceCode("EnhancedRichTextEditor rte = new EnhancedRichTextEditor();\n"
                        + "Icon newUndoIcon = new Icon(VaadinIcon.ARROW_BACKWARD);\n"
                        + "newUndoIcon.setColor(\"grey\");\n"
                        + "newUndoIcon.setSize(\"1.25em\");\n"
                        + "rte.replaceStandardToolbarButtonIcon(\n"
                        + "    ToolbarButton.UNDO, newUndoIcon);\n" + "// ... same for REDO and IMAGE"));
    }

    private Component createEditorWithNoRulers() {
        var rte = new EnhancedRichTextEditor();
        rte.setMaxHeight("200px");
        rte.setNoRulers(true);

        return createCard("Rich Text Editor with no rulers", rte,
                createSourceCode("EnhancedRichTextEditor rte = new EnhancedRichTextEditor();\n"
                        + "rte.setMaxHeight(\"200px\");\n" + "rte.setNoRulers(true);"));
    }

    private Component createEditorWithTableSample() {
        String deltaString;
        String templatesString;
        try (InputStream deltaStream = getClass().getClassLoader()
                .getResourceAsStream("table-sample-delta.json");
                InputStream templatesStream = getClass().getClassLoader()
                        .getResourceAsStream("table-sample-templates.json")) {
            Objects.requireNonNull(deltaStream);
            Objects.requireNonNull(templatesStream);
            deltaString = new String(deltaStream.readAllBytes(), StandardCharsets.UTF_8);
            templatesString = new String(templatesStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        var rte = new EnhancedRichTextEditor();
        var tables = EnhancedRichTextEditorTables.enable(rte);
        tables.setTemplates(TemplateParser.parseJson(templatesString));
        rte.asDelta().setValue(deltaString);
        rte.setMaxHeight("500px");
        rte.setValueChangeMode(ValueChangeMode.EAGER);

        return createCard("Rich Text Editor with Table Addon", rte,
                createSourceCode("EnhancedRichTextEditor rte = new EnhancedRichTextEditor();\n"
                        + "EnhancedRichTextEditorTables tables =\n"
                        + "    EnhancedRichTextEditorTables.enable(rte);\n"
                        + "tables.setTemplates(\n"
                        + "    TemplateParser.parseJson(templatesString));"));
    }

    private Component createEditorWithTableI18nSample() {
        String deltaString;
        String templatesString;
        try (InputStream deltaStream = getClass().getClassLoader()
                .getResourceAsStream("table-sample-delta.json");
                InputStream templatesStream = getClass().getClassLoader()
                        .getResourceAsStream("table-sample-templates.json")) {
            Objects.requireNonNull(deltaStream);
            Objects.requireNonNull(templatesStream);
            deltaString = new String(deltaStream.readAllBytes(), StandardCharsets.UTF_8);
            templatesString = new String(templatesStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        var rte = new EnhancedRichTextEditor();
        var tablesI18n = new TablesI18n();
        tablesI18n.setInsertTableToolbarSwitchTooltip("Neue Tabelle hinzufügen");
        tablesI18n.setInsertTableRowsFieldLabel("Zeilen");
        tablesI18n.setInsertTableRowsFieldTooltip("Anzahl der hinzuzufügenden Zeilen");
        tablesI18n.setInsertTableColumnsFieldLabel("Spalten");
        tablesI18n.setInsertTableColumnsFieldTooltip("Anzahl der hinzuzufügenden Spalten");
        tablesI18n.setInsertTableAddButtonTooltip("Tabelle hinzufügen");
        tablesI18n.setModifyTableToolbarSwitchTooltip("Tabelle anpassen");
        tablesI18n.setTableTemplatesToolbarSwitchTooltip("Formatvorlagen");

        var templatesI18n = tablesI18n.getTemplatesI18n();
        templatesI18n.setCurrentTemplateSelectFieldLabel("Aktuelle Vorlage");
        templatesI18n.setCurrentTemplateNameNotUniqueError(
                "Es gibt bereits eine Vorlage mit diesem Namen!");
        templatesI18n.setCreateNewTemplateButtonTooltip("Neue Vorlage hinzufügen");
        templatesI18n.setCopyTemplateButtonTooltip("Vorlage kopieren");
        templatesI18n.setDeleteTemplateButtonTooltip("Vorlage löschen");
        templatesI18n.setDeleteTemplateConfirmTitle("Vorlage löschen");
        templatesI18n.setDeleteTemplateConfirmText("Möchten Sie die ausgewählte Vorlage löschen?");
        templatesI18n.setDeleteTemplateConfirmYesButton("Löschen");
        templatesI18n.setDeleteTemplateConfirmNoButton("Abbrechen");

        var tables = EnhancedRichTextEditorTables.enable(rte, tablesI18n);
        tables.setTemplates(TemplateParser.parseJson(templatesString));
        rte.asDelta().setValue(deltaString);
        rte.setMaxHeight("500px");
        rte.setValueChangeMode(ValueChangeMode.EAGER);

        return createCard("Rich Text Editor with Table Addon - I18n Sample", rte,
                createSourceCode("TablesI18n tablesI18n = new TablesI18n();\n"
                        + "tablesI18n.setInsertTableToolbarSwitchTooltip(\n"
                        + "    \"Neue Tabelle hinzufügen\");\n"
                        + "tablesI18n.setInsertTableRowsFieldLabel(\"Zeilen\");\n"
                        + "// ... more i18n settings\n"
                        + "EnhancedRichTextEditorTables tables =\n"
                        + "    EnhancedRichTextEditorTables.enable(rte, tablesI18n);"));
    }

    private Div createCard(String title, Component... content) {
        var card = new Div();
        card.getStyle().set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("border-radius", "var(--lumo-border-radius-l)")
                .set("padding", "var(--lumo-space-l)")
                .set("background", "var(--lumo-base-color)").set("display", "flex")
                .set("flex-direction", "column").set("gap", "var(--lumo-space-s)");

        var h3 = new H3(title);
        h3.getStyle().set("margin", "0");

        card.add(h3);
        for (Component c : content) {
            card.add(c);
        }
        return card;
    }

    private Component createSourceCode(String code) {
        String escaped = code
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
        var wrapper = new Div();
        wrapper.getElement().setProperty("innerHTML",
                "<pre style=\"border-radius:var(--lumo-border-radius-s);"
                        + "font-size:var(--lumo-font-size-xs);margin:0;overflow-x:auto\">"
                        + "<code class=\"language-java\">" + escaped + "</code></pre>");
        return wrapper;
    }
}
