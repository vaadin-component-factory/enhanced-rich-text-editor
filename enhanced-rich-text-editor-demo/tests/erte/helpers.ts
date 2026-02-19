import { Page, Locator, expect } from '@playwright/test';

/** Base URL for ERTE test views */
export const ERTE_TEST_BASE = `${process.env.BASE_URL || 'http://127.0.0.1:8080'}/erte-test`;

/**
 * Wait for the ERTE editor to be fully ready.
 * Checks: element visible, Quill initialized (_editor.root exists),
 * contenteditable="true" on .ql-editor, and data-ready attribute set by Java view.
 */
export async function waitForEditor(page: Page, id = 'test-editor') {
  // Wait for the component to be visible
  await page.locator(`#${id}`).waitFor({ state: 'visible', timeout: 60000 });

  // Wait for Quill to be initialized (check _editor.root inside shadow DOM)
  await page.waitForFunction(
    (elId) => {
      const el = document.getElementById(elId) as any;
      return el?._editor?.root != null;
    },
    id,
    { timeout: 60000 }
  );

  // Wait for editor to be editable
  await page.locator(`#${id}`).locator('.ql-editor[contenteditable="true"]').waitFor({ timeout: 10000 });

  // Wait for the Java view's ready indicator (element is display:none, so use 'attached')
  await page.locator('#test-ready[data-ready="true"]').waitFor({ state: 'attached', timeout: 10000 });
}

/**
 * Get the ERTE web component locator.
 */
export function getErte(page: Page, id = 'test-editor'): Locator {
  return page.locator(`#${id}`);
}

/**
 * Get the .ql-editor locator scoped inside the ERTE component.
 */
export function getEditor(page: Page, id = 'test-editor'): Locator {
  return page.locator(`#${id}`).locator('.ql-editor');
}

/**
 * Get the toolbar locator scoped inside the ERTE component.
 */
export function getToolbar(page: Page, id = 'test-editor'): Locator {
  return page.locator(`#${id}`).locator('[part="toolbar"]');
}

/**
 * Get all .ql-tab locators scoped inside the ERTE component.
 */
export function getTabs(page: Page, id = 'test-editor'): Locator {
  return page.locator(`#${id}`).locator('.ql-tab');
}

/**
 * Get all .ql-soft-break locators scoped inside the ERTE component.
 */
export function getSoftBreaks(page: Page, id = 'test-editor'): Locator {
  return page.locator(`#${id}`).locator('.ql-soft-break');
}

/**
 * Get the Delta JSON from the delta output element.
 * The test view updates this element on every text-change (client-side).
 */
export async function getDelta(page: Page): Promise<any> {
  const output = page.locator('#delta-output');
  // Wait for non-empty content with assertion-based wait
  await expect(output).not.toHaveText('', { timeout: 5000 });
  const text = await output.textContent();
  if (!text || text.trim() === '') {
    throw new Error('Delta output is empty');
  }
  return JSON.parse(text);
}

/**
 * Get the HTML output from the test view.
 */
export async function getHtmlOutput(page: Page): Promise<string> {
  const output = page.locator('#html-output');
  return (await output.textContent()) || '';
}

/**
 * Get the event log entries from the test view.
 */
export async function getEventLog(page: Page): Promise<string[]> {
  const log = page.locator('#event-log');
  const text = await log.textContent();
  if (!text || text.trim() === '') return [];
  return text.split('\n').filter(line => line.trim() !== '');
}

/**
 * Clear the event log.
 */
export async function clearEventLog(page: Page): Promise<void> {
  await page.evaluate(() => {
    const el = document.getElementById('event-log');
    if (el) el.textContent = '';
  });
}

/**
 * Count occurrences of a specific embed type in the delta.
 */
export function countInDelta(delta: any, type: string): number {
  return delta.ops.filter((op: any) => op.insert && op.insert[type]).length;
}

/**
 * Count occurrences of a specific format attribute in the delta.
 */
export function countFormatInDelta(delta: any, format: string): number {
  return delta.ops.filter((op: any) => op.attributes && op.attributes[format]).length;
}

/**
 * Get the ruler locator (horizontal ruler) scoped inside the ERTE component.
 */
export function getRuler(page: Page, id = 'test-editor'): Locator {
  return page.locator(`#${id}`).locator('[part~="horizontalRuler"]');
}

