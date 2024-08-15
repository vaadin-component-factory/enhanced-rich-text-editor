package com.vaadin.componentfactory.erte.tables;

import com.vaadin.componentfactory.EnhancedRichTextEditor;
import com.vaadin.componentfactory.erte.tables.templates.*;
import com.vaadin.componentfactory.toolbar.ToolbarPopup;
import com.vaadin.componentfactory.toolbar.ToolbarSelectPopup;
import com.vaadin.componentfactory.toolbar.ToolbarSwitch;
import com.vaadin.flow.component.*;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dependency.NpmPackage;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.function.ValueProvider;
import com.vaadin.flow.shared.Registration;
import elemental.json.JsonObject;

import javax.annotation.Nullable;
import java.util.Objects;

@NpmPackage(value = "quill-delta", version = "5.1.0")
@JsModule("./src/erte-table/connector.js")
@CssImport(value = "./src/erte-table/css/erte-shadow.css", themeFor = "vcf-enhanced-rich-text-editor")
@CssImport(value = "./src/erte-table/css/toolbar.css")
public class EnhancedRichTextEditorTables {

    private static final String SCRIPTS_TABLE = "window.Vaadin.Flow.vcfEnhancedRichTextEditor.extensions.tables.";

    private final EnhancedRichTextEditor rte;
    private final TablesI18n i18n;
    private TemplateDialog templatesDialog;

    public EnhancedRichTextEditorTables(EnhancedRichTextEditor rte) {
        this(rte, new TablesI18n());
    }

    public EnhancedRichTextEditorTables(EnhancedRichTextEditor rte, TablesI18n i18n) {
        this.rte = Objects.requireNonNull(rte);
        this.i18n = Objects.requireNonNull(i18n);

        Element element = rte.getElement();
        element.addEventListener("table-selected", event -> {
                    JsonObject eventData = event.getEventData();
                    fireEvent(new TableSelectedEvent(
                            this,
                            true,
                            eventData.getBoolean("event.detail.selected"),
                            eventData.getBoolean("event.detail.cellSelectionActive"),
                            eventData.hasKey("event.detail.template") ? eventData.getString("event.detail.template") : null
                    ));
                })
                .addEventData("event.detail.selected")
                .addEventData("event.detail.cellSelectionActive")
                .addEventData("event.detail.template");

        element.addEventListener("table-cell-changed", event -> {
                    JsonObject eventData = event.getEventData();
                    fireEvent(new TableCellChangedEvent(
                            this,
                            true,
                            toInteger(eventData, "event.detail.rowIndex"),
                            toInteger(eventData, "event.detail.colIndex"),
                            toInteger(eventData, "event.detail.oldRowIndex"),
                            toInteger(eventData, "event.detail.oldColIndex")
                    ));
                })
                .addEventData("event.detail.rowIndex")
                .addEventData("event.detail.oldRowIndex")
                .addEventData("event.detail.colIndex")
                .addEventData("event.detail.oldColIndex");
    }

    private Integer toInteger(JsonObject object, String key) {
        return object.hasKey(key) ? Double.valueOf(object.getNumber(key)).intValue() : null;
    }

    public static EnhancedRichTextEditorTables enable(EnhancedRichTextEditor rte) {
        return enable(rte, new TablesI18n());
    }

    public static EnhancedRichTextEditorTables enable(EnhancedRichTextEditor rte, TablesI18n i18n) {
        EnhancedRichTextEditorTables tables = new EnhancedRichTextEditorTables(rte, i18n);
        tables.initToolbarTable();

        return tables;
    }

