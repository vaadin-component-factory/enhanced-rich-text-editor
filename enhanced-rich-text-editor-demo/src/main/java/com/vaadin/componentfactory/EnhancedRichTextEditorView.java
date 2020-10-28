package com.vaadin.componentfactory;

import java.util.*;

import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.demo.DemoView;
import com.vaadin.flow.router.Route;

/**
 * View for {@link EnhancedRichTextEditor} demo.
 */
@Route("enhanced-rich-text-editor")
public class EnhancedRichTextEditorView extends DemoView {

    @Override
    protected void initView() {
        createDefaultEditor();
        createEditorWithTabstops();
        createGetValue();
        createGetHtmlValue();
        createEditorWithLimitedToolbar();
        createEditorWithReadonlySections();
        createEditorWithPlaceholders();
    }

    private void createDefaultEditor() {
        // begin-source-example
        // source-example-heading: Basic Rich Text Editor
        EnhancedRichTextEditor rte = new EnhancedRichTextEditor();
        // end-source-example

        addCard("Basic Rich Text Editor", rte);
    }

    private void createEditorWithPlaceholders() {
        // begin-source-example
        // source-example-heading: Basic Rich Text Editor
        EnhancedRichTextEditor rte = new EnhancedRichTextEditor();

        List<Placeholder> placeholders = new ArrayList<>();
        Placeholder placeholder1 = new Placeholder();
        placeholder1.setText("N-1=Vaadin");
        placeholder1.getFormat().put("italic", true);
        placeholder1.getAltFormat().put("italic", false);
        placeholder1.getAltFormat().put("bold", true);

        Placeholder placeholder2 = new Placeholder();
        placeholder2.setText("A-1=Turku, 20540");
        placeholder2.getAltFormat().put("link", "https://goo.gl/maps/EX8RTEMUWeEAdkNN8");

        Placeholder placeholder3 = new Placeholder();
        placeholder3.setText("D-1=01-01-2000");

        placeholders.add(placeholder1);
        placeholders.add(placeholder2);
        placeholders.add(placeholder3);

        rte.setPlacehoderAltAppearence(true);
        rte.setPlaceholderAltAppearencePattern("(?<=\\\\=).*$");

        rte.setPlaceholders(placeholders);
        
        
        // end-source-example

        Div valueHolder = new Div();
        rte.addValueChangeListener(event -> {
            Html value = new Html("<div>"+rte.getHtmlValueString()+"</div>");
        	valueHolder.removeAll();
        	valueHolder.add(value);
        });
        
        rte.setValue("[{\"insert\":\"The company \"},{\"insert\":{\"placeholder\":{\"text\":\"N-1=Vaadin\",\"format\":{\"italic\":true},\"altFormat\":{\"italic\":false,\"bold\":true}}}},{\"insert\":\", located in \"},{\"insert\":{\"placeholder\":{\"text\":\"A-1=Turku, 20540\",\"altFormat\":{\"link\":\"https://goo.gl/maps/EX8RTEMUWeEAdkNN8\"}}}},{\"insert\":\", was founded in \"},{\"insert\":{\"placeholder\":{\"text\":\"D-1=01-01-2000\"}}},{\"insert\":\".\"}]" );
        addCard("Rich Text Editor with Placeholders",rte,valueHolder);
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
        rte.setValue("[{\"attributes\":{\"tab\":\"3\"},\"insert\":\"﻿\"}," +
                "{\"attributes\":{\"line-part\":true},\"insert\":\"﻿\"}," +
                "{\"attributes\":{\"underline\":true,\"line-part\":true},\"insert\":\"3rd tab-stop\"}," +
                "{\"attributes\":{\"tabs-cont\":\"TABS-CONT\"},\"insert\":\"\\n\"},{\"insert\":\"\\nThis line is just a normal text. Tab-stops are not affecting it.\\n\\n\"}," +
                "{\"attributes\":{\"tab\":\"1\"},\"insert\":\"﻿\"},{\"attributes\":{\"bold\":true,\"line-part\":true},\"insert\":\"Product\"}," +
                "{\"attributes\":{\"tab\":\"1\"},\"insert\":\"﻿\"},{\"attributes\":{\"bold\":true,\"line-part\":true},\"insert\":\"Price\"}," +
                "{\"attributes\":{\"tab\":\"1\"},\"insert\":\"﻿\"},{\"attributes\":{\"bold\":true,\"line-part\":true},\"insert\":\"Quantity\"}," +
                "{\"attributes\":{\"tabs-cont\":\"TABS-CONT\"},\"insert\":\"\\n\"},{\"attributes\":{\"tab\":\"1\"},\"insert\":\"﻿\"}," +
                "{\"attributes\":{\"line-part\":true},\"insert\":\"Apples\"},{\"attributes\":{\"tab\":\"1\"},\"insert\":\"﻿\"}," +
                "{\"attributes\":{\"line-part\":true},\"insert\":\"2.00\"},{\"attributes\":{\"tab\":\"1\"},\"insert\":\"﻿\"}," +
                "{\"attributes\":{\"line-part\":true},\"insert\":\"5\"},{\"attributes\":{\"tabs-cont\":\"TABS-CONT\"},\"insert\":\"\\n\"}," +
                "{\"attributes\":{\"tab\":\"1\"},\"insert\":\"﻿\"},{\"attributes\":{\"line-part\":true},\"insert\":\"Salmon\"}," +
                "{\"attributes\":{\"tab\":\"1\"},\"insert\":\"﻿\"},{\"attributes\":{\"line-part\":true},\"insert\":\"25.00\"},{\"attributes\":{\"tab\":\"1\"},\"insert\":\"﻿\"}," +
                "{\"attributes\":{\"line-part\":true},\"insert\":\"2\"},{\"attributes\":{\"tabs-cont\":\"TABS-CONT\"},\"insert\":\"\\n\"}," +
                "{\"attributes\":{\"tab\":\"1\"},\"insert\":\"﻿\"},{\"attributes\":{\"tab\":\"true\"},\"insert\":\"﻿\"},{\"insert\":\"\\n\"}," +
                "{\"attributes\":{\"tab\":\"true\"},\"insert\":\"﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿\"},{\"insert\":\"\\n\"},{\"attributes\":{\"tab\":\"true\"},\"insert\":\"﻿\"}," +
                "{\"attributes\":{\"bold\":true,\"tab\":\"1\"},\"insert\":\"﻿\"},{\"insert\":\"\\n\"},{\"attributes\":{\"tab\":\"1\"},\"insert\":\"﻿\"}," +
                "{\"attributes\":{\"tab\":\"true\"},\"insert\":\"﻿\"},{\"attributes\":{\"tab\":\"1\"},\"insert\":\"﻿\"},{\"insert\":\"\\n\"}]");

        // end-source-example

        addCard("Basic Rich Text Editor with Tab-stops", rte);
    }

