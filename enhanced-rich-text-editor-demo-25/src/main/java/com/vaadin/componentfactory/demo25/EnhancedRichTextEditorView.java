/*-
 * #%L
 * Enhanced Rich Text Editor Demo (V25)
 * %%
 * Copyright (C) 2025 Vaadin Ltd
 * %%
 * This program is available under Commercial Vaadin Add-On License 3.0
 * (CVALv3).
 *
 * See the file licensing.txt distributed with this software for more
 * information about licensing.
 *
 * You should have received a copy of the CVALv3 along with this program.
 * If not, see <http://vaadin.com/license/cval-3>.
 * #L%
 */
package com.vaadin.componentfactory.demo25;

import com.vaadin.componentfactory.Placeholder;
import com.vaadin.componentfactory.TabStop;
import com.vaadin.componentfactory.erte.tables.EnhancedRichTextEditorTables;
import com.vaadin.componentfactory.erte.tables.TablesI18n;
import com.vaadin.componentfactory.toolbar.ToolbarSlot;
import com.vaadin.componentfactory.toolbar.ToolbarSwitch;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.DialogVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.richtexteditor.EnhancedRichTextEditor;
import com.vaadin.flow.component.richtexteditor.EnhancedRichTextEditor.ToolbarButton;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import org.jspecify.annotations.NonNull;
import tools.jackson.databind.node.ObjectNode;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * View for {@link EnhancedRichTextEditor} demo (V25).
 * <p>
 * Migrated from the V24 demo, showcasing all 14 editor features.
 */
@Route("")
@RouteAlias("enhanced-rich-text-editor")
public class EnhancedRichTextEditorView extends VerticalLayout {

    public EnhancedRichTextEditorView() {
        setPadding(true);
        setSpacing(true);

//        createDefaultEditor();
//        createEditorWithTabstops();
//        createGetValue();
//        createGetHtmlValue();
//        createEditorWithLimitedToolbar();
//        createEditorWithReadonlySections();
//        createEditorWithPlaceholders();
//        createEditorWithCustomButtons();
//        createEditorWithCustomButtonsExtended();
//        createEditorWithCustomShortcutsForStandardButtons();
//        createEditorWithIconReplacementForStandardButtons();
//        createEditorWithNoRulers();
        createEditorWithTableSample();
//        createEditorWithTableI18nSample();
    }

    private void addCard(String heading, Component... components) {
        Div card = new Div();
        card.getStyle()
                .set("border", "1px solid var(--lumo-contrast-20pct)")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("padding", "var(--lumo-space-m)")
                .set("margin-bottom", "var(--lumo-space-m)");
        card.setWidthFull();

        H3 title = new H3(heading);
        title.getStyle().set("margin-top", "0");
        card.add(title);
        card.add(components);
        add(card);
    }

    private void createDefaultEditor() {
        EnhancedRichTextEditor rte = initEditor();
        rte.setMaxHeight("200px");
        addCard("Basic Rich Text Editor", rte);
    }

    private @NonNull EnhancedRichTextEditor initEditor() {
        EnhancedRichTextEditor rte = new EnhancedRichTextEditor();
        rte.setHeight("200px");
        return rte;
    }

