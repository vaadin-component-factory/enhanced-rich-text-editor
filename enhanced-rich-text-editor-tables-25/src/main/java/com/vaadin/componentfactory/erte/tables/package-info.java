/**
 * Enhanced Rich Text Editor - Tables Extension (V25).
 * <p>
 * Table blots for Quill 2 / Parchment 3. Ported from the ERTE 1 table
 * extension with 5 critical Parchment 3 API changes applied:
 * <ol>
 *   <li>Parchment.create() -> scroll.create() (global factory removed)</li>
 *   <li>replace() -> replaceWith() (reversed semantics)</li>
 *   <li>defaultChild must be class reference, not string</li>
 *   <li>checkMerge() must be overridden on TD/TR/Table</li>
 *   <li>While loops needed in optimize() merge logic</li>
 * </ol>
 * <p>
 * See migration_v25/spike/SPIKE_RESULTS.md for detailed findings.
 */
package com.vaadin.componentfactory.erte.tables;
