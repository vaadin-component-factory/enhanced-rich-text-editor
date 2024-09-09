# Enhanced Rich Text Editor Tables Extension
This addon extends the Enhanced Rich Text Editor with a table feature. Users can create and modify tables
inside the ERTE. Also the addon provides a style template functionality, that allows to predefine, modifiy and/or
apply visual styles for tables.

## Source
The addon code is forked from https://github.com/dclement8/quill1-table and has been partially modified afterwards. 

Any licensing different to the Apache License 2.0 applies only to code changes made on the original or new code, that
does not exist in the forked repository (as of August 1st, 2024). 

## Addon usage
The addon extends an existing ERTE instance by registering Quill (the internally used library for the rich text editor)
modules and adding custom toolbar components to allow the user to add or modify tables.

Due to the extension nature of this addon it is not necessary to create a subclass of the ERTE and activating
the table features is just one line of code. 

```
EnhancedRichTextEditor rte = new EnhancedRichTextEditor();
EnhancedRichTextEditorTables tables = EnhancedRichTextEditorTables.enable(rte);

add(rte);
```

The toolbar of the ERTE will now show three additional buttons.
* Add new table
* Modify selected table
* Modify the style template for the selected table

### Add new table
The "Add new table" button opens a small popup. The user can set the amount of rows and columns of the new table
and insert it using the "+" button.

### Modify selected table
The "Modify table" button shows a set of action, that can be applied onto the current selected table, row, column or
cell(s).

Please note, that merge cells is a special operation, that needs the user to use "cell selection".

### Modify Style Templates

The button "Style Templates" opens a new dialog, that allows the user to create, modify or delete style templates
for the current table. See the chapter "Style templates" for details.

### Cell selection
By default a click into a table will set the text cursor to the respective cell. The user can now insert text or 
navigate to other cells using the keyboard. 

Some actions (at the moment only "merge cells") need the user to select multiple cells at once - this is called 
"cell selection" and is done by holding the Ctrl key while selecting one or multiple cells. The respective
cells will be colorized. 

## Style templates
The addon provides a feature to style tables, called "Style Templates". A style template is a named set of rules,
that is converted into css and applied to the given table. Multiple tables can share the same template and any
changes to that template will affect all assigned tables.

The user can by default create, modify or delete templates for a specific ERTE instance. The resulting templates
can be imported/exported as a json file to allow storing them in your backend. 

### Initializing the templates
You can provide a predefined set of templates by providing a json to the table extension. That json could
be created earlier using the template dialog or crafted manually using a text editor.

If you have no json file, the tables extension automatically creates an empty one for you, so there is no
need to set an empty string initally.


```
String styleTemplatesJson = // ... read from your backend

EnhancedRichTextEditorTables tables = // ...
Json parsedJson = TemplateParser.parseJson(styleTemplatesJson);
tables.setTemplates(parsedJson);
```

The tables extension uses the same Json objects, that other Vaadin API uses. Therefore you can create the
underlying json structur, using `Json.parse()` and other methods. However, it is recommended to use
the `TemplateParser` as there might be changes regarding the parsing process in future.

The template for the currently selected table can be read/set using the style templates dialog.
See also the "Events" chapter for more information regarding template changes.
```
String activeTemplate = tables.getStyleTemplatesDialog().getActiveTemplate().orElse(null)
```

### Create CSS
While using the ERTE, you do not need to create the templates CSS yourself. But there might be use cases, where
you want to apply them manually. 

```
String styleTemplatesJson = // ... read from your backend

String css = TemplateParser.convertToCss(styleTemplatesJson); // can also take the Json object

// ... set the css to a manually create styles element or similar
```

### Remove unneeded json content
It can happen, that empty json nodes exist after a json template has been edited in different ways. This is
no problem and also the resulting css file would simply contain selectors without any declarations inside. 

If you want to remove empty children from the Json, you can use the method 
`TemplateParser.removeEmptyChildren(JsonObject container)`. It will clean the given json object directly. 

### Template dialog
The template dialog provides multiple sections with the following functions.

#### Active template section
The first part of the dialog allows the user to (un-) assign a style template to the selected table using a combobox.
The combobox contains all templates, that the user created or that have been set programmatically using a json.

