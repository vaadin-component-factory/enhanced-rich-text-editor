/**
 * ERTE Tables connector — V25 / Quill 2.
 *
 * Phase 4.1 stub: registers into extendQuill/extendEditor hooks.
 * Actual blot registration and table module come in Phase 4.2+.
 */
(function () {
  'use strict';

  const TAG = '[ERTE Tables]';

  // --- extendQuill hook: called BEFORE editor instantiation ---
  // Used for blot registration (Phase 4.2)
  if (!window.__erteExtendQuillHooks) {
    window.__erteExtendQuillHooks = [];
  }
  window.__erteExtendQuillHooks.push(function (Quill) {
    console.log(TAG, 'extendQuill hook registered (Quill available:', !!Quill, ')');
    // TODO Phase 4.2: Register table blots (TableBlot, TableRowBlot, TableCellBlot, ContainBlot)
    // TODO Phase 4.2: Register table module
  });

  // --- extendEditor hook: called AFTER editor instantiation ---
  // Used for toolbar integration, event wiring (Phase 4.3)
  if (!window.__erteExtendEditorHooks) {
    window.__erteExtendEditorHooks = [];
  }
  window.__erteExtendEditorHooks.push(function (editor, element) {
    console.log(TAG, 'extendEditor hook registered (editor available:', !!editor, ')');
    // TODO Phase 4.3: Initialize table selection, toolbar, history module
  });

  // --- Connector namespace for Java executeJs calls ---
  window.Vaadin = window.Vaadin || {};
  window.Vaadin.Flow = window.Vaadin.Flow || {};
  window.Vaadin.Flow.vcfEnhancedRichTextEditor =
    window.Vaadin.Flow.vcfEnhancedRichTextEditor || {};
  window.Vaadin.Flow.vcfEnhancedRichTextEditor.extensions =
    window.Vaadin.Flow.vcfEnhancedRichTextEditor.extensions || {};
  window.Vaadin.Flow.vcfEnhancedRichTextEditor.extensions.tables = {
    init: function (element) {
      console.log(TAG, 'Connector initialized for element:', element.tagName);
      // TODO Phase 4.3: Wire DOM event listeners (table-selected, table-cell-changed)
    },
    // Stubs for Java-side executeJs calls
    insert: function (element, rows, cols, templateId) {
      console.log(TAG, 'insert table', rows, 'x', cols, 'template:', templateId);
    },
    action: function (element, action) {
      console.log(TAG, 'table action:', action);
    },
    setTemplate: function (element, templateId) {
      console.log(TAG, 'set template:', templateId);
    },
    _setStyles: function (element, cssString) {
      console.log(TAG, 'set styles, length:', cssString?.length || 0);
    },
    _setCustomStyles: function (element, cssString, beforeGenerated) {
      console.log(TAG, 'set custom styles, before:', beforeGenerated);
    }
  };

  console.log(TAG, 'Connector loaded (Phase 4.1 stub)');
})();
