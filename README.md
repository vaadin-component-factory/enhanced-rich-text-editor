# Enhanced Rich Text Editor for Flow

Enhanced Rich Text Editor for Flow is an extended version of Vaadin Rich Text 
Editor with more functionalities like tab-stops, non-breaking space, rulers, customizing toolbar buttons and
read-only sections.

 [Live Demo ↗](https://incubator.app.fi/enhanced-rich-text-editor-demo/enhanced-rich-text-editor)

# Usage

## Tabstops

Tabstops can be set in UI by clicking on horizontal ruler, on top of the editor. 
There are 3 tabstop possible directions: Left, Right and Middle.
* When Direction set to Left: then left side of text will be aligned to right side of tab stop (>text)
* When Direction set to Right: then right side of text will be aligned to left side of tab stop  (text<)
* When Direction set to Middle: then text will be centered to tab stop  (te|xt)

When you click on ruler left tabstop will appear, 
then if you click on left tabstop it will change to right tabstop, and if you click on right tabstop it will change to middle tabstop. 
It is also possible to set tabstops programmatically by using tabStops property of editor. For example:

```
EnhancedRichTextEditor rte = new EnhancedRichTextEditor();
List<TabStop> tabStops = new ArrayList<>();

tabStops.add(new TabStop(TabStop.Direction.LEFT, 150));
tabStops.add(new TabStop(TabStop.Direction.RIGHT, 350));
tabStops.add(new TabStop(TabStop.Direction.MIDDLE, 550));

rte.setTabStops(tabStops);
```

Position of tab stop is set in pixels.

## Non-breaking space

To add a non-breaking space where the caret is located, press `shift+space`.

## Customizing toolbar buttons

You can use `setToolbarButtonsVisibility` to show/hide the toolbar buttons. For example, the following piece of code hides Image and Link buttons.

```
Map<EnhancedRichTextEditor.ToolbarButton, Boolean> buttons = new HashMap<>();
buttons.put(EnhancedRichTextEditor.ToolbarButton.IMAGE, false);
buttons.put(EnhancedRichTextEditor.ToolbarButton.LINK, false);
rte.setToolbarButtonsVisibility(buttons);
```

You can also add custom components, like `Button`, `ComboBox` or `ToolbarSwitch` to the toolbar, using the
`addToolbarComponents` method. Currently the toolbar supports placement of custom components at its start, end, between
groups and in the special (legacy) "custom" slot at the end of the toolbar. The enumeration `ToolbarSlot` provides a 
list of potential places to add them. 

```
ComboBox<String> presets = new ComboBox<>("", "Preset 1", "Preset 2", "Preset 3");
presets.setValue("Preset 1");
presets.setTooltipText("A (non functional) custom toolbar component, placed in the '" + 
ToolbarSlot.START.getSlotName() + "' slot");
rte.addToolbarComponents(ToolbarSlot.START, presets);

Select<String> colors = new Select<>();
colors.setItems("Red", "Green", "Blue");
colors.setValue("Red");
colors.setTooltipText("A (non functional) custom toolbar component, placed in the '" + 
ToolbarSlot.BEFORE_GROUP_GLYPH_TRANSFORMATION.getSlotName() + "' slot");
rte.addToolbarComponents(ToolbarSlot.BEFORE_GROUP_GLYPH_TRANSFORMATION, colors);
```

## Readonly sections

To make part of text read only, select text and click `lock` icon in toolbar. Now text is not editable. 
To make text editable egain, select it and clicl `lock` button again.

Limitations of readonly functionality:
* Readonly is not working in `code` block
* Readonly is a inline element(like span), so it is still possible to put cursore after the area and add some text
* Readonly area can be deleted, if user put cursor after it and press backspace
* Readonly area can be styled using toolbar buttons
* Selecting multiple lines and making them readonly will create multiple Readonly areas

The following example shows how you can have readonly sections in the middle of your text. 

```
rte.setValue("[" +
        "{\"insert\":\"Some text\\n\"}," +
        "{\"insert\":{\"readonly\":\"Some readonly text\\n\"}}," +
        "{\"insert\":\"More text\\n\"}," +
        "{\"insert\":{\"readonly\":\"More readonly text\\n\"}}]");
``` 

## Tables
To enable the usage of tables in the editor, you need to add the 
[Table Extension ↗](https://vaadin.com/directory/component/enhanced-rich-text-editor-tables-extension), which is a 
separate addon. Follow the instructions of the extension on how to use the table functionality.

# Running the demo
* Run from the command line `mvn install -DskipTests`
* Run from the command line `mvn -pl enhanced-rich-text-editor-demo -Pwar jetty:run`
* Browse http://127.0.0.1:8080/enhanced-rich-text-editor

