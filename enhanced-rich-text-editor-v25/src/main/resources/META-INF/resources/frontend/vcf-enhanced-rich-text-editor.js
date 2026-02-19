/*-
 * #%L
 * Enhanced Rich Text Editor V25
 * %%
 * Copyright (C) 2019 - 2025 Vaadin Ltd
 * %%
 * This program is available under Commercial Vaadin Add-On License 3.0
 * (CVALv3).
 *
 * See the file license.html distributed with this software for more
 * information about licensing.
 *
 * You should have received a copy of the CVALv3 along with this program.
 * If not, see <http://vaadin.com/license/cval-3>.
 * #L%
 */

/**
 * ERTE V25 — extends RTE 2's web component with ERTE features.
 *
 * Import path: package entry point (@vaadin/rich-text-editor), NOT the
 * internal /src/ path. customElements.get() decouples from internal module
 * structure. Import path is stable as of Vaadin 25.0.5.
 */
import '@vaadin/rich-text-editor';
import { css } from 'lit';

const Quill = window.Quill;

const RteBase = customElements.get('vaadin-rich-text-editor');
if (!RteBase) {
  throw new Error(
    'vcf-enhanced-rich-text-editor: vaadin-rich-text-editor not registered. '
    + 'Ensure @vaadin/rich-text-editor is loaded first.'
  );
}

// ============================================================================
// Tab engine constants
// ============================================================================
const TAB_WRAP_DETECTION_MULTIPLIER = 0.8;
const TAB_DEFAULT_TAB_CHARS = 8;
const TAB_MIN_TAB_WIDTH = 2;
const TAB_FIXED_TAB_FALLBACK = 50;
const TAB_BLOCK_ELEMENTS = ['P', 'DIV', 'LI', 'H1', 'H2', 'H3', 'H4', 'H5', 'H6',
                            'BLOCKQUOTE', 'PRE', 'OL', 'UL', 'TABLE', 'TR', 'TD', 'TH'];
const TAB_BLOCK_SELECTOR = TAB_BLOCK_ELEMENTS.map(t => t.toLowerCase()).join(', ');

// ============================================================================
// Ruler background image (base64 PNG)
// ============================================================================
const RULER_HORI_BASE64 = 'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAV4AAAAPBAMAAABeoLrPAAAAA3NCSVQICAjb4U/gAAAAHlBMVEXS0tLR0dHQ0NCerLmfq7eeqrafqbOdqbWcqLT///9ePaWcAAAACnRSTlP///////////8AsswszwAAAAlwSFlzAAALEgAACxIB0t1+/AAAACB0RVh0U29mdHdhcmUATWFjcm9tZWRpYSBGaXJld29ya3MgTVi7kSokAAAAFnRFWHRDcmVhdGlvbiBUaW1lADA1LzEwLzEyhpCxGgAAAKtJREFUeJztksENgCAMRXt1BEZgICdwBvco3NxWqwYDFGMrajT2QOD/0v8kwvCugqcBhPXzXluf4XViA+uNKmfIeX09Q5Eh5y0+o9xQZFT8H24xINgXLwmMdtl4fVjcruYO9nEans6YeA2NMSQaEtedYzQMx0RLbkTzbHmeImPibWhrY8cy2to3IyRalM7P89ldVQZk39ksPZhpXJ9hUHfeDanlVAZ0ffumGgEWlrgeDxx/xAAAAABJRU5ErkJggg==';

// ============================================================================
// ReadOnlyBlot — Inline format: <span class="ql-readonly" contenteditable="false">
// Registered globally before element creation (proven pattern, used by RTE 2).
// ============================================================================
const Inline = Quill.import('blots/inline');

class ReadOnlyBlot extends Inline {
  static blotName = 'readonly';
  static className = 'ql-readonly';

  static create(value) {
    const node = super.create(value);
    if (value) {
      node.setAttribute('contenteditable', 'false');
      node.setAttribute('aria-readonly', 'true');
    }
    return node;
  }

  static formats(domNode) {
    return domNode.classList.contains('ql-readonly');
  }
}

Quill.register('formats/readonly', ReadOnlyBlot, true);

// ============================================================================
// TabBlot — Embed: <span class="ql-tab" contenteditable="false">
// Inline-block span with iterative width calculation.
// ============================================================================
const Embed = Quill.import('blots/embed');

class TabBlot extends Embed {
  static blotName = 'tab';
  static tagName = 'span';
  static className = 'ql-tab';

