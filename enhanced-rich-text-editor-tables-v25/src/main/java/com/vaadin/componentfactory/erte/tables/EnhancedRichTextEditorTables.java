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
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.function.ValueProvider;
import com.vaadin.flow.shared.Registration;
import jakarta.annotation.Nullable;
import tools.jackson.databind.node.ObjectNode;

import java.util.Objects;
import java.util.Set;

@JsModule("./src/erte-table/connector.js")
public class EnhancedRichTextEditorTables {

    private final EnhancedRichTextEditor rte;
    private final TablesI18n i18n;

    protected EnhancedRichTextEditorTables(EnhancedRichTextEditor rte, TablesI18n i18n) {
        this.rte = Objects.requireNonNull(rte);
        this.i18n = Objects.requireNonNull(i18n);
        // TODO Phase 4.3: initConnector(), event listeners
    }

    public static EnhancedRichTextEditorTables enable(EnhancedRichTextEditor rte) {
        return enable(rte, new TablesI18n());
    }

    public static EnhancedRichTextEditorTables enable(EnhancedRichTextEditor rte, TablesI18n i18n) {
        EnhancedRichTextEditorTables tables = new EnhancedRichTextEditorTables(rte, i18n);
        // TODO Phase 4.3: tables.initToolbarTable()
        return tables;
    }

    public EnhancedRichTextEditor getRte() {
        return rte;
    }

    // TODO Phase 4.3: setTemplates, getTemplates, getAssignedTemplateIds, insertTableAtCurrentPosition,
    //  setTemplateIdForCurrentTable, executeTableAction, set*Color, addListener methods, initToolbarTable

    public String getI18nOrDefault(ValueProvider<TablesI18n, String> valueProvider, String defaultValue) {
        String value = valueProvider.apply(this.i18n);
        return value != null ? value : defaultValue;
    }

    private void fireEvent(EnhancedRichTextEditorTablesComponentEvent event) {
        ComponentUtil.fireEvent(rte, event);
    }

    private <T extends ComponentEvent<EnhancedRichTextEditor>> Registration addListener(
            Class<T> type, ComponentEventListener<T> listener) {
        return ComponentUtil.addListener(rte, type, listener);
    }
}
