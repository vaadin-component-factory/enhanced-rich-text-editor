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
    // _editor (Quill instance) is now available.
    // Phase 3+ will initialize ERTE features here.
    console.debug('[ERTE] Shell ready, _editor:', !!this._editor);
  }
}

customElements.define(VcfEnhancedRichTextEditor.is, VcfEnhancedRichTextEditor);

// Exported for potential extension by tables addon or test utilities.
export { VcfEnhancedRichTextEditor };