    private void createEditorWithTabstops() {
        EnhancedRichTextEditor rte = initEditor();

        List<TabStop> tabStops = new ArrayList<>();
        tabStops.add(new TabStop(TabStop.Direction.LEFT, 150));
        tabStops.add(new TabStop(TabStop.Direction.RIGHT, 350));
        tabStops.add(new TabStop(TabStop.Direction.MIDDLE, 550));

        rte.setTabStops(tabStops);
        rte.asDelta().setValue("[{\"attributes\":{\"tab\":\"3\"},\"insert\":\"﻿\"},"
                + "{\"attributes\":{\"line-part\":true},\"insert\":\"﻿\"},"
                + "{\"attributes\":{\"underline\":true,\"line-part\":true},\"insert\":\"3rd tab-stop\"},"
                + "{\"attributes\":{\"tabs-cont\":\"TABS-CONT\"},\"insert\":\"\\n\"},{\"insert\":\"\\nThis line is just a normal text. Tab-stops are not affecting it.\\n\\n\"},"
                + "{\"attributes\":{\"tab\":\"1\"},\"insert\":\"﻿\"},{\"attributes\":{\"bold\":true,\"line-part\":true},\"insert\":\"Product\"},"
                + "{\"attributes\":{\"tab\":\"1\"},\"insert\":\"﻿\"},{\"attributes\":{\"bold\":true,\"line-part\":true},\"insert\":\"Price\"},"
                + "{\"attributes\":{\"tab\":\"1\"},\"insert\":\"﻿\"},{\"attributes\":{\"bold\":true,\"line-part\":true},\"insert\":\"Quantity\"},"
                + "{\"attributes\":{\"tabs-cont\":\"TABS-CONT\"},\"insert\":\"\\n\"},{\"attributes\":{\"tab\":\"1\"},\"insert\":\"﻿\"},"
                + "{\"attributes\":{\"line-part\":true},\"insert\":\"Apples\"},{\"attributes\":{\"tab\":\"1\"},\"insert\":\"﻿\"},"
                + "{\"attributes\":{\"line-part\":true},\"insert\":\"2.00\"},{\"attributes\":{\"tab\":\"1\"},\"insert\":\"﻿\"},"
                + "{\"attributes\":{\"line-part\":true},\"insert\":\"5\"},{\"attributes\":{\"tabs-cont\":\"TABS-CONT\"},\"insert\":\"\\n\"},"
                + "{\"attributes\":{\"tab\":\"1\"},\"insert\":\"﻿\"},{\"attributes\":{\"line-part\":true},\"insert\":\"Salmon\"},"
                + "{\"attributes\":{\"tab\":\"1\"},\"insert\":\"﻿\"},{\"attributes\":{\"line-part\":true},\"insert\":\"25.00\"},{\"attributes\":{\"tab\":\"1\"},\"insert\":\"﻿\"},"
                + "{\"attributes\":{\"line-part\":true},\"insert\":\"2\"},{\"attributes\":{\"tabs-cont\":\"TABS-CONT\"},\"insert\":\"\\n\"},"
                + "{\"attributes\":{\"tab\":\"1\"},\"insert\":\"﻿\"},{\"attributes\":{\"tab\":\"true\"},\"insert\":\"﻿\"},{\"insert\":\"\\n\"},"
                + "{\"attributes\":{\"tab\":\"true\"},\"insert\":\"﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿\"},{\"insert\":\"\\n\"},{\"attributes\":{\"tab\":\"true\"},\"insert\":\"﻿\"},"
                + "{\"attributes\":{\"bold\":true,\"tab\":\"1\"},\"insert\":\"﻿\"},{\"insert\":\"\\n\"},{\"attributes\":{\"tab\":\"1\"},\"insert\":\"﻿\"},"
                + "{\"attributes\":{\"tab\":\"true\"},\"insert\":\"﻿\"},{\"attributes\":{\"tab\":\"1\"},\"insert\":\"﻿\"},{\"insert\":\"\\n\"}]");

        addCard("Basic Rich Text Editor with Tab-stops", rte);
    }

    private void createGetValue() {
        TextArea valueBlock = new TextArea();
        EnhancedRichTextEditor rte = initEditor();
        Button saveBtn = new Button("Save value",
                e -> valueBlock.setValue(rte.asDelta().getValue()));
        Button setBtn = new Button("Set value",
                e -> rte.asDelta().setValue(valueBlock.getValue()));

        addCard("Save Rich Text Editor value", rte, saveBtn, setBtn,
                valueBlock);
    }

