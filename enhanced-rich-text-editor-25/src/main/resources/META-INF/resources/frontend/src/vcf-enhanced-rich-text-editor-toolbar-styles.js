/**
 * ERTE v25 - Toolbar Styles (Lit CSS)
 *
 * Ported from ERTE 1's Polymer <dom-module> pattern to Lit css tagged template.
 *
 * RTE 2's base toolbar styles are already provided by the parent class.
 * These are ERTE-specific toolbar additions:
 * - Slotted toolbar button styling (for custom buttons added via Java API)
 * - ERTE-specific button icons (whitespace, readonly, etc.)
 * - Placeholder display button styling
 */
import { css } from 'lit';

export const erteToolbarStyles = css`
  /* Slotted toolbar buttons (added via Java's addToolbarComponents API) */
  ::slotted([part~='toolbar-button']) {
    width: auto;
    height: var(--lumo-size-m, 2rem);
    border-radius: var(--lumo-border-radius-m, 0.25em);
    color: var(--lumo-contrast-80pct, rgba(0, 0, 0, 0.7));
    margin: 2px 1px;
    cursor: default;
    transition: background-color 100ms, color 100ms;
    padding: 0 var(--lumo-space-s, 0.5rem);
  }

  ::slotted([part~='toolbar-button']:hover) {
    background-color: var(--lumo-contrast-5pct, rgba(0, 0, 0, 0.05));
    color: var(--lumo-contrast, #1a1a1a);
    box-shadow: none;
  }

  /* ERTE icon stroke color for SVG-based icons */
  [part~='toolbar-button-whitespace-icon'],
  [part~='toolbar-button-readonly-icon'] {
    --rte-extra-icons-stroke-color: var(--lumo-contrast-60pct, rgba(0, 0, 0, 0.54));
  }

  /* Placeholder display button (wider than standard toolbar buttons) */
  [part~='toolbar-button'][part~='toolbar-button-placeholder-display'] {
    width: auto;
    min-width: var(--lumo-size-l, 2.5rem);
    padding: 0 var(--lumo-space-xs, 0.25rem);
    font-size: 0.875em;
  }
`;
