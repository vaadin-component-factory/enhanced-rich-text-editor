// import '../../vendor/vaadin-quill.js';
// import ContainBlot from './ContainBlot.js';
// import TableTrick from "./TableTrick.js";
// import TableCol from "./TableColBlot.js";
//
// const Quill = window.Quill;
// const Container = Quill.import('blots/container');
// const Block = Quill.import('blots/block');
// const BlockEmbed = Quill.import('blots/block/embed');
// const Parchment = Quill.import('parchment');
//
// class TableColGroup extends ContainBlot {
//   static create(value) {
//     console.warn("create colgroup");
//
//     let node = super.create();
//     let atts = value.split('|');
//     node.setAttribute('table_id', atts[0]);
//     node.setAttribute('group_id', atts[1]);
//     return node;
//   }
//
//   format() {}
//
//   formats() {
//     console.warn("format colgroup");
//     // We don't inherit from FormatBlot
//     return {
//       [this.statics.blotName]:
//         [
//           this.domNode.getAttribute('table_id'),
//           this.domNode.getAttribute('group_id'),
//         ].join('|')
//     }
//   }
//
//   optimize(context) {
//     console.warn("optimize colgroup");
//     if (this.children.length === 0) {
//       if (this.statics.defaultChild != null) {
//         var child = this.createDefaultChild();
//         this.appendChild(child);
//         child.optimize(context);
//       } else {
//         this.remove();
//       }
//     }
//     let next = this.next;
//     if (next != null && next.prev === this &&
//         next.statics.blotName === this.statics.blotName &&
//         next.domNode.tagName === this.domNode.tagName &&
//         next.domNode.getAttribute('group_id') === this.domNode.getAttribute('group_id')
//     ) {
//       next.moveChildren(this);
//       next.remove();
//     }
//   }
//
//   insertBefore(childBlot, refBlot) {
//     console.warn("insert before colgroup");
//     if (this.statics.allowedChildren != null && !this.statics.allowedChildren.some(function (child) {
//       return childBlot instanceof child;
//     })) {
//       let newChild = this.createDefaultChild(refBlot);
//       newChild.appendChild(childBlot);
//       childBlot = newChild;
//     }
//     super.insertBefore(childBlot, refBlot);
//   }
//
//   replace(target) {
//     console.warn("replace colgroup");
//     if (target.statics.blotName !== this.statics.blotName) {
//       let item = this.createDefaultChild();
//       target.moveChildren(item, this);
//       this.appendChild(item);
//     }
//     super.replace(target);
//   }
//
//   createDefaultChild(refBlot) {
//     console.warn("create default child colgroup");
//     let table_id = null;
//     if (refBlot) {
//       table_id = refBlot.domNode.getAttribute('table_id');
//     } else if (this.parent) {
//       table_id = this.parent.domNode.getAttribute('table_id');
//     } else {
//       table_id = this.domNode.parent.getAttribute('table_id');
//     }
//
//     return Parchment.create(this.statics.defaultChild, [table_id, this.domNode.getAttribute('group_id'), TableTrick.random_id()].join('|'));
//   }
// }
//
// TableColGroup.blotName = 'colgroup';
// TableColGroup.tagName = 'colgroup';
// TableColGroup.scope = Parchment.Scope.BLOCK_BLOT;
// TableColGroup.defaultChild = 'col';
// TableColGroup.allowedChildren = [TableCol];
//
// export default TableColGroup;