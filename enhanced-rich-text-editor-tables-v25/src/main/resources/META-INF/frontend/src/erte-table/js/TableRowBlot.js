/**
 * TableRowBlot — <tr> row blot for ERTE tables.
 * V25 / Parchment 3 version.
 *
 * Based on v25-old working implementation.
 * Key: optimize() does NOT call super.optimize() — manual empty-child
 * handling + merge loop only. This avoids Parchment's enforceAllowedChildren
 * and Container.checkMerge which can conflict with our custom merge logic.
 */
import ContainBlot from './ContainBlot.js';
import { randomId } from './utils.js';

const Quill = window.Quill;
const Block = Quill.import('blots/block');
const Parchment = Quill.import('parchment');

// Circular dependency resolution — set by connector after TableCell is loaded
let TableCell = null;
export function setTableCellClass(cls) {
  TableCell = cls;
  TableRow.allowedChildren = [cls];
  TableRow.defaultChild = cls;
}

class TableRow extends ContainBlot {
  static blotName = 'tr';
  static tagName = 'tr';
  static scope = Parchment.Scope.BLOCK_BLOT;
  static defaultChild = Block;  // Temporary, set to TableCell via setTableCellClass
  static allowedChildren = [Block];  // Temporary, set to [TableCell] via setTableCellClass

  static create(value) {
    const node = super.create();
    node.setAttribute('row_id', typeof value === 'string' ? value : randomId());
    return node;
  }

  format() {}

  // Parchment 3 requirement: only merge rows with same row_id
  checkMerge() {
    return this.next != null &&
      this.next.statics.blotName === this.statics.blotName &&
      this.next.domNode.getAttribute('row_id') === this.domNode.getAttribute('row_id');
  }

  optimize(context) {
    // Deliberately NOT calling super.optimize() — matches v25-old working pattern.
    // ParentBlot.optimize() would run enforceAllowedChildren() which can
    // conflict with our manual child management + merge logic.

    // 1. Ensure at least one child
    if (this.children.length === 0) {
      if (this.statics.defaultChild != null) {
        const child = this.createDefaultChild();
        this.appendChild(child);
        child.optimize(context);
      } else {
        this.remove();
      }
    }

    // 2. Merge same row_id siblings
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

  createDefaultChild(refBlot) {
    let tableId = null;
    if (refBlot) {
      tableId = refBlot.domNode.getAttribute('table_id');
    } else if (this.parent) {
      tableId = this.parent.domNode.getAttribute('table_id');
    } else {
      tableId = this.domNode.parentNode ? this.domNode.parentNode.getAttribute('table_id') : null;
    }
    return this.scroll.create(this.statics.defaultChild.blotName,
      [tableId, this.domNode.getAttribute('row_id'), randomId()].join('|'));
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

  moveChildren(targetParent, refBlot) {
    this.children.forEach(function (child) {
      targetParent.insertBefore(child, refBlot);
    });
  }
}

export default TableRow;
