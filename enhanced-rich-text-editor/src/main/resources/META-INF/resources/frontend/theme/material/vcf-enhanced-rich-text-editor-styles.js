import '@vaadin/vaadin-material-styles/color.js';
import { typography } from '@vaadin/vaadin-material-styles/typography.js';
import { css, registerStyles } from '@vaadin/vaadin-themable-mixin/vaadin-themable-mixin.js';

const richTextEditor = css`

        :host {
          background-color: var(--material-background-color);
          min-height: 288px;
        }

        [part='toolbar'] {
          background-color: var(--material-secondary-background-color);
          padding: 0;
          border: 0;
          overflow: hidden;
        }

        [part~='toolbar-group'] {
          margin: 8px 0;
          padding: 0 8px;
        }

        [part~='toolbar-group'] + [part~='toolbar-group'] {
          box-shadow: -1px 0 0 0 rgba(0, 0, 0, 0.1);
        }

        [part~='toolbar-button'] {
          border-radius: 3px;
          color: var(--material-secondary-text-color);
          font-family: 'vaadin-rte-icons', var(--material-font-family);
          font-weight: 600;
          margin: -4px 2px;
        }

        [part~='toolbar-button']:hover {
          background-color: transparent;
          color: inherit;
        }

        [part~='toolbar-button'][on] {
          background-color: rgba(0, 0, 0, 0.1);
          color: inherit;
        }

        @media (hover: none) {
          [part~='toolbar-button']:hover {
            color: var(--material-secondary-text-color);
          }
        }

        /* SVG icons */
        [part~='toolbar-button-undo-icon']::before,
        [part~='toolbar-button-redo-icon']::before,
        [part~='toolbar-button-list-ordered-icon']::before,
        [part~='toolbar-button-list-bullet-icon']::before,
        [part~='toolbar-button-image-icon']::before,
        [part~='toolbar-button-link-icon']::before,
        [part~='toolbar-button-clean-icon']::before {
          font-size: 24px;
          font-weight: 400;
        }

        /* Text icons */
        [part~='toolbar-button-bold-icon']::before,
        [part~='toolbar-button-italic-icon']::before,
        [part~='toolbar-button-underline-icon']::before,
        [part~='toolbar-button-strike-icon']::before {
          font-size: 20px;
        }

        /* SVG extra icon set */
        [part~="toolbar-button-align-justify-icon"], 
        [part~="toolbar-button-align-left-icon"],
        [part~="toolbar-button-align-center-icon"],
        [part~="toolbar-button-align-right-icon"],
        [part~="toolbar-button-deindent-icon"], 
        [part~="toolbar-button-indent-icon"],
        [part~="toolbar-button-readonly-icon"] {
          --rte-extra-icons-stroke-color: var(--material-secondary-text-color);
        }

        /* TODO unsupported selector */
        [part='content'] > .ql-editor {
          padding: 0 16px;
          line-height: inherit;
        }

        /* Theme variants */

        /* No border */

        :host(:not([theme~='no-border'])) {
          border: 1px solid rgba(0, 0, 0, 0.12);
        }

        :host(:not([theme~='no-border']):not([readonly])) [part='content'] {
          border-top: 1px solid rgba(0, 0, 0, 0.12);
        }

        b,
        strong {
          font-weight: 600;
        }
`;

registerStyles('vcf-enhanced-rich-text-editor', [typography, richTextEditor], {
  moduleId: 'material-rich-text-editor',
});