    public void initToolbarTable() {
        // insert new table
        IntegerField rows = createTableInsertNumberField(
                getI18nOrDefault(TablesI18n::getInsertTableRowsFieldPlaceholder, "Rows"),
                getI18nOrDefault(TablesI18n::getInsertTableRowsFieldTooltip, "Amount of rows for the new table")
        );

        IntegerField cols = createTableInsertNumberField(
                getI18nOrDefault(TablesI18n::getInsertTableColumnsFieldPlaceholder, "Columns"),
                getI18nOrDefault(TablesI18n::getInsertTableColumnsFieldTooltip, "Amount of columns for the new table")

        );

        Button add = new Button(VaadinIcon.PLUS.create(), event -> insertTableAtCurrentPosition(rows.getValue(), cols.getValue()));
        add.setTooltipText(getI18nOrDefault(TablesI18n::getInsertTableAddButtonTooltip, "Add table"));

        ToolbarSwitch insertButton = new ToolbarSwitch(VaadinIcon.TABLE, VaadinIcon.PLUS);
        insertButton.setTooltipText(getI18nOrDefault(TablesI18n::getInsertTableToolbarSwitchTooltip, "Add new table"));

        ToolbarPopup insertPopup = ToolbarPopup.horizontal(insertButton, rows, new Span("x"), cols, add);
        insertPopup.setFocusOnOpenTarget(rows);
        add.addClickListener(event -> insertPopup.setOpened(false));

        ToolbarSwitch settingsButton = new ToolbarSwitch(VaadinIcon.TABLE, VaadinIcon.TOOLS);
        settingsButton.setTooltipText(getI18nOrDefault(TablesI18n::getModifyTableToolbarSwitchTooltip, "Modify Table"));
        settingsButton.setEnabled(false);

        ToolbarSelectPopup selectPopup = new ToolbarSelectPopup(settingsButton);
        selectPopup.addItem(
                getI18nOrDefault(TablesI18n::getModifyTableAppendRowAboveItemLabel, "Append row above"),
                event -> executeTableRowAction("append-row-above")
        );

        selectPopup.addItem(
                getI18nOrDefault(TablesI18n::getModifyTableAppendRowBelowItemLabel, "Append row below"),
                event -> executeTableRowAction("append-row-below")
        );
        selectPopup.addItem(
                getI18nOrDefault(TablesI18n::getModifyTableRemoveRowItemLabel, "Remove row"),
                event -> executeTableRowAction("remove-row")
        );
        selectPopup.add(new Hr());
        selectPopup.addItem(
                getI18nOrDefault(TablesI18n::getModifyTableAppendColumnBeforeItemLabel, "Append column before"),
                event -> executeTableColumnAction("append-col-before")
        );
        selectPopup.addItem(
                getI18nOrDefault(TablesI18n::getModifyTableAppendColumnAfterItemLabel, "Append column after"),
                event -> executeTableColumnAction("append-col-after")
        );
        selectPopup.addItem(
                getI18nOrDefault(TablesI18n::getModifyTableRemoveColumnItemLabel, "Remove column"),
                event -> executeTableColumnAction("remove-col")
        );

        selectPopup.add(new Hr());
        MenuItem mergeCells = selectPopup.addItem(
                getI18nOrDefault(TablesI18n::getModifyTableMergeCellsItemLabel, "Merge selected cells"),
                event -> executeTableAction( "merge-selection")
        );

        selectPopup.addItem(
                getI18nOrDefault(TablesI18n::getModifyTableSplitCellItemLabel, "Split cell"),
                event -> executeTableAction("split-cell")
        );

        selectPopup.add(new Hr());
        selectPopup.addItem(
                getI18nOrDefault(TablesI18n::getModifyTableRemoveTableItemLabel, "Remove table"),
                event -> executeTableAction("remove-table")
        );

        ToolbarSwitch stylesButton = new ToolbarSwitch(VaadinIcon.TABLE, VaadinIcon.EYE);
        stylesButton.setTooltipText(getI18nOrDefault(TablesI18n::getTableTemplatesToolbarSwitchTooltip, "Table Template"));
        stylesButton.setEnabled(false);
        templatesDialog = new TemplateDialog(stylesButton, i18n.getTemplatesI18n());
        templatesDialog.setWidth("26rem"); // turned out to be the best width by default - if not, change in future

        addTableSelectedListener(event -> {
            insertButton.setEnabled(!event.isSelected());
            settingsButton.setEnabled(event.isSelected());
            stylesButton.setEnabled(event.isSelected());

            boolean cellSelectionActive = event.isCellSelectionActive();
            mergeCells.setEnabled(cellSelectionActive);

            // update the styles popup with the selected table's template
            templatesDialog.setActiveTemplate(event.getTemplate());
            templatesDialog.setCurrentPartsEnabled(!cellSelectionActive);
        });

        addTableCellChangedListener(event -> {
            if (event.getRowIndex() != null) {
                templatesDialog.setSelectedRow(event.getRowIndex());
            }
            if (event.getColIndex() != null) {
                templatesDialog.setSelectedColumn(event.getColIndex());
            }
        });

        templatesDialog.setTemplateSelectedCallback((template, fromClient) -> setTemplateForCurrentTable(template, fromClient));
        templatesDialog.setTemplatesChangedCallback((templates, fromClient) -> {
//            try {
                String string = TemplateParser.parse(templates);
                setClientSideStyles(string);

                fireEvent(new TemplatesChangedEvent(this, true, templates, string));
//            } catch (Exception e) {
//                TODO add error handler?
//                Notification
//                        .show("Could not parse changes from template popup. Please check your inputs and inform the admin.")
//                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
//
//
//            }
        });

        rte.addCustomToolbarComponents(insertButton, settingsButton, stylesButton);
    }

