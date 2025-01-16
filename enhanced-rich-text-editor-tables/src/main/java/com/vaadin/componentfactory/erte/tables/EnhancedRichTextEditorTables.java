package com.vaadin.componentfactory.erte.tables;

import com.vaadin.componentfactory.EnhancedRichTextEditor;
import com.vaadin.componentfactory.erte.tables.events.TableCellChangedEvent;
import com.vaadin.componentfactory.erte.tables.events.TableSelectedEvent;
import com.vaadin.componentfactory.erte.tables.templates.TemplateDialog;
import com.vaadin.componentfactory.erte.tables.templates.TemplateParser;
import com.vaadin.componentfactory.erte.tables.templates.events.*;
import com.vaadin.componentfactory.toolbar.ToolbarPopover;
import com.vaadin.componentfactory.toolbar.ToolbarSelectPopup;
import com.vaadin.componentfactory.toolbar.ToolbarSwitch;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dependency.NpmPackage;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.function.ValueProvider;
import com.vaadin.flow.shared.Registration;
import elemental.json.JsonObject;
import jakarta.annotation.Nullable;
import org.apache.commons.lang3.StringUtils;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A table extension for the {@link EnhancedRichTextEditor}. Allows the user to define new or modify existing
 * tables in an ERTE document.
 */
@NpmPackage(value = "quill-delta", version = "5.1.0")
@JsModule("./src/erte-table/connector.js")
@CssImport(value = "./src/erte-table/css/erte-shadow.css", themeFor = "vcf-enhanced-rich-text-editor")
@CssImport(value = "./src/erte-table/css/toolbar.css")
public class EnhancedRichTextEditorTables {

    private static final Pattern ASSIGNED_TEMPLATE_IDS_DELTA_PATTERN =
            Pattern.compile("\\{\"attributes\":\\{\"0\":\"T\",\"1\":\"A\",\"2\":\"B\",\"3\":\"L\",\"4\":\"E\",\"td\":\"" +
                            // extend these if the pipe pattern changes
                            "([a-zA-Z0-9]+)\\|([a-zA-Z0-9]+)\\|([a-zA-Z0-9]+)\\|" +
                            "([a-zA-Z0-9]*)\\|([a-zA-Z0-9]*)\\|([a-zA-Z0-9]*)\\|" +
                            "([a-zA-Z0-9]+)" +
                            "\"},");

    private static final int ASSIGNED_TEMPLATE_IDS_DELTA_PATTERN_INDEX = 7;

    private static final String SCRIPTS_TABLE = "window.Vaadin.Flow.vcfEnhancedRichTextEditor.extensions.tables.";

    private final EnhancedRichTextEditor rte;
    private final TablesI18n i18n;
    private TemplateDialog templatesDialog;
    private ToolbarSwitch addTableButton;
    private ToolbarSwitch modifyTableButton;
    private ToolbarSwitch styleTemplatesDialogButton;
    private String tableHoverColor;
    private String tableFocusColor;
    private String cellFocusColor;
    private String cellHoverColor;
    private ToolbarSelectPopup modifyTableSelectPopup;
    private ToolbarPopover addTablePopup;