  static create(value) {
    const node = super.create();
    // NOTE: Do NOT set contenteditable="false" on the outer node.
    // Quill 2 Embed constructor places guard nodes (zero-width text nodes)
    // INSIDE the domNode. They must remain editable for proper cursor
    // placement before/after the embed. The inner contentNode already
    // has contenteditable="false".

    // Smart cursor placement on click: measures click position
    // on left/right half of tab width to place cursor before or after.
    const mouseHandler = (e) => {
      e.preventDefault();
      e.stopPropagation();

      // Multi-editor safe: find Quill instance via DOM traversal
      // Use Quill.find() NOT containerEl.__quill (Spike Item 20)
      const containerEl = node.closest('.ql-container');
      if (!containerEl) return;
      const quill = Quill.find(containerEl);
      if (!quill) return;

      const blot = Quill.find(node);
      if (!blot) return;

      const index = quill.getIndex(blot);
      // CRITICAL (Spike Item 20): Measure OUTER .ql-tab rect, NOT contentNode.
      // Guard nodes in Quill 2 are INSIDE the embed element.
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

Quill.register('formats/tab', TabBlot, true);

// ============================================================================
// SoftBreakBlot — Embed: <span class="ql-soft-break"><br></span>
// Visual line break within a paragraph, inserted via Shift+Enter.
// ============================================================================
class SoftBreakBlot extends Embed {
  static blotName = 'soft-break';
  static tagName = 'span';
  static className = 'ql-soft-break';

  static create(value) {
    const node = super.create(value);
    // NOTE: No contenteditable="false" on outer node — see TabBlot comment.
    // Use createElement (NOT innerHTML) per security rules
    const br = document.createElement('br');
    node.appendChild(br);
    return node;
  }
}

Quill.register('formats/soft-break', SoftBreakBlot, true);

/**
 * ERTE CSS classes to preserve in htmlValue (not stripped by __updateHtmlValue).
 * Each phase adds its classes here.
 */
const ERTE_PRESERVED_CLASSES = ['ql-readonly', 'ql-tab', 'ql-soft-break'];

class VcfEnhancedRichTextEditor extends RteBase {

  static get is() {
    return 'vcf-enhanced-rich-text-editor';
  }

  static get properties() {
    return {
      ...super.properties,
      tabStops: { type: Array },
      noRulers: { type: Boolean, reflect: true },
    };
  }

  static get styles() {
    const base = super.styles ? [...super.styles] : [];
    return [
      ...base,
      css`
        /* Readonly sections — Lumo design tokens for light/dark compatibility */
        .ql-readonly {
          color: var(--lumo-secondary-text-color);
          background-color: var(--lumo-contrast-5pct);
          border-radius: var(--lumo-border-radius-s);
          padding-inline: 0.125em;
          outline: 1px solid var(--lumo-contrast-10pct);
          outline-offset: -1px;
        }

        /* Tab stops — inline-flex spans with calculated width.
           Quill 2 Embed structure: [guard \uFEFF] [contentNode span] [guard \uFEFF]
           Guard nodes are zero-width text nodes for cursor placement.
           Using inline-flex so the contentNode (flex:1) pushes the trailing
           guard node to the RIGHT edge — otherwise both guards render at
           left:0 inside the inline-block and the "after tab" cursor appears
           at the same position as the "before tab" cursor.
           overflow:clip (not hidden) — hidden creates a BFC that isolates
           the caret, preventing cursor placement in guard nodes. */
        .ql-tab {
          display: inline-flex;
          min-width: 2px;
          height: 1rem;
          white-space: pre;
          vertical-align: baseline;
          position: relative;
          cursor: default;
          line-height: 1rem;
          overflow: clip;
          will-change: width;
          transform: translateZ(0);
        }
        .ql-tab > span {
          flex: 1;
          font-size: 0;
        }

        /* Soft breaks — inline line break */
        .ql-soft-break {
          display: inline;
        }
      `,
    ];
  }

  static get lumoInjector() {
    // Reuse the parent tag name so that the LumoInjector injects the same
    // Lumo theme styles (toolbar icons, colors, spacing) as vaadin-rich-text-editor.
    // Without this, the injector looks for --_lumo-vcf-enhanced-rich-text-editor-inject
    // which doesn't exist in any Lumo CSS file, leaving ERTE with base SVG icons
    // instead of the Lumo text-based/font icons.
    return { ...super.lumoInjector, is: 'vaadin-rich-text-editor' };
  }

  /** @protected */
  render() {
    return super.render();
  }

  /**
   * Vaadin-specific lifecycle hook (inherited from Polymer compat layer,
   * NOT a standard Lit lifecycle method). Called from within the Lit update
   * cycle after connectedCallback -> willUpdate -> firstUpdated -> updated.
   * _editor (Quill instance) is available immediately after super.ready().
   * See SPIKE_RESULTS.md Item 14 for full lifecycle timeline.
   * @protected
   */
  ready() {
    super.ready();
    this._injectToolbarSlots();
    this._injectReadonlyButton();
    this._initReadonlyProtection();

    // Tab engine initialization
    this._tabStopsArray = [];
    this._textWidthCache = new Map();
    this._tabUpdateRafId = null;
    this._createMeasureSpan();

    // Patch keyboard bindings (Tab, Shift+Enter, Shift+Tab)
    this._patchKeyboard();

    // Inject ruler DOM (must be before property observers so initial tabStops can render markers)
    this._injectRuler();

    // Property observers for tabStops and noRulers
    this._createPropertyObserver('tabStops', '_onTabStopsChanged');
    this._createPropertyObserver('noRulers', '_onNoRulersChanged');

    // If tabStops was set before ready(), trigger the observer now
    if (this.tabStops) {
      this._onTabStopsChanged(this.tabStops);
    }

    // Recalculate tab widths on every text change
    this._editor.on('text-change', () => this._requestTabUpdate());

    // Recalculate tab widths on editor resize
    new ResizeObserver(() => this._requestTabUpdate()).observe(this._editor.root);

    console.debug('[ERTE] ready, _editor:', !!this._editor, 'readonly protection active, tab engine initialized');
  }

  /**
   * Injects 25 named <slot> elements into the toolbar DOM produced by
   * super.render(). Slots are placed: START before first group, BEFORE/AFTER
   * around each of the 11 standard groups, a custom group span with
   * BEFORE_CUSTOM/GROUP_CUSTOM/AFTER_CUSTOM, and END at the end.
   *
   * Proven: injected DOM nodes survive all Lit re-renders (i18n, readonly,
   * requestUpdate) because Lit's template diffing ignores nodes inserted
   * between its comment marker boundaries.
   * @protected
   */
  _injectToolbarSlots() {
    const toolbar = this.shadowRoot.querySelector('[part="toolbar"]');
    if (!toolbar) return;

    // Group part names in toolbar order (matches RTE 2 V25.0.x)
    const groupNames = [
      'history', 'emphasis', 'style', 'heading',
      'glyph-transformation', 'list', 'indent',
      'alignment', 'rich-text', 'block', 'format',
    ];

    const _slot = (name) => {
      const s = document.createElement('slot');
      s.setAttribute('name', `toolbar-${name}`);
      return s;
    };

    // Discover groups by part attribute
    const groups = groupNames.map((name) =>
      toolbar.querySelector(`[part~="toolbar-group-${name}"]`)
    ).filter(Boolean);

    // START slot — before the first group
    if (groups.length > 0) {
      toolbar.insertBefore(_slot('start'), groups[0]);
    }

    // BEFORE / AFTER each group
    for (const group of groups) {
      const name = group.getAttribute('part')
        .split(/\s+/)
        .find((p) => p.startsWith('toolbar-group-'))
        ?.replace('toolbar-group-', '');
      if (!name) continue;
      group.parentNode.insertBefore(_slot(`before-group-${name}`), group);
      group.after(_slot(`after-group-${name}`));
    }

    // Custom group: BEFORE_CUSTOM, group span with GROUP_CUSTOM slot, AFTER_CUSTOM, END
    // Insert before the #fileInput (hidden file input at end of toolbar)
    const fileInput = toolbar.querySelector('#fileInput');
    const customGroupSpan = document.createElement('span');
    customGroupSpan.setAttribute('part', 'toolbar-group toolbar-group-custom');
    // GROUP_CUSTOM slot has name="toolbar" (legacy compatibility)
    const customSlot = document.createElement('slot');
    customSlot.setAttribute('name', 'toolbar');
    customGroupSpan.appendChild(customSlot);

    const anchor = fileInput || null;
    toolbar.insertBefore(_slot('before-group-custom'), anchor);
    toolbar.insertBefore(customGroupSpan, anchor);
    toolbar.insertBefore(_slot('after-group-custom'), anchor);
    toolbar.insertBefore(_slot('end'), anchor);
  }

  // ==========================================================================
  // Readonly: toolbar button
  // ==========================================================================

  /**
   * Injects a readonly toggle button into the toolbar, placed in the
   * format group (last standard group). Lock icon via vaadin-icon.
   * @protected
   */
  _injectReadonlyButton() {
    const toolbar = this.shadowRoot.querySelector('[part="toolbar"]');
    if (!toolbar) return;

    const btn = document.createElement('button');
    btn.type = 'button';
    btn.setAttribute('part', 'toolbar-button toolbar-button-readonly');
    btn.setAttribute('aria-label', 'Readonly');
    btn.addEventListener('click', () => this._onReadonlyClick());

    // Lock icon — using inline SVG to avoid dependency on vaadin-icon in shadow DOM
    btn.innerHTML = `<svg viewBox="0 0 16 16" width="1em" height="1em" style="fill:currentColor">
      <path d="M12 7h-1V5c0-1.7-1.3-3-3-3S5 3.3 5 5v2H4c-.6 0-1 .4-1 1v5c0 .6.4 1 1 1h8c.6 0 1-.4 1-1V8c0-.6-.4-1-1-1zM6 5c0-1.1.9-2 2-2s2 .9 2 2v2H6V5z"/>
    </svg>`;

    // Place in format group (last standard group)
    const formatGroup = toolbar.querySelector('[part~="toolbar-group-format"]');
    if (formatGroup) {
      // Insert before the clean button (last button in format group)
      const cleanBtn = formatGroup.querySelector('[part~="toolbar-button-clean"]');
      if (cleanBtn) {
        formatGroup.insertBefore(btn, cleanBtn);
      } else {
        formatGroup.appendChild(btn);
      }
    } else {
      // Fallback: append to toolbar
      const fileInput = toolbar.querySelector('#fileInput');
      toolbar.insertBefore(btn, fileInput || null);
    }

    this.__readonlyButton = btn;

    // Track active state via editor-change
    this._editor.on('editor-change', () => {
      const selection = this._editor.getSelection();
      if (selection && selection.length > 0) {
        const format = this._editor.getFormat(selection.index, selection.length);
        if (format.readonly) {
          btn.classList.add('ql-active');
        } else {
          btn.classList.remove('ql-active');
        }
      } else if (selection) {
        const format = this._editor.getFormat(selection.index);
        if (format.readonly) {
          btn.classList.add('ql-active');
        } else {
          btn.classList.remove('ql-active');
        }
      }
    });
  }

  /**
   * Toggle readonly format on the current selection.
   * @protected
   */
  _onReadonlyClick() {
    const selection = this._editor.getSelection();
    if (!selection || selection.length === 0) return;
    const format = this._editor.getFormat(selection.index, selection.length);
    this._editor.formatText(
      selection.index, selection.length,
      'readonly', !format.readonly, 'user'
    );
  }

  // ==========================================================================
  // Readonly: delete protection
  // ==========================================================================

  /**
   * Installs a text-change handler that reverts any user edit that
   * decreases the number of readonly sections. This prevents deletion
   * of readonly blots via Backspace, Delete, Cut, Select-All+Delete, etc.
   * @protected
   */
  _initReadonlyProtection() {
    const editor = this._editor;

    // Serialize all readonly text content for comparison.
    // Catches both full deletion (count change) and partial deletion
    // (text within a readonly op changes).
    const readonlySignature = (ops) =>
      ops
        .filter((op) => op.attributes && op.attributes.readonly === true)
        .map((op) => (typeof op.insert === 'string' ? op.insert : JSON.stringify(op.insert)))
        .join('\0');

    editor.on('text-change', (delta, oldDelta, source) => {
      if (source !== 'user') return;
      // Only check if the change includes a delete op
      if (!delta.ops.some((op) => op.delete != null)) return;

      const oldSig = readonlySignature(oldDelta.ops);
      const newSig = readonlySignature(editor.getContents().ops);

      if (newSig !== oldSig) {
        // Revert: restore the old contents
        editor.setContents(oldDelta, 'silent');
        // Try to restore cursor position
        if (delta.ops[0] && delta.ops[0].retain != null) {
          editor.setSelection(delta.ops[0].retain, 0, 'silent');
        }
      }
    });
  }

  // ==========================================================================
  // __updateHtmlValue override: preserve ERTE classes in htmlValue
  // ==========================================================================

  /** @private */
  __updateHtmlValue() {
    let content = this._editor.getSemanticHTML();
    // Remove Quill classes, except for align, indent, and ERTE-specific classes
    content = content.replace(/class="([^"]*)"/gu, (_match, group1) => {
      const classes = group1.split(' ').filter((className) => {
        if (!className.startsWith('ql-')) return true;
        if (className.startsWith('ql-align') || className.startsWith('ql-indent')) return true;
        if (ERTE_PRESERVED_CLASSES.includes(className)) return true;
        return false;
      });
      return `class="${classes.join(' ')}"`;
    });
    // Process align and indent classes (parent's method)
    content = this.__processQuillClasses(content);
    this._setHtmlValue(content);
  }

