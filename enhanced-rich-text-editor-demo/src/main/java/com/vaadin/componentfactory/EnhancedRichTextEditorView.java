package com.vaadin.componentfactory;

import com.vaadin.componentfactory.EnhancedRichTextEditor.ToolbarButton;
import com.vaadin.componentfactory.erte.tables.EnhancedRichTextEditorTables;
import com.vaadin.componentfactory.erte.tables.TablesI18n;
import com.vaadin.componentfactory.toolbar.ToolbarSlot;
import com.vaadin.componentfactory.toolbar.ToolbarSwitch;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dialog.DialogVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.demo.DemoView;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import elemental.json.Json;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * View for {@link EnhancedRichTextEditor} demo.
 */
@RouteAlias("")
@Route("enhanced-rich-text-editor")
@JsModule("./src/sampleEditorExtensionConnector.js")
public class EnhancedRichTextEditorView extends DemoView {

    @Override
    protected void initView() {
//        createDefaultEditor();
//        createEditorWithTabstops();
//        createGetValue();
//        createGetHtmlValue();
//        createEditorWithLimitedToolbar();
//        createEditorWithReadonlySections();
//        createEditorWithPlaceholders();
//        createEditorWithCustomButtons();
        createEditorWithCustomButtonsExtended();
//        createEditorWithCustomShortcutsForStandardButtons();
//        createEditorWithIconReplacementForStandardButtons();
//        createEditorWithNoRules();
//        createEditorWithTableSample();
//        createEditorWithTableI18nSample();
    }

    private void createDefaultEditor() {
        // begin-source-example
        // source-example-heading: Basic Rich Text Editor
        EnhancedRichTextEditor rte = new EnhancedRichTextEditor();
        rte.setMaxHeight("200px");
        // end-source-example
        addCard("Basic Rich Text Editor", rte);
    }

    private void createEditorWithCustomShortcutsForStandardButtons() {
      // begin-source-example
      // source-example-heading: Basic Rich Text Editor with custom shortcuts for standard buttons
      EnhancedRichTextEditor rte = new EnhancedRichTextEditor();
      
      // shift + f9 for align center button
      rte.addStandardToolbarButtonShortcut(ToolbarButton.ALIGN_CENTER, 120, false, true, false);
      // shift + P for superscript
      rte.addStandardToolbarButtonShortcut(ToolbarButton.SUPERSCRIPT, 80, false, true, false);      
      // ctrol + B for header 1
      rte.addStandardToolbarButtonShortcut(ToolbarButton.H1, 66, true, false, false);
      // f9 to load an image
      rte.addStandardToolbarButtonShortcut(ToolbarButton.IMAGE, 120, false, false, false);
      // alt + G for code block
      rte.addStandardToolbarButtonShortcut(ToolbarButton.CODE_BLOCK, 71, false, false, true);
      
      // focus toolbar with shift + J
      rte.addToobarFocusShortcut(74, false, true, false);
      
      // end-source-example
      addCard("Basic Rich Text Editor with custom shortcuts for standard buttons", rte);
    }

    private void createEditorWithIconReplacementForStandardButtons() {
        // begin-source-example
        // source-example-heading: Basic Rich Text Editor with icon replacement for standard buttons
        EnhancedRichTextEditor rte = new EnhancedRichTextEditor();
        
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
        
        // replace redo button icon
        Icon imageIcon = new Icon(VaadinIcon.PICTURE);
        imageIcon.setSize("1.25em");
        rte.replaceStandardToolbarButtonIcon(ToolbarButton.IMAGE, imageIcon);
                
        // end-source-example
        addCard("Basic Rich Text Editor with icon replacement for standard buttons", rte);
      }
    
    
    private void createEditorWithCustomButtons() {
        // begin-source-example
        // source-example-heading: Rich Text Editor With Custom Buttons
        EnhancedRichTextEditor rte = new EnhancedRichTextEditor();

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
        // end-source-example

        addCard("Rich Text Editor With Custom Buttons", rte, button);
    }

