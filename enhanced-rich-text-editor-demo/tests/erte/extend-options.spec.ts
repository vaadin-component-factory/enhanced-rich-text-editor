import { test, expect } from '@playwright/test';
import { ERTE_TEST_BASE, waitForEditor, getEditor, getDelta } from './helpers';

test.describe('Phase 3.1g: extendQuill / extendEditor hooks', () => {

  test.beforeEach(async ({ page }) => {
    await page.goto(`${ERTE_TEST_BASE}/extend-options`);
    await waitForEditor(page);
  });

  test('extendEditor hook fires and sets flag on editor root', async ({ page }) => {
    const flag = await page.evaluate(() => {
      const el = document.getElementById('test-editor') as any;
      return el._editor.root.dataset.extendEditorCalled;
    });
    expect(flag).toBe('true');
  });

  test('extendQuill hook registers highlight format', async ({ page }) => {
    const editor = getEditor(page);
    await editor.click();
    await page.keyboard.type('Hello World');

    // Apply highlight format to first 5 characters via Quill API
    await page.evaluate(() => {
      const el = document.getElementById('test-editor') as any;
      el._editor.formatText(0, 5, 'highlight', true, 'user');
    });

    // Verify <mark> tag appears in the editor
    const markCount = await editor.locator('mark').count();
    expect(markCount).toBe(1);
    const markText = await editor.locator('mark').textContent();
    expect(markText).toBe('Hello');
  });

  test('highlight format survives delta roundtrip', async ({ page }) => {
    const editor = getEditor(page);
    await editor.click();
    await page.keyboard.type('Highlighted text');

    // Apply highlight format
    await page.evaluate(() => {
      const el = document.getElementById('test-editor') as any;
      el._editor.formatText(0, 11, 'highlight', true, 'user');
    });

    // Check delta contains highlight attribute
    const delta = await getDelta(page);
    const highlightOp = delta.ops.find((op: any) => op.attributes?.highlight);
    expect(highlightOp).toBeTruthy();
    expect(highlightOp.insert).toBe('Highlighted');
    expect(highlightOp.attributes.highlight).toBe(true);

    // Verify the <mark> tag renders correctly
    const markEl = editor.locator('mark');
    await expect(markEl).toHaveText('Highlighted');
  });

  test('V24 extendOptions deprecation warning', async ({ page }) => {
    // Inject extendOptions before page scripts load (survives reload)
    await page.addInitScript(() => {
      (window as any).Vaadin = { Flow: { vcfEnhancedRichTextEditor: { extendOptions: [() => {}] } } };
    });

    // Collect console warnings on reload
    const warnings: string[] = [];
    page.on('console', msg => {
      if (msg.type() === 'warning') {
        warnings.push(msg.text());
      }
    });

    await page.reload();
    await waitForEditor(page);

    const deprecationWarning = warnings.find(w => w.includes('extendOptions is deprecated'));
    expect(deprecationWarning).toBeTruthy();
  });
});