  // ==========================================================================
  // Tab Width Calculation Engine
  // ==========================================================================

  /**
   * Create reusable measure span for text width calculation.
   * @protected
   */
  _createMeasureSpan() {
    if (this._measureSpan) return;
    this._measureSpan = document.createElement('span');
    this._measureSpan.style.cssText = 'visibility:hidden;position:absolute;white-space:pre;left:-9999px;top:-9999px';
    this.shadowRoot.appendChild(this._measureSpan);
  }

  /**
   * RAF-based coalescing for tab width updates.
   * @protected
   */
  _requestTabUpdate() {
    if (this._tabUpdateRafId) return;
    this._tabUpdateRafId = requestAnimationFrame(() => {
      this._updateTabWidths();
      this._tabUpdateRafId = null;
    });
  }

  /**
   * Core iterative tab width calculation engine.
   * Processes tabs one by one: measure position -> calculate width -> set width -> next.
   * @protected
   */
  _updateTabWidths() {
    if (!this._editor) return;

    const editorNode = this._editor.root;
    const tabs = Array.from(editorNode.querySelectorAll('.ql-tab'));

    if (tabs.length === 0) return;

    const charWidth8 = this._measureTextWidth('0'.repeat(TAB_DEFAULT_TAB_CHARS), editorNode);
    const fixedTabWidth = charWidth8 > 0 ? charWidth8 : TAB_FIXED_TAB_FALLBACK;

    const blockVisualLines = new Map();

    // Editor rect is hoisted outside the loop since the editor's outer dimensions
    // don't change during iteration. Per-tab rects must be read inside the loop because
    // each tab's position depends on the previous tab's width (iterative algorithm).
    const editorRect = editorNode.getBoundingClientRect();

    tabs.forEach(tab => {
      // CRITICAL (Spike Item 20): Measure OUTER .ql-tab rect, NOT contentNode.
      // Guard nodes in Quill 2 are INSIDE the embed element.
      const tabRect = tab.getBoundingClientRect();
      const parentBlock = tab.closest(TAB_BLOCK_SELECTOR) || tab.parentElement;
      const parentRect = parentBlock ? parentBlock.getBoundingClientRect() : null;
      const startPos = tabRect.left - editorRect.left;

      const isWrappedLine = this._isWrappedLine(tab, tabRect, parentBlock, parentRect);

      tab.classList.remove('ql-auto-wrap-start');

      if (isWrappedLine && parentBlock) {
        const topPos = Math.round(tabRect.top);
        if (!blockVisualLines.has(parentBlock)) {
          blockVisualLines.set(parentBlock, new Set());
        }
        const seenTops = blockVisualLines.get(parentBlock);
        if (!seenTops.has(topPos)) {
          seenTops.add(topPos);
          tab.classList.add('ql-auto-wrap-start');
        }
      }

      const contentWidth = this._measureContentWidth(tab);

      let targetStop = null;
      if (!isWrappedLine && this._tabStopsArray) {
        targetStop = this._tabStopsArray.find(
          stop => stop.pos > (startPos + TAB_MIN_TAB_WIDTH)
        );
      }

      let widthNeeded = 0;

      if (targetStop) {
        const stopPos = targetStop.pos;
        const alignment = targetStop.align || 'left';
        const rawDistance = stopPos - startPos;

        if (alignment === 'right') {
          widthNeeded = rawDistance - contentWidth;
        } else if (alignment === 'center') {
          widthNeeded = rawDistance - (contentWidth / 2);
        } else {
          widthNeeded = rawDistance;
        }
      } else {
        widthNeeded = fixedTabWidth;
      }

      if (widthNeeded < TAB_MIN_TAB_WIDTH) {
        widthNeeded = TAB_MIN_TAB_WIDTH;
      }

      tab.style.width = widthNeeded + 'px';
    });
  }

