import '@vaadin/vaadin-lumo-styles/font-icons.js';
import '@vaadin/vaadin-lumo-styles/sizing.js';
import '@vaadin/vaadin-lumo-styles/spacing.js';
import '@vaadin/vaadin-lumo-styles/style.js';
import { color } from '@vaadin/vaadin-lumo-styles/color.js';
import { typography } from '@vaadin/vaadin-lumo-styles/typography.js';
import { css, registerStyles } from '@vaadin/vaadin-themable-mixin/vaadin-themable-mixin.js';

const lightDomStyles = css`
  [slot="toolbar"] vaadin-button,
  vaadin-button[slot="toolbar"] {
      width: var(--lumo-size-m);
      height: var(--lumo-size-m);
      border-radius: var(--lumo-border-radius);
      color: var(--lumo-contrast-60pct);
      margin: 2px 1px;
      background: transparent;
  }
  
  [slot="toolbar"] vaadin-button[disabled],
  vaadin-button[slot="toolbar"][disabled] {
      color: var(--lumo-contrast-30pct);
  }

  [slot="toolbar"] vaadin-button:hover,
  vaadin-button[slot="toolbar"]:hover {
      background-color: var(--lumo-contrast-5pct);
      color: var(--lumo-contrast-80pct);
      box-shadow: none;
  }
  
  [slot="toolbar"] vaadin-button[on],
  vaadin-button[slot="toolbar"][on] {
      background-color: var(--lumo-primary-color-10pct);
      color: var(--lumo-primary-text-color);
  }
  
  .switchable-content[slot="toolbar"] {
    gap: var(--lumo-space-xs);
    padding: var(--lumo-space-xs);
    background: var(--lumo-contrast-5pct);
    height: var(--lumo-size-m);
    margin-top: 2px;
    border-radius: var(--lumo-border-radius-m);
  }
  
  .switchable-content[slot="toolbar"] vaadin-integer-field {
    --lumo-text-field-size: var(--lumo-size-s);
    --lumo-font-size-m: 0.8rem;
  }

  .switchable-content[slot="toolbar"] vaadin-button {
    width: var(--lumo-size-s);
    height: var(--lumo-size-s);
    --lumo-font-size-m: 0.8rem;
    background-color: var(--lumo-contrast-20pct);
  }
  
  .switchable-content[slot="toolbar"] vaadin-button:hover {
    background-color: var(--lumo-contrast-30pct);
  }
  
  vaadin-button.suffix-icon vaadin-icon[slot="suffix"] {
    vertical-align: super;
    font-size: var(--lumo-font-size-xs);
  }

  vaadin-button.suffix-icon[slot="toolbar"],
  [slot="toolbar"] vaadin-button.suffix-icon {
    width: var(--lumo-size-l);
  }
  
  vaadin-button.suffix-icon[slot="toolbar"] vaadin-icon[slot="suffix"],
  [slot="toolbar"] vaadin-button.suffix-icon vaadin-icon[slot="suffix"] {
    padding-left: 0;
  }
  
`;

