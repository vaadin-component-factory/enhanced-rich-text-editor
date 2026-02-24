/**
 * TableModule — functional module for ERTE tables.
 * V25 / Quill 2 version.
 * Called from extendEditor hook after editor creation.
 */
import { randomId, HIDDEN_BORDER_CLASS } from './utils.js';
import TableTrick from './TableTrick.js';
import TableSelection from './TableSelection.js';
import TableHistory from './TableHistory.js';

export function initTableModule(quill, Quill) {
  const TAG = '[TableModule]';
  console.log(TAG, 'Initializing table module');

  // Initialize quill.table state
  quill.table = {
    isInTable: false,
    tables: {}  // tableId -> table data (populated by TableSelection/TableHistory in Phase 4.3)
  };

  // Initialize history.tableStack for Phase 4.3
  quill.history.tableStack = {};

  // --- Clipboard Matchers ---
  const Delta = Quill.import('delta');
  const clipboard = quill.getModule('clipboard');

  // TABLE matcher — records metadata for optimize(), passes delta through
  clipboard.addMatcher('TABLE', function(node, delta) {
    const isPastedData = node.closest('.ql-editor') === null;
    const tableId = node.getAttribute('table_id');
    if (tableId) {
      quill.table.tables[tableId] = {
        pasted: isPastedData,
        row_counter: node.querySelectorAll('tr').length,
        cell_counter: node.querySelectorAll('td').length
      };
    }
    return delta;
  });

  // TR matcher — passes delta through
  clipboard.addMatcher('TR', function(node, delta) {
    return delta;
  });

  // TD/TH matcher — assigns IDs and applies td format to delta
  clipboard.addMatcher('TD, TH', function(node, delta) {
    // Ensure delta is not empty and ends with \n
    if (delta.length() === 0) {
      delta.ops = [{ insert: '\n' }];
    } else if (delta.ops && delta.ops.length) {
      const lastIndex = delta.ops.reduce(
        (acc, op, idx) => typeof op.insert !== 'undefined' ? idx : acc, -1
      );
      if (lastIndex >= 0 && typeof delta.ops[lastIndex].insert === 'string' &&
          !delta.ops[lastIndex].insert.endsWith('\n')) {
        delta.ops[lastIndex].insert += '\n';
      }
    }

    // Assign table_id from parent <table> if not set
    const tableNode = node.closest('table');
    if (!node.getAttribute('table_id') && tableNode) {
      if (!tableNode.getAttribute('table_id')) {
        tableNode.setAttribute('table_id', randomId());
      }
      node.setAttribute('table_id', tableNode.getAttribute('table_id'));
    }

    // Assign row_id from parent <tr> if not set
    if (!node.getAttribute('row_id')) {
      const rowNode = node.closest('tr');
      if (rowNode) {
        if (!rowNode.getAttribute('row_id')) {
          rowNode.setAttribute('row_id', randomId());
        }
        node.setAttribute('row_id', rowNode.getAttribute('row_id'));
      }
    }

    // Assign cell_id if not set
    if (!node.getAttribute('cell_id')) {
      node.setAttribute('cell_id', randomId());
    }

    // Compose td format attribute onto entire delta
    return delta.compose(new Delta().retain(delta.length(), {
      td: [
        node.getAttribute('table_id'),
        node.getAttribute('row_id'),
        node.getAttribute('cell_id'),
        node.getAttribute('merge_id') || '',
        node.getAttribute('colspan') || '',
        node.getAttribute('rowspan') || '',
        tableNode && tableNode.classList.contains(HIDDEN_BORDER_CLASS) ? HIDDEN_BORDER_CLASS : ''
      ].join('|')
    }));
  });

  // --- Keyboard Bindings ---
  const keyboard = quill.getModule('keyboard');

  // Tab/Shift+Tab navigation handlers
  function handleTab(range, context) {
    if (!context.format.td) return true; // NOT in table → pass to ERTE tabstop
    const [leaf] = quill.getLeaf(quill.getSelection().index);
    let blot;
    let unmergedCell;
    const td = leaf.parent.domNode.closest('td');
    if (!td) return true;

    if (td.nextSibling) {
      unmergedCell = td.nextSibling;
      while (unmergedCell && unmergedCell.getAttribute('merge_id')) {
        unmergedCell = unmergedCell.nextSibling;
      }
      blot = Quill.find(unmergedCell || td.closest('tr').nextSibling);
    } else {
      if (td.closest('tr').nextSibling) {
        blot = Quill.find(td.closest('tr').nextSibling);
      } else if (td.closest('table').nextSibling) {
        blot = Quill.find(td.closest('table').nextSibling);
      }
    }
    if (blot) {
      const selectionIndex = blot.offset(quill.scroll);
      quill.setSelection(selectionIndex, 0);
    }
    return false;
  }

  function handleShiftTab(range, context) {
    if (!context.format.td) return true; // NOT in table → pass through
    const [leaf] = quill.getLeaf(quill.getSelection().index);
    let blot;
    let unmergedCell;
    const td = leaf.parent.domNode.closest('td');
    if (!td) return true;

    if (td.previousSibling) {
      unmergedCell = td.previousSibling;
      while (unmergedCell && unmergedCell.getAttribute('merge_id')) {
        unmergedCell = unmergedCell.previousSibling;
      }
      blot = Quill.find(unmergedCell || td.closest('tr').previousSibling);
    } else {
      if (td.closest('tr').previousSibling) {
        const prevRow = td.closest('tr').previousSibling;
        const lastCell = prevRow.querySelector('td:last-child');
        blot = Quill.find(lastCell);
      } else if (td.closest('table').previousSibling) {
        blot = Quill.find(td.closest('table').previousSibling);
      }
    }
    if (blot) {
      const selectionIndex = blot.offset(quill.scroll);
      quill.setSelection(selectionIndex, 0);
    }
    return false;
  }

  // PREPEND Tab/Shift+Tab bindings to run before ERTE tabstop handler
  const existingTabBindings = keyboard.bindings['Tab'] ? [...keyboard.bindings['Tab']] : [];
  keyboard.bindings['Tab'] = [
    { key: 'Tab', handler: handleTab },
    { key: 'Tab', shiftKey: true, handler: handleShiftTab },
    ...existingTabBindings
  ];

  // Backspace — prevent backspace at cell border from removing table cells
  const existingBackspace = keyboard.bindings['Backspace'] ? [...keyboard.bindings['Backspace']] : [];
  keyboard.bindings['Backspace'] = [{
    key: 'Backspace',
    handler: function(range, context) {
      if (!context.format.td) return true;

      // If at cell border (offset 0, no selection)
      if (!context.offset && !range.length) {
        const selection = window.getSelection();
        const editor = quill.root;
        const nodeList = editor.querySelectorAll('p');
        const resultNodes = [...nodeList].filter(cell =>
          selection.containsNode(cell, true)
        );

        if (!resultNodes.length) return true;
        if (TableSelection.getSelectionCoords()) return false;

        let nodeRemoved = false;
        resultNodes.forEach(resultNode => {
          if (resultNode.previousSibling) {
            if (resultNode.previousSibling.nodeName === 'TABLE') {
              const cells = resultNode.previousSibling.querySelectorAll('td');
              if (cells.length && TableTrick._removeCell(cells[cells.length - 1])) {
                nodeRemoved = true;
              }
            }
          } else if (resultNode.parentNode.nodeName === 'TD') {
            if (TableTrick._removeCell(resultNode.parentNode)) {
              nodeRemoved = true;
            }
          }
        });

        if (nodeRemoved) TableHistory.add(quill);
        return !nodeRemoved;
      }
      return true;
    }
  }, ...existingBackspace];

  // Delete — prevent delete at end of cell from crossing cell boundary
  const existingDelete = keyboard.bindings['Delete'] ? [...keyboard.bindings['Delete']] : [];
  keyboard.bindings['Delete'] = [{
    key: 'Delete',
    handler: function(range, context) {
      if (!context.format.td) return true;
      // Check if cursor is NOT at end of text — allow standard delete
      const node = quill.selection.getNativeRange()?.start?.node;
      if (!node) return false;
      const blot = Quill.find(node);
      if (blot && context.offset < (blot.text ? blot.text.length : 0)) {
        return true;
      }
      // At end of cell text — prevent crossing cell boundary
      const [prev] = quill.getLine(range.index - 1);
      const [next] = quill.getLine(range.index + 1);
      if (next && next.prev) return true;
      return false;
    }
  }, ...existingDelete];

  // Ctrl+A — select all text within current cell only (not whole editor)
  const SHORTKEY = /Mac/i.test(navigator.platform) ? 'metaKey' : 'ctrlKey';
  const existingSelectAll = keyboard.bindings['a'] ? [...keyboard.bindings['a']] : [];
  keyboard.bindings['a'] = [{
    key: 'a',
    [SHORTKEY]: true,
    handler: function(range, context) {
      if (!context.format.td) return true;
      let [line] = quill.getLine(quill.getSelection().index);
      let nextBlot;
      if (line.parent.domNode.nodeName === "TD") {
        if (line.parent.domNode.nextSibling) {
          nextBlot = Quill.find(line.parent.domNode.nextSibling);
        } else if (line.parent.domNode.closest('tr').nextSibling) {
          nextBlot = Quill.find(line.parent.domNode.closest('tr').nextSibling);
        } else {
          nextBlot = Quill.find(line.parent.domNode.closest('table').nextSibling);
        }
        const blot = Quill.find(line.parent.domNode);
        const selectionIndex = blot.offset(quill.scroll);
        const nextBlotIndex = nextBlot.offset(quill.scroll);
        quill.setSelection(selectionIndex, nextBlotIndex - selectionIndex - 1);
        return false;
      }
      return true;
    }
  }, ...existingSelectAll];

  // Ctrl+Z / Ctrl+Shift+Z — undo/redo with table history support
  const existingUndo = keyboard.bindings['z'] ? [...keyboard.bindings['z']] : [];
  keyboard.bindings['z'] = [{
    key: 'z',
    [SHORTKEY]: true,
    handler: function(range, context) {
      return TableTrick.table_handler('undo', quill);
    }
  }, {
    key: 'z',
    [SHORTKEY]: true,
    shiftKey: true,
    handler: function(range, context) {
      return TableTrick.table_handler('redo', quill);
    }
  }, ...existingUndo];

  // Escape — clear cell selection
  const existingEscape = keyboard.bindings['Escape'] ? [...keyboard.bindings['Escape']] : [];
  keyboard.bindings['Escape'] = [{
    key: 'Escape',
    handler: function(range, context) {
      if (TableSelection.selectionStartElement || TableSelection.selectionEndElement) {
        TableSelection.resetSelection(quill.container);
        TableSelection.selectionStartElement = TableSelection.selectionEndElement = null;
        return false;
      }
      return true;
    }
  }, ...existingEscape];

  console.log(TAG, 'Table module initialized');
}
