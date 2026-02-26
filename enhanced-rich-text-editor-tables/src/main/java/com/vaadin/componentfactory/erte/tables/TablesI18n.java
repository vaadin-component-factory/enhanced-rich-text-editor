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
package com.vaadin.componentfactory.erte.tables;

import java.io.Serializable;
import java.util.Objects;

public class TablesI18n implements Serializable {

    private String insertTableRowsFieldLabel;
    private String insertTableColumnsFieldLabel;
    private String insertTableRowsFieldTooltip;
    private String insertTableColumnsFieldTooltip;
    private String insertTableAddButtonTooltip;
    private String insertTableToolbarSwitchTooltip;

    private String modifyTableToolbarSwitchTooltip;
    private String modifyTableAppendRowAboveItemLabel;
    private String modifyTableAppendRowBelowItemLabel;
    private String modifyTableRemoveRowItemLabel;

    private String modifyTableAppendColumnBeforeItemLabel;
    private String modifyTableAppendColumnAfterItemLabel;
    private String modifyTableRemoveColumnItemLabel;

    private String modifyTableRemoveTableItemLabel;

    private String modifyTableMergeCellsItemLabel;
    private String modifyTableSplitCellItemLabel;

    private String tableTemplatesToolbarSwitchTooltip;

    private TemplatesI18n templatesI18n = new TemplatesI18n();

    public String getInsertTableRowsFieldLabel() {
        return insertTableRowsFieldLabel;
    }

    public void setInsertTableRowsFieldLabel(String insertTableRowsFieldLabel) {
        this.insertTableRowsFieldLabel = Objects.requireNonNull(insertTableRowsFieldLabel);
    }

    public String getInsertTableColumnsFieldLabel() {
        return insertTableColumnsFieldLabel;
    }

    public void setInsertTableColumnsFieldLabel(String insertTableColumnsFieldLabel) {
        this.insertTableColumnsFieldLabel = Objects.requireNonNull(insertTableColumnsFieldLabel);
    }

    public String getInsertTableRowsFieldTooltip() {
        return insertTableRowsFieldTooltip;
    }

    public void setInsertTableRowsFieldTooltip(String insertTableRowsFieldTooltip) {
        this.insertTableRowsFieldTooltip = Objects.requireNonNull(insertTableRowsFieldTooltip);
    }

    public String getInsertTableColumnsFieldTooltip() {
        return insertTableColumnsFieldTooltip;
    }

    public void setInsertTableColumnsFieldTooltip(String insertTableColumnsFieldTooltip) {
        this.insertTableColumnsFieldTooltip = Objects.requireNonNull(insertTableColumnsFieldTooltip);
    }

    public String getInsertTableAddButtonTooltip() {
        return insertTableAddButtonTooltip;
    }

    public void setInsertTableAddButtonTooltip(String insertTableAddButtonTooltip) {
        this.insertTableAddButtonTooltip = Objects.requireNonNull(insertTableAddButtonTooltip);
    }

    public String getInsertTableToolbarSwitchTooltip() {
        return insertTableToolbarSwitchTooltip;
    }

    public void setInsertTableToolbarSwitchTooltip(String insertTableToolbarSwitchTooltip) {
        this.insertTableToolbarSwitchTooltip = Objects.requireNonNull(insertTableToolbarSwitchTooltip);
    }

    public String getModifyTableAppendRowAboveItemLabel() {
        return modifyTableAppendRowAboveItemLabel;
    }

    public void setModifyTableAppendRowAboveItemLabel(String modifyTableAppendRowAboveItemLabel) {
        this.modifyTableAppendRowAboveItemLabel = Objects.requireNonNull(modifyTableAppendRowAboveItemLabel);
    }

    public String getModifyTableAppendRowBelowItemLabel() {
        return modifyTableAppendRowBelowItemLabel;
    }

    public void setModifyTableAppendRowBelowItemLabel(String modifyTableAppendRowBelowItemLabel) {
        this.modifyTableAppendRowBelowItemLabel = Objects.requireNonNull(modifyTableAppendRowBelowItemLabel);
    }

    public String getModifyTableRemoveRowItemLabel() {
        return modifyTableRemoveRowItemLabel;
    }

    public void setModifyTableRemoveRowItemLabel(String modifyTableRemoveRowItemLabel) {
        this.modifyTableRemoveRowItemLabel = Objects.requireNonNull(modifyTableRemoveRowItemLabel);
    }

