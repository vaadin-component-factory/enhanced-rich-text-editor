/**
 * ERTE Tables connector — V25 / Quill 2.
 * Phase 4.3a: Full connector with table operations, selection, history, CSS injection.
 */
import ContainBlot from './js/ContainBlot.js';
import Table from './js/TableBlot.js';
import TableRow, { setTableCellClass } from './js/TableRowBlot.js';
import TableCell from './js/TableCellBlot.js';
import { initTableModule } from './js/TableModule.js';
import TableTrick from './js/TableTrick.js';
import TableSelection from './js/TableSelection.js';
import TableHistory from './js/TableHistory.js';
import tableCss from './css/erte-table-styles.css?inline';

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

  // extendEditor: init table module + mouse events AFTER editor creation
  extNs.extendEditor.push(function(editor, Quill) {
    console.log(TAG, 'Initializing table module');
    initTableModule(editor, Quill);

    // Wire mouse events for cell selection
    const quill = editor;
    quill.container.addEventListener('mousedown', e => TableSelection.mouseDown(quill, e));
    quill.container.addEventListener('mousemove', e => TableSelection.mouseMove(quill, e));
    document.addEventListener('mouseup', e => TableSelection.mouseUp(quill, e));
    quill.on('selection-change', (range, oldRange) => TableSelection.selectionChange(quill, range, oldRange));
  });

  // Connector namespace for Java executeJs calls
  extNs.extensions = extNs.extensions || {};
  extNs.extensions.tables = {
    /**
     * Initialize tables extension — inject CSS into shadow root.
     * @param {HTMLElement} rte - The vcf-enhanced-rich-text-editor host element
     */
    init(rte) {
      const shadowRoot = rte.shadowRoot;
      if (!shadowRoot) {
        console.warn(TAG, 'No shadowRoot on', rte.tagName);
        return;
      }

      // Inject base CSS + 3 style slots for templates
      if (!shadowRoot.querySelector('#erte-table-base')) {
        const baseStyle = document.createElement('style');
        baseStyle.id = 'erte-table-base';
        baseStyle.textContent = tableCss;
        shadowRoot.append(
          baseStyle,
          this._createStyleElement('table-template-custom-styles-1'),
          this._createStyleElement('table-template-styles'),
          this._createStyleElement('table-template-custom-styles-2')
        );
      }
    },

    /**
     * Create empty style element with given ID.
     * @private
     */
    _createStyleElement(id) {
      const s = document.createElement('style');
      s.id = id;
      return s;
    },

    /**
     * Insert table at current cursor position.
     * @param {HTMLElement} rte - The editor host element
     * @param {string} rows - Row count as string
     * @param {string} cols - Column count as string
     * @param {string} template - Optional CSS class name for table template
     */
    insert(rte, rows, cols, template) {
      this._assureFocus(rte);
      const row_count = Number.parseInt(rows);
      const col_count = Number.parseInt(cols);
      TableTrick.insertTable(rte._editor, col_count, row_count, template);
      TableSelection.selectionChange(rte._editor);
    },

    /**
     * Execute table action (add-col, remove-row, merge-selection, etc.).
     * @param {HTMLElement} rte - The editor host element
     * @param {string} action - Action name from TableTrick.table_handler
     */
    action(rte, action) {
      this._assureFocus(rte);
      TableTrick.table_handler(action, rte._editor);
    },

    /**
     * Ensure editor has focus before operations.
     * @private
     */
    _assureFocus(rte) {
      if (!rte._editor.hasFocus()) {
        rte._editor.focus();
      }
    },

    /**
     * Set template CSS class on currently selected table.
     * @param {HTMLElement} rte - The editor host element
     * @param {string} template - CSS class name
     */
    setTemplate(rte, template) {
      const selectedTable = rte._editor.__selectedTable;
      if (!selectedTable) return;

      const classList = selectedTable.classList;
      if (classList) {
        // Clear existing classes
        classList.remove(...classList);
        // Add new template class
        if (template) {
          classList.add(template);
        }
      }

      // Trigger value change by detaching and reattaching first cell
      // (Quill doesn't detect class-only changes otherwise)
      const Quill = window.Quill;
      const tableBlot = Quill.find(selectedTable);
      if (tableBlot) {
        const firstRow = tableBlot.children?.head;
        const firstCell = firstRow?.children?.head;
        if (firstRow?.domNode && firstCell?.domNode) {
          const cNode = firstCell.domNode;
          cNode.remove();
          firstRow.domNode.prepend(cNode);
        }
      }
    },

    /**
     * Set generated table template styles (from server-side TemplateManager).
     * @param {HTMLElement} rte - The editor host element
     * @param {string} css - CSS string
     * @private
     */
    _setStyles(rte, css) {
      const s = rte.shadowRoot.querySelector('#table-template-styles');
      if (s) {
        s.textContent = css; // Security: textContent, NOT innerHTML
      }
    },

    /**
     * Set custom table template styles (before or after generated styles).
     * @param {HTMLElement} rte - The editor host element
     * @param {string} css - CSS string
     * @param {boolean} beforeGenerated - Insert before (true) or after (false) generated styles
     * @private
     */
    _setCustomStyles(rte, css, beforeGenerated) {
      const s = rte.shadowRoot.querySelector(`#table-template-custom-styles-${beforeGenerated ? '1' : '2'}`);
      if (s) {
        s.textContent = css; // Security: textContent, NOT innerHTML
      }
    }
  };

  console.log(TAG, 'Connector loaded (Phase 4.3a)');
})();
