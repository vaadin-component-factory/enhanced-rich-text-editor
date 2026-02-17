/**
 * ERTE v25 - Icon Definitions (Lit CSS)
 *
 * RTE 2 already provides all standard toolbar button icons via mask-image CSS
 * custom properties (undo, redo, bold, italic, etc.). Those are inherited
 * from the parent class and do NOT need to be redefined here.
 *
 * This file provides ERTE-specific toolbar button icons as mask-image CSS
 * properties, following the same pattern as RTE 2's icon system.
 *
 * For the SVG iconset used in ERTE's extra icon components (align, indent,
 * whitespace, readonly, table), see vcf-enhanced-rich-text-editor-extra-icons.js.
 */
import { css } from 'lit';

/**
 * ERTE-specific toolbar button icons using the RTE 2 mask-image pattern.
 * These icons are for ERTE-only toolbar buttons that don't exist in RTE 2.
 */
export const erteIconStyles = css`
  :host {
    /* Whitespace toggle button (pilcrow symbol) */
    --_erte-icon-whitespace: url('data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24"><text x="12" y="18" text-anchor="middle" font-family="serif" font-size="20" font-weight="bold" fill="currentColor">%C2%B6</text></svg>');

    /* Read-only lock icon */
    --_erte-icon-readonly: url('data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><rect x="5" y="11" width="14" height="10" rx="2" /><circle cx="12" cy="16" r="1" /><path d="M8 11v-4a4 4 0 0 1 8 0v4" /></svg>');

    /* Table icon (grid) */
    --_erte-icon-table: url('data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" height="24" viewBox="0 -960 960 960" width="24" fill="currentColor"><path d="M200-120q-33 0-56.5-23.5T120-200v-560q0-33 23.5-56.5T200-840h560q33 0 56.5 23.5T840-760v560q0 33-23.5 56.5T760-120H200Zm0-80h133v-133H200v133Zm213 0h134v-133H413v133Zm214 0h133v-133H627v133ZM200-413h133v-134H200v134Zm213 0h134v-134H413v134Zm214 0h133v-134H627v134ZM200-627h133v-133H200v133Zm213 0h134v-133H413v133Zm214 0h133v-133H627v133Z"/></svg>');
  }

  /* Apply ERTE-specific icons to toolbar buttons */
  [part~='toolbar-button-whitespace']::before {
    mask-image: var(--_erte-icon-whitespace);
  }

  [part~='toolbar-button-readonly']::before {
    mask-image: var(--_erte-icon-readonly);
  }

  [part~='toolbar-button-table']::before {
    mask-image: var(--_erte-icon-table);
  }
`;