    public String getModifyTableAppendColumnBeforeItemLabel() {
        return modifyTableAppendColumnBeforeItemLabel;
    }

    public void setModifyTableAppendColumnBeforeItemLabel(String modifyTableAppendColumnBeforeItemLabel) {
        this.modifyTableAppendColumnBeforeItemLabel = Objects.requireNonNull(modifyTableAppendColumnBeforeItemLabel);
    }

    public String getModifyTableAppendColumnAfterItemLabel() {
        return modifyTableAppendColumnAfterItemLabel;
    }

    public void setModifyTableAppendColumnAfterItemLabel(String modifyTableAppendColumnAfterItemLabel) {
        this.modifyTableAppendColumnAfterItemLabel = Objects.requireNonNull(modifyTableAppendColumnAfterItemLabel);
    }

    public String getModifyTableRemoveColumnItemLabel() {
        return modifyTableRemoveColumnItemLabel;
    }

    public void setModifyTableRemoveColumnItemLabel(String modifyTableRemoveColumnItemLabel) {
        this.modifyTableRemoveColumnItemLabel = Objects.requireNonNull(modifyTableRemoveColumnItemLabel);
    }

    public String getModifyTableRemoveTableItemLabel() {
        return modifyTableRemoveTableItemLabel;
    }

    public void setModifyTableRemoveTableItemLabel(String modifyTableRemoveTableItemLabel) {
        this.modifyTableRemoveTableItemLabel = Objects.requireNonNull(modifyTableRemoveTableItemLabel);
    }

    public String getModifyTableMergeCellsItemLabel() {
        return modifyTableMergeCellsItemLabel;
    }

    public void setModifyTableMergeCellsItemLabel(String modifyTableMergeCellsItemLabel) {
        this.modifyTableMergeCellsItemLabel = Objects.requireNonNull(modifyTableMergeCellsItemLabel);
    }

    public String getModifyTableSplitCellItemLabel() {
        return modifyTableSplitCellItemLabel;
    }

    public void setModifyTableSplitCellItemLabel(String modifyTableSplitCellItemLabel) {
        this.modifyTableSplitCellItemLabel = Objects.requireNonNull(modifyTableSplitCellItemLabel);
    }

    public TemplatesI18n getTemplatesI18n() {
        return templatesI18n;
    }

    public void setTemplatesI18n(TemplatesI18n templatesI18n) {
        this.templatesI18n = Objects.requireNonNull(templatesI18n);
    }

    public String getModifyTableToolbarSwitchTooltip() {
        return modifyTableToolbarSwitchTooltip;
    }

    public void setModifyTableToolbarSwitchTooltip(String modifyTableToolbarSwitchTooltip) {
        this.modifyTableToolbarSwitchTooltip = modifyTableToolbarSwitchTooltip;
    }

    public String getTableTemplatesToolbarSwitchTooltip() {
        return tableTemplatesToolbarSwitchTooltip;
    }

    public void setTableTemplatesToolbarSwitchTooltip(String tableTemplatesToolbarSwitchTooltip) {
        this.tableTemplatesToolbarSwitchTooltip = tableTemplatesToolbarSwitchTooltip;
    }

    public static class TemplatesI18n {
        private String dialogTitle;
        private String tableSectionTitle;
        private String specialRowsSectionTitle;
        private String currentRowSectionTitle;
        private String currentColumnSectionTitle;

        private String specialRowsHeaderRowTitle;
        private String specialRowsFooterRowTitle;
        private String specialRowsEvenRowsTitle;
        private String specialRowsOddRowsTitle;

        private String currentTemplateSelectFieldLabel;
        private String currentTemplateNameFieldLabel;
        private String currentTemplateNameNotUniqueError;
        private String templateCopySuffix;

        private String createNewTemplateButtonTooltip;
        private String copyTemplateButtonTooltip;
        private String deleteTemplateButtonTooltip;
        private String deleteTemplateConfirmTitle;
        private String deleteTemplateConfirmText;
        private String deleteTemplateConfirmYesButton;
        private String deleteTemplateConfirmNoButton;

        private String formTableBorderFieldLabel;
        private String formTableCellsBorderFieldLabel;

        private String formBorderFieldLabel;
        private String formBorderFieldTooltip;

        private String formColorFieldTooltip;

