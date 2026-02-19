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
 * ERTE V25 shell — extends RTE 2's web component with ERTE's own tag.
 * Phase 2: pure passthrough. Features added in Phase 3+.
 *
 * Import path: package entry point (@vaadin/rich-text-editor), NOT the
 * internal /src/ path. customElements.get() decouples from internal module
 * structure. Import path is stable as of Vaadin 25.0.5.
 */
import '@vaadin/rich-text-editor';

const RteBase = customElements.get('vaadin-rich-text-editor');
if (!RteBase) {
  throw new Error(
    'vcf-enhanced-rich-text-editor: vaadin-rich-text-editor not registered. '
    + 'Ensure @vaadin/rich-text-editor is loaded first.'
  );
}

class VcfEnhancedRichTextEditor extends RteBase {

  static get is() {
    return 'vcf-enhanced-rich-text-editor';
  }

  static get styles() {
    // Defensive: guard against undefined (shouldn't happen, RTE 2 has 5 sheets)
    return super.styles ? [...super.styles] : [];
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
    console.debug('[ERTE] ready, _editor:', !!this._editor, 'slots injected');
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
}

customElements.define(VcfEnhancedRichTextEditor.is, VcfEnhancedRichTextEditor);

// Exported for potential extension by tables addon or test utilities.
export { VcfEnhancedRichTextEditor };