    private void createEditorWithCustomButtonsExtended() {
        // begin-source-example
        // source-example-heading: Rich Text Editor With Custom Buttons
        EnhancedRichTextEditor rte = new EnhancedRichTextEditor();

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
            if(slot == ToolbarSlot.GROUP_CUSTOM) {
                continue; // special handling for the custom group
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
            slottedButtons.forEach(button -> button.setVisible(active));
        });
        toolbarSwitch.setTooltipText("A toolbar switch, placed in the '" +
                                     ToolbarSlot.GROUP_CUSTOM.getSlotName() + "' slot. Click to show/hide" +
                                     " additional toolbar components, that are placed between" +
                                     " the standard button groups");
        rte.addToolbarComponents(ToolbarSlot.GROUP_CUSTOM, toolbarSwitch);

        // end-source-example

        Span info = new Span("Click the EYE Button to show/hide additional toolbar components, that are placed between" +
                             " the standard button groups");

        addCard("Rich Text Editor With Custom Buttons (extended)", info, rte);
    }

    private void createEditorWithPlaceholders() {
        // begin-source-example
        // source-example-heading: Rich Text Editor with Placeholders
        EnhancedRichTextEditor rte = new EnhancedRichTextEditor();

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

        rte.setPlacehoderAltAppearance(true);
        rte.setPlaceholderAltAppearancePattern("(?<=\\=).*$");

        rte.setPlaceholders(placeholders);

        rte.addPlaceholderBeforeInsertListener(event -> {
            String texts = "";
            for (Placeholder placeholder : event.getPlaceholders()) {
                texts+=" "+placeholder.getText();
                texts+=" at "+placeholder.getIndex();
            }
            Notification.show(texts + " to be inserted");
            event.insert();
        });

        rte.addPlaceholderInsertedListener(event -> {
            String texts = "";
            for (Placeholder placeholder : event.getPlaceholders()) {
                texts+=" "+placeholder.getText();
                texts+=" at "+placeholder.getIndex();
            }
            Notification.show(texts + " inserted");
        });

        rte.addPlaceholderBeforeRemoveListener(event -> {
            String texts = "";
            for (Placeholder placeholder : event.getPlaceholders()) {
                texts+=" "+placeholder.getText();
            }
            Notification
                    .show(texts + " to be removed");
            if (!texts.contains("Turku"))
                event.remove();
        });

        rte.addPlaceholderSelectedListener(event -> {
            String texts = "";
            for (Placeholder placeholder : event.getPlaceholders()) {
                texts+=" "+placeholder.getText();
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
                texts+=" "+placeholder.getText();
            }
            Notification.show(texts + " removed");
        });

        rte.addPlaceholderAppearanceChangedListener(event -> {
            if (event.isFromClient())
                Notification.show(
                        "Appearence changed to " + event.getAppearanceLabel());
        });
        // end-source-example

        Div valueHolder = new Div();
        rte.addValueChangeListener(event -> {
            Html value = new Html(
                    "<div>" + rte.getHtmlValueString() + "</div>");
            valueHolder.removeAll();
            valueHolder.add(value);
        });

        Button button = new Button("Click me");
        button.addClickListener(event -> {
            Notification.show("Clicked");
            rte.focus();
        });

        rte.setValue(
                "[{\"insert\":\"The company \"},{\"insert\":{\"placeholder\":{\"text\":\"N-1=Vaadin\",\"format\":{\"italic\":true},\"altFormat\":{\"italic\":false,\"bold\":true}}}},{\"insert\":\", located in \"},{\"insert\":{\"placeholder\":{\"text\":\"A-1=Turku, 20540\",\"altFormat\":{\"link\":\"https://goo.gl/maps/EX8RTEMUWeEAdkNN8\"}}}},{\"insert\":\", was founded in \"},{\"insert\":{\"placeholder\":{\"text\":\"D-1=01-01-2000\"}}},{\"insert\":\".\"}]");
        addCard("Rich Text Editor with Placeholders", rte, button, valueHolder);        
    }

    private void createEditorWithTabstops() {
        // begin-source-example
        // source-example-heading: Basic Rich Text Editor with Tab-stops
        EnhancedRichTextEditor rte = new EnhancedRichTextEditor();

        List<TabStop> tabStops = new ArrayList<>();
        tabStops.add(new TabStop(TabStop.Direction.LEFT, 150));
        tabStops.add(new TabStop(TabStop.Direction.RIGHT, 350));
        tabStops.add(new TabStop(TabStop.Direction.MIDDLE, 550));

        rte.setTabStops(tabStops);
        rte.setValue("[{\"attributes\":{\"tab\":\"3\"},\"insert\":\"﻿\"},"
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

        // end-source-example

        addCard("Basic Rich Text Editor with Tab-stops", rte);
    }

    private void createGetValue() {
        // begin-source-example
        // source-example-heading: Save Rich Text Editor value
        TextArea valueBlock = new TextArea();
        EnhancedRichTextEditor rte = new EnhancedRichTextEditor();
        Button saveBtn = new Button("Save value",
                e -> valueBlock.setValue(rte.getValue()));
        Button setBtn = new Button("Set value",
                e -> rte.setValue(valueBlock.getValue()));
        // end-source-example

        addCard("Save Rich Text Editor value", rte, saveBtn, setBtn,
                valueBlock);
    }

    private void createGetHtmlValue() {
        // begin-source-example
        // source-example-heading: Save Rich Text Editor htmlValue
        Div htmlBlock = new Div();
        EnhancedRichTextEditor rte = new EnhancedRichTextEditor();
        Button showHtmlValue = new Button("Show html value", e -> {
            String exsValue = htmlBlock.getElement().getProperty("innerHTML");
            if (exsValue == null || !exsValue.equals(rte.getHtmlValue())) {
                htmlBlock.getElement().setProperty("innerHTML",
                        rte.getHtmlValue());
            }
        });
        // end-source-example

        addCard("Save Rich Text Editor htmlValue", rte, showHtmlValue,
                htmlBlock);
    }

    private void createEditorWithLimitedToolbar() {
        // begin-source-example
        // source-example-heading: Rich Text Editor with limited toolbar
        EnhancedRichTextEditor rte = new EnhancedRichTextEditor();
        Map<EnhancedRichTextEditor.ToolbarButton, Boolean> buttons = new HashMap<>();
        buttons.put(EnhancedRichTextEditor.ToolbarButton.CLEAN, false);
        buttons.put(EnhancedRichTextEditor.ToolbarButton.BLOCKQUOTE, false);
        buttons.put(EnhancedRichTextEditor.ToolbarButton.CODE_BLOCK, false);
        buttons.put(EnhancedRichTextEditor.ToolbarButton.IMAGE, false);
        buttons.put(EnhancedRichTextEditor.ToolbarButton.LINK, false);
        buttons.put(EnhancedRichTextEditor.ToolbarButton.STRIKE, false);
        buttons.put(EnhancedRichTextEditor.ToolbarButton.READONLY, false);
        rte.setToolbarButtonsVisibility(buttons);
        // end-source-example

        addCard("Rich Text Editor with limited toolbar", rte);
    }

    private void createEditorWithReadonlySections() {
        // begin-source-example
        // source-example-heading: Basic Rich Text Editor with readonly sections
        EnhancedRichTextEditor rte = new EnhancedRichTextEditor();
        rte.setValue("[" + "{\"insert\":\"Some text\\n\"},"
                + "{\"insert\":{\"readonly\":\"Some readonly text\\n\"}},"
                + "{\"insert\":\"More text\\n\"},"
                + "{\"insert\":{\"readonly\":\"More readonly text\\n\"}}]");
        // end-source-example

        addCard("Basic Rich Text Editor with readonly sections", rte);
    }
    
    private void createEditorWithNoRules() {
    	// begin-source-example
        // source-example-heading: Rich Text Editor with no rulers
        EnhancedRichTextEditor rte = new EnhancedRichTextEditor();
        rte.setMaxHeight("200px");    
        rte.setNoRulers(true);
        // end-source-example
        addCard("Rich Text Editor with no rulers", rte);
    }

    private void createEditorWithTableSample() {
        String deltaString;
        String templatesString;

        try (
                InputStream deltaStream = getClass().getClassLoader().getResourceAsStream("table-sample-delta.json");
                InputStream templatesStream = getClass().getClassLoader().getResourceAsStream("table-sample-templates.json")
        ){
            Objects.requireNonNull(deltaStream);
            Objects.requireNonNull(templatesStream);

            deltaString = new String(deltaStream.readAllBytes(), StandardCharsets.UTF_8);
            templatesString = new String(templatesStream.readAllBytes(), StandardCharsets.UTF_8);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // begin-source-example
        // source-example-heading: Rich Text Editor with Table Addon
        EnhancedRichTextEditor rte = new EnhancedRichTextEditor();
        EnhancedRichTextEditorTables tables = EnhancedRichTextEditorTables.enable(rte);

        tables.setTemplates(Json.parse(templatesString));

        // end-source-example
        rte.setValue(deltaString);
        rte.setMaxHeight("500px");
        rte.setValueChangeMode(ValueChangeMode.EAGER);

        addCard("Rich Text Editor with Table Addon", rte);
    }

    private void createEditorWithTableI18nSample() {
        String deltaString;
        String templatesString;

        try (
                InputStream deltaStream = getClass().getClassLoader().getResourceAsStream("table-sample-delta.json");
                InputStream templatesStream = getClass().getClassLoader().getResourceAsStream("table-sample-templates.json")
        ){
            Objects.requireNonNull(deltaStream);
            Objects.requireNonNull(templatesStream);

            deltaString = new String(deltaStream.readAllBytes(), StandardCharsets.UTF_8);
            templatesString = new String(templatesStream.readAllBytes(), StandardCharsets.UTF_8);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // begin-source-example
        // source-example-heading: Rich Text Editor with Table Addon - I18n Sample
        EnhancedRichTextEditor rte = new EnhancedRichTextEditor();

        TablesI18n tablesI18n = new TablesI18n();
        tablesI18n.setInsertTableToolbarSwitchTooltip("Neue Tabelle hinzufügen");
        tablesI18n.setInsertTableRowsFieldLabel("Zeilen");
        tablesI18n.setInsertTableRowsFieldTooltip("Anzahl der hinzuzufügenden Zeilen");
        tablesI18n.setInsertTableColumnsFieldLabel("Spalten");
        tablesI18n.setInsertTableColumnsFieldTooltip("Anzahl der hinzuzufügenden Spalten");
        tablesI18n.setInsertTableAddButtonTooltip("Tabelle hinzufügen");

        tablesI18n.setModifyTableToolbarSwitchTooltip("Tablle anpassen");
        tablesI18n.setTableTemplatesToolbarSwitchTooltip("Formatvorlagen");

        TablesI18n.TemplatesI18n templatesI18n = tablesI18n.getTemplatesI18n();
        templatesI18n.setCurrentTemplateSelectFieldLabel("Aktuelle Vorlage");
        templatesI18n.setCurrentTemplateNameNotUniqueError("Es gibt bereits eine Vorlage mit diesem Namen!");

        templatesI18n.setCreateNewTemplateButtonTooltip("Neue Vorlage hinzufügen");
        templatesI18n.setCopyTemplateButtonTooltip("Vorlage kopieren");
        templatesI18n.setDeleteTemplateButtonTooltip("Vorlage löschen");

        templatesI18n.setDeleteTemplateConfirmTitle("Vorlage löschen");
        templatesI18n.setDeleteTemplateConfirmText("Möchten Sie die ausgewählte Vorlage löschen? Dieser" +
                                                   " Vorgang kann nicht rückgängig gemacht werden");
        templatesI18n.setDeleteTemplateConfirmYesButton("Löschen");
        templatesI18n.setDeleteTemplateConfirmNoButton("Abbrechen");

        // ... more i18n settings

        EnhancedRichTextEditorTables tables = EnhancedRichTextEditorTables.enable(rte, tablesI18n);
        tables.setTemplates(Json.parse(templatesString));
        tables.getStyleTemplatesDialog().addThemeVariants(DialogVariant.LUMO_NO_PADDING);

        // end-source-example
        rte.setValue(deltaString);
        rte.setMaxHeight("500px");
        rte.setValueChangeMode(ValueChangeMode.EAGER);
        addCard("Rich Text Editor with Table Addon - I18n Sample", rte);

    }


}

