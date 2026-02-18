// Quill 2 / Parchment 3 version of TableBlot
// Changes from V1:
// 1. Parchment.create() -> this.scroll.create()
// 3. defaultChild = TableRow (class reference, not string 'tr')
// 4. checkMerge() override
// 5. while loop in optimize() merge logic
// No vendor import needed

import TableTrick from './TableTrick.js';
import TableRow from './TableRowBlot.js';
import TableHistory from './TableHistory.js';
import ContainBlot from './ContainBlot.js';
import { hiddenBorderClassName } from './TableTrick.js';

const Quill = window.Quill;
const Parchment = Quill.import('parchment');

class Table extends ContainBlot {
  static create(value) {
    const tagName = 'table';
    let node = super.create(tagName);
    let atts = value.split('|');
    node.setAttribute('table_id', atts[0]);
    if (atts[1] && typeof atts[1] === "string" && atts[1] !== "null"){
      node.classList.add(atts[1]);
    }
    return node
  }

  // Change 4: Parchment 3 checkMerge() override -- only merge tables with same table_id
  checkMerge() {
    return this.next != null &&
      this.next.statics.blotName === this.statics.blotName &&
      this.next.domNode.getAttribute('table_id') === this.domNode.getAttribute('table_id');
  }

  // we have no formats here, since the original addon is intended to build the table structure from TDs only

  optimize(context) {
    super.optimize(context);
    let quill = TableTrick.getQuill(this.domNode);
    if (!quill) return;

    const table_id = this.domNode.getAttribute('table_id');

    // Change 5: while loop for merging same table_id siblings
    let next = this.next;
    while (next != null && next.prev === this &&
      next.domNode.getAttribute('table_id') === table_id &&
      next.statics.blotName === this.statics.blotName && next.domNode.tagName === this.domNode.tagName
    ) {
      // merge table containing single cell with table
      next.moveChildren(this);
      next.remove();
      next = this.next;
    }

    if (this.domNode.tagName === "TABLE" && this.domNode.querySelectorAll('colgroup').length === 0) {
      // Only add colgroup if we are the only table with this ID (fully merged)
      const nextSib = this.next;
      const prevSib = this.prev;
      if ((nextSib == null || nextSib.domNode.getAttribute('table_id') !== table_id) &&
          (prevSib == null || prevSib.domNode.getAttribute('table_id') !== table_id)) {
        let maxCols = 0;
        this.domNode.querySelectorAll('tr').forEach(row => {
          maxCols = Math.max(maxCols, row.querySelectorAll('td').length);
        });

        const colgroup = document.createElement("colgroup");
        for (let i = 0; i < maxCols; i++) {
          const col = document.createElement("col");
          colgroup.append(col);
        }

        this.domNode.prepend(colgroup);
      }
    }

    if (
      quill.table && quill.table.tables &&
      typeof quill.table.tables[table_id] !== 'undefined' &&
      quill.table.tables[table_id].cell_counter === this.domNode.querySelectorAll('td').length &&
      quill.table.tables[table_id].row_counter === this.domNode.querySelectorAll('tr').length
    ) {
      // our table is fully initialized, we can do more optimizations

      // add hidden merged cells
      this.domNode.querySelectorAll('td[cell_id][colspan], td[cell_id][rowspan]').forEach(cell => {
        const index = Array.prototype.indexOf.call(cell.parentNode.children, cell);
        const colSpan = Number.parseInt(cell.getAttribute('colspan') || 1);
        const rowSpan = Number.parseInt(cell.getAttribute('rowspan') || 1);

        if (!this.domNode.querySelector('td[merge_id="' + cell.getAttribute('cell_id') + '"]') && (colSpan > 1 || rowSpan > 1)) {
          let row = cell.parentNode;
          for (let y = 1; y <= rowSpan; y++) {
            if (!row) break;
            let nextCell = y === 1 ? row.children[index + 1] : row.children[index];
            for (let x = 1; x <= colSpan; x++) {
              if (x === 1 && y === 1) {
                continue;
              }
              let newCell = document.createElement('td');
              newCell.setAttribute('cell_id', TableTrick.random_id());
              newCell.setAttribute('row_id', row.getAttribute('row_id'));
              newCell.setAttribute('table_id', this.domNode.getAttribute('table_id'));
              newCell.setAttribute('merge_id', cell.getAttribute('cell_id'));
              let p = document.createElement('p');
              let br = document.createElement('br');
              p.appendChild(br);
              newCell.appendChild(p);
              row.insertBefore(newCell, nextCell);
            }
            row = row.nextSibling;
          }
        }
      });

      if (quill.table.tables[table_id].pasted) {
        // add to history
        TableHistory.register('insert', { node: this.domNode, nextNode: this.domNode.nextSibling, parentNode: this.domNode.parentNode });
        TableHistory.add(quill);
      }

      // delete entry for optimizing only once
      delete quill.table.tables[table_id];
    }
  }

  insertBefore(childBlot, refBlot) {
    if (this.statics.allowedChildren != null && !this.statics.allowedChildren.some(function (child) {
      return childBlot instanceof child;
    })) {
      // Change 1+3: this.scroll.create() with defaultChild.blotName
      let newChild = this.scroll.create(this.statics.defaultChild.blotName, TableTrick.random_id());
      newChild.appendChild(childBlot);
      childBlot = newChild;
    }
    super.insertBefore(childBlot, refBlot)
  }
}

Table.blotName = 'table';
Table.tagName = 'table';
Table.scope = Parchment.Scope.BLOCK_BLOT;
// Change 3: defaultChild = class reference
Table.defaultChild = TableRow;
Table.allowedChildren = [/*TableColGroupBlot, */TableRow];

export default Table;
