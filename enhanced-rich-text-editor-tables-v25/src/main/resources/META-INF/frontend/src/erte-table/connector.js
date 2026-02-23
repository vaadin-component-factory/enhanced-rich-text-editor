/**
 * ERTE Tables connector — V25 / Quill 2.
 * Phase 4.2: Blot registration + table module initialization.
 */
import ContainBlot from './js/ContainBlot.js';
import Table from './js/TableBlot.js';
import TableRow, { setTableCellClass } from './js/TableRowBlot.js';
import TableCell from './js/TableCellBlot.js';
import { initTableModule } from './js/TableModule.js';

(function() {
  'use strict';
  const TAG = '[ERTE Tables]';

  // Resolve circular dependency
  setTableCellClass(TableCell);

  // Defensive namespace + array initialization (connector may load before ERTE core)
  window.Vaadin = window.Vaadin || {};
  window.Vaadin.Flow = window.Vaadin.Flow || {};
  window.Vaadin.Flow.vcfEnhancedRichTextEditor =
    window.Vaadin.Flow.vcfEnhancedRichTextEditor || {};
  const extNs = window.Vaadin.Flow.vcfEnhancedRichTextEditor;
  extNs.extendQuill = extNs.extendQuill || [];
  extNs.extendEditor = extNs.extendEditor || [];

  // extendQuill: register blots BEFORE editor creation
  extNs.extendQuill.push(function(Quill) {
    if (Quill.__tablesRegistered) return;
    Quill.__tablesRegistered = true;
    console.log(TAG, 'Registering table blots');

    const Container = Quill.import('blots/container');
    Container.order = ['list', 'contain', 'td', 'tr', 'table'];

    Quill.register('formats/contain', ContainBlot, true);
    Quill.register('formats/td', TableCell, true);
    Quill.register('formats/tr', TableRow, true);
    Quill.register('formats/table', Table, true);
  });

  // extendEditor: init table module AFTER editor creation
  extNs.extendEditor.push(function(editor, Quill) {
    console.log(TAG, 'Initializing table module');
    initTableModule(editor, Quill);
  });

  // Connector namespace for Java executeJs calls (stubs for Phase 4.3)
  extNs.extensions = extNs.extensions || {};
  extNs.extensions.tables = {
    init: function(el) { console.log(TAG, 'Connector init:', el?.tagName); },
    insert: function(el, rows, cols, templateId) { console.log(TAG, 'Insert table', rows, 'x', cols); },
    action: function(el, action) { console.log(TAG, 'Table action:', action); },
    setTemplate: function(el, templateId) { console.log(TAG, 'Set template:', templateId); },
    _setStyles: function(el, css) { console.log(TAG, 'Set styles, length:', css?.length || 0); },
    _setCustomStyles: function(el, css, beforeGenerated) { console.log(TAG, 'Custom styles, before:', beforeGenerated); }
  };

  console.log(TAG, 'Connector loaded (Phase 4.2)');
})();