  /**
   * Line wrap detection: returns true ONLY for automatic browser text wrapping.
   * Returns false for first line and for lines after soft-break.
   * @protected
   */
  _isWrappedLine(tab, tabRect, parentBlock, parentRect) {
    if (!parentRect || !parentBlock) return false;

    const computedStyle = this._getComputedStyleFor(parentBlock);
    const lineHeight = parseFloat(computedStyle.lineHeight) ||
                       parseFloat(computedStyle.fontSize) * 1.2;

    const verticalOffset = tabRect.top - parentRect.top;
    const threshold = lineHeight * TAB_WRAP_DETECTION_MULTIPLIER;

    if (verticalOffset <= threshold) {
      return false;
    }

    let prevSibling = tab.previousSibling;
    while (prevSibling) {
      if (prevSibling.nodeType === 1) {
        if (prevSibling.classList && prevSibling.classList.contains('ql-soft-break')) {
          return false;
        }
        const siblingRect = prevSibling.getBoundingClientRect();
        if (Math.abs(siblingRect.top - tabRect.top) > threshold) {
          return true;
        }
      }
      prevSibling = prevSibling.previousSibling;
    }

    return true;
  }

  /**
   * Measure content width after a tab (until next tab/soft-break/block).
   * @protected
   */
  _measureContentWidth(tab) {
    let contentWidth = 0;
    let nextNode = tab.nextSibling;

    while (nextNode) {
      if (this._isBreakingNode(nextNode)) break;

      const textNodes = this._getTextNodes(nextNode);
      for (const { text, element } of textNodes) {
        contentWidth += this._measureTextWidth(text, element);
      }

      nextNode = nextNode.nextSibling;
    }

    return contentWidth;
  }

