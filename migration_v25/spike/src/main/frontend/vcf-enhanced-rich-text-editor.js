/**
 * ERTE v25 Migration Spike - JS Module
 *
 * Phase 1 (COMPLETE):
 * - Item 2:  Custom Embed blot registration (TabBlot)
 * - Item 3:  Insert content with custom blot
 * - Item 4:  Toolbar button in render() override
 * - Item 10: Module load order logging
 * - Item 13: render() override + parent observer compatibility
 * - Item 20: Embed guard node impact
 *
 * Phase 2 (COMPLETE):
 * - Item 5:  Toolbar button survives Lit re-render (extended)
 * - Item 6:  Access keyboard module
 * - Item 7:  Add custom keyboard binding
 * - Item 14: Detailed lifecycle order
 *
 * Phase 3 (NEW):
 * - Item 11: ThemableMixin inheritance
 * - Item 12: ::part() selectors
 * - Item 21: Clipboard / Delta round-trip
 *
 * Table Spike:
 * - T1: Container.order (Parchment 3)
 * - T2: Blot registration verification
 * - T3: Table creation via Delta
 * - T4: Pipe-format round-trip
 * - T5: optimize() hierarchy verification
 * - T6: clipboard.addMatcher() API
 * - T7: History module access
 */

// ============================================================
// Module-level setup
// ============================================================

// Import RTE 2 (loads Quill 2.0.3 + defines vaadin-rich-text-editor)
import '@vaadin/rich-text-editor/src/vaadin-rich-text-editor.js';
import { html } from 'lit';

// Access Quill from RTE 2's vendored global
const Quill = window.Quill;
console.log('[ERTE] Quill available:', !!Quill, Quill ? Quill.version : 'N/A');

// Get the parent RTE 2 class from the custom elements registry
const RichTextEditor = customElements.get('vaadin-rich-text-editor');
console.log('[ERTE] RichTextEditor class:', !!RichTextEditor);

// ============================================================
// Item 2: Register custom Embed blot (TabBlot)
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
    node.style.backgroundColor = 'rgba(0, 120, 212, 0.1)';
    node.style.borderBottom = '1px dotted #0078d4';
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
console.log('[ERTE] TabBlot registered');

// ============================================================
// Table Spike: Register Table Blots for Quill 2
// ============================================================

const Container = Quill.import('blots/container');
const Block = Quill.import('blots/block');
const BlockEmbed = Quill.import('blots/block/embed');
const Parchment = Quill.import('parchment');

// --- ContainBlot (base for all table blots) ---
class ContainBlot extends Container {
  static blotName = 'contain';
  static tagName = 'contain';
  static scope = Parchment.Scope.BLOCK_BLOT;
  // Parchment 3: defaultChild must be a CLASS reference (not a string!)
  // Parchment 3's ContainerBlot.optimize() accesses defaultChild.blotName
  static defaultChild = Block;
  static allowedChildren = [Block, BlockEmbed, Container];

  static create(value) {
    return super.create(value);
  }

  // Parchment 3 fix: returning a string like "TABLE" gets spread into
  // individual character attributes ("0":"T","1":"A",...). Container blots
  // don't contribute to delta formats — only TableCell does.
  formats() {
    return {};
  }
}

// --- TableCellBlot ---
class TableCell extends ContainBlot {
  static blotName = 'td';
  static tagName = 'td';
  static className = 'td-q';
  static scope = Parchment.Scope.BLOCK_BLOT;
  static allowedChildren = [Block, BlockEmbed, Container];

  static create(value) {
    let node = super.create();
    let atts = value.split('|');
    node.setAttribute('table_id', atts[0]);
    node.setAttribute('row_id', atts[1]);
    node.setAttribute('cell_id', atts[2]);
    if (atts[3]) node.setAttribute('merge_id', atts[3]);
    if (atts[4]) node.setAttribute('colspan', atts[4]);
    if (atts[5]) node.setAttribute('rowspan', atts[5]);
    if (atts[6]) node.setAttribute('table-class', atts[6]);
    return node;
  }

  format() {}

