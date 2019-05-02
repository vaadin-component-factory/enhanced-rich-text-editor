package com.vaadin.flow.component.incubatorrichtexteditor;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.demo.DemoView;
import com.vaadin.flow.router.Route;

/**
 * View for {@link RichTextEditor} demo.
 */
@Route("incubator-rich-text-editor")
public class RichTextEditorView extends DemoView {

    @Override
    protected void initView() {
        createDefaultEditor();
        createGetValue();
        createGetHtmlValue();
        createEditorWithLimitedToolbar();
        createEditorWithReadonlySections();
    }

    private void createDefaultEditor() {
        // begin-source-example
        // source-example-heading: Basic Rich Text Editor
        RichTextEditor rte = new RichTextEditor();
        // end-source-example

        addCard("Basic Rich Text Editor", rte);
    }

    private void createGetValue() {
        // begin-source-example
        // source-example-heading: Save Rich Text Editor value
        TextArea valueBlock = new TextArea();
        RichTextEditor rte = new RichTextEditor();
        Button saveBtn = new Button("Save value", e -> valueBlock.setValue(rte.getValue()));
        Button setBtn = new Button("Set value", e ->  rte.setValue(valueBlock.getValue()));
        // end-source-example

        addCard("Save Rich Text Editor value", rte, saveBtn, setBtn, valueBlock);
    }

    private void createGetHtmlValue() {
        // begin-source-example
        // source-example-heading: Save Rich Text Editor htmlValue
        Div htmlBlock = new Div();
        RichTextEditor rte = new RichTextEditor();
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
        RichTextEditor rte = new RichTextEditor();
        Map<RichTextEditor.ToolbarButton, Boolean> buttons = new HashMap<>();
        buttons.put(RichTextEditor.ToolbarButton.CLEAN, false);
        buttons.put(RichTextEditor.ToolbarButton.BLOCKQUOTE, false);
        buttons.put(RichTextEditor.ToolbarButton.CODE_BLOCK, false);
        buttons.put(RichTextEditor.ToolbarButton.IMAGE, false);
        buttons.put(RichTextEditor.ToolbarButton.LINK, false);
        buttons.put(RichTextEditor.ToolbarButton.STRIKE, false);
        rte.setToolbarButtonsVisibility(buttons);
        // end-source-example

        addCard("Rich Text Editor with limited toolbar", rte);
    }

    private void createEditorWithReadonlySections() {
        // begin-source-example
        // source-example-heading: Basic Rich Text Editor with readonly sections
        RichTextEditor rte = new RichTextEditor();
        rte.setValue("[" +
                "{\"insert\":\"Some text\\n\"}," +
                "{\"insert\":{\"readonly\":\"Some readonly text\\n\"}}," +
                "{\"insert\":\"More text\\n\"}," +
                "{\"insert\":{\"readonly\":\"More readonly text\\n\"}}]");
        // end-source-example

        addCard("Basic Rich Text Editor with readonly sections", rte);
    }
}
