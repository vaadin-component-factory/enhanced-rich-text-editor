// Quill 2 / Parchment 3 version of ContainBlot
// Changes from V1:
// 1. No vendor import (Quill 2 is globally available via RTE 2)
// 3. defaultChild = Block (class reference, not string)
// 6. formats() returns {} instead of tagName string (prevents character-level attribute spread)

const Quill = window.Quill;
const Container = Quill.import('blots/container');
const Block = Quill.import('blots/block');
const BlockEmbed = Quill.import('blots/block/embed');
const Parchment = Quill.import('parchment');

class ContainBlot extends Container {
  static create(value) {
    return super.create(value);
  }

  // Parchment 3 fix: returning a string like "TABLE" gets spread into
  // individual character attributes ("0":"T","1":"A",...). Container blots
  // don't contribute to delta formats -- only TableCell does.
  formats() {
    return {};
  }
}

ContainBlot.blotName = 'contain';
ContainBlot.tagName = 'contain';
ContainBlot.scope = Parchment.Scope.BLOCK_BLOT;
// Parchment 3: defaultChild must be a CLASS reference (not a string!)
// Parchment 3's ContainerBlot.optimize() accesses defaultChild.blotName
ContainBlot.defaultChild = Block;
ContainBlot.allowedChildren = [Block, BlockEmbed, Container];

export default ContainBlot;
