/**
 * ContainBlot — base container for table structures.
 * V25 / Parchment 3 version.
 *
 * Note: No optimize() override. Container.optimize() from Parchment 3
 * handles enforceAllowedChildren + checkMerge natively.
 * Table/Row/Cell blots override optimize() for their specific logic.
 */
const Quill = window.Quill;
const Container = Quill.import('blots/container');
const Block = Quill.import('blots/block');
const BlockEmbed = Quill.import('blots/block/embed');
const Parchment = Quill.import('parchment');

class ContainBlot extends Container {
  static blotName = 'contain';
  static tagName = 'contain';
  static scope = Parchment.Scope.BLOCK_BLOT;
  static defaultChild = Block;
  static allowedChildren = [Block, BlockEmbed, Container];

  // Parchment 3 fix: returning a string like "TABLE" gets spread into
  // individual character attributes ("0":"T","1":"A",...). Container blots
  // don't contribute to delta formats — only TableCell does.
  formats() {
    return {};
  }
}

export default ContainBlot;