    private void createGetHtmlValue() {
        Div htmlBlock = new Div();
        EnhancedRichTextEditor rte = initEditor();
        Button showHtmlValue = new Button("Show html value", e -> {
            String exsValue = htmlBlock.getElement().getProperty("innerHTML");
            String htmlValue = rte.getValue();
            if (exsValue == null || !exsValue.equals(htmlValue)) {
                htmlBlock.getElement().setProperty("innerHTML", htmlValue);
            }
        });

        addCard("Save Rich Text Editor htmlValue", rte, showHtmlValue,
                htmlBlock);
    }

    private void createEditorWithLimitedToolbar() {
        EnhancedRichTextEditor rte = initEditor();
        Map<ToolbarButton, Boolean> buttons = new HashMap<>();
        buttons.put(ToolbarButton.CLEAN, false);
        buttons.put(ToolbarButton.BLOCKQUOTE, false);
        buttons.put(ToolbarButton.CODE_BLOCK, false);
        buttons.put(ToolbarButton.IMAGE, false);
        buttons.put(ToolbarButton.LINK, false);
        buttons.put(ToolbarButton.STRIKE, false);
        buttons.put(ToolbarButton.READONLY, false);
        rte.setToolbarButtonsVisibility(buttons);

        addCard("Rich Text Editor with limited toolbar", rte);
    }

    private void createEditorWithReadonlySections() {
        EnhancedRichTextEditor rte = initEditor();
        rte.asDelta().setValue("[" + "{\"insert\":\"Some text\\n\"},"
                + "{\"insert\":{\"readonly\":\"Some readonly text\\n\"}},"
                + "{\"insert\":\"More text\\n\"},"
                + "{\"insert\":{\"readonly\":\"More readonly text\\n\"}}]");

        addCard("Basic Rich Text Editor with readonly sections", rte);
    }

    private void createEditorWithPlaceholders() {
        EnhancedRichTextEditor rte = initEditor();

        List<Placeholder> placeholders = new ArrayList<>();
        Placeholder placeholder1 = new Placeholder();
        placeholder1.setText("N-1=Vaadin");
        placeholder1.getFormat().put("italic", true);
        placeholder1.getAltFormat().put("italic", false);
        placeholder1.getAltFormat().put("bold", true);

        Placeholder placeholder2 = new Placeholder();
        placeholder2.setText("A-1=Turku, 20540");
        placeholder2.getAltFormat().put("link",
                "https://goo.gl/maps/EX8RTEMUWeEAdkNN8");

        Placeholder placeholder3 = new Placeholder();
        placeholder3.setText("D-1=01-01-2000");

        placeholders.add(placeholder1);
        placeholders.add(placeholder2);
        placeholders.add(placeholder3);

        rte.setPlaceholderAltAppearance(true);
        rte.setPlaceholderAltAppearancePattern("(?<=\\=).*$");

        rte.setPlaceholders(placeholders);

        rte.addPlaceholderBeforeInsertListener(event -> {
            String texts = "";
            for (Placeholder placeholder : event.getPlaceholders()) {
                texts += " " + placeholder.getText();
                texts += " at " + placeholder.getIndex();
            }
            Notification.show(texts + " to be inserted");
            event.insert();
        });

        rte.addPlaceholderInsertedListener(event -> {
            String texts = "";
            for (Placeholder placeholder : event.getPlaceholders()) {
                texts += " " + placeholder.getText();
                texts += " at " + placeholder.getIndex();
            }
            Notification.show(texts + " inserted");
        });

        rte.addPlaceholderBeforeRemoveListener(event -> {
            String texts = "";
            for (Placeholder placeholder : event.getPlaceholders()) {
                texts += " " + placeholder.getText();
            }
            Notification
                    .show(texts + " to be removed");
            if (!texts.contains("Turku"))
                event.remove();
        });

        rte.addPlaceholderSelectedListener(event -> {
            String texts = "";
            for (Placeholder placeholder : event.getPlaceholders()) {
                texts += " " + placeholder.getText();
            }
            Notification
                    .show(texts + " selected");
        });

        rte.addPlaceholderLeaveListener(event -> {
            Notification.show("Placeholder leaved");
        });

        rte.addPlaceholderRemovedListener(event -> {
            String texts = "";
            for (Placeholder placeholder : event.getPlaceholders()) {
                texts += " " + placeholder.getText();
            }
            Notification.show(texts + " removed");
        });

        rte.addPlaceholderAppearanceChangedListener(event -> {
            if (event.getAltAppearance() != null)
                Notification.show(
                        "Appearance changed to " + event.getAppearanceLabel());
        });

        Div valueHolder = new Div();
        rte.addValueChangeListener(event -> {
            String htmlValue = rte.getValue();
            if (htmlValue != null && !htmlValue.isEmpty()) {
                Html value = new Html("<div>" + htmlValue + "</div>");
                valueHolder.removeAll();
                valueHolder.add(value);
            }
        });

        Button button = new Button("Click me");
        button.addClickListener(event -> {
            Notification.show("Clicked");
            rte.getElement().callJsFunction("focus");
        });

        rte.asDelta().setValue(
                "[{\"insert\":\"The company \"},{\"insert\":{\"placeholder\":{\"text\":\"N-1=Vaadin\",\"format\":{\"italic\":true},\"altFormat\":{\"italic\":false,\"bold\":true}}}},{\"insert\":\", located in \"},{\"insert\":{\"placeholder\":{\"text\":\"A-1=Turku, 20540\",\"altFormat\":{\"link\":\"https://goo.gl/maps/EX8RTEMUWeEAdkNN8\"}}}},{\"insert\":\", was founded in \"},{\"insert\":{\"placeholder\":{\"text\":\"D-1=01-01-2000\"}}},{\"insert\":\".\"}]");
        addCard("Rich Text Editor with Placeholders", rte, button, valueHolder);
    }

