package com.vaadin.componentfactory;

import java.util.HashMap;
import java.util.Map;

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
        createGetValue();
        createGetHtmlValue();
        createEditorWithLimitedToolbar();
        createEditorWithReadonlySections();
    }

    private void createDefaultEditor() {
        // begin-source-example
        // source-example-heading: Basic Rich Text Editor
        EnhancedRichTextEditor rte = new EnhancedRichTextEditor();
        // end-source-example

        addCard("Basic Rich Text Editor", rte);
    }

    private void createGetValue() {
        // begin-source-example
        // source-example-heading: Save Rich Text Editor value
        TextArea valueBlock = new TextArea();
        EnhancedRichTextEditor rte = new EnhancedRichTextEditor();
        Button saveBtn = new Button("Save value", e -> valueBlock.setValue(rte.getValue()));
        Button setBtn = new Button("Set value", e ->  rte.setValue(valueBlock.getValue()));
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