Beside the combobox a set of buttons allow to create, copy or delete a template. A textfield allows to rename
an existing template.

The "copy" button will create a duplicate of the active template, adding a "Copy (x)" to the name.

#### Table section
The table section allows modifying the appearance of the current table. The user can set the text- and background color
plus the table border and global cell borders. The table border is the outline of the table, while the cell borders
are the "inner" borders of the table.

#### Current row / column
The current row / column sections allow modifying the appearence of the row and column of the cell, that contains
the text cursor. You can set text- and background color plus the height / width of the row / column.

To keep things simple, the height / width are currently just numeric values, that are interpreted as the css unit "rem". 

### Special rows
The special rows section allows the user to set stylings for even / odd rows and the header and footer row
of a table. The differnt parts contain the same potential appearance definition as the "current row". 

#### Rules of order
Please note, that styles for rows can mix, when using for instance odd plus first row settings or apply styles for
the first row using the "current row" and the "header row" styles. The current row settings always override the
special row settings. This means, having a "header row" and "current row == 1" setting will let the "current row"
override matching definitions.

The order of prioritization is as following (with ascending importance):
* even / odd rows
* header / footer
* specific rows (set by current row)

## Events
There are several events, that are fired by the ERTE regarding tables. All of the following events
can be obtained by registering the respective listener on the table extension instance. Please check
the javadocs of the respective event for additional details.

* TableSelectedEvent - a table has been (de-) selected in the ERTE
* TableCellChangedEvent - the selected table cell indices have changed
* TemplatesChangedEvent - the style templates json has changed (e.g. a new template has been added or styles have been manipulated)
* TemplateSelectedEvent - the active template for the current table has changed (e.g. the user used the dialog combobox)

## Modifying toolbar components
Depending on your use case, you may not want the user to use some features or allow them to modify each part
of a style template. 

Most components can be accessed and modified, using normal Vaadin logic like disable or hide the respective 
components.

```
EnhancedRichTextEditorTables tables = ...;

tables.getAddTableToolbarButton().setVisible(false) // hides the "Add table" toolbar button

List<MenuItem> modifyTableItems = tables.getModifyTableSelectPopup().getMenuItems();
// ... change the available options to modify the table
    

// change the style templates dialog
tables.getStyleTemplatesDialog().addThemeVariants(DialogVariant.LUMO_NO_PADDING);

TemplateDialog templateDialog = tables.getStyleTemplatesDialog();
templateDialog.addThemeVariants(DialogVariant.LUMO_NO_PADDING); // change the dialog theme
templateDialog.getTemplateButtonsContainer().setVisible(false); // hide the buttons to create, copy or delete the template
templateDialog.getTemplateNameField().setVisible(false); // hide the field to change the active template's name 
```

## I18n
The table extension supports built-in i18n support to change any label or tooltip to another language.
Due to the nature of the extensions (setting things inside the ERTE), the i18n configuration has to be provided, when
the table extensions is applied to the ERTE. This may change in future, if required. 

```
        EnhancedRichTextEditor rte = ...;

        TablesI18n tablesI18n = new TablesI18n();
        tablesI18n.setInsertTableToolbarSwitchTooltip("Neue Tabelle hinzufügen");
        tablesI18n.setInsertTableRowsFieldPlaceholder("Zeilen");
        // ...

        tablesI18n.setModifyTableToolbarSwitchTooltip("Tablle anpassen");
        tablesI18n.setTableTemplatesToolbarSwitchTooltip("Formatvorlagen");
        // ...

        // i18n for the style templates dialog
        TablesI18n.TemplatesI18n templatesI18n = tablesI18n.getTemplatesI18n();
        templatesI18n.setCurrentTemplateSelectFieldLabel("Aktuelle Vorlage");
        templatesI18n.setCurrentTemplateNameNotUniqueError("Es gibt bereits eine Vorlage mit diesem Namen!");
        // ...

        // ... more i18n settings

        EnhancedRichTextEditorTables tables = EnhancedRichTextEditorTables.enable(rte, tablesI18n);
```

