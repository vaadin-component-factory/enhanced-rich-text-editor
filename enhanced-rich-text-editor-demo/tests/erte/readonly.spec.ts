import { test, expect } from '@playwright/test';
import {
  waitForEditor,
  getErte,
  getEditor,
  getDelta,
  ERTE_TEST_BASE,
  focusEditor,
  getToolbar,
} from './helpers';

const READONLY_URL = `${ERTE_TEST_BASE}/readonly`;

/**
 * Helper: wait for editor ready, but without requiring contenteditable="true"
 * (needed after toggling whole-editor readonly).
 */
async function waitForEditorLoaded(page: import('@playwright/test').Page, id = 'test-editor') {
  await page.locator(`#${id}`).waitFor({ state: 'visible', timeout: 60000 });
  await page.waitForFunction(
    (elId) => {
      const el = document.getElementById(elId) as any;
      return el?._editor?.root != null;
    },
    id,
    { timeout: 60000 }
  );
  await page.locator('#test-ready[data-ready="true"]').waitFor({ timeout: 10000 });
}

/**
 * Helper: get the current delta directly from the editor (bypasses delta-output element timing).
 */
async function getEditorDelta(page: import('@playwright/test').Page, id = 'test-editor'): Promise<any> {
  return await page.evaluate((elId) => {
    const el = document.getElementById(elId) as any;
    return el._editor.getContents();
  }, id);
}

/**
 * Helper: count readonly blots in a delta.
 * Readonly is a format (attribute), not an embed. Count ops with attributes.readonly === true.
 */
function countReadonly(delta: any): number {
  return delta.ops.filter((op: any) => op.attributes && op.attributes.readonly === true).length;
}

/**
 * Helper: get all readonly span locators inside the editor.
 */
function getReadonlySpans(page: import('@playwright/test').Page, id = 'test-editor') {
  return page.locator(`#${id}`).locator('span.ql-readonly[contenteditable="false"]');
}

/**
 * Helper: short stabilization delay for delta output to update after editor operations.
 */
async function waitForDeltaUpdate(page: import('@playwright/test').Page) {
  await page.waitForTimeout(300);
}