    private void createEditorWithCustomButtons() {
        EnhancedRichTextEditor rte = initEditor();

        Button textButton1 = new Button("");
        textButton1.setIcon(VaadinIcon.AIRPLANE.create());
        textButton1.addClickShortcut(Key.F8);
        textButton1.getElement().setProperty("title", "Airplanes are flying machines.");
        textButton1.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        textButton1.addClickListener(event -> {
            rte.addText("Airplanes are flying machines. ");
        });

        Button textButton2 = new Button("");
        textButton2.setIcon(VaadinIcon.DENTAL_CHAIR.create());
        textButton2.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        textButton2.getElement().setProperty("title", "Dentists are drilling people.");
        textButton2.addClickShortcut(Key.F9);
        textButton2.addClickListener(event -> {
            rte.addText("Dentists are drilling people. ");
        });

        rte.addCustomToolbarComponents(textButton1, textButton2);

        Button button = new Button("Remove airplane");
        button.addClickListener(event -> {
            rte.removeToolbarComponent(ToolbarSlot.GROUP_CUSTOM, textButton1);
        });

        addCard("Rich Text Editor With Custom Buttons", rte, button);
    }

    private void createEditorWithCustomButtonsExtended() {
        EnhancedRichTextEditor rte = initEditor();

        ComboBox<String> presets = new ComboBox<>("", "Preset 1", "Preset 2", "Preset 3");
        presets.setValue("Preset 1");
        presets.setTooltipText("A (non functional) custom toolbar component, placed in the '" + ToolbarSlot.START.getSlotName() + "' slot");
        rte.addToolbarComponents(ToolbarSlot.START, presets);

        Select<String> colors = new Select<>();
        colors.setItems("Red", "Green", "Blue");
        colors.setValue("Red");
        colors.setTooltipText("A (non functional) custom toolbar component, placed in the '" + ToolbarSlot.BEFORE_GROUP_GLYPH_TRANSFORMATION.getSlotName() + "' slot");
        rte.addToolbarComponents(ToolbarSlot.BEFORE_GROUP_GLYPH_TRANSFORMATION, colors);

        List<Button> slottedButtons = new LinkedList<>();
        for (ToolbarSlot slot : ToolbarSlot.values()) {
            if (slot == ToolbarSlot.GROUP_CUSTOM) {
                continue;
            }

            Button button = new Button(VaadinIcon.CIRCLE_THIN.create(), event -> {
                Notification.show("This is a button in the '" + slot.getSlotName() + "' slot of the toolbar. " +
                                  "You can add more buttons or other components, like ComboBoxes, if you want to.");
            });

            button.setTooltipText("A button in the '" + slot.getSlotName() + "' slot");
            button.setVisible(false);
            button.getStyle().set("color", "red");
            slottedButtons.add(button);
            rte.addToolbarComponents(slot, button);
        }

        ToolbarSwitch toolbarSwitch = new ToolbarSwitch(VaadinIcon.EYE.create());
        toolbarSwitch.addActiveChangedListener(event -> {
            boolean active = event.isActive();
            slottedButtons.forEach(b -> b.setVisible(active));
        });
        toolbarSwitch.setTooltipText("A toolbar switch, placed in the '" +
                                     ToolbarSlot.GROUP_CUSTOM.getSlotName() + "' slot. Click to show/hide" +
                                     " additional toolbar components, that are placed between" +
                                     " the standard button groups");
        rte.addToolbarComponents(ToolbarSlot.GROUP_CUSTOM, toolbarSwitch);

        Span info = new Span("Click the EYE Button to show/hide additional toolbar components, that are placed between" +
                             " the standard button groups");

        addCard("Rich Text Editor With Custom Buttons (extended)", info, rte);
    }