## UX helper
Depending on the set styles, a table might be more or less invisible for the naked eye. To provide users some visual
feedback regarding which table / cell they are currently hovering or working on, you may set hover and focus colors
for the current table and the current cell. 

These colors are then applied as a special border color (table) or background color (cell). Note: the hover/focused
border style for the table is currently hard coded. It may change in future.

```
// by default applies the Lumo blue
tables.setTableHoverColor("var(--lumo-primary-color-50pct)");
tables.setTableCellHoverColor("var(--lumo-primary-color-10pct)");

// by default applies the Lumo warning yellow-orange
tables.setTableFocusColor("var(--lumo-warning-color)");
tables.setTableCellFocusColor("var(--lumo-warning-color-10pct)");
```

Additionally, when focused, the respective table cell gets a class name `focused-cell` applied. Using this and 
normal css, you can create your own styles to provide a similar feature. Please note, that the table element is
placed inside the ERTE's shadow dom. You have to inject your custom styles into it to make them work.

## Data formats
### Table content / Quill delta
The table extension has a unique format to encode its contents into the Quill delta format. It has been taken
from the forked base project and modified minimal to allow containing the style template name

The format is built the following way: 

Each cell consists of one attribute entry, starting with a static `"0":"T","1":"A","2":"B","3":"L","4":"E","td":`.
This static part is followed by a pipe `|` separated list of the following cell details:
* table id
* row id
* cell id
* reference cell id (only used for merged cells, see below) 
* col span (only used for merged cells, see below)
* row span (only used for merged cells, see below)
* style template id (only used for styled tables, only applied on the very first cell)

A "normal" cell without any merges or styles looks like this. The addon itself will always generate randomized
IDs for the tables, rows and cells. 

```
{"attributes":{"0":"T","1":"A","2":"B","3":"L","4":"E","td":"otk9n3dsmkb|h5bdd8slh2|xhnt6hldo9||||"},"insert":"\n"}
```

The following sample creates an empty, unstyled 3x3 table. 
```json
[
    {"attributes":{"0":"T","1":"A","2":"B","3":"L","4":"E","td":"otk9n3dsmkb|h5bdd8slh2|xhnt6hldo9||||"},"insert":"\n"},
    {"attributes":{"0":"T","1":"A","2":"B","3":"L","4":"E","td":"otk9n3dsmkb|h5bdd8slh2|zw6yx1sjna||||"},"insert":"\n"},
    {"attributes":{"0":"T","1":"A","2":"B","3":"L","4":"E","td":"otk9n3dsmkb|h5bdd8slh2|g3vtidnawfv||||"},"insert":"\n"},
    {"attributes":{"0":"T","1":"A","2":"B","3":"L","4":"E","td":"otk9n3dsmkb|1qhd8f1ab54|89fbepdbvon||||"},"insert":"\n"},
    {"attributes":{"0":"T","1":"A","2":"B","3":"L","4":"E","td":"otk9n3dsmkb|1qhd8f1ab54|kqjodlt3c4a||||"},"insert":"\n"},
    {"attributes":{"0":"T","1":"A","2":"B","3":"L","4":"E","td":"otk9n3dsmkb|1qhd8f1ab54|rwztbe0qvff||||"},"insert":"\n"},
    {"attributes":{"0":"T","1":"A","2":"B","3":"L","4":"E","td":"otk9n3dsmkb|tved8qig6k|u9vy2nlhnh||||"},"insert":"\n"},
    {"attributes":{"0":"T","1":"A","2":"B","3":"L","4":"E","td":"otk9n3dsmkb|tved8qig6k|rdz2kt2ndf||||"},"insert":"\n"},
    {"attributes":{"0":"T","1":"A","2":"B","3":"L","4":"E","td":"otk9n3dsmkb|tved8qig6k|c7tv5wno5jo||||"},"insert":"\n"},
    {"insert":"\n"}
]
```

To merge cells, the delta format expects the most top-left cell to be the "root" of the merge, containing the 
relevant info, while all other merged cells contain the "root" cell id as a reference. When cells are merged,
colspan and rowspan have to be set. If for instance only two cells in a row are merged, the colspan will be set to
"2" and the rowspan must be "1".


