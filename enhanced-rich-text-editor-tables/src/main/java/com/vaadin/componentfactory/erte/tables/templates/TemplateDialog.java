package com.vaadin.componentfactory.erte.tables.templates;

import com.vaadin.componentfactory.erte.tables.TablesI18n.TemplatesI18n;
import com.vaadin.componentfactory.erte.tables.templates.ruleformparts.*;
import com.vaadin.componentfactory.erte.toolbar.ToolbarDialog;
import com.vaadin.componentfactory.erte.toolbar.ToolbarSwitch;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.textfield.TextFieldVariant;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.function.SerializableBiConsumer;
import com.vaadin.flow.function.ValueProvider;
import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.vaadin.componentfactory.erte.tables.templates.TemplateJsonConstants.*;

public class TemplateDialog extends ToolbarDialog {

    private final VerticalLayout layout;
    private final TemplatesI18n i18n;
    private final Details tableDetails;
    private final Details currentRowDetails;
    private final Details currentColDetails;
    private final Details specialRowsDetails;
    private final TableFormPart tableFormPart;
    private final FixedIndexRowFormPart headerRowFormPart;
    private final FixedIndexRowFormPart footerRowFormPart;
    private final FixedIndexRowFormPart evenRowsFormPart;
    private final FixedIndexRowFormPart oddRowsFormPart;
    private ComboBox<String> templateSelectionField;
    private JsonObject currentTemplate;
    private JsonObject templates = Json.createObject();
    private final CurrentRowFormPart currentRowFormPart;
    private final CurrentColFormPart currentColFormPart;

    private final List<RuleFormPart> parts = new ArrayList<>();
    private SerializableBiConsumer<String, Boolean> templateSelectedCallback;
    private SerializableBiConsumer<JsonObject, Boolean> templatesChangedCallback;

    private static final String TEMPLATE_NAME = "template";
    private TextField templateNameField;
    private Button createNewTemplateButton;
    private Button copySelectedTemplateButton;
    private Button deleteSelectedTemplateButton;
    private HorizontalLayout templateButtonsContainer;
    private HorizontalLayout templateSection;

    public TemplateDialog(ToolbarSwitch referencedSwitch, TemplatesI18n i18n) {
        super(referencedSwitch);
        this.i18n = i18n;

        setHeaderTitle(getI18nOrDefault(TemplatesI18n::getDialogTitle, "Table Templates"));
        Button button = new Button(VaadinIcon.CLOSE.create(), event -> close());
        button.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        getHeader().add(button);

        layout = new VerticalLayout();
        layout.setSpacing(false);

        initTemplateSelection();

        tableDetails = addPartDetails(getI18nOrDefault(TemplatesI18n::getTableSectionTitle, "Table"));
        tableFormPart = addPart(new TableFormPart(this), tableDetails);

        currentRowDetails = addPartDetails(getI18nOrDefault(TemplatesI18n::getCurrentRowSectionTitle, "Current Row"));
        currentRowFormPart = addPart(new CurrentRowFormPart(this), currentRowDetails);

        currentColDetails = addPartDetails(getI18nOrDefault(TemplatesI18n::getCurrentColumnSectionTitle, "Current Column"));
        currentColFormPart = addPart(new CurrentColFormPart(this), currentColDetails);

        specialRowsDetails = addPartDetails(getI18nOrDefault(TemplatesI18n::getSpecialRowsSectionTitle, "Special Rows"), false);
        headerRowFormPart = addPart(new FixedIndexRowFormPart(this, getI18nOrDefault(TemplatesI18n::getSpecialRowsHeaderRowTitle, "Header Row"), "0n+1"), specialRowsDetails);
        footerRowFormPart = addPart(new FixedIndexRowFormPart(this, getI18nOrDefault(TemplatesI18n::getSpecialRowsFooterRowTitle, "Footer Row"), "0n+1", true), specialRowsDetails);
        evenRowsFormPart = addPart(new FixedIndexRowFormPart(this, getI18nOrDefault(TemplatesI18n::getSpecialRowsEvenRowsTitle, "Even Rows"), "2n"), specialRowsDetails);
        oddRowsFormPart = addPart(new FixedIndexRowFormPart(this, getI18nOrDefault(TemplatesI18n::getSpecialRowsOddRowsTitle, "Odd Rows"), "2n+1"), specialRowsDetails);
        add(layout);

        setFocusOnOpenTarget(templateSelectionField);

        addOpenedChangeListener(event -> {
            if (event.isOpened()) {
                getElement().executeJs("const {left, top, width, height} = $0.getBoundingClientRect();" +
                                       "this.$.overlay.$.overlay.style.position = 'absolute';" +
                                       "this.$.overlay.$.overlay.style.left = left + width + 'px';",
                        getToolbarSwitch());
            }
        });
    }