  // Parchment 3: Container.checkMerge() only checks blotName, which would
  // merge ALL adjacent TDs regardless of cell_id. Override to check cell_id.
  checkMerge() {
    return this.next != null &&
      this.next.statics.blotName === this.statics.blotName &&
      this.next.domNode.getAttribute('cell_id') === this.domNode.getAttribute('cell_id');
  }

  formats() {
    let className = "";
    const tr = this.domNode.parentNode;
    if (tr && tr.children[0] === this.domNode) {
      const table = tr.parentNode;
      if (table && table.querySelectorAll("tr")[0] === tr) {
        className = table.classList.toString();
      }
    }
    return {
      [this.statics.blotName]: [
        this.domNode.getAttribute('table_id'),
        this.domNode.getAttribute('row_id'),
        this.domNode.getAttribute('cell_id'),
        this.domNode.getAttribute('merge_id'),
        this.domNode.getAttribute('colspan'),
        this.domNode.getAttribute('rowspan'),
        className,
      ].join('|')
    };
  }

  optimize(context) {
    super.optimize(context);
    let parent = this.parent;
    if (parent != null) {
      if (parent.statics.blotName === 'td') {
        this.moveChildren(parent, this);
        this.remove();
        return;
      } else if (parent.statics.blotName !== 'tr') {
        // Parchment 3 fix: avoid mark-based approach that breaks when
        // this.next is stale from concurrent optimize passes.
        // Instead: capture position, move self into table, then insert table.
        const origParent = this.parent;
        const origNext = this.next;
        let table = this.scroll.create('table', this.domNode.getAttribute('table_id') + '|' + (this.domNode.getAttribute('table-class') || ''));
        this.domNode.removeAttribute('table-class');
        let tr = this.scroll.create('tr', this.domNode.getAttribute('row_id'));
        table.appendChild(tr);
        tr.appendChild(this); // moves this out of origParent
        // Insert table at the original position
        if (origNext && origNext.parent === origParent) {
          origParent.insertBefore(table, origNext);
        } else {
          origParent.appendChild(table);
        }
      }
    }
    // merge same TD id (loop to handle multiple same-id siblings)
    let next = this.next;
    while (next != null && next.prev === this &&
      next.statics.blotName === this.statics.blotName &&
      next.domNode.tagName === this.domNode.tagName &&
      next.domNode.getAttribute('cell_id') === this.domNode.getAttribute('cell_id')
    ) {
      next.moveChildren(this);
      next.remove();
      next = this.next;
    }
  }

  insertBefore(childBlot, refBlot) {
    if (this.statics.allowedChildren != null && !this.statics.allowedChildren.some(child => childBlot instanceof child)) {
      // Parchment 3: defaultChild is a class ref, use .blotName for scroll.create()
      let newChild = this.scroll.create(this.statics.defaultChild.blotName);
      newChild.appendChild(childBlot);
      childBlot = newChild;
    }
    super.insertBefore(childBlot, refBlot);
  }

  // Parchment 3: replace() renamed to replaceWith()
  // Quill 1: newBlot.replace(oldBlot) → Quill 2: oldBlot.replaceWith(newBlot)
  // In Quill 1 this was called as: newCell.replace(target) where target is what gets replaced
  // In Quill 2 this is: target.replaceWith(newCell) — so 'this' is the TARGET being replaced
  replaceWith(replacement) {
    if (replacement.statics.blotName !== this.statics.blotName) {
      let item = replacement.scroll.create(replacement.statics.defaultChild.blotName);
      this.moveChildren(item);
      replacement.appendChild(item);
    }
    if (this.parent == null) return replacement;
    return super.replaceWith(replacement);
  }

  moveChildren(targetParent, refNode) {
    this.children.forEach(child => {
      targetParent.insertBefore(child, refNode);
    });
  }
}

// --- TableRowBlot ---
class TableRow extends ContainBlot {
  static blotName = 'tr';
  static tagName = 'tr';
  static scope = Parchment.Scope.BLOCK_BLOT;
  // Parchment 3: defaultChild must be a CLASS reference
  static defaultChild = TableCell;
  static allowedChildren = [TableCell];

