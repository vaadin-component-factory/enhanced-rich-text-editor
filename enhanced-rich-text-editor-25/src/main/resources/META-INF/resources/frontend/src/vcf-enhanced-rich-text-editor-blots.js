/**
 * ERTE v25 - Custom Quill 2 Blots
 *
 * Ported from ERTE 1 (Quill 1.3.6) to Quill 2 (2.0.3).
 *
 * Quill 2 API changes applied:
 * - Embed constructor signature: (scroll, domNode, value) instead of (node, value)
 * - Blot registration via Quill.register() at module top level (confirmed in spike)
 * - Guard nodes (\uFEFF) are INSIDE embed elements in Quill 2, not outside
 * - Inline.order still accessible via Quill.import('blots/inline')
 * - Static class fields used where possible (Quill 2 supports them)
 */

const Quill = window.Quill;
const Inline = Quill.import('blots/inline');
const Embed = Quill.import('blots/embed');
const Block = Quill.import('blots/block');
const BlockEmbed = Quill.import('blots/block/embed');
const TextBlot = Quill.import('blots/text');

// ============================================================
// ReadOnlyBlot (Embed)
// ============================================================

/**
 * Read-only section blot.
 * Renders as a non-editable span via contenteditable="false".
 *
 * In Quill 2, the delta format {"insert":{"readonly":"text"}} requires an
 * Embed blot (Quill 1 was tolerant with Inline). The text value is stored
 * in a data-value attribute for HTML round-trip preservation.
 *
 * @class ReadOnlyBlot
 * @extends {Embed}
 */
class ReadOnlyBlot extends Embed {
  static blotName = 'readonly';
  static tagName = 'SPAN';
  static className = 'ql-readonly';

  static create(value) {
    const node = super.create(value);
    node.setAttribute('contenteditable', 'false');
    // Store value for round-trip (data-value survives sanitizer)
    const text = typeof value === 'string' ? value : '';
    node.setAttribute('data-value', text);
    // Set visible text -- Embed constructor will move it into contentNode
    node.textContent = text.replace(/\n$/, '');
    return node;
  }

  static value(domNode) {
    return domNode.getAttribute('data-value') || domNode.textContent || '';
  }
}

Quill.register(ReadOnlyBlot);

// ============================================================
// TabBlot (Embed)
// ============================================================

/**
 * Tab Blot (Embed) - Inline-block spacer with iterative width calculation.
 * Width is set dynamically by the tab-stop system in the main component.
 *
 * In Quill 2, the Embed base class wraps content in a contentNode span and
 * adds guard text nodes (\uFEFF) INSIDE the embed element. The outer .ql-tab
 * element is what gets measured and styled for tab width.
 *
 * @class TabBlot
 * @extends {Embed}
 */
class TabBlot extends Embed {
  static blotName = 'tab';
  static tagName = 'SPAN';
  static className = 'ql-tab';

  static create(value) {
    const node = super.create(value);
    node.setAttribute('contenteditable', 'false');

    // Smart cursor placement on click -- positions cursor before or after
    // the tab embed based on which half of the tab was clicked.
    const mouseHandler = (e) => {
      e.preventDefault();
      e.stopPropagation();

      // Multi-editor safe: find Quill instance via DOM traversal.
      // Quill 2 stores itself on .ql-container.__quill
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

  static formats(domNode) {
    return true;
  }

  static value(domNode) {
    return true;
  }

  detach() {
    if (this.domNode._mouseHandler) {
      this.domNode.removeEventListener('mousedown', this.domNode._mouseHandler);
      delete this.domNode._mouseHandler;
    }
    super.detach();
  }
}

Quill.register('formats/tab', TabBlot);

// ============================================================
// SoftBreakBlot (Embed)
// ============================================================

/**
 * Soft Break Blot (Embed) - Visual line break within a paragraph.
 * Inserted via Shift+Enter. Contains a <br> for visual line breaking.
 *
 * @class SoftBreakBlot
 * @extends {Embed}
 */
class SoftBreakBlot extends Embed {
  static blotName = 'soft-break';
  static tagName = 'SPAN';
  static className = 'ql-soft-break';

  static create(value) {
    const node = super.create(value);
    node.innerHTML = '<br>';
    return node;
  }
}

Quill.register(SoftBreakBlot);

// ============================================================
// Nbsp (Inline)
// ============================================================

/**
 * Non-breaking space blot.
 *
 * @class Nbsp
 * @extends {Inline}
 */
class Nbsp extends Inline {
  static blotName = 'nbsp';
  static tagName = 'SPAN';

  static create(value) {
    const node = super.create(value);
    node.innerHTML = '&nbsp;';
    return node;
  }
}

Quill.register({ 'formats/nbsp': Nbsp });

// ============================================================
// PlaceholderBlot (Embed)
// ============================================================

/**
 * Placeholder token blot.
 * Stores JSON metadata in data-placeholder attribute.
 * Supports alt appearance (regex-based) and inline formatting.
 *
 * Quill 2 constructor signature: (scroll, domNode, value)
 * vs Quill 1: (node, value)
 *
 * @class PlaceholderBlot
 * @extends {Embed}
 */
class PlaceholderBlot extends Embed {
  static blotName = 'placeholder';
  static tagName = 'SPAN';
  static className = 'ql-placeholder';

  /**
   * Quill 2 Embed constructor: (scroll, domNode, value)
   * The domNode is already created by static create() before the constructor runs.
   */
  constructor(scroll, domNode, value) {
    super(scroll, domNode, value);
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
    return PlaceholderBlot.loadValue(node);
  }

  static loadValue(node) {
    const raw = node.dataset.placeholder;
    if (!raw || raw === 'undefined') return { text: '' };
    try {
      return JSON.parse(raw) || { text: '' };
    } catch (e) {
      return { text: '' };
    }
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
      if (match) {
        altText = match[0];
        const altTextNodeStr = `<span alt>${altText}</span>`;
        const startIndex = match.index;
        const endIndex = startIndex + altText.length;
        if (placeholder.altAppearance) text = altTextNodeStr;
        else text = text.slice(0, startIndex) + altTextNodeStr + text.slice(endIndex);
      } else {
        if (placeholder.altAppearance) {
          text = '';
        }
      }
    }
    if (PlaceholderBlot.tags && !placeholder.altAppearance) text = PlaceholderBlot._wrapTags(text);
    node.innerHTML = text;
  }

  static deltaToInline(node, attr) {
    if (node) {
      Object.keys(attr).forEach((key) => {
        const value = attr[key];
        switch (key) {
          case 'bold':
            node.style.fontWeight = value ? 'bold' : 'normal';
            break;
          case 'code':
            PlaceholderBlot._wrapContent('code', node);
            break;
          case 'font':
            node.style.fontFamily = value;
            break;
          case 'italic':
            node.style.fontStyle = value ? 'italic' : 'normal';
            break;
          case 'link':
            PlaceholderBlot._wrapContent('a', node, [{ name: 'href', value: value }]);
            break;
          case 'script':
            if (value === 'super') PlaceholderBlot._wrapContent('sup', node);
            else if (value === 'sub') PlaceholderBlot._wrapContent('sub', node);
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
      attrs.forEach((attr) => el.setAttribute(attr.name, attr.value));
    }
  }
}

Quill.register(PlaceholderBlot);

export { ReadOnlyBlot, TabBlot, SoftBreakBlot, Nbsp, PlaceholderBlot };