test.describe('ERTE Readonly Sections', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto(READONLY_URL);
    await waitForEditor(page);
  });

  // ==========================================================================
  // 1. Readonly blot renders
  // ==========================================================================
  test('Readonly blot renders as span.ql-readonly with contenteditable false', async ({ page }) => {
    const readonlySpans = getReadonlySpans(page);
    await expect(readonlySpans.first()).toBeVisible();

    // Verify the attribute
    const contentEditable = await readonlySpans.first().getAttribute('contenteditable');
    expect(contentEditable).toBe('false');

    // Verify CSS class
    const className = await readonlySpans.first().getAttribute('class');
    expect(className).toContain('ql-readonly');

    // We expect at least 2 readonly sections from the initial delta
    const count = await readonlySpans.count();
    expect(count).toBeGreaterThanOrEqual(2);
  });

  // ==========================================================================
  // 2. Cannot edit inside readonly section
  // ==========================================================================
  test('Cannot edit inside readonly section', async ({ page }) => {
    const readonlySpan = getReadonlySpans(page).first();
    const deltaBefore = await getEditorDelta(page);
    const readonlyCountBefore = countReadonly(deltaBefore);

    // Click on the readonly text
    await readonlySpan.click();
    await page.waitForTimeout(100);

    // Try typing
    await page.keyboard.type('INJECTED');
    await waitForDeltaUpdate(page);

    // Verify no text was injected into the readonly blot
    const deltaAfter = await getEditorDelta(page);
    const readonlyCountAfter = countReadonly(deltaAfter);
    expect(readonlyCountAfter).toBe(readonlyCountBefore);

    // The readonly blot text should not contain the injected text
    const readonlyText = await readonlySpan.textContent();
    expect(readonlyText).not.toContain('INJECTED');
  });

  // ==========================================================================
  // 3. Cannot delete readonly section (Backspace)
  // ==========================================================================
  // FIXME: Backspace adjacent to contenteditable=false span may delete it in some browsers.
  test('Cannot delete readonly section with Backspace', async ({ page }) => {
    const deltaBefore = await getEditorDelta(page);
    const readonlyCountBefore = countReadonly(deltaBefore);

    // Position cursor right after the first readonly section by clicking on editable text after it
    const editor = getEditor(page);
    // Click at the start of " Editable text after." which is right after the readonly span
    const readonlySpan = getReadonlySpans(page).first();
    const box = await readonlySpan.boundingBox();
    // Click just to the right of the readonly span
    await page.mouse.click(box!.x + box!.width + 2, box!.y + box!.height / 2);
    await page.waitForTimeout(100);

    // Press Backspace to try to delete the readonly section
    await page.keyboard.press('Backspace');
    await waitForDeltaUpdate(page);

    const deltaAfter = await getEditorDelta(page);
    const readonlyCountAfter = countReadonly(deltaAfter);
    expect(readonlyCountAfter).toBe(readonlyCountBefore);
  });

  // ==========================================================================
  // 4. Cannot delete readonly section (Delete key)
  // ==========================================================================
  test('Cannot delete readonly section with Delete key', async ({ page }) => {
    const deltaBefore = await getEditorDelta(page);
    const readonlyCountBefore = countReadonly(deltaBefore);

    // Position cursor right before the first readonly section
    const readonlySpan = getReadonlySpans(page).first();
    const box = await readonlySpan.boundingBox();
    // Click just to the left of the readonly span
    await page.mouse.click(box!.x - 2, box!.y + box!.height / 2);
    await page.waitForTimeout(100);

    // Press Delete to try to delete the readonly section
    await page.keyboard.press('Delete');
    await waitForDeltaUpdate(page);

    const deltaAfter = await getEditorDelta(page);
    const readonlyCountAfter = countReadonly(deltaAfter);
    expect(readonlyCountAfter).toBe(readonlyCountBefore);
  });

  // ==========================================================================
  // 5. Select-all + Delete prevented
  // ==========================================================================
  test('Select-all + Delete does not remove readonly sections', async ({ page }) => {
    const deltaBefore = await getEditorDelta(page);
    const readonlyCountBefore = countReadonly(deltaBefore);
    expect(readonlyCountBefore).toBeGreaterThan(0);

    await focusEditor(page);
    await page.keyboard.press('Control+a');
    await page.waitForTimeout(100);
    await page.keyboard.press('Delete');
    await waitForDeltaUpdate(page);

    const deltaAfter = await getEditorDelta(page);
    const readonlyCountAfter = countReadonly(deltaAfter);
    expect(readonlyCountAfter).toBe(readonlyCountBefore);
  });

  // ==========================================================================
  // 6. Cut operation on readonly prevented
  // ==========================================================================
  test('Cut operation on readonly text is prevented', async ({ page }) => {
    const deltaBefore = await getEditorDelta(page);
    const readonlyCountBefore = countReadonly(deltaBefore);

    // Select the first readonly span
    const readonlySpan = getReadonlySpans(page).first();
    const box = await readonlySpan.boundingBox();

    // Triple-click to select, or use click-drag across the readonly span
    await page.mouse.click(box!.x + 1, box!.y + box!.height / 2);
    await page.waitForTimeout(50);
    // Select the readonly content with shift+click at the end
    await page.mouse.click(box!.x + box!.width - 1, box!.y + box!.height / 2, { modifiers: ['Shift'] });
    await page.waitForTimeout(100);

    // Try to cut
    await page.keyboard.press('Control+x');
    await waitForDeltaUpdate(page);

    const deltaAfter = await getEditorDelta(page);
    const readonlyCountAfter = countReadonly(deltaAfter);
    expect(readonlyCountAfter).toBe(readonlyCountBefore);
  });

  // ==========================================================================
  // 7. Can select and copy readonly text
  // ==========================================================================
  test('Can select and copy readonly text without removing it', async ({ page }) => {
    const readonlySpan = getReadonlySpans(page).first();
    const textBefore = await readonlySpan.textContent();

    // Select the readonly content
    const box = await readonlySpan.boundingBox();
    await page.mouse.click(box!.x + 1, box!.y + box!.height / 2);
    await page.waitForTimeout(50);
    await page.mouse.click(box!.x + box!.width - 1, box!.y + box!.height / 2, { modifiers: ['Shift'] });
    await page.waitForTimeout(100);

    // Copy (should not throw or remove content)
    await page.keyboard.press('Control+c');
    await waitForDeltaUpdate(page);

    // Verify readonly content is still present and unchanged
    const textAfter = await readonlySpan.textContent();
    expect(textAfter).toBe(textBefore);

    const delta = await getEditorDelta(page);
    expect(countReadonly(delta)).toBeGreaterThan(0);
  });

  // ==========================================================================
  // 8. Toggle readonly via toolbar button
  // ==========================================================================
  test('Toggle readonly via toolbar button applies and removes format', async ({ page }) => {
    const editor = getEditor(page);
    await editor.click();

    // Type some new text
    await page.keyboard.press('End');
    await page.keyboard.press('Enter');
    await page.keyboard.type('Text to make readonly');

    // Select the text we just typed
    await page.keyboard.press('Home');
    await page.keyboard.press('Shift+End');
    await page.waitForTimeout(100);

    // Count readonly sections before clicking toolbar button
    const deltaBefore = await getEditorDelta(page);
    const readonlyCountBefore = countReadonly(deltaBefore);

    // Click the readonly toolbar button
    const readonlyButton = page.locator('#test-editor').locator('[part~="toolbar-button-readonly"]');
    await readonlyButton.click();
    await waitForDeltaUpdate(page);

    // Verify a new readonly section was created
    const deltaAfterApply = await getEditorDelta(page);
    expect(countReadonly(deltaAfterApply)).toBeGreaterThan(readonlyCountBefore);

    // Select again and toggle off
    await page.keyboard.press('Home');
    await page.keyboard.press('Shift+End');
    await page.waitForTimeout(100);
    await readonlyButton.click();
    await waitForDeltaUpdate(page);

    const deltaAfterRemove = await getEditorDelta(page);
    expect(countReadonly(deltaAfterRemove)).toBe(readonlyCountBefore);
  });

  // ==========================================================================
  // 9. Cursor navigation around readonly
  // ==========================================================================
  test('Cursor navigation with arrow keys around readonly boundary', async ({ page }) => {
    // Click before the first readonly span
    const readonlySpan = getReadonlySpans(page).first();
    const box = await readonlySpan.boundingBox();
    await page.mouse.click(box!.x - 2, box!.y + box!.height / 2);
    await page.waitForTimeout(100);

    // Press ArrowRight to move past the readonly section
    // The cursor should jump over the readonly blot (contenteditable=false)
    await page.keyboard.press('ArrowRight');
    await page.waitForTimeout(100);

    // Type a character after the readonly -- it should go in editable area, not inside readonly
    await page.keyboard.type('X');
    await waitForDeltaUpdate(page);

    // Verify the X is not inside the readonly blot
    const readonlyText = await readonlySpan.textContent();
    expect(readonlyText).not.toContain('X');

    // The delta should still have the same readonly count
    const delta = await getEditorDelta(page);
    expect(countReadonly(delta)).toBeGreaterThanOrEqual(2);
  });

  // ==========================================================================
  // 10. Click inside readonly places cursor at boundary
  // ==========================================================================
  test('Click inside readonly places cursor at boundary, not inside', async ({ page }) => {
    const readonlySpan = getReadonlySpans(page).first();
    const box = await readonlySpan.boundingBox();

    // Click in the middle of the readonly span
    await page.mouse.click(box!.x + box!.width / 2, box!.y + box!.height / 2);
    await page.waitForTimeout(100);

    // Type something -- it should not appear inside the readonly blot
    await page.keyboard.type('Z');
    await waitForDeltaUpdate(page);

    const readonlyText = await readonlySpan.textContent();
    expect(readonlyText).not.toContain('Z');
  });

  // ==========================================================================
  // 11. Readonly survives undo/redo
  // ==========================================================================
  // FIXME: Undo operation removes readonly formatting (readonly count drops to 0).
  // The ERTE undo handler needs to preserve readonly attributes during history operations.
  // TODO(post-migration): Re-attempt fix with Quill 2 history module — may behave differently.
  test.fixme('Readonly sections survive undo and redo', async ({ page }) => {
    const deltaBefore = await getEditorDelta(page);
    const readonlyCountBefore = countReadonly(deltaBefore);

    // Focus and type some editable content
    await focusEditor(page);
    await page.keyboard.press('End');
    await page.keyboard.type(' added text');
    await waitForDeltaUpdate(page);

    // Undo the typed text
    await page.keyboard.press('Control+z');
    await waitForDeltaUpdate(page);

    const deltaAfterUndo = await getEditorDelta(page);
    expect(countReadonly(deltaAfterUndo)).toBe(readonlyCountBefore);

    // Redo
    await page.keyboard.press('Control+y');
    await waitForDeltaUpdate(page);

    const deltaAfterRedo = await getEditorDelta(page);
    expect(countReadonly(deltaAfterRedo)).toBe(readonlyCountBefore);
  });

  // ==========================================================================
  // 12. Whole-editor readonly mode hides toolbar and disables editing
  // ==========================================================================
  test('Whole-editor readonly mode hides toolbar and prevents editing', async ({ page }) => {
    const toolbar = getToolbar(page);
    const editor = getEditor(page);

    // Toolbar should be visible initially
    await expect(toolbar).toBeVisible();

    // Click toggle-readonly button
    await page.locator('#toggle-readonly').click();
    await page.waitForTimeout(500);

    // Toolbar should be hidden in readonly mode
    await expect(toolbar).toBeHidden();

    // Editor should be non-editable
    const contentEditable = await editor.getAttribute('contenteditable');
    expect(contentEditable).toBe('false');
  });

  // ==========================================================================
  // 13. Whole-editor readonly toggle back restores toolbar
  // ==========================================================================
  test('Whole-editor readonly toggle back restores toolbar and editing', async ({ page }) => {
    const toolbar = getToolbar(page);
    const editor = getEditor(page);

    // Toggle readonly ON
    await page.locator('#toggle-readonly').click();
    await page.waitForTimeout(500);
    await expect(toolbar).toBeHidden();

    // Toggle readonly OFF
    await page.locator('#toggle-readonly').click();
    await page.waitForTimeout(500);

    // Toolbar should reappear
    await expect(toolbar).toBeVisible();

    // Editor should be editable again
    const contentEditable = await editor.getAttribute('contenteditable');
    expect(contentEditable).toBe('true');
  });

  // ==========================================================================
  // 14. Readonly + formatted text preserved
  // ==========================================================================
  test('Bold and italic formatting preserved inside readonly sections', async ({ page }) => {
    // The initial delta includes a readonly section with bold formatting:
    // {"insert":"bold formatting","attributes":{"readonly":true,"bold":true}}
    const delta = await getEditorDelta(page);

    // Find the readonly op that also has bold formatting
    const boldReadonlyOps = delta.ops.filter(
      (op: any) =>
        typeof op.insert === 'string' &&
        op.attributes &&
        op.attributes.readonly === true &&
        op.attributes.bold === true
    );
    expect(boldReadonlyOps.length).toBeGreaterThan(0);

    // Verify the text content is correct
    expect(boldReadonlyOps[0].insert).toBe('bold formatting');
  });

  // ==========================================================================
  // 15. Readonly + copy-paste behavior
  // ==========================================================================
  test('Pasting content does not affect readonly sections', async ({ page }) => {
    const deltaBefore = await getEditorDelta(page);
    const readonlyCountBefore = countReadonly(deltaBefore);

    // Focus editor and place cursor at the very beginning
    await focusEditor(page);
    await page.keyboard.press('Home');
    await page.keyboard.press('Home');
    await page.waitForTimeout(100);

    // Type text, select it, copy it
    await page.keyboard.type('PASTE_SOURCE');
    await page.keyboard.press('Home');
    for (let i = 0; i < 'PASTE_SOURCE'.length; i++) {
      await page.keyboard.press('Shift+ArrowRight');
    }
    await page.keyboard.press('Control+c');
    await page.waitForTimeout(100);

    // Move cursor to end and paste
    await page.keyboard.press('End');
    await page.keyboard.press('Control+v');
    await waitForDeltaUpdate(page);

    // Readonly sections should still be intact
    const deltaAfter = await getEditorDelta(page);
    expect(countReadonly(deltaAfter)).toBe(readonlyCountBefore);
  });

  // ==========================================================================
  // 16. Load Delta with readonly sections (programmatic setValue)
  // ==========================================================================
  test('Load Delta with readonly sections via setValue button', async ({ page }) => {
    // Click the load-readonly button
    await page.locator('#load-readonly').click();
    await page.waitForTimeout(500);

    // Wait for delta output to update
    await waitForDeltaUpdate(page);

    // The loaded delta should contain: "Start " + readonly("Protected Section") + " End\n"
    const delta = await getEditorDelta(page);
    expect(countReadonly(delta)).toBe(1);

    // Verify the readonly blot rendered in the DOM
    const readonlySpans = getReadonlySpans(page);
    await expect(readonlySpans).toHaveCount(1);

    // Verify the text content
    const readonlyText = await readonlySpans.first().textContent();
    expect(readonlyText).toBe('Protected Section');
  });

  // ==========================================================================
  // 17. Readonly in Delta output (serialization)
  // ==========================================================================
  test('Readonly sections correctly serialized in delta output', async ({ page }) => {
    // Get the delta from the delta-output element (client-side serialization)
    const delta = await getDelta(page);

    // Verify readonly ops exist in the serialized delta (attribute format)
    const readonlyOps = delta.ops.filter(
      (op: any) => op.attributes && op.attributes.readonly === true
    );
    expect(readonlyOps.length).toBeGreaterThanOrEqual(2);

    // Verify the first readonly op has the expected text
    const firstReadonly = readonlyOps[0];
    expect(firstReadonly.insert).toBe('This is readonly content.');

    // Verify the structure contains both editable and readonly interleaved
    const insertTypes = delta.ops.map((op: any) => {
      if (op.attributes && op.attributes.readonly === true) return 'readonly';
      if (typeof op.insert === 'string') return 'text';
      return 'other';
    });
    // Should have pattern: text, readonly, text, ...
    expect(insertTypes).toContain('text');
    expect(insertTypes).toContain('readonly');
  });

  // ==========================================================================
  // Screenshot test: Readonly section gray background
  // ==========================================================================
  test('Screenshot: Readonly section gray background', async ({ page }) => {
    // Wait for rendering to stabilize
    await page.waitForTimeout(500);
    await expect(getErte(page)).toHaveScreenshot('readonly-section.png');
  });
});