/**
 * Get tabstop markers (vaadin-icon elements) on the ruler.
 */
export function getRulerMarkers(page: Page, id = 'test-editor'): Locator {
  return page.locator(`#${id}`).locator('[part~="horizontalRuler"] vaadin-icon');
}

/**
 * Get the alignment direction of a ruler marker icon.
 * Returns 'left', 'right', or 'middle' based on the icon attribute.
 */
export async function getRulerMarkerDirection(marker: Locator): Promise<string> {
  const icon = await marker.getAttribute('icon');
  if (icon === 'vaadin:caret-right') return 'left';
  if (icon === 'vaadin:caret-left') return 'right';
  if (icon === 'vaadin:dot-circle') return 'middle';
  return 'unknown';
}

/**
 * Get the Show Whitespace toolbar button.
 */
function getWhitespaceButton(page: Page, id = 'test-editor'): Locator {
  return page.locator(`#${id}`).locator('[part~="toolbar-button-whitespace"]');
}

/**
 * Check whether the Show Whitespace toolbar button is currently active.
 */
export async function isShowWhitespaceActive(page: Page, id = 'test-editor'): Promise<boolean> {
  const btn = getWhitespaceButton(page, id);
  const cls = await btn.getAttribute('class') ?? '';
  return cls.includes('ql-active');
}

/**
 * Enable Show Whitespace via the toolbar button (click it if not already active).
 */
export async function enableShowWhitespace(page: Page, id = 'test-editor'): Promise<void> {
  if (!(await isShowWhitespaceActive(page, id))) {
    await getWhitespaceButton(page, id).click();
  }
}

/**
 * Disable Show Whitespace via the toolbar button (click it if currently active).
 */
export async function disableShowWhitespace(page: Page, id = 'test-editor'): Promise<void> {
  if (await isShowWhitespaceActive(page, id)) {
    await getWhitespaceButton(page, id).click();
  }
}

/**
 * Wait for a specific event to appear in the event log.
 */
export async function waitForEvent(page: Page, eventName: string, timeout = 5000): Promise<void> {
  await expect(page.locator('#event-log')).toContainText(eventName, { timeout });
}

/**
 * Get the last event from the event log.
 */
export async function getLastEvent(page: Page): Promise<string> {
  const entries = await getEventLog(page);
  return entries.length > 0 ? entries[entries.length - 1] : '';
}

/**
 * Click on the ERTE editor to focus it.
 */
export async function focusEditor(page: Page, id = 'test-editor'): Promise<void> {
  await getEditor(page, id).click();
}

/**
 * Type text into the ERTE editor.
 */
export async function typeInEditor(page: Page, text: string): Promise<void> {
  await page.keyboard.type(text);
}

/**
 * Press a key combination (e.g., 'Shift+Enter', 'Tab', 'Control+p').
 */
export async function pressKey(page: Page, key: string): Promise<void> {
  await page.keyboard.press(key);
}

/**
 * Select all content in the editor (Ctrl+A).
 */
export async function selectAll(page: Page): Promise<void> {
  await page.keyboard.press('Control+a');
}

/**
 * Get the Delta JSON directly from the Quill editor instance.
 * Unlike getDelta() which reads from #delta-output (only updated on text-change),
 * this reads directly from the editor and works after server-side setValue calls
 * that use SOURCE.SILENT and don't fire text-change events.
 */
export async function getDeltaFromEditor(page: Page, id = 'test-editor'): Promise<any> {
  const result = await page.evaluate((elId) => {
    const el = document.getElementById(elId) as any;
    if (!el?._editor) throw new Error('Editor not initialized');
    return el._editor.getContents();
  }, id);
  return result;
}

/**
 * Wait for the Delta output to contain a specific number of a given embed type.
 */
export async function waitForEmbedCount(
  page: Page,
  type: string,
  expectedCount: number,
  timeout = 5000
): Promise<void> {
  await page.waitForFunction(
    ({ type, expectedCount }) => {
      const el = document.getElementById('delta-output');
      if (!el || !el.textContent) return false;
      try {
        const delta = JSON.parse(el.textContent);
        const count = delta.ops.filter((op: any) => op.insert && op.insert[type]).length;
        return count === expectedCount;
      } catch {
        return false;
      }
    },
    { type, expectedCount },
    { timeout }
  );
}