const richTextEditor = css`
  :host {
          min-height: calc(var(--lumo-size-m) * 8);
        }

        [part='toolbar'] {
          background-color: var(--lumo-contrast-5pct);
          padding: calc(var(--lumo-space-s) - 1px) var(--lumo-space-xs);
        }

        [part~='toolbar-group'] {
          margin: 0 calc(var(--lumo-space-l) / 2 - 1px);
        }

        [part~='toolbar-button'] {
          width: var(--lumo-size-m);
          height: var(--lumo-size-m);
          border-radius: var(--lumo-border-radius);
          color: var(--lumo-contrast-60pct);
          margin: 2px 1px;
          cursor: default;
          transition: background-color 100ms, color 100ms;
        }

        [part~='toolbar-button']:focus,
        ::slotted([part~="toolbar-button"]:focus) {
          outline: none;
          box-shadow: 0 0 0 2px var(--lumo-primary-color-50pct);
        }

        [part~='toolbar-button']:hover {
          background-color: var(--lumo-contrast-5pct);
          color: var(--lumo-contrast-80pct);
          box-shadow: none;
        }

        @media (hover: none) {
          [part~='toolbar-button']:hover {
            background-color: transparent;
          }
        }

        [part~='toolbar-button'][on] {
          background-color: var(--lumo-primary-color-10pct);
          color: var(--lumo-primary-text-color);
        }

        [part~='toolbar-button']:active {
          background-color: var(--lumo-contrast-10pct);
          color: var(--lumo-contrast-90pct);
        }

        [part~='toolbar-button-icon']::before {
          font-family: 'lumo-icons', var(--lumo-font-family);
          font-size: var(--lumo-icon-size-m);
        }

        [part~='toolbar-button-undo-icon']::before {
          content: var(--lumo-icons-undo);
        }

        [part~='toolbar-button-redo-icon']::before {
          content: var(--lumo-icons-redo);
        }

        [part~='toolbar-button-bold-icon']::before,
        [part~='toolbar-button-italic-icon']::before,
        [part~='toolbar-button-underline-icon']::before,
        [part~='toolbar-button-strike-icon']::before {
          font-size: var(--lumo-font-size-m);
          font-weight: 600;
          padding-left: 0.45em;
        }

        [part~='toolbar-button-bold-icon']::before {
          font-weight: 700;          
        }

        [part~='toolbar-button-italic-icon']::before {
          padding-left: 0.60em;        
        }

        [part~='toolbar-button-h1-icon']::before,
        [part~='toolbar-button-h2-icon']::before,
        [part~='toolbar-button-h3-icon']::before {
          font-weight: 600;
          padding-left: 0.25em;
        }

        [part~='toolbar-button-h1-icon']::before {
          font-size: var(--lumo-font-size-m);
        }

        [part~='toolbar-button-h2-icon']::before {
          font-size: var(--lumo-font-size-s);
        }

        [part~='toolbar-button-h3-icon']::before {
          font-size: var(--lumo-font-size-xs);
        }

        [part~='toolbar-button-subscript-icon']::before,
        [part~='toolbar-button-superscript-icon']::before {
          font-weight: 600;
          font-size: var(--lumo-font-size-s);          
          padding-left: 0.35em;
        }

        [part~='toolbar-button-subscript-icon']::after,
        [part~='toolbar-button-superscript-icon']::after {
          font-size: 0.625em;
          font-weight: 700;
        }

        [part~='toolbar-button-list-ordered-icon']::before {
          content: var(--lumo-icons-ordered-list);
        }

        [part~='toolbar-button-list-bullet-icon']::before {
          content: var(--lumo-icons-unordered-list);
        }

        [part~="toolbar-button-align-justify-icon"], 
        [part~="toolbar-button-align-left-icon"],
        [part~="toolbar-button-align-center-icon"],
        [part~="toolbar-button-align-right-icon"],
        [part~="toolbar-button-deindent-icon"], 
        [part~="toolbar-button-indent-icon"],
        [part~="toolbar-button-readonly-icon"],
        [part~="toolbar-button-vaadin-icon"] {
          --rte-extra-icons-stroke-color: var(--lumo-contrast-60pct);
        }
        
        [part~='toolbar-button-blockquote-icon']::before {
          font-size: var(--lumo-font-size-xxl);
          padding-left: 0.25em;
        }

        [part~='toolbar-button-code-block-icon']::before {
          content: var(--lumo-icons-angle-left) var(--lumo-icons-angle-right);
          font-size: var(--lumo-font-size-l);
          letter-spacing: -0.5em;
          margin-left: -0.25em;
          font-weight: 600;
          padding-left: 0.20em;
        }

        [part~='toolbar-button-image-icon']::before {
          content: var(--lumo-icons-photo);
        }

        [part~='toolbar-button-link-icon']::before {
          font-family: 'vaadin-rte-icons';
          font-size: var(--lumo-icon-size-m);
        }

        [part~='toolbar-button-clean-icon']::before {
          font-family: 'vaadin-rte-icons';
          font-size: var(--lumo-font-size-l);
          padding-left: 0.20em;
        }

        [part='content'] {
          background-color: var(--lumo-base-color);
        }

        /* TODO unsupported selector */
        [part='content'] > .ql-editor {
          padding: 0 var(--lumo-space-m);
          line-height: inherit;
        }

        /* Theme variants */

        /* No border */

        :host(:not([theme~='no-border'])) {
          border: 1px solid var(--lumo-contrast-20pct);
        }

        :host(:not([theme~='no-border']):not([readonly])) [part='content'] {
          border-top: 1px solid var(--lumo-contrast-20pct);
        }

        :host([theme~='no-border']) [part='toolbar'] {
          padding-top: var(--lumo-space-s);
          padding-bottom: var(--lumo-space-s);
        }

        /* Compact */

        :host([theme~='compact']) {
          min-height: calc(var(--lumo-size-m) * 6);
        }

        :host([theme~='compact']) [part='toolbar'] {
          padding: var(--lumo-space-xs) 0;
        }

        :host([theme~='compact'][theme~='no-border']) [part='toolbar'] {
          padding: calc(var(--lumo-space-xs) + 1px) 0;
        }

        :host([theme~='compact']) [part~='toolbar-button'] {
          width: var(--lumo-size-s);
          height: var(--lumo-size-s);
        }

        :host([theme~='compact']) [part~='toolbar-group'] {
          margin: 0 calc(var(--lumo-space-m) / 2 - 1px);
        } 

`;

const styles = document.createElement("style");
styles.innerHTML = lightDomStyles.cssText;
document.head.append(styles);

registerStyles('vcf-enhanced-rich-text-editor', [color, typography, richTextEditor], {
  moduleId: 'lumo-rich-text-editor',
});
