/*-
 * #%L
 * Enhanced Rich Text Editor Tables Extension V25
 * %%
 * Copyright (C) 2025 Vaadin Ltd
 * %%
 * This program is available under Commercial Vaadin Add-On License 3.0
 * (CVALv3).
 *
 * See the file license.html distributed with this software for more
 * information about licensing.
 *
 * You should have received a copy of the CVALv3 along with this program.
 * If not, see <http://vaadin.com/license/cval-3>.
 * #L%
 */
package com.vaadin.componentfactory.erte.tables.templates;

import com.vaadin.componentfactory.erte.tables.TablesI18n.TemplatesI18n;
import com.vaadin.componentfactory.toolbar.ToolbarDialog;
import com.vaadin.componentfactory.toolbar.ToolbarSwitch;
import com.vaadin.flow.function.SerializableBiConsumer;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.function.ValueProvider;
import tools.jackson.databind.node.ObjectNode;
import jakarta.annotation.Nullable;

import java.util.EventObject;
import java.util.Optional;

public class TemplateDialog extends ToolbarDialog {

    private final TemplatesI18n i18n;
    // TODO Phase 4.3: form parts, binder, template state

    public TemplateDialog(ToolbarSwitch referencedSwitch, TemplatesI18n i18n) {
        super(referencedSwitch);
        this.i18n = i18n;
        // TODO Phase 4.3: build dialog UI
    }

    public ObjectNode getTemplates() {
        return null; // TODO Phase 4.3
    }

    public void setTemplates(ObjectNode templates) {
        // TODO Phase 4.3
    }

    public void setActiveTemplateId(@Nullable String templateId) {
        // TODO Phase 4.3
    }

    public Optional<String> getActiveTemplateId() {
        return Optional.empty(); // TODO Phase 4.3
    }

    public void setSelectedRow(int row) {
        // TODO Phase 4.3
    }

    public void setSelectedColumn(int col) {
        // TODO Phase 4.3
    }

    public void setCurrentPartsEnabled(boolean enabled) {
        // TODO Phase 4.3
    }

    public void setTemplateSelectedCallback(SerializableBiConsumer<String, Boolean> callback) {
        // TODO Phase 4.3
    }

    public void setTemplateCreatedCallback(SerializableConsumer<TemplateModificationDetails> callback) {
        // TODO Phase 4.3
    }

    public void setTemplateCopiedCallback(SerializableConsumer<TemplateModificationDetails> callback) {
        // TODO Phase 4.3
    }

    public void setTemplateDeletedCallback(SerializableConsumer<TemplateModificationDetails> callback) {
        // TODO Phase 4.3
    }

    public void setTemplateUpdatedCallback(SerializableConsumer<TemplateModificationDetails> callback) {
        // TODO Phase 4.3
    }

    public void updateRowIndexesOnAdd(boolean before) {
        // TODO Phase 4.3
    }

    public void updateColIndexesOnAdd(boolean before) {
        // TODO Phase 4.3
    }

    public void updateRowIndexesOnRemove() {
        // TODO Phase 4.3
    }

    public void updateColIndexesOnRemove() {
        // TODO Phase 4.3
    }

    public String getI18nOrDefault(ValueProvider<TemplatesI18n, String> valueProvider, String defaultValue) {
        String value = valueProvider.apply(this.i18n);
        return value != null ? value : defaultValue;
    }

    public Defaults getDefaults() {
        return new Defaults(this); // TODO Phase 4.3: make field
    }

    public static final class TemplateModificationDetails {
        private final String id;
        private final String activeTemplateId;
        private final ObjectNode modifiedTemplate;
        private final boolean changedByClient;

        public TemplateModificationDetails(String id, String activeTemplateId,
                                           ObjectNode modifiedTemplate, boolean changedByClient) {
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

        Defaults(TemplateDialog dialog) {
            this.dialog = dialog;
        }

        public String getDimensionUnit() { return dimensionUnit; }
        public void setDimensionUnit(String dimensionUnit) {
            this.dimensionUnit = java.util.Objects.requireNonNull(dimensionUnit);
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
        public TemplateDialog getSource() {
            return (TemplateDialog) super.getSource();
        }

        public T getNewValue() { return newValue; }
        public T getOldValue() { return oldValue; }
    }
}