        private String formTextColorFieldLabel;
        private String formBackgroundColorFieldLabel;

        private String formSizeFieldTooltip;
        private String formNumberFieldLessOrEqualZeroError;

        private String formWidthFieldLabel;
        private String formHeightFieldLabel;

        public String getDialogTitle() {
            return dialogTitle;
        }

        public void setDialogTitle(String dialogTitle) {
            this.dialogTitle = dialogTitle;
        }

        public String getTableSectionTitle() {
            return tableSectionTitle;
        }

        public void setTableSectionTitle(String tableSectionTitle) {
            this.tableSectionTitle = tableSectionTitle;
        }

        public String getSpecialRowsSectionTitle() {
            return specialRowsSectionTitle;
        }

        public void setSpecialRowsSectionTitle(String specialRowsSectionTitle) {
            this.specialRowsSectionTitle = specialRowsSectionTitle;
        }

        public String getCurrentRowSectionTitle() {
            return currentRowSectionTitle;
        }

        public void setCurrentRowSectionTitle(String currentRowSectionTitle) {
            this.currentRowSectionTitle = currentRowSectionTitle;
        }

        public String getCurrentColumnSectionTitle() {
            return currentColumnSectionTitle;
        }

        public void setCurrentColumnSectionTitle(String currentColumnSectionTitle) {
            this.currentColumnSectionTitle = currentColumnSectionTitle;
        }

        public String getSpecialRowsHeaderRowTitle() {
            return specialRowsHeaderRowTitle;
        }

        public void setSpecialRowsHeaderRowTitle(String specialRowsHeaderRowTitle) {
            this.specialRowsHeaderRowTitle = specialRowsHeaderRowTitle;
        }

        public String getSpecialRowsFooterRowTitle() {
            return specialRowsFooterRowTitle;
        }

        public void setSpecialRowsFooterRowTitle(String specialRowsFooterRowTitle) {
            this.specialRowsFooterRowTitle = specialRowsFooterRowTitle;
        }

        public String getSpecialRowsEvenRowsTitle() {
            return specialRowsEvenRowsTitle;
        }

        public void setSpecialRowsEvenRowsTitle(String specialRowsEvenRowsTitle) {
            this.specialRowsEvenRowsTitle = specialRowsEvenRowsTitle;
        }

        public String getSpecialRowsOddRowsTitle() {
            return specialRowsOddRowsTitle;
        }

        public void setSpecialRowsOddRowsTitle(String specialRowsOddRowsTitle) {
            this.specialRowsOddRowsTitle = specialRowsOddRowsTitle;
        }

        public String getCurrentTemplateSelectFieldLabel() {
            return currentTemplateSelectFieldLabel;
        }

        public void setCurrentTemplateSelectFieldLabel(String currentTemplateSelectFieldLabel) {
            this.currentTemplateSelectFieldLabel = currentTemplateSelectFieldLabel;
        }

        public String getCurrentTemplateNameFieldLabel() {
            return currentTemplateNameFieldLabel;
        }

        public void setCurrentTemplateNameFieldLabel(String currentTemplateNameFieldLabel) {
            this.currentTemplateNameFieldLabel = currentTemplateNameFieldLabel;
        }

        public String getCurrentTemplateNameNotUniqueError() {
            return currentTemplateNameNotUniqueError;
        }

        public void setCurrentTemplateNameNotUniqueError(String currentTemplateNameNotUniqueError) {
            this.currentTemplateNameNotUniqueError = currentTemplateNameNotUniqueError;
        }

        public String getTemplateCopySuffix() {
            return templateCopySuffix;
        }

        public void setTemplateCopySuffix(String templateCopySuffix) {
            this.templateCopySuffix = templateCopySuffix;
        }

        public String getCreateNewTemplateButtonTooltip() {
            return createNewTemplateButtonTooltip;
        }

        public void setCreateNewTemplateButtonTooltip(String createNewTemplateButtonTooltip) {
            this.createNewTemplateButtonTooltip = createNewTemplateButtonTooltip;
        }

        public String getCopyTemplateButtonTooltip() {
            return copyTemplateButtonTooltip;
        }

        public void setCopyTemplateButtonTooltip(String copyTemplateButtonTooltip) {
            this.copyTemplateButtonTooltip = copyTemplateButtonTooltip;
        }

        public String getDeleteTemplateButtonTooltip() {
            return deleteTemplateButtonTooltip;
        }

