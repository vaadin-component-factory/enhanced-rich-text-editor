# Enhanced Rich Text Editor Tables Extension
This addon extends the Enhanced Rich Text Editor with a table feature. Users can create and modify tables
inside the ERTE. Also the addon provides a style template functionality, that allows to predefine, modifiy and/or
apply visual styles for tables.

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
of a table. The differnt parts contain the same potential appearance definitions as the "current row". 

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

``````

## I18n
## File formats
