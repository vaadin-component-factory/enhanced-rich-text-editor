package com.vaadin.componentfactory.erte.tables.templates;

import com.vaadin.componentfactory.erte.tables.TablesI18n;
import com.vaadin.componentfactory.erte.tables.TablesI18n.TemplatesI18n;
import com.vaadin.componentfactory.erte.tables.templates.ruleformparts.*;
import com.vaadin.componentfactory.toolbar.ToolbarDialog;
import com.vaadin.componentfactory.toolbar.ToolbarSwitch;
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
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.textfield.TextFieldVariant;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.function.SerializableBiConsumer;
import com.vaadin.flow.function.ValueProvider;
import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;
import javax.print.attribute.standard.MediaSize;
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
    private ComboBox<String> templateField;
    private JsonObject currentTemplate;
    private JsonObject templates = Json.createObject();
    private final CurrentRowFormPart currentRowFormPart;
    private final CurrentColFormPart currentColFormPart;

    private final List<RuleFormPart> parts = new ArrayList<>();
    private SerializableBiConsumer<String, Boolean> templateSelectedCallback;
    private SerializableBiConsumer<JsonObject, Boolean> templatesChangedCallback;

    private static final String TEMPLATE_NAME = "template";

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

        Details tableDetails = addPartDetails(getI18nOrDefault(TemplatesI18n::getTableSectionTitle, "Table"));
        addPart(new TableFormPart(this), tableDetails);

        Details currentRowDetails = addPartDetails(getI18nOrDefault(TemplatesI18n::getCurrentRowSectionTitle, "Current Row"));
        currentRowFormPart = addPart(new CurrentRowFormPart(this), currentRowDetails);

        Details currentColDetails = addPartDetails(getI18nOrDefault(TemplatesI18n::getCurrentColumnSectionTitle, "Current Column"));
        currentColFormPart = addPart(new CurrentColFormPart(this), currentColDetails);

        Details fixedRowsDetails = addPartDetails(getI18nOrDefault(TemplatesI18n::getSpecialRowsSectionTitle, "Special Rows"), false);
        addPart(new FixedIndexRowFormPart(this, getI18nOrDefault(TemplatesI18n::getSpecialRowsHeaderRowTitle, "Header Row"), "0n+1"), fixedRowsDetails);
        addPart(new FixedIndexRowFormPart(this, getI18nOrDefault(TemplatesI18n::getSpecialRowsFooterRowTitle, "Footer Row"), "0n+1", true), fixedRowsDetails);
        addPart(new FixedIndexRowFormPart(this, getI18nOrDefault(TemplatesI18n::getSpecialRowsEvenRowsTitle, "Even Rows"), "2n"), fixedRowsDetails);
        addPart(new FixedIndexRowFormPart(this, getI18nOrDefault(TemplatesI18n::getSpecialRowsOddRowsTitle, "Odd Rows"), "2n+1"), fixedRowsDetails);
        add(layout);

        setFocusOnOpenTarget(templateField);

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
        templateField = new ComboBox<>();
        templateField.setLabel(getI18nOrDefault(TemplatesI18n::getCurrentTemplateSelectFieldLabel, "Current Template"));

        TextField nameField = new TextField(getI18nOrDefault(TemplatesI18n::getCurrentTemplateNameFieldLabel, "Name"));
        nameField.addThemeVariants(TextFieldVariant.LUMO_SMALL);

        Button createNewTemplate = initCreateTemplateButton();
        Button copySelectedTemplate = initCopySelectedTemplateButton();
        Button deleteSelectedTemplate = initDeleteSelectedTemplateButton();
        HorizontalLayout buttons = new HorizontalLayout(createNewTemplate, copySelectedTemplate, deleteSelectedTemplate);

        HorizontalLayout templateContainer = new HorizontalLayout(templateField, buttons, nameField);
        templateContainer.setAlignItems(FlexComponent.Alignment.BASELINE);
        templateContainer.getStyle().set("flex-wrap", "wrap");
        templateContainer.addClassNames("form-part");
        layout.add(templateContainer);

        Binder<JsonObject> nameBinder = new Binder<>();
        nameBinder.forField(nameField)
                .asRequired()
                .withValidator(s -> {
                    Set<String> strings = collectExistingNames(false);
                    boolean b = !strings.contains(s);
                    return b;
                }, getI18nOrDefault(TemplatesI18n::getCurrentTemplateNameNotUniqueError, "Name already used!"))
                .bind(o -> o.getString(NAME), (o, s) -> o.put(NAME, s));

        nameBinder.addValueChangeListener(event -> {
            if (nameBinder.validate().isOk()) {
                String value = templateField.getValue();
                templateField.getDataProvider().refreshAll();
                templateField.setValue(value);
            }
        });

        templateField.addValueChangeListener(event -> {
            String value = StringUtils.trimToNull(event.getValue());
            if (value != null && this.templates.hasKey(value)) {
                currentTemplate = this.templates.getObject(value);
                nameBinder.setBean(currentTemplate);
            } else {
                currentTemplate = null;
                nameBinder.removeBean();
            }

            if (currentTemplate != null) {
                parts.forEach(ruleFormPart -> ruleFormPart.readTemplate(currentTemplate));
            }

            nameField.setEnabled(currentTemplate != null);
            copySelectedTemplate.setEnabled(currentTemplate != null);
            deleteSelectedTemplate.setEnabled(currentTemplate != null);

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
            templateField.setValue(name);
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
            templateField.setValue(key);
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
                templates.remove(templateField.getValue());
                updateTemplatesField();
                notifyTemplateCange(event.isFromClient());
                templateField.clear();
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

        templateField.setItems(keys);
        templateField.setItemLabelGenerator(item -> this.templates.getObject(item).getString("name"));
    }

    public void setSelectedRow(int row) {
        currentRowFormPart.setSelectedRow(row); // maybe combine these two calls when form part stores the template
        currentRowFormPart.readTemplate(currentTemplate);
    }

    public void setSelectedColumn(int col) {
        currentColFormPart.setSelectedColumn(col); // maybe combine these two calls when form part stores the template
        currentColFormPart.readTemplate(currentTemplate);
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
        templateField.setValue(template);
    }

    public Optional<String> getActiveTemplate() {
        return templateField.getOptionalValue();
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

//    private void applyOverlayPopupCloseWorkaround(Component component) {
//        component.getElement().executeJs("this.addEventListener('opened-changed', e => {" +
//                                         "if(e.detail.value) {  " +
//                                         "    $0.__stayOpen = true;" +
//                                         "}" +
//                                         "});", this);
//    }


}