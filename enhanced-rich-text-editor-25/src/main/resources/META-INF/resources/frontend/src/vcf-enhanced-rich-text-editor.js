/**
 * ERTE v25 - Enhanced Rich Text Editor for Vaadin 25
 *
 * Extends RTE 2's Lit-based web component with additional features.
 * Architecture: render() override for toolbar, ready() hook for Quill access.
 */

// Import RTE 2 (loads Quill 2.0.3 + defines vaadin-rich-text-editor)
import '@vaadin/rich-text-editor/src/vaadin-rich-text-editor.js';
import { html } from 'lit';

// Access Quill from RTE 2's vendored global
const Quill = window.Quill;

// Get the parent RTE 2 class from the custom elements registry
const RichTextEditor = customElements.get('vaadin-rich-text-editor');

// ============================================================
// Custom Blot: TabBlot (Embed)
// ============================================================

const Embed = Quill.import('blots/embed');

class TabBlot extends Embed {
  static blotName = 'tab';
  static tagName = 'SPAN';
  static className = 'ql-tab';

  static create(value) {
    const node = super.create(value);
    node.setAttribute('contenteditable', 'false');
    node.style.display = 'inline-block';
    node.style.minWidth = '2em';
    return node;
  }

  static formats(domNode) {
    return true;
  }

  static value(domNode) {
    return true;
  }
}

Quill.register('formats/tab', TabBlot);

// ============================================================
// ERTE Custom Element
// ============================================================

class EnhancedRichTextEditor extends RichTextEditor {

  /**
   * Override render() to inject custom toolbar content.
   * Parent observers (_onReadonlyChanged etc.) still work via part selectors.
   */
  render() {
    // Start with the parent render output
    const parentResult = super.render();

    // TODO: Add custom toolbar slots, additional buttons, ruler, etc.
    // For now, return parent render unchanged
    return parentResult;
  }

  /** @protected */
  ready() {
    super.ready();

    // _editor is the Quill instance, available immediately after super.ready()
    const quill = this._editor;
    if (quill) {
      // TODO: Register custom keyboard bindings
      // TODO: Set up tab-stop system
      // TODO: Initialize read-only section handling
    }
  }
}

// Register the custom element with our own tag
customElements.define('vcf-enhanced-rich-text-editor', EnhancedRichTextEditor);
