/**
 * TableHistory — Undo/redo tracking for ERTE Tables V25.
 * Ported from V24 with Parchment 3 adaptations.
 *
 * Key changes from V24:
 * - Import randomId from utils.js (not TableTrick.random_id())
 * - Parchment.find() → Quill.find()
 * - Parchment.create() removed — use DOM operations + quill.update() for insert/remove
 * - undo/redo pass quill to insert() for update() call
 * - split/merge keep innerHTML for now (security concern noted)
 */
import { randomId } from './utils.js';
import TableTrick from './TableTrick.js';

const Quill = window.Quill;

class TableHistory {
  static changes = [];

  // Register DOM change into current table history entry
  static register(type, change) {
    TableHistory.changes.push({ type, ...change });
  }

  // Add table history entry
  static add(quill) {
    if (!TableHistory.changes.length) return;

    const historyChangeStatus = quill.history.ignoreChange;
    // ignore history change and reset last recorded time for adding later changes in a new history entry
    quill.history.ignoreChange = true;
    quill.history.lastRecorded = 0;

    // wait history update
    setTimeout(() => {
      // reset history changes value
      quill.history.ignoreChange = historyChangeStatus;

      // add new entry in table stack
      const id = randomId();
      quill.history.tableStack[id] = TableHistory.changes;

      // set reference to table stack entry in a new history entry
      quill.history.stack.undo.push({ type: 'tableHistory', id: id });

      TableHistory.changes = [];
    }, 0);
  }

  static undo(quill, id) {
    const historyChangeStatus = quill.history.ignoreChange;
    quill.history.ignoreChange = true;

    const entry = quill.history.tableStack[id];
    if (typeof entry !== 'undefined') {
      // apply changes from last change to first change (undo)
      // Use slice() to avoid mutating the original array (needed for redo)
      const oldDelta = quill.getContents();
      [...entry].reverse().forEach(change => {
        switch (change.type) {
          case 'insert':
            // remove node (undo)
            TableHistory.remove(change);
            break;
          case 'remove':
            // add node (undo)
            TableHistory.insert(quill, change);
            break;
          case 'split':
            // merge cell (undo → re-merge)
            TableHistory.merge(change, true);
            break;
          case 'merge':
            // split cell (undo → re-split)
            TableHistory.split(change, true);
            break;
          case 'propertyChange':
            // property change (undo)
            TableHistory.propertyChange(change, true);
            break;
        }
      });
      // Sync blot tree with all DOM changes
      quill.update();
      // Emit text-change so UI (delta output, HTML) reflects the undo
      TableTrick.emitTextChange(quill, oldDelta);
    }

    // Move entry from undo to redo stack synchronously so consecutive
    // Ctrl+Z presses find the correct next entry immediately.
    const historyEntry = quill.history.stack.undo.pop();
    quill.history.stack.redo.push(historyEntry);
    // Reset ignoreChange asynchronously (after Quill's internal processing)
    setTimeout(() => {
      quill.history.ignoreChange = historyChangeStatus;
    }, 0);
  }

  static redo(quill, id) {
    const historyChangeStatus = quill.history.ignoreChange;
    quill.history.ignoreChange = true;

    const entry = quill.history.tableStack[id];
    if (typeof entry !== 'undefined') {
      const oldDelta = quill.getContents();
      // apply changes from first change to last change (redo)
      entry.forEach(change => {
        switch (change.type) {
          case 'insert':
            // add node (redo)
            TableHistory.insert(quill, change);
            break;
          case 'remove':
            // remove node (redo)
            TableHistory.remove(change);
            break;
          case 'split':
            // split cell (redo)
            TableHistory.split(change, false);
            break;
          case 'merge':
            // merge cell (redo)
            TableHistory.merge(change, false);
            break;
          case 'propertyChange':
            // property change (redo)
            TableHistory.propertyChange(change, false);
            break;
        }
      });
      // Sync blot tree with all DOM changes
      quill.update();
      // Emit text-change so UI (delta output, HTML) reflects the redo
      TableTrick.emitTextChange(quill, oldDelta);
    }

    // Move entry from redo to undo stack synchronously so consecutive
    // Ctrl+Y presses find the correct next entry immediately.
    const historyEntry = quill.history.stack.redo.pop();
    quill.history.stack.undo.push(historyEntry);
    // Reset ignoreChange asynchronously (after Quill's internal processing)
    setTimeout(() => {
      quill.history.ignoreChange = historyChangeStatus;
    }, 0);
  }

  static insert(quill, change) {
    // Parchment 3: Cannot use Parchment.create() on existing DOM node.
    // Use DOM insertBefore/appendChild + quill.update() to sync blot tree.
    const parentNode = change.parentNode || change.nextNode.parentNode;
    if (parentNode) {
      if (change.nextNode && change.nextNode.parentNode === parentNode) {
        parentNode.insertBefore(change.node, change.nextNode);
      } else {
        parentNode.appendChild(change.node);
      }

      // Force Quill to rebuild blot tree
      quill.update();

      // force re-rendering cells border (Firefox bug)
      const tableNode = change.node.nodeName === 'TABLE' ? change.node : parentNode.closest('table');
      if (tableNode) {
        tableNode.style.setProperty('overflow', (window.getComputedStyle(tableNode)['overflow'] || 'visible') === 'visible' ? 'hidden' : 'visible');
        setTimeout(() => {
          tableNode.style.removeProperty('overflow');
        }, 0);
      }

      return true;
    }
    return false;
  }

  static remove(change) {
    // Simple DOM remove — quill.update() will be called after all changes
    change.node.remove();
    return true;
  }

  static split(change, revert) {
    const td = change.node;
    // remove colspan and rowspan attributes
    td.removeAttribute('colspan');
    td.removeAttribute('rowspan');
    // for each merged node, remove merge_id attribute and restore content
    change.mergedNodes.forEach(cell => {
      cell.node.removeAttribute('merge_id');
      // TODO: Security concern - innerHTML assignment. Needs proper Quill delta strategy.
      cell.node.innerHTML = cell[revert ? 'oldContent' : 'newContent'];
    });
    // restore content
    // TODO: Security concern - innerHTML assignment
    td.innerHTML = change[revert ? 'oldContent' : 'newContent'];
    return true;
  }

  static merge(change, revert) {
    const td = change.node;
    const cell_id = td.getAttribute('cell_id');
    // set colspan and rowspan attributes
    td.setAttribute('colspan', change.colSpan);
    td.setAttribute('rowspan', change.rowSpan);
    // for each node to merge, set merge_id attribute and restore content
    change.mergedNodes.forEach(cell => {
      // TODO: Security concern - innerHTML assignment
      cell.node.innerHTML = cell[revert ? 'oldContent' : 'newContent'];
      cell.node.setAttribute('merge_id', cell_id);
    });
    // restore content
    // TODO: Security concern - innerHTML assignment
    td.innerHTML = change[revert ? 'oldContent' : 'newContent'];
    return true;
  }

  static propertyChange(change, revert) {
    const { node, property, oldValue, newValue } = change;
    const value = revert ? oldValue : newValue;
    if (value) {
      node.setAttribute(property, value);
    } else {
      node.removeAttribute(property);
    }
  }
}

export default TableHistory;
