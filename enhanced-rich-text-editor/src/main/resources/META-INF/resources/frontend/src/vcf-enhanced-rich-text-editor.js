/**
 * @license
 * Copyright (C) 2015 Vaadin Ltd.
 * This program is available under Commercial Vaadin Add-On License 3.0 (CVALv3).
 * See the file LICENSE.md distributed with this software for more information about licensing.
 * See [the website]{@link https://vaadin.com/license/cval-3} for the complete license.
 */

import { html, PolymerElement } from '@polymer/polymer/polymer-element';
import { timeOut } from '@polymer/polymer/lib/utils/async';
import { Debouncer } from '@polymer/polymer/lib/utils/debounce';
import { resetMouseCanceller } from '@polymer/polymer/lib/utils/gestures';
import { useShadow } from '@polymer/polymer/lib/utils/settings';
import { ThemableMixin } from '@vaadin/vaadin-themable-mixin';
import { ElementMixin } from '@vaadin/component-base/src/element-mixin.js';
import '@vaadin/button';
import '@vaadin/confirm-dialog';
import '@vaadin/text-field';
import '@vaadin/combo-box';
import '@vaadin/vaadin-license-checker/vaadin-license-checker';
import '@vaadin/icons';
import './vendor/vaadin-quill';
import './vcf-enhanced-rich-text-editor-styles';
import './vcf-enhanced-rich-text-editor-toolbar-styles';
import './vcf-enhanced-rich-text-editor-extra-icons';
import { ReadOnlyBlot, TabBlot, SoftBreakBlot, PlaceholderBlot } from './vcf-enhanced-rich-text-editor-blots';

const Quill = window.Quill;
const Inline = Quill.import('blots/inline');

// Only Inline blots go in Inline.order; Embeds (TabBlot, SoftBreakBlot) must NOT be listed here
Inline.order.push(PlaceholderBlot.blotName, ReadOnlyBlot.blotName);

