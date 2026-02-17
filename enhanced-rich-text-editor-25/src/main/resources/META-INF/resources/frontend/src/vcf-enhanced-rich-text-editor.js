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
 */

// Import RTE 2 (loads Quill 2.0.3 + defines vaadin-rich-text-editor)
import '@vaadin/rich-text-editor/src/vaadin-rich-text-editor.js';
import { html } from 'lit';

// Import and register all ERTE blots (registration happens at import time)
import './vcf-enhanced-rich-text-editor-blots.js';

// Import ERTE-specific styles
import { erteStyles } from './vcf-enhanced-rich-text-editor-styles.js';
import { erteIconStyles } from './vcf-enhanced-rich-text-editor-icons.js';

// Import extra SVG iconset (registers vaadin-iconset in document head)
import './vcf-enhanced-rich-text-editor-extra-icons.js';

// Access Quill from RTE 2's vendored global
const Quill = window.Quill;

// Get the parent RTE 2 class from the custom elements registry
const RichTextEditor = customElements.get('vaadin-rich-text-editor');

// ============================================================
// ERTE Custom Element
// ============================================================

class EnhancedRichTextEditor extends RichTextEditor {

  static get is() {
    return 'vcf-enhanced-rich-text-editor';
  }

  /**
   * Layer ERTE-specific styles on top of the parent RTE 2 styles.
   * The parent's static get styles() returns richTextEditorStyles (icons, base,
   * content, toolbar, states). We append ERTE additions.
   */
  static get styles() {
    const parentStyles = super.styles;
    const base = Array.isArray(parentStyles) ? parentStyles : [parentStyles];
    return [...base, erteIconStyles, ...erteStyles];
  }

  /**
   * Override render() to inject custom toolbar content.
   * The parent's render() provides the full toolbar with all standard buttons.
   * We reproduce the parent template and add ERTE-specific toolbar groups,
   * slots, and buttons.
   *
   * Parent observers (_onReadonlyChanged, _onDisabledChanged) work via
   * [part] selectors, so they continue to function on our rendered output.
   */
  render() {
    return html`
      <div class="vaadin-rich-text-editor-container">
        <div part="toolbar" role="toolbar">
          <!-- Slot for toolbar components added BEFORE all groups (Java API) -->
          <slot name="toolbar-start"></slot>

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

          <span part="toolbar-group toolbar-group-glyph-transformation">
            <button id="btn-subscript" class="ql-script" value="sub"
              part="toolbar-button toolbar-button-subscript"
              aria-label="${this.__effectiveI18n?.subscript || 'Subscript'}"></button>
            <button id="btn-superscript" class="ql-script" value="super"
              part="toolbar-button toolbar-button-superscript"
              aria-label="${this.__effectiveI18n?.superscript || 'Superscript'}"></button>
          </span>

          <span part="toolbar-group toolbar-group-list">
            <button id="btn-ol" type="button" class="ql-list" value="ordered"
              part="toolbar-button toolbar-button-list-ordered"
              aria-label="${this.__effectiveI18n?.listOrdered || 'Ordered list'}"></button>
            <button id="btn-ul" type="button" class="ql-list" value="bullet"
              part="toolbar-button toolbar-button-list-bullet"
              aria-label="${this.__effectiveI18n?.listBullet || 'Bullet list'}"></button>
          </span>

          <span part="toolbar-group toolbar-group-indent">
            <button id="btn-outdent" type="button" class="ql-indent" value="-1"
              part="toolbar-button toolbar-button-outdent"
              aria-label="${this.__effectiveI18n?.outdent || 'Decrease indent'}"></button>
            <button id="btn-indent" type="button" class="ql-indent" value="+1"
              part="toolbar-button toolbar-button-indent"
              aria-label="${this.__effectiveI18n?.indent || 'Increase indent'}"></button>
          </span>

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

          <span part="toolbar-group toolbar-group-block">
            <button id="btn-blockquote" type="button" class="ql-blockquote"
              part="toolbar-button toolbar-button-blockquote"
              aria-label="${this.__effectiveI18n?.blockquote || 'Blockquote'}"></button>
            <button id="btn-code" type="button" class="ql-code-block"
              part="toolbar-button toolbar-button-code-block"
              aria-label="${this.__effectiveI18n?.codeBlock || 'Code block'}"></button>
          </span>

          <span part="toolbar-group toolbar-group-format">
            <button id="btn-clean" type="button" class="ql-clean"
              part="toolbar-button toolbar-button-clean"
              aria-label="${this.__effectiveI18n?.clean || 'Clean'}"></button>
          </span>

          <!-- ERTE custom toolbar group -->
          <span part="toolbar-group toolbar-group-erte">
            <button id="btn-whitespace" type="button"
              part="toolbar-button toolbar-button-whitespace"
              title="Toggle Whitespace"
              aria-label="Whitespace"
              @click="${this._onWhitespaceClick}"></button>
          </span>

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

  /** @protected */
  ready() {
    super.ready();

    // _editor is the Quill instance, available immediately after super.ready()
    const quill = this._editor;
    if (quill) {
      // TODO: Register custom keyboard bindings (Tab key for tab-stop system)
      // TODO: Set up tab-stop system (iterative width calculation)
      // TODO: Initialize read-only section handling
      // TODO: Set up soft-break (Shift+Enter) binding
      // TODO: Set up placeholder handling
    }
  }

  // ============================================================
  // Whitespace toggle
  // ============================================================

  _onWhitespaceClick() {
    this._showWhitespace = !this._showWhitespace;
    const btn = this.shadowRoot.querySelector('#btn-whitespace');
    if (btn) {
      btn.classList.toggle('ql-active', this._showWhitespace);
      btn.part.toggle('toolbar-button-pressed', this._showWhitespace);
    }

    // Toggle show-whitespace class on the Quill editor element
    if (this._editor && this._editor.root) {
      this._editor.root.classList.toggle('show-whitespace', this._showWhitespace);
    }
  }
}

// Register the custom element with our own tag
customElements.define('vcf-enhanced-rich-text-editor', EnhancedRichTextEditor);

export { EnhancedRichTextEditor };
