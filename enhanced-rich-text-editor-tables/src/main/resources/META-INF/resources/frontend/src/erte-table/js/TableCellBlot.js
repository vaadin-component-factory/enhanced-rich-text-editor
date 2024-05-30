import '../../vendor/vaadin-quill.js';
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

  formats() {
    // We don't inherit from FormatBlot
    let className = "";
    const tr = this.domNode.parentNode;
    if (tr && tr.children[0] === this.domNode) {
      const table = tr.parentNode;
      if (table && table.children[0] === tr) {
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
        // we will mark td position, put in table and replace mark
        let mark = Parchment.create('block');
        this.parent.insertBefore(mark, this.next);
        let table = Parchment.create('table', this.domNode.getAttribute('table_id') + '|' + this.domNode.getAttribute('table-class'));
        this.domNode.removeAttribute('table-class'); //no longer need this once we set it in the table, since blots created from cell to table.
        let tr = Parchment.create('tr', this.domNode.getAttribute('row_id'));
        table.appendChild(tr);
        tr.appendChild(this);
        table.replace(mark);
      }
    }

    // merge same TD id
    let next = this.next;
    if (next != null && next.prev === this &&
      next.statics.blotName === this.statics.blotName &&
      next.domNode.tagName === this.domNode.tagName &&
      next.domNode.getAttribute('cell_id') === this.domNode.getAttribute('cell_id')
    ) {
      next.moveChildren(this);
      next.remove();
    }
  }

  insertBefore(childBlot, refBlot) {
    if (this.statics.allowedChildren != null && !this.statics.allowedChildren.some(function (child) {
      return childBlot instanceof child;
    })) {
      let newChild = Parchment.create(this.statics.defaultChild);
      newChild.appendChild(childBlot);
      childBlot = newChild;
    }
    super.insertBefore(childBlot, refBlot)
  }

  replace(target) {
    if (target.statics.blotName !== this.statics.blotName) {
      let item = Parchment.create(this.statics.defaultChild);
      target.moveChildren(item);
      this.appendChild(item);
    }
    if (target.parent == null) return;
    super.replace(target)
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