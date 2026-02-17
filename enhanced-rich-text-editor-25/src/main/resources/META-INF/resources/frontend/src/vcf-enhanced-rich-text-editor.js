/**
 * ERTE v25 - Enhanced Rich Text Editor for Vaadin 25
 *
 * Extends RTE 2's Lit-based web component with ERTE features:
 * - Tab-stop system (TabBlot, soft-break, iterative width calculation)
 * - Read-only sections (ReadOnlyBlot)
 * - Placeholders (PlaceholderBlot)
 * - Non-breaking spaces (Nbsp)
 * - Whitespace indicator toggle
 * - Custom toolbar slots
 * - Ruler (TODO)
 *
 * Architecture:
 * - render() override for custom toolbar buttons and slots
 * - ready() hook for Quill access (keyboard bindings, blot setup)
 * - static get styles() layers ERTE styles on top of RTE 2 base
 * - updated() for reactive property change callbacks
 */

// Import RTE 2 (loads Quill 2.0.3 + defines vaadin-rich-text-editor)
import '@vaadin/rich-text-editor/src/vaadin-rich-text-editor.js';
import { html } from 'lit';

// Import and register all ERTE blots (registration happens at import time)
import { ReadOnlyBlot, PlaceholderBlot } from './vcf-enhanced-rich-text-editor-blots.js';

// Import ERTE-specific styles
import { erteStyles } from './vcf-enhanced-rich-text-editor-styles.js';
import { erteIconStyles } from './vcf-enhanced-rich-text-editor-icons.js';

// Import extra SVG iconset (registers vaadin-iconset in document head)
import './vcf-enhanced-rich-text-editor-extra-icons.js';

// Access Quill from RTE 2's vendored global
const Quill = window.Quill;
const Inline = Quill.import('blots/inline');

// Ensure Inline blot ordering includes ERTE custom inline blots.
// Only Inline blots go in Inline.order; Embeds (TabBlot, SoftBreakBlot) must NOT be listed.
if (!Inline.order.includes(PlaceholderBlot.blotName)) {
  Inline.order.push(PlaceholderBlot.blotName, ReadOnlyBlot.blotName);
}

// Get the parent RTE 2 class from the custom elements registry
const RichTextEditor = customElements.get('vaadin-rich-text-editor');

// ============================================================
// Constants
// ============================================================

const TOOLBAR_BUTTON_GROUPS = {
  history: ['undo', 'redo'],
  emphasis: ['bold', 'italic', 'underline', 'strike'],
  style: ['color', 'background'],
  heading: ['h1', 'h2', 'h3'],
  'glyph-transformation': ['subscript', 'superscript'],
  list: ['listOrdered', 'listBullet'],
  indent: ['deindent', 'indent'],
  alignment: ['alignLeft', 'alignCenter', 'alignRight'],
  'rich-text': ['image', 'link'],
  block: ['blockquote', 'codeBlock', 'placeholder', 'placeholderAppearance'],
  format: ['readonly', 'whitespace', 'clean']
};

const STATE = {
  DEFAULT: 0,
  FOCUSED: 1,
  CLICKED: 2
};

// Tab engine constants (from prototype)
const TAB_WRAP_DETECTION_MULTIPLIER = 0.8;
const TAB_DEFAULT_TAB_CHARS = 8;
const TAB_MIN_TAB_WIDTH = 2;
const TAB_FIXED_TAB_FALLBACK = 50;
const TAB_BLOCK_ELEMENTS = ['P', 'DIV', 'LI', 'H1', 'H2', 'H3', 'H4', 'H5', 'H6',
                            'BLOCKQUOTE', 'PRE', 'OL', 'UL', 'TABLE', 'TR', 'TD', 'TH'];
const TAB_BLOCK_SELECTOR = TAB_BLOCK_ELEMENTS.map(t => t.toLowerCase()).join(', ');

// ============================================================
// ERTE Custom Element
// ============================================================

class EnhancedRichTextEditor extends RichTextEditor {

  static get is() {
    return 'vcf-enhanced-rich-text-editor';
  }

  /**
   * ERTE-specific reactive properties.
   * Java side sets these via getElement().setPropertyJson() / setProperty().
   * Lit reactivity triggers updated() when they change.
   */
  static get properties() {
    return {
      ...super.properties,

      /** Tab stops array: [{direction: 'left'|'right'|'middle', position: number}] */
      tabStops: { type: Array },

      /** When true, whitespace indicators are shown */
      showWhitespace: { type: Boolean, reflect: true },

      /** Hide the ruler (horizontal tab stop ruler) */
      noRulers: { type: Boolean },

      /** Toolbar button visibility map: {buttonName: boolean} */
      toolbarButtons: { type: Object },

      /** Placeholder definitions: array of strings or {text, format, altFormat} objects */
      placeholders: { type: Array },

      /** Placeholder tag delimiters: {start, end} */
      placeholderTags: { type: Object },

      /** Whether alternate placeholder appearance is active */
      placeholderAltAppearance: { type: Boolean },

      /** Regex pattern for placeholder alternate appearance */
      placeholderAltAppearancePattern: { type: String },

      /** Label for current placeholder appearance */
      placeholderAppearance: { type: String }
    };
  }

  /**
   * Layer ERTE-specific styles on top of the parent RTE 2 styles.
   */
  static get styles() {
    const parentStyles = super.styles;
    const base = Array.isArray(parentStyles) ? parentStyles : [parentStyles];
    return [...base, erteIconStyles, ...erteStyles];
  }

  constructor() {
    super();

    // Default property values
    this.tabStops = [];
    this.showWhitespace = false;
    this.noRulers = false;
    this.toolbarButtons = {};
    this.placeholders = [];
    this.placeholderTags = { start: '@', end: '' };
    this.placeholderAltAppearance = false;
    this.placeholderAltAppearancePattern = '';
    this.placeholderAppearance = '';

    // Internal state
    this._toolbarState = STATE.DEFAULT;
    this._tabStopsArray = [];
    this._textWidthCache = new Map();
    this._tabUpdateRafId = null;
    this._measureSpan = null;
    this._resizeHandler = null;
    this._inPlaceholder = undefined;
  }

