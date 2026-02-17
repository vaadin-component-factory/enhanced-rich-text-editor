package com.vaadin.componentfactory.erte.tables.templates;

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
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.textfield.TextFieldVariant;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.function.SerializableBiConsumer;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.function.ValueProvider;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.shared.Registration;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.vaadin.componentfactory.erte.tables.templates.TemplateJsonConstants.*;

/**
 * Dialog to modify the templates of an ERTE instance.
 */
public class TemplateDialog extends ToolbarDialog {

    private final Defaults defaults = new Defaults(this);

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
    private ObjectNode currentTemplate;
    private ObjectNode templates = JacksonUtils.createObjectNode();
    private final CurrentRowFormPart currentRowFormPart;
    private final CurrentColFormPart currentColFormPart;

    private final List<RuleFormPart> parts = new ArrayList<>();
    private SerializableBiConsumer<String, Boolean> templateSelectedCallback;
    private SerializableConsumer<TemplateModificationDetails> templateCreatedCallback;
    private SerializableConsumer<TemplateModificationDetails> templateCopiedCallback;
    private SerializableConsumer<TemplateModificationDetails> templateDeletedCallback;
    private SerializableConsumer<TemplateModificationDetails> templateUpdatedCallback;

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

        parts.forEach(ruleFormPart -> ruleFormPart.setEnabled(false));

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
        templateSelectionField.setClearButtonVisible(true);

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

        Binder<ObjectNode> nameBinder = new Binder<>();
        nameBinder.forField(templateNameField)
                .asRequired()
                .withValidator(s -> {
                    Set<String> strings = collectExistingNames(false);
                    boolean b = !strings.contains(s);
                    return b;
                }, getI18nOrDefault(TemplatesI18n::getCurrentTemplateNameNotUniqueError, "Name already used!"))
                .bind(o -> o.get(NAME).asText(), (o, s) -> o.put(NAME, s));

        nameBinder.addValueChangeListener(event -> {
            if (nameBinder.validate().isOk()) {
                String value = templateSelectionField.getValue();
                templateSelectionField.getDataProvider().refreshAll();
                templateSelectionField.setValue(value);
            }
        });

        templateSelectionField.addValueChangeListener(event -> {
            String value = trimToNull(event.getValue());
            if (value != null && this.templates.has(value)) {
                currentTemplate = (ObjectNode) this.templates.get(value);
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
                parts.forEach(ruleFormPart -> {
                    ruleFormPart.clearValues();
                    ruleFormPart.setEnabled(false);
                });
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
            String id = generateTemplateId();
            ObjectNode template = JacksonUtils.createObjectNode();
            template.put(NAME, id);
            templates.set(id, template);
            updateTemplatesField();

            if (templateCreatedCallback != null) {
                String activeTemplateId = getActiveTemplateId().orElse(null);
                templateCreatedCallback.accept(new TemplateModificationDetails(id, activeTemplateId, template, event.isFromClient()));
            }

            templateSelectionField.setValue(id);
        });

        return createNewTemplate;
    }

    protected Button initCopySelectedTemplateButton() {
        Button copySelectedTemplate = new Button(VaadinIcon.COPY.create());
        copySelectedTemplate.setEnabled(false);
        copySelectedTemplate.addThemeVariants(ButtonVariant.LUMO_SMALL);
        copySelectedTemplate.setTooltipText(getI18nOrDefault(TemplatesI18n::getCopyTemplateButtonTooltip, "Copy template"));

        copySelectedTemplate.addClickListener(event -> {
            String currentName = currentTemplate.get(NAME).asText();
            String id = generateTemplateId();

            String i18nCopy = getI18nOrDefault(TemplatesI18n::getTemplateCopySuffix, "Copy");

            int copyIndex = currentName.indexOf("- " + i18nCopy);
            if (copyIndex < 0) {
                currentName += " - " + i18nCopy;
            } else {
                int numberedCopyIndex = currentName.indexOf("(", copyIndex);
                int start = 1;
                String firstPart;
                if (numberedCopyIndex >= 0) {
                    int endOf = currentName.indexOf(")", numberedCopyIndex);
                    String substring = currentName.substring(numberedCopyIndex, endOf);
                    firstPart = currentName.substring(0, numberedCopyIndex);
                    try {
                        start = Integer.parseInt(substring);
                    } catch (NumberFormatException numberFormatException) {
                    }
                } else {
                    firstPart = currentName + " ";
                }

                currentName = generateNumberedName(firstPart + "(", ")", start, collectExistingNames());
            }
            ObjectNode clonedTemplate = TemplateParser.clone(currentTemplate);
            clonedTemplate.put(NAME, currentName);
            templates.set(id, clonedTemplate);

            String originId = getActiveTemplateIdOrThrow();

            updateTemplatesField();

            if (templateCopiedCallback != null) {
                templateCopiedCallback.accept(new TemplateModificationDetails(id, originId, clonedTemplate, event.isFromClient()));
            }

            templateSelectionField.setValue(id);
        });

        return copySelectedTemplate;
    }