  /**
   * Check if a node breaks the content measurement.
   * @protected
   */
  _isBreakingNode(node) {
    if (!node) return true;

    if (node.classList && (
      node.classList.contains('ql-tab') ||
      node.classList.contains('ql-soft-break')
    )) {
      return true;
    }

    if (node.tagName && TAB_BLOCK_ELEMENTS.includes(node.tagName)) {
      return true;
    }

    return false;
  }

  /**
   * Recursively get all text nodes with their parent elements for style measurement.
   * @protected
   */
  _getTextNodes(node) {
    const result = [];

    if (node.nodeType === 3) {
      result.push({ text: node.nodeValue, element: node.parentNode });
    } else if (node.childNodes && node.childNodes.length > 0) {
      for (const child of node.childNodes) {
        result.push(...this._getTextNodes(child));
      }
    }

    return result;
  }

  /**
   * Cached text width measurement with LRU eviction (max 500 entries).
   * @protected
   */
  _measureTextWidth(text, referenceNode) {
    if (!text) return 0;

    const computedStyle = this._getComputedStyleFor(referenceNode);
    const cacheKey = `${text}|${computedStyle.fontFamily}|${computedStyle.fontSize}|${computedStyle.fontWeight}|${computedStyle.fontStyle}|${computedStyle.letterSpacing}`;

    if (this._textWidthCache.has(cacheKey)) {
      const value = this._textWidthCache.get(cacheKey);
      this._textWidthCache.delete(cacheKey);
      this._textWidthCache.set(cacheKey, value);
      return value;
    }

    const measureSpan = this._measureSpan;
    measureSpan.style.fontFamily = computedStyle.fontFamily;
    measureSpan.style.fontSize = computedStyle.fontSize;
    measureSpan.style.fontWeight = computedStyle.fontWeight;
    measureSpan.style.fontStyle = computedStyle.fontStyle;
    measureSpan.style.letterSpacing = computedStyle.letterSpacing;
    measureSpan.textContent = text;

    const width = measureSpan.getBoundingClientRect().width;

    if (this._textWidthCache.size >= 500) {
      const firstKey = this._textWidthCache.keys().next().value;
      this._textWidthCache.delete(firstKey);
    }
    this._textWidthCache.set(cacheKey, width);

    return width;
  }