    private void createGetValue() {
        // begin-source-example
        // source-example-heading: Save Rich Text Editor value
        TextArea valueBlock = new TextArea();
        EnhancedRichTextEditor rte = new EnhancedRichTextEditor();
        Button saveBtn = new Button("Save value", e -> valueBlock.setValue(rte.getValue()));
        Button setBtn = new Button("Set value", e -> rte.setValue(valueBlock.getValue()));
        // end-source-example

        addCard("Save Rich Text Editor value", rte, saveBtn, setBtn, valueBlock);
    }

    private void createGetHtmlValue() {
        // begin-source-example
        // source-example-heading: Save Rich Text Editor htmlValue
        Div htmlBlock = new Div();
        EnhancedRichTextEditor rte = new EnhancedRichTextEditor();
        Button showHtmlValue = new Button("Show html value", e -> {
            String exsValue = htmlBlock.getElement().getProperty("innerHTML");
            if (exsValue == null || !exsValue.equals(rte.getHtmlValue())) {
                htmlBlock.getElement().setProperty("innerHTML", rte.getHtmlValue());
            }
        });
        // end-source-example

        addCard("Save Rich Text Editor htmlValue", rte, showHtmlValue, htmlBlock);
    }

    private void createEditorWithLimitedToolbar () {
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
        rte.setValue("[" +
                "{\"insert\":\"Some text\\n\"}," +
                "{\"insert\":{\"readonly\":\"Some readonly text\\n\"}}," +
                "{\"insert\":\"More text\\n\"}," +
                "{\"insert\":{\"readonly\":\"More readonly text\\n\"}}]");
        // end-source-example

        addCard("Basic Rich Text Editor with readonly sections", rte);
    }
}
