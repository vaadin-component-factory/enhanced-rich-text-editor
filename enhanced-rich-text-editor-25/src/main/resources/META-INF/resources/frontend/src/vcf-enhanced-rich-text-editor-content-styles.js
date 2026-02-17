/**
 * ERTE v25 - Content Styles (Lit CSS)
 *
 * Ported from ERTE 1's Polymer <dom-module> pattern to Lit css tagged template.
 *
 * These styles are ERTE-specific additions to the editor content area.
 * RTE 2's base styles (indent, alignment, lists, blockquote, code, etc.)
 * are already provided by the parent class via richTextEditorStyles.
 * We only include styles for ERTE-specific features here:
 * - Placeholder blot (.ql-placeholder)
 * - Tab blot (.ql-tab)
 * - Soft-break blot (.ql-soft-break)
 * - Whitespace indicators (show-whitespace mode)
 */
import { css } from 'lit';

export const erteContentStyles = css`
  /* ==========================================
     PLACEHOLDER BLOT
     ========================================== */
  .ql-placeholder {
    padding: 0 var(--lumo-space-xs, 0.25rem);
    display: inline-block;
    background-color: var(--lumo-primary-color-10pct, rgba(0, 120, 215, 0.1));
    color: var(--lumo-primary-color, #1676f3);
    border-radius: var(--lumo-border-radius-s, 0.25em);
  }

  /* ==========================================
     TAB BLOT (Embed) - inline-block spacer
     with iterative width calculation
     ========================================== */
  span.ql-tab {
    display: inline-block;
    min-width: 2px;
    height: 1rem;
    white-space: pre;
    vertical-align: bottom;
    position: relative;
    cursor: default;
    font-size: 0;
    line-height: 1rem;
    overflow: hidden;
    will-change: width;
    transform: translateZ(0);
  }

  /* ==========================================
     SOFT BREAK BLOT
     ========================================== */
  span.ql-soft-break {
    display: inline;
  }

  /* ==========================================
     WHITESPACE INDICATORS (Show Whitespace)
     Shows special characters for tabs,
     soft-breaks, and paragraph ends.
     Activated by adding 'show-whitespace' class
     on .ql-editor.
     CRITICAL: ::after on .ql-tab inherits
     font-size:0, overflow:hidden from parent.
     Must explicitly override to make indicators
     visible.
     ========================================== */

  /* Tab indicator: right arrow */
  .show-whitespace span.ql-tab::after {
    position: absolute;
    content: '\\2192'; /* right arrow */
    right: 2px;
    top: 0;
    line-height: 1rem;
    font-size: var(--lumo-font-size-m, 1rem);
    overflow: visible;
    color: var(--lumo-contrast-40pct, rgba(0, 0, 0, 0.38));
    pointer-events: none;
  }

  /* Auto-wrap indicator */
  .show-whitespace span.ql-tab.ql-auto-wrap-start::after {
    content: '\\2BB0\\2192'; /* return + right arrow */
  }

  /* Soft-break indicator: return symbol */
  .show-whitespace span.ql-soft-break::before {
    content: '\\21B5'; /* return symbol */
    font-size: var(--lumo-font-size-s, 0.875rem);
    color: var(--lumo-contrast-40pct, rgba(0, 0, 0, 0.38));
    vertical-align: baseline;
    pointer-events: none;
  }

  /* Paragraph / Hard-break indicator: pilcrow */
  .show-whitespace p:not(:last-child),
  .show-whitespace h1:not(:last-child),
  .show-whitespace h2:not(:last-child),
  .show-whitespace h3:not(:last-child),
  .show-whitespace li:not(:last-child),
  .show-whitespace blockquote:not(:last-child) {
    position: relative;
  }

  .show-whitespace p:not(:last-child)::after,
  .show-whitespace h1:not(:last-child)::after,
  .show-whitespace h2:not(:last-child)::after,
  .show-whitespace h3:not(:last-child)::after,
  .show-whitespace li:not(:last-child)::after,
  .show-whitespace blockquote:not(:last-child)::after {
    content: '\\00B6'; /* pilcrow */
    position: absolute;
    bottom: 0;
    font-size: var(--lumo-font-size-s, 0.875rem);
    color: var(--lumo-contrast-30pct, rgba(0, 0, 0, 0.26));
    pointer-events: none;
    margin-left: 2px;
  }
`;