    private void initTemplateSelection() {
        templateSelectionField = new ComboBox<>();
        templateSelectionField.setLabel(getI18nOrDefault(TemplatesI18n::getCurrentTemplateSelectFieldLabel, "Current Template"));

        templateNameField = new TextField(getI18nOrDefault(TemplatesI18n::getCurrentTemplateNameFieldLabel, "Name"));
        templateNameField.addThemeVariants(TextFieldVariant.LUMO_SMALL);

        createNewTemplateButton = initCreateTemplateButton();
        copySelectedTemplateButton = initCopySelectedTemplateButton();
        deleteSelectedTemplateButton = initDeleteSelectedTemplateButton();
        templateButtonsContainer = new HorizontalLayout(createNewTemplateButton, copySelectedTemplateButton, deleteSelectedTemplateButton);

        templateSection = new HorizontalLayout(templateSelectionField, templateButtonsContainer, templateNameField);
        templateSection.setAlignItems(FlexComponent.Alignment.BASELINE);
        templateSection.getStyle().set("flex-wrap", "wrap");
        templateSection.addClassNames("form-part");
        layout.add(templateSection);

        Binder<JsonObject> nameBinder = new Binder<>();
        nameBinder.forField(templateNameField)
                .asRequired()
                .withValidator(s -> {
                    Set<String> strings = collectExistingNames(false);
                    boolean b = !strings.contains(s);
                    return b;
                }, getI18nOrDefault(TemplatesI18n::getCurrentTemplateNameNotUniqueError, "Name already used!"))
                .bind(o -> o.getString(NAME), (o, s) -> o.put(NAME, s));

        nameBinder.addValueChangeListener(event -> {
            if (nameBinder.validate().isOk()) {
                String value = templateSelectionField.getValue();
                templateSelectionField.getDataProvider().refreshAll();
                templateSelectionField.setValue(value);
            }
        });

        templateSelectionField.addValueChangeListener(event -> {
            String value = StringUtils.trimToNull(event.getValue());
            if (value != null && this.templates.hasKey(value)) {
                currentTemplate = this.templates.getObject(value);
                nameBinder.setBean(currentTemplate);
            } else {
                currentTemplate = null;
                nameBinder.removeBean();
            }

            if (currentTemplate != null) {
                parts.forEach(ruleFormPart -> {
                    ruleFormPart.setEnabled(true);
                    ruleFormPart.readTemplate(currentTemplate);
                });
            } else {
                parts.forEach(ruleFormPart -> ruleFormPart.setEnabled(false));
            }

            templateNameField.setEnabled(currentTemplate != null);
            copySelectedTemplateButton.setEnabled(currentTemplate != null);
            deleteSelectedTemplateButton.setEnabled(currentTemplate != null);

            if(templateSelectedCallback != null) {
                templateSelectedCallback.accept(value, event.isFromClient());
            }
        });
    }

    protected Button initCreateTemplateButton() {
        Button createNewTemplate = new Button(VaadinIcon.PLUS.create());
        createNewTemplate.setTooltipText(getI18nOrDefault(TemplatesI18n::getCreateNewTemplateButtonTooltip, "Create new template"));
        createNewTemplate.addThemeVariants(ButtonVariant.LUMO_SMALL);

        createNewTemplate.addClickListener(event -> {
            String name = generateTemplateKey();
            JsonObject object = Json.createObject();
            object.put(NAME, name);
            templates.put(name, object);
            updateTemplatesField();
            notifyTemplateCange(event.isFromClient());
            templateSelectionField.setValue(name);
        });

        return createNewTemplate;
    }

