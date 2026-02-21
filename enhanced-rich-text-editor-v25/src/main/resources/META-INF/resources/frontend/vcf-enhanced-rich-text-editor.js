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

  constructor(scroll, node) {
    super(scroll, node);
    // Wrap Quill 2's guard TextNodes (\uFEFF) in named <span> elements.
    // Without wrapping, bare TextNodes with zero-width \uFEFF collapse to 0px,
    // making the caret invisible at tab positions. Wrapping in inline-block spans
    // with min-width 2px ensures reliable caret rendering.
    // TextNode object identity is preserved (reparenting, not copying), so Quill's
    // index(), restore(), and update() methods continue to work correctly.
    this._wrapGuardNodes();
  }

  _wrapGuardNodes() {
    if (this.leftGuard?.nodeType === Node.TEXT_NODE &&
        this.leftGuard.parentNode === this.domNode) {
      const w = document.createElement('span');
      w.className = 'ql-tab-guard';
      this.domNode.insertBefore(w, this.leftGuard);
      w.appendChild(this.leftGuard);
    }
    if (this.rightGuard?.nodeType === Node.TEXT_NODE &&
        this.rightGuard.parentNode === this.domNode) {
      const w = document.createElement('span');
      w.className = 'ql-tab-guard';
      this.domNode.insertBefore(w, this.rightGuard);
      w.appendChild(this.rightGuard);
    }
  }

  /**
   * Override Embed.position() for the "after tab" case (index > 0).
   * With inline-block, guard nodes render at the left edge of the tab,
   * causing the native caret to appear at the wrong X position.
   * For "after tab", we return the first text descendant of the next DOM
   * sibling, which is physically at the tab's right edge (= the tabstop).
   * This fixes caret rendering for setSelection() and typing insertion.
   */
  position(index, inclusive) {
    if (index <= 0) {
      return super.position(index, inclusive);
    }
    const nextNode = this.domNode.nextSibling;
    if (nextNode) {
      if (nextNode.nodeType === Node.TEXT_NODE) {
        return [nextNode, 0];
      }
      // Next sibling is an element (e.g., another .ql-tab) — use its first text node
      const walker = document.createTreeWalker(nextNode, NodeFilter.SHOW_TEXT);
      const firstText = walker.firstChild();
      if (firstText) {
        return [firstText, 0];
      }
    }
    // Fallback: return right guard (positioned at tab's right edge via CSS).
    // super.position() returns [parentNode, childIndex+1] which produces
    // a zero-size bounding rect when the tab is the last element in a line,
    // making the cursor invisible or stuck at the wrong position.
    if (this.rightGuard) {
      return [this.rightGuard, this.rightGuard.textContent.length];
    }
    return super.position(index, inclusive);
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

// ============================================================================
// PlaceholderBlot — Embed: <span class="ql-placeholder" data-placeholder="{...}">
// Inline token for configurable placeholder text with format/altFormat support.
// ============================================================================
class PlaceholderBlot extends Embed {
  static blotName = 'placeholder';
  static tagName = 'SPAN';
  static className = 'ql-placeholder';
  static tags = null;               // Set by _onPlaceholderTagsChanged
  static altAppearanceRegex = null;  // Set by _onPlaceholderAltAppearancePatternChanged

  static create(value) {
    const node = super.create();
    // Do NOT set contenteditable="false" on outer node — guard nodes must stay editable
    PlaceholderBlot.storeValue(node, value);
    // NOTE: Do NOT call setText here — contentNode is created by the Embed
    // constructor (which runs AFTER create()). setText is called in constructor().
    return node;
  }

  static value(node) { return PlaceholderBlot.loadValue(node); }

  static loadValue(node) {
    try { return JSON.parse(node.dataset.placeholder); }
    catch (e) { return true; }
  }

  static storeValue(node, value) {
    node.dataset.placeholder = JSON.stringify(value);
  }

  static setText(node) {
    const placeholder = PlaceholderBlot.loadValue(node);
    if (!placeholder || !placeholder.text) return;
    let text = placeholder.text;
    const contentNode = node.querySelector('[contenteditable="false"]');
    if (!contentNode) return;
    contentNode.textContent = ''; // SECURITY: never innerHTML

    if (PlaceholderBlot.altAppearanceRegex) {
      const match = new RegExp(PlaceholderBlot.altAppearanceRegex).exec(text);
      if (match) {
        const altText = match[0];
        if (placeholder.altAppearance) {
          // Alt mode: show only alt text in [alt] span
          const altSpan = document.createElement('span');
          altSpan.setAttribute('alt', '');
          altSpan.textContent = altText;
          contentNode.appendChild(altSpan);
        } else {
          // Normal with alt span embedded
          const before = text.slice(0, match.index);
          const after = text.slice(match.index + altText.length);
          let head = (PlaceholderBlot.tags ? PlaceholderBlot.tags.start : '') + before;
          let tail = after + (PlaceholderBlot.tags ? PlaceholderBlot.tags.end : '');
          contentNode.appendChild(document.createTextNode(head));
          const altSpan = document.createElement('span');
          altSpan.setAttribute('alt', '');
          altSpan.textContent = altText;
          contentNode.appendChild(altSpan);
          if (tail) contentNode.appendChild(document.createTextNode(tail));
        }
        return;
      } else if (placeholder.altAppearance) {
        return; // No match in alt mode → show nothing
      }
    }
    // Normal display: text with optional tags
    if (PlaceholderBlot.tags && !placeholder.altAppearance) {
      text = PlaceholderBlot.tags.start + text + PlaceholderBlot.tags.end;
    }
    contentNode.textContent = text;
  }

  // Apply Quill format attributes as inline styles
  static deltaToInline(node, attr) {
    if (!node) return;
    Object.keys(attr).forEach(key => {
      const value = attr[key];
      switch (key) {
        case 'bold':   node.style.fontWeight = value ? 'bold' : 'normal'; break;
        case 'italic': node.style.fontStyle = value ? 'italic' : 'normal'; break;
        case 'font':   node.style.fontFamily = value; break;
        case 'code':   PlaceholderBlot._wrapContent('code', node); break;
        case 'link':
          // SECURITY: Only allow safe URL protocols (prevent javascript: XSS)
          if (/^(https?:|mailto:)/i.test(value)) {
            PlaceholderBlot._wrapContent('a', node, [{ name: 'href', value }]);
          }
          break;
        case 'script':
          if (value === 'super') PlaceholderBlot._wrapContent('sup', node);
          else if (value === 'sub') PlaceholderBlot._wrapContent('sub', node);
          break;
        default: node.style[key] = value;
      }
    });
  }

  // SECURITY FIX: DOM methods instead of innerHTML
  static _wrapContent(tag, node, attrs = []) {
    if (!node.querySelector(tag)) {
      const el = document.createElement(tag);
      while (node.firstChild) el.appendChild(node.firstChild);
      node.appendChild(el);
      attrs.forEach(a => el.setAttribute(a.name, a.value));
    }
  }

  constructor(scroll, node) {
    super(scroll, node);
    // setText + applyFormat MUST run here (not in create()) because the Embed
    // constructor creates the contentNode and guard nodes. In create(), the
    // contentNode doesn't exist yet → querySelector returns null.
    PlaceholderBlot.setText(this.domNode);
    this.applyFormat();
  }

  applyFormat() {
    const placeholder = PlaceholderBlot.loadValue(this.domNode);
    if (!placeholder) return;
    if (placeholder.altFormat) {
      const altNode = this.domNode.querySelector('[alt]');
      if (altNode) PlaceholderBlot.deltaToInline(altNode, placeholder.altFormat);
    }
    if (placeholder.format) {
      const contentNode = this.domNode.querySelector('[contenteditable="false"]');
      if (contentNode) PlaceholderBlot.deltaToInline(contentNode, placeholder.format);
    }
  }
}

Quill.register('formats/placeholder', PlaceholderBlot, true);

// ============================================================================
// NbspBlot — Embed: <span class="ql-nbsp">&nbsp;</span>
// Non-breaking space, inserted via Shift+Space.
// ============================================================================
class NbspBlot extends Embed {
  static blotName = 'nbsp';
  static tagName = 'span';
  static className = 'ql-nbsp';

  constructor(scroll, node) {
    super(scroll, node);
    // Set NBSP on contentNode (created by Embed constructor, not available in create()).
    // Use this.contentNode — canonical Quill 2 API.
    // SECURITY: textContent instead of innerHTML.
    if (this.contentNode) {
      this.contentNode.textContent = '\u00A0';
    }
  }
}

Quill.register('formats/nbsp', NbspBlot, true);

/**
 * ERTE CSS classes to preserve in htmlValue (not stripped by __updateHtmlValue).
 * Each phase adds its classes here.
 */
const ERTE_PRESERVED_CLASSES = ['ql-readonly', 'ql-tab', 'ql-soft-break', 'ql-placeholder', 'ql-nbsp'];

/**
 * Default English labels for ERTE-specific buttons and dialogs.
 * These augment RTE 2's standard i18n. Values are used as fallback
 * when the Java side sends an i18n object without ERTE keys.
 */
const ERTE_I18N_DEFAULTS = {
  readonly: 'Readonly',
  whitespace: 'Show whitespace',
  placeholder: 'Placeholder',
  placeholderAppearance: 'Toggle placeholder appearance',
  placeholderDialogTitle: 'Placeholders',
  placeholderComboBoxLabel: 'Select a placeholder',
  placeholderAppearanceLabel1: 'Plain',
  placeholderAppearanceLabel2: 'Value',
  alignJustify: 'Justify',
};

class VcfEnhancedRichTextEditor extends RteBase {

  static get is() {
    return 'vcf-enhanced-rich-text-editor';
  }

  static get properties() {
    return {
      ...super.properties,
      tabStops: { type: Array },
      noRulers: { type: Boolean, reflect: true },
      placeholders: { type: Array },
      placeholderTags: { type: Object },
      placeholderAltAppearance: { type: Boolean },
      placeholderAltAppearancePattern: { type: String },
      showWhitespace: { type: Boolean },
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

        /* Tab stops — inline-block spans with calculated width.
           Quill 2 Embed structure after guard wrapping:
             [span.ql-tab-guard > guard \uFEFF] [contentNode span] [span.ql-tab-guard > guard \uFEFF]
           Guard TextNodes are wrapped in named <span> elements by TabBlot constructor.
           Guards need min 2px width — Chrome cannot render a caret in a 0px element.
           The \uFEFF glyph has zero advance width, so without explicit sizing the guards
           collapse to 0px. 2×2px = 4px total overhead per tab, imperceptible at typical
           tab widths (100-200px). Tab width engine sets outer .ql-tab width, so guards
           are included and ruler alignment is unaffected.
           CRITICAL: Must use inline-block, NOT inline-flex. Chrome treats inline-flex
           elements as atomic inlines for vertical navigation — ArrowUp/ArrowDown gets
           trapped in guard nodes instead of moving between lines.
           overflow:visible — tab is invisible spacing; hidden/clip clips the caret
           (Lumo line-height 1.625 makes caret ~26px, taller than 1rem box).
           No height/line-height — inherit from paragraph so caret matches text.
           No will-change/translateZ — compositor layers break caret in Chrome. */
        .ql-tab {
          display: inline-block;
          min-width: 2px;
          white-space: pre;
          vertical-align: baseline;
          position: relative;
          overflow: visible;
          cursor: default;
        }
        .ql-tab-guard {
          display: inline-block;
          min-width: 2px;
          font-size: inherit;
          line-height: inherit;
          pointer-events: none;
        }
        /* Right guard at the tab's right edge for correct caret placement.
           Without this, the right guard renders at the left edge (inline-block)
           and the cursor appears at the wrong X position after the last tab. */
        .ql-tab > .ql-tab-guard:last-child {
          position: absolute;
          right: 0;
        }
        .ql-tab > span[contenteditable="false"] {
          font-size: 0;
          line-height: 0;
          overflow: hidden;
        }

        /* Soft breaks — inline line break */
        .ql-soft-break {
          display: inline;
        }

        /* Placeholders — inline embed tokens */
        .ql-placeholder {
          background-color: var(--lumo-primary-color-10pct);
          border-radius: var(--lumo-border-radius-s);
          padding-inline: 0.125em;
          cursor: default;
        }
        .ql-placeholder [contenteditable="false"] {
          font-size: inherit;
          line-height: inherit;
        }

        /* Whitespace indicators — activated by 'show-whitespace' class on .ql-editor.
           CRITICAL: ::after on .ql-tab inherits font-size:0, overflow:hidden
           from parent. Must explicitly override to make indicators visible. */

        /* Tab indicator: → (right arrow) */
        .show-whitespace span.ql-tab::after {
          position: absolute;
          content: '→';
          right: 2px;
          top: 0;
          line-height: 1rem;
          font-size: var(--lumo-font-size-m, 1rem);
          overflow: visible;
          color: var(--lumo-contrast-40pct, rgba(0, 0, 0, 0.38));
          pointer-events: none;
        }

        /* Auto-wrap indicator: DISABLED — only triggers for tabs that wrap,
           not for text wrapping. Inconsistent behavior, so deactivated for now.
        .show-whitespace span.ql-tab.ql-auto-wrap-start::after {
          content: '⮐→';
        }
        */

        /* Soft-break indicator: ↵ (return symbol) */
        .show-whitespace span.ql-soft-break::before {
          content: '↵';
          font-size: var(--lumo-font-size-s, 0.875rem);
          color: var(--lumo-contrast-40pct, rgba(0, 0, 0, 0.38));
          vertical-align: baseline;
          pointer-events: none;
        }

        /* Paragraph/Hard-break indicator: ¶ (pilcrow) */
        .show-whitespace p:not(:last-child),
        .show-whitespace h1:not(:last-child),
        .show-whitespace h2:not(:last-child),
        .show-whitespace h3:not(:last-child),
        .show-whitespace li:not(:last-child),
        .show-whitespace blockquote:not(:last-child) {
          position: relative;
        }

        .show-whitespace p:not(:last-child)::after,
        .show-whitespace h1:not(:last-child)::after,
        .show-whitespace h2:not(:last-child)::after,
        .show-whitespace h3:not(:last-child)::after,
        .show-whitespace li:not(:last-child)::after,
        .show-whitespace blockquote:not(:last-child)::after {
          content: '¶';
          position: absolute;
          bottom: 0;
          font-size: var(--lumo-font-size-s, 0.875rem);
          color: var(--lumo-contrast-30pct, rgba(0, 0, 0, 0.26));
          pointer-events: none;
          margin-left: 2px;
        }

        /* NBSP indicator: · (middle dot) */
        .show-whitespace span.ql-nbsp::before {
          content: '·';
          font-size: var(--lumo-font-size-m, 1rem);
          color: var(--lumo-contrast-40pct, rgba(0, 0, 0, 0.38));
          vertical-align: middle;
          pointer-events: none;
          margin-right: 2px;
        }

        /* Justify button icon — RTE 2 base styles define icons for align-left/center/right
           but not justify. Using SVG mask-image matching the Vaadin iconset icon
           (vaadin:align-justify: 4 horizontal lines of equal length). */
        [part~='toolbar-button-align-justify']::before {
          -webkit-mask-image: url('data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 16 16"><path d="M0 0h16v3h-16v-3z"></path><path d="M0 4h16v3h-16v-3z"></path><path d="M0 12h16v3h-16v-3z"></path><path d="M0 8h16v3h-16v-3z"></path></svg>');
          mask-image: url('data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 16 16"><path d="M0 0h16v3h-16v-3z"></path><path d="M0 4h16v3h-16v-3z"></path><path d="M0 12h16v3h-16v-3z"></path><path d="M0 8h16v3h-16v-3z"></path></svg>');
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

  // ==========================================================================
  // I18n: extend RTE 2's i18n with ERTE-specific labels
  // ==========================================================================

  get i18n() { return super.i18n; }

  set i18n(value) {
    super.i18n = value;
    this._applyErteI18n();
  }

  /**
   * Applies ERTE-specific i18n labels to custom toolbar buttons and the
   * placeholder dialog. Reads from __effectiveI18n (populated by I18nMixin
   * after the setter), falls back to ERTE_I18N_DEFAULTS.
   * @protected
   */
  _applyErteI18n() {
    const i18n = this.__effectiveI18n || {};
    const d = ERTE_I18N_DEFAULTS;

    // Toolbar buttons
    if (this.__readonlyButton) {
      this.__readonlyButton.setAttribute('aria-label', i18n.readonly || d.readonly);
    }
    if (this.__whitespaceBtn) {
      this.__whitespaceBtn.setAttribute('aria-label', i18n.whitespace || d.whitespace);
    }
    if (this.__placeholderBtn) {
      this.__placeholderBtn.setAttribute('aria-label', i18n.placeholder || d.placeholder);
    }
    if (this.__placeholderAppearanceBtn) {
      this.__placeholderAppearanceBtn.setAttribute('aria-label',
        i18n.placeholderAppearance || d.placeholderAppearance);
    }
    if (this.__justifyButton) {
      this.__justifyButton.setAttribute('aria-label', i18n.alignJustify || d.alignJustify);
    }

    // Placeholder dialog
    if (this.__placeholderDialog) {
      this.__placeholderDialog.header = i18n.placeholderDialogTitle || d.placeholderDialogTitle;
      this.__placeholderDialog.setAttribute('aria-label',
        i18n.placeholderDialogTitle || d.placeholderDialogTitle);
    }
    if (this.__placeholderComboBox) {
      this.__placeholderComboBox.label = i18n.placeholderComboBoxLabel || d.placeholderComboBoxLabel;
    }

    // Store appearance labels for use in placeholderAltAppearance setter
    const label1 = i18n.placeholderAppearanceLabel1 || d.placeholderAppearanceLabel1;
    const label2 = i18n.placeholderAppearanceLabel2 || d.placeholderAppearanceLabel2;
    this.__erteI18nLabels = { label1, label2 };

    // Update appearance button text if it exists
    if (this.__placeholderAppearanceBtn) {
      this.__placeholderAppearanceBtn.textContent =
        this.placeholderAltAppearance ? label2 : label1;
    }
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
    // --- Extension hooks (V25 API) ---
    const extNs = window.Vaadin?.Flow?.vcfEnhancedRichTextEditor;
    if (extNs && Array.isArray(extNs.extendQuill)) {
      extNs.extendQuill.forEach(cb => cb(Quill));
    }
    if (extNs && Array.isArray(extNs.extendOptions)) {
      console.warn(
        '[ERTE] extendOptions is deprecated in V25. Use extendQuill (pre-init) ' +
        'and/or extendEditor (post-init) instead.'
      );
    }

    super.ready();
    this._injectToolbarSlots();
    this._injectStandardButtonSlots();
    this._injectJustifyButton();
    this._injectReadonlyButton();
    this._initReadonlyProtection();
    this._initPlaceholderDialog();
    this._injectPlaceholderButtons();
    this._injectWhitespaceButton();

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

    // Placeholder property observers
    this._createPropertyObserver('placeholders', '_onPlaceholdersChanged');
    this._createPropertyObserver('placeholderTags', '_onPlaceholderTagsChanged');
    this._createPropertyObserver('placeholderAltAppearance', '_onPlaceholderAltAppearanceChanged');
    this._createPropertyObserver('placeholderAltAppearancePattern', '_onPlaceholderAltAppearancePatternChanged');

    // Trigger placeholder observers if set before ready()
    if (this.placeholders) this._onPlaceholdersChanged(this.placeholders);
    if (this.placeholderTags) this._onPlaceholderTagsChanged(this.placeholderTags);
    if (this.placeholderAltAppearancePattern) this._onPlaceholderAltAppearancePatternChanged(this.placeholderAltAppearancePattern);

    // Whitespace indicator property observer
    this._createPropertyObserver('showWhitespace', '_showWhitespaceChanged');
    if (this.showWhitespace) this._showWhitespaceChanged(this.showWhitespace);

    // Recalculate tab widths on every text change
    this._editor.on('text-change', () => this._requestTabUpdate());

    // Recalculate tab widths on editor resize
    new ResizeObserver(() => this._requestTabUpdate()).observe(this._editor.root);

    // Clear dialog-just-closed flag on any editor interaction, and handle
    // placeholder clicks. Clicking on a placeholder's contenteditable="false"
    // contentNode does NOT trigger a Quill selection-change, so we intercept
    // mousedown, preventDefault the browser's non-editable handling, and
    // manually set the Quill selection so placeholder-select fires properly.
    this._editor.root.addEventListener('mousedown', (e) => {
      this._dialogJustClosed = false;
      const phEl = e.target.closest('.ql-placeholder');
      if (phEl) {
        e.preventDefault();
        const blot = Quill.find(phEl);
        if (blot) {
          const index = this._editor.getIndex(blot);
          // Defer setSelection to after mousedown processing completes,
          // so the Quill selection-change and resulting placeholder-select
          // CustomEvent fire outside the browser's mouse event chain.
          setTimeout(() => this._editor.setSelection(index + 1, 0, Quill.sources.USER), 0);
        }
      }
    });

    // Placeholder selection tracking
    this._editor.on('selection-change', (range) => {
      if (!range) return;
      const placeholders = this._getSelectedPlaceholders(range);
      if (placeholders.length) {
        if (!this._inPlaceholder) {
          this._inPlaceholder = true;
          if (this.__placeholderBtn) {
            this.__placeholderBtn.classList.add('ql-active');
            this.__placeholderBtn.part.add('toolbar-button-pressed');
          }
          this.dispatchEvent(new CustomEvent('placeholder-select', {
            bubbles: true, composed: true, cancelable: false,
            detail: { placeholders }
          }));
        }
      } else {
        if (this._inPlaceholder) {
          this._inPlaceholder = false;
          if (this.__placeholderBtn) {
            this.__placeholderBtn.classList.remove('ql-active');
            this.__placeholderBtn.part.remove('toolbar-button-pressed');
          }
          this.dispatchEvent(new CustomEvent('placeholder-leave', {
            bubbles: true, composed: true, cancelable: false
          }));
        }
      }
    });

    // --- Extension hooks: post-init (V25 API) ---
    if (extNs && Array.isArray(extNs.extendEditor)) {
      extNs.extendEditor.forEach(cb => cb(this._editor, Quill));
    }

    // Apply ERTE i18n labels to custom buttons/dialog (initial defaults)
    this._applyErteI18n();

    // SPIKE: Test Aura Style Proxy (Phase 3.4j)
    this._injectAuraStyleProxySpike();

    console.debug('[ERTE] ready, _editor:', !!this._editor, 'readonly protection active, tab engine initialized');
  }

  /**
   * SPIKE: Aura Style Proxy - scans document stylesheets and clones RTE rules for ERTE.
   * Part of Phase 3.4j spike investigation.
   * @private
   */
  _injectAuraStyleProxySpike() {
    console.group('ERTE Spike: Aura Style Proxy');

    // Guard: avoid duplicate injection
    if (document.querySelector('style[data-vcf-erte-spike-proxy]')) {
      console.log('Proxy already injected, skipping.');
      console.groupEnd();
      return;
    }

    const startTime = performance.now();
    const stats = {
      totalSheets: 0,
      scannedSheets: 0,
      corsSkipped: 0,
      totalRules: 0,
      matchedRules: 0,
    };

    const rteRules = [];

    // Scan all stylesheets
    for (const sheet of document.styleSheets) {
      stats.totalSheets++;

      try {
        stats.scannedSheets++;
        const rules = Array.from(sheet.cssRules || []);
        stats.totalRules += rules.length;

        for (const rule of rules) {
          if (rule.cssText && rule.cssText.includes('vaadin-rich-text-editor')) {
            rteRules.push({
              original: rule.cssText,
              sheet: sheet.href || '(inline)',
            });
            stats.matchedRules++;
          }
        }
      } catch (e) {
        // CORS-protected sheet
        stats.corsSkipped++;
        console.warn('Skipped stylesheet (CORS):', sheet.href, e.message);
      }
    }

    if (rteRules.length === 0) {
      console.log('No RTE-specific rules found.');
      console.groupEnd();
      return;
    }

    // Clone rules with ERTE tag (CRITICAL: negative lookbehind to avoid CSS var collision)
    const proxyStyles = rteRules.map(({ original }) =>
      original.replace(/(?<!--)vaadin-rich-text-editor/g, 'vcf-enhanced-rich-text-editor')
    );

    // Inject into document
    const styleEl = document.createElement('style');
    styleEl.setAttribute('data-vcf-erte-spike-proxy', '');
    styleEl.textContent = [
      '/* ERTE Aura Style Proxy (SPIKE) */',
      `/* Generated: ${new Date().toISOString()} */`,
      `/* Cloned ${stats.matchedRules} rules */`,
      '',
      ...proxyStyles,
    ].join('\n');
    document.head.appendChild(styleEl);

    const endTime = performance.now();
    const duration = (endTime - startTime).toFixed(2);

    // Log stats
    console.table(stats);
    console.log(`Duration: ${duration}ms`);
    console.log('Sample cloned rules:', proxyStyles.slice(0, 3));
    console.log('Full injected stylesheet:', styleEl.textContent.substring(0, 500) + '...');
    console.groupEnd();

    // Write debug output to page
    this._writeSpikeDebugOutput(stats, duration, rteRules.length);
  }

  /**
   * SPIKE: Writes spike debug output to the page (if debug div exists).
   * Part of Phase 3.4j spike investigation.
   * @private
   */
  _writeSpikeDebugOutput(stats, duration, clonedCount) {
    const debugDiv = document.getElementById('debug-output');
    if (!debugDiv) return;

    debugDiv.innerHTML = `
      <h3>Spike Results</h3>
      <ul>
        <li><strong>Total Stylesheets:</strong> ${stats.totalSheets}</li>
        <li><strong>Scanned:</strong> ${stats.scannedSheets}</li>
        <li><strong>CORS Skipped:</strong> ${stats.corsSkipped}</li>
        <li><strong>Total Rules:</strong> ${stats.totalRules}</li>
        <li><strong>Matched RTE Rules:</strong> ${stats.matchedRules}</li>
        <li><strong>Cloned Rules:</strong> ${clonedCount}</li>
        <li><strong>Duration:</strong> ${duration}ms</li>
      </ul>
      <p><em>Check browser console for full details.</em></p>
    `;
  }

  /**
   * Override focus() to delegate to Quill's internal focus mechanism.
   * The inherited HTMLElement focus() only focuses the web component element,
   * but does NOT focus the Quill editor (cursor remains invisible, typing doesn't work).
   * This override properly delegates to Quill, fixing the focus() API.
   * @public
   */
  focus() {
    if (this._editor) {
      this._editor.focus();
    } else {
      super.focus();
    }
  }

  /**
   * Returns all focusable elements in the toolbar, including custom slotted
   * components. Broader than RTE 2's _toolbarButtons (which only queries buttons).
   * @protected
   */
  get _toolbarFocusableElements() {
    const toolbar = this.shadowRoot?.querySelector('[part="toolbar"]');
    if (!toolbar) return [];

    // Walk the toolbar's children in visual/DOM order to build the elements list.
    // This ensures slotted elements appear at the correct position, not at the end.
    const elements = [];

    const collectFocusable = (node) => {
      // If this is a slot, collect its assigned elements
      if (node.tagName === 'SLOT') {
        const assigned = node.assignedElements({ flatten: true });
        for (const el of assigned) {
          elements.push(el);
        }
        return;
      }

      // If this is a focusable element itself, add it
      const selector = 'button, [tabindex], a[href], vaadin-text-field, vaadin-text-area, vaadin-combo-box';
      if (node.matches && node.matches(selector)) {
        elements.push(node);
      }

      // Recurse into children (for groups, spans, etc.)
      if (node.children) {
        for (const child of node.children) {
          collectFocusable(child);
        }
      }
    };

    // Start recursion from toolbar root
    for (const child of toolbar.children) {
      collectFocusable(child);
    }

    // Filter by visibility and enabled state
    // For Vaadin components, check if they or their children are disabled
    return elements.filter(el => {
      if (el.clientHeight === 0) return false;
      if (el.disabled || el.hasAttribute('disabled')) return false;
      // Vaadin components might delegate to internal button - check that too
      if (el.shadowRoot) {
        const internalButton = el.shadowRoot.querySelector('button');
        if (internalButton && (internalButton.disabled || internalButton.hasAttribute('disabled'))) {
          return false;
        }
      }
      return true;
    });
  }

  /**
   * Returns true if the element consumes arrow keys for internal navigation
   * (input fields, textareas, combo-boxes, etc.). These elements should NOT
   * participate in toolbar arrow-key navigation.
   * @private
   */
  _isArrowKeyConsumer(element) {
    if (!element) return false;

    // Native input elements that use arrow keys
    if (element.matches('input[type="text"], input[type="search"], input[type="email"], input[type="url"], input[type="tel"], input[type="number"], textarea')) {
      return true;
    }

    // Vaadin components that wrap inputs
    if (element.matches('vaadin-text-field, vaadin-text-area, vaadin-email-field, vaadin-number-field, vaadin-integer-field, vaadin-password-field, vaadin-combo-box, vaadin-select')) {
      return true;
    }

    // Contenteditable elements
    if (element.getAttribute('contenteditable') === 'true') {
      return true;
    }

    // Check for focusElement property (Vaadin pattern for internal focus delegation)
    if (element.focusElement && element.focusElement !== element) {
      return this._isArrowKeyConsumer(element.focusElement);
    }

    return false;
  }

  /**
   * Tracks the index of the currently focused toolbar element in the
   * _toolbarFocusableElements list. Used by roving tabindex management.
   * @private
   */
  _currentFocusedToolbarIndex = 0;

  /**
   * Resets all toolbar elements to tabindex="-1", enforcing the roving
   * tabindex invariant that exactly one element has tabindex="0" at a time.
   * @param {Element[]} elements - The toolbar focusable elements list
   * @private
   */
  _resetAllTabindex(elements) {
    for (const el of elements) {
      el.setAttribute('tabindex', '-1');
    }
  }

  /**
   * Unified focus management for toolbar elements. Handles:
   * - Resetting all tabindex to "-1" (roving tabindex invariant)
   * - Paint synchronization (fixes Issue 1: duplicate focus indicators)
   * - Vaadin focus delegation (focusElement property)
   * - Index tracking (_currentFocusedToolbarIndex)
   *
   * @param {number} index - Target index in _toolbarFocusableElements
   * @private
   */
  _focusToolbarElement(index) {
    const elements = this._toolbarFocusableElements;
    if (!elements.length || index < 0 || index >= elements.length) return;

    // 1. Reset ALL tabindex to -1 (prevents Issue 2: stale tabindex state)
    this._resetAllTabindex(elements);

    // 2. Force paint sync on previous element (fixes Issue 1: duplicate :focus-visible)
    // Reading offsetHeight forces the browser to recompute styles BEFORE we set
    // focus on the next element, ensuring the previous element's :focus-visible clears.
    const prevIndex = this._currentFocusedToolbarIndex;
    if (prevIndex >= 0 && prevIndex < elements.length && prevIndex !== index) {
      const prev = elements[prevIndex];
      if (prev.focusElement && prev.focusElement !== prev) {
        prev.focusElement.blur();
      }
      prev.blur();
      void prev.offsetHeight; // Force style recalc
    }

    // 3. Set target tabindex and focus
    const target = elements[index];
    target.setAttribute('tabindex', '0');

    if (target.focusElement && target.focusElement !== target) {
      target.focusElement.focus();
    } else {
      target.focus();
    }

    // 4. Track current index
    this._currentFocusedToolbarIndex = index;
  }

  /**
   * Finds the index of the currently focused element in the toolbar elements list.
   * Handles shadow DOM boundaries and Vaadin focus delegation.
   *
   * @param {Element[]} elements - The toolbar focusable elements list
   * @param {Event} e - The keyboard event (for composedPath)
   * @returns {number} Index of the focused element, or -1 if not found
   * @private
   */
  _findCurrentIndex(elements, e) {
    // 1. Check composedPath() - works for shadow DOM event targets
    if (e) {
      const path = e.composedPath();
      for (const pathElement of path) {
        const idx = elements.indexOf(pathElement);
        if (idx !== -1) return idx;
      }
    }

    // 2. Check if any element contains document.activeElement
    // Handles Vaadin components with focus delegation
    const active = document.activeElement;
    if (active) {
      const directIdx = elements.indexOf(active);
      if (directIdx !== -1) return directIdx;

      for (let i = 0; i < elements.length; i++) {
        if (elements[i].contains(active)) return i;
      }
    }

    // 3. Check shadowRoot.activeElement
    const shadowActive = this.shadowRoot?.activeElement;
    if (shadowActive) {
      const shadowIdx = elements.indexOf(shadowActive);
      if (shadowIdx !== -1) return shadowIdx;

      for (let i = 0; i < elements.length; i++) {
        if (elements[i].contains(shadowActive)) return i;
      }
    }

    // 4. Fall back to tracked index
    return this._currentFocusedToolbarIndex;
  }

  /**
   * Override RTE 2's _addToolbarListeners() to:
   * 1. Include ALL focusable elements (not just buttons) — supports custom components
   * 2. Respect arrow-key consumers (inputs, etc.) — don't preventDefault for those
   *
   * RTE 2 captures _toolbarButtons in a closure at init time, which becomes stale
   * after ERTE injects custom buttons. ERTE reads dynamically on each keydown.
   * @private
   */
  _addToolbarListeners() {
    const toolbar = this._toolbar;

    // Initial tabindex setup — roving tabindex: only first element is tabbable
    const initialElements = this._toolbarFocusableElements;
    this._resetAllTabindex(initialElements);
    if (initialElements.length > 0) {
      initialElements[0].setAttribute('tabindex', '0');
    }
    this._currentFocusedToolbarIndex = 0;

    toolbar.addEventListener('keydown', (e) => {
      // Arrow key navigation
      if ([37, 39].indexOf(e.keyCode) > -1) {
        // Check if target consumes arrow keys (TextField, Input, etc.)
        if (this._isArrowKeyConsumer(e.target)) {
          return;
        }

        e.preventDefault();

        const elements = this._toolbarFocusableElements;
        const currentIndex = this._findCurrentIndex(elements, e);
        if (currentIndex === -1) return;

        const step = e.keyCode === 39 ? 1 : -1;
        const nextIndex = (elements.length + currentIndex + step) % elements.length;
        this._focusToolbarElement(nextIndex);
      }

      // Esc and Tab focuses the content
      if (e.keyCode === 27 || (e.key === 'Tab' && !e.shiftKey)) {
        e.preventDefault();
        this._editor.focus();
      }
    });

    // Mousedown handler — sync tracked index when user clicks toolbar elements
    toolbar.addEventListener('mousedown', (e) => {
      const elements = this._toolbarFocusableElements;
      const path = e.composedPath();
      for (const pathElement of path) {
        const idx = elements.indexOf(pathElement);
        if (idx !== -1) {
          this._markToolbarFocused();
          // Sync roving tabindex state to clicked element
          this._resetAllTabindex(elements);
          elements[idx].setAttribute('tabindex', '0');
          this._currentFocusedToolbarIndex = idx;
          break;
        }
      }
    });
  }

  /**
   * Override RTE 2's __patchKeyboard() to use _toolbarFocusableElements
   * (includes slotted components) instead of just shadow DOM buttons.
   * Fixes Issue 3: Shift+Tab now focuses the FIRST toolbar element in
   * visual order, whether it's a shadow DOM button or a slotted component.
   * @private
   */
  __patchKeyboard() {
    const focusToolbar = () => {
      this._markToolbarFocused();
      this._focusToolbarElement(0);
    };

    const keyboard = this._editor.keyboard;
    keyboard.addBinding({ key: 'Tab', shiftKey: true, handler: focusToolbar });
    keyboard.addBinding({ key: 'F10', altKey: true, handler: focusToolbar });
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

  /**
   * Injects icon slots into standard toolbar buttons for icon replacement.
   * Each button gets an unnamed <slot> prepended as its first child, allowing
   * Java-side Icon components with slot="button-name" to replace the default
   * SVG icon (rendered via CSS ::before pseudo-element).
   * @protected
   */
  _injectStandardButtonSlots() {
    const toolbar = this.shadowRoot.querySelector('[part="toolbar"]');
    if (!toolbar) return;

    // Query all standard toolbar buttons by part attribute pattern
    const buttons = toolbar.querySelectorAll('[part*="toolbar-button-"]');

    for (const btn of buttons) {
      // Extract button name from part attribute (e.g., "toolbar-button-bold" → "bold")
      const partAttr = btn.getAttribute('part');
      if (!partAttr) continue;

      const match = partAttr.match(/toolbar-button-([a-z0-9-]+)/);
      if (!match) continue;

      const buttonName = match[1];

      // Create slot element with name matching button name
      const slot = document.createElement('slot');
      slot.setAttribute('name', buttonName);

      // Prepend slot as first child so slotted content appears before ::before icon
      btn.prepend(slot);
    }
  }

  /**
   * Injects the justify button into the alignment toolbar group.
   * RTE 2 only provides left/center/right; ERTE adds justify.
   * Quill 2 supports align:justify natively (already in RTE 2's whitelist).
   * @protected
   */
  _injectJustifyButton() {
    const toolbar = this.shadowRoot?.querySelector('[part="toolbar"]');
    if (!toolbar) return;

    const alignGroup = toolbar.querySelector('[part~="toolbar-group-alignment"]');
    if (!alignGroup) return;

    // Create justify button matching RTE 2 button structure
    const btn = document.createElement('button');
    btn.type = 'button';
    btn.className = 'ql-align';
    btn.value = 'justify';
    btn.setAttribute('part', 'toolbar-button toolbar-button-align-justify');
    btn.setAttribute('aria-label', this.__effectiveI18n?.alignJustify || ERTE_I18N_DEFAULTS.alignJustify);

    // Manually bind click handler since button is added after toolbar module init
    // Quill's toolbar module only binds buttons that exist at initialization time
    btn.addEventListener('click', (e) => {
      e.preventDefault();
      const range = this._editor.getSelection(true);
      const format = this._editor.getFormat(range);
      const value = format.align === 'justify' ? false : 'justify';
      this._editor.format('align', value, Quill.sources.USER);
      // Update button active state (Quill's toolbar module does this for bound buttons)
      if (value === 'justify') {
        btn.classList.add('ql-active');
      } else {
        btn.classList.remove('ql-active');
      }
    });

    // Update button active state on selection change
    this._editor.on('selection-change', (range) => {
      if (!range) return;
      const format = this._editor.getFormat(range);
      if (format.align === 'justify') {
        btn.classList.add('ql-active');
      } else {
        btn.classList.remove('ql-active');
      }
    });

    // Insert after the align-right button (last in group)
    const rightBtn = alignGroup.querySelector('[part~="toolbar-button-align-right"]');
    if (rightBtn && rightBtn.nextSibling) {
      alignGroup.insertBefore(btn, rightBtn.nextSibling);
    } else {
      alignGroup.appendChild(btn);
    }

    // Inject slot for icon replacement (follows ERTE pattern)
    const slot = document.createElement('slot');
    slot.name = 'toolbar-button-align-justify';
    btn.appendChild(slot);

    // Store reference for i18n updates
    this.__justifyButton = btn;
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
  // Whitespace indicators: toolbar button + toggle
  // ==========================================================================

  /**
   * Injects a whitespace indicator toggle button into the toolbar,
   * placed in the format group before the readonly button.
   * @protected
   */
  _injectWhitespaceButton() {
    const toolbar = this.shadowRoot.querySelector('[part="toolbar"]');
    if (!toolbar) return;

    const btn = document.createElement('button');
    btn.type = 'button';
    btn.setAttribute('part', 'toolbar-button toolbar-button-whitespace');
    btn.setAttribute('aria-label', 'Show whitespace');
    btn.addEventListener('click', () => this._onWhitespaceClick());

    // Pilcrow icon — inline SVG
    btn.innerHTML = `<svg viewBox="0 0 24 24" width="1em" height="1em" style="fill:currentColor">
      <text x="12" y="19" text-anchor="middle" font-family="serif" font-size="20" font-weight="bold">¶</text>
    </svg>`;

    // Place in format group, before readonly button
    const formatGroup = toolbar.querySelector('[part~="toolbar-group-format"]');
    if (formatGroup) {
      const readonlyBtn = formatGroup.querySelector('[part~="toolbar-button-readonly"]');
      if (readonlyBtn) {
        formatGroup.insertBefore(btn, readonlyBtn);
      } else {
        formatGroup.appendChild(btn);
      }
    } else {
      const fileInput = toolbar.querySelector('#fileInput');
      toolbar.insertBefore(btn, fileInput || null);
    }

    this.__whitespaceBtn = btn;
  }

  /**
   * Toggle whitespace indicators on/off.
   * @protected
   */
  _onWhitespaceClick() {
    this.showWhitespace = !this.showWhitespace;
  }

  /**
   * Property observer for showWhitespace — toggles CSS class and button state.
   * @protected
   */
  _showWhitespaceChanged(show) {
    const editor = this._editor?.root;
    if (editor) editor.classList.toggle('show-whitespace', !!show);
    if (this.__whitespaceBtn) {
      this.__whitespaceBtn.classList.toggle('ql-active', !!show);
    }
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
  // Placeholder: dialog
  // ==========================================================================

  /**
   * Creates the placeholder confirm-dialog + combo-box programmatically
   * in the shadow DOM. NOT in render() — updatability rule.
   * @protected
   */
  _initPlaceholderDialog() {
    const dialog = document.createElement('vaadin-confirm-dialog');
    dialog.header = 'Placeholders';
    dialog.setAttribute('aria-label', 'Placeholders');

    const comboBox = document.createElement('vaadin-combo-box');
    comboBox.setAttribute('item-label-path', 'text');
    comboBox.setAttribute('item-value-path', 'text');
    comboBox.label = 'Select a placeholder';
    comboBox.style.width = '100%';
    comboBox.clearButtonVisible = true;
    dialog.appendChild(comboBox);

    const okBtn = document.createElement('vaadin-button');
    okBtn.setAttribute('slot', 'confirm-button');
    okBtn.setAttribute('theme', 'primary');
    okBtn.textContent = 'OK';
    okBtn.addEventListener('click', () => this._onPlaceholderEditConfirm());
    dialog.appendChild(okBtn);

    const removeBtn = document.createElement('vaadin-button');
    removeBtn.setAttribute('slot', 'reject-button');
    removeBtn.setAttribute('theme', 'error');
    removeBtn.textContent = 'Remove';
    removeBtn.hidden = true;
    removeBtn.addEventListener('click', () => this._onPlaceholderEditRemove());
    dialog.appendChild(removeBtn);

    const cancelBtn = document.createElement('vaadin-button');
    cancelBtn.setAttribute('slot', 'cancel-button');
    cancelBtn.textContent = 'Cancel';
    cancelBtn.addEventListener('click', () => this._onPlaceholderEditCancel());
    dialog.appendChild(cancelBtn);

    // Handle external close (Escape, overlay click)
    dialog.addEventListener('opened-changed', (e) => {
      if (!e.detail.value && this.__placeholderDialog) {
        this._closePlaceholderDialog();
      }
    });

    this.shadowRoot.appendChild(dialog);
    this.__placeholderDialog = dialog;
    this.__placeholderComboBox = comboBox;
    this.__placeholderRemoveBtn = removeBtn;
  }

  set _placeholderEditing(v) {
    if (this.__placeholderDialog) this.__placeholderDialog.opened = v;
  }
  get _placeholderEditing() {
    return this.__placeholderDialog?.opened || false;
  }

  // ==========================================================================
  // Placeholder: toolbar buttons
  // ==========================================================================

  /**
   * Injects two placeholder buttons into the format group:
   * 1. Placeholder insert button (@ symbol)
   * 2. Appearance toggle button (Plain/Value)
   * @protected
   */
  _injectPlaceholderButtons() {
    const toolbar = this.shadowRoot.querySelector('[part="toolbar"]');
    if (!toolbar) return;

    // Placeholder insert button
    const btn = document.createElement('button');
    btn.type = 'button';
    btn.setAttribute('part', 'toolbar-button toolbar-button-placeholder');
    btn.setAttribute('aria-label', 'Placeholder');
    btn.textContent = '@';
    btn.hidden = true;
    btn.addEventListener('click', () => this._onPlaceholderClick());

    // Appearance toggle button
    const appBtn = document.createElement('button');
    appBtn.type = 'button';
    appBtn.setAttribute('part', 'toolbar-button toolbar-button-placeholder-display');
    appBtn.setAttribute('aria-label', 'Toggle placeholder appearance');
    appBtn.textContent = 'Plain';
    appBtn.hidden = true;
    appBtn.addEventListener('click', () => {
      this.placeholderAltAppearance = !this.placeholderAltAppearance;
    });

    // Place in format group, before the readonly button
    const formatGroup = toolbar.querySelector('[part~="toolbar-group-format"]');
    if (formatGroup) {
      const readonlyBtn = formatGroup.querySelector('[part~="toolbar-button-readonly"]');
      if (readonlyBtn) {
        formatGroup.insertBefore(btn, readonlyBtn);
        formatGroup.insertBefore(appBtn, readonlyBtn);
      } else {
        formatGroup.appendChild(btn);
        formatGroup.appendChild(appBtn);
      }
    }

    this.__placeholderBtn = btn;
    this.__placeholderAppearanceBtn = appBtn;
  }

  // ==========================================================================
  // Placeholder: property observers
  // ==========================================================================

  /** @protected */
  _onPlaceholdersChanged(placeholders) {
    if (!placeholders || !Array.isArray(placeholders)) return;
    if (this.__placeholderBtn) {
      this.__placeholderBtn.hidden = !placeholders.length;
    }
    if (this.__placeholderAppearanceBtn) {
      this.__placeholderAppearanceBtn.hidden = !(placeholders.length && this.placeholderAltAppearancePattern);
    }
    if (this.__placeholderComboBox && placeholders.length) {
      this.__placeholderComboBox.items = placeholders.map(p => this._getPlaceholderOptions(p));
    }
  }

  /** @protected */
  _onPlaceholderTagsChanged(tags) {
    PlaceholderBlot.tags = tags;
    // Update button text to show the start tag
    if (this.__placeholderBtn && tags && tags.start) {
      this.__placeholderBtn.textContent = tags.start;
    }
    // Re-render existing placeholders with new tags
    if (this._editor) {
      this._editor.setContents(this._editor.getContents(), Quill.sources.SILENT);
    }
  }

  /** @protected */
  _onPlaceholderAltAppearanceChanged(altAppearance) {
    if (!this._editor) return;

    // Update button label (use i18n labels if available)
    if (this.__placeholderAppearanceBtn) {
      const labels = this.__erteI18nLabels || ERTE_I18N_DEFAULTS;
      this.__placeholderAppearanceBtn.textContent = altAppearance ? labels.label2 : labels.label1;
      if (altAppearance) {
        this.__placeholderAppearanceBtn.classList.add('ql-active');
      } else {
        this.__placeholderAppearanceBtn.classList.remove('ql-active');
      }
    }

    // Update delta: set altAppearance on each placeholder op
    const delta = this._editor.getContents();
    let changed = false;
    delta.ops.forEach(op => {
      if (typeof op.insert === 'object' && op.insert.placeholder) {
        op.insert.placeholder.altAppearance = altAppearance;
        changed = true;
      }
    });
    if (changed) {
      this._editor.setContents(delta, Quill.sources.SILENT);
    }

    // Fire appearance-change event (use i18n labels)
    const labels = this.__erteI18nLabels || ERTE_I18N_DEFAULTS;
    const appearanceLabel = altAppearance ? labels.label2 : labels.label1;
    this.dispatchEvent(new CustomEvent('placeholder-appearance-change', {
      bubbles: true, composed: true, cancelable: false,
      detail: { altAppearance, appearanceLabel }
    }));
  }

  /** @protected */
  _onPlaceholderAltAppearancePatternChanged(regex) {
    PlaceholderBlot.altAppearanceRegex = regex;
    if (this.__placeholderAppearanceBtn) {
      this.__placeholderAppearanceBtn.hidden = !(this.placeholders && this.placeholders.length && regex);
    }
  }

  // ==========================================================================
  // Placeholder: dialog logic
  // ==========================================================================

  /** @protected */
  _onPlaceholderClick() {
    const range = this._editor.getSelection(true);
    if (!range) return;

    // After dialog close, skip placeholder detection to allow consecutive inserts.
    // Without this, cursor at index+1 after a fresh insert is falsely detected as
    // "on a placeholder" (getContents(index-1) returns the just-inserted embed).
    // Flag is cleared on editor mousedown (user clicked somewhere in the editor).
    const justClosed = this._dialogJustClosed;
    this._dialogJustClosed = false;
    const placeholder = justClosed ? null : this.selectedPlaceholder;
    const detail = { position: range.index };

    if (placeholder && placeholder.text) {
      // Editing existing placeholder
      this.__placeholderRemoveBtn.hidden = false;
      this.__placeholderDialog.rejectButtonVisible = true;
      this._placeholderRange = { index: range.index - 1, length: 1 };
      if (this.__placeholderComboBox) {
        this.__placeholderComboBox.value = placeholder.text;
      }
    } else {
      // New placeholder insert
      this.__placeholderRemoveBtn.hidden = true;
      this.__placeholderDialog.rejectButtonVisible = false;
      this._insertPlaceholderIndex = range.index;
      if (this.__placeholderComboBox) {
        this.__placeholderComboBox.value = '';
      }
    }

    const event = new CustomEvent('placeholder-button-click', {
      bubbles: true, composed: true, cancelable: true, detail
    });
    const cancelled = !this.dispatchEvent(event);
    if (!cancelled) {
      this._placeholderEditing = true;
    }
  }

  /** @protected */
  _onPlaceholderEditConfirm() {
    const value = this.__placeholderComboBox ? this.__placeholderComboBox.value : '';
    if (!value) {
      this._closePlaceholderDialog();
      return;
    }
    const placeholder = this._getPlaceholderOptions(value);
    if (this._insertPlaceholderIndex != null) {
      this._insertPlaceholders([{ placeholder, index: this._insertPlaceholderIndex }]);
    } else if (this._placeholderRange) {
      // Update: remove old, insert new
      this._confirmRemovePlaceholders([placeholder], true);
      this._confirmInsertPlaceholders([{ placeholder, index: this._placeholderRange.index }]);
    }
    this._closePlaceholderDialog();
  }

  /** @protected */
  _onPlaceholderEditCancel() {
    this._closePlaceholderDialog();
  }

  /** @protected */
  _onPlaceholderEditRemove() {
    this._removePlaceholders();
    this._closePlaceholderDialog();
  }

  /** @protected */
  _insertPlaceholders(placeholders) {
    const detail = { placeholders: placeholders.map(i => i.placeholder || i) };
    const event = new CustomEvent('placeholder-before-insert', {
      bubbles: true, composed: true, cancelable: true, detail
    });
    this._insertPlaceholdersList = placeholders;
    const cancelled = !this.dispatchEvent(event);
    if (!cancelled) {
      this._confirmInsertPlaceholders(placeholders);
    }
  }

  /** @protected */
  _confirmInsertPlaceholders(placeholders = this._insertPlaceholdersList) {
    if (!placeholders || !placeholders.length) return;
    const detail = { placeholders: placeholders.map(i => i.placeholder || i) };

    placeholders.forEach(({ placeholder, index: i }) => {
      if (this.placeholderAltAppearance) placeholder.altAppearance = true;
      this._editor.insertEmbed(i, 'placeholder', placeholder, Quill.sources.USER);
    });

    // Move cursor after last inserted placeholder
    const last = placeholders[placeholders.length - 1];
    this._editor.setSelection(last.index + 1, 0, Quill.sources.USER);

    this.dispatchEvent(new CustomEvent('placeholder-insert', {
      bubbles: true, composed: true, cancelable: false, detail
    }));
  }

  /** @protected */
  _removePlaceholders(placeholders = this._getSelectedPlaceholders(this._editor.getSelection(true))) {
    if (!placeholders || !placeholders.length) return;
    const detail = { placeholders };
    const event = new CustomEvent('placeholder-before-delete', {
      bubbles: true, composed: true, cancelable: true, detail
    });
    this._removePlaceholdersList = placeholders;
    const cancelled = !this.dispatchEvent(event);
    if (!cancelled) {
      this._confirmRemovePlaceholders(placeholders);
    }
  }

  /** @protected */
  _confirmRemovePlaceholders(placeholders = this._removePlaceholdersList, silent = false) {
    if (!placeholders || !placeholders.length) return;
    const range = this._editor.getSelection(true);
    if (!range) return;

    let deleteRange;
    if (range.length >= 1) {
      deleteRange = range;
    } else if (this._placeholderRange) {
      deleteRange = this._placeholderRange;
    } else {
      deleteRange = { index: range.index - 1, length: 1 };
    }

    this._editor.deleteText(deleteRange.index, deleteRange.length, Quill.sources.USER);

    if (!silent) {
      const detail = { placeholders };
      this.dispatchEvent(new CustomEvent('placeholder-delete', {
        bubbles: true, composed: true, cancelable: false, detail
      }));
    }
  }

  /** @protected */
  _closePlaceholderDialog() {
    if (this.__placeholderDialog) this.__placeholderDialog.opened = false;
    if (this.__placeholderComboBox) this.__placeholderComboBox.value = '';
    this._insertPlaceholderIndex = null;
    this._placeholderRange = null;
    // NOTE: do NOT clear _insertPlaceholdersList here — it's needed by the async
    // _confirmInsertPlaceholders() callback from Java (executeJs runs after this).
    this._dialogJustClosed = true;
    if (this._editor) this._editor.focus();
  }

  /** @protected */
  _getPlaceholderOptions(text) {
    if (!text) return null;
    if (typeof text === 'object') return { ...text };
    const placeholders = this.placeholders || [];
    const found = placeholders.find(i => i.text === text);
    if (found) return { ...found };
    return { text };
  }

  // ==========================================================================
  // Placeholder: selection getters
  // ==========================================================================

  get selectedPlaceholder() {
    const range = this._editor.getSelection();
    if (!range) return null;
    // Check the character at cursor-1 (embeds have length 1)
    const delta = this._editor.getContents(range.index - 1, 1);
    if (delta.ops.length > 0) {
      const op = delta.ops[0];
      if (op.insert && op.insert.placeholder) return op.insert.placeholder;
    }
    return null;
  }

  get selectedPlaceholders() {
    const range = this._editor.getSelection();
    if (!range) return [];
    return this._getSelectedPlaceholders(range);
  }

  /** @protected */
  _getSelectedPlaceholders(range) {
    if (!range) return [];
    const placeholders = [];

    if (range.length > 0) {
      // Multi-character selection: check entire range
      const delta = this._editor.getContents(range.index, range.length);
      delta.ops.forEach(op => {
        if (op.insert && op.insert.placeholder) {
          placeholders.push(op.insert.placeholder);
        }
      });
    } else {
      // Zero-length selection (cursor position):
      // Check BOTH index-1 (after placeholder) AND index (before/at placeholder)
      // to handle Quill 2 Embed guard nodes that can create ambiguous cursor positions.

      // Check index-1: cursor after placeholder (right guard or next character)
      if (range.index > 0) {
        const delta1 = this._editor.getContents(range.index - 1, 1);
        delta1.ops.forEach(op => {
          if (op.insert && op.insert.placeholder) {
            placeholders.push(op.insert.placeholder);
          }
        });
      }

      // Check index: cursor before/at placeholder (left guard or at embed start)
      if (range.index < this._editor.getLength()) {
        const delta2 = this._editor.getContents(range.index, 1);
        delta2.ops.forEach(op => {
          if (op.insert && op.insert.placeholder) {
            // Avoid duplicates if both checks found the same placeholder
            if (!placeholders.some(p => p.id === op.insert.placeholder.id)) {
              placeholders.push(op.insert.placeholder);
            }
          }
        });
      }
    }

    return placeholders;
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
  // Toolbar button visibility
  // ==========================================================================

  /**
   * Shows/hides toolbar buttons by part suffix.
   * Called from Java via executeJs. Pass an object like {bold: false, clean: false}
   * to hide those buttons, or an empty object to reset all to visible.
   * Groups where ALL buttons are hidden are auto-hidden.
   * @param {Object} visMap - map of partSuffix → boolean
   */
  setToolbarButtonsVisibility(visMap) {
    this.__toolbarButtonsVisibility = visMap;
    this._applyToolbarButtonsVisibility();
  }

  /** @protected */
  _applyToolbarButtonsVisibility() {
    const toolbar = this.shadowRoot?.querySelector('[part="toolbar"]');
    if (!toolbar) return;
    const vis = this.__toolbarButtonsVisibility || {};

    // Reset all buttons to visible, then hide specified ones
    toolbar.querySelectorAll('[part~="toolbar-button"]').forEach(btn => {
      btn.style.display = '';
    });
    for (const [name, visible] of Object.entries(vis)) {
      if (visible === false) {
        const btn = toolbar.querySelector(`[part~="toolbar-button-${name}"]`);
        if (btn) btn.style.display = 'none';
      }
    }

    // Auto-hide groups where ALL buttons are hidden
    toolbar.querySelectorAll('[part~="toolbar-group"]').forEach(group => {
      const buttons = group.querySelectorAll('[part~="toolbar-button"]');
      if (buttons.length === 0) return; // skip empty groups (e.g., custom slot group)
      const allHidden = [...buttons].every(b => b.style.display === 'none');
      group.style.display = allHidden ? 'none' : '';
    });
  }

  // ==========================================================================
  // Custom Keyboard Shortcuts — Phase 3.2b
  // ==========================================================================

  /**
   * Binds a keyboard shortcut to a standard toolbar button.
   * Clicking the button triggers its native handler (format toggle, dialog, etc.).
   * @param {string} partSuffix - button part suffix (e.g. 'bold', 'align-center')
   * @param {string} key - Quill 2 key name (e.g. 'F9', 'b')
   * @param {boolean} shortKey - Ctrl (Win/Linux) or Cmd (Mac)
   * @param {boolean} shiftKey
   * @param {boolean} altKey
   */
  addStandardToolbarButtonShortcut(partSuffix, key, shortKey, shiftKey, altKey) {
    const toolbar = this.shadowRoot?.querySelector('[part="toolbar"]');
    if (!toolbar || !this._editor) return;
    const btn = toolbar.querySelector(`[part~="toolbar-button-${partSuffix}"]`);
    if (!btn) return;

    const SHORTKEY = /Mac/i.test(navigator.platform) ? 'metaKey' : 'ctrlKey';
    const binding = {
      key,
      shiftKey: !!shiftKey,
      altKey: !!altKey,
      [SHORTKEY]: !!shortKey,
      handler: () => {
        btn.click();
        return false;
      }
    };

    const bindings = this._editor.keyboard.bindings;
    const existing = bindings[key] || [];
    bindings[key] = [binding, ...existing];
  }

  /**
   * Binds a keyboard shortcut that moves focus to the toolbar.
   * @param {string} key - Quill 2 key name (e.g. 'F10')
   * @param {boolean} shortKey - Ctrl (Win/Linux) or Cmd (Mac)
   * @param {boolean} shiftKey
   * @param {boolean} altKey
   */
  addToolbarFocusShortcut(key, shortKey, shiftKey, altKey) {
    const toolbar = this.shadowRoot?.querySelector('[part="toolbar"]');
    if (!toolbar || !this._editor) return;

    const SHORTKEY = /Mac/i.test(navigator.platform) ? 'metaKey' : 'ctrlKey';
    const self = this;
    const binding = {
      key,
      shiftKey: !!shiftKey,
      altKey: !!altKey,
      [SHORTKEY]: !!shortKey,
      handler: () => {
        self._markToolbarFocused();
        const firstBtn = toolbar.querySelector('button:not([style*="display: none"]):not([hidden])');
        if (firstBtn) firstBtn.focus();
        return false;
      }
    };

    const bindings = this._editor.keyboard.bindings;
    const existing = bindings[key] || [];
    bindings[key] = [binding, ...existing];
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

      // Auto-wrap indicator: DISABLED — only triggers for tabs that wrap,
      // not for text wrapping. Inconsistent behavior, so deactivated for now.
      // tab.classList.remove('ql-auto-wrap-start');
      // if (isWrappedLine && parentBlock) {
      //   const topPos = Math.round(tabRect.top);
      //   if (!blockVisualLines.has(parentBlock)) {
      //     blockVisualLines.set(parentBlock, new Set());
      //   }
      //   const seenTops = blockVisualLines.get(parentBlock);
      //   if (!seenTops.has(topPos)) {
      //     seenTops.add(topPos);
      //     tab.classList.add('ql-auto-wrap-start');
      //   }
      // }

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

      tab.style.width = Math.round(widthNeeded) + 'px';
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
      node.classList.contains('ql-soft-break') ||
      node.classList.contains('ql-placeholder')
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
      this._focusToolbarElement(0);
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

    // ArrowUp/ArrowDown: Custom vertical navigation that uses Quill's getBounds()
    // for precise targeting. Chrome's native vertical navigation uses DOM text node
    // positions, which are wrong for tab embeds with inline-block display (guard
    // nodes are at the left edge, not at the tabstop position). This override
    // computes the correct target using Quill's accurate bounds information.
    const arrowHandler = function(direction, range) {
      const quill = self._editor;
      const currentBounds = quill.getBounds(range.index);
      const currentLeft = currentBounds.left;
      const docLength = quill.getLength();

      // Scan for the target line
      const step = direction === 'up' ? -1 : 1;
      let targetLineTop = null;
      let bestIndex = range.index;
      let bestDist = Infinity;

      for (let i = range.index + step; i >= 0 && i < docLength; i += step) {
        const bounds = quill.getBounds(i);
        // Detect line change (top differs by more than 5px from current)
        const isNewLine = direction === 'up'
          ? bounds.top < currentBounds.top - 5
          : bounds.top > currentBounds.top + 5;

        if (!isNewLine) continue;

        // First position on the target line — record its top
        if (targetLineTop === null) {
          targetLineTop = bounds.top;
        }

        // Still on the same target line? (within 5px of its top)
        if (Math.abs(bounds.top - targetLineTop) > 5) break;

        // Track closest horizontal match
        const dist = Math.abs(bounds.left - currentLeft);
        if (dist < bestDist) {
          bestDist = dist;
          bestIndex = i;
        }
      }

      if (bestIndex !== range.index) {
        quill.setSelection(bestIndex, 0, Quill.sources.USER);
        return false;
      }
      // No target line found (first/last line). Replicate Quill's default:
      // ArrowUp on first line → jump to line start; ArrowDown on last → line end.
      // Cannot return true (let browser handle) because Chrome steps through
      // inline-block guard nodes instead of jumping to line boundary.
      if (direction === 'up') {
        // Find start of current line (scan backward for line change)
        let lineStart = 0;
        for (let i = range.index - 1; i >= 0; i--) {
          const b = quill.getBounds(i);
          if (b.top < currentBounds.top - 5) {
            lineStart = i + 1;
            break;
          }
        }
        quill.setSelection(lineStart, 0, Quill.sources.USER);
      } else {
        // Find end of current line (scan forward for line change)
        let lineEnd = docLength - 1;
        for (let i = range.index + 1; i < docLength; i++) {
          const b = quill.getBounds(i);
          if (b.top > currentBounds.top + 5) {
            lineEnd = i - 1;
            break;
          }
        }
        quill.setSelection(lineEnd, 0, Quill.sources.USER);
      }
      return false;
    };

    const arrowUpBinding = {
      key: 'ArrowUp',
      handler: function(range) { return arrowHandler('up', range); }
    };
    const arrowDownBinding = {
      key: 'ArrowDown',
      handler: function(range) { return arrowHandler('down', range); }
    };

    // Prepend to existing bindings so ERTE handler fires first
    const arrowUpBindings = keyboard.bindings['ArrowUp'] || [];
    keyboard.bindings['ArrowUp'] = [arrowUpBinding, ...arrowUpBindings];
    const arrowDownBindings = keyboard.bindings['ArrowDown'] || [];
    keyboard.bindings['ArrowDown'] = [arrowDownBinding, ...arrowDownBindings];

    // Shift+Space: insert non-breaking space
    const nbspBinding = {
      key: ' ',
      shiftKey: true,
      handler: function(range) {
        this.quill.insertEmbed(range.index, 'nbsp', true, Quill.sources.USER);
        this.quill.setSelection(range.index + 1, 0, Quill.sources.USER);
        return false;
      }
    };
    const spaceBindings = keyboard.bindings[' '] || [];
    keyboard.bindings[' '] = [nbspBinding, ...spaceBindings];

    // Ctrl+P (Cmd+P on Mac): open placeholder dialog.
    // Must use ctrlKey/metaKey directly — shortKey is only resolved by Quill's
    // addBinding()/normalize(), which we bypass for prepend semantics.
    const SHORTKEY = /Mac/i.test(navigator.platform) ? 'metaKey' : 'ctrlKey';
    const placeholderBinding = {
      key: 'p', [SHORTKEY]: true,
      handler: function() { self._onPlaceholderClick(); return false; }
    };
    const pBindings = keyboard.bindings['p'] || [];
    keyboard.bindings['p'] = [placeholderBinding, ...pBindings];
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
      // Defer icon placement to next frame — getBoundingClientRect() in
      // _getRulerEditorOffset() returns zeros if called before first layout.
      requestAnimationFrame(() => {
        tabStops.forEach(stop => {
          this._addTabStopIcon(stop);
        });
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
