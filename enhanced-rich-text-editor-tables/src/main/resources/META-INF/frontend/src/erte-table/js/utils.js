/**
 * Shared utilities for ERTE Tables.
 * Extracted from V24 TableTrick.js.
 */

export function randomId() {
  return Math.random().toString(36).slice(2);
}

export function getQuill(el) {
  const Quill = window.Quill;
  if (!el) return null;
  if (typeof el === 'object' && el.domNode) el = el.domNode;
  if (el instanceof Node && !(el instanceof Element)) el = el.parentElement;
  if (el instanceof Element) {
    const container = el.closest('.ql-container');
    if (container) return Quill.find(container);
  }
  return null;
}

export const HIDDEN_BORDER_CLASS = 'ql-editor__table--hideBorder';