    protected Button initCopySelectedTemplateButton() {
        Button copySelectedTemplate = new Button(VaadinIcon.COPY.create());
        copySelectedTemplate.setEnabled(false);
        copySelectedTemplate.addThemeVariants(ButtonVariant.LUMO_SMALL);
        copySelectedTemplate.setTooltipText(getI18nOrDefault(TemplatesI18n::getCopyTemplateButtonTooltip, "Copy template"));


        copySelectedTemplate.addClickListener(event -> {
            String currentName = currentTemplate.getString(NAME);
            String key = generateTemplateKey();

            String i18nCopy = getI18nOrDefault(TemplatesI18n::getTemplateCopySuffix, "Copy");

            int copyIndex = currentName.indexOf("- " + i18nCopy);
            if (copyIndex < 0) {
                currentName += " - " + i18nCopy;
            } else {
                int numberedCopyIndex = currentName.indexOf("(", copyIndex);
                int start = 1;
                String firstPart;
                if (numberedCopyIndex >= 0) { // count the copy up - a convenient feature to prevent "- Kopie - Kopie" etc tails
                    int endOf = currentName.indexOf(")", numberedCopyIndex);
                    String substring = currentName.substring(numberedCopyIndex, endOf);
                    firstPart = currentName.substring(0, numberedCopyIndex);
                    try {
                        start = Integer.parseInt(substring);
                    } catch (NumberFormatException numberFormatException) {
                        // could not parse the number, so instead we just ignore it and count up our own
                    }
                } else {
                    firstPart = currentName + " "; // otherwise the parentheses will be attached directly to the copied name
                }


                currentName = generateNumberedName(firstPart + "(", ")", start, collectExistingNames());
            }
            JsonObject clone = clone(currentTemplate);
            clone.put(NAME, currentName);
            templates.put(key, clone);
            updateTemplatesField();
            notifyTemplateCange(event.isFromClient());
            templateSelectionField.setValue(key);
        });

        return copySelectedTemplate;
    }

    private Set<String> collectExistingNames() {
        return collectExistingNames(true);
    }

    private Set<String> collectExistingNames(boolean withCurrentTemplate) {
        Stream<JsonObject> stream = Stream.of(templates.keys())
                .map(templates::getObject);

        if (!withCurrentTemplate && currentTemplate != null) {
            stream = stream.filter(o -> o != currentTemplate);
        }

        return stream
                .map(o -> o.getString(NAME))
                .collect(Collectors.toSet());
    }

    protected Button initDeleteSelectedTemplateButton() {
        Button button = new Button(VaadinIcon.TRASH.create());
        button.setEnabled(false);
        button.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR);
        button.setTooltipText(getI18nOrDefault(TemplatesI18n::getDeleteTemplateButtonTooltip, "Delete template"));


        button.addClickListener(event -> {
            String currentName = currentTemplate.getString(NAME);
            new ConfirmDialog(
                    getI18nOrDefault(TemplatesI18n::getDeleteTemplateConfirmTitle, "Delete Template"),
                    getI18nOrDefault(TemplatesI18n::getDeleteTemplateConfirmText, "Shall the selected template be deleted? This process is irreversible."),
                    getI18nOrDefault(TemplatesI18n::getDeleteTemplateConfirmYesButton, "Delete"), confirmEvent -> {
                templates.remove(templateSelectionField.getValue());
                updateTemplatesField();
                notifyTemplateCange(event.isFromClient());
                templateSelectionField.clear();
            }, getI18nOrDefault(TemplatesI18n::getDeleteTemplateConfirmNoButton, "Cancel"), cancelEvent -> {
            }).open();
        });

