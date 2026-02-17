// Quill 2 / Parchment 3 version of TableRowBlot
// Changes from V1:
// 1. Parchment.create() -> this.scroll.create()
// 3. defaultChild = TableCell (class reference, not string 'td')
// 4. checkMerge() override
// 5. while loop in optimize() merge logic
// No vendor import needed

import TableCell from './TableCellBlot.js';
import ContainBlot from './ContainBlot.js';
import TableTrick from './TableTrick.js';

const Quill = window.Quill;
const Parchment = Quill.import('parchment');

class TableRow extends ContainBlot {
  static create(value) {
    const tagName = 'tr';
    let node = super.create(tagName);
    node.setAttribute('row_id', value ? value : TableTrick.random_id());
    return node;
  }

  format() {}

  // Change 4: Parchment 3 checkMerge() override -- only merge rows with same row_id
  checkMerge() {
    return this.next != null &&
      this.next.statics.blotName === this.statics.blotName &&
      this.next.domNode.getAttribute('row_id') === this.domNode.getAttribute('row_id');
  }

  // we have no formats here, since the original addon is intended to build the table structure from TDs only

  optimize(context) {
    if (this.children.length === 0) {
      if (this.statics.defaultChild != null) {
        var child = this.createDefaultChild();
        this.appendChild(child);
        child.optimize(context);
      } else {
        this.remove();
      }
    }
    // Change 5: while loop for merging same row_id siblings
    let next = this.next;
    while (next != null && next.prev === this &&
      next.statics.blotName === this.statics.blotName &&
      next.domNode.tagName === this.domNode.tagName &&
      next.domNode.getAttribute('row_id') === this.domNode.getAttribute('row_id')
    ) {
      next.moveChildren(this);
      next.remove();
      next = this.next;
    }
  }

  insertBefore(childBlot, refBlot) {
    if (this.statics.allowedChildren != null && !this.statics.allowedChildren.some(function (child) {
      return childBlot instanceof child;
    })) {
      let newChild = this.createDefaultChild(refBlot);
      newChild.appendChild(childBlot);
      childBlot = newChild;
    }
    super.insertBefore(childBlot, refBlot);
  }

  // Change 2 not needed here (replace was calling super.replace which is fine)
  // But we keep the same pattern as V1 since TableRow.replace was just calling super

  createDefaultChild(refBlot) {
    let table_id = null;
    if (refBlot) {
      table_id = refBlot.domNode.getAttribute('table_id');
    } else if (this.parent) {
      table_id = this.parent.domNode.getAttribute('table_id');
    } else {
      table_id = this.domNode.parent.getAttribute('table_id');
    }

    // Change 1+3: this.scroll.create() with defaultChild.blotName
    return this.scroll.create(this.statics.defaultChild.blotName, [table_id, this.domNode.getAttribute('row_id'), TableTrick.random_id()].join('|'));
  }
}

TableRow.blotName = 'tr';
TableRow.tagName = 'tr';
TableRow.scope = Parchment.Scope.BLOCK_BLOT;
// Change 3: defaultChild = class reference
TableRow.defaultChild = TableCell;
TableRow.allowedChildren = [TableCell];

export default TableRow;
