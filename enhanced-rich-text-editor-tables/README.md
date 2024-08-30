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

## Data formats
### Table content / Quill delta
The table extension has a unique format to encode its contents into the Quill delta format.

```

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