    /**
     * Sets the style templates to be used for this instance. These templates will be converted to css and
     * applied to the client side to modify the tables' appearance.
     * @param templates templates json object.
     */
    public void setTemplates(JsonObject templates) {
        if (templatesDialog != null) {
            templatesDialog.setTemplates(templates);
        }
        String cssString = TemplateParser.parse(templates);
        setClientSideStyles(cssString);
        fireEvent(new TemplatesChangedEvent(this, false, templates, cssString));
    }

    /**
     * Returns the current templates. Only available when the style popup has been activated.
     * @return templates json object or null
     */
    public JsonObject getTemplates() {
        return templatesDialog != null ? templatesDialog.getTemplates() : null;
    }

    /**
     * Returns the current templates as a css string. Only available when the style popup has been activated.
     * @return css string or null
     */
    public String getTemplatesAsCssString() {
        JsonObject templates = getTemplates();
        return templates != null ? TemplateParser.parse(templates) : null;
    }

    /**
     * Interpretes the given string as css (without any additional parsing or escaping!) and sets it
     * as the client side style for tables in the RTE. This will override any internal set styles and might
     * be overriden itself, when the styles popup is used together with templates.
     * @param cssString css string
     */
    public void setClientSideStyles(String cssString) {
        rte.getElement().executeJs(SCRIPTS_TABLE + "_setStyles(this, $0)", cssString);
    }

    public void insertTableAtCurrentPosition(int rows, int cols) {
        if (rows <= 0 || cols <= 0) {
            throw new IllegalArgumentException("Rows and cols must be greater than 0!");
        }

        rte.getElement().executeJs(SCRIPTS_TABLE+ "insert(this, $0, $1)", rows, cols);
    }

    public void setTemplateForCurrentTable(@Nullable String template) {
        setTemplateForCurrentTable(template, false);
    }

    private void setTemplateForCurrentTable(@Nullable String template, boolean fromClient) {
        rte.getElement().executeJs(SCRIPTS_TABLE + "setTemplate(this, $0)", template);

        fireEvent(new TemplateSelectedEvent(this, fromClient, template));
    }

    public void executeTableAction(String action) {
        rte.getElement().executeJs(SCRIPTS_TABLE+ "action(this, $0)", action);
    }

    public void executeTableRowAction(String action) {
        executeTableAction(action);
        if (action.contains("remove")) {
            templatesDialog.updateRowIndexesOnRemove();
        } else {
            templatesDialog.updateRowIndexesOnAdd(action.contains("above"));
        }
    }

    public void executeTableColumnAction(String action) {
        executeTableAction(action);
        if (action.contains("remove")) {
            templatesDialog.updateColIndexesOnRemove();
        } else {
            templatesDialog.updateColIndexesOnAdd(action.contains("before"));
        }
    }

    private IntegerField createTableInsertNumberField(String placeholder, String tooltip) {
        IntegerField field = new IntegerField();
        field.setValue(1);
        field.addValueChangeListener(event -> {
            if (event.getSource().isEmpty()) {
                event.getSource().setValue(1);
            }
        });

        field.setMin(1);
        field.setMax(10);
        field.setAutoselect(true);

        field.setStepButtonsVisible(true);
        field.setPlaceholder(placeholder);
        field.setTooltipText(tooltip);

        return field;
    }

    private void fireEvent(EnhancedRichTextEditorTablesComponentEvent event) {
        ComponentUtil.fireEvent(rte, event);
    }

    private <T extends ComponentEvent<EnhancedRichTextEditor>> Registration addListener(Class<T> type, ComponentEventListener<T> listener) {
        return ComponentUtil.addListener(rte, type, listener);
    }

    public Registration addTemplatesChangedListener(ComponentEventListener<TemplatesChangedEvent> listener) {
        return addListener(TemplatesChangedEvent.class, listener);
    }

    public Registration addTemplateSelectedListener(ComponentEventListener<TemplateSelectedEvent> listener) {
        return addListener(TemplateSelectedEvent.class, listener);
    }

    public Registration addTableSelectedListener(ComponentEventListener<TableSelectedEvent> listener) {
        return addListener(TableSelectedEvent.class, listener);
    }

    public Registration addTableCellChangedListener(ComponentEventListener<TableCellChangedEvent> listener) {
        return addListener(TableCellChangedEvent.class, listener);
    }

    public EnhancedRichTextEditor getRte() {
        return rte;
    }

    public String getI18nOrDefault(ValueProvider<TablesI18n, String> valueProvider, String defaultValue) {
        String value = valueProvider.apply(this.i18n);
        return value != null ? value : defaultValue;
    }

    /**
     * Returns the dialog used for template management.
     * @return templates dialog
     */
    public TemplateDialog getTemplatesDialog() {
        return templatesDialog;
    }
}