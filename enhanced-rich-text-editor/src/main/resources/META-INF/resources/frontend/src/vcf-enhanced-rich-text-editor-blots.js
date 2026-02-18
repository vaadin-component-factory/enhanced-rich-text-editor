const Quill = window.Quill;
const BlockEmbed = Quill.import('blots/block/embed');
const Block = Quill.import('blots/block');
const Inline = Quill.import('blots/inline');
const Embed = Quill.import('blots/embed');
const TextBlot = Quill.import('blots/text');
const ListItem = Quill.import('formats/list/item');
const ListContainer = Quill.import('formats/list');

/**
 * @class ReadOnlyBlot
 * @extends {Inline}
 */
class ReadOnlyBlot extends Inline {
  static create(value) {
    const node = super.create(value);
    if (value) {
      node.setAttribute('contenteditable', 'false');
    } else {
      node.removeAttribute('contenteditable');
    }
    return node;
  }

  static formats() {
    return true;
  }
}

ReadOnlyBlot.blotName = 'readonly';
ReadOnlyBlot.tagName = 'span';
ReadOnlyBlot.className = 'ql-readonly';
ReadOnlyBlot.allowedChildren = [Block, BlockEmbed, Inline, TextBlot, ListItem, ListContainer];

Quill.register(ReadOnlyBlot);

/**
 * Tab Blot (Embed) - New format using inline-block span with iterative width calculation.
 * Replaces old Inline-based TabBlot + PreTabBlot + TabsContBlot + LinePartBlot system.
 * @class TabBlot
 * @extends {Embed}
 */
class TabBlot extends Embed {
  static create(value) {
    const node = super.create();
    node.setAttribute('contenteditable', 'false');
    node.innerText = '\u200B'; // Zero-width space

    // Smart cursor placement on click
    const mouseHandler = (e) => {
      e.preventDefault();
      e.stopPropagation();

      // Multi-editor safe: find Quill instance via DOM traversal
      // Quill stores itself on .ql-container.__quill (not on .ql-editor)
      const containerEl = node.closest('.ql-container');
      if (!containerEl || !containerEl.__quill) return;
      const quill = containerEl.__quill;

      const blot = Quill.find(node);
      if (!blot) return;

      const index = quill.getIndex(blot);
      const rect = node.getBoundingClientRect();
      const clickX = e.clientX - rect.left;

      if (clickX < rect.width / 2) {
        quill.setSelection(index, 0, Quill.sources.USER);
      } else {
        quill.setSelection(index + 1, 0, Quill.sources.USER);
      }
    };

    node._mouseHandler = mouseHandler;
    node.addEventListener('mousedown', mouseHandler);

    return node;
  }

  detach() {
    if (this.domNode._mouseHandler) {
      this.domNode.removeEventListener('mousedown', this.domNode._mouseHandler);
      delete this.domNode._mouseHandler;
    }
    super.detach();
  }
}

TabBlot.blotName = 'tab';
TabBlot.tagName = 'span';
TabBlot.className = 'ql-tab';

Quill.register(TabBlot);

/**
 * Soft Break Blot (Embed) - Visual line break within a paragraph.
 * Inserted via Shift+Enter. Contains a <br> for visual line breaking.
 * @class SoftBreakBlot
 * @extends {Embed}
 */
class SoftBreakBlot extends Embed {
  static create(value) {
    const node = super.create(value);
    node.innerHTML = '<br>';
    return node;
  }
}

SoftBreakBlot.blotName = 'soft-break';
SoftBreakBlot.tagName = 'span';
SoftBreakBlot.className = 'ql-soft-break';

Quill.register(SoftBreakBlot);

/**
 * Non-breaking space
 * @class Nbsp
 * @extends {Embed}
 */
class Nbsp extends Embed {
  static create(value) {
    const node = super.create(value);
    node.innerHTML = '&nbsp;';
    return node;
  }
}

Nbsp.blotName = 'nbsp';
Nbsp.tagName = 'SPAN';
Nbsp.className = 'ql-nbsp';

Quill.register({ 'formats/nbsp': Nbsp });

/**
 * Placeholder token
 * @class PlaceholderBlot
 * @extends {Embed}
 */
class PlaceholderBlot extends Embed {
  constructor(node, value) {
    super(node, value);
    this.applyFormat();
  }

  applyFormat() {
    const node = this.domNode;
    const placeholder = PlaceholderBlot.loadValue(node);
    if (placeholder.altFormat) {
      const altTextNode = node.querySelector('[alt]');
      if (altTextNode) PlaceholderBlot.deltaToInline(altTextNode, placeholder.altFormat);
    }
    if (placeholder.format) {
      const contentNode = node.querySelector('[contenteditable="false"]');
      PlaceholderBlot.deltaToInline(contentNode, placeholder.format);
    }
  }

  static create(value) {
    const node = super.create();
    PlaceholderBlot.storeValue(node, value);
    PlaceholderBlot.setText(node);
    return node;
  }

  static value(node) {
    const placeholder = PlaceholderBlot.loadValue(node);
    return placeholder;
  }

  static loadValue(node) {
    return JSON.parse(node.dataset.placeholder) || true;
  }

  static storeValue(node, placeholder) {
    node.dataset.placeholder = JSON.stringify(placeholder);
  }

  static setText(node) {
    const placeholder = PlaceholderBlot.loadValue(node);
    let altText = '';
    let text = placeholder.text;
    if (PlaceholderBlot.altAppearanceRegex) {
      const match = new RegExp(PlaceholderBlot.altAppearanceRegex).exec(text);
      if(match) {
        altText = match[0];
        const altTextNodeStr = `<span alt>${altText}</span>`;
        const startIndex = match.index;
        const endIndex = startIndex + altText.length;
        if(placeholder.altAppearance) text = altTextNodeStr;
        else text = text.slice(0,startIndex) + altTextNodeStr + text.slice(endIndex);
      } else {
        if(placeholder.altAppearance) {
          text = '';
        }
      }
    }
    if (PlaceholderBlot.tags && !placeholder.altAppearance) text = PlaceholderBlot._wrapTags(text);
    node.innerHTML = text;
  }

  static deltaToInline(node, attr) {
    if (node) {
      Object.keys(attr).forEach(key => {
        const value = attr[key];
        switch (key) {
          case 'bold':
            node.style.fontWeight = value ? 'bold' : 'normal';
            break;
          case 'code':
            this._wrapContent('code', node);
            break;
          case 'font':
            node.style.fontFamily = value;
            break;
          case 'italic':
            node.style.fontStyle = value ? 'italic' : 'normal';
            break;
          case 'link':
            this._wrapContent('a', node, [{ name: 'href', value: value }]);
            break;
          case 'script':
            if (value === 'super') this._wrapContent('sup', node);
            else if (value === 'sub') this._wrapContent('sub', node);
            break;
          default:
            node.style[key] = value;
        }
      });
    }
  }

  static _wrapTags(text) {
    const { start, end } = PlaceholderBlot.tags;
    return start + text + end;
  }

  static _wrapContent(tag, node, attrs = []) {
    if (!node.querySelector(tag)) {
      const el = document.createElement(tag);
      el.innerHTML = node.innerHTML;
      node.innerHTML = '';
      node.appendChild(el);
      attrs.forEach(attr => el.setAttribute(attr.name, attr.value));
    }
  }
}

PlaceholderBlot.blotName = 'placeholder';
PlaceholderBlot.className = 'ql-placeholder';
PlaceholderBlot.tagName = 'SPAN';

Quill.register(PlaceholderBlot);

export { ReadOnlyBlot, TabBlot, SoftBreakBlot, PlaceholderBlot };