    private void createEditorWithCustomShortcutsForStandardButtons() {
        EnhancedRichTextEditor rte = initEditor();

        // shift + f9 for align center button
        rte.addStandardToolbarButtonShortcut(ToolbarButton.ALIGN_CENTER, 120, false, true, false);
        // shift + P for superscript
        rte.addStandardToolbarButtonShortcut(ToolbarButton.SUPERSCRIPT, 80, false, true, false);
        // ctrl + B for header 1
        rte.addStandardToolbarButtonShortcut(ToolbarButton.H1, 66, true, false, false);
        // f9 to load an image
        rte.addStandardToolbarButtonShortcut(ToolbarButton.IMAGE, 120, false, false, false);
        // alt + G for code block
        rte.addStandardToolbarButtonShortcut(ToolbarButton.CODE_BLOCK, 71, false, false, true);

        // focus toolbar with shift + J
        rte.addToolbarFocusShortcut(74, false, true, false);

        addCard("Basic Rich Text Editor with custom shortcuts for standard buttons", rte);
    }

    private void createEditorWithIconReplacementForStandardButtons() {
        EnhancedRichTextEditor rte = initEditor();

        // replace undo button icon
        Icon newUndoIcon = new Icon(VaadinIcon.ARROW_BACKWARD);
        newUndoIcon.setColor("grey");
        newUndoIcon.setSize("1.25em");
        rte.replaceStandardToolbarButtonIcon(ToolbarButton.UNDO, newUndoIcon);

        // replace redo button icon
        Icon newRedoIcon = new Icon(VaadinIcon.ARROW_FORWARD);
        newRedoIcon.setColor("grey");
        newRedoIcon.setSize("1.25em");
        rte.replaceStandardToolbarButtonIcon(ToolbarButton.REDO, newRedoIcon);

        // replace image button icon
        Icon imageIcon = new Icon(VaadinIcon.PICTURE);
        imageIcon.setSize("1.25em");
        rte.replaceStandardToolbarButtonIcon(ToolbarButton.IMAGE, imageIcon);

        addCard("Basic Rich Text Editor with icon replacement for standard buttons", rte);
    }

    private void createEditorWithNoRulers() {
        EnhancedRichTextEditor rte = initEditor();
        rte.setMaxHeight("200px");
        rte.setNoRulers(true);
        addCard("Rich Text Editor with no rulers", rte);
    }