A table having the first two cells of the first row merged produces a delta like the following. The first row represents the
"root" cell, containing the col- and rowspan. The second two represents a merged cell, that references the "root" id.
Please note, that, if created manually, you need to assure, that the resulting col- and rowspans are valid for 
the generated html table, otherwise it will most likely lead to a broken table.

```json
{"attributes":{"0":"T","1":"A","2":"B","3":"L","4":"E","td":"07v201kj4523|7kt0r2e1dao|oxrbl0mohn||2|1|"},"insert":"\n"},
{"attributes":{"0":"T","1":"A","2":"B","3":"L","4":"E","td":"07v201kj4523|7kt0r2e1dao|6yu9swho64e|oxrbl0mohn|||"},"insert":"\n"},
```

To initialize a table with a style template the template key needs to be set to the delta (not the display name,
that is shown to the user). See Style Templates Json for details regarding the json structure for style templates.

Also please note, that only the very first cell of the table in the delta contains the template key. 

The following sample applies the template with the key "template1" to this table:

```json
{"attributes":{"0":"T","1":"A","2":"B","3":"L","4":"E","td":"07v201kj4523|7kt0r2e1dao|oxrbl0mohn||||template1"},"insert":"\n"},
```

### Style Templates Json
The extension handles style templates as json and uses that to generate the resulting css. 

The root is a json object, containing named keys "template1" to "templateN" ("template key") by default. 
These keys need to be valid css class names. They will be applied to the html table class attribute and used as 
css selectors.

Each "template key" refers to a json object ("template object").

A "template object" supports the following named keys:
* name
* table
* rows
* cols
* cells

"name" contains the name, that will be shown inside the styles template dialog for the user. It is a simple string
and can contain any characters.

_The other keys point to json objects, that themselves contain "declarations" somewhere inside their hierarchy 
("definition set"). See below, what declarations exist and what rules apply to them._

"table" points to a definition set, that supports the following declaration
* color
* bgColor
* width
* height
* border
* borderCells

"borderCells" is a special declaration, that applies the same rules as "border". 

"rows", "cols" and "cells" point to json arrays to allow multiple declarations for different rows, cols, etc. 
Each array item is a json object. 

For "rows" and "cols" each json object contains a key "index" and a declaration set. Additionaly they may contain
a "last" key, pointing to a boolean, to indicate, that the row/col index is to be counted from the end. 

The index has to be a valid css index, usable in the ":nth-of-type" or ":last-of-type" pseudoclass. These indexes
are also used to differ between "normal" / specific rows, like the 2nd row or 3rd column and special rows like
the header or odd rows. 

"Normal" rows have a numeric index (but written as a string in json). Special rows use a different approach to
make it distinguishable for the parser to see, if the related row is "normal" or special. 

The following special indexes are used:
* 2n - even rows
* 2n+1 - odd rows
* 0n+1 - header / footer (footer has the "last" flag set to true)

Rows and Cols support the following declarations:
* color
* bgColor
* border

Additional, rows support the "height" declaration. Columns support the "width" declaration.

"Cells" do not have an index, but "x"/"y" coordinates instead. These are normal numeric values, written as a json
string. 

Cells support the following declarations:
* color
* bgColor
* border

_Please note: while the json file and template parser support cells, the template dialog currently does not yet.
Therefore it is recommended to not use cell stylings for modifiable style templates. Alternatively feel free
to fork the addon and add cell support to the dialog._

#### Declarations
Declarations are the "leafs" of the json, representing the actual style declarations. While each "owner" may support 
different declarations, the allowed content of each declaration are the same.
* color - String value. Allows any valid css color definiton.
* bgColor - String value. Represents the css `background-color`. Allows any valid css color definiton.
* width - Numeric value. Will be interpreted as the css unit `rem` at the moment. Must be greater than zero. 
* height - Numeric value. Will be interpreted as the css unit `rem` at the moment. Must be greater than zero. 
* border - String value. Expects a valid css `border` declaration, e.g. "1px solid red";

Each "owner" contains a key "declarations", that point to a single json object ("declaration json object"), supporting
at least one of the above keys.

#### Sample
Please see the file sample.json. It does not contain all possible combinations of declarations, but should give
a good impression, how the structure works and be a potential foundation for your own files.