(function() {
  'use strict';

  const Quill = window.Quill;

  const HANDLERS = ['bold', 'italic', 'underline', 'strike', 'header', 'script', 'list', 'indent', 'align', 'blockquote', 'code-block', 'placeholder'];

  const TOOLBAR_BUTTON_GROUPS = {
    history: ['undo', 'redo'],
    emphasis: ['bold', 'italic', 'underline', 'strike'],
    heading: ['h1', 'h2', 'h3'],
    'glyph-transformation': ['subscript', 'superscript'],
    list: ['listOrdered', 'listBullet'],
    indent: ['deindent', 'indent'],
    alignment: ['alignLeft', 'alignCenter', 'alignRight', 'alignJustify'],
    'rich-text': ['image', 'link'],
    block: ['blockquote', 'codeBlock', 'placeholder', 'placeholderAppearance'],
    format: ['readonly', 'clean']
  };

  const SOURCE = {
    USER: 'user',
    SILENT: 'silent'
  };

  const STATE = {
    DEFAULT: 0,
    FOCUSED: 1,
    CLICKED: 2
  };

  const DELETE_KEY = 46;
  const BACKSPACE_KEY = 8;
  const TAB_KEY = 9;
  const QL_EDITOR_PADDING_LEFT = 16;

  // Tab engine constants (from prototype)
  const TAB_WRAP_DETECTION_MULTIPLIER = 0.8;
  const TAB_DEFAULT_TAB_CHARS = 8;
  const TAB_MIN_TAB_WIDTH = 2;
  const TAB_FIXED_TAB_FALLBACK = 50;
  const TAB_BLOCK_ELEMENTS = ['P', 'DIV', 'LI', 'H1', 'H2', 'H3', 'H4', 'H5', 'H6',
                              'BLOCKQUOTE', 'PRE', 'OL', 'UL', 'TABLE', 'TR', 'TD', 'TH'];
  const TAB_BLOCK_SELECTOR = TAB_BLOCK_ELEMENTS.map(t => t.toLowerCase()).join(', ');

  /**
   * `<vcf-enhanced-rich-text-editor>` is a Web Component for rich text editing.
   * It provides a set of toolbar controls to apply formatting on the content,
   * which is stored and can be accessed as HTML5 or JSON string.
   *
   * ```
   * <vcf-enhanced-rich-text-editor></vcf-enhanced-rich-text-editor>
   * ```
   *
   * Vaadin Rich Text Editor focuses on the structure, not the styling of content.
   * Therefore, the semantic HTML5 tags such as <h1>, <strong> and <ul> are used,
   * and CSS usage is limited to most common cases, like horizontal text alignment.
   *
   * ### Styling
   *
   * The following state attributes are available for styling:
   *
   * Attribute    | Description | Part name
   * -------------|-------------|------------
   * `disabled`   | Set to a disabled text editor | :host
   * `readonly`   | Set to a readonly text editor | :host
   * `on`         | Set to a toolbar button applied to the selected text | toolbar-button
   *
   * The following shadow DOM parts are available for styling:
   *
   * Part name                            | Description
   * -------------------------------------|----------------
   * `content`                            | The content wrapper
   * `toolbar`                            | The toolbar wrapper
   * `toolbar-group`                      | The group for toolbar controls
   * `toolbar-group-history`              | The group for histroy controls
   * `toolbar-group-emphasis`             | The group for emphasis controls
   * `toolbar-group-heading`              | The group for heading controls
   * `toolbar-group-glyph-transformation` | The group for glyph transformation controls
   * `toolbar-group-group-list`           | The group for group list controls
   * `toolbar-group-group-indent`         | The group for indent controls
   * `toolbar-group-alignment`            | The group for alignment controls
   * `toolbar-group-rich-text`            | The group for rich text controls
   * `toolbar-group-block`                | The group for preformatted block controls
   * `toolbar-group-format`               | The group for format controls
   * `toolbar-button`                     | The toolbar button (applies to all buttons)
   * `toolbar-button-undo`                | The "undo" button
   * `toolbar-button-redo`                | The "redo" button
   * `toolbar-button-bold`                | The "bold" button
   * `toolbar-button-italic`              | The "italic" button
   * `toolbar-button-underline`           | The "underline" button
   * `toolbar-button-strike`              | The "strike-through" button
   * `toolbar-button-h1`                  | The "header 1" button
   * `toolbar-button-h2`                  | The "header 2" button
   * `toolbar-button-h3`                  | The "header 3" button
   * `toolbar-button-subscript`           | The "subscript" button
   * `toolbar-button-superscript`         | The "superscript" button
   * `toolbar-button-list-ordered`        | The "ordered list" button
   * `toolbar-button-list-bullet`         | The "bullet list" button
   * `toolbar-button-deindent`            | The "decrease indent" button
   * `toolbar-button-indent`              | The "increase indent" button
   * `toolbar-button-align-left`          | The "left align" button
   * `toolbar-button-align-center`        | The "center align" button
   * `toolbar-button-align-right`         | The "right align" button
   * `toolbar-button-align-justify`       | The "justify align" button
   * `toolbar-button-image`               | The "image" button
   * `toolbar-button-link`                | The "link" button
   * `toolbar-button-blockquote`          | The "blockquote" button
   * `toolbar-button-code-block`          | The "code block" button
   * `toolbar-button-clean`               | The "clean formatting" button
   *
   * See [ThemableMixin – how to apply styles for shadow parts](https://github.com/vaadin/vaadin-themable-mixin/wiki)
   *
   * ### Keyboard Hotkeys
   *
   * Keyboard Hotkeys | Description
   * --|--
   * `Alt + F10` | Focus on the toolbar.
   * `Shift + Space` | Insert non-breaking space.
   * `Ctrl + P` (Mac: `Meta + P`) | Insert placeholder.
   *
   * @memberof Vaadin
   * @mixes Vaadin.ElementMixin
   * @mixes Vaadin.ThemableMixin
   * @demo demo/index.html
   */
  class VcfEnhancedRichTextEditor extends ElementMixin(ThemableMixin(PolymerElement)) {
    static get template() {
      return html`
        <style include="vcf-enhanced-rich-text-editor-styles">
          :host {
            display: flex;
            flex-direction: column;
            box-sizing: border-box;
          }

          :host([hidden]),
          button[hidden] {
            display: none !important;
          }

          .announcer {
            position: fixed;
            clip: rect(0, 0, 0, 0);
          }

          input[type='file'] {
            display: none;
          }

          .vcf-enhanced-rich-text-editor-container {
            display: flex;
            flex-direction: column;
            min-height: inherit;
            max-height: inherit;
            flex: auto;
          }

          .ql-readonly {
            color: #676767;
            /* background: #f9f9f9; */
            background: #f1f1f1;
            border-radius: 0.1em;
          }

          /* FIXME (Yuriy): workaround for auto-grow feature in flex layout for IE11 */
          @media all and (-ms-high-contrast: none) {
            .ql-editor {
              flex: auto;
            }
          }
        </style>

        <div class="vcf-enhanced-rich-text-editor-container">
          <!-- Create toolbar container -->
          <div part="toolbar">
            <slot name="toolbar-start"></slot>
            <slot name="toolbar-before-group-history"></slot>

            <span part="toolbar-group toolbar-group-history" style="display: [[_buttonGroupDisplay(toolbarButtons, 'history')]];">
              <!-- Undo and Redo -->
              <button type="button" part="toolbar-button toolbar-button-undo" on-click="_undo" title$="[[i18n.undo]]" style="display: [[_buttonDisplay(toolbarButtons, 'undo')]];">
                <slot name="undo">
                  <vaadin-icon part="toolbar-button-icon toolbar-button-undo-icon"></vaadin-icon>
                </slot>
              </button>
              <button type="button" part="toolbar-button toolbar-button-redo" on-click="_redo" title$="[[i18n.redo]]" style="display: [[_buttonDisplay(toolbarButtons, 'redo')]];">
                <slot name="redo">
                  <vaadin-icon part="toolbar-button-icon toolbar-button-redo-icon"></vaadin-icon>
                </slot>
              </button>
            </span>
            <slot name="toolbar-after-group-history"></slot>
            <slot name="toolbar-before-group-emphasis"></slot>

            <span part="toolbar-group toolbar-group-emphasis" style="display: [[_buttonGroupDisplay(toolbarButtons, 'emphasis')]];">
              <!-- Bold -->
              <button class="ql-bold" part="toolbar-button toolbar-button-bold" title$="[[i18n.bold]]" style="display: [[_buttonDisplay(toolbarButtons, 'bold')]];">
                <slot name="bold">
                  <vaadin-icon part="toolbar-button-icon toolbar-button-bold-icon"></vaadin-icon>
                </slot>
              </button>

              <!-- Italic -->
              <button class="ql-italic" part="toolbar-button toolbar-button-italic" title$="[[i18n.italic]]" style="display: [[_buttonDisplay(toolbarButtons, 'italic')]];">
                <slot name="italic">
                  <vaadin-icon part="toolbar-button-icon toolbar-button-italic-icon"></vaadin-icon>
                </slot>
              </button>

              <!-- Underline -->
              <button class="ql-underline" part="toolbar-button toolbar-button-underline" title$="[[i18n.underline]]" style="display: [[_buttonDisplay(toolbarButtons, 'underline')]];">
                <slot name="underline">
                  <vaadin-icon part="toolbar-button-icon toolbar-button-underline-icon"></vaadin-icon>
                </slot>
              </button>

              <!-- Strike -->
              <button class="ql-strike" part="toolbar-button toolbar-button-strike" title$="[[i18n.strike]]" style="display: [[_buttonDisplay(toolbarButtons, 'strike')]];">
                <slot name="strike">
                  <vaadin-icon part="toolbar-button-icon toolbar-button-strike-icon"></vaadin-icon>
                </slot>
              </button>
            </span>
            <slot name="toolbar-after-group-emphasis"></slot>
            <slot name="toolbar-before-group-heading"></slot>

            <span part="toolbar-group toolbar-group-heading" style="display: [[_buttonGroupDisplay(toolbarButtons, 'heading')]];">
              <!-- Header buttons -->
              <button type="button" class="ql-header" value="1" part="toolbar-button toolbar-button-h1" title$="[[i18n.h1]]" style="display: [[_buttonDisplay(toolbarButtons, 'h1')]];">
                <slot name="h1">
                  <vaadin-icon part="toolbar-button-icon toolbar-button-h1-icon"></vaadin-icon>
                </slot>
              </button>
              <button type="button" class="ql-header" value="2" part="toolbar-button toolbar-button-h2" title$="[[i18n.h2]]" style="display: [[_buttonDisplay(toolbarButtons, 'h2')]];">
                <slot name="h2">
                  <vaadin-icon part="toolbar-button-icon toolbar-button-h2-icon"></vaadin-icon>
                </slot>
              </button>
              <button type="button" class="ql-header" value="3" part="toolbar-button toolbar-button-h3" title$="[[i18n.h3]]" style="display: [[_buttonDisplay(toolbarButtons, 'h3')]];">
                <slot name="h3">
                  <vaadin-icon part="toolbar-button-icon toolbar-button-h3-icon"></vaadin-icon>
                </slot>
              </button>
            </span>
            <slot name="toolbar-after-group-heading"></slot>
            <slot name="toolbar-before-group-glyph-transformation"></slot>

            <span part="toolbar-group toolbar-group-glyph-transformation" style="display: [[_buttonGroupDisplay(toolbarButtons, 'glyph-transformation')]];">
              <!-- Subscript and superscript -->
              <button class="ql-script" value="sub" part="toolbar-button toolbar-button-subscript" title$="[[i18n.subscript]]" style="display: [[_buttonDisplay(toolbarButtons, 'subscript')]];">
                <slot name="subscript">
                  <vaadin-icon part="toolbar-button-icon toolbar-button-subscript-icon"></vaadin-icon>
                </slot>
              </button>
              <button class="ql-script" value="super" part="toolbar-button toolbar-button-superscript" title$="[[i18n.superscript]]" style="display: [[_buttonDisplay(toolbarButtons, 'superscript')]];">
                <slot name="superscript">
                  <vaadin-icon part="toolbar-button-icon toolbar-button-superscript-icon"></vaadin-icon>
                </slot>
              </button>
            </span>
            <slot name="toolbar-after-group-glyph-transformation"></slot>
            <slot name="toolbar-before-group-list"></slot>

            <span part="toolbar-group toolbar-group-list" style="display: [[_buttonGroupDisplay(toolbarButtons, 'list')]];">
              <!-- List buttons -->
              <button type="button" class="ql-list" value="ordered" part="toolbar-button toolbar-button-list-ordered" title$="[[i18n.listOrdered]]" style="display: [[_buttonDisplay(toolbarButtons, 'listOrdered')]];">
                <slot name="listOrdered">
                  <vaadin-icon part="toolbar-button-icon toolbar-button-list-ordered-icon"></vaadin-icon>
                </slot>
              </button>
              <button type="button" class="ql-list" value="bullet" part="toolbar-button toolbar-button-list-bullet" title$="[[i18n.listBullet]]" style="display: [[_buttonDisplay(toolbarButtons, 'listBullet')]];">
                <slot name="listBullet">
                  <vaadin-icon part="toolbar-button-icon toolbar-button-list-bullet-icon"></vaadin-icon>
                </slot>
              </button>
            </span>
            <slot name="toolbar-after-group-list"></slot>
            <slot name="toolbar-before-group-indent"></slot>

            <span part="toolbar-group toolbar-group-indent" style="display: [[_buttonGroupDisplay(toolbarButtons, 'indent')]];">
              <!-- List buttons -->
              <button type="button" class="ql-indent" value="-1" part="toolbar-button toolbar-button-deindent" title$="[[i18n.deindent]]" style="display: [[_buttonDisplay(toolbarButtons, 'deindent')]];">
                <slot name="deindent">
                  <vaadin-icon icon="vcf-erte-extra-icons:deindent-icon" part="toolbar-button-deindent-icon"></vaadin-icon>
                </slot>
              </button>
              <button type="button" class="ql-indent" value="+1" part="toolbar-button toolbar-button-indent" title$="[[i18n.indent]]" style="display: [[_buttonDisplay(toolbarButtons, 'indent')]];">
                <slot name="indent">
                  <vaadin-icon icon="vcf-erte-extra-icons:indent-icon" part="toolbar-button-indent-icon"></vaadin-icon>
                </slot>
              </button>
            </span>
            <slot name="toolbar-after-group-indent"></slot>
            <slot name="toolbar-before-group-alignment"></slot>

            <span part="toolbar-group toolbar-group-alignment" style="display: [[_buttonGroupDisplay(toolbarButtons, 'alignment')]];">
              <!-- Align buttons -->
              <button type="button" class="ql-align" value="" part="toolbar-button toolbar-button-align-left" title$="[[i18n.alignLeft]]" style="display: [[_buttonDisplay(toolbarButtons, 'alignLeft')]];">
                <slot name="alignLeft">
                  <vaadin-icon icon="vcf-erte-extra-icons:align-left-icon" part="toolbar-button-align-left-icon"></vaadin-icon>
                </slot>
              </button>
              <button type="button" class="ql-align" value="center" part="toolbar-button toolbar-button-align-center" title$="[[i18n.alignCenter]]" style="display: [[_buttonDisplay(toolbarButtons, 'alignCenter')]];">
                <slot name="alignCenter">
                  <vaadin-icon icon="vcf-erte-extra-icons:align-center-icon" part="toolbar-button-align-center-icon"></vaadin-icon>
                </slot>
              </button>
              <button type="button" class="ql-align" value="right" part="toolbar-button toolbar-button-align-right" title$="[[i18n.alignRight]]" style="display: [[_buttonDisplay(toolbarButtons, 'alignRight')]];">
                <slot name="alignRight">
                  <vaadin-icon icon="vcf-erte-extra-icons:align-right-icon" part="toolbar-button-align-right-icon"></vaadin-icon>
                </slot>
              </button>
              <button type="button" class="ql-align" value="justify" part="toolbar-button toolbar-button-align-justify" title$="[[i18n.alignJustify]]" style="display: [[_buttonDisplay(toolbarButtons, 'alignJustify')]];">
                <slot name="alignJustify">
                  <vaadin-icon icon="vcf-erte-extra-icons:align-justify-icon" part="toolbar-button-align-justify-icon"></vaadin-icon>
                </slot>
              </button>
            </span>
            <slot name="toolbar-after-group-alignment"></slot>
            <slot name="toolbar-before-group-rich-text"></slot>

            <span part="toolbar-group toolbar-group-rich-text" style="display: [[_buttonGroupDisplay(toolbarButtons, 'rich-text')]];">
              <!-- Image -->
              <button type="button" part="toolbar-button toolbar-button-image" title$="[[i18n.image]]" on-touchend="_onImageTouchEnd" on-click="_onImageClick" style="display: [[_buttonDisplay(toolbarButtons, 'image')]];">
                <slot name="image">
                  <vaadin-icon part="toolbar-button-icon toolbar-button-image-icon"></vaadin-icon>
                </slot>
              </button>
              <!-- Link -->
              <button type="button" part="toolbar-button toolbar-button-link" title$="[[i18n.link]]" on-click="_onLinkClick" style="display: [[_buttonDisplay(toolbarButtons, 'link')]];">
                <slot name="link">
                  <vaadin-icon part="toolbar-button-icon toolbar-button-link-icon"></vaadin-icon>
                </slot>
              </button>
            </span>
            <slot name="toolbar-after-group-rich-text"></slot>
            <slot name="toolbar-before-group-block"></slot>

            <span part="toolbar-group toolbar-group-block" style="display: [[_buttonGroupDisplay(toolbarButtons, 'block')]];">
              <!-- Blockquote -->
              <button type="button" class="ql-blockquote" part="toolbar-button toolbar-button-blockquote" title$="[[i18n.blockquote]]" style="display: [[_buttonDisplay(toolbarButtons, 'blockquote')]];">
                <slot name="blockquote">
                  <vaadin-icon part="toolbar-button-icon toolbar-button-blockquote-icon"></vaadin-icon>
                </slot>
              </button>

              <!-- Code block -->
              <button type="button" class="ql-code-block" part="toolbar-button toolbar-button-code-block" title$="[[i18n.codeBlock]]" style="display: [[_buttonDisplay(toolbarButtons, 'codeBlock')]];">
                <slot name="codeBlock">
                  <vaadin-icon part="toolbar-button-icon toolbar-button-code-block-icon"></vaadin-icon>
                </slot>
              </button>

              <!-- Placeholder -->
              <button id="placeholderBtn" type="button" class="ql-placeholder" part="toolbar-button toolbar-button-placeholder" title$="[[i18n.placeholder]]" style="display: [[_buttonDisplay(toolbarButtons, 'placeholder')]];" hidden>
                [[placeholderTags.start]]
              </button>

              <!-- Placeholder display -->
              <button id="placeholderAppearanceBtn" type="button" part="toolbar-button toolbar-button-placeholder-display" title$="[[i18n.placeholderAppearance]]" style="display: [[_buttonDisplay(toolbarButtons, 'placeholderAppearance')]];" hidden>
                [[placeholderAppearance]]
              </button>
            </span>
            <slot name="toolbar-after-group-block"></slot>
            <slot name="toolbar-before-group-format"></slot>

            <span part="toolbar-group toolbar-group-format" style="display: [[_buttonGroupDisplay(toolbarButtons, 'format')]];">
              <!-- Show Whitespace -->
              <button type="button" class="rte-whitespace" part="toolbar-button toolbar-button-whitespace" title$="[[i18n.whitespace]]" style="display: [[_buttonDisplay(toolbarButtons, 'whitespace')]];" on-click="_onWhitespaceClick">
                <slot name="whitespace">
                  <vaadin-icon icon="vcf-erte-extra-icons:whitespace-icon" part="toolbar-button-whitespace-icon"></vaadin-icon>
                </slot>
              </button>

              <!-- Read-only -->
              <button type="button" class="rte-readonly" part="toolbar-button toolbar-button-readonly" title$="[[i18n.readonly]]" style="display: [[_buttonDisplay(toolbarButtons, 'readonly')]];" on-click="_onReadonlyClick">
                <slot name="readonly">
                  <vaadin-icon icon="vcf-erte-extra-icons:lock-icon" part="toolbar-button-readonly-icon"></vaadin-icon>
                </slot>
              </button>

              <!-- Clean -->
              <button type="button" class="ql-clean" part="toolbar-button toolbar-button-clean" title$="[[i18n.clean]]" style="display: [[_buttonDisplay(toolbarButtons, 'clean')]];">
                <slot name="clean">
                  <vaadin-icon part="toolbar-button-icon toolbar-button-clean-icon"></vaadin-icon>
                </slot>
              </button>
            </span>
            <slot name="toolbar-after-group-format"></slot>
            <slot name="toolbar-before-group-custom"></slot>

            <span part="toolbar-group toolbar-group-custom">
              <slot name="toolbar" on-slot-change="_onToolbarSlotChange"></slot>
            </span>
            <slot name="toolbar-after-group-custom"></slot>
            <slot name="toolbar-end"></slot>

            <input id="fileInput" type="file" accept="image/png, image/gif, image/jpeg, image/bmp, image/x-icon" on-change="_uploadImage" />

          </div>

          <div style="overflow: hidden; box-sizing: content-box; width: 100% !important; height: 15px !important; flex-shrink: 0; display: [[_rulerDisplayFlexWrapper(noRulers)]];">
            <div style="overflow: hidden; box-sizing: content-box; border-color: rgb(158, 170, 182); border-style: solid; border-width: 0 1px 1px 0; width: 14px !important; height: 14px !important; display: [[_rulerDisplay(noRulers)]];"></div>
            <div style="position:relative; overflow: hidden; box-sizing: content-box; background: url('[[_rulerHori]]') repeat-x; flex-grow: 1; height: 15px !important; padding: 0; display: [[_rulerDisplay(noRulers)]];" on-click="_addTabStop" part="horizontalRuler"></div>
          </div>

          <div style="display: flex; flex-grow: 1; overflow: auto;">
            <div style="overflow: hidden; box-sizing: content-box; background: url('[[_rulerVert]]') repeat-y; width: 15px !important; flex-shrink: 0; display: [[_rulerDisplay(noRulers)]];" part="verticalRuler"></div>
            <div part="content"></div>
          </div>

          <div class="announcer" aria-live="polite"></div>
        </div>

        <vaadin-confirm-dialog id="linkDialog" opened="{{_linkEditing}}" header="[[i18n.linkDialogTitle]]">
          <vaadin-text-field id="linkUrl" value="{{_linkUrl}}" style="width: 100%;" on-keydown="_onLinkKeydown"> </vaadin-text-field>
          <vaadin-button id="confirmLink" slot="confirm-button" theme="primary" on-click="_onLinkEditConfirm">
            [[i18n.ok]]
          </vaadin-button>
          <vaadin-button id="removeLink" slot="reject-button" theme="error" on-click="_onLinkEditRemove" hidden$="[[!_linkRange]]">
            [[i18n.remove]]
          </vaadin-button>
          <vaadin-button id="cancelLink" slot="cancel-button" on-click="_onLinkEditCancel">
            [[i18n.cancel]]
          </vaadin-button>
        </vaadin-confirm-dialog>

        <vaadin-confirm-dialog id="placeholderDialog" header="[[i18n.placeholderDialogTitle]]">
          <vaadin-combo-box label="[[i18n.placeholderComboBoxLabel]]" id="placeholderComboBox" value="{{_placeholder}}" item-label-path="text" item-value-path="text" style="width: 100%;" on-value-changed="{{_placeholderChanged}}"></vaadin-combo-box>
          <vaadin-button slot="confirm-button" theme="primary" on-click="_onPlaceholderEditConfirm">
            [[i18n.ok]]
          </vaadin-button>
          <vaadin-button id="placeholderRemoveButton" slot="reject-button" theme="error" on-click="_onPlaceholderEditRemove" hidden$="[[!_placeholderRange]]">
            [[i18n.remove]]
          </vaadin-button>
          <vaadin-button slot="cancel-button" on-click="_onPlaceholderEditCancel">
            [[i18n.cancel]]
          </vaadin-button>
        </vaadin-confirm-dialog>
      `;
    }

    static get is() {
      return 'vcf-enhanced-rich-text-editor';
    }

    static get version() {
      return '3.0.1';
    }

    static get properties() {
      return {
        /**
         * Value is a list of the operations which describe change to the document.
         * Each of those operations describe the change at the current index.
         * They can be an `insert`, `delete` or `retain`. The format is as follows:
         *
         * ```js
         *  [
         *    { insert: 'Hello World' },
         *    { insert: '!', attributes: { bold: true }}
         *  ]
         * ```
         *
         * See also https://github.com/quilljs/delta for detailed documentation.
         */
        value: {
          type: String,
          notify: true,
          value: ''
        },

        /**
         * HTML representation of the rich text editor content.
         */
        htmlValue: {
          type: String,
          notify: true,
          readOnly: true
        },

        /**
         * When true, the user can not modify, nor copy the editor content.
         */
        disabled: {
          type: Boolean,
          value: false,
          reflectToAttribute: true
        },

        /**
         * When true, the rulers are not visible.
         */
        noRulers: {
          type: Boolean,
          value: false,
          reflectToAttribute: true,
          notify: true
        },

        /**
         * When true, the user can not modify the editor content, but can copy it.
         */
        readonly: {
          type: Boolean,
          value: false,
          reflectToAttribute: true
        },

        /**
         * An object used to localize this component. The properties are used
         * e.g. as the tooltips for the editor toolbar buttons.
         *
         * @default {English/US}
         */
        i18n: {
          type: Array,
          value: () => {
            return {
              undo: 'undo',
              redo: 'redo',
              bold: 'bold',
              italic: 'italic',
              underline: 'underline',
              strike: 'strike',
              h1: 'h1',
              h2: 'h2',
              h3: 'h3',
              subscript: 'subscript',
              superscript: 'superscript',
              listOrdered: 'list ordered',
              listBullet: 'list bullet',
              deindent: 'decrease indent',
              indent: 'increase indent',
              alignLeft: 'align left',
              alignCenter: 'align center',
              alignRight: 'align right',
              alignJustify: 'align justify',
              image: 'image',
              link: 'link',
              blockquote: 'blockquote',
              codeBlock: 'code block',
              whitespace: 'show whitespace',
              readonly: 'readonly',
              placeholder: 'placeholder',
              placeholderAppearance: 'toggle placeholder appearance',
              placeholderComboBoxLabel: 'Select a placeholder',
              placeholderAppearanceLabel1: 'Plain',
              placeholderAppearanceLabel2: 'Value',
              placeholderDialogTitle: 'Placeholders',
              clean: 'clean',
              linkDialogTitle: 'Link address',
              ok: 'OK',
              cancel: 'Cancel',
              remove: 'Remove'
            };
          }
        },

        /**
         * An object used to show/hide toolbar buttons.
         * Default value of any unspecified button is true.
         */
        toolbarButtons: {
          type: Object,
          value: {}
        },

        tabStops: {
          type: Array,
          notify: true,
          value: () => []
        },

        /**
         * When true, whitespace indicators are shown (→ tab, ↵ soft-break, ¶ paragraph, ⮐ auto-wrap).
         */
        showWhitespace: {
          type: Boolean,
          value: false,
          reflectToAttribute: true,
          observer: '_showWhitespaceChanged'
        },

        /**
         * An array of strings or a `Placeholder` objects. Here is the syntax for a `Placeholder` object:
         * ```
         * {
         *   text: 'placeholder',
         *   format: { bold: true, italic: false }, // main placeholder format
         *   altFormat:  { underline: true, bold: false } // alternate placeholder appearance format
         * }
         * ```
         * The `format` and `altFormat` properties accept [Inline](https://quilljs.com/docs/formats/#inline) formats.
         */
        placeholders: {
          type: Array,
          notify: true,
          observer: '_placeholdersChanged'
        },

        /**
         * Object containing `start` and `end` properties used for the start and end tags of a placeholder.
         */
        placeholderTags: {
          type: Object,
          value: () => ({
            start: '@',
            end: ''
          }),
          observer: '_placeholderTagsChanged'
        },

        /**
         * Label for current placeholder appearance.
         */
        placeholderAppearance: String,

        /**
         * Returns whether alternate appearance is active.
         */
        placeholderAltAppearance: {
          type: Boolean,
          observer: '_placeholderAltAppearanceChanged'
        },

        /**
         * Regular expression used for placeholder alternate appearance.
         */
        placeholderAltAppearancePattern: {
          type: String,
          observer: '_placeholderAltAppearancePatternChanged'
        },

        _editor: {
          type: Object
        },

        /**
         * Stores old value
         */
        _oldValue: String,

        _lastCommittedChange: {
          type: String,
          value: ''
        },

        _linkEditing: {
          type: Boolean
        },

        _linkRange: {
          type: Object,
          value: null
        },

        _linkIndex: {
          type: Number,
          value: null
        },

        _linkUrl: {
          type: String,
          value: ''
        },

        _rulerHori: {
          type: String,
          value:
          // eslint-disable-next-line max-len
              'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAV4AAAAPBAMAAABeoLrPAAAAA3NCSVQICAjb4U/gAAAAHlBMVEXS0tLR0dHQ0NCerLmfq7eeqrafqbOdqbWcqLT///9ePaWcAAAACnRSTlP///////////8AsswszwAAAAlwSFlzAAALEgAACxIB0t1+/AAAACB0RVh0U29mdHdhcmUATWFjcm9tZWRpYSBGaXJld29ya3MgTVi7kSokAAAAFnRFWHRDcmVhdGlvbiBUaW1lADA1LzEwLzEyhpCxGgAAAKtJREFUeJztksENgCAMRXt1BEZgICdwBvco3NxWqwYDFGMrajT2QOD/0v8kwvCugqcBhPXzXluf4XViA+uNKmfIeX09Q5Eh5y0+o9xQZFT8H24xINgXLwmMdtl4fVjcruYO9nEans6YeA2NMSQaEtedYzQMx0RLbkTzbHmeImPibWhrY8cy2to3IyRalM7P89ldVQZk39ksPZhpXJ9hUHfeDanlVAZ0ffumGgEWlrgeDxx/xAAAAABJRU5ErkJggg=='
        },

        _rulerVert: {
          type: String,
          value:
          // eslint-disable-next-line max-len
              'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAA8AAAAyBAMAAABxHJwKAAAAA3NCSVQICAjb4U/gAAAAG1BMVEXS0tLR0dHQ0NCfq7eeqradq7idqbWcqLT///+TeDeAAAAACXRSTlP//////////wBTT3gSAAAACXBIWXMAAAsSAAALEgHS3X78AAAAIHRFWHRTb2Z0d2FyZQBNYWNyb21lZGlhIEZpcmV3b3JrcyBNWLuRKiQAAAAWdEVYdENyZWF0aW9uIFRpbWUAMDUvMTAvMTKGkLEaAAAATklEQVR4nGPogIAABijDAMZwQGM0CqKLYGNAtDcK4lOcgGGyAS4pDF1NgoIJuJ2KLtKIUIxpcgKGmzHV4AkNTClc2pFDo4Bq4awoCAYOAKbZvafXusxYAAAAAElFTkSuQmCC'
        },

        _placeholderEditing: {
          type: Boolean,
          observer: '_placeholderEditingChanged'
        },

        _placeholderRange: {
          type: Object,
          value: null
        },

        _placeholderIndex: {
          type: Number,
          value: null
        },

        _placeholder: {
          type: String,
          value: ''
        },

        _allToolbarButtons: {
          type: Array,
          notify: true,
          value: () => []
        }
      };
    }

    _rulerDisplay(noruler) {
      if (noruler) {
        return 'none';
      } else {
        return 'block';
      }
    }

    _rulerDisplayFlexWrapper(noruler) {
      if (noruler) {
        return 'none';
      } else {
        return 'flex';
      }
    }

    _buttonDisplay(toolbarButtons, button) {
      if (toolbarButtons[button] === false) return 'none';
      return '';
    }

    _buttonGroupDisplay(toolbarButtons, group) {
      var visible = false;
      TOOLBAR_BUTTON_GROUPS[group].forEach(button => {
        if (toolbarButtons[button] !== false) {
          visible = true;
          return;
        }
      });

      return visible ? '' : 'none';
    }

    // ============================================
    // Tab Width Calculation Engine (from prototype)
    // ============================================

    /**
     * Create reusable measure span for text width calculation.
     */
    _createMeasureSpan() {
      if (this._measureSpan) return;
      this._measureSpan = document.createElement('span');
      this._measureSpan.style.cssText = 'visibility:hidden;position:absolute;white-space:pre;left:-9999px;top:-9999px';
      this.shadowRoot.appendChild(this._measureSpan);
    }

    /**
     * RAF-based coalescing for tab width updates.
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
     */
    _updateTabWidths() {
      if (!this._editor) return;

      const editorNode = this._editor.root;
      const tabs = Array.from(editorNode.querySelectorAll('.ql-tab'));

      if (tabs.length === 0) return;

      const charWidth8 = this._measureTextWidth("0".repeat(TAB_DEFAULT_TAB_CHARS), editorNode);
      const fixedTabWidth = charWidth8 > 0 ? charWidth8 : TAB_FIXED_TAB_FALLBACK;

      const blockVisualLines = new Map();

      // Editor rect is hoisted outside the loop since the editor's outer dimensions
      // don't change during iteration. Per-tab rects must be read inside the loop because
      // each tab's position depends on the previous tab's width (iterative algorithm).
      let editorRect = editorNode.getBoundingClientRect();

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
     * Get computed style for an element.
     * Note: getComputedStyle() returns a live CSSStyleDeclaration that always reflects
     * current values, so caching the object provides no benefit.
     */
    _getComputedStyleFor(element) {
      return window.getComputedStyle(element);
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
     * Observer for showWhitespace property.
     */
    _showWhitespaceChanged(show) {
      const editor = this.shadowRoot.querySelector('.ql-editor');
      if (editor) editor.classList.toggle('show-whitespace', show);
      const btn = this.shadowRoot.querySelector('[part~="toolbar-button-whitespace"]');
      if (btn) {
        btn.classList.toggle('ql-active', show);
        if (show) {
          btn.setAttribute('on', '');
        } else {
          btn.removeAttribute('on');
        }
      }
    }

    /**
     * Handle ArrowUp/ArrowDown through tab-filled lines.
     * Tab blots are inline-block with tiny/zero font-size, which confuses
     * the browser's vertical cursor navigation (it skips intermediate lines).
     * This method manually computes the target line and positions the cursor.
     * @param {Object} range - current Quill selection
     * @param {number} direction - -1 for ArrowUp, +1 for ArrowDown
     * @returns {boolean} false to prevent default browser handling
     */
    _handleArrowNavigation(range, direction) {
      const quill = this._editor;
      if (!quill) return true;

      const allLines = quill.getLines(0, quill.getLength());
      if (allLines.length <= 1) return true;

      // Find current line index
      const [currentLine] = quill.getLine(range.index);
      if (!currentLine) return true;
      let currentLineIdx = -1;
      for (let i = 0; i < allLines.length; i++) {
        if (allLines[i] === currentLine) { currentLineIdx = i; break; }
      }
      if (currentLineIdx < 0) return true;

      // Check if adjacent line contains tab blots (the problematic case)
      const targetLineIdx = currentLineIdx + direction;
      if (targetLineIdx < 0 || targetLineIdx >= allLines.length) return true;

      const targetLine = allLines[targetLineIdx];
      const targetLineStart = quill.getIndex(targetLine);
      const targetLineLen = targetLine.length() - 1; // exclude trailing newline

      // Check if target line or current line has tabs
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
        // Neither line has tabs - let browser handle normally
        return true;
      }

      // Get current cursor X position
      const currentBounds = quill.getBounds(range.index);
      const targetX = currentBounds.left;

      // Find the position on the target line closest to the current X
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

    static get observers() {
      return ['_valueChanged(value, _editor)', '_disabledChanged(disabled, readonly, _editor)', '_tabStopsChanged(tabStops, _editor)'];
    }

    constructor() {
      super();
      this._setCustomButtons();
    }

    ready() {
      super.ready();

      const editor = this.shadowRoot.querySelector('[part="content"]');
      const toolbarConfig = this._prepareToolbar();
      this._toolbar = toolbarConfig.container;

      this._addToolbarListeners();

      // Tab engine: initialize caches and measure span BEFORE Quill creation.
      // Setting this._editor triggers the _tabStopsChanged observer which writes
      // to _tabStopsArray, so these must exist before that happens.
      this._textWidthCache = new Map();
      this._tabStopsArray = [];
      this._tabUpdateRafId = null;
      this._createMeasureSpan();

      let options = {
        modules: {
          toolbar: toolbarConfig,
        }
      };

      // If there is some registered callback, run that
      if (typeof window.Vaadin.Flow.vcfEnhancedRichTextEditor === "object"
          && Array.isArray(window.Vaadin.Flow.vcfEnhancedRichTextEditor.extendOptions)) {

        window.Vaadin.Flow.vcfEnhancedRichTextEditor.extendOptions.forEach(cb => cb(options, Quill));
      }

      this._editor = new Quill(editor, options);
      const _editor = this._editor;

      this._editor.on('text-change', function(delta, oldDelta, source) {
        if (source === 'user' && delta.ops.some(o => !!o.delete)) {
          // Prevent user to delete a readonly Blot
          const currentDelta = _editor.getContents().ops;
          if (oldDelta.ops.some(v => !!v.insert && v.attributes && v.attributes.readonly)) {
            // There were readonly sections in the previous value. Check for them in the new value.
            const readonlySectionsCount = oldDelta.ops.filter(v => !!v.insert && v.attributes && v.attributes.readonly).length;
            const newReadonlySectionsCount = currentDelta.filter(v => !!v.insert && v.attributes && v.attributes.readonly).length;

            if (readonlySectionsCount != newReadonlySectionsCount) {
              _editor.setContents(oldDelta);
              _editor.setSelection(delta.ops[0].retain + 1, 0);
            }
          }
        }
      });

      this._patchToolbar();
      this._patchKeyboard();

      /* istanbul ignore if */
      if (navigator.userAgent.toLowerCase().indexOf('firefox') > -1 && useShadow) {
        this._patchFirefoxFocus();
      }

      this.$.linkDialog.$.dialog.$.overlay.addEventListener('vaadin-overlay-open', () => {
        this.$.linkUrl.focus();
      });

      const editorContent = editor.querySelector('.ql-editor');

      editorContent.setAttribute('role', 'textbox');
      editorContent.setAttribute('aria-multiline', 'true');

      this._editor.on('text-change', () => {
        const timeout = 200;
        this._debounceSetValue = Debouncer.debounce(this._debounceSetValue, timeOut.after(timeout), () => {
          if (!this._silentTextChange) this.value = JSON.stringify(this._editor.getContents().ops);
          this._silentTextChange = false;
        });
      });

      // Tab engine: update tab widths on every text change
      this._editor.on('text-change', () => {
        this._requestTabUpdate();
      });

      // Resize handler: recalculate tabs (text width cache is viewport-independent, no need to clear)
      this._resizeHandler = () => {
        this._requestTabUpdate();
      };
      window.addEventListener('resize', this._resizeHandler);

      // Register clipboard matchers for old-format HTML tags (paste migration)
      this._editor.clipboard.addMatcher('TAB', (node, delta) => {
        const Delta = Quill.imports.delta;
        return new Delta().insert({ tab: true });
      });
      this._editor.clipboard.addMatcher('PRE-TAB', (node, delta) => {
        const Delta = Quill.imports.delta;
        return new Delta().insert({ tab: true });
      });
      this._editor.clipboard.addMatcher('TABS-CONT', (node, delta) => {
        // Let Quill process children normally; the tabs-cont wrapper is just stripped
        return delta;
      });
      this._editor.clipboard.addMatcher('LINE-PART', (node, delta) => {
        // Let Quill process children normally; the line-part wrapper is just stripped
        return delta;
      });

      editorContent.addEventListener('focusout', () => {
        if (this._toolbarState === STATE.FOCUSED) {
          this._cleanToolbarState();
        } else {
          this._emitChangeEvent();
        }
      });

      editorContent.addEventListener('focus', () => {
        // format changed, but no value changed happened
        if (this._toolbarState === STATE.CLICKED) {
          this._cleanToolbarState();
        }
      });

      this._editor.on('selection-change', this._announceFormatting.bind(this));

      // Ensure tabStopsArray is populated if tabStops was set before _editor was ready.
      // The Polymer complex observer _tabStopsChanged(tabStops, _editor) may not fire if
      // tabStops was set before _editor, so we explicitly initialize here.
      if (this.tabStops && this.tabStops.length > 0) {
        this._tabStopsChanged(this.tabStops, this._editor);
      }

      this._requestTabUpdate();

      this._editor.on('selection-change', opt => {
        if (opt !== null) {
          const timeout = 50;
          this.__debounceSetPlaceholder = Debouncer.debounce(this.__debounceSetPlaceholder, timeOut.after(timeout), () => {
            const placeholders = this.selectedPlaceholders;
            if (placeholders.length) {
              this._inPlaceholder = true;
              this.$.placeholderBtn.classList.add('ql-active');
              this.$.placeholderBtn.setAttribute('on', true);
              const detail = { placeholders };
              this.dispatchEvent(new CustomEvent('placeholder-select', { bubbles: true, cancelable: false, detail }));
            } else {
              if (this._inPlaceholder === true) this._inPlaceholder = false;
              this.$.placeholderBtn.classList.remove('ql-active');
              this.$.placeholderBtn.removeAttribute('on');
            }
            if (this._inPlaceholder === false) {
              this.dispatchEvent(new CustomEvent('placeholder-leave', { bubbles: true }));
              delete this._inPlaceholder;
            }

            let detailObject = {
              selected: undefined,
              path: [],
              isTable: false,
              isList: false
            };

            if (opt) {
              let lineElement = this._editor.getLine(opt.index)[0];
              if (lineElement) {
                let current = lineElement.domNode;

                if (this.__lastSelectedDomNode !== current) {
                  this.__lastSelectedDomNode = current;
                  detailObject.selected = current.tagName.toLowerCase();

                  while(current && current !== this._editor.root) {
                    let tagName = current.tagName;
                    if (tagName === "TABLE") {
                      detailObject.isTable = true;
                    } else if (tagName === "UL" || tagName === "OL") {
                      detailObject.isList = true;
                    }

                    detailObject.path.push(tagName.toLowerCase());
                    current = current.parentNode;
                  }

                  let event = new CustomEvent("selected-line-changed", {
                    detail: detailObject
                  });
                  this.dispatchEvent(event);
                }
              }
            } else {
              delete this.__lastSelectedDomNode;
              let event = new CustomEvent("selected-line-changed", {detail: detailObject});
              this.dispatchEvent(event);
            }
          });
        }
      });

      // Prevent cursor inside placeholder
      this._editor.root.addEventListener('selectstart', e => {
        let node = e.target.nodeType === 3 ? e.target.parentElement : e.target;
        const isPlaceholder = node => node.classList.contains('ql-placeholder');
        while (node.parentElement && !isPlaceholder(node)) node = node.parentElement;
        if (isPlaceholder(node)) {
          e.preventDefault();
          this._setSelectionNode(node.childNodes[2], 1);
        }
      });

      // Placeholder delete on character keypress
      this._editor.root.addEventListener('keypress', e => {
        const sel = this._editor.getSelection();
        if (sel && this._isCharacterKey(e) && sel.length && this.selectedPlaceholders.length) {
          e.preventDefault();
          this._removePlaceholders(this.selectedPlaceholders, false, e.key);
        }
      });

      // Placeholder insert on paste
      this._editor.clipboard.addMatcher('.ql-placeholder', (node, delta) => {
        const index = this._editor.selection.savedRange.index;
        const placeholder = node.dataset.placeholder;
        this._confirmInsertPlaceholders([{ placeholder, index }], false, true);
        return delta;
      });

      this._ready = true;
    }

    _onToolbarSlotChange() {
      this._setCustomButtons();
    }

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

    _setCustomButtons() {
      const buttons = this._customButtons;
      buttons.forEach((btn, i) => btn.setAttribute('part', `toolbar-button toolbar-button-custom-${i}`));
    }

    /**
     * Adds custom toolbar button.
     * @param {string} label
     * @param {string} icon
     * @param {Function} clickListener
     * @param {KeyboardShortcut} keyboardShortcut
     */
    addCustomButton(label, tooltip, icon = '', clickListener, keyboardShortcut) {
      const btn = document.createElement('vaadin-button');
      btn.setAttribute('slot', 'toolbar');
      this.setCustomButtonLabel(label, btn);
      this.setCustomButtonTooltip(tooltip, btn);
      this.setCustomButtonIcon(icon, btn);
      this.setCustomButtonClickListener(clickListener, btn);
      this.setCustomButtonKeyboardShortcut(keyboardShortcut, btn);
      this.appendChild(btn);
      this._setCustomButtons();
      this._updateToolbarButtons();
    }

    setCustomButtonLabel(label, btn) {
      if (btn && label) btn.innerText = label;
    }

    setCustomButtonTooltip(tooltip, btn) {
      if (btn && tooltip) btn.title = tooltip;
    }

    setCustomButtonIcon(icon, btn, suffix = false) {
      if (btn && icon) {
        let iconEl = btn.querySelector('vaadin-icon');
        if (!iconEl) {
          iconEl = document.createElement('vaadin-icon');
          btn.appendChild(iconEl);
        }
        if (btn.tagName.toLowerCase() !== 'vaadin-button') {
          const vaadinBtn = document.createElement('vaadin-button');
          vaadinBtn.setAttribute('title', btn.getAttribute('title'));
          vaadinBtn.setAttribute('part', btn.getAttribute('part'));
          vaadinBtn.appendChild(iconEl);
          vaadinBtn.innerText = btn.innerText;
          btn.parentElement.replaceChild(vaadinBtn, btn);
          btn = vaadinBtn;
        }
        if (!btn.innerText) btn.setAttribute('theme', 'icon');
        else iconEl.setAttribute('slot', suffix ? 'suffix' : 'prefix');
        iconEl.setAttribute('icon', icon);
      }
    }

    setCustomButtonClickListener(clickListener, btn) {
      if (btn && clickListener) btn.addEventListener('click', e => clickListener(e));
    }

    setCustomButtonKeyboardShortcut(keyboardShortcut, btn) {
      if (btn && keyboardShortcut) {
        const keyboard = this._editor.getModule('keyboard');
        const bindings = keyboard.bindings[keyboardShortcut.key] || [];
        keyboard.bindings[keyboardShortcut.key] = [
          {
            key: keyboardShortcut.key,
            shiftKey: keyboardShortcut.shiftKey,
            shortKey: keyboardShortcut.shortKey,
            altKey: keyboardShortcut.altKey,
            handler: keyboardShortcut.handler
          },
          ...bindings
        ];
      }
    }

    _prepareToolbar() {
      const clean = Quill.imports['modules/toolbar'].DEFAULTS.handlers.clean;
      const self = this;
      const toolbar = {
        container: this.shadowRoot.querySelector('[part="toolbar"]'),
        handlers: {
          clean: function() {
            self._markToolbarClicked();
            clean.call(this);
          }
        }
      };

      HANDLERS.forEach(handler => {
        toolbar.handlers[handler] = value => {
          if (handler === 'placeholder') {
            this._onPlaceholderClick();
          } else {
            this._markToolbarClicked();
            this._editor.format(handler, value, SOURCE.USER);
          }
        };
      });

      this.$.placeholderAppearanceBtn.classList.add('ql-active');
      this.$.placeholderAppearanceBtn.setAttribute('on', true);
      this.$.placeholderAppearanceBtn.addEventListener('click', () => {
        this._markToolbarClicked();
        this.placeholderAltAppearance = !this.placeholderAltAppearance;
      });

      return toolbar;
    }

    connectedCallback() {
      super.connectedCallback();
      this._updateToolbarButtons();

      // Re-initialize tab engine resources after reconnection
      if (this._editor && !this._measureSpan) {
        this._createMeasureSpan();
      }
      if (!this._textWidthCache) {
        this._textWidthCache = new Map();
      }
      if (this._editor && !this._resizeHandler) {
        this._resizeHandler = () => {
          this._requestTabUpdate();
        };
        window.addEventListener('resize', this._resizeHandler);
        this._requestTabUpdate();
      }
    }

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

    _updateToolbarButtons() {
      this._allToolbarButtons = this._toolbarButtons;
      // Disable tabbing to all buttons but the first one
      this._allToolbarButtons.forEach((button, index) => index > 0 && button.setAttribute('tabindex', '-1'));
    }

    _addToolbarListeners() {
      const toolbar = this._toolbar;

      toolbar.addEventListener('keydown', e => {
        const buttons = this._allToolbarButtons;

        // Use roving tab-index for the toolbar buttons
        if ([37, 39].indexOf(e.keyCode) > -1) {
          e.preventDefault();
          let index = buttons.indexOf(e.target);
          buttons[index].setAttribute('tabindex', '-1');

          // need to get in consideration if placeholders or any other button are hidden
          // or not when navigating through arrow keys
          if (e.keyCode === 39) {
            ++index;
            while (index < buttons.length && this._isHiddenButton(buttons, index)) {
              ++index;
            }
            if (index === buttons.length) {
              index = buttons.indexOf(this._getFirstVisibleToolbarButton());
            }
          } else if (e.keyCode === 37) {
            --index;
            while (index > -1 && index < buttons.length && this._isHiddenButton(buttons, index)) {
              --index;
            }
            if (index === -1) {
              index = buttons.length - 1;
            }
          }

          if ('vaadin-button' == buttons[index].tagName.toLowerCase()) {
            buttons[index].setAttribute('tabindex', '0');
          } else {
            buttons[index].removeAttribute('tabindex');
          }
          buttons[index].focus();
        }

        // Esc and Tab focuses the content
        if (e.keyCode === 27 || (e.keyCode === TAB_KEY && !e.shiftKey)) {
          e.preventDefault();
          this._editor.focus();
        }
      });

      // mousedown happens before editor focusout
      toolbar.addEventListener('mousedown', e => {
        const buttons = this._allToolbarButtons;
        if (buttons.indexOf(e.composedPath()[0]) > -1) {
          this._markToolbarFocused();
        }
      });
    }

    _isHiddenButton(buttons, index) {
      return buttons[index].hidden || buttons[index].style.display == 'none';
    }

    _markToolbarClicked() {
      this._toolbarState = STATE.CLICKED;
    }

    _markToolbarFocused() {
      this._toolbarState = STATE.FOCUSED;
    }

    _cleanToolbarState() {
      this._toolbarState = STATE.DEFAULT;
    }

    _createFakeFocusTarget() {
      const isRTL = document.documentElement.getAttribute('dir') == 'rtl';
      const elem = document.createElement('textarea');
      // Reset box model
      elem.style.border = '0';
      elem.style.padding = '0';
      elem.style.margin = '0';
      // Move element out of screen horizontally
      elem.style.position = 'absolute';
      elem.style[isRTL ? 'right' : 'left'] = '-9999px';
      // Move element to the same position vertically
      const yPosition = window.pageYOffset || document.documentElement.scrollTop;
      elem.style.top = `${yPosition}px`;
      return elem;
    }

    _patchFirefoxFocus() {
      // in Firefox 63 with native Shadow DOM, when moving focus out of
      // contenteditable and back again within same shadow root, cursor
      // disappears. See https://jsfiddle.net/webpadawan/g6vku9L3/
      const editorContent = this.shadowRoot.querySelector('.ql-editor');
      let isFake = false;

      const focusFake = () => {
        isFake = true;
        this._fakeTarget = this._createFakeFocusTarget();
        document.body.appendChild(this._fakeTarget);
        // let the focus step out of shadow root!
        this._fakeTarget.focus();
        return new Promise(resolve => setTimeout(resolve));
      };

      const focusBack = (offsetNode, offset) => {
        this._editor.focus();
        if (offsetNode) {
          this._editor.selection.setNativeRange(offsetNode, offset);
        }
        document.body.removeChild(this._fakeTarget);
        delete this._fakeTarget;
        isFake = false;
      };

      editorContent.addEventListener('mousedown', e => {
        if (!this._editor.hasFocus()) {
          const { x, y } = e;
          const { offset, offsetNode } = document.caretPositionFromPoint(x, y);
          focusFake().then(() => {
            focusBack(offsetNode, offset);
          });
        }
      });

      editorContent.addEventListener('focusin', () => {
        if (isFake === false) {
          focusFake().then(() => focusBack());
        }
      });
    }

    _patchToolbar() {
      const toolbar = this._editor.getModule('toolbar');
      const update = toolbar.update;

      // add custom link button to toggle state attribute
      const linkButton = this.shadowRoot.querySelector('[part~="toolbar-button-link"]');
      if (linkButton) {
        toolbar.controls.push(['link', linkButton]);
      }

      const readonlyButton = this.shadowRoot.querySelector('[part~="toolbar-button-readonly"]');
      if (readonlyButton) {
        toolbar.controls.push(['readonly', readonlyButton]);
      }

      toolbar.update = function(range) {
        update.call(toolbar, range);

        toolbar.controls.forEach(pair => {
          const input = pair[1];
          if (input.classList.contains('ql-active')) {
            input.setAttribute('on', '');
          } else {
            input.removeAttribute('on');
          }
        });
      };
    }

    _patchKeyboard() {
      const focusToolbar = () => {
        this._markToolbarFocused();
        const standardButton = this._toolbar.querySelector('button:not([tabindex])');
        if (standardButton != null) {
          standardButton.focus();
        } else {
          const button = Array.from(this.shadowRoot.querySelectorAll('[part="toolbar"] slot[name="toolbar"]'))[0]
              .assignedElements()
              .filter(e => e.getAttribute('tabindex') == 0 || e.getAttribute('tabindex') == undefined)[0];
          if (button != null) {
            button.focus();
          }
        }
      };

      const keyboard = this._editor.getModule('keyboard');
      const bindings = keyboard.bindings[TAB_KEY];

      // exclude Quill shift-tab bindings, except for code block,
      // as some of those are breaking when on a newline in the list
      // https://github.com/vaadin/vcf-enhanced-rich-text-editor/issues/67
      const originalBindings = bindings.filter(b => !b.shiftKey || (b.format && b.format['code-block']));
      const moveFocusBinding = { key: TAB_KEY, shiftKey: true, handler: focusToolbar };
      const self = this;
      // Binding for tabstop functionality (new embed-based tabs).
      const tabStopBinding = {
        key: TAB_KEY,
        handler: function(range) {
          if (range) {
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

      // Soft-break binding (Shift+Enter): insert visual line break with tab copying
      const softBreakBinding = {
        key: 13,
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

      // Ensure normal Enter still works (hard break)
      const hardBreakBinding = {
        key: 13,
        shiftKey: false,
        handler: function() { return true; }
      };

      keyboard.bindings[TAB_KEY] = [tabStopBinding, ...originalBindings, moveFocusBinding];

      // Add soft-break and hard-break bindings for Enter key
      const enterBindings = keyboard.bindings[13] || [];
      keyboard.bindings[13] = [softBreakBinding, hardBreakBinding, ...enterBindings];

      // Backspace key bindings
      const backspaceKeyBindings = keyboard.bindings[BACKSPACE_KEY];
      keyboard.bindings[BACKSPACE_KEY] = [
        {
          key: BACKSPACE_KEY,
          handler: () => {
            if (this.selectedPlaceholders.length) this._removePlaceholders();
            else return true;
          }
        },
        ...backspaceKeyBindings
      ];

      // Delete key bindings
      const deleteKeyBindings = keyboard.bindings[DELETE_KEY];
      keyboard.bindings[DELETE_KEY] = [
        {
          key: DELETE_KEY,
          handler: () => {
            const sel = this._editor.getSelection();
            let nextPlaceholder = false;
            if (sel && sel.length === 0) {
              const index = sel.index;
              const ops = this._editor.getContents(index).ops || [];
              nextPlaceholder = ops[0].insert && ops[0].insert.placeholder;
              this._editor.setSelection(index, 1);
            }
            if (this.selectedPlaceholders.length || nextPlaceholder) this._removePlaceholders();
            else return true;
          }
        },
        ...deleteKeyBindings
      ];

      // Z key bindings
      const Z_KEY = 90;
      const zKeyBindings = keyboard.bindings[Z_KEY];
      keyboard.bindings[Z_KEY] = [
        {
          key: Z_KEY,
          shortKey: true,
          handler: () => {
            this._undoPlaceholderEvents();
            return true;
          }
        },
        {
          key: Z_KEY,
          shiftKey: true,
          shortKey: true,
          handler: () => {
            this._undoPlaceholderEvents();
            return true;
          }
        },
        ...zKeyBindings
      ];

      // V key bindings
      const V_KEY = 86;
      keyboard.bindings[V_KEY] = [
        {
          key: V_KEY,
          shortKey: true,
          handler: () => {
            const placeholders = this.selectedPlaceholders;
            if (placeholders.length) this._confirmRemovePlaceholders(placeholders, false, true);
            return true;
          }
        }
      ];

      // ArrowUp/ArrowDown correction for tab-filled lines.
      // Tab blots confuse browser's vertical cursor navigation, causing line skipping.
      // We intercept and handle manually, returning false to prevent browser default.
      const ARROW_UP = 38;
      const ARROW_DOWN = 40;
      const arrowUpHandler = function(range) {
        return self._handleArrowNavigation(range, -1);
      };
      const arrowDownHandler = function(range) {
        return self._handleArrowNavigation(range, +1);
      };
      keyboard.bindings[ARROW_UP] = [{ key: ARROW_UP, shiftKey: false, handler: arrowUpHandler }, ...(keyboard.bindings[ARROW_UP] || [])];
      keyboard.bindings[ARROW_DOWN] = [{ key: ARROW_DOWN, shiftKey: false, handler: arrowDownHandler }, ...(keyboard.bindings[ARROW_DOWN] || [])];

      // alt-f10 focuses a toolbar button
      keyboard.addBinding({ key: 121, altKey: true, handler: focusToolbar });

      // Shift+Space inserts a non-breaking space.
      // Prepend before Quill's default space handler so ERTE handler fires first.
      const SPACE_KEY = 32;
      keyboard.bindings[SPACE_KEY] = [
        {
          key: SPACE_KEY,
          shiftKey: true,
          handler: function() {
            const index = this.quill.getSelection().index;
            this.quill.insertEmbed(index, 'nbsp', true);
            return false;
          }
        },
        ...(keyboard.bindings[SPACE_KEY] || [])
      ];

      // Ctrl + P inserts placeholder.
      keyboard.addBinding({ key: 80, shortKey: true }, () => this._onPlaceholderClick());
    }

    _emitPlaceholderHistoryEvents(ops) {
      const placeholders = [];
      let insert = true;
      for (const op of ops) {
        if (op.delete) {
          insert = false;
          break;
        }
      }
      if (insert) {
        // Get placeholders from insert ops
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
        // Get placeholders from delete ops
        let deleteIndex = -1;
        let deleteLength = 0;
        for (const op of ops) {
          if (op.retain) deleteIndex = op.retain;
          if (op.delete) {
            deleteIndex = deleteIndex > 0 ? deleteIndex : 0;
            deleteLength = op.delete;
            const selected = this._getPlaceholdersInSelection(deleteIndex, deleteLength);
            selected.forEach(placeholder => placeholders.push(placeholder));
            deleteIndex = -1;
            deleteLength = 0;
          }
        }
      }
      if (placeholders.length) {
        const placeholderHistoryEventMethod = `_confirm${insert ? 'Insert' : 'Remove'}Placeholders`;

        // Event only confirm insert/delete
        this[placeholderHistoryEventMethod](placeholders, false, true);
      }
      return true;
    }

    _isCharacterKey(e) {
      let result = false;
      // This is IE, which only fires keypress events for printable keys
      if (typeof e.keyCode === 'undefined') result = true;
      else if (typeof e.which == 'number' && e.which > 0) {
        // In other browsers except old versions of WebKit, evt.which is
        // only greater than zero if the keypress is a printable key.
        // We need to filter out backspace and ctrl/alt/meta key combinations
        result = !e.ctrlKey && !e.metaKey && !e.altKey && e.keyCode !== 8;
      }
      return result;
    }

    _undoPlaceholderEvents() {
      const historyStack = this._editor.history.stack;
      const undo = historyStack.undo[historyStack.undo.length - 1] || [];
      if (undo && undo.undo) this._emitPlaceholderHistoryEvents(undo.undo.ops);
      return true;
    }

    _redoPlaceholderEvents() {
      const historyStack = this._editor.history.stack;
      const redo = historyStack.redo[historyStack.redo.length - 1] || [];
      if (redo && redo.redo) this._emitPlaceholderHistoryEvents(redo.redo.ops);
      return true;
    }

    _emitChangeEvent() {
      this._debounceSetValue && this._debounceSetValue.flush();

      if (this._lastCommittedChange !== this.value) {
        this.dispatchEvent(new CustomEvent('change', { bubbles: true, cancelable: false }));
        this._lastCommittedChange = this.value;
      }
    }

    _onWhitespaceClick() {
      this._markToolbarClicked();
      this.showWhitespace = !this.showWhitespace;
      const btn = this.shadowRoot.querySelector('[part~="toolbar-button-whitespace"]');
      if (btn) {
        btn.classList.toggle('ql-active', this.showWhitespace);
        if (this.showWhitespace) {
          btn.setAttribute('on', '');
        } else {
          btn.removeAttribute('on');
        }
      }
    }

    _onReadonlyClick() {
      const range = this._getSelection();
      if (range) {
        const [readOnlySection] = this._editor.scroll.descendant(ReadOnlyBlot, range.index);
        this._editor.formatText(range.index, range.length, 'readonly', readOnlySection == null, 'user');
      }
    }

    _onLinkClick() {
      const range = this._getSelection();
      if (range) {
        const LinkBlot = Quill.imports['formats/link'];
        const [link, offset] = this._editor.scroll.descendant(LinkBlot, range.index);
        if (link != null) {
          // existing link
          this._linkRange = { index: range.index - offset, length: link.length() };
          this._linkUrl = LinkBlot.formats(link.domNode);
        } else if (range.length === 0) {
          this._linkIndex = range.index;
        }
        this._linkEditing = true;
      }
    }

    _applyLink(link) {
      if (link) {
        this._markToolbarClicked();
        this._editor.format('link', link, SOURCE.USER);
        this._editor.getModule('toolbar').update(this._editor.selection.savedRange);
      }
      this._closeLinkDialog();
    }

    _insertLink(link, position) {
      if (link) {
        this._markToolbarClicked();
        this._editor.insertText(position, link, { link });
        this._editor.setSelection(position, link.length);
      }
      this._closeLinkDialog();
    }

    _updateLink(link, range) {
      this._markToolbarClicked();
      this._editor.formatText(range, 'link', link, SOURCE.USER);
      this._closeLinkDialog();
    }

    _removeLink() {
      this._markToolbarClicked();
      if (this._linkRange != null) {
        this._editor.formatText(this._linkRange, { link: false, color: false }, SOURCE.USER);
      }
      this._closeLinkDialog();
    }

    _closeLinkDialog() {
      this._linkEditing = false;
      this._linkUrl = '';
      this._linkIndex = null;
      this._linkRange = null;
    }

    _onLinkEditConfirm() {
      if (this._linkIndex != null) {
        this._insertLink(this._linkUrl, this._linkIndex);
      } else if (this._linkRange) {
        this._updateLink(this._linkUrl, this._linkRange);
      } else {
        this._applyLink(this._linkUrl);
      }
    }

    _onLinkEditCancel() {
      this._closeLinkDialog();
      this._editor.focus();
    }

    _onLinkEditRemove() {
      this._removeLink();
      this._closeLinkDialog();
    }

    _onLinkKeydown(e) {
      if (e.keyCode === 13) {
        e.preventDefault();
        e.stopPropagation();
        this.$.confirmLink.click();
      }
    }

    _updateHtmlValue() {
      const className = 'ql-editor';
      const editor = this.shadowRoot.querySelector(`.${className}`);
      let content = editor.innerHTML;

      // Remove style-scoped classes that are appended when ShadyDOM is enabled
      Array.from(editor.classList).forEach(c => (content = content.replace(new RegExp('\\s*' + c, 'g'), '')));

      // Remove Quill classes, e.g. ql-syntax, except for align, indent, tab, soft-break
      content = content.replace(/\s*ql-(?!align|indent|tab|soft-break)[\w\-]*\s*/g, '');

      // Replace Quill align classes with inline styles
      ['right', 'center', 'justify'].forEach(align => {
        content = content.replace(new RegExp(` class=[\\\\]?"\\s?ql-align-${align}[\\\\]?"`, 'g'), ` style="text-align: ${align}"`);
      });

      content = content.replace(/ class=""/g, '');

      this._setHtmlValue(content);
    }

    _announceFormatting() {
      const timeout = 200;

      const announcer = this.shadowRoot.querySelector('.announcer');
      announcer.textContent = '';

      this._debounceAnnounceFormatting = Debouncer.debounce(this._debounceAnnounceFormatting, timeOut.after(timeout), () => {
        const formatting = Array.from(this.shadowRoot.querySelectorAll('[part="toolbar"] .ql-active'))
            .map(button => button.getAttribute('title'))
            .join(', ');
        announcer.textContent = formatting;
      });
    }

    get _customButtons() {
      return Array.from(this.querySelectorAll('button, vaadin-button, [part="custom-button"]')).filter(el => el.getAttribute('slot') === 'toolbar');
    }

    get _toolbarButtons() {
      return Array.from(this.shadowRoot.querySelectorAll('[part="toolbar"] button')).concat(this._customButtons);
    }

    _clear() {
      this._editor.deleteText(0, this._editor.getLength(), SOURCE.SILENT);
      this._updateHtmlValue();
    }

    _undo(e) {
      e.preventDefault();
      this._undoPlaceholderEvents();
      this._editor.history.undo();
      this._editor.focus();
    }

    _redo(e) {
      e.preventDefault();
      this._redoPlaceholderEvents();
      this._editor.history.redo();
      this._editor.focus();
    }

    _toggleToolbarDisabled(disable) {
      const buttons = this._toolbarButtons;
      if (disable) {
        buttons.forEach(btn => btn.setAttribute('disabled', 'true'));
      } else {
        buttons.forEach(btn => btn.removeAttribute('disabled'));
      }
    }

    _onImageTouchEnd(e) {
      // Cancel the event to avoid the following click event
      e.preventDefault();
      // FIXME(platosha): workaround for Polymer Gestures mouseCanceller
      // cancelling the following synthetic click. See also:
      // https://github.com/Polymer/polymer/issues/5289
      this._resetMouseCanceller();
      this._onImageClick();
    }

    _resetMouseCanceller() {
      resetMouseCanceller();
    }

    _onImageClick() {
      this.$.fileInput.value = '';
      this.$.fileInput.click();
    }

    _uploadImage(e) {
      const fileInput = e.target;
      // NOTE: copied from https://github.com/quilljs/quill/blob/1.3.6/themes/base.js#L128
      // needs to be updated in case of switching to Quill 2.0.0
      if (fileInput.files != null && fileInput.files[0] != null) {
        const reader = new FileReader();
        reader.onload = e => {
          const image = e.target.result;
          const range = this._getSelection(true);
          this._editor.updateContents(
              new Quill.imports.delta()
                  .retain(range.index)
                  .delete(range.length)
                  .insert({ image }),
              SOURCE.USER
          );
          this._markToolbarClicked();
          this._editor.setSelection(range.index + 1, SOURCE.SILENT);
          fileInput.value = '';
        };
        reader.readAsDataURL(fileInput.files[0]);
      }
    }

    _disabledChanged(disabled, readonly, editor) {
      if (disabled === undefined || readonly === undefined || editor === undefined) {
        return;
      }

      if (disabled || readonly) {
        editor.enable(false);

        if (disabled) {
          this._toggleToolbarDisabled(true);
        }
      } else {
        editor.enable();

        if (this._oldDisabled) {
          this._toggleToolbarDisabled(false);
        }
      }

      this._oldDisabled = disabled;
    }

    _tabStopsChanged(tabStops, _editor) {
      const horizontalRuler = this.shadowRoot.querySelector('[part="horizontalRuler"]');
      if (horizontalRuler) {
        horizontalRuler.innerHTML = '';
      }

      tabStops.forEach(stop => {
        this._addTabStopIcon(stop);
      });

      // Convert external {direction, position} format to internal {pos, align} format
      this._tabStopsArray = (tabStops || []).map(stop => ({
        pos: stop.position,
        align: stop.direction === 'middle' ? 'center' : (stop.direction || 'left')
      }));

      if (_editor) {
        this._requestTabUpdate();
      }
    }

    _valueChanged(value, editor) {
      if (editor === undefined) {
        return;
      }

      if (value == null || value == '[{"insert":"\\n"}]') {
        this.value = '';
        return;
      }

      if (value === '') {
        this._clear();
        return;
      }

      let parsedValue;
      try {
        parsedValue = JSON.parse(value);
        if (Array.isArray(parsedValue)) {
          this._oldValue = value;
          // Set altAppearance
          let AltAppearance = false;
          for (const op of parsedValue) {
            if (op.insert.placeholder) {
              AltAppearance = op.insert.placeholder.altAppearance || false;
              break;
            }
          }
          this.placeholderAltAppearance = AltAppearance;
        } else {
          throw new Error('expected JSON string with array of objects, got: ' + value);
        }
      } catch (err) {
        // Use old value in case new one is not suitable
        this.value = this._oldValue;
        console.error('Invalid value set to rich-text-editor:', err);
        return;
      }
      const delta = new Quill.imports.delta(parsedValue);
      // suppress text-change event to prevent infinite loop
      if (JSON.stringify(editor.getContents()) !== JSON.stringify(delta)) {
        editor.setContents(delta, SOURCE.SILENT);
        // Recalculate tab widths after content change
        this._requestTabUpdate();
      }
      this._updateHtmlValue();

      if (this._toolbarState === STATE.CLICKED) {
        this._cleanToolbarState();
        this._emitChangeEvent();
      } else if (!this._editor.hasFocus()) {
        // value changed from outside
        this._lastCommittedChange = this.value;
      }
    }

    _addTabStopIcon(tabStop) {
      var icon = document.createElement('vaadin-icon');
      let iconIcon;
      if (tabStop.direction == 'left') {
        iconIcon = 'vaadin:caret-right';
      } else if (tabStop.direction == 'right') {
        iconIcon = 'vaadin:caret-left';
      } else {
        iconIcon = 'vaadin:dot-circle';
      }

      icon.setAttribute('icon', iconIcon);
      icon.style.width = '15px';
      icon.style.height = '15px';
      icon.style.position = 'absolute';
      icon.style.top = '0px';
      icon.style.left = tabStop.position - 7 + 'px';
      const horizontalRuler = this.shadowRoot.querySelector('[part="horizontalRuler"]');
      horizontalRuler.appendChild(icon);
      icon.tabStop = tabStop;

      var self = this;
      icon.onclick = function(iconEvent) {
        var icon = iconEvent.target;
        var index = self.tabStops.indexOf(icon.tabStop);

        if (icon.getAttribute('icon') == 'vaadin:caret-right') {
          icon.setAttribute('icon', 'vaadin:caret-left');
          icon.tabStop.direction = 'right';
          self.tabStops[index] = icon.tabStop;
        } else if (icon.getAttribute('icon') == 'vaadin:caret-left') {
          icon.setAttribute('icon', 'vaadin:dot-circle');
          icon.tabStop.direction = 'middle';
          self.tabStops[index] = icon.tabStop;
        } else {
          self.tabStops.splice(index, 1);
          icon.parentElement.removeChild(icon);
          icon.remove();
        }

        self.tabStops = Object.assign([], self.tabStops);

        iconEvent.stopPropagation();
      };
    }

    _addTabStop(event) {
      const tabStop = { direction: 'left', position: event.offsetX };
      this.tabStops.push(tabStop);
      this.tabStops.sort((a, b) => a['position'] - b['position']);
      this.tabStops = Object.assign([], this.tabStops);
    }

    _placeholderEditingChanged(value) {
      this.$.placeholderDialog.opened = value;
    }

    _onPlaceholderChanged(e) {
      this._placeholder = e.detail.value;
    }

    _onPlaceholderClick() {
      const range = this._getSelection();

      if (range) {
        const placeholder = this.selectedPlaceholder;
        if (placeholder && placeholder.text) {
          const value = placeholder.text.replace(this.placeholderTags.start, '').replace(this.placeholderTags.end, '');
          this.$.placeholderRemoveButton.style.display = 'block';
          this._placeholderRange = { index: range.index - 1, length: 1 };
          this._placeholder = value;
        } else if (range.length === 0) {
          this.$.placeholderRemoveButton.style.display = 'none';
          this._insertPlaceholderIndex = range.index;
        }

        const detail = { position: range.index };
        const event = new CustomEvent('placeholder-button-click', { bubbles: true, cancelable: true, detail });
        const cancelled = !this.dispatchEvent(event);
        if (!cancelled) this._placeholderEditing = true;
      }
    }

    get selectedPlaceholder() {
      const range = this._getSelection();
      let placeholder = null;
      if (range) {
        const op = this._editor.getContents(this._getSelection().index - 1, 1).ops[0];
        placeholder = (op && op.insert.placeholder) || null;
      }
      return placeholder;
    }

    get selectedPlaceholders() {
      const range = this._getSelection();
      const placeholders = [];
      if (range) {
        for (let i = range.index - 1; i < range.index + range.length; i++) {
          const op = this._editor.getContents(i, 1).ops[0];
          const placeholder = (op && op.insert.placeholder) || null;
          if (placeholder) placeholders.push(placeholder);
        }
      }
      return placeholders;
    }

    _getPlaceholdersInSelection(index, length) {
      const sel = this._editor.getSelection();
      index = index || (sel && sel.index);
      length = length || (sel && sel.length);
      this._editor.setSelection(index, length, SOURCE.SILENT);
      const placeholders = this.selectedPlaceholders;
      if (index !== sel.index && length !== sel.length) {
        this._editor.setSelection(sel.index, sel.length, SOURCE.SILENT);
      }
      return placeholders;
    }

    _insertPlaceholders(placeholders, index = 0, remove = false) {
      if (!Array.isArray(placeholders)) placeholders = [{ placeholder: placeholders, index }];
      this._markToolbarClicked();
      const detail = { placeholders };
      const event = new CustomEvent(`placeholder-before-insert`, { bubbles: true, cancelable: true, detail });
      const cancelled = !this.dispatchEvent(event);
      this._insertPlaceholdersList = placeholders;
      if (!cancelled && placeholders) this._confirmInsertPlaceholders(placeholders);
      else if (remove) this._confirmRemovePlaceholders(placeholders, true);
      this._closePlaceholderDialog();
    }

    _updatePlaceholder(placeholder) {
      this._markToolbarClicked();
      const detail = { placeholder };
      const event = new CustomEvent(`placeholder-before-update`, { bubbles: true, cancelable: true, detail });
      const cancelled = !this.dispatchEvent(event);
      if (!cancelled && placeholder && this._placeholderRange) {
        this._confirmRemovePlaceholders(placeholder);
        this._confirmInsertPlaceholders([{ placeholder, index: this._placeholderRange.index }]);
      }
      this._closePlaceholderDialog();
    }

    _setInsertPlaceholder(placeholder, index) {
      if (placeholder) this._placeholder = placeholder;
      if (index) this._insertPlaceholderIndex = index;
    }

    _confirmInsertPlaceholders(placeholders = this._insertPlaceholdersList, silent = false, eventsOnly = false) {
      const detail = { placeholders: placeholders.map(i => i.placeholder) };
      let selectIndex = 0;
      if (!eventsOnly) {
        placeholders.forEach(({ placeholder, index: i }) => {
          if (this.placeholderAltAppearance) placeholder.altAppearance = true;
          this._editor.insertEmbed(i, 'placeholder', placeholder);
          selectIndex = i;
        });
        this._editor.setSelection(selectIndex + 1, 0);
      }
      if (!silent) this.dispatchEvent(new CustomEvent('placeholder-insert', { bubbles: true, cancelable: false, detail }));
    }

    _getSelection(focus = false) {
      return this._editor.getSelection(focus);
    }

    _getPlaceholderOptions(placeholder) {
      let placeholderOptions = this.placeholders.filter(i => i.text === placeholder)[0] || placeholder;
      if (typeof placeholderOptions === 'string') placeholderOptions = { text: placeholder };
      else if (placeholderOptions.text) placeholderOptions = { ...placeholderOptions };
      else console.error('Invalid placeholder format');
      return placeholderOptions;
    }

    _removePlaceholders(placeholders = this.selectedPlaceholders, restore = false, replace = '') {
      this._markToolbarClicked();
      if (placeholders.length) {
        const detail = { placeholders };
        const event = new CustomEvent(`placeholder-before-delete`, { bubbles: true, cancelable: true, detail });
        const cancelled = !this.dispatchEvent(event);
        if (!cancelled) this._confirmRemovePlaceholders(placeholders, false, false, replace);
        else if (restore) this._confirmInsertPlaceholders(placeholders, true);
      }
      this._closePlaceholderDialog();
    }

    _confirmRemovePlaceholders(placeholders = this.selectedPlaceholders, silent = false, eventsOnly = false, replace = '') {
      if (placeholders.length) {
        if (!eventsOnly) {
          const range = this._getSelection();
          if (range) {
            let deleteRange = range;
            if (!this._placeholderRange) this._placeholderRange = { index: range.index - 1, length: 1 };
            if (range.length < 1) deleteRange = this._placeholderRange;
            if (deleteRange) {
              this._editor.deleteText(deleteRange.index, deleteRange.length);
              if (replace) {
                this._editor.insertText(deleteRange.index, replace);
                this._editor.setSelection(deleteRange.index + replace.length, 0);
              } else {
                this._editor.setSelection(deleteRange.index, 0);
              }
            }
          }
        }
        if (!silent) {
          const detail = { placeholders };
          this.dispatchEvent(new CustomEvent('placeholder-delete', { bubbles: true, cancelable: false, detail }));
        }
      }
    }

    _closePlaceholderDialog() {
      this._placeholderEditing = false;
      this._placeholder = '';
      this._insertPlaceholderIndex = null;
      this._placeholderRange = null;
    }

    _onPlaceholderEditConfirm() {
      const placeholder = this._getPlaceholderOptions(this._placeholder);
      if (this._insertPlaceholderIndex !== null) this._insertPlaceholders(placeholder, this._insertPlaceholderIndex);
      else if (this._placeholderRange) this._updatePlaceholder(placeholder, this._placeholderRange);
    }

    _onPlaceholderEditCancel() {
      this._closePlaceholderDialog();
      this._editor.focus();
    }

    _onPlaceholderEditRemove() {
      this._removePlaceholders();
      this._closePlaceholderDialog();
    }

    _placeholderAltAppearanceChanged(altAppearance) {
      if (altAppearance) this.set('placeholderAppearance', this.i18n.placeholderAppearanceLabel2);
      else this.set('placeholderAppearance', this.i18n.placeholderAppearanceLabel1);
      if (this.value) {
        this.value = JSON.stringify(
            JSON.parse(this.value).map(op => {
              if (typeof op.insert === 'object' && op.insert.placeholder) {
                op.insert.placeholder.altAppearance = altAppearance;
              }
              return op;
            })
        );
        this._silentTextChange = true;
        if (this._ready) {
          const detail = { altAppearance: this.placeholderAltAppearance, appearanceLabel: this.placeholderAppearance };
          this.dispatchEvent(new CustomEvent('placeholder-appearance-change', { bubbles: true, cancelable: false, detail }));
        }
      }
    }

    _placeholderTagsChanged(tags) {
      PlaceholderBlot.tags = tags;
      this._resetPlaceholderAppearance();
    }

    _resetPlaceholderAppearance() {
      [1, 2].forEach(() => (this.placeholderAltAppearance = !this.placeholderAltAppearance));
    }

    _placeholderAltAppearancePatternChanged(altAppearanceRegex) {
      PlaceholderBlot.altAppearanceRegex = altAppearanceRegex;
      this.$.placeholderAppearanceBtn.hidden = !(this.placeholders.length && altAppearanceRegex);
    }

    _placeholdersChanged(placeholders) {
      this.$.placeholderBtn.hidden = !placeholders.length;
      this.$.placeholderAppearanceBtn.hidden = !(placeholders.length && this.placeholderAltAppearancePattern);
      if (placeholders.length) this.$.placeholderComboBox.items = placeholders.map(placeholder => this._getPlaceholderOptions(placeholder));
    }

    /**
     * Adds shortcut binding to a specific standard toolbar button.
     *
     * @param {string} button
     * @param {number} key
     * @param {boolean} shortKey
     * @param {boolean} shiftKey
     * @param {boolean} altKey
     */
    addStandardButtonBinding(button, key, shortKey, shiftKey, altKey) {
      const btnTitle = this.i18n[button];
      if (btnTitle) {
        var toolbarBtn = Array.from(this.shadowRoot.querySelectorAll('[part="toolbar"] button')).filter(btn => btn.title == btnTitle)[0];

        if (toolbarBtn) {
          const keyboard = this._editor.getModule('keyboard');

          let handler = btnTitle;

          if (button.includes('align')) {
            handler = 'align';
          }
          if (button.includes('script')) {
            handler = 'script';
          }
          if (button == 'h1' || button == 'h2' || button == 'h3') {
            handler = 'header';
          }
          if (button.includes('list')) {
            handler = 'list';
          }
          if (button.includes('indent')) {
            handler = 'indent';
          }
          if (button.includes('placeholder')) {
            handler = 'placeholder';
          }
          if (button == 'codeBlock') {
            handler = 'code-block';
          }

          let buttonHandler = () => {
            const toolbar = this._editor.getModule('toolbar');
            const value = !toolbarBtn.classList.contains('ql-active') && (toolbarBtn.value || !toolbarBtn.hasAttribute('value'));
            toolbar.handlers[handler].call(toolbar, value);
          };

          if (button == 'link') {
            buttonHandler = () => this._onLinkClick();
          }
          if (button == 'image') {
            buttonHandler = () => this._onImageClick();
          }
          if (button == 'redo') {
            buttonHandler = () => this._redo();
          }
          if (button == 'undo') {
            buttonHandler = () => this._undo();
          }
          if (button == 'readonly') {
            buttonHandler = () => this._onReadonlyClick();
          }
          if (button == 'clean') {
            buttonHandler = this._editor.getModule('toolbar').handlers[button];
          }

          const bindings = keyboard.bindings[key] || [];
          keyboard.bindings[key] = [
            {
              key: key,
              shiftKey: shiftKey,
              shortKey: shortKey,
              altKey: altKey,
              handler: buttonHandler
            },
            ...bindings
          ];
        }
      }
    }

    /**
     * Adds custom shortcut to focus toolbar.
     *
     * @param {number} key
     * @param {boolean} shortKey
     * @param {boolean} shiftKey
     * @param {boolean} altKey
     */
    addToolbarFocusBinding(key, shortKey, shiftKey, altKey) {
      const keyboard = this._editor.getModule('keyboard');

      const focusToolbar = () => {
        this._markToolbarFocused();
        this._getFirstVisibleToolbarButton().focus();
      };

      const bindings = keyboard.bindings[key] || [];
      keyboard.bindings[key] = [
        {
          key: key,
          shiftKey: shiftKey,
          shortKey: shortKey,
          altKey: altKey,
          handler: focusToolbar
        },
        ...bindings
      ];
    }

    _getFirstVisibleToolbarButton() {
      return this._toolbar.querySelector('button:not([style*="display: none"]):not([style*="display:none"]):not([hidden])');
    }

    /**
     * Fired when the user commits a value change.
     *
     * @event change
     */

    /**
     * Fired when the user commits a value change.
     *
     * @event placeholder-appearance-change
     * ```
     * e.detail = { altAppearance: boolean, appearanceLabel: string }
     * ```
     */

    /**
     * Fired when the user selects a placeholder.
     *
     * @event placeholder-select
     * ```
     * e.detail = { placeholder: object }
     * ```
     */

    /**
     * Fired after clicking addPlaceholder button.
     *
     * @event placeholder-button-click
     * ```
     * e.detail = { position: number }
     * ```
     */

    /**
     * Fired before updating a placeholder.
     *
     * @event placeholder-before-update
     * ```
     * e.detail = { placeholder: object }
     * ```
     */

    /**
     * Fired before a placeholder is inserted.
     *
     * @event placeholder-before-insert
     */

    /**
     * Fired after a placeholder is inserted.
     *
     * @event placeholder-insert
     * ```
     * e.detail = { placeholder: object }
     * ```
     */

    /**
     * Fired before a placeholder is deleted.
     *
     * @event placeholder-before-delete
     * ```
     * e.detail = { placeholder: object }
     * ```
     */

    /**
     * Fired after a placeholder is deleted.
     *
     * @event placeholder-delete
     * ```
     * e.detail = { placeholder: object }
     * ```
     */

    /**
     * @protected
     */
    static _finalizeClass() {
      super._finalizeClass();

      const devModeCallback = window.Vaadin.developmentModeCallback;
      const licenseChecker = devModeCallback && devModeCallback['vaadin-license-checker'];
      if (typeof licenseChecker === 'function') {
        licenseChecker(VcfEnhancedRichTextEditor);
      }
    }
  }

  customElements.define(VcfEnhancedRichTextEditor.is, VcfEnhancedRichTextEditor);

  /**
   * @namespace Vaadin
   */
  window.Vaadin.VcfEnhancedRichTextEditor = VcfEnhancedRichTextEditor;
})();