  /**
   * Get computed style for an element.
   * Note: getComputedStyle() returns a live CSSStyleDeclaration that always reflects
   * current values, so caching the object provides no benefit.
   * @protected
   */
  _getComputedStyleFor(element) {
    return window.getComputedStyle(element);
  }

  // ==========================================================================
  // Keyboard Bindings
  // ==========================================================================

  /**
   * Patches Quill keyboard bindings for Tab (insert tab embed),
   * Shift+Enter (soft-break with tab copying), and Shift+Tab (focus toolbar).
   * Uses string key names (V25/Quill 2) and function() handlers for this.quill.
   * @protected
   */
  _patchKeyboard() {
    const self = this;

    const focusToolbar = () => {
      this._markToolbarFocused();
      const toolbar = this.shadowRoot.querySelector('[part="toolbar"]');
      if (!toolbar) return;
      const standardButton = toolbar.querySelector('button:not([tabindex])');
      if (standardButton != null) {
        standardButton.focus();
      } else {
        const customSlot = toolbar.querySelector('slot[name="toolbar"]');
        if (customSlot) {
          const button = customSlot
            .assignedElements()
            .filter(e => e.getAttribute('tabindex') == 0 || e.getAttribute('tabindex') == undefined)[0];
          if (button != null) {
            button.focus();
          }
        }
      }
    };

    const keyboard = this._editor.getModule('keyboard');
    const bindings = keyboard.bindings['Tab'] || [];

    // Exclude Quill shift-tab bindings, except for code block,
    // as some of those break when on a newline in the list
    // https://github.com/vaadin/vcf-enhanced-rich-text-editor/issues/67
    const originalBindings = bindings.filter(b => !b.shiftKey || (b.format && b.format['code-block']));

    const moveFocusBinding = { key: 'Tab', shiftKey: true, handler: focusToolbar };

    // Binding for tabstop functionality (insert tab embed)
    const tabStopBinding = {
      key: 'Tab',
      handler: function(range) {
        if (range) {
          self._editor.insertEmbed(range.index, 'tab', true, Quill.sources.USER);
          // Sync width calculation BEFORE cursor move — RAF would defer
          // the width update, leaving the cursor at the old (0-width) position.
          self._updateTabWidths();
          self._editor.setSelection(range.index + 1, 0, Quill.sources.USER);
          return false;
        } else {
          return true;
        }
      }
    };

    // Soft-break binding (Shift+Enter): insert visual line break with tab copying
    const softBreakBinding = {
      key: 'Enter',
      shiftKey: true,
      handler: function(range) {
        const quill = self._editor;
        const [line, offset] = quill.getLine(range.index);
        const lineStartIndex = quill.getIndex(line);

        // Find boundaries of the VISUAL line (between soft-breaks)
        let currentBlot = line.children.head;
        let posInLine = 0;
        let visualLineStart = 0;
        let visualLineEnd = line.length() - 1;

        while (currentBlot) {
          if (currentBlot.statics.blotName === 'soft-break') {
            if (posInLine < offset) {
              visualLineStart = posInLine + 1;
            } else {
              visualLineEnd = posInLine;
              break;
            }
          }
          posInLine += currentBlot.length();
          currentBlot = currentBlot.next;
        }

        // Count tabs between visualLineStart and offset (cursor position)
        currentBlot = line.children.head;
        posInLine = 0;
        let tabsBeforeCursor = 0;

        while (currentBlot && posInLine < offset) {
          if (posInLine >= visualLineStart && currentBlot.statics.blotName === 'tab') {
            tabsBeforeCursor++;
          }
          posInLine += currentBlot.length();
          currentBlot = currentBlot.next;
        }

        // Insert soft-break at cursor position
        const insertIndex = lineStartIndex + offset;
        quill.insertEmbed(insertIndex, 'soft-break', true, Quill.sources.USER);

        // Limit tabs to copy: max = number of defined tabstops
        const maxTabstops = self._tabStopsArray ? self._tabStopsArray.length : 0;
        const tabsToCopy = Math.min(tabsBeforeCursor, maxTabstops);

        // Insert tabs after the soft-break
        let insertPos = insertIndex + 1;
        for (let i = 0; i < tabsToCopy; i++) {
          quill.insertEmbed(insertPos, 'tab', true, Quill.sources.USER);
          insertPos++;
        }

        quill.setSelection(insertPos, 0, Quill.sources.USER);
        self._requestTabUpdate();
        return false;
      }
    };

    // Ensure normal Enter still works (hard break)
    const hardBreakBinding = {
      key: 'Enter',
      shiftKey: false,
      handler: function() { return true; }
    };

    // Replace Tab bindings: ERTE tab first, then original (non-shift), then shift-tab
    keyboard.bindings['Tab'] = [tabStopBinding, ...originalBindings, moveFocusBinding];

    // Add soft-break and hard-break bindings for Enter key
    const enterBindings = keyboard.bindings['Enter'] || [];
    keyboard.bindings['Enter'] = [softBreakBinding, hardBreakBinding, ...enterBindings];
  }

