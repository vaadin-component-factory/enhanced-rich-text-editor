/**
 * TableBlot — <table> blot for ERTE tables.
 * V25 / Parchment 3 version.
 *
 * Based on v25-old working implementation.
 * Key: merge loop has `next.prev === this` safety check + tagName check,
 * matching the proven v25-old pattern.
 */
import ContainBlot from './ContainBlot.js';
import TableRow from './TableRowBlot.js';
import { randomId, getQuill, HIDDEN_BORDER_CLASS } from './utils.js';

const Quill = window.Quill;
const Parchment = Quill.import('parchment');

class Table extends ContainBlot {
  static blotName = 'table';
  static tagName = 'table';
  static scope = Parchment.Scope.BLOCK_BLOT;
  static defaultChild = TableRow;
  static allowedChildren = [TableRow];

  static create(value) {
    const node = super.create();
    if (typeof value === 'string') {
      const atts = value.split('|');
      node.setAttribute('table_id', atts[0]);
      if (atts[1] && atts[1] !== 'null') {
        node.classList.add(atts[1]);
      }
    } else {
      // Auto-wrapping by Parchment (e.g., dangerouslySetHtmlValue) — assign random ID
      node.setAttribute('table_id', randomId());
    }
    return node;
  }

  format() {}

  // Parchment 3: only merge tables with same table_id
  checkMerge() {
    return this.next != null &&
      this.next.statics.blotName === this.statics.blotName &&
      this.next.domNode.getAttribute('table_id') === this.domNode.getAttribute('table_id');
  }

  optimize(context) {
    super.optimize(context);

    let quill = getQuill(this.domNode);
    if (!quill) return;

    const table_id = this.domNode.getAttribute('table_id');

    // 1. Merge adjacent tables with same table_id — while loop with safety checks
    let next = this.next;
    while (next != null && next.prev === this &&
      next.domNode.getAttribute('table_id') === table_id &&
      next.statics.blotName === this.statics.blotName &&
      next.domNode.tagName === this.domNode.tagName
    ) {
      next.moveChildren(this);
      next.remove();
      next = this.next;
    }

    // 2. After table merge, consolidate same-row_id TRs within this table.
    // Parchment 3's bottom-up optimize runs TDs → TRs → Tables, so by the time
    // tables merge, TRs have already been optimized and won't auto-re-merge.
    // We must do it here since the TRs are now siblings after the table merge.
    let tr = this.children.head;
    while (tr) {
      let nextTr = tr.next;
      while (nextTr != null && nextTr.prev === tr &&
        nextTr.statics.blotName === 'tr' &&
        nextTr.domNode.getAttribute('row_id') === tr.domNode.getAttribute('row_id')
      ) {
        nextTr.moveChildren(tr);
        const toRemove = nextTr;
        nextTr = nextTr.next;
        toRemove.remove();
      }
      tr = nextTr;
    }

    // 3. Colgroup creation (only when fully merged — no adjacent same-id tables)
    if (this.domNode.tagName === 'TABLE' && this.domNode.querySelectorAll('colgroup').length === 0) {
      const nextSib = this.next;
      const prevSib = this.prev;
      if ((nextSib == null || nextSib.domNode.getAttribute('table_id') !== table_id) &&
          (prevSib == null || prevSib.domNode.getAttribute('table_id') !== table_id)) {
        let maxCols = 0;
        this.domNode.querySelectorAll('tr').forEach(row => {
          maxCols = Math.max(maxCols, row.querySelectorAll('td').length);
        });
        const colgroup = document.createElement('colgroup');
        for (let i = 0; i < maxCols; i++) {
          colgroup.appendChild(document.createElement('col'));
        }
        this.domNode.prepend(colgroup);
      }
    }

    // 3. Table optimization when fully initialized
    if (
      quill.table && quill.table.tables &&
      typeof quill.table.tables[table_id] !== 'undefined' &&
      quill.table.tables[table_id].cell_counter === this.domNode.querySelectorAll('td').length &&
      quill.table.tables[table_id].row_counter === this.domNode.querySelectorAll('tr').length
    ) {
      // Add hidden merged cells
      this.domNode.querySelectorAll('td[cell_id][colspan], td[cell_id][rowspan]').forEach(cell => {
        const index = Array.prototype.indexOf.call(cell.parentNode.children, cell);
        const colSpan = Number.parseInt(cell.getAttribute('colspan') || 1);
        const rowSpan = Number.parseInt(cell.getAttribute('rowspan') || 1);

        if (!this.domNode.querySelector('td[merge_id="' + cell.getAttribute('cell_id') + '"]') &&
            (colSpan > 1 || rowSpan > 1)) {
          let row = cell.parentNode;
          for (let y = 1; y <= rowSpan; y++) {
            if (!row) break;
            let nextCell = y === 1 ? row.children[index + 1] : row.children[index];
            for (let x = 1; x <= colSpan; x++) {
              if (x === 1 && y === 1) continue;
              let newCell = document.createElement('td');
              newCell.setAttribute('cell_id', randomId());
              newCell.setAttribute('row_id', row.getAttribute('row_id'));
              newCell.setAttribute('table_id', this.domNode.getAttribute('table_id'));
              newCell.setAttribute('merge_id', cell.getAttribute('cell_id'));
              let p = document.createElement('p');
              p.appendChild(document.createElement('br'));
              newCell.appendChild(p);
              row.insertBefore(newCell, nextCell);
            }
            row = row.nextSibling;
          }
        }
      });

      // TODO Phase 4.3: TableHistory registration for pasted tables
      // if (quill.table.tables[table_id].pasted) { ... }

      // Delete entry — optimize only once
      delete quill.table.tables[table_id];
    }
  }

  insertBefore(childBlot, refBlot) {
    if (this.statics.allowedChildren != null && !this.statics.allowedChildren.some(function (child) {
      return childBlot instanceof child;
    })) {
      let newChild = this.scroll.create(this.statics.defaultChild.blotName, randomId());
      newChild.appendChild(childBlot);
      childBlot = newChild;
    }
    super.insertBefore(childBlot, refBlot);
  }
}

export default Table;