    private void createEditorWithTableSample() {
        String deltaString;
        String templatesString;

        try (
                InputStream deltaStream = getClass().getClassLoader().getResourceAsStream("table-sample-delta.json");
                InputStream templatesStream = getClass().getClassLoader().getResourceAsStream("table-sample-templates.json")
        ) {
            Objects.requireNonNull(deltaStream);
            Objects.requireNonNull(templatesStream);

            deltaString = new String(deltaStream.readAllBytes(), StandardCharsets.UTF_8);
            templatesString = new String(templatesStream.readAllBytes(), StandardCharsets.UTF_8);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        EnhancedRichTextEditor rte = initEditor();
        EnhancedRichTextEditorTables tables = EnhancedRichTextEditorTables.enable(rte);

        tables.setTemplates((ObjectNode) JacksonUtils.readTree(templatesString));

        rte.asDelta().setValue(deltaString);
        rte.setMaxHeight("500px");
        rte.setValueChangeMode(ValueChangeMode.EAGER);

        add(rte);
//        addCard("Rich Text Editor with Table Addon", rte);
    }

    private void createEditorWithTableI18nSample() {
        String deltaString;
        String templatesString;

        try (
                InputStream deltaStream = getClass().getClassLoader().getResourceAsStream("table-sample-delta.json");
                InputStream templatesStream = getClass().getClassLoader().getResourceAsStream("table-sample-templates.json")
        ) {
            Objects.requireNonNull(deltaStream);
            Objects.requireNonNull(templatesStream);

            deltaString = new String(deltaStream.readAllBytes(), StandardCharsets.UTF_8);
            templatesString = new String(templatesStream.readAllBytes(), StandardCharsets.UTF_8);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        EnhancedRichTextEditor rte = initEditor();

        TablesI18n tablesI18n = new TablesI18n();
        tablesI18n.setInsertTableToolbarSwitchTooltip("Neue Tabelle hinzufuegen");
        tablesI18n.setInsertTableRowsFieldLabel("Zeilen");
        tablesI18n.setInsertTableRowsFieldTooltip("Anzahl der hinzuzufuegenden Zeilen");
        tablesI18n.setInsertTableColumnsFieldLabel("Spalten");
        tablesI18n.setInsertTableColumnsFieldTooltip("Anzahl der hinzuzufuegenden Spalten");
        tablesI18n.setInsertTableAddButtonTooltip("Tabelle hinzufuegen");

        tablesI18n.setModifyTableToolbarSwitchTooltip("Tabelle anpassen");
        tablesI18n.setTableTemplatesToolbarSwitchTooltip("Formatvorlagen");

        TablesI18n.TemplatesI18n templatesI18n = tablesI18n.getTemplatesI18n();
        templatesI18n.setCurrentTemplateSelectFieldLabel("Aktuelle Vorlage");
        templatesI18n.setCurrentTemplateNameNotUniqueError("Es gibt bereits eine Vorlage mit diesem Namen!");

        templatesI18n.setCreateNewTemplateButtonTooltip("Neue Vorlage hinzufuegen");
        templatesI18n.setCopyTemplateButtonTooltip("Vorlage kopieren");
        templatesI18n.setDeleteTemplateButtonTooltip("Vorlage loeschen");

        templatesI18n.setDeleteTemplateConfirmTitle("Vorlage loeschen");
        templatesI18n.setDeleteTemplateConfirmText("Moechten Sie die ausgewaehlte Vorlage loeschen? Dieser" +
                                                   " Vorgang kann nicht rueckgaengig gemacht werden");
        templatesI18n.setDeleteTemplateConfirmYesButton("Loeschen");
        templatesI18n.setDeleteTemplateConfirmNoButton("Abbrechen");

        EnhancedRichTextEditorTables tables = EnhancedRichTextEditorTables.enable(rte, tablesI18n);
        tables.setTemplates((ObjectNode) JacksonUtils.readTree(templatesString));
        tables.getStyleTemplatesDialog().addThemeVariants(DialogVariant.LUMO_NO_PADDING);

        rte.asDelta().setValue(deltaString);
        rte.setMaxHeight("500px");
        rte.setValueChangeMode(ValueChangeMode.EAGER);
        addCard("Rich Text Editor with Table Addon - I18n Sample", rte);
    }
}
