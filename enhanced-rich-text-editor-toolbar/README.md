# Enhanced Rich Text Editor Toolbar Extension
This addon provides several additional components to add custom buttons to the Enhanced Rich Text Editor Toolbar.
These features have been packed into a separated addon to prevent the inclusion of maybe unneeded dependencies
into the main project.

## Usage

### ToolbarSwitch
To add new features to the toolbar of the ERTE, you may simply add normal Vaadin buttons. To apply some built-in
interactivity, we recommend to use the `ToolbarSwitch` instead. he toolbar switch is a Vaadin button, 
that has an active state, that toggles when the button is clicked. 

```
EnhancedRichTextEditor erte = ...;
ToolbarSwitch switch = new ToolbarSwitch(VaadinIcon.EDIT.create());

erte.addCustomToolbarComponents(switch); // adds the button to the custom toolbar part

switch.addActiveChangedListener(event -> {
    if (event.isActive()) {
        // active the related feature
    }
});
```

The switch also provides built-in support for combined icons. The following constructor shows a "main" icon and
a small / super suffix icon 

```
new ToolbarSwitch(VaadinIcon.TABLE.create(), VaadinIcon.PLUS.create());
```

### Toolbar overlays
The addon also provides some prebuild overlay components, that can be used together with the toolbar switch.

* ToolbarDialog (extends Dialog)
* ToolbarSelectPopup (extends ContextMenu)
* ToolbarPopup (extends the Popup addon from Vaadin Component Factory)

All three of these components are created with a toolbar switch as their owner. Toggling the toolbar switch
will show/hide the overlays and on the other hand, closing these overlays also toggles the switch.

```
ToolbarSwitch switch = ...;

// creates a dialog, that will open centered, when the switch is activated
new ToolbarDialog(switch);

// creates a dialog, that will open beneath the toolbar switch, when opened
new ToolbarDialog(switch, true);

// creates a dialog with a vertical layout as root layout
ToolbarDialog.vertical(switch, someComponents);
```

The toolbar overlay components can be used as their base classes. They simply add the switch interactivity (and
maybe one or two other features).