  // ==========================================================================
  // TabStops Property & Observer
  // ==========================================================================

  /**
   * Observer for tabStops property. Converts external {direction, position}
   * format to internal {pos, align} format and updates ruler markers.
   * @protected
   */
  _onTabStopsChanged(tabStops) {
    const horizontalRuler = this.shadowRoot.querySelector('[part="horizontalRuler"]');
    if (horizontalRuler) {
      // Clear existing icons
      const icons = horizontalRuler.querySelectorAll('vaadin-icon');
      icons.forEach(icon => icon.remove());
    }

    if (tabStops) {
      tabStops.forEach(stop => {
        this._addTabStopIcon(stop);
      });
    }

    // Convert external {direction, position} format to internal {pos, align} format
    this._tabStopsArray = (tabStops || []).map(stop => ({
      pos: stop.position,
      align: stop.direction === 'middle' ? 'center' : (stop.direction || 'left')
    }));

    // Sort by position
    this._tabStopsArray.sort((a, b) => a.pos - b.pos);

    if (this._editor) {
      this._requestTabUpdate();
    }
  }

  /**
   * Observer for noRulers property. Toggles ruler wrapper visibility.
   * @protected
   */
  _onNoRulersChanged(noRulers) {
    const rulerWrapper = this.shadowRoot.querySelector('[part="ruler-wrapper"]');
    if (rulerWrapper) {
      rulerWrapper.style.display = noRulers ? 'none' : 'flex';
    }
  }

  // ==========================================================================
  // Ruler DOM
  // ==========================================================================