    private Set<String> collectExistingNames() {
        return collectExistingNames(true);
    }

    private Set<String> collectExistingNames(boolean withCurrentTemplate) {
        Stream<ObjectNode> stream = templates.propertyNames().stream()
                .map(key -> (ObjectNode) templates.get(key));

        if (!withCurrentTemplate && currentTemplate != null) {
            stream = stream.filter(o -> o != currentTemplate);
        }

        return stream
                .map(o -> o.get(NAME).asText())
                .collect(Collectors.toSet());
    }

    protected Button initDeleteSelectedTemplateButton() {
        Button button = new Button(VaadinIcon.TRASH.create());
        button.setEnabled(false);
        button.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR);
        button.setTooltipText(getI18nOrDefault(TemplatesI18n::getDeleteTemplateButtonTooltip, "Delete template"));

        button.addClickListener(event -> {
            new ConfirmDialog(
                    getI18nOrDefault(TemplatesI18n::getDeleteTemplateConfirmTitle, "Delete Template"),
                    getI18nOrDefault(TemplatesI18n::getDeleteTemplateConfirmText, "Shall the selected template be deleted? This process is irreversible."),
                    getI18nOrDefault(TemplatesI18n::getDeleteTemplateConfirmYesButton, "Delete"), confirmEvent -> {

                String id = getActiveTemplateIdOrThrow();
                ObjectNode deletedTemplate = (ObjectNode) templates.get(id);
                templates.remove(id);

                updateTemplatesField();

                if (templateDeletedCallback != null) {
                    templateDeletedCallback.accept(new TemplateModificationDetails(id, id, deletedTemplate, event.isFromClient()));
                }

                templateSelectionField.clear();
            }, getI18nOrDefault(TemplatesI18n::getDeleteTemplateConfirmNoButton, "Cancel"), cancelEvent -> {
            }).open();
        });