  static create(value) {
    const tagName = 'tr';
    let node = super.create(tagName);
    node.setAttribute('row_id', value ? value : 'row-' + Math.random().toString(36).substr(2, 6));
    return node;
  }

  format() {}

  // Parchment 3: Override checkMerge to only merge rows with same row_id
  checkMerge() {
    return this.next != null &&
      this.next.statics.blotName === this.statics.blotName &&
      this.next.domNode.getAttribute('row_id') === this.domNode.getAttribute('row_id');
  }

  optimize(context) {
    if (this.children.length === 0) {
      if (this.statics.defaultChild != null) {
        var child = this.createDefaultChild();
        this.appendChild(child);
        child.optimize(context);
      } else {
        this.remove();
      }
    }
    // merge same row_id (loop to handle multiple same-id siblings)
    let next = this.next;
    while (next != null && next.prev === this &&
      next.statics.blotName === this.statics.blotName &&
      next.domNode.tagName === this.domNode.tagName &&
      next.domNode.getAttribute('row_id') === this.domNode.getAttribute('row_id')
    ) {
      next.moveChildren(this);
      next.remove();
      next = this.next;
    }
  }

  insertBefore(childBlot, refBlot) {
    if (this.statics.allowedChildren != null && !this.statics.allowedChildren.some(child => childBlot instanceof child)) {
      let newChild = this.createDefaultChild(refBlot);
      newChild.appendChild(childBlot);
      childBlot = newChild;
    }
    super.insertBefore(childBlot, refBlot);
  }

  createDefaultChild(refBlot) {
    let table_id = null;
    if (refBlot) {
      table_id = refBlot.domNode.getAttribute('table_id');
    } else if (this.parent) {
      table_id = this.parent.domNode.getAttribute('table_id');
    }
    const row_id = this.domNode.getAttribute('row_id');
    const cell_id = 'cell-' + Math.random().toString(36).substr(2, 6);
    // Parchment 3: defaultChild is a class ref, use .blotName for scroll.create()
    return this.scroll.create(this.statics.defaultChild.blotName, [table_id, row_id, cell_id].join('|'));
  }
}

// --- TableBlot (simplified for spike -- no TableTrick dependency) ---
class Table extends ContainBlot {
  static blotName = 'table';
  static tagName = 'table';
  static scope = Parchment.Scope.BLOCK_BLOT;
  // Parchment 3: defaultChild must be a CLASS reference
  static defaultChild = TableRow;
  static allowedChildren = [TableRow];

  static create(value) {
    const tagName = 'table';
    let node = super.create(tagName);
    let atts = value.split('|');
    node.setAttribute('table_id', atts[0]);
    if (atts[1] && atts[1] !== "null" && atts[1] !== "") {
      node.classList.add(atts[1]);
    }
    return node;
  }

  // Parchment 3: Override checkMerge to only merge tables with same table_id
  checkMerge() {
    return this.next != null &&
      this.next.statics.blotName === this.statics.blotName &&
      this.next.domNode.getAttribute('table_id') === this.domNode.getAttribute('table_id');
  }

  optimize(context) {
    super.optimize(context);
    // merge same table_id (loop to handle multiple same-id siblings)
    const table_id = this.domNode.getAttribute('table_id');
    let next = this.next;
    while (next != null && next.prev === this &&
      next.domNode.getAttribute('table_id') === table_id &&
      next.statics.blotName === this.statics.blotName
    ) {
      next.moveChildren(this);
      next.remove();
      next = this.next;
    }
  }

  insertBefore(childBlot, refBlot) {
    if (this.statics.allowedChildren != null && !this.statics.allowedChildren.some(child => childBlot instanceof child)) {
      const row_id = 'row-' + Math.random().toString(36).substr(2, 6);
      // Parchment 3: defaultChild is a class ref, use .blotName for scroll.create()
      let newChild = this.scroll.create(this.statics.defaultChild.blotName, row_id);
      newChild.appendChild(childBlot);
      childBlot = newChild;
    }
    super.insertBefore(childBlot, refBlot);
  }
}