        return button;
    }

    private String generateTemplateKey() {
        return generateNumberedName(TEMPLATE_NAME, "", 1, Set.of(templates.keys()));
    }

    private String generateNumberedName(String prefix, String suffix, int startingCount, Set<String> existingItems) {
        int i = startingCount;
        String name;

        do {
            name = prefix + (i++) + suffix;
        }
        while (existingItems.contains(name));

        return name;
    }

    private Details addPartDetails(String title) {
        return addPartDetails(title, true);
    }

    private Details addPartDetails(String title, boolean initiallyOpened) {
        Details details = new Details(title);
        details.setOpened(initiallyOpened);
        layout.add(details);
        return details;
    }

    private <T extends RuleFormPart> T addPart(T part, Component container) {
        part.addValueChangeListener(event -> {
            notifyTemplateCange(event.isFromClient());
        });
        container.getElement().appendChild(part.getElement());
        parts.add(part);
        return part;
    }

    private void notifyTemplateCange(boolean fromClient) {
        if (templatesChangedCallback != null) {
            templatesChangedCallback.accept(getTemplates(), fromClient);
        }
    }

    public JsonObject getTemplates() {
        JsonObject clone = clone(templates);
        TemplateParser.removeEmptyChildren(clone);
        return clone;
    }

    private static JsonObject clone(JsonObject objectToClone) {
        return Json.parse(objectToClone.toJson());
    }

    public void setTemplates(JsonObject templates) {
        this.templates = clone(templates);

        updateTemplatesField();
    }

    private void updateTemplatesField() {
        List<String> keys = new ArrayList<>(templates.keys().length);
        for (String key : templates.keys()) {
            if (!TemplateJsonConstants.PATTERN_TEMPLATE_NAME.matcher(key).matches()) {
                throw new IllegalArgumentException("Invalid template name: " + key);
            }
            keys.add(key);
        }

        templateSelectionField.setItems(keys);
        templateSelectionField.setItemLabelGenerator(item -> {
            JsonObject object = this.templates.getObject(item);
            return object != null ? object.getString("name") : ("#" + item);
        });
    }

    public void setSelectedRow(int row) {
        currentRowFormPart.setSelectedRow(row); // maybe combine these two calls when form part stores the template
        if (currentTemplate != null) {
            currentRowFormPart.readTemplate(currentTemplate);
        }
    }

    public void setSelectedColumn(int col) {
        currentColFormPart.setSelectedColumn(col); // maybe combine these two calls when form part stores the template
        if (currentTemplate != null) {
            currentColFormPart.readTemplate(currentTemplate);
        }
    }

    /**
     * Allows to specifically enable / disable the "current selection" form parts. This might be used, if there
     * is a multiline selection or similar.
     * @param enabled enable or disable
     */
    public void setCurrentPartsEnabled(boolean enabled) {
        currentColFormPart.setEnabled(enabled);
        currentRowFormPart.setEnabled(enabled);
    }

    public void setActiveTemplate(@Nullable String template) {
        templateSelectionField.setValue(template);
    }

    public Optional<String> getActiveTemplate() {
        return templateSelectionField.getOptionalValue();
    }

    public void setTemplateSelectedCallback(SerializableBiConsumer<String, Boolean> callback) {
        this.templateSelectedCallback = callback;
    }

    public void setTemplatesChangedCallback(SerializableBiConsumer<JsonObject, Boolean> callback) {
        this.templatesChangedCallback = callback;
    }

    public void updateRowIndexesOnAdd(boolean before) {
        updateIndexesOnAdd(ROWS, before);
    }

    public void updateColIndexesOnAdd(boolean before) {
        updateIndexesOnAdd(COLUMNS, before);
    }

    private void updateIndexesOnAdd(String key, boolean before) {
        if (currentTemplate != null) {
            // if the item has been added before the current one, we need to increase all indices after ours including us
            int startingIndex = (ROWS.equals(key) ? currentRowFormPart.getSelectedRow() : currentColFormPart.getSelectedCol()) + 1;

            if (!before) {
                startingIndex++; // if the item has been added after the current one, we need to increase all indices after ours excluding us
            }

            JsonArray array = currentTemplate.getArray(key);
            if (array != null) {
                for (int i = 0; i < array.length(); i++) {
                    JsonObject value = array.getObject(i);
                    String sIndex = value.getString(INDEX);
                    try {
                        int index = Integer.parseInt(sIndex);
                        if (index >= startingIndex) {
                            value.put(INDEX, String.valueOf(++index));
                        }
                    } catch (NumberFormatException nfe) {
                        // NOOP
                    }

                }

                notifyTemplateCange(true);
            }
        }
    }

    public void updateRowIndexesOnRemove() {
        updateIndexesOnRemove(ROWS);
    }

    public void updateColIndexesOnRemove() {
        updateIndexesOnRemove(COLUMNS);
    }

    private void updateIndexesOnRemove(String key) {
        if (currentTemplate != null) {
            int startingIndex = (ROWS.equals(key) ? currentRowFormPart.getSelectedRow() : currentColFormPart.getSelectedCol()) + 1;

            JsonArray array = currentTemplate.getArray(key);
            if (array != null) {
                for (int i = array.length() - 1; i >= 0 ; i--) {
                    JsonObject value = array.getObject(i);
                    String sIndex = value.getString(INDEX);
                    try {
                        int index = Integer.parseInt(sIndex);
                        if (index == startingIndex) {
                            array.remove(i);
                        } else if (index > startingIndex) {
                            value.put(INDEX, String.valueOf(--index));
                        }
                    } catch (NumberFormatException nfe) {
                        // NOOP
                    }

                }

                notifyTemplateCange(true);
            }
        }
    }

    public String getI18nOrDefault(ValueProvider<TemplatesI18n, String> valueProvider, String defaultValue) {
        String value = valueProvider.apply(this.i18n);
        return value != null ? value : defaultValue;
    }

    /**
     * Returns the section container, that contains the template combobox, the buttons and the template name field.
     * @return template section container
     */
    public HorizontalLayout getTemplateSection() {
        return templateSection;
    }

    /**
     * Returns the section container, that contains the table form part.
     * @return table section
     */
    public Details getTableSection() {
        return tableDetails;
    }

    /**
     * Returns the section container, that contains the current row form part.
     * @return current row section
     */
    public Details getCurrentRowSection() {
        return currentRowDetails;
    }

    /**
     * Returns the section container, that contains the current column form part.
     * @return current column section
     */
    public Details getCurrentColSection() {
        return currentColDetails;
    }

    /**
     * Returns the section container, that contains the special rows form part. Special rows are for instance
     * even and odd rows.
     * @return special rows section
     */
    public Details getSpecialRowsSection() {
        return specialRowsDetails;
    }

    /**
     * Returns the table form part. A form part contains the different input fields to define concrete css stylings.
     * @return table form part
     */
    public TableFormPart getTableFormPart() {
        return tableFormPart;
    }

    /**
     * Returns the header row form part. A form part contains the different input fields to define concrete css stylings.
     * @return header row form part
     */
    public FixedIndexRowFormPart getHeaderRowFormPart() {
        return headerRowFormPart;
    }

    /**
     * Returns the footer row form part. A form part contains the different input fields to define concrete css stylings.
     * @return footer row form part
     */
    public FixedIndexRowFormPart getFooterRowFormPart() {
        return footerRowFormPart;
    }

    /**
     * Returns the even rows form part. A form part contains the different input fields to define concrete css stylings.
     * @return even rows form part
     */
    public FixedIndexRowFormPart getEvenRowsFormPart() {
        return evenRowsFormPart;
    }

    /**
     * Returns the odd rows form part. A form part contains the different input fields to define concrete css stylings.
     * @return odd rows form part
     */
    public FixedIndexRowFormPart getOddRowsFormPart() {
        return oddRowsFormPart;
    }

    /**
     * Returns the current row form part. A form part contains the different input fields to define concrete css stylings.
     * @return current row form part
     */
    public CurrentRowFormPart getCurrentRowFormPart() {
        return currentRowFormPart;
    }

    /**
     * Returns the current column form part. A form part contains the different input fields to define concrete css stylings.
     * @return current column form part
     */
    public CurrentColFormPart getCurrentColFormPart() {
        return currentColFormPart;
    }

    /**
     * Returns the template selection field. This allows the user to switch to a different template for the selected
     * table.
     * @return template selection
     */
    public ComboBox<String> getTemplateSelectionField() {
        return templateSelectionField;
    }

    /**
     * Returns the template name field. This allows the user to change the name of the selected template.
     * @return template name field
     */
    public TextField getTemplateNameField() {
        return templateNameField;
    }

    /**
     * Returns the button that allows the user to create a new template.
     * @return new template button
     */
    public Button getCreateNewTemplateButton() {
        return createNewTemplateButton;
    }

    /**
     * Returns the button, that allows the user to copy the selected template.
     * @return copy selected template button
     */
    public Button getCopySelectedTemplateButton() {
        return copySelectedTemplateButton;
    }

    /**
     * Returns the button, that allows the user to delete the selected template.
     * @return delete selected template button
     */
    public Button getDeleteSelectedTemplateButton() {
        return deleteSelectedTemplateButton;
    }

    /**
     * Returns the component, that contains the template buttons.
     * @return template buttons container
     */
    public HorizontalLayout getTemplateButtonsContainer() {
        return templateButtonsContainer;
    }

    //    private void applyOverlayPopupCloseWorkaround(Component component) {
//        component.getElement().executeJs("this.addEventListener('opened-changed', e => {" +
//                                         "if(e.detail.value) {  " +
//                                         "    $0.__stayOpen = true;" +
//                                         "}" +
//                                         "});", this);
//    }


}