    /**
     * Extends the given ERTE instance with table functionality. Uses the given i18n instance to initialize
     * components with the respective translations.
     * @param rte editor instance to extend
     * @param i18n i18n instance to use
     */
    protected EnhancedRichTextEditorTables(EnhancedRichTextEditor rte, TablesI18n i18n) {
        this.rte = Objects.requireNonNull(rte);
        this.i18n = Objects.requireNonNull(i18n);

        initConnector();

        rte.addAttachListener(event -> {
            if (!event.isInitialAttach()) {
                initConnector(); // init connector on re-attach
            }
        });

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

    private void initConnector() {
        rte.getElement().executeJs(SCRIPTS_TABLE + "init(this)");
    }

    private Integer toInteger(JsonObject object, String key) {
        return object.hasKey(key) ? Double.valueOf(object.getNumber(key)).intValue() : null;
    }

    /**
     * Extends the given ERTE instance with table functionality. Uses the default i18n.
     * @param rte editor instance to extend
     */
    public static EnhancedRichTextEditorTables enable(EnhancedRichTextEditor rte) {
        return enable(rte, new TablesI18n());
    }

    /**
     * Extends the given ERTE instance with table functionality. Uses the given i18n instance to initialize
     * components with the respective translations.
     * @param rte editor instance to extend
     * @param i18n i18n instance to use
     */
    public static EnhancedRichTextEditorTables enable(EnhancedRichTextEditor rte, TablesI18n i18n) {
        EnhancedRichTextEditorTables tables = new EnhancedRichTextEditorTables(rte, i18n);
        tables.initToolbarTable();

        return tables;
    }

    /**
     * Builds up the toolbar, that provides table functionality for the user.
     */
    protected void initToolbarTable() {
        // insert new table
        IntegerField rows = createTableInsertNumberField(
                getI18nOrDefault(TablesI18n::getInsertTableRowsFieldLabel, "Rows"),
                getI18nOrDefault(TablesI18n::getInsertTableRowsFieldTooltip, "Amount of rows for the new table")
        );

        IntegerField cols = createTableInsertNumberField(
                getI18nOrDefault(TablesI18n::getInsertTableColumnsFieldLabel, "Columns"),
                getI18nOrDefault(TablesI18n::getInsertTableColumnsFieldTooltip, "Amount of columns for the new table")

        );

        Button add = new Button(VaadinIcon.PLUS.create(), event -> insertTableAtCurrentPosition(rows.getValue(), cols.getValue()));
        add.setTooltipText(getI18nOrDefault(TablesI18n::getInsertTableAddButtonTooltip, "Add table"));

        addTableButton = new ToolbarSwitch(VaadinIcon.TABLE, VaadinIcon.PLUS);
        addTableButton.setTooltipText(getI18nOrDefault(TablesI18n::getInsertTableToolbarSwitchTooltip, "Add new table"));

        addTablePopup = ToolbarPopover.horizontal(addTableButton, Alignment.BASELINE, rows, new Span("x"), cols, add);
        addTablePopup.setFocusOnOpenTarget(rows);
        add.addClickListener(event -> addTablePopup.setOpened(false));

        modifyTableButton = new ToolbarSwitch(VaadinIcon.TABLE, VaadinIcon.TOOLS);
        modifyTableButton.setTooltipText(getI18nOrDefault(TablesI18n::getModifyTableToolbarSwitchTooltip, "Modify Table"));
        modifyTableButton.setEnabled(false);

        modifyTableSelectPopup = new ToolbarSelectPopup(modifyTableButton);
        modifyTableSelectPopup.addItem(
                getI18nOrDefault(TablesI18n::getModifyTableAppendRowAboveItemLabel, "Append row above"),
                event -> executeTableRowAction("append-row-above")
        );

        modifyTableSelectPopup.addItem(
                getI18nOrDefault(TablesI18n::getModifyTableAppendRowBelowItemLabel, "Append row below"),
                event -> executeTableRowAction("append-row-below")
        );
        modifyTableSelectPopup.addItem(
                getI18nOrDefault(TablesI18n::getModifyTableRemoveRowItemLabel, "Remove row"),
                event -> executeTableRowAction("remove-row")
        );
        modifyTableSelectPopup.add(new Hr());
        modifyTableSelectPopup.addItem(
                getI18nOrDefault(TablesI18n::getModifyTableAppendColumnBeforeItemLabel, "Append column before"),
                event -> executeTableColumnAction("append-col-before")
        );
        modifyTableSelectPopup.addItem(
                getI18nOrDefault(TablesI18n::getModifyTableAppendColumnAfterItemLabel, "Append column after"),
                event -> executeTableColumnAction("append-col-after")
        );
        modifyTableSelectPopup.addItem(
                getI18nOrDefault(TablesI18n::getModifyTableRemoveColumnItemLabel, "Remove column"),
                event -> executeTableColumnAction("remove-col")
        );

        modifyTableSelectPopup.add(new Hr());
        MenuItem mergeCells = modifyTableSelectPopup.addItem(
                getI18nOrDefault(TablesI18n::getModifyTableMergeCellsItemLabel, "Merge selected cells"),
                event -> executeTableAction( "merge-selection")
        );

        modifyTableSelectPopup.addItem(
                getI18nOrDefault(TablesI18n::getModifyTableSplitCellItemLabel, "Split cell"),
                event -> executeTableAction("split-cell")
        );

        modifyTableSelectPopup.add(new Hr());
        modifyTableSelectPopup.addItem(
                getI18nOrDefault(TablesI18n::getModifyTableRemoveTableItemLabel, "Remove table"),
                event -> executeTableAction("remove-table")
        );

        styleTemplatesDialogButton = new ToolbarSwitch(VaadinIcon.TABLE, VaadinIcon.EYE);
        styleTemplatesDialogButton.setTooltipText(getI18nOrDefault(TablesI18n::getTableTemplatesToolbarSwitchTooltip, "Style Templates"));
        styleTemplatesDialogButton.setEnabled(false);
        templatesDialog = new TemplateDialog(styleTemplatesDialogButton, i18n.getTemplatesI18n());
        templatesDialog.setWidth("26rem"); // turned out to be the best width by default - if not, change in future

        addTableSelectedListener(event -> {
            addTableButton.setEnabled(!event.isSelected());
            modifyTableButton.setEnabled(event.isSelected());
            styleTemplatesDialogButton.setEnabled(event.isSelected());

            if(!event.isSelected()) { // close the dialog, when not having a table selected
                styleTemplatesDialogButton.setActive(false);
            }

            boolean cellSelectionActive = event.isCellSelectionActive();
            mergeCells.setEnabled(cellSelectionActive);


            // update the styles popup with the selected table's template
            templatesDialog.setActiveTemplateId(event.getTemplate());
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

        templatesDialog.setTemplateSelectedCallback((template, fromClient) -> internalUpdateTemplateForCurrentTable(template, fromClient));

        templatesDialog.setTemplateCreatedCallback(details -> {
            onTemplateModificationByTemplateDialog();
            fireEvent(new TemplateCreatedEvent(this, details.isChangedByClient(), details.getId(), details.getModifiedTemplate()));
        });

        templatesDialog.setTemplateCopiedCallback(details -> {
            onTemplateModificationByTemplateDialog();
            fireEvent(new TemplateCopiedEvent(this, details.isChangedByClient(), details.getId(), details.getActiveTemplateId(), details.getModifiedTemplate()));
        });

        templatesDialog.setTemplateUpdatedCallback(details -> {
            onTemplateModificationByTemplateDialog();
            fireEvent(new TemplateUpdatedEvent(this, details.isChangedByClient(), details.getId(), details.getModifiedTemplate()));
        });

        templatesDialog.setTemplateDeletedCallback(details -> {
            onTemplateModificationByTemplateDialog();
            fireEvent(new TemplateDeletedEvent(this, details.isChangedByClient(), details.getId(), details.getModifiedTemplate()));
        });

        rte.addCustomToolbarComponents(addTableButton, modifyTableButton, styleTemplatesDialogButton);

        addTableButton.addActiveChangedListener(event -> {
            if (event.isActive()) {
                modifyTableButton.setActive(false);
                styleTemplatesDialogButton.setActive(false);
            }
        });

        modifyTableButton.addActiveChangedListener(event -> {
            if (event.isActive()) {
                addTableButton.setActive(false);
            }
        });
        styleTemplatesDialogButton.addActiveChangedListener(event -> {
            if (event.isActive()) {
                addTableButton.setActive(false);
            }
        });
    }

    /**
     * To be called, when the dialog modifies the templates in any way.
     */
    private void onTemplateModificationByTemplateDialog() {
        JsonObject templates = getTemplates();
        String string = TemplateParser.convertToCss(templates);
        setClientSideStyles(string);
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
        String cssString = TemplateParser.convertToCss(templates);
        setClientSideStyles(cssString);
        fireEvent(new TemplatesInitialiazedEvent(this, false, templates, cssString));
    }

    /**
     * Returns the current templates. Only available when the style popup has been activated.
     * @return templates json object or null
     */
    public JsonObject getTemplates() {
        return templatesDialog != null ? templatesDialog.getTemplates() : null;
    }

    /**
     * Reads the given delta string and returns all template ids, that are currently assigned to at least one table.
     *
     * @param delta delta string to parse
     * @return set of assigned template ids (empty if none are assigned)
     */
    public static Set<String> getAssignedTemplateIds(String delta) {
        Matcher matcher = ASSIGNED_TEMPLATE_IDS_DELTA_PATTERN.matcher(delta);

        Set<String> ids = new HashSet<>();
        while (matcher.find()) {
            if(matcher.groupCount() >= ASSIGNED_TEMPLATE_IDS_DELTA_PATTERN_INDEX) {
                String templateId = StringUtils.trimToEmpty(matcher.group(ASSIGNED_TEMPLATE_IDS_DELTA_PATTERN_INDEX));
                if (!templateId.isEmpty()) {
                    ids.add(templateId);
                }
            }
        }

        return ids;
    }

    /**
     * Returns the current templates as a css string. Only available when the style popup has been activated.
     * @return css string or null
     */
    public String getTemplatesAsCssString() {
        JsonObject templates = getTemplates();
        return templates != null ? TemplateParser.convertToCss(templates) : null;
    }

    /**
     * Extends any auto generated styles with custom styles.
     * Interpretes the given string as css (without any additional parsing or escaping!)
     * <p/>
     * Depending on the boolean parameter, these custom
     * styles will be placed before or after the auto generated styles. Calling this method again will replace
     * previously set custom styles for the respective position (i.e. calling it twice with {@code true} overrides
     * the previous custom styles placed before the auto generated styles.
     * Calling it once with {@code true} and {@code false} will not override the other variant).
     * <p/>
     *
     * @param cssString css string
     * @param beforeGenerated place custom string before auto generated string
     */
    public void setCustomStyles(String cssString, boolean beforeGenerated) {
        rte.getElement().executeJs(SCRIPTS_TABLE + "_setCustomStyles(this, $0, $1)", cssString, beforeGenerated);
    }

    /**
     * Interpretes the given string as css (without any additional parsing or escaping!) and sets it
     * as the client side style for tables in the RTE. This will override any internal set styles and might
     * be overriden itself, when the styles popup is used together with templates.
     * @param cssString css string
     */
    private void setClientSideStyles(String cssString) {
        if (tableHoverColor != null) {
            cssString = "table td {border: 1px solid transparent}\n\n" + cssString;
            cssString = cssString + "\n\n table:hover td {border: 1px dashed " + tableHoverColor + " !important}\n\n";
        }

        if (cellHoverColor != null) {
            cssString = cssString + "table td:hover {background-image: linear-gradient("+cellHoverColor+", "+cellHoverColor + ") !important}\n\n";
        }

        if (tableFocusColor != null) {
            cssString = cssString + "table:has(td.focused-cell) {position:relative;}\n\n";
            cssString = cssString + "table:has(td.focused-cell)::after {    content: '';\n" +
                        "    border: 1px dashed " + tableFocusColor + " !important;\n" +
                        "    width: 100%;\n" +
                        "    height: 100%;\n" +
                        "    position: absolute;\n" +
                        "    top: 0;\n" +
                        "    left: 0;\n" +
                        "    box-sizing: border-box;" +
                        "    pointer-events:none}\n\n";
        }

        if (cellFocusColor != null) {
            cssString = cssString + "table td.focused-cell {background-image: linear-gradient(" + cellFocusColor + ", " + cellFocusColor + ") !important}";
        }

        rte.getElement().executeJs(SCRIPTS_TABLE + "_setStyles(this, $0)", cssString);
    }

    /**
     * Inserst a new table with the given dimension at the current cursor position.
     * @param rows amount of rows
     * @param cols amount of cols
     */
    public void insertTableAtCurrentPosition(int rows, int cols) {
        insertTableAtCurrentPosition(rows, cols, null);
    }

    /**
     * Inserst a new table with the given dimension at the current cursor position. Applies the given template
     * to the table. This template must match the css class name of the template / its id, not its display name.
     * @param rows amount of rows
     * @param cols amount of cols
     * @param templateId template class name
     */
    public void insertTableAtCurrentPosition(int rows, int cols, String templateId) {
        if (rows <= 0 || cols <= 0) {
            throw new IllegalArgumentException("Rows and cols must be greater than 0!");
        }

        rte.getElement().executeJs(SCRIPTS_TABLE+ "insert(this, $0, $1, $2)", rows, cols, templateId);
    }

    /**
     * Sets the given template id as current / active template to use.
     * @param templateId template
     */
    public void setTemplateIdForCurrentTable(@Nullable String templateId) {
        if(templatesDialog != null) {
            templatesDialog.setActiveTemplateId(templateId);
        } else { // fallback if no templates dialog is available
            internalUpdateTemplateForCurrentTable(templateId, false);
        }
    }

    /**
     * This internal method will update the current template for the table on the client and fire a template selected
     * event. Intended to be used by the template dialog, or if none is available, by the setTemplateForCurrentTable
     * method.
     * @param template template to be set
     * @param fromClient change comes from client
     */
    private void internalUpdateTemplateForCurrentTable(@Nullable String template, boolean fromClient) {
        rte.getElement().executeJs(SCRIPTS_TABLE + "setTemplate(this, $0)", template);
        fireEvent(new TemplateSelectedEvent(this, fromClient, template));
    }

    /**
     * Executes a specific client side action on the table. See TableTrick.js#table_handler for more details.
     * @param action action
     */
    protected void executeTableAction(String action) {
        rte.getElement().executeJs(SCRIPTS_TABLE+ "action(this, $0)", action);
    }

    /**
     * Executes a specific, row related client side action on the table. See TableTrick.js#table_handler for more details.
     * @param action action
     */
    protected void executeTableRowAction(String action) {
        executeTableAction(action);
        if (action.contains("remove")) {
            templatesDialog.updateRowIndexesOnRemove();
        } else {
            templatesDialog.updateRowIndexesOnAdd(action.contains("above"));
        }
    }
    /**
     * Executes a specific, col related client side action on the table. See TableTrick.js#table_handler for more details.
     * @param action action
     */
    protected void executeTableColumnAction(String action) {
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
        field.setLabel(placeholder);
        field.setTooltipText(tooltip);

        return field;
    }

    private void fireEvent(EnhancedRichTextEditorTablesComponentEvent event) {
        ComponentUtil.fireEvent(rte, event);
    }

    private <T extends ComponentEvent<EnhancedRichTextEditor>> Registration addListener(Class<T> type, ComponentEventListener<T> listener) {
        return ComponentUtil.addListener(rte, type, listener);
    }

    public Registration addTemplatesInitializedListener(ComponentEventListener<TemplatesInitialiazedEvent> listener) {
        return addListener(TemplatesInitialiazedEvent.class, listener);
    }

    public Registration addTemplateCreatedListener(ComponentEventListener<TemplateCreatedEvent> listener) {
        return addListener(TemplateCreatedEvent.class, listener);
    }

    public Registration addTemplateCopiedListener(ComponentEventListener<TemplateCopiedEvent> listener) {
        return addListener(TemplateCopiedEvent.class, listener);
    }

    public Registration addTemplateUpdatedListener(ComponentEventListener<TemplateUpdatedEvent> listener) {
        return addListener(TemplateUpdatedEvent.class, listener);
    }

    public Registration addTemplateDeletedListener(ComponentEventListener<TemplateDeletedEvent> listener) {
        return addListener(TemplateDeletedEvent.class, listener);
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
    public TemplateDialog getStyleTemplatesDialog() {
        return templatesDialog;
    }

    public ToolbarSwitch getAddTableToolbarButton() {
        return addTableButton;
    }

    public ToolbarPopover getAddTablePopup() {
        return addTablePopup;
    }

    public ToolbarSwitch getModifyTableToolbarButton() {
        return modifyTableButton;
    }

    public ToolbarSelectPopup getModifyTableSelectPopup() {
        return modifyTableSelectPopup;
    }

    public ToolbarSwitch getStyleTemplatesDialogToolbarButton() {
        return styleTemplatesDialogButton;
    }

    /**
     * This method activates a UX helping feature. When setting a css color, that color will be shown as
     * the table cells border, when the user hovers the table. Passing null will disable this feature.
     * @param hoverColor css color
     */
    public void setTableHoverColor(@Nullable String hoverColor) {
        this.tableHoverColor = hoverColor;
        if (templatesDialog != null && templatesDialog.getTemplates() != null) {
            setClientSideStyles(TemplateParser.convertToCss(templatesDialog.getTemplates()));
        } else {
            setClientSideStyles("");
        }
    }

    /**
     * This method activates a UX helping feature. When setting a css color, that color will be shown as
     * a slight cell background color, when the user hovers a table cell. Passing null will disable this feature.
     * @param hoverColor css color
     */
    public void setTableCellHoverColor(@Nullable String hoverColor) {
        this.cellHoverColor = hoverColor;
        if (templatesDialog != null && templatesDialog.getTemplates() != null) {
            setClientSideStyles(TemplateParser.convertToCss(templatesDialog.getTemplates()));
        } else {
            setClientSideStyles("");
        }
    }

    /**
     * This method activates a UX helping feature. When setting a css color, that color will be shown as
     * a slight cell background color, when the user focuses a table cell. Passing null will disable this feature.
     * @param focusColor css color
     */
    public void setTableCellFocusColor(@Nullable String focusColor) {
        this.cellFocusColor = focusColor;
        if (templatesDialog != null && templatesDialog.getTemplates() != null) {
            setClientSideStyles(TemplateParser.convertToCss(templatesDialog.getTemplates()));
        } else {
            setClientSideStyles("");
        }
    }

    /**
     * This method activates a UX helping feature. When setting a css color, that color will be shown as
     * a the table border color, when the user focuses a table cell. Passing null will disable this feature.
     * @param focusColor css color
     */
    public void setTableFocusColor(@Nullable String focusColor) {
        this.tableFocusColor = focusColor;
        if (templatesDialog != null && templatesDialog.getTemplates() != null) {
            setClientSideStyles(TemplateParser.convertToCss(templatesDialog.getTemplates()));
        } else {
            setClientSideStyles("");
        }
    }

}