import { test, expect, Page, Locator } from '@playwright/test';
import {
  waitForEditor,
  getErte,
  getEditor,
  getDelta,
  getEventLog,
  clearEventLog,
  waitForEvent,
  countInDelta,
  ERTE_TEST_BASE,
  focusEditor,
} from './helpers';

const PLACEHOLDER_URL = `${ERTE_TEST_BASE}/placeholders`;

// ── Locator helpers ─────────────────────────────────────────────────────────

/** Get the placeholder toolbar button inside the ERTE shadow DOM. */
function getPlaceholderButton(page: Page, id = 'test-editor'): Locator {
  return page.locator(`#${id}`).locator('[part~="toolbar-button-placeholder"]');
}

/** Get the placeholder appearance toggle button inside the ERTE shadow DOM. */
function getAppearanceToggleButton(page: Page, id = 'test-editor'): Locator {
  return page.locator(`#${id}`).locator('[part~="toolbar-button-placeholder-display"]');
}

/** Get the confirm-dialog overlay for the Placeholders dialog (not the link dialog). */
function getDialog(page: Page): Locator {
  return page.locator('vaadin-confirm-dialog-overlay[aria-label="Placeholders"]');
}

/** Get the combo-box inside the placeholder dialog. */
function getComboBox(page: Page): Locator {
  return getDialog(page).locator('vaadin-combo-box');
}

/** Get all placeholder blot spans inside the editor content area. */
function getPlaceholderBlots(page: Page, id = 'test-editor'): Locator {
  return page.locator(`#${id}`).locator('.ql-editor .ql-placeholder');
}

/** Click the OK / confirm button inside the dialog. */
async function confirmDialog(page: Page): Promise<void> {
  await getDialog(page).locator('[slot="confirm-button"]').click();
}

/** Cancel the dialog by pressing Escape (cancel button is hidden by default in vaadin-confirm-dialog). */
async function cancelDialog(page: Page): Promise<void> {
  await page.keyboard.press('Escape');
}

/** Click the Remove / reject button inside the dialog. */
async function removeViaDialog(page: Page): Promise<void> {
  await getDialog(page).locator('[slot="reject-button"]').click();
}

/** Open the combo-box dropdown and select an item by its visible text. */
async function selectComboItem(page: Page, text: string): Promise<void> {
  const comboBox = getComboBox(page);
  await comboBox.click();
  // Wait for the overlay to appear
  const overlay = page.locator('vaadin-combo-box-overlay');
  await overlay.waitFor({ state: 'visible', timeout: 5000 });
  // Click the item matching the text
  await overlay.locator(`vaadin-combo-box-item`).filter({ hasText: text }).click();
}

/** Open the placeholder dialog by clicking the toolbar button. */
async function openPlaceholderDialog(page: Page): Promise<void> {
  await getPlaceholderButton(page).click();
  await getDialog(page).waitFor({ state: 'visible', timeout: 5000 });
}

/**
 * Full workflow: open dialog, select a placeholder by text, confirm.
 * Returns after the dialog closes and the embed is inserted.
 */
async function insertPlaceholder(page: Page, text: string): Promise<void> {
  await openPlaceholderDialog(page);
  await selectComboItem(page, text);
  await confirmDialog(page);
  // Wait for the dialog to close
  await expect(getDialog(page)).toBeHidden({ timeout: 5000 });
}

// ── Test suite ──────────────────────────────────────────────────────────────

test.describe('Placeholder functionality', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto(PLACEHOLDER_URL);
    await waitForEditor(page);
    await clearEventLog(page);
  });

  // ──────────────────────────────────────────────────────────────────────────
  // 1. Dialog & combo-box basics
  // ──────────────────────────────────────────────────────────────────────────

  test('1 - Placeholder button opens dialog', async ({ page }) => {
    await focusEditor(page);
    await getPlaceholderButton(page).click();
    await expect(getDialog(page)).toBeVisible({ timeout: 5000 });
  });

  test('2 - Combo box populated with configured placeholders', async ({ page }) => {
    await focusEditor(page);
    await openPlaceholderDialog(page);
    const comboBox = getComboBox(page);
    await comboBox.click();
    const overlay = page.locator('vaadin-combo-box-overlay');
    await overlay.waitFor({ state: 'visible', timeout: 5000 });
    const items = overlay.locator('vaadin-combo-box-item');
    await expect(items).toHaveCount(3);
    await expect(items.nth(0)).toContainText('N-1=Company Name');
    await expect(items.nth(1)).toContainText('A-1=Street Address');
    await expect(items.nth(2)).toContainText('D-1=2024-01-01');
  });

  test('3 - Combo box filters/searches', async ({ page }) => {
    await focusEditor(page);
    await openPlaceholderDialog(page);
    const comboBox = getComboBox(page);
    // Click to open dropdown, then type filter text
    await comboBox.click();
    const listbox = page.getByRole('listbox');
    await listbox.waitFor({ state: 'visible', timeout: 5000 });
    await comboBox.locator('input').pressSequentially('Street', { delay: 50 });
    // Wait for filter to apply — visible options in the listbox
    const options = listbox.getByRole('option');
    await expect(options).toHaveCount(1, { timeout: 5000 });
    await expect(options.first()).toContainText('A-1=Street Address');
  });

  test('4 - Insert placeholder from dialog (OK)', async ({ page }) => {
    await focusEditor(page);
    await insertPlaceholder(page, 'N-1=Company Name');
    // Verify a PlaceholderBlot span exists in the DOM
    await expect(getPlaceholderBlots(page)).toHaveCount(1);
    // Verify it appears in the delta
    const delta = await getDelta(page);
    expect(countInDelta(delta, 'placeholder')).toBe(1);
  });

  test('5 - Cancel dialog without selection', async ({ page }) => {
    await focusEditor(page);
    await openPlaceholderDialog(page);
    await cancelDialog(page);
    await expect(getDialog(page)).toBeHidden({ timeout: 5000 });
    // No placeholder should have been inserted
    await expect(getPlaceholderBlots(page)).toHaveCount(0);
  });

  test('6 - Insert multiple placeholders', async ({ page }) => {
    await focusEditor(page);
    await insertPlaceholder(page, 'N-1=Company Name');
    await insertPlaceholder(page, 'A-1=Street Address');
    await expect(getPlaceholderBlots(page)).toHaveCount(2);
    const delta = await getDelta(page);
    expect(countInDelta(delta, 'placeholder')).toBe(2);
  });

  test('7 - Remove placeholder via dialog (Remove button)', async ({ page }) => {
    await focusEditor(page);
    await insertPlaceholder(page, 'N-1=Company Name');
    await expect(getPlaceholderBlots(page)).toHaveCount(1);
    // Click on the placeholder to select it
    await getPlaceholderBlots(page).first().click();
    await page.waitForTimeout(200);
    // Open dialog — because we are on a placeholder, the Remove button should be visible
    await openPlaceholderDialog(page);
    await removeViaDialog(page);
    await expect(getDialog(page)).toBeHidden({ timeout: 5000 });
    await expect(getPlaceholderBlots(page)).toHaveCount(0);
  });

  // ──────────────────────────────────────────────────────────────────────────
  // 8–10. Placeholder display
  // ──────────────────────────────────────────────────────────────────────────

  test('8 - Placeholder displays correct text', async ({ page }) => {
    await focusEditor(page);
    await insertPlaceholder(page, 'N-1=Company Name');
    const blot = getPlaceholderBlots(page).first();
    await expect(blot).toContainText('N-1=Company Name');
  });

  test('9 - Placeholder tags (start/end)', async ({ page }) => {
    // Default tags: start="@", end=""
    await focusEditor(page);
    await insertPlaceholder(page, 'D-1=2024-01-01');
    const blot = getPlaceholderBlots(page).first();
    const text = await blot.textContent();
    // The visible text should start with the "@" tag
    expect(text).toContain('@');
    expect(text).toContain('D-1=2024-01-01');
  });

  test('10 - Tags change after placeholders exist', async ({ page }) => {
    // This test would require a Java API call to change tags at runtime.
    // The test view does not expose a tag-change control, so we verify via JS evaluation.
    await focusEditor(page);
    await insertPlaceholder(page, 'N-1=Company Name');
    const blotBefore = await getPlaceholderBlots(page).first().textContent();
    expect(blotBefore).toContain('@');

    // Change tags via JS on the component
    await page.evaluate(() => {
      const el = document.getElementById('test-editor') as any;
      el.placeholderTags = { start: '[', end: ']' };
    });
    await page.waitForTimeout(500);

    // After tag change, existing placeholders should re-render with new tags
    const blotAfter = await getPlaceholderBlots(page).first().textContent();
    expect(blotAfter).toContain('[');
    expect(blotAfter).toContain(']');
    expect(blotAfter).not.toContain('@');
  });

  // ──────────────────────────────────────────────────────────────────────────
  // 11–12. Alt appearance
  // ──────────────────────────────────────────────────────────────────────────

  test('11 - Alt appearance toggle', async ({ page }) => {
    await focusEditor(page);
    await insertPlaceholder(page, 'N-1=Company Name');
    const blot = getPlaceholderBlots(page).first();
    // Default appearance: full text with tags
    const textBefore = await blot.textContent();
    expect(textBefore).toContain('N-1=Company Name');

    // Click the appearance toggle button
    await getAppearanceToggleButton(page).click();
    await page.waitForTimeout(500);

    // After toggle, alt appearance pattern "(?<=\=).*$" extracts text after "="
    // So "N-1=Company Name" -> alt text is "Company Name"
    const textAfter = await getPlaceholderBlots(page).first().textContent();
    expect(textAfter).toContain('Company Name');
    // Alt appearance should NOT have the full "N-1=" prefix visible
    expect(textAfter).not.toContain('N-1=');
  });

  test('12 - Alt appearance pattern matching', async ({ page }) => {
    await focusEditor(page);
    // Insert all three placeholders to test pattern matching on different texts
    await insertPlaceholder(page, 'N-1=Company Name');
    await insertPlaceholder(page, 'A-1=Street Address');
    await insertPlaceholder(page, 'D-1=2024-01-01');

    // Toggle to alt appearance
    await getAppearanceToggleButton(page).click();
    await page.waitForTimeout(500);

    // Pattern "(?<=\=).*$" extracts everything after "="
    // Verify all 3 alt texts appear (order depends on cursor position at insertion time)
    const allText = await getPlaceholderBlots(page).allTextContents();
    const joined = allText.join('|');
    expect(joined).toContain('Company Name');
    expect(joined).toContain('Street Address');
    expect(joined).toContain('2024-01-01');
  });

  // ──────────────────────────────────────────────────────────────────────────
  // 13–15. Placeholder formatting
  // ──────────────────────────────────────────────────────────────────────────

  test('13 - Placeholder format (bold/italic)', async ({ page }) => {
    // p1 has format: { italic: true }
    await focusEditor(page);
    await insertPlaceholder(page, 'N-1=Company Name');
    const blot = getPlaceholderBlots(page).first();
    // Format is applied to the [contenteditable="false"] child, not the blot span itself
    const contentNode = blot.locator('[contenteditable="false"]');
    const fontStyle = await contentNode.evaluate(el => window.getComputedStyle(el).fontStyle);
    expect(fontStyle).toBe('italic');
  });

  test('14 - Placeholder altFormat', async ({ page }) => {
    // p1 has altFormat: { bold: true }
    await focusEditor(page);
    await insertPlaceholder(page, 'N-1=Company Name');
    // Toggle to alt appearance — altFormat { bold: true } should apply
    await getAppearanceToggleButton(page).click();
    await page.waitForTimeout(500);
    const blot = getPlaceholderBlots(page).first();
    // altFormat is applied to the [alt] child span inside the blot
    const altNode = blot.locator('[alt]');
    const fontWeight = await altNode.evaluate(el => {
      return window.getComputedStyle(el).fontWeight;
    });
    // bold = 700 or "bold"
    expect(parseInt(fontWeight)).toBeGreaterThanOrEqual(700);
  });

  test('15 - Placeholder format with link', async ({ page }) => {
    // p2 (A-1=Street Address) has altFormat: { link: "https://example.com" }
    await focusEditor(page);
    await insertPlaceholder(page, 'A-1=Street Address');
    // Toggle to alt appearance to trigger altFormat with link
    await getAppearanceToggleButton(page).click();
    await page.waitForTimeout(500);
    const blot = getPlaceholderBlots(page).first();
    // Should contain an <a> tag wrapping the content
    const link = blot.locator('a');
    await expect(link).toBeVisible();
    const href = await link.getAttribute('href');
    expect(href).toBe('https://example.com');
  });

  // ──────────────────────────────────────────────────────────────────────────
  // 16. Keyboard shortcut
  // ──────────────────────────────────────────────────────────────────────────

  test('16 - Keyboard insert (Ctrl+P)', async ({ page }) => {
    await focusEditor(page);
    // Press Ctrl+P to open the placeholder dialog
    await page.keyboard.press('Control+p');
    await expect(getDialog(page)).toBeVisible({ timeout: 5000 });
  });

  // ──────────────────────────────────────────────────────────────────────────
  // 17–20. Edit operations on placeholders
  // ──────────────────────────────────────────────────────────────────────────

  test('17 - Delete selected placeholder via keyboard', async ({ page }) => {
    await focusEditor(page);
    await insertPlaceholder(page, 'N-1=Company Name');
    await expect(getPlaceholderBlots(page)).toHaveCount(1);

    // Select all content (including the placeholder embed)
    await page.keyboard.press('Control+a');
    await page.waitForTimeout(200);

    // Delete the selection
    await page.keyboard.press('Delete');
    await page.waitForTimeout(500);

    // Placeholder should be gone
    await expect(getPlaceholderBlots(page)).toHaveCount(0);
  });

  test.fixme('18 - Copy-paste placeholder', async ({ page }) => {
    // FIXME: Placeholder embed doesn't survive clipboard HTML→delta roundtrip
    // TODO(post-migration): Re-attempt with Quill 2 clipboard module — may handle embeds better.
    await focusEditor(page);
    await insertPlaceholder(page, 'N-1=Company Name');
    await expect(getPlaceholderBlots(page)).toHaveCount(1);

    // Select all content and copy
    await page.keyboard.press('Control+a');
    await page.keyboard.press('Control+c');
    // Move to end
    await page.keyboard.press('End');
    // Paste
    await page.keyboard.press('Control+v');
    await page.waitForTimeout(500);

    // Should now have 2 placeholder blots (original + pasted)
    await expect(getPlaceholderBlots(page)).toHaveCount(2);
    const delta = await getDelta(page);
    expect(countInDelta(delta, 'placeholder')).toBe(2);
  });

  test('19 - Undo placeholder insert', async ({ page }) => {
    await focusEditor(page);
    await insertPlaceholder(page, 'N-1=Company Name');
    await expect(getPlaceholderBlots(page)).toHaveCount(1);

    // Undo
    await page.keyboard.press('Control+z');
    await page.waitForTimeout(500);

    await expect(getPlaceholderBlots(page)).toHaveCount(0);
  });

  test.fixme('20 - Undo placeholder remove', async ({ page }) => {
    // FIXME: Quill history doesn't properly restore placeholder embed blots after undo
    // TODO(post-migration): Re-attempt with Quill 2 history module — embed undo may work differently.
    await focusEditor(page);
    await insertPlaceholder(page, 'N-1=Company Name');
    await expect(getPlaceholderBlots(page)).toHaveCount(1);

    // Click on placeholder, then remove it via dialog
    await getPlaceholderBlots(page).first().click();
    await page.waitForTimeout(200);
    await openPlaceholderDialog(page);
    await removeViaDialog(page);
    await expect(getDialog(page)).toBeHidden({ timeout: 5000 });
    await expect(getPlaceholderBlots(page)).toHaveCount(0);

    // Undo the removal
    await focusEditor(page);
    await page.keyboard.press('Control+z');
    await page.waitForTimeout(500);

    await expect(getPlaceholderBlots(page)).toHaveCount(1);
  });

  // ──────────────────────────────────────────────────────────────────────────
  // 21–30. Events
  // ──────────────────────────────────────────────────────────────────────────

  test('21 - PlaceholderButtonClickedEvent', async ({ page }) => {
    await focusEditor(page);
    await clearEventLog(page);
    await getPlaceholderButton(page).click();
    await waitForEvent(page, 'PlaceholderButtonClicked');
    const events = await getEventLog(page);
    const match = events.find(e => e.includes('PlaceholderButtonClicked'));
    expect(match).toBeTruthy();
    expect(match).toContain('position=');
    // Close the dialog to clean up
    await cancelDialog(page);
  });

  test('22 - PlaceholderBeforeInsertEvent (confirm)', async ({ page }) => {
    // auto-confirm is ON by default
    await focusEditor(page);
    await clearEventLog(page);
    await insertPlaceholder(page, 'N-1=Company Name');
    await waitForEvent(page, 'PlaceholderBeforeInsert');
    const events = await getEventLog(page);
    const beforeInsert = events.find(e => e.includes('PlaceholderBeforeInsert'));
    expect(beforeInsert).toBeTruthy();
    expect(beforeInsert).toContain('N-1=Company Name');
    // Placeholder should actually be inserted (auto-confirm is on)
    await expect(getPlaceholderBlots(page)).toHaveCount(1);
  });

  test('23 - PlaceholderBeforeInsertEvent (cancel)', async ({ page }) => {
    // Uncheck auto-confirm-inserts to prevent insertion
    await page.locator('#auto-confirm-inserts').click();
    await page.waitForTimeout(200);

    await focusEditor(page);
    await clearEventLog(page);
    await openPlaceholderDialog(page);
    await selectComboItem(page, 'N-1=Company Name');
    await confirmDialog(page);
    await expect(getDialog(page)).toBeHidden({ timeout: 5000 });

    // The before-insert event should fire
    await waitForEvent(page, 'PlaceholderBeforeInsert');
    // But the placeholder should NOT be inserted (auto-confirm is off, event.insert() not called)
    await expect(getPlaceholderBlots(page)).toHaveCount(0);
    // And no PlaceholderInserted event should have fired
    const events = await getEventLog(page);
    const inserted = events.find(e => e.includes('PlaceholderInserted:'));
    expect(inserted).toBeUndefined();
  });

  test('24 - PlaceholderInsertedEvent', async ({ page }) => {
    await focusEditor(page);
    await clearEventLog(page);
    await insertPlaceholder(page, 'A-1=Street Address');
    await waitForEvent(page, 'PlaceholderInserted');
    const events = await getEventLog(page);
    const inserted = events.find(e => e.includes('PlaceholderInserted'));
    expect(inserted).toBeTruthy();
    expect(inserted).toContain('A-1=Street Address');
  });

  test('25 - PlaceholderBeforeRemoveEvent (confirm)', async ({ page }) => {
    await focusEditor(page);
    await insertPlaceholder(page, 'N-1=Company Name');
    await clearEventLog(page);

    // Click on placeholder, remove via dialog
    await getPlaceholderBlots(page).first().click();
    await page.waitForTimeout(200);
    await openPlaceholderDialog(page);
    await removeViaDialog(page);
    await expect(getDialog(page)).toBeHidden({ timeout: 5000 });

    await waitForEvent(page, 'PlaceholderBeforeRemove');
    const events = await getEventLog(page);
    const beforeRemove = events.find(e => e.includes('PlaceholderBeforeRemove'));
    expect(beforeRemove).toBeTruthy();
    expect(beforeRemove).toContain('N-1=Company Name');
    // Placeholder should actually be removed (auto-confirm-removes is on)
    await expect(getPlaceholderBlots(page)).toHaveCount(0);
  });

  test('26 - PlaceholderBeforeRemoveEvent (cancel)', async ({ page }) => {
    await focusEditor(page);
    await insertPlaceholder(page, 'N-1=Company Name');
    await expect(getPlaceholderBlots(page)).toHaveCount(1);

    // Uncheck auto-confirm-removes to prevent removal
    await page.locator('#auto-confirm-removes').click();
    await page.waitForTimeout(200);
    await clearEventLog(page);

    // Click on placeholder, attempt to remove via dialog
    await getPlaceholderBlots(page).first().click();
    await page.waitForTimeout(200);
    await openPlaceholderDialog(page);
    await removeViaDialog(page);
    await expect(getDialog(page)).toBeHidden({ timeout: 5000 });

    // The before-remove event should fire
    await waitForEvent(page, 'PlaceholderBeforeRemove');
    // But the placeholder should NOT be removed (auto-confirm-removes is off)
    await expect(getPlaceholderBlots(page)).toHaveCount(1);
    // And no PlaceholderRemoved event should have fired
    const events = await getEventLog(page);
    const removed = events.find(e => e.includes('PlaceholderRemoved:'));
    expect(removed).toBeUndefined();
  });

  test('27 - PlaceholderRemovedEvent', async ({ page }) => {
    await focusEditor(page);
    await insertPlaceholder(page, 'D-1=2024-01-01');
    await clearEventLog(page);

    await getPlaceholderBlots(page).first().click();
    await page.waitForTimeout(200);
    await openPlaceholderDialog(page);
    await removeViaDialog(page);
    await expect(getDialog(page)).toBeHidden({ timeout: 5000 });

    await waitForEvent(page, 'PlaceholderRemoved');
    const events = await getEventLog(page);
    const removed = events.find(e => e.includes('PlaceholderRemoved'));
    expect(removed).toBeTruthy();
    expect(removed).toContain('D-1=2024-01-01');
  });

  test('28 - PlaceholderSelectedEvent', async ({ page }) => {
    await focusEditor(page);
    await insertPlaceholder(page, 'N-1=Company Name');
    // Type text after the placeholder so we can move cursor away
    await page.keyboard.type(' text');
    await page.waitForTimeout(200);

    // Move cursor away from placeholder (to end of line)
    await page.keyboard.press('End');
    await page.waitForTimeout(300);
    await clearEventLog(page);

    // Click on the placeholder to trigger selection event
    await getPlaceholderBlots(page).first().click();
    await waitForEvent(page, 'PlaceholderSelected');
    const events = await getEventLog(page);
    const selected = events.find(e => e.includes('PlaceholderSelected'));
    expect(selected).toBeTruthy();
    expect(selected).toContain('N-1=Company Name');
  });

  test('29 - PlaceholderLeaveEvent', async ({ page }) => {
    await focusEditor(page);
    await insertPlaceholder(page, 'N-1=Company Name');
    // Type some text after the placeholder so we have a non-placeholder position
    await page.keyboard.type(' some text');
    await clearEventLog(page);

    // Click on the placeholder first
    await getPlaceholderBlots(page).first().click();
    await waitForEvent(page, 'PlaceholderSelected');
    await clearEventLog(page);

    // Now click somewhere else in the editor (on the text after the placeholder)
    const editor = getEditor(page);
    // Click at the end of the editor content
    await editor.click({ position: { x: 200, y: 10 } });
    await waitForEvent(page, 'PlaceholderLeave');
    const events = await getEventLog(page);
    const leave = events.find(e => e.includes('PlaceholderLeave'));
    expect(leave).toBeTruthy();
  });

  test('30 - PlaceholderAppearanceChangedEvent', async ({ page }) => {
    await focusEditor(page);
    await insertPlaceholder(page, 'N-1=Company Name');
    await clearEventLog(page);

    // Toggle appearance
    await getAppearanceToggleButton(page).click();
    await waitForEvent(page, 'PlaceholderAppearanceChanged');
    const events = await getEventLog(page);
    const appearance = events.find(e => e.includes('PlaceholderAppearanceChanged'));
    expect(appearance).toBeTruthy();
    expect(appearance).toContain('alt=');
    expect(appearance).toContain('label=');
  });

  // ──────────────────────────────────────────────────────────────────────────
  // 31–32. Batch / multi-placeholder operations
  // ──────────────────────────────────────────────────────────────────────────

  test('31 - Batch insert multiple placeholders', async ({ page }) => {
    // The dialog inserts one placeholder at a time, so "batch insert" is tested
    // by inserting multiple placeholders in sequence and verifying all are present.
    await focusEditor(page);

    await insertPlaceholder(page, 'N-1=Company Name');
    await insertPlaceholder(page, 'A-1=Street Address');
    await insertPlaceholder(page, 'D-1=2024-01-01');

    await expect(getPlaceholderBlots(page)).toHaveCount(3);

    const delta = await getDelta(page);
    expect(countInDelta(delta, 'placeholder')).toBe(3);
  });

  test('32 - Delete selection with multiple placeholders', async ({ page }) => {
    await focusEditor(page);
    // Insert two placeholders with text between them
    await insertPlaceholder(page, 'N-1=Company Name');
    await page.keyboard.type(' middle text ');
    await insertPlaceholder(page, 'A-1=Street Address');

    await expect(getPlaceholderBlots(page)).toHaveCount(2);

    // Select all content
    await page.keyboard.press('Control+a');
    await page.waitForTimeout(200);

    // Delete the selection
    await page.keyboard.press('Delete');
    await page.waitForTimeout(500);

    // Both placeholders and all text should be gone
    await expect(getPlaceholderBlots(page)).toHaveCount(0);
    const delta = await getDelta(page);
    expect(countInDelta(delta, 'placeholder')).toBe(0);
  });
});