  /**
   * Override render() to inject ERTE toolbar content.
   * Parent observers (_onReadonlyChanged, etc.) work via [part] selectors.
   */
  render() {
    return html`
      <div class="vaadin-rich-text-editor-container">
        <div part="toolbar" role="toolbar">
          <!-- Slot for toolbar components added BEFORE all groups (Java API) -->
          <slot name="toolbar-start"></slot>

          <slot name="toolbar-before-group-history"></slot>
          <span part="toolbar-group toolbar-group-history">
            <button id="btn-undo" type="button"
              part="toolbar-button toolbar-button-undo"
              aria-label="${this.__effectiveI18n?.undo || 'Undo'}"
              @click="${this._undo}"></button>
            <button id="btn-redo" type="button"
              part="toolbar-button toolbar-button-redo"
              aria-label="${this.__effectiveI18n?.redo || 'Redo'}"
              @click="${this._redo}"></button>
          </span>
          <slot name="toolbar-after-group-history"></slot>

          <slot name="toolbar-before-group-emphasis"></slot>
          <span part="toolbar-group toolbar-group-emphasis">
            <button id="btn-bold" class="ql-bold"
              part="toolbar-button toolbar-button-bold"
              aria-label="${this.__effectiveI18n?.bold || 'Bold'}"></button>
            <button id="btn-italic" class="ql-italic"
              part="toolbar-button toolbar-button-italic"
              aria-label="${this.__effectiveI18n?.italic || 'Italic'}"></button>
            <button id="btn-underline" class="ql-underline"
              part="toolbar-button toolbar-button-underline"
              aria-label="${this.__effectiveI18n?.underline || 'Underline'}"></button>
            <button id="btn-strike" class="ql-strike"
              part="toolbar-button toolbar-button-strike"
              aria-label="${this.__effectiveI18n?.strike || 'Strike'}"></button>
          </span>
          <slot name="toolbar-after-group-emphasis"></slot>

          <span part="toolbar-group toolbar-group-style">
            <button id="btn-color" type="button"
              part="toolbar-button toolbar-button-color"
              aria-label="${this.__effectiveI18n?.color || 'Color'}"
              @click="${this.__onColorClick}"></button>
            <button id="btn-background" type="button"
              part="toolbar-button toolbar-button-background"
              aria-label="${this.__effectiveI18n?.background || 'Background'}"
              @click="${this.__onBackgroundClick}"></button>
          </span>

          <slot name="toolbar-before-group-heading"></slot>
          <span part="toolbar-group toolbar-group-heading">
            <button id="btn-h1" type="button" class="ql-header" value="1"
              part="toolbar-button toolbar-button-h1"
              aria-label="${this.__effectiveI18n?.h1 || 'Header 1'}"></button>
            <button id="btn-h2" type="button" class="ql-header" value="2"
              part="toolbar-button toolbar-button-h2"
              aria-label="${this.__effectiveI18n?.h2 || 'Header 2'}"></button>
            <button id="btn-h3" type="button" class="ql-header" value="3"
              part="toolbar-button toolbar-button-h3"
              aria-label="${this.__effectiveI18n?.h3 || 'Header 3'}"></button>
          </span>
          <slot name="toolbar-after-group-heading"></slot>

          <slot name="toolbar-before-group-glyph-transformation"></slot>
          <span part="toolbar-group toolbar-group-glyph-transformation">
            <button id="btn-subscript" class="ql-script" value="sub"
              part="toolbar-button toolbar-button-subscript"
              aria-label="${this.__effectiveI18n?.subscript || 'Subscript'}"></button>
            <button id="btn-superscript" class="ql-script" value="super"
              part="toolbar-button toolbar-button-superscript"
              aria-label="${this.__effectiveI18n?.superscript || 'Superscript'}"></button>
          </span>
          <slot name="toolbar-after-group-glyph-transformation"></slot>

          <slot name="toolbar-before-group-list"></slot>
          <span part="toolbar-group toolbar-group-list">
            <button id="btn-ol" type="button" class="ql-list" value="ordered"
              part="toolbar-button toolbar-button-list-ordered"
              aria-label="${this.__effectiveI18n?.listOrdered || 'Ordered list'}"></button>
            <button id="btn-ul" type="button" class="ql-list" value="bullet"
              part="toolbar-button toolbar-button-list-bullet"
              aria-label="${this.__effectiveI18n?.listBullet || 'Bullet list'}"></button>
          </span>
          <slot name="toolbar-after-group-list"></slot>

          <slot name="toolbar-before-group-indent"></slot>
          <span part="toolbar-group toolbar-group-indent">
            <button id="btn-outdent" type="button" class="ql-indent" value="-1"
              part="toolbar-button toolbar-button-outdent"
              aria-label="${this.__effectiveI18n?.outdent || 'Decrease indent'}"></button>
            <button id="btn-indent" type="button" class="ql-indent" value="+1"
              part="toolbar-button toolbar-button-indent"
              aria-label="${this.__effectiveI18n?.indent || 'Increase indent'}"></button>
          </span>
          <slot name="toolbar-after-group-indent"></slot>

          <slot name="toolbar-before-group-alignment"></slot>
          <span part="toolbar-group toolbar-group-alignment">
            <button id="btn-left" type="button" class="ql-align" value=""
              part="toolbar-button toolbar-button-align-left"
              aria-label="${this.__effectiveI18n?.alignLeft || 'Align left'}"></button>
            <button id="btn-center" type="button" class="ql-align" value="center"
              part="toolbar-button toolbar-button-align-center"
              aria-label="${this.__effectiveI18n?.alignCenter || 'Align center'}"></button>
            <button id="btn-right" type="button" class="ql-align" value="right"
              part="toolbar-button toolbar-button-align-right"
              aria-label="${this.__effectiveI18n?.alignRight || 'Align right'}"></button>
          </span>
          <slot name="toolbar-after-group-alignment"></slot>

          <slot name="toolbar-before-group-rich-text"></slot>
          <span part="toolbar-group toolbar-group-rich-text">
            <button id="btn-image" type="button"
              part="toolbar-button toolbar-button-image"
              aria-label="${this.__effectiveI18n?.image || 'Image'}"
              @touchend="${this._onImageTouchEnd}"
              @click="${this._onImageClick}"></button>
            <button id="btn-link" type="button"
              part="toolbar-button toolbar-button-link"
              aria-label="${this.__effectiveI18n?.link || 'Link'}"
              @click="${this._onLinkClick}"></button>
          </span>
          <slot name="toolbar-after-group-rich-text"></slot>

          <slot name="toolbar-before-group-block"></slot>
          <span part="toolbar-group toolbar-group-block">
            <button id="btn-blockquote" type="button" class="ql-blockquote"
              part="toolbar-button toolbar-button-blockquote"
              aria-label="${this.__effectiveI18n?.blockquote || 'Blockquote'}"></button>
            <button id="btn-code" type="button" class="ql-code-block"
              part="toolbar-button toolbar-button-code-block"
              aria-label="${this.__effectiveI18n?.codeBlock || 'Code block'}"></button>
            <button id="btn-placeholder" type="button"
              part="toolbar-button toolbar-button-placeholder"
              aria-label="Placeholder"
              @click="${this._onPlaceholderClick}"></button>
            <button id="btn-placeholder-appearance" type="button"
              part="toolbar-button toolbar-button-placeholder-appearance"
              aria-label="Placeholder Appearance"
              @click="${this._onPlaceholderAppearanceClick}"></button>
          </span>
          <slot name="toolbar-after-group-block"></slot>

          <slot name="toolbar-before-group-format"></slot>
          <span part="toolbar-group toolbar-group-format">
            <button id="btn-readonly" type="button"
              part="toolbar-button toolbar-button-readonly"
              aria-label="Read-only"
              @click="${this._onReadonlyClick}"></button>
            <button id="btn-whitespace" type="button"
              part="toolbar-button toolbar-button-whitespace"
              aria-label="Whitespace"
              @click="${this._onWhitespaceClick}"></button>
            <button id="btn-clean" type="button" class="ql-clean"
              part="toolbar-button toolbar-button-clean"
              aria-label="${this.__effectiveI18n?.clean || 'Clean'}"></button>
          </span>
          <slot name="toolbar-after-group-format"></slot>

          <!-- Custom group slot (legacy group for extensions like tables) -->
          <slot name="toolbar-before-group-custom"></slot>
          <slot name="toolbar"></slot>
          <slot name="toolbar-after-group-custom"></slot>

          <!-- Slot for toolbar components added AFTER all groups (Java API) -->
          <slot name="toolbar-end"></slot>

          <input id="fileInput" type="file"
            accept="image/png, image/gif, image/jpeg, image/bmp, image/x-icon"
            @change="${this._uploadImage}" />
        </div>

        <div part="content"></div>

        <div class="announcer" aria-live="polite"></div>
      </div>

      <slot name="tooltip"></slot>
      <slot name="link-dialog"></slot>
      <slot name="color-popup"></slot>
      <slot name="background-popup"></slot>
    `;
  }

