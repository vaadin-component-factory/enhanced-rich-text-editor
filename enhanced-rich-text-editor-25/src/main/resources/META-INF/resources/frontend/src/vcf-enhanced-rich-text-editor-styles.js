/**
 * ERTE v25 - Aggregated Styles (Lit CSS)
 *
 * Combines all ERTE-specific style modules into a single export.
 * The parent RTE 2 class already provides base styles (content, toolbar, icons)
 * via its static get styles(). This module adds ERTE-specific styles on top.
 *
 * In Lit, the subclass's static get styles() can return
 * [super.styles, ...erteStyles] to layer ERTE styles on top of RTE 2 base.
 */
import { css } from 'lit';
import { erteContentStyles } from './vcf-enhanced-rich-text-editor-content-styles.js';
import { erteToolbarStyles } from './vcf-enhanced-rich-text-editor-toolbar-styles.js';

/**
 * ERTE-specific state styles (readonly, disabled).
 * These supplement the parent RTE 2 state styles with ERTE-specific selectors
 * (e.g., slotted toolbar buttons).
 */
const erteStateStyles = css`
  :host([readonly]) ::slotted([part~='toolbar-button']) {
    display: none;
  }

  :host([disabled]) ::slotted([part~='toolbar-button']) {
    background-color: transparent;
  }
`;

export const erteStyles = [erteContentStyles, erteToolbarStyles, erteStateStyles];
