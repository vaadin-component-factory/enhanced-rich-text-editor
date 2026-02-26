/**
 * TableCellBlot — <td> cell blot for ERTE tables.
 * V25 / Parchment 3 version.
 *
 * Based on v25-old working implementation.
 * Key differences from V24:
 * - Parchment.create() → this.scroll.create()
 * - replace() → replaceWith() (reversed semantics)
 * - defaultChild = Block (class reference, not string)
 * - checkMerge() override (Parchment 3 only checks blotName)
 * - While loop in merge logic (handles 4+ same-id siblings)
 */
import ContainBlot from './ContainBlot.js';
import { randomId } from './utils.js';

const Quill = window.Quill;
const Container = Quill.import('blots/container');
const Block = Quill.import('blots/block');
const BlockEmbed = Quill.import('blots/block/embed');
const Parchment = Quill.import('parchment');

class TableCell extends ContainBlot {
  static blotName = 'td';
  static tagName = 'td';
  static className = 'td-q';  // Distinguishes ERTE table cells from generic <td>
  static scope = Parchment.Scope.BLOCK_BLOT;
  static allowedChildren = [Block, BlockEmbed, Container];

  static create(value) {
    let node = super.create();
    if (typeof value !== 'string') {
      // Auto-wrapping by Parchment — assign random IDs
      node.setAttribute('table_id', randomId());
      node.setAttribute('row_id', randomId());
      node.setAttribute('cell_id', randomId());
      return node;
    }
    let atts = value.split('|');
    node.setAttribute('table_id', atts[0]);
    node.setAttribute('row_id', atts[1]);
    node.setAttribute('cell_id', atts[2]);
    if (atts[3]) node.setAttribute('merge_id', atts[3]);
    if (atts[4]) node.setAttribute('colspan', atts[4]);
    if (atts[5]) node.setAttribute('rowspan', atts[5]);
    if (atts[6]) node.setAttribute('table-class', atts[6]);
    return node;
  }

  format() {}

  // Parchment 3: only merge cells with same cell_id
  checkMerge() {
    return this.next != null &&
      this.next.statics.blotName === this.statics.blotName &&
      this.next.domNode.getAttribute('cell_id') === this.domNode.getAttribute('cell_id');
  }

  formats() {
    let className = '';
    const tr = this.domNode.parentNode;
    if (tr && tr.children[0] === this.domNode) {
      const table = tr.parentNode;
      if (table && table.querySelectorAll('tr')[0] === tr) {
        className = table.classList.toString();
      }
    }
    return {
      [this.statics.blotName]: [
        this.domNode.getAttribute('table_id') || '',
        this.domNode.getAttribute('row_id') || '',
        this.domNode.getAttribute('cell_id') || '',
        this.domNode.getAttribute('merge_id') || '',
        this.domNode.getAttribute('colspan') || '',
        this.domNode.getAttribute('rowspan') || '',
        className,
      ].join('|')
    };
  }

  optimize(context) {
    super.optimize(context);

    let parent = this.parent;
    if (parent != null) {
      if (parent.statics.blotName === 'td') {
        // Nested TD — flatten
        this.moveChildren(parent, this);
        this.remove();
        return;
      } else if (parent.statics.blotName !== 'tr') {
        // Orphan TD — wrap in table > tr structure
        const origParent = this.parent;
        const origNext = this.next;
        let table = this.scroll.create('table',
          this.domNode.getAttribute('table_id') + '|' +
          (this.domNode.getAttribute('table-class') || ''));
        this.domNode.removeAttribute('table-class');
        let tr = this.scroll.create('tr', this.domNode.getAttribute('row_id'));
        table.appendChild(tr);
        tr.appendChild(this);
        if (origNext && origNext.parent === origParent) {
          origParent.insertBefore(table, origNext);
        } else {
          origParent.appendChild(table);
        }
      }
    }

    // Merge same cell_id siblings — while loop with safety checks
    let next = this.next;
    while (next != null && next.prev === this &&
      next.statics.blotName === this.statics.blotName &&
      next.domNode.tagName === this.domNode.tagName &&
      next.domNode.getAttribute('cell_id') === this.domNode.getAttribute('cell_id')
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
      let newChild = this.scroll.create(this.statics.defaultChild.blotName);
      newChild.appendChild(childBlot);
      childBlot = newChild;
    }
    super.insertBefore(childBlot, refBlot);
  }

  replaceWith(replacement) {
    if (replacement.statics.blotName !== this.statics.blotName) {
      let item = replacement.scroll.create(replacement.statics.defaultChild.blotName);
      this.moveChildren(item);
      replacement.appendChild(item);
    }
    if (this.parent == null) return replacement;
    return super.replaceWith(replacement);
  }

  moveChildren(targetParent, refNode) {
    this.children.forEach(function (child) {
      targetParent.insertBefore(child, refNode);
    });
  }
}

export default TableCell;