// Test Container.order (Item T1)
console.log('[ERTE Table] Container.order before:', Container.order);
try {
  Container.order = ['list', 'contain', 'td', 'tr', 'table'];
  console.log('[ERTE Table] Container.order after set:', Container.order);
} catch (err) {
  console.error('[ERTE Table] Container.order set FAILED:', err);
}

// Register table blots
Quill.register(ContainBlot);
Quill.register(TableCell);
Quill.register(TableRow);
Quill.register(Table);
console.log('[ERTE Table] All table blots registered');

// ============================================================
// Custom Element
// ============================================================

class EnhancedRichTextEditor extends RichTextEditor {

  static get is() {
    return 'vcf-enhanced-rich-text-editor';
  }

  /** @protected */
  render() {
    return html`
      <div class="vaadin-rich-text-editor-container">
        <div part="toolbar" role="toolbar">
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
              @click="${this._onWhitespaceClick}">WS</button>
          </span>

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

    if (this._editor) {
      console.log('[ERTE] ready() complete, _editor available');

      // Insert some test content with tabs
      const Delta = Quill.import('delta');
      try {
        this._editor.setContents(new Delta()
          .insert('Hello')
          .insert({ tab: true })
          .insert('Tab1')
          .insert({ tab: true })
          .insert('Tab2')
          .insert({ tab: true })
          .insert('World\n')
          .insert('Second line\n')
        );
      } catch (err) {
        console.error('[ERTE] Failed to insert content:', err);
      }

      // Set up custom Tab binding (prepend for priority)
      const kb = this._editor.keyboard;
      if (kb && kb.bindings && kb.bindings['Tab']) {
        const existing = kb.bindings['Tab'].slice();
        kb.bindings['Tab'] = [];
        kb.addBinding({ key: 'Tab' }, (range, context) => {
          this._editor.insertEmbed(range.index, 'tab', true, 'user');
          this._editor.setSelection(range.index + 1, 0, 'user');
          return false;
        });
        existing.forEach(b => kb.bindings['Tab'].push(b));
        console.log('[ERTE] Tab binding installed with priority');
      }
    } else {
      console.error('[ERTE] _editor NOT available after super.ready()!');
    }
  }

  // ============================================================
  // Whitespace button handler
  // ============================================================

  _onWhitespaceClick() {
    console.log('[ERTE] Whitespace button clicked');
    this._showWhitespace = !this._showWhitespace;
    const btn = this.shadowRoot.querySelector('#btn-whitespace');
    if (btn) {
      btn.classList.toggle('ql-active', this._showWhitespace);
      btn.part.toggle('toolbar-button-pressed', this._showWhitespace);
    }
  }

  checkWhitespaceButton() {
    const btn = this.shadowRoot.querySelector('#btn-whitespace');
    if (!btn) {
      return JSON.stringify({ exists: false, error: 'Button not found' });
    }
    return JSON.stringify({
      exists: true,
      text: btn.textContent,
      id: btn.id,
      hasActiveClass: btn.classList.contains('ql-active'),
    });
  }

  // ============================================================
  // Item 11: ThemableMixin Inheritance Analysis
  // ============================================================

  analyzeThemeInheritance() {
    const results = {};

    // 1. Check adopted stylesheets on this element's shadow root
    const adopted = this.shadowRoot.adoptedStyleSheets || [];
    results.adoptedStyleSheetCount = adopted.length;
    results.adoptedStyleSheetDetails = adopted.map((sheet, i) => {
      try {
        const rules = Array.from(sheet.cssRules || []);
        return {
          index: i,
          ruleCount: rules.length,
          sampleRules: rules.slice(0, 3).map(r => r.cssText?.substring(0, 120))
        };
      } catch (e) {
        return { index: i, error: e.message };
      }
    });

    // 2. Check <style> elements in shadow DOM
    const styleElements = this.shadowRoot.querySelectorAll('style');
    results.styleElementCount = styleElements.length;
    results.styleElementSamples = Array.from(styleElements).map((el, i) => ({
      index: i,
      textLength: el.textContent?.length || 0,
      sample: el.textContent?.substring(0, 200)
    }));

    // 3. Check if toolbar buttons have Lumo icons (SVG backgrounds)
    const undoBtn = this.shadowRoot.querySelector('#btn-undo');
    const boldBtn = this.shadowRoot.querySelector('#btn-bold');
    results.toolbarButtonStyles = {};
    if (undoBtn) {
      const styles = getComputedStyle(undoBtn);
      results.toolbarButtonStyles.undo = {
        width: styles.width,
        height: styles.height,
        backgroundImage: styles.backgroundImage?.substring(0, 100),
        hasIcon: styles.backgroundImage !== 'none' && styles.backgroundImage !== '',
        color: styles.color,
        padding: styles.padding,
      };
    }
    if (boldBtn) {
      const styles = getComputedStyle(boldBtn);
      results.toolbarButtonStyles.bold = {
        width: styles.width,
        height: styles.height,
        backgroundImage: styles.backgroundImage?.substring(0, 100),
        hasIcon: styles.backgroundImage !== 'none' && styles.backgroundImage !== '',
      };
    }

    // 4. Check the parent class's static styles registration
    const parentClass = customElements.get('vaadin-rich-text-editor');
    results.parentStaticStyles = !!parentClass?.styles;
    results.parentStylesType = typeof parentClass?.styles;
    if (parentClass?.styles) {
      if (Array.isArray(parentClass.styles)) {
        results.parentStylesLength = parentClass.styles.length;
      }
    }

    // 5. Check our class's static styles
    results.ownStaticStyles = !!EnhancedRichTextEditor.styles;
    results.ownStylesType = typeof EnhancedRichTextEditor.styles;

    // 6. Check if ThemableMixin's is() matters for style registration
    // The key question: does ThemableMixin register styles keyed by the `is` getter?
    results.parentIs = parentClass?.is;
    results.ownIs = EnhancedRichTextEditor.is;

    // 7. Check if there are any vaadin-rich-text-editor theme styles registered
    // (These would be registered via registerStyles() in Vaadin's ThemableMixin)
    try {
      // Try to access Vaadin's style registry
      if (window.Vaadin && window.Vaadin.registrations) {
        results.vaadinRegistrations = true;
      }
    } catch (e) {
      results.vaadinRegistrationsError = e.message;
    }

    // 8. Check if the container div has any applied styles
    const container = this.shadowRoot.querySelector('.vaadin-rich-text-editor-container');
    if (container) {
      const cs = getComputedStyle(container);
      results.containerStyles = {
        display: cs.display,
        fontFamily: cs.fontFamily?.substring(0, 50),
      };
    }

    // 9. Check if Lumo theme vars are available
    const root = getComputedStyle(document.documentElement);
    results.lumoThemeVars = {
      '--lumo-primary-color': root.getPropertyValue('--lumo-primary-color')?.trim() || 'not set',
      '--lumo-font-family': root.getPropertyValue('--lumo-font-family')?.trim()?.substring(0, 50) || 'not set',
      '--lumo-font-size-m': root.getPropertyValue('--lumo-font-size-m')?.trim() || 'not set',
    };

    return JSON.stringify(results, null, 2);
  }

  // ============================================================
  // Item 12: ::part() Selectors Analysis
  // ============================================================

  testPartSelectors() {
    const results = {};

    // 1. Check toolbar part
    const toolbar = this.shadowRoot.querySelector('[part~="toolbar"]');
    results.toolbarPart = {
      exists: !!toolbar,
      partValue: toolbar?.getAttribute('part'),
      role: toolbar?.getAttribute('role'),
    };

    // 2. Check content part
    const content = this.shadowRoot.querySelector('[part~="content"]');
    results.contentPart = {
      exists: !!content,
      partValue: content?.getAttribute('part'),
    };

    // 3. Check all elements with part attributes
    const parted = this.shadowRoot.querySelectorAll('[part]');
    results.allParts = Array.from(parted).map(el => ({
      tag: el.tagName,
      part: el.getAttribute('part'),
    }));
    results.totalPartElements = parted.length;

    // 4. Test if ::part() from external CSS can match
    // We create a test style, apply it, and check if computed styles change
    const toolbarBgBefore = toolbar ? getComputedStyle(toolbar).backgroundColor : 'N/A';
    const contentBgBefore = content ? getComputedStyle(content).backgroundColor : 'N/A';

    // Apply test style
    const testStyle = document.createElement('style');
    testStyle.id = 'erte-part-test-style';
    testStyle.textContent = `
      vcf-enhanced-rich-text-editor::part(toolbar) {
        background-color: rgb(255, 0, 0) !important;
      }
      vcf-enhanced-rich-text-editor::part(content) {
        background-color: rgb(0, 255, 0) !important;
      }
    `;
    document.head.appendChild(testStyle);

    // Force style recalc
    this.offsetHeight; // trigger reflow

    const toolbarBgAfter = toolbar ? getComputedStyle(toolbar).backgroundColor : 'N/A';
    const contentBgAfter = content ? getComputedStyle(content).backgroundColor : 'N/A';

    results.partStylingTest = {
      toolbar: {
        bgBefore: toolbarBgBefore,
        bgAfter: toolbarBgAfter,
        partStylingWorks: toolbarBgAfter === 'rgb(255, 0, 0)',
      },
      content: {
        bgBefore: contentBgBefore,
        bgAfter: contentBgAfter,
        partStylingWorks: contentBgAfter === 'rgb(0, 255, 0)',
      },
    };

    // Clean up test style
    testStyle.remove();

    // 5. Also test using vaadin-rich-text-editor as selector (should NOT work for our element)
    const testStyle2 = document.createElement('style');
    testStyle2.id = 'erte-part-test-style2';
    testStyle2.textContent = `
      vaadin-rich-text-editor::part(toolbar) {
        background-color: rgb(0, 0, 255) !important;
      }
    `;
    document.head.appendChild(testStyle2);
    this.offsetHeight;

    const toolbarBgWrongTag = toolbar ? getComputedStyle(toolbar).backgroundColor : 'N/A';
    results.wrongTagTest = {
      selector: 'vaadin-rich-text-editor::part(toolbar)',
      bgAfter: toolbarBgWrongTag,
      matchesOurElement: toolbarBgWrongTag === 'rgb(0, 0, 255)',
    };

    testStyle2.remove();

    return JSON.stringify(results, null, 2);
  }

  // ============================================================
  // Item 21: Clipboard / Delta Round-Trip
  // ============================================================

  testDeltaRoundTrip() {
    const results = {};

    if (!this._editor) {
      return JSON.stringify({ error: '_editor not available' });
    }

    const Delta = Quill.import('delta');

    // 1. Set content with tab embeds
    const inputDelta = new Delta()
      .insert('Before')
      .insert({ tab: true })
      .insert('Middle')
      .insert({ tab: true })
      .insert({ tab: true })
      .insert('After\n');

    this._editor.setContents(inputDelta, 'api');

    // 2. Read delta back
    const outputDelta = this._editor.getContents();
    results.inputOps = inputDelta.ops;
    results.outputOps = outputDelta.ops;

    // 3. Compare
    results.opsMatch = JSON.stringify(inputDelta.ops) === JSON.stringify(outputDelta.ops);

    // 4. Check that tab embeds survived
    const tabOps = outputDelta.ops.filter(op => op.insert && op.insert.tab);
    results.tabEmbedCount = tabOps.length;
    results.expectedTabCount = 3;
    results.tabEmbedsPreserved = tabOps.length === 3;

    // 5. Read HTML from Quill
    results.quillHtml = this._editor.root.innerHTML;

    // 6. Check DOM for tab elements
    const tabElements = this._editor.root.querySelectorAll('.ql-tab');
    results.tabDomElements = tabElements.length;

    // 7. Test getSemanticHTML (Quill 2's HTML export)
    if (typeof this._editor.getSemanticHTML === 'function') {
      results.semanticHtml = this._editor.getSemanticHTML();
    } else {
      results.semanticHtml = 'getSemanticHTML not available';
    }

    // 8. Test: set via HTML, read back as delta
    const tabHtml = '<p>HTML<span class="ql-tab" contenteditable="false" style="display:inline-block;min-width:2em"></span>Test</p>';
    // Use Quill's clipboard to parse HTML into delta (simulates paste)
    const clipboard = this._editor.getModule('clipboard');
    if (clipboard) {
      try {
        const parsed = clipboard.convert({ html: tabHtml });
        results.clipboardParsedDelta = parsed.ops;
        const parsedTabOps = parsed.ops.filter(op => op.insert && op.insert.tab);
        results.clipboardRecognizedTabs = parsedTabOps.length;
      } catch (e) {
        results.clipboardError = e.message;
      }
    } else {
      results.clipboardModule = 'not available';
    }

    // 9. Test: programmatic clipboard convert (HTML to Delta)
    // This simulates what happens when a user pastes tab content from clipboard
    if (clipboard) {
      try {
        const simpleHtml = '<p>A<span class="ql-tab"></span>B</p>';
        const parsed2 = clipboard.convert({ html: simpleHtml });
        results.simpleClipboardParse = parsed2.ops;
      } catch (e) {
        results.simpleClipboardError = e.message;
      }
    }

    return JSON.stringify(results, null, 2);
  }

  // ============================================================
  // Table Spike Tests
  // ============================================================

  testTableSpike() {
    const results = {};

    if (!this._editor) {
      return JSON.stringify({ error: '_editor not available' });
    }

    const Delta = Quill.import('delta');
    const Container = Quill.import('blots/container');

    // T1: Container.order
    results.t1_containerOrder = {
      exists: 'order' in Container,
      value: Container.order,
      type: typeof Container.order,
    };

    // T2: Blot registration verification
    const registeredContain = Quill.import('blots/contain') || Quill.import('formats/contain');
    const registeredTd = Quill.import('blots/td') || Quill.import('formats/td');
    const registeredTr = Quill.import('blots/tr') || Quill.import('formats/tr');
    const registeredTable = Quill.import('blots/table') || Quill.import('formats/table');
    results.t2_registration = {
      contain: !!registeredContain,
      td: !!registeredTd,
      tr: !!registeredTr,
      table: !!registeredTable,
    };

    // T3: Table creation via Delta
    const tableId = 'spike-t1';
    const rowId1 = 'row-1';
    const rowId2 = 'row-2';
    const cellId1 = 'cell-1a';
    const cellId2 = 'cell-1b';
    const cellId3 = 'cell-2a';
    const cellId4 = 'cell-2b';

    const tableDelta = new Delta()
      .insert('Cell 1A')
      .insert('\n', { td: `${tableId}|${rowId1}|${cellId1}||||` })
      .insert('Cell 1B')
      .insert('\n', { td: `${tableId}|${rowId1}|${cellId2}||||` })
      .insert('Cell 2A')
      .insert('\n', { td: `${tableId}|${rowId2}|${cellId3}||||` })
      .insert('Cell 2B')
      .insert('\n', { td: `${tableId}|${rowId2}|${cellId4}||||` })
      .insert('\n'); // trailing newline

    try {
      this._editor.setContents(tableDelta, 'api');
      results.t3_creation = { success: true };

      // Check DOM
      const root = this._editor.root;
      const tables = root.querySelectorAll('table');
      const trs = root.querySelectorAll('tr');
      const tds = root.querySelectorAll('td');
      results.t3_creation.domTableCount = tables.length;
      results.t3_creation.domRowCount = trs.length;
      results.t3_creation.domCellCount = tds.length;
      results.t3_creation.html = root.innerHTML.substring(0, 500);

      if (tables.length > 0) {
        results.t3_creation.tableId = tables[0].getAttribute('table_id');
        results.t3_creation.firstRowId = trs[0]?.getAttribute('row_id');
        results.t3_creation.firstCellId = tds[0]?.getAttribute('cell_id');
      }
    } catch (err) {
      results.t3_creation = { success: false, error: err.message, stack: err.stack?.substring(0, 300) };
    }

    // T4: Pipe-format round-trip
    try {
      const outputDelta = this._editor.getContents();
      const tdOps = outputDelta.ops.filter(op => op.attributes && op.attributes.td);
      results.t4_pipeFormat = {
        tdOpsCount: tdOps.length,
        expectedCount: 4,
        ops: tdOps.map(op => ({
          insert: op.insert,
          td: op.attributes.td,
        })),
      };
      // Check if pipe format matches
      if (tdOps.length > 0) {
        const firstTd = tdOps[0].attributes.td;
        const parts = firstTd.split('|');
        results.t4_pipeFormat.firstTdParts = parts.length;
        results.t4_pipeFormat.firstTdTableId = parts[0];
        results.t4_pipeFormat.firstTdRowId = parts[1];
        results.t4_pipeFormat.firstTdCellId = parts[2];
        results.t4_pipeFormat.formatPreserved = parts[0] === tableId && parts[1] === rowId1 && parts[2] === cellId1;
      }
    } catch (err) {
      results.t4_pipeFormat = { error: err.message };
    }

    // T5: optimize() verification
    try {
      // Check if table structure was built (optimize creates Table > TR > TD hierarchy from flat TD ops)
      const root = this._editor.root;
      const table = root.querySelector('table');
      if (table) {
        const directTrs = table.querySelectorAll(':scope > tr');
        const firstRowTds = directTrs[0]?.querySelectorAll(':scope > td');
        results.t5_optimize = {
          hierarchyCorrect: directTrs.length >= 1 && (firstRowTds?.length >= 1),
          directRowCount: directTrs.length,
          firstRowCellCount: firstRowTds?.length || 0,
          tableHasTableId: !!table.getAttribute('table_id'),
        };
      } else {
        results.t5_optimize = { tableFound: false, html: root.innerHTML.substring(0, 300) };
      }
    } catch (err) {
      results.t5_optimize = { error: err.message };
    }

    // T6: clipboard.addMatcher() API
    try {
      const clipboard = this._editor.getModule('clipboard');
      results.t6_clipboard = {
        moduleExists: !!clipboard,
        hasAddMatcher: typeof clipboard?.addMatcher === 'function',
      };
      if (clipboard && typeof clipboard.addMatcher === 'function') {
        // Try adding a matcher (harmless: just logs)
        clipboard.addMatcher('TABLE', (node, delta) => {
          console.log('[ERTE Table Spike] TABLE matcher fired');
          return delta;
        });
        results.t6_clipboard.matcherAdded = true;

        // Also test clipboard.convert with table HTML
        try {
          const tableHtml = '<table><tr><td>A</td><td>B</td></tr></table>';
          const parsed = clipboard.convert({ html: tableHtml });
          results.t6_clipboard.convertWorks = true;
          results.t6_clipboard.parsedOps = parsed.ops;
        } catch (convertErr) {
          results.t6_clipboard.convertError = convertErr.message;
        }
      }
    } catch (err) {
      results.t6_clipboard = { error: err.message };
    }

    // T7: History module access
    try {
      const history = this._editor.getModule('history');
      results.t7_history = {
        moduleExists: !!history,
        hasStack: !!history?.stack,
        stackKeys: history?.stack ? Object.keys(history.stack) : [],
        hasUndo: !!history?.stack?.undo,
        hasRedo: !!history?.stack?.redo,
        undoType: history?.stack?.undo ? (Array.isArray(history.stack.undo) ? 'array' : typeof history.stack.undo) : 'N/A',
        redoType: history?.stack?.redo ? (Array.isArray(history.stack.redo) ? 'array' : typeof history.stack.redo) : 'N/A',
        hasIgnoreChange: 'ignoreChange' in (history || {}),
        hasLastRecorded: 'lastRecorded' in (history || {}),
      };
      // Try setting tableStack (ERTE tables uses this)
      if (history) {
        history.tableStack = {};
        results.t7_history.tableStackSet = 'tableStack' in history;
      }
    } catch (err) {
      results.t7_history = { error: err.message };
    }

    return JSON.stringify(results, null, 2);
  }
}

// Register the custom element
customElements.define('vcf-enhanced-rich-text-editor', EnhancedRichTextEditor);

export { EnhancedRichTextEditor };