        return button;
    }

    private String generateTemplateId() {
        return generateNumberedName("template", "", 1, new HashSet<>(templates.propertyNames()));
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
        part.addValueChangeListener(event -> notifyTemplateUpdated(event.isFromClient()));
        container.getElement().appendChild(part.getElement());
        parts.add(part);
        return part;
    }

    private void notifyTemplateUpdated(boolean fromClient) {
        if (templateUpdatedCallback != null) {
            getActiveTemplateId().ifPresent(id -> {
                templateUpdatedCallback.accept(new TemplateModificationDetails(id, id, currentTemplate, fromClient));
            });
        }
    }

    public ObjectNode getTemplates() {
        ObjectNode clone = TemplateParser.clone(templates);
        TemplateParser.removeEmptyChildren(clone);
        return clone;
    }

    public void setTemplates(ObjectNode templates) {
        this.templates = TemplateParser.clone(templates);
        updateTemplatesField();
    }

    private void updateTemplatesField() {
        Collection<String> propertyNames = templates.propertyNames();
        List<String> keys = new ArrayList<>(propertyNames.size());
        for (String key : propertyNames) {
            if (!PATTERN_TEMPLATE_ID.matcher(key).matches()) {
                throw new IllegalArgumentException("Invalid template name: " + key);
            }
            keys.add(key);
        }

        String value = templateSelectionField.getValue();
        templateSelectionField.setItems(keys);
        templateSelectionField.setValue(value);
        templateSelectionField.setItemLabelGenerator(item -> {
            ObjectNode object = templates.has(item) ? (ObjectNode) this.templates.get(item) : null;
            return object != null ? object.get("name").asText() : ("#" + item);
        });
    }

    public void setSelectedRow(int row) {
        currentRowFormPart.setSelectedRow(row);
        if (currentTemplate != null) {
            currentRowFormPart.readTemplate(currentTemplate);
        }
    }

    public void setSelectedColumn(int col) {
        currentColFormPart.setSelectedColumn(col);
        if (currentTemplate != null) {
            currentColFormPart.readTemplate(currentTemplate);
        }
    }

    public void setCurrentPartsEnabled(boolean enabled) {
        boolean internalEnabled = enabled && currentTemplate != null;
        currentColFormPart.setEnabled(internalEnabled);
        currentRowFormPart.setEnabled(internalEnabled);
    }

    public void setActiveTemplateId(String templateId) {
        templateSelectionField.setValue(trimToNull(templateId));
    }

    public Optional<String> getActiveTemplateId() {
        return templateSelectionField.getOptionalValue();
    }

    public String getActiveTemplateIdOrThrow() {
        return getActiveTemplateId().orElseThrow(() ->
                new IllegalStateException("No active template found!"));
    }

    public void setTemplateSelectedCallback(SerializableBiConsumer<String, Boolean> callback) {
        this.templateSelectedCallback = callback;
    }

    public void setTemplateCreatedCallback(SerializableConsumer<TemplateModificationDetails> templateCreatedCallback) {
        this.templateCreatedCallback = templateCreatedCallback;
    }

    public void setTemplateCopiedCallback(SerializableConsumer<TemplateModificationDetails> templateCopiedCallback) {
        this.templateCopiedCallback = templateCopiedCallback;
    }

    public void setTemplateDeletedCallback(SerializableConsumer<TemplateModificationDetails> templateDeletedCallback) {
        this.templateDeletedCallback = templateDeletedCallback;
    }

    public void setTemplateUpdatedCallback(SerializableConsumer<TemplateModificationDetails> templateUpdatedCallback) {
        this.templateUpdatedCallback = templateUpdatedCallback;
    }

    public void updateRowIndexesOnAdd(boolean before) { updateIndexesOnAdd(ROWS, before); }
    public void updateColIndexesOnAdd(boolean before) { updateIndexesOnAdd(COLUMNS, before); }

    private void updateIndexesOnAdd(String key, boolean before) {
        if (currentTemplate != null) {
            int startingIndex = (ROWS.equals(key) ? currentRowFormPart.getSelectedRow() : currentColFormPart.getSelectedCol()) + 1;
            if (!before) { startingIndex++; }

            ArrayNode array = currentTemplate.has(key) ? (ArrayNode) currentTemplate.get(key) : null;
            if (array != null) {
                for (int i = 0; i < array.size(); i++) {
                    ObjectNode value = (ObjectNode) array.get(i);
                    String sIndex = value.get(INDEX).asText();
                    try {
                        int index = Integer.parseInt(sIndex);
                        if (index >= startingIndex) {
                            value.put(INDEX, String.valueOf(++index));
                        }
                    } catch (NumberFormatException nfe) { }
                }
                notifyTemplateUpdated(true);
            }
        }
    }

    public void updateRowIndexesOnRemove() { updateIndexesOnRemove(ROWS); }
    public void updateColIndexesOnRemove() { updateIndexesOnRemove(COLUMNS); }

    private void updateIndexesOnRemove(String key) {
        if (currentTemplate != null) {
            int startingIndex = (ROWS.equals(key) ? currentRowFormPart.getSelectedRow() : currentColFormPart.getSelectedCol()) + 1;

            ArrayNode array = currentTemplate.has(key) ? (ArrayNode) currentTemplate.get(key) : null;
            if (array != null) {
                for (int i = array.size() - 1; i >= 0 ; i--) {
                    ObjectNode value = (ObjectNode) array.get(i);
                    String sIndex = value.get(INDEX).asText();
                    try {
                        int index = Integer.parseInt(sIndex);
                        if (index == startingIndex) {
                            array.remove(i);
                        } else if (index > startingIndex) {
                            value.put(INDEX, String.valueOf(--index));
                        }
                    } catch (NumberFormatException nfe) { }
                }
                notifyTemplateUpdated(true);
            }
        }
    }

    public String getI18nOrDefault(ValueProvider<TemplatesI18n, String> valueProvider, String defaultValue) {
        String value = valueProvider.apply(this.i18n);
        return value != null ? value : defaultValue;
    }

    public HorizontalLayout getTemplateSection() { return templateSection; }
    public Details getTableSection() { return tableDetails; }
    public Details getCurrentRowSection() { return currentRowDetails; }
    public Details getCurrentColSection() { return currentColDetails; }
    public Details getSpecialRowsSection() { return specialRowsDetails; }
    public TableFormPart getTableFormPart() { return tableFormPart; }
    public FixedIndexRowFormPart getHeaderRowFormPart() { return headerRowFormPart; }
    public FixedIndexRowFormPart getFooterRowFormPart() { return footerRowFormPart; }
    public FixedIndexRowFormPart getEvenRowsFormPart() { return evenRowsFormPart; }
    public FixedIndexRowFormPart getOddRowsFormPart() { return oddRowsFormPart; }
    public CurrentRowFormPart getCurrentRowFormPart() { return currentRowFormPart; }
    public CurrentColFormPart getCurrentColFormPart() { return currentColFormPart; }
    public ComboBox<String> getTemplateSelectionField() { return templateSelectionField; }
    public TextField getTemplateNameField() { return templateNameField; }
    public Button getCreateNewTemplateButton() { return createNewTemplateButton; }
    public Button getCopySelectedTemplateButton() { return copySelectedTemplateButton; }
    public Button getDeleteSelectedTemplateButton() { return deleteSelectedTemplateButton; }
    public HorizontalLayout getTemplateButtonsContainer() { return templateButtonsContainer; }

    public Defaults getDefaults() { return defaults; }

    /**
     * Trims the given string and returns null if the result is empty or the input is null.
     * Inline replacement for StringUtils.trimToNull.
     */
    private static String trimToNull(String str) {
        if (str == null) return null;
        String trimmed = str.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public static final class TemplateModificationDetails {
        private final String id;
        private final String activeTemplateId;
        private final ObjectNode modifiedTemplate;
        private final boolean changedByClient;

        TemplateModificationDetails(String id, String activeTemplateId, ObjectNode modifiedTemplate, boolean changedByClient) {
            this.id = id;
            this.activeTemplateId = activeTemplateId;
            this.modifiedTemplate = modifiedTemplate;
            this.changedByClient = changedByClient;
        }

        public String getId() { return id; }
        public String getActiveTemplateId() { return activeTemplateId; }
        public ObjectNode getModifiedTemplate() { return modifiedTemplate; }
        public boolean isChangedByClient() { return changedByClient; }
    }

    public static class Defaults {
        public static String DIMENSION_UNIT = "rem";
        private final TemplateDialog dialog;
        private String dimensionUnit = DIMENSION_UNIT;

        @SuppressWarnings("rawtypes")
        private final Map<String, Set<Consumer>> changeListeners = new HashMap<>();

        Defaults(TemplateDialog dialog) { this.dialog = dialog; }

        public String getDimensionUnit() { return dimensionUnit; }

        public void setDimensionUnit(String dimensionUnit) {
            String old = this.dimensionUnit;
            this.dimensionUnit = Objects.requireNonNull(dimensionUnit);
            fireValueChangeEvent("dimensionUnit", old, dimensionUnit);
        }

        @SuppressWarnings({"rawtypes", "unchecked"})
        private void fireValueChangeEvent(String property, Object oldValue, Object newValue) {
            Set<Consumer> consumers = changeListeners.get(property);
            if (consumers != null && !consumers.isEmpty()) {
                DefaultValueChangeEvent event = new DefaultValueChangeEvent(dialog, oldValue, newValue);
                for (Consumer consumer : consumers) {
                    consumer.accept(event);
                }
            }
        }

        private Registration addListener(String property, Consumer<DefaultValueChangeEvent<?>> listener) {
            return Registration.addAndRemove(changeListeners.computeIfAbsent(property, aClass -> new LinkedHashSet<>()), listener);
        }

        @SuppressWarnings({"rawtypes", "unchecked"})
        public Registration addDimensionUnitChangedListener(Consumer<DefaultValueChangeEvent<String>> listener) {
            return addListener("dimensionUnit", (Consumer) listener);
        }
    }

    public static class DefaultValueChangeEvent<T> extends EventObject {
        private final T oldValue;
        private final T newValue;

        public DefaultValueChangeEvent(TemplateDialog templateDialog, T oldValue, T newValue) {
            super(templateDialog);
            this.oldValue = oldValue;
            this.newValue = newValue;
        }

        @Override
        public TemplateDialog getSource() { return (TemplateDialog) super.getSource(); }
        public T getNewValue() { return newValue; }
        public T getOldValue() { return oldValue; }
    }
}
