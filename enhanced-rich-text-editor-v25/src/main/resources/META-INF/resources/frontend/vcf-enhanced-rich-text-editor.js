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

/**
 * ERTE CSS classes to preserve in htmlValue (not stripped by __updateHtmlValue).
 * Each phase adds its classes here.
 */
const ERTE_PRESERVED_CLASSES = ['ql-readonly'];

class VcfEnhancedRichTextEditor extends RteBase {

  static get is() {
    return 'vcf-enhanced-rich-text-editor';
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
    console.debug('[ERTE] ready, _editor:', !!this._editor, 'readonly protection active');
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
    editor.on('text-change', (delta, oldDelta, source) => {
      if (source !== 'user') return;
      // Only check if the change includes a delete op
      if (!delta.ops.some((op) => op.delete != null)) return;

      const oldOps = oldDelta.ops;
      const newOps = editor.getContents().ops;

      // Count readonly sections (ops with attributes.readonly === true)
      const oldCount = oldOps.filter(
        (op) => op.attributes && op.attributes.readonly === true
      ).length;
      const newCount = newOps.filter(
        (op) => op.attributes && op.attributes.readonly === true
      ).length;

      if (newCount < oldCount) {
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
}

customElements.define(VcfEnhancedRichTextEditor.is, VcfEnhancedRichTextEditor);

// Exported for potential extension by tables addon or test utilities.
export { VcfEnhancedRichTextEditor };
