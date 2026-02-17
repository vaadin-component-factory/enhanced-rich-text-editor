// Quill 2 / Parchment 3 version of TableCellBlot
// Changes from V1:
// 1. Parchment.create() -> this.scroll.create() (no global factory in Parchment 3)
// 2. replace() -> replaceWith() (reversed semantics in Parchment 3)
// 3. defaultChild = Block (class reference via ContainBlot)
// 4. checkMerge() override (Parchment 3 default only checks blotName)
// 5. while loop in optimize() merge logic (handles 4+ same-id siblings)
// No vendor import needed (Quill 2 globally available)

import ContainBlot from './ContainBlot.js';

const Quill = window.Quill;
const Container = Quill.import('blots/container');
const Block = Quill.import('blots/block');
const BlockEmbed = Quill.import('blots/block/embed');
const Parchment = Quill.import('parchment');

class TableCell extends ContainBlot {
  static create(value) {
    let node = super.create();
    let atts = value.split('|');
    node.setAttribute('table_id', atts[0]);
    node.setAttribute('row_id', atts[1]);
    node.setAttribute('cell_id', atts[2]);
    if (atts[3]) {
      node.setAttribute('merge_id', atts[3]);
    }
    if (atts[4]) {
      node.setAttribute('colspan', atts[4]);
    }
    if (atts[5]) {
      node.setAttribute('rowspan', atts[5]);
    }
    if(atts[6]){
      node.setAttribute('table-class', atts[6]);
    }
    return node;
  }

  format() {}

  // Parchment 3: Container.checkMerge() only checks blotName, which would
  // merge ALL adjacent TDs regardless of cell_id. Override to check cell_id.
  checkMerge() {
    return this.next != null &&
      this.next.statics.blotName === this.statics.blotName &&
      this.next.domNode.getAttribute('cell_id') === this.domNode.getAttribute('cell_id');
  }

  formats() {
    // We don't inherit from FormatBlot
    let className = "";
    const tr = this.domNode.parentNode;
    if (tr && tr.children[0] === this.domNode) {
      const table = tr.parentNode;
      if (table && table.querySelectorAll("tr")[0] === tr) {
        className = table.classList.toString();
      }
    }

    return {
      [this.statics.blotName]:
        [
          this.domNode.getAttribute('table_id'),
          this.domNode.getAttribute('row_id'),
          this.domNode.getAttribute('cell_id'),
          this.domNode.getAttribute('merge_id'),
          this.domNode.getAttribute('colspan'),
          this.domNode.getAttribute('rowspan'),
          className,
        ].join('|')
    };
  }

  optimize(context) {
    super.optimize(context);

    let parent = this.parent;
    if (parent != null) {
      if (parent.statics.blotName === 'td') {
        this.moveChildren(parent, this);
        this.remove();
        return;
      } else if (parent.statics.blotName !== 'tr') {
        // Parchment 3 fix: avoid mark-based approach that breaks when
        // this.next is stale from concurrent optimize passes.
        // Instead: capture position, move self into table, then insert table.
        const origParent = this.parent;
        const origNext = this.next;
        // Change 1: Parchment.create() -> this.scroll.create()
        let table = this.scroll.create('table', this.domNode.getAttribute('table_id') + '|' + (this.domNode.getAttribute('table-class') || ""));
        this.domNode.removeAttribute('table-class');
        let tr = this.scroll.create('tr', this.domNode.getAttribute('row_id'));
        table.appendChild(tr);
        tr.appendChild(this); // moves this out of origParent
        // Insert table at the original position
        if (origNext && origNext.parent === origParent) {
          origParent.insertBefore(table, origNext);
        } else {
          origParent.appendChild(table);
        }
      }
    }

    // Change 5: merge same TD id -- while loop for 4+ same-id siblings
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
      // Change 1: Parchment.create() -> this.scroll.create()
      // Change 3: defaultChild is class ref, use .blotName
      let newChild = this.scroll.create(this.statics.defaultChild.blotName);
      newChild.appendChild(childBlot);
      childBlot = newChild;
    }
    super.insertBefore(childBlot, refBlot)
  }

  // Change 2: replace(target) -> replaceWith(replacement)
  // In Quill 1: newBlot.replace(oldBlot) -- "this" is the NEW blot
  // In Quill 2: oldBlot.replaceWith(newBlot) -- "this" is the OLD blot being replaced
  replaceWith(replacement) {
    if (replacement.statics.blotName !== this.statics.blotName) {
      // Change 1+3: scroll.create() with defaultChild.blotName
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

TableCell.blotName = 'td';
TableCell.tagName = 'td';
TableCell.className = 'td-q';
TableCell.scope = Parchment.Scope.BLOCK_BLOT;
TableCell.allowedChildren = [Block, BlockEmbed, Container];

export default TableCell;