  // ============================================================
  // HTML value override
  // ============================================================

  /**
   * Overrides RTE 2's __updateHtmlValue to preserve ERTE-specific
   * ql-* CSS classes in the htmlValue output.
   *
   * The parent strips ALL ql-* classes except ql-align and ql-indent.
   * ERTE needs ql-tab, ql-soft-break, ql-readonly, and ql-placeholder
   * preserved for HTML round-trip (getValue/setValue).
   */
  __updateHtmlValue() {
    if (!this._editor) return;
    const erteClasses = new Set([
      'ql-tab', 'ql-soft-break', 'ql-readonly', 'ql-placeholder'
    ]);
    let content = this._editor.getSemanticHTML();
    content = content.replace(/class="([^"]*)"/gu, (_match, group1) => {
      const classes = group1.split(' ').filter((cls) => {
        return !cls.startsWith('ql-') ||
               cls.startsWith('ql-align') ||
               cls.startsWith('ql-indent') ||
               erteClasses.has(cls);
      });
      return `class="${classes.join(' ')}"`;
    });
    content = this.__processQuillClasses(content);
    this._setHtmlValue(content);
  }

  // ============================================================
  // Lifecycle
  // ============================================================

  /** @protected */
  ready() {
    super.ready();

    const quill = this._editor;
    if (!quill) return;

    // --- Tab engine initialization ---
    this._createMeasureSpan();

    // Populate _tabStopsArray from initial tabStops property if already set
    if (this.tabStops && this.tabStops.length > 0) {
      this._tabStopsChanged(this.tabStops);
    }

    // --- Read-only section protection ---
    this._setupReadonlyProtection(quill);

    // --- Keyboard bindings ---
    this._patchKeyboard(quill);

    // --- Toolbar patching ---
    this._patchToolbar(quill);

    // --- Clipboard matchers for old-format HTML tags (paste migration) ---
    this._setupClipboardMatchers(quill);

    // --- Event listeners ---

    // Tab widths update on every text change
    quill.on('text-change', () => {
      this._requestTabUpdate();
    });

    // Resize handler: recalculate tab widths
    this._resizeHandler = () => {
      this._requestTabUpdate();
    };
    window.addEventListener('resize', this._resizeHandler);

    // Selection change: placeholder tracking + selected-line-changed event
    quill.on('selection-change', (range) => {
      if (range !== null) {
        // Placeholder selection tracking
        const placeholders = this.selectedPlaceholders;
        const placeholderBtn = this.shadowRoot.querySelector('#btn-placeholder');
        if (placeholders.length) {
          this._inPlaceholder = true;
          if (placeholderBtn) {
            placeholderBtn.classList.add('ql-active');
            placeholderBtn.part.toggle('toolbar-button-pressed', true);
          }
          this.dispatchEvent(new CustomEvent('placeholder-select', {
            bubbles: true, cancelable: false,
            detail: { placeholders }
          }));
        } else {
          if (this._inPlaceholder === true) this._inPlaceholder = false;
          if (placeholderBtn) {
            placeholderBtn.classList.remove('ql-active');
            placeholderBtn.part.toggle('toolbar-button-pressed', false);
          }
        }
        if (this._inPlaceholder === false) {
          this.dispatchEvent(new CustomEvent('placeholder-leave', { bubbles: true }));
          delete this._inPlaceholder;
        }

        // selected-line-changed event for Java integration
        let detailObject = { selected: undefined, path: [], isTable: false, isList: false };
        let lineElement = quill.getLine(range.index)[0];
        if (lineElement) {
          let current = lineElement.domNode;
          if (this.__lastSelectedDomNode !== current) {
            this.__lastSelectedDomNode = current;
            detailObject.selected = current.tagName.toLowerCase();
            while (current && current !== quill.root) {
              let tagName = current.tagName;
              if (tagName === 'TABLE') detailObject.isTable = true;
              else if (tagName === 'UL' || tagName === 'OL') detailObject.isList = true;
              detailObject.path.push(tagName.toLowerCase());
              current = current.parentNode;
            }
            this.dispatchEvent(new CustomEvent('selected-line-changed', { detail: detailObject }));
          }
        }
      } else {
        delete this.__lastSelectedDomNode;
        this.dispatchEvent(new CustomEvent('selected-line-changed', {
          detail: { selected: undefined, path: [], isTable: false, isList: false }
        }));
      }
    });

    // Prevent cursor inside placeholder
    quill.root.addEventListener('selectstart', e => {
      let node = e.target.nodeType === 3 ? e.target.parentElement : e.target;
      const isPlaceholder = n => n.classList && n.classList.contains('ql-placeholder');
      while (node && node.parentElement && !isPlaceholder(node)) node = node.parentElement;
      if (node && isPlaceholder(node)) {
        e.preventDefault();
        if (node.childNodes[2]) {
          this._setSelectionNode(node.childNodes[2], 1);
        }
      }
    });

    // Placeholder delete on character keypress
    quill.root.addEventListener('keypress', e => {
      const sel = quill.getSelection();
      if (sel && sel.length && this.selectedPlaceholders.length) {
        if (!e.ctrlKey && !e.metaKey && !e.altKey) {
          e.preventDefault();
          this._removePlaceholders(this.selectedPlaceholders, false, e.key);
        }
      }
    });

    // Apply initial showWhitespace state
    if (this.showWhitespace) {
      this._showWhitespaceChanged(true);
    }

    // Apply initial toolbar button visibility
    if (this.toolbarButtons && Object.keys(this.toolbarButtons).length > 0) {
      this._applyToolbarButtonVisibility(this.toolbarButtons);
    }

    // Apply initial placeholder configuration
    if (this.placeholderTags) {
      PlaceholderBlot.tags = this.placeholderTags;
    }
    if (this.placeholderAltAppearancePattern) {
      PlaceholderBlot.altAppearanceRegex = this.placeholderAltAppearancePattern;
    }

    // Initial tab width calculation
    this._requestTabUpdate();
  }

  /**
   * Lit reactive property change callback.
   */
  updated(changedProperties) {
    super.updated(changedProperties);

    if (changedProperties.has('tabStops') && this._editor) {
      this._tabStopsChanged(this.tabStops);
    }

    if (changedProperties.has('showWhitespace')) {
      this._showWhitespaceChanged(this.showWhitespace);
    }

    if (changedProperties.has('toolbarButtons') && this.toolbarButtons) {
      this._applyToolbarButtonVisibility(this.toolbarButtons);
    }

    if (changedProperties.has('placeholderTags') && this.placeholderTags) {
      PlaceholderBlot.tags = this.placeholderTags;
    }

    if (changedProperties.has('placeholderAltAppearancePattern')) {
      PlaceholderBlot.altAppearanceRegex = this.placeholderAltAppearancePattern || null;
    }

    if (changedProperties.has('placeholderAltAppearance')) {
      this._placeholderAltAppearanceChanged(this.placeholderAltAppearance);
    }
  }

  /** @protected */
  connectedCallback() {
    super.connectedCallback();

    // Re-initialize tab engine resources after reconnection
    if (this._editor && !this._measureSpan) {
      this._createMeasureSpan();
    }
    if (!this._textWidthCache) {
      this._textWidthCache = new Map();
    }
    if (this._editor && !this._resizeHandler) {
      this._resizeHandler = () => this._requestTabUpdate();
      window.addEventListener('resize', this._resizeHandler);
      this._requestTabUpdate();
    }
  }

  /** @protected */
  disconnectedCallback() {
    super.disconnectedCallback();

    // Cleanup tab engine resources
    if (this._tabUpdateRafId) {
      cancelAnimationFrame(this._tabUpdateRafId);
      this._tabUpdateRafId = null;
    }
    if (this._resizeHandler) {
      window.removeEventListener('resize', this._resizeHandler);
      this._resizeHandler = null;
    }
    if (this._measureSpan && this._measureSpan.parentNode) {
      this._measureSpan.parentNode.removeChild(this._measureSpan);
    }
    this._measureSpan = null;
    if (this._textWidthCache) this._textWidthCache.clear();
  }

  // ============================================================
  // Tab Engine (iterative width calculation)
  // ============================================================

  /**
   * Create reusable measure span for text width calculation.
   * Placed inside shadow DOM to inherit editor font styles.
   */
  _createMeasureSpan() {
    if (this._measureSpan) return;
    this._measureSpan = document.createElement('span');
    this._measureSpan.style.cssText = 'visibility:hidden;position:absolute;white-space:pre;left:-9999px;top:-9999px';
    this.shadowRoot.appendChild(this._measureSpan);
  }

  /**
   * RAF-based coalescing for tab width updates.
   * Multiple calls per frame are coalesced into a single update.
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
   * This ensures each tab's position is measured AFTER previous tabs have been sized.
   */
  _updateTabWidths() {
    if (!this._editor) return;

    const editorNode = this._editor.root;
    const tabs = Array.from(editorNode.querySelectorAll('.ql-tab'));

    if (tabs.length === 0) return;

    const charWidth8 = this._measureTextWidth('0'.repeat(TAB_DEFAULT_TAB_CHARS), editorNode);
    const fixedTabWidth = charWidth8 > 0 ? charWidth8 : TAB_FIXED_TAB_FALLBACK;

    const blockVisualLines = new Map();
    const editorRect = editorNode.getBoundingClientRect();

    tabs.forEach(tab => {
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

    // Traverse ALL previous siblings looking for soft-break on the same visual line.
    // If we find an element on a different visual line first, it's genuine auto-wrap.
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
   * Cached text width measurement with LRU eviction.
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
    if (!measureSpan) return 0;

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
   */
  _getComputedStyleFor(element) {
    return window.getComputedStyle(element);
  }

  // ============================================================
  // Keyboard Bindings (Quill 2 string keys)
  // ============================================================

  /**
   * Set up all ERTE keyboard bindings.
   * Quill 2 uses string key names (e.g., 'Tab', 'Enter') instead of numeric keyCodes.
   */
  _patchKeyboard(quill) {
    const keyboard = quill.getModule('keyboard');
    const self = this;

    // --- Focus toolbar helper ---
    const focusToolbar = () => {
      this._markToolbarFocused();
      const toolbar = this.shadowRoot.querySelector('[part~="toolbar"]');
      if (toolbar) {
        const btn = toolbar.querySelector('button:not([tabindex="-1"])');
        if (btn) btn.focus();
      }
    };

    // === TAB KEY ===
    const tabBindings = keyboard.bindings['Tab'] || [];
    // Exclude Quill's shift-tab bindings except for code-block
    const originalTabBindings = tabBindings.filter(b => !b.shiftKey || (b.format && b.format['code-block']));

    const tabStopBinding = {
      key: 'Tab',
      handler: function(range) {
        if (self.tabStops.length > 0 && range) {
          self._editor.insertEmbed(range.index, 'tab', true, Quill.sources.USER);
          Promise.resolve().then(() => {
            self._editor.setSelection(range.index + 1, 0, Quill.sources.API);
          });
          self._requestTabUpdate();
          return false;
        } else {
          return true;
        }
      }
    };

    const moveFocusBinding = { key: 'Tab', shiftKey: true, handler: focusToolbar };

    keyboard.bindings['Tab'] = [tabStopBinding, ...originalTabBindings, moveFocusBinding];

    // === ENTER KEY (Soft-break + Hard-break) ===
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

        Promise.resolve().then(() => {
          quill.setSelection(insertPos, Quill.sources.SILENT);
          self._requestTabUpdate();
        });
        return false;
      }
    };

    const hardBreakBinding = {
      key: 'Enter',
      shiftKey: false,
      handler: function() { return true; }
    };

    const enterBindings = keyboard.bindings['Enter'] || [];
    keyboard.bindings['Enter'] = [softBreakBinding, hardBreakBinding, ...enterBindings];

    // === BACKSPACE KEY ===
    const backspaceBindings = keyboard.bindings['Backspace'] || [];
    keyboard.bindings['Backspace'] = [
      {
        key: 'Backspace',
        handler: () => {
          if (this.selectedPlaceholders.length) {
            this._removePlaceholders();
            return false;
          }
          return true;
        }
      },
      ...backspaceBindings
    ];

    // === DELETE KEY ===
    const deleteBindings = keyboard.bindings['Delete'] || [];
    keyboard.bindings['Delete'] = [
      {
        key: 'Delete',
        handler: () => {
          const sel = this._editor.getSelection();
          let nextPlaceholder = false;
          if (sel && sel.length === 0) {
            const index = sel.index;
            const ops = this._editor.getContents(index, 1).ops || [];
            nextPlaceholder = ops[0] && ops[0].insert && ops[0].insert.placeholder;
            if (nextPlaceholder) this._editor.setSelection(index, 1);
          }
          if (this.selectedPlaceholders.length || nextPlaceholder) {
            this._removePlaceholders();
            return false;
          }
          return true;
        }
      },
      ...deleteBindings
    ];

    // === ARROW UP/DOWN (correction for tab-filled lines) ===
    const arrowUpBindings = keyboard.bindings['ArrowUp'] || [];
    const arrowDownBindings = keyboard.bindings['ArrowDown'] || [];

    keyboard.bindings['ArrowUp'] = [
      { key: 'ArrowUp', shiftKey: false, handler: (range) => self._handleArrowNavigation(range, -1) },
      ...arrowUpBindings
    ];
    keyboard.bindings['ArrowDown'] = [
      { key: 'ArrowDown', shiftKey: false, handler: (range) => self._handleArrowNavigation(range, +1) },
      ...arrowDownBindings
    ];

    // === ALT+F10: Focus toolbar ===
    keyboard.addBinding({ key: 'F10', altKey: true, handler: focusToolbar });

    // === SHIFT+SPACE: Non-breaking space ===
    keyboard.addBinding({ key: ' ', shiftKey: true }, () => {
      const sel = this._editor.getSelection();
      if (sel) {
        this._editor.insertEmbed(sel.index, 'nbsp', '');
      }
    });

    // === CTRL+P: Insert placeholder ===
    keyboard.addBinding({ key: 'p', shortKey: true }, () => this._onPlaceholderClick());

    // === Z KEY: Undo/Redo placeholder events ===
    const zBindings = keyboard.bindings['z'] || [];
    keyboard.bindings['z'] = [
      {
        key: 'z',
        shortKey: true,
        handler: () => {
          self._undoPlaceholderEvents();
          return true;
        }
      },
      {
        key: 'z',
        shiftKey: true,
        shortKey: true,
        handler: () => {
          self._redoPlaceholderEvents();
          return true;
        }
      },
      ...zBindings
    ];

    // === V KEY: Paste removes placeholders ===
    const vBindings = keyboard.bindings['v'] || [];
    keyboard.bindings['v'] = [
      {
        key: 'v',
        shortKey: true,
        handler: () => {
          const placeholders = self.selectedPlaceholders;
          if (placeholders.length) self._confirmRemovePlaceholders(placeholders, false, true);
          return true;
        }
      },
      ...vBindings
    ];
  }

  /**
   * Handle ArrowUp/ArrowDown through tab-filled lines.
   * Tab blots are inline-block with tiny/zero font-size, which confuses
   * the browser's vertical cursor navigation.
   */
  _handleArrowNavigation(range, direction) {
    const quill = this._editor;
    if (!quill) return true;

    const allLines = quill.getLines(0, quill.getLength());
    if (allLines.length <= 1) return true;

    const [currentLine] = quill.getLine(range.index);
    if (!currentLine) return true;
    let currentLineIdx = -1;
    for (let i = 0; i < allLines.length; i++) {
      if (allLines[i] === currentLine) { currentLineIdx = i; break; }
    }
    if (currentLineIdx < 0) return true;

    const targetLineIdx = currentLineIdx + direction;
    if (targetLineIdx < 0 || targetLineIdx >= allLines.length) return true;

    const targetLine = allLines[targetLineIdx];
    const targetLineStart = quill.getIndex(targetLine);
    const targetLineLen = targetLine.length() - 1;

    const lineHasTabs = (line) => {
      if (!line.children) return false;
      let child = line.children.head;
      while (child) {
        if (child.statics && child.statics.blotName === 'tab') return true;
        child = child.next;
      }
      return false;
    };

    if (!lineHasTabs(targetLine) && !lineHasTabs(currentLine)) {
      return true;
    }

    const currentBounds = quill.getBounds(range.index);
    const targetX = currentBounds.left;

    let bestIndex = targetLineStart;
    let bestDist = Infinity;

    for (let i = targetLineStart; i <= targetLineStart + targetLineLen; i++) {
      const b = quill.getBounds(i);
      const dist = Math.abs(b.left - targetX);
      if (dist < bestDist) {
        bestDist = dist;
        bestIndex = i;
      }
    }

    quill.setSelection(bestIndex, 0, Quill.sources.SILENT);
    return false;
  }

  // ============================================================
  // Toolbar Patching
  // ============================================================

  /**
   * Patch the Quill toolbar module to add custom button bindings and
   * toggle the 'toolbar-button-pressed' part on active toolbar buttons.
   */
  _patchToolbar(quill) {
    const toolbar = quill.getModule('toolbar');
    if (!toolbar) return;

    const update = toolbar.update;

    // Register readonly button with toolbar controls for active state tracking
    const readonlyBtn = this.shadowRoot.querySelector('[part~="toolbar-button-readonly"]');
    if (readonlyBtn) {
      toolbar.controls.push(['readonly', readonlyBtn]);
    }

    const linkBtn = this.shadowRoot.querySelector('[part~="toolbar-button-link"]');
    if (linkBtn) {
      toolbar.controls.push(['link', linkBtn]);
    }

    // Patch toolbar update to toggle 'toolbar-button-pressed' part for styling
    toolbar.update = function(range) {
      update.call(toolbar, range);
      toolbar.controls.forEach(pair => {
        const input = pair[1];
        if (input.classList.contains('ql-active')) {
          input.part?.toggle('toolbar-button-pressed', true);
        } else {
          input.part?.toggle('toolbar-button-pressed', false);
        }
      });
    };
  }

  // ============================================================
  // Read-only Section Protection
  // ============================================================

  /**
   * Prevent user from deleting read-only blots.
   * If a delete operation removes readonly sections, restore the old content.
   */
  _setupReadonlyProtection(quill) {
    quill.on('text-change', function(delta, oldDelta, source) {
      if (source === 'user' && delta.ops.some(o => !!o.delete)) {
        const currentDelta = quill.getContents().ops;
        if (oldDelta.ops.some(v => !!v.insert && v.insert.readonly)) {
          const readonlySectionsCount = oldDelta.ops.filter(v => !!v.insert && v.insert.readonly).length;
          const newReadonlySectionsCount = currentDelta.filter(v => !!v.insert && v.insert.readonly).length;

          if (readonlySectionsCount !== newReadonlySectionsCount) {
            quill.setContents(oldDelta);
            const retainOp = delta.ops.find(o => o.retain != null);
            if (retainOp) {
              quill.setSelection(retainOp.retain + 1, 0);
            }
          }
        }
      }
    });
  }

  // ============================================================
  // Clipboard Matchers
  // ============================================================

  /**
   * Register clipboard matchers for old-format HTML tags (paste migration).
   * Converts old ERTE 1 tags (TAB, PRE-TAB, TABS-CONT, LINE-PART) to
   * new embed-based format on paste.
   */
  _setupClipboardMatchers(quill) {
    const Delta = Quill.import('delta');

    quill.clipboard.addMatcher('TAB', (node, delta) => {
      return new Delta().insert({ tab: true });
    });
    quill.clipboard.addMatcher('PRE-TAB', (node, delta) => {
      return new Delta().insert({ tab: true });
    });
    quill.clipboard.addMatcher('TABS-CONT', (node, delta) => {
      return delta; // Strip wrapper, keep children
    });
    quill.clipboard.addMatcher('LINE-PART', (node, delta) => {
      return delta; // Strip wrapper, keep children
    });

    // Placeholder paste handler
    quill.clipboard.addMatcher('.ql-placeholder', (node, delta) => {
      const index = quill.selection.savedRange ? quill.selection.savedRange.index : 0;
      const placeholder = node.dataset.placeholder;
      if (placeholder) {
        this._confirmInsertPlaceholders([{ placeholder, index }], false, true);
      }
      return delta;
    });
  }

  // ============================================================
  // Property Change Callbacks
  // ============================================================

  /**
   * Convert external tabStops format to internal format and trigger tab update.
   * External: [{direction: 'left'|'right'|'middle', position: number}]
   * Internal: [{pos: number, align: 'left'|'center'|'right'}]
   */
  _tabStopsChanged(tabStops) {
    this._tabStopsArray = (tabStops || []).map(stop => ({
      pos: stop.position,
      align: stop.direction === 'middle' ? 'center' : (stop.direction || 'left')
    }));

    if (this._editor) {
      this._requestTabUpdate();
    }
  }

  /**
   * Toggle show-whitespace class on the Quill editor element
   * and update the toolbar button state.
   */
  _showWhitespaceChanged(show) {
    if (this._editor && this._editor.root) {
      this._editor.root.classList.toggle('show-whitespace', show);
    }
    const btn = this.shadowRoot?.querySelector('#btn-whitespace');
    if (btn) {
      btn.classList.toggle('ql-active', show);
      btn.part.toggle('toolbar-button-pressed', show);
    }
  }

  /**
   * Apply toolbar button visibility based on the toolbarButtons map.
   * Map format: { buttonName: false } hides the button.
   * Group is hidden when all its buttons are hidden.
   */
  _applyToolbarButtonVisibility(toolbarButtons) {
    if (!toolbarButtons) return;

    // Map of button name → CSS selector for the part attribute
    const buttonPartMap = {
      undo: 'toolbar-button-undo', redo: 'toolbar-button-redo',
      bold: 'toolbar-button-bold', italic: 'toolbar-button-italic',
      underline: 'toolbar-button-underline', strike: 'toolbar-button-strike',
      color: 'toolbar-button-color', background: 'toolbar-button-background',
      h1: 'toolbar-button-h1', h2: 'toolbar-button-h2', h3: 'toolbar-button-h3',
      subscript: 'toolbar-button-subscript', superscript: 'toolbar-button-superscript',
      listOrdered: 'toolbar-button-list-ordered', listBullet: 'toolbar-button-list-bullet',
      deindent: 'toolbar-button-outdent', indent: 'toolbar-button-indent',
      alignLeft: 'toolbar-button-align-left', alignCenter: 'toolbar-button-align-center',
      alignRight: 'toolbar-button-align-right',
      image: 'toolbar-button-image', link: 'toolbar-button-link',
      blockquote: 'toolbar-button-blockquote', codeBlock: 'toolbar-button-code-block',
      placeholder: 'toolbar-button-placeholder',
      placeholderAppearance: 'toolbar-button-placeholder-appearance',
      readonly: 'toolbar-button-readonly',
      whitespace: 'toolbar-button-whitespace',
      clean: 'toolbar-button-clean'
    };

    // Apply button visibility
    for (const [buttonName, partName] of Object.entries(buttonPartMap)) {
      const btn = this.shadowRoot?.querySelector(`[part~="${partName}"]`);
      if (btn) {
        if (toolbarButtons[buttonName] === false) {
          btn.style.display = 'none';
        } else {
          btn.style.display = '';
        }
      }
    }

    // Apply group visibility: hide group when all its buttons are hidden
    for (const [groupName, buttons] of Object.entries(TOOLBAR_BUTTON_GROUPS)) {
      const groupEl = this.shadowRoot?.querySelector(`[part~="toolbar-group-${groupName}"]`);
      if (groupEl) {
        const allHidden = buttons.every(btn => toolbarButtons[btn] === false);
        groupEl.style.display = allHidden ? 'none' : '';
      }
    }
  }

  /**
   * Toggle alternate placeholder appearance on all existing placeholders.
   */
  _placeholderAltAppearanceChanged(altAppearance) {
    if (!this._editor) return;

    const placeholderNodes = this._editor.root.querySelectorAll('.ql-placeholder');
    placeholderNodes.forEach(node => {
      const data = PlaceholderBlot.loadValue(node);
      data.altAppearance = altAppearance;
      PlaceholderBlot.storeValue(node, data);
      PlaceholderBlot.setText(node);
    });

    // Re-apply formats after setText
    placeholderNodes.forEach(node => {
      const blot = Quill.find(node);
      if (blot && blot.applyFormat) blot.applyFormat();
    });
  }

  // ============================================================
  // Toolbar State Management
  // ============================================================

  _markToolbarClicked() {
    this._toolbarState = STATE.CLICKED;
  }

  _markToolbarFocused() {
    this._toolbarState = STATE.FOCUSED;
  }

  _cleanToolbarState() {
    this._toolbarState = STATE.DEFAULT;
  }

  // ============================================================
  // Toolbar Button Handlers
  // ============================================================

  _onWhitespaceClick() {
    this._markToolbarClicked();
    this.showWhitespace = !this.showWhitespace;
  }

  /**
   * Toggle read-only format on current selection.
   */
  _onReadonlyClick() {
    this._markToolbarClicked();
    const range = this._getSelection();
    if (range && range.length > 0) {
      const [readOnlySection] = this._editor.scroll.descendant(ReadOnlyBlot, range.index);
      this._editor.formatText(range.index, range.length, 'readonly', readOnlySection == null, 'user');
    }
  }

  /**
   * Handle placeholder button click.
   * Dispatches events for Java-side handling.
   */
  _onPlaceholderClick() {
    this._markToolbarClicked();
    const range = this._getSelection();
    if (range) {
      const detail = { position: range.index };
      const event = new CustomEvent('placeholder-button-click', { bubbles: true, cancelable: true, detail });
      this.dispatchEvent(event);
    }
  }

  /**
   * Toggle placeholder alternate appearance.
   */
  _onPlaceholderAppearanceClick() {
    this._markToolbarClicked();
    this.placeholderAltAppearance = !this.placeholderAltAppearance;
    const btn = this.shadowRoot.querySelector('#btn-placeholder-appearance');
    if (btn) {
      btn.classList.toggle('ql-active', !this.placeholderAltAppearance);
      btn.part.toggle('toolbar-button-pressed', !this.placeholderAltAppearance);
    }
    this.dispatchEvent(new CustomEvent('placeholder-appearance-change', {
      bubbles: true,
      detail: { altAppearance: this.placeholderAltAppearance }
    }));
  }

  // ============================================================
  // Placeholder Support
  // ============================================================

  /** Get the placeholder at current cursor position. */
  get selectedPlaceholder() {
    const range = this._getSelection();
    let placeholder = null;
    if (range && this._editor) {
      const op = this._editor.getContents(range.index - 1, 1).ops[0];
      placeholder = (op && op.insert && op.insert.placeholder) || null;
    }
    return placeholder;
  }

  /** Get all placeholders in current selection range. */
  get selectedPlaceholders() {
    const range = this._getSelection();
    const placeholders = [];
    if (range && this._editor) {
      for (let i = range.index - 1; i < range.index + range.length; i++) {
        const op = this._editor.getContents(i, 1).ops[0];
        const placeholder = (op && op.insert && op.insert.placeholder) || null;
        if (placeholder) placeholders.push(placeholder);
      }
    }
    return placeholders;
  }

  /**
   * Remove selected placeholders.
   * Dispatches placeholder-before-delete and placeholder-delete events.
   */
  _removePlaceholders(placeholders, skipEvent, replaceText) {
    const selected = placeholders || this.selectedPlaceholders;
    if (!selected.length) return;

    const detail = { placeholders: selected };
    const beforeEvent = new CustomEvent('placeholder-before-delete', {
      bubbles: true, cancelable: true, detail
    });
    const cancelled = !skipEvent && !this.dispatchEvent(beforeEvent);
    if (cancelled) return;

    this._confirmRemovePlaceholders(selected, skipEvent, replaceText);
  }

  /**
   * Confirm removal of placeholders.
   */
  _confirmRemovePlaceholders(placeholders, skipEvent, replaceText) {
    const range = this._getSelection();
    if (!range || !this._editor) return;

    this._editor.deleteText(range.index - 1, range.length || 1, Quill.sources.USER);

    if (replaceText && typeof replaceText === 'string') {
      this._editor.insertText(range.index - 1, replaceText, Quill.sources.USER);
    }

    if (!skipEvent) {
      this.dispatchEvent(new CustomEvent('placeholder-delete', {
        bubbles: true,
        detail: { placeholders }
      }));
    }
  }

  /**
   * Confirm insertion of placeholders.
   * Inserts the placeholder embeds into Quill and dispatches the placeholder-insert event.
   * When eventsOnly=true (e.g. from paste/undo), only fires the event without inserting.
   * Called from Java via executeJs after PlaceholderButtonClickedEvent.insert().
   */
  _confirmInsertPlaceholders(placeholders, skipEvent, eventsOnly) {
    if (!this._editor) return;

    const detail = { placeholders: placeholders.map(i => i.placeholder || i) };
    let selectIndex = 0;

    if (!eventsOnly) {
      placeholders.forEach(({ placeholder, index }) => {
        const data = typeof placeholder === 'string' ? JSON.parse(placeholder) : placeholder;
        if (this.placeholderAltAppearance && data) data.altAppearance = true;
        this._editor.insertEmbed(index, 'placeholder', data, Quill.sources.USER);
        selectIndex = index;
      });
      this._editor.setSelection(selectIndex + 1, 0);
    }

    if (!skipEvent) {
      this.dispatchEvent(new CustomEvent('placeholder-insert', {
        bubbles: true,
        detail
      }));
    }
  }

  /**
   * Insert placeholders at specified positions.
   * Lower-level method that only inserts embeds, no events.
   * Called from Java via executeJs for programmatic insertion.
   */
  _insertPlaceholders(placeholders) {
    if (!this._editor || !placeholders) return;

    placeholders.forEach(({ placeholder, index }) => {
      const data = typeof placeholder === 'string' ? JSON.parse(placeholder) : placeholder;
      this._editor.insertEmbed(index, 'placeholder', data, Quill.sources.USER);
    });
  }

  // ============================================================
  // Custom Keyboard Shortcuts (Java API)
  // ============================================================

  /**
   * Add a toolbar focus binding.
   * Called from Java via executeJs to register a keyboard shortcut that focuses the toolbar.
   */
  addToolbarFocusBinding(key, shortKey, shiftKey, altKey) {
    if (!this._editor) return;

    const keyboard = this._editor.getModule('keyboard');
    if (!keyboard) return;

    keyboard.addBinding({ key, shortKey, shiftKey, altKey }, () => {
      this._markToolbarFocused();
      const toolbar = this.shadowRoot.querySelector('[part~="toolbar"]');
      if (toolbar) {
        const btn = toolbar.querySelector('button:not([tabindex="-1"])');
        if (btn) btn.focus();
      }
    });
  }

  /**
   * Add a standard button binding.
   * Called from Java via executeJs to register keyboard shortcuts for toolbar buttons.
   */
  addStandardButtonBinding(button, key, shortKey, shiftKey, altKey) {
    if (!this._editor) return;

    const keyboard = this._editor.getModule('keyboard');
    const toolbar = this._editor.getModule('toolbar');
    if (!keyboard || !toolbar) return;

    const handler = toolbar.handlers[button];
    if (!handler) return;

    keyboard.addBinding({ key, shortKey, shiftKey, altKey }, () => {
      handler.call(toolbar);
    });
  }

  // ============================================================
  // Placeholder History (Undo/Redo)
  // ============================================================

  _emitPlaceholderHistoryEvents(ops) {
    const placeholders = [];
    let insert = true;
    for (const op of ops) {
      if (op.delete) { insert = false; break; }
    }
    if (insert) {
      let insertIndex = -1;
      const end = this._editor.getLength() + 1;
      for (const op of ops) {
        if (op.retain) insertIndex = op.retain;
        if (op.insert) {
          insertIndex = insertIndex > 0 ? insertIndex : end;
          const placeholder = op.insert.placeholder;
          if (placeholder) {
            placeholders.push({ placeholder, index: insertIndex });
            insertIndex++;
          } else if (typeof op.insert === 'string') {
            insertIndex += op.insert.length;
          }
        }
      }
    } else {
      let deleteIndex = -1;
      for (const op of ops) {
        if (op.retain) deleteIndex = op.retain;
        if (op.delete) {
          deleteIndex = deleteIndex > 0 ? deleteIndex : 0;
          const selected = this._getPlaceholdersInSelection(deleteIndex, op.delete);
          selected.forEach(p => placeholders.push(p));
          deleteIndex = -1;
        }
      }
    }
    if (placeholders.length) {
      const method = `_confirm${insert ? 'Insert' : 'Remove'}Placeholders`;
      this[method](placeholders, false, true);
    }
    return true;
  }

  _undoPlaceholderEvents() {
    if (!this._editor) return true;
    const stack = this._editor.history.stack;
    const undo = stack.undo[stack.undo.length - 1];
    // Quill 2: stack items have .delta (not .undo/.redo)
    if (undo && undo.delta) this._emitPlaceholderHistoryEvents(undo.delta.ops);
    return true;
  }

  _redoPlaceholderEvents() {
    if (!this._editor) return true;
    const stack = this._editor.history.stack;
    const redo = stack.redo[stack.redo.length - 1];
    if (redo && redo.delta) this._emitPlaceholderHistoryEvents(redo.delta.ops);
    return true;
  }

  _getPlaceholdersInSelection(index, length) {
    const placeholders = [];
    if (!this._editor) return placeholders;
    for (let i = index; i < index + length; i++) {
      const op = this._editor.getContents(i, 1).ops[0];
      const placeholder = (op && op.insert && op.insert.placeholder) || null;
      if (placeholder) placeholders.push({ placeholder, index: i });
    }
    return placeholders;
  }

  // ============================================================
  // Utility Methods
  // ============================================================

  /**
   * Get current Quill selection, focusing if needed.
   */
  _getSelection() {
    if (!this._editor) return null;
    return this._editor.getSelection(true);
  }

  /**
   * Set native selection to a specific node and offset.
   */
  _setSelectionNode(node, index = 0) {
    const sel = window.getSelection();
    const range = document.createRange();
    range.setStart(node, index);
    range.collapse(true);
    if (sel) {
      sel.removeAllRanges();
      sel.addRange(range);
    }
  }
}

// Register the custom element with our own tag
customElements.define('vcf-enhanced-rich-text-editor', EnhancedRichTextEditor);

export { EnhancedRichTextEditor };
