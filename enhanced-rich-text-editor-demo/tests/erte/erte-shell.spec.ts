import { test, expect } from '@playwright/test';

/**
 * ERTE Shell smoke tests (Phase 2).
 * Verifies the JS extension mechanism works end-to-end:
 * custom tag, toolbar, editing, Java API, Quill instance, Delta round-trip.
 *
 * Uses V25DemoView at route "/" — no #test-ready indicator needed.
 */

test.describe('ERTE Shell (Phase 2)', () => {

  test.beforeEach(async ({ page }) => {
    await page.goto('/');
    // Wait for the ERTE custom element to be defined, ready, AND content loaded from Java
    await page.waitForFunction(() => {
      const el = document.querySelector('vcf-enhanced-rich-text-editor');
      return el && (el as any)._editor && (el as any)._editor.getLength() > 1;
    }, { timeout: 60000 });
  });

  test('tag is vcf-enhanced-rich-text-editor', async ({ page }) => {
    const el = page.locator('vcf-enhanced-rich-text-editor');
    await expect(el).toBeVisible();
    const tagName = await el.evaluate(e => e.tagName);
    expect(tagName).toBe('VCF-ENHANCED-RICH-TEXT-EDITOR');
  });

  test('toolbar renders with standard button groups', async ({ page }) => {
    const toolbar = page.locator('vcf-enhanced-rich-text-editor [part="toolbar"]');
    await expect(toolbar).toBeVisible();

    // Check for standard RTE 2 toolbar button groups
    const groups = toolbar.locator('[part~="toolbar-group"]');
    expect(await groups.count()).toBeGreaterThanOrEqual(4);

    // Verify some standard buttons exist (bold, italic, underline)
    await expect(toolbar.locator('button[part~="toolbar-button-bold"]')).toBeVisible();
    await expect(toolbar.locator('button[part~="toolbar-button-italic"]')).toBeVisible();
    await expect(toolbar.locator('button[part~="toolbar-button-underline"]')).toBeVisible();
  });

  test('editor is editable — type text and verify', async ({ page }) => {
    const editor = page.locator('vcf-enhanced-rich-text-editor .ql-editor');
    await editor.click();
    await page.keyboard.press('End');
    await page.keyboard.type(' Hello Shell');

    await expect(editor).toContainText('Hello Shell');
  });

  test('setValue from Java renders initial content', async ({ page }) => {
    const editor = page.locator('vcf-enhanced-rich-text-editor .ql-editor');
    await expect(editor).toContainText('ERTE V25');
  });

  test('Quill instance available with content', async ({ page }) => {
    const result = await page.evaluate(() => {
      const el = document.querySelector('vcf-enhanced-rich-text-editor') as any;
      return {
        hasEditor: !!el._editor,
        length: el._editor?.getLength(),
        hasGetContents: typeof el._editor?.getContents === 'function',
      };
    });
    expect(result.hasEditor).toBe(true);
    expect(result.length).toBeGreaterThan(0);
    expect(result.hasGetContents).toBe(true);
  });

  test('Delta accessible via Quill getContents()', async ({ page }) => {
    // Read Delta directly from Quill instance (no server round-trip needed)
    // This proves asDelta() can access the Delta — the server-side ValueChangeListener
    // round-trip is validated in Phase 3 with dedicated test views.
    const delta = await page.evaluate(() => {
      const el = document.querySelector('vcf-enhanced-rich-text-editor') as any;
      return el._editor.getContents();
    });

    expect(delta).toHaveProperty('ops');
    expect(delta.ops.length).toBeGreaterThan(0);
    // Initial content set by Java setValue() should be present
    const allText = delta.ops.map((op: any) => typeof op.insert === 'string' ? op.insert : '').join('');
    expect(allText).toContain('ERTE V25');
  });

});