        public void setDeleteTemplateButtonTooltip(String deleteTemplateButtonTooltip) {
            this.deleteTemplateButtonTooltip = deleteTemplateButtonTooltip;
        }

        public String getDeleteTemplateConfirmTitle() {
            return deleteTemplateConfirmTitle;
        }

        public void setDeleteTemplateConfirmTitle(String deleteTemplateConfirmTitle) {
            this.deleteTemplateConfirmTitle = deleteTemplateConfirmTitle;
        }

        public String getDeleteTemplateConfirmText() {
            return deleteTemplateConfirmText;
        }

        public void setDeleteTemplateConfirmText(String deleteTemplateConfirmText) {
            this.deleteTemplateConfirmText = deleteTemplateConfirmText;
        }

        public String getDeleteTemplateConfirmYesButton() {
            return deleteTemplateConfirmYesButton;
        }

        public void setDeleteTemplateConfirmYesButton(String deleteTemplateConfirmYesButton) {
            this.deleteTemplateConfirmYesButton = deleteTemplateConfirmYesButton;
        }

        public String getDeleteTemplateConfirmNoButton() {
            return deleteTemplateConfirmNoButton;
        }

        public void setDeleteTemplateConfirmNoButton(String deleteTemplateConfirmNoButton) {
            this.deleteTemplateConfirmNoButton = deleteTemplateConfirmNoButton;
        }

        public String getFormTableBorderFieldLabel() {
            return formTableBorderFieldLabel;
        }

        public void setFormTableBorderFieldLabel(String formTableBorderFieldLabel) {
            this.formTableBorderFieldLabel = formTableBorderFieldLabel;
        }

        public String getFormTableCellsBorderFieldLabel() {
            return formTableCellsBorderFieldLabel;
        }

        public void setFormTableCellsBorderFieldLabel(String formTableCellsBorderFieldLabel) {
            this.formTableCellsBorderFieldLabel = formTableCellsBorderFieldLabel;
        }

        public String getFormBorderFieldLabel() {
            return formBorderFieldLabel;
        }

        public void setFormBorderFieldLabel(String formBorderFieldLabel) {
            this.formBorderFieldLabel = formBorderFieldLabel;
        }

        public String getFormBorderFieldTooltip() {
            return formBorderFieldTooltip;
        }

        public void setFormBorderFieldTooltip(String formBorderFieldTooltip) {
            this.formBorderFieldTooltip = formBorderFieldTooltip;
        }

        public String getFormColorFieldTooltip() {
            return formColorFieldTooltip;
        }

        public void setFormColorFieldTooltip(String formColorFieldTooltip) {
            this.formColorFieldTooltip = formColorFieldTooltip;
        }

        public String getFormTextColorFieldLabel() {
            return formTextColorFieldLabel;
        }

        public void setFormTextColorFieldLabel(String formTextColorFieldLabel) {
            this.formTextColorFieldLabel = formTextColorFieldLabel;
        }

        public String getFormBackgroundColorFieldLabel() {
            return formBackgroundColorFieldLabel;
        }

        public void setFormBackgroundColorFieldLabel(String formBackgroundColorFieldLabel) {
            this.formBackgroundColorFieldLabel = formBackgroundColorFieldLabel;
        }

        public String getFormSizeFieldTooltip() {
            return formSizeFieldTooltip;
        }

        public void setFormSizeFieldTooltip(String formSizeFieldTooltip) {
            this.formSizeFieldTooltip = formSizeFieldTooltip;
        }

        public String getFormNumberFieldLessOrEqualZeroError() {
            return formNumberFieldLessOrEqualZeroError;
        }

        public void setFormNumberFieldLessOrEqualZeroError(String formNumberFieldLessOrEqualZeroError) {
            this.formNumberFieldLessOrEqualZeroError = formNumberFieldLessOrEqualZeroError;
        }

        public String getFormWidthFieldLabel() {
            return formWidthFieldLabel;
        }

        public void setFormWidthFieldLabel(String formWidthFieldLabel) {
            this.formWidthFieldLabel = formWidthFieldLabel;
        }

        public String getFormHeightFieldLabel() {
            return formHeightFieldLabel;
        }

        public void setFormHeightFieldLabel(String formHeightFieldLabel) {
            this.formHeightFieldLabel = formHeightFieldLabel;
        }
    }
}