  /**
   * Injects the ruler DOM (wrapper, corner, horizontal ruler) between the
   * toolbar and the content area in the shadow DOM.
   * @protected
   */
  _injectRuler() {
    const contentDiv = this.shadowRoot.querySelector('[part="content"]');
    if (!contentDiv) return;

    // Ruler wrapper: flex row container
    const wrapper = document.createElement('div');
    wrapper.setAttribute('part', 'ruler-wrapper');
    wrapper.style.cssText = 'overflow:hidden;box-sizing:content-box;width:100%;height:15px;flex-shrink:0;display:flex;';

    // Ruler corner: 14x14px box with border
    const corner = document.createElement('div');
    corner.setAttribute('part', 'ruler-corner');
    corner.style.cssText = 'overflow:hidden;box-sizing:content-box;border-color:rgb(158,170,182);border-style:solid;border-width:0 1px 1px 0;width:14px;height:14px;';
    wrapper.appendChild(corner);

    // Horizontal ruler: repeating tick-mark background
    const ruler = document.createElement('div');
    ruler.setAttribute('part', 'horizontalRuler');
    ruler.style.cssText = `position:relative;overflow:hidden;box-sizing:content-box;background:url('${RULER_HORI_BASE64}') repeat-x;flex-grow:1;height:15px;padding:0;cursor:crosshair;`;
    ruler.addEventListener('click', (event) => this._addTabStop(event));
    wrapper.appendChild(ruler);

    // Insert wrapper before the content div
    contentDiv.parentNode.insertBefore(wrapper, contentDiv);

    // Apply initial noRulers state
    if (this.noRulers) {
      wrapper.style.display = 'none';
    }
  }

  /**
   * Returns the pixel offset between the editor's border-box left
   * and the ruler's left edge. Used to convert between editor
   * coordinate space (used by tab stop positions) and ruler
   * coordinate space (used for marker display).
   * @protected
   */
  _getRulerEditorOffset() {
    const editor = this.shadowRoot.querySelector('.ql-editor');
    const ruler = this.shadowRoot.querySelector('[part="horizontalRuler"]');
    if (!editor || !ruler) return 0;
    return editor.getBoundingClientRect().left - ruler.getBoundingClientRect().left;
  }

  /**
   * Add a tabstop icon to the horizontal ruler.
   * LEFT -> caret-right, RIGHT -> caret-left, MIDDLE -> dot-circle.
   * Click cycles: LEFT -> RIGHT -> MIDDLE -> remove.
   * @protected
   */
  _addTabStopIcon(tabStop) {
    const icon = document.createElement('vaadin-icon');
    let iconName;
    if (tabStop.direction === 'left') {
      iconName = 'vaadin:caret-right';
    } else if (tabStop.direction === 'right') {
      iconName = 'vaadin:caret-left';
    } else {
      iconName = 'vaadin:dot-circle';
    }

    icon.setAttribute('icon', iconName);
    icon.style.width = '15px';
    icon.style.height = '15px';
    icon.style.position = 'absolute';
    icon.style.top = '0px';
    // Convert editor coordinate space to ruler coordinate space
    const offset = this._getRulerEditorOffset();
    icon.style.left = (tabStop.position + offset - 7) + 'px';

    const horizontalRuler = this.shadowRoot.querySelector('[part="horizontalRuler"]');
    if (!horizontalRuler) return;
    horizontalRuler.appendChild(icon);
    icon.tabStop = tabStop;

    const self = this;
    icon.addEventListener('click', function(iconEvent) {
      const clickedIcon = iconEvent.currentTarget;
      const index = self.tabStops.indexOf(clickedIcon.tabStop);
      if (index === -1) return;

      if (clickedIcon.getAttribute('icon') === 'vaadin:caret-right') {
        clickedIcon.setAttribute('icon', 'vaadin:caret-left');
        clickedIcon.tabStop.direction = 'right';
        self.tabStops[index] = clickedIcon.tabStop;
      } else if (clickedIcon.getAttribute('icon') === 'vaadin:caret-left') {
        clickedIcon.setAttribute('icon', 'vaadin:dot-circle');
        clickedIcon.tabStop.direction = 'middle';
        self.tabStops[index] = clickedIcon.tabStop;
      } else {
        self.tabStops.splice(index, 1);
        clickedIcon.remove();
      }

      // Trigger property change notification
      self.tabStops = [...self.tabStops];

      iconEvent.stopPropagation();
    });
  }

  /**
   * Add a new LEFT tabstop at the clicked position on the ruler.
   * @protected
   */
  _addTabStop(event) {
    // Convert ruler click position to editor coordinate space
    const offset = this._getRulerEditorOffset();
    const tabStop = { direction: 'left', position: event.offsetX - offset };
    if (!this.tabStops) {
      this.tabStops = [];
    }
    this.tabStops.push(tabStop);
    this.tabStops.sort((a, b) => a.position - b.position);
    // Trigger property change notification
    this.tabStops = [...this.tabStops];
  }
}

customElements.define(VcfEnhancedRichTextEditor.is, VcfEnhancedRichTextEditor);

// Exported for potential extension by tables addon or test utilities.
export { VcfEnhancedRichTextEditor };
