# Enhanced Rich Text Editor for Flow

Enhanced Rich Text Editor for Flow is an extended version of Vaadin Rich Text 
Editor with more functionalities like tab-stops, non-breaking space, rulers, customizing toolbar buttons and
read-only sections.

# Usage
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


## Running the demo
* Run from the command line `mvn install -DskipTests`
* Run from the command line `mvn -pl enhanced-rich-text-editor-demo -Pwar jetty:run`
* Browse http://127.0.0.1:8080/enhanced-rich-text-editor

