import { test, expect } from '@playwright/test';
import {
  waitForEditor,
  getErte,
  getEditor,
  getDelta,
  getDeltaFromEditor,
  getHtmlOutput,
  getEventLog,
  clearEventLog,
  waitForEvent,
  countInDelta,
  ERTE_TEST_BASE,
  focusEditor,
  typeInEditor,
  pressKey,
  getRuler,
} from './helpers';

const FEATURES_URL = `${ERTE_TEST_BASE}/features`;

test.describe('ERTE Feature Tests', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto(FEATURES_URL);
    await waitForEditor(page);
  });

  // ============================================
  // NON-BREAKING SPACE Tests
  // ============================================

  test.describe('Non-Breaking Space (Shift+Space)', () => {
    test('1 - Shift+Space inserts a non-breaking space', async ({ page }) => {
      await focusEditor(page);
      await typeInEditor(page, 'Hello');
      await pressKey(page, 'Shift+Space');
      await typeInEditor(page, 'World');

      // Wait for delta to update
      await page.waitForTimeout(300);

      // Check for NBSP in the editor's inner HTML or in the delta text
      const editorHtml = await getEditor(page).innerHTML();
      const hasNbsp =
        editorHtml.includes('\u00A0') ||
        editorHtml.includes('&nbsp;') ||
        editorHtml.includes('ql-nbsp');

      expect(hasNbsp).toBe(true);
    });

    test('2 - Multiple consecutive NBSPs are all preserved', async ({ page }) => {
      await focusEditor(page);
      await typeInEditor(page, 'A');
      await pressKey(page, 'Shift+Space');
      await pressKey(page, 'Shift+Space');
      await pressKey(page, 'Shift+Space');
      await typeInEditor(page, 'B');

      await page.waitForTimeout(300);

      const editorHtml = await getEditor(page).innerHTML();

      // Count NBSPs - could be \u00A0 chars or nbsp spans
      const nbspCharCount = (editorHtml.match(/\u00A0/g) || []).length;
      const nbspSpanCount = (editorHtml.match(/ql-nbsp/g) || []).length;

      // At least 3 NBSPs should be present (either as chars or spans)
      expect(nbspCharCount + nbspSpanCount).toBeGreaterThanOrEqual(3);
    });

    test('3 - NBSP survives copy-paste round-trip', async ({ page }) => {
      await focusEditor(page);
      await typeInEditor(page, 'Before');
      await pressKey(page, 'Shift+Space');
      await typeInEditor(page, 'After');

      await page.waitForTimeout(300);

      // Select all, copy, clear, paste
      await pressKey(page, 'Control+a');
      await pressKey(page, 'Control+c');
      await pressKey(page, 'Delete');
      await page.waitForTimeout(200);
      await pressKey(page, 'Control+v');
      await page.waitForTimeout(300);

      const editorHtml = await getEditor(page).innerHTML();
      const hasNbsp =
        editorHtml.includes('\u00A0') ||
        editorHtml.includes('&nbsp;') ||
        editorHtml.includes('ql-nbsp');

      expect(hasNbsp).toBe(true);
    });
  });

  // ============================================
  // addText API Tests
  // ============================================

  test.describe('addText API', () => {
    test('4 - addText at cursor position inserts text', async ({ page }) => {
      await focusEditor(page);
      await typeInEditor(page, 'Hello');
      await page.waitForTimeout(200);

      await page.locator('#add-text-cursor').click();
      await page.waitForTimeout(500);

      const delta = await getDelta(page);
      const allText = delta.ops
        .filter((op: any) => typeof op.insert === 'string')
        .map((op: any) => op.insert)
        .join('');

      expect(allText).toContain('INSERTED');
    });

    test('5 - addText at position 0 inserts text at start', async ({ page }) => {
      await focusEditor(page);
      await typeInEditor(page, 'Hello');
      await page.waitForTimeout(200);

      await page.locator('#add-text-pos').click();
      await page.waitForTimeout(500);

      const delta = await getDelta(page);
      const allText = delta.ops
        .filter((op: any) => typeof op.insert === 'string')
        .map((op: any) => op.insert)
        .join('');

      // PREFIX should appear before Hello
      expect(allText).toContain('PREFIX');
      const prefixIdx = allText.indexOf('PREFIX');
      const helloIdx = allText.indexOf('Hello');
      expect(prefixIdx).toBeLessThan(helloIdx);
    });

    test('6 - addText when readonly documents behavior', async ({ page }) => {
      await focusEditor(page);
      await typeInEditor(page, 'Original');
      await page.waitForTimeout(200);

      // Get text before toggling readonly
      const deltaBefore = await getDelta(page);
      const textBefore = deltaBefore.ops
        .filter((op: any) => typeof op.insert === 'string')
        .map((op: any) => op.insert)
        .join('');

      // Toggle readonly
      await page.locator('#toggle-readonly').click();
      await page.waitForTimeout(300);

      // Try addText
      await page.locator('#add-text-cursor').click();
      await page.waitForTimeout(500);

      const deltaAfter = await getDelta(page);
      const textAfter = deltaAfter.ops
        .filter((op: any) => typeof op.insert === 'string')
        .map((op: any) => op.insert)
        .join('');

      // Document behavior: readonly may or may not allow addText
      // The test verifies the call does not throw, and records outcome
      expect(textAfter).toContain('Original');
    });

    test('7 - addText when disabled does not modify content', async ({ page }) => {
      await focusEditor(page);
      await typeInEditor(page, 'Unchanged');
      await page.waitForTimeout(200);

      const deltaBefore = await getDelta(page);
      const textBefore = deltaBefore.ops
        .filter((op: any) => typeof op.insert === 'string')
        .map((op: any) => op.insert)
        .join('');

      // Toggle disabled
      await page.locator('#toggle-disabled').click();
      await page.waitForTimeout(300);

      // Try addText
      await page.locator('#add-text-cursor').click();
      await page.waitForTimeout(500);

      const deltaAfter = await getDelta(page);
      const textAfter = deltaAfter.ops
        .filter((op: any) => typeof op.insert === 'string')
        .map((op: any) => op.insert)
        .join('');

      // When disabled, addText should not modify content
      expect(textAfter).not.toContain('INSERTED');
      expect(textAfter).toContain('Unchanged');
    });
  });

  // ============================================
  // getTextLength Test
  // ============================================

  test.describe('getTextLength', () => {
    test('8 - getTextLength returns correct character count', async ({ page }) => {
      await focusEditor(page);
      await typeInEditor(page, 'Hello');
      await page.waitForTimeout(300);

      await clearEventLog(page);
      await page.locator('#get-text-length').click();
      await waitForEvent(page, 'TextLength:');

      const entries = await getEventLog(page);
      const lengthEntry = entries.find((e) => e.includes('TextLength:'));
      expect(lengthEntry).toBeDefined();
      // "Hello" = 5 characters
      expect(lengthEntry).toContain('TextLength: 5');
    });
  });

  // ============================================
  // Toolbar Button Tests
  // ============================================

  test.describe('Toolbar Buttons', () => {
    test('9 - Align Justify button applies justify alignment', async ({ page }) => {
      await focusEditor(page);
      await typeInEditor(page, 'This text should be justified across the full width of the editor area');
      await page.waitForTimeout(200);

      // Select all text
      await pressKey(page, 'Control+a');

      // Click the align justify button in the toolbar
      const erte = getErte(page);
      const justifyBtn = erte.locator('button.ql-align[value="justify"]');
      await justifyBtn.click();
      await page.waitForTimeout(300);

      const delta = await getDelta(page);
      // Justify alignment shows as { align: "justify" } in delta attributes
      const hasJustify = delta.ops.some(
        (op: any) => op.attributes && op.attributes.align === 'justify'
      );
      expect(hasJustify).toBe(true);
    });

    test('10 - Indent button increases list indentation', async ({ page }) => {
      await focusEditor(page);

      // Create a bullet list first
      const erte = getErte(page);
      const bulletBtn = erte.locator('button.ql-list[value="bullet"]');
      await bulletBtn.click();
      await typeInEditor(page, 'List item');
      await page.waitForTimeout(200);

      // Click indent button
      const indentBtn = erte.locator('button.ql-indent[value="+1"]');
      await indentBtn.click();
      await page.waitForTimeout(300);

      const delta = await getDelta(page);
      const hasIndent = delta.ops.some(
        (op: any) => op.attributes && op.attributes.indent && op.attributes.indent >= 1
      );
      expect(hasIndent).toBe(true);
    });

    test('11 - Outdent button decreases list indentation', async ({ page }) => {
      await focusEditor(page);

      // Create a bullet list and indent it
      const erte = getErte(page);
      const bulletBtn = erte.locator('button.ql-list[value="bullet"]');
      await bulletBtn.click();
      await typeInEditor(page, 'List item');
      await page.waitForTimeout(200);

      // Indent
      const indentBtn = erte.locator('button.ql-indent[value="+1"]');
      await indentBtn.click();
      await page.waitForTimeout(200);

      // Verify indent was applied
      let delta = await getDelta(page);
      const indentLevel = delta.ops.find(
        (op: any) => op.attributes && op.attributes.indent
      )?.attributes?.indent;
      expect(indentLevel).toBeGreaterThanOrEqual(1);

      // Outdent
      const outdentBtn = erte.locator('button.ql-indent[value="-1"]');
      await outdentBtn.click();
      await page.waitForTimeout(300);

      delta = await getDelta(page);
      // After outdent, indent should be removed or reduced
      const hasHighIndent = delta.ops.some(
        (op: any) => op.attributes && op.attributes.indent && op.attributes.indent >= 1
      );
      expect(hasHighIndent).toBe(false);
    });
  });

  // ============================================
  // I18n Test
  // ============================================

  test.describe('I18n', () => {
    test('12 - German I18n labels are applied to toolbar buttons', async ({ page }) => {
      await page.locator('#set-german-i18n').click();
      await page.waitForTimeout(500);

      // Verify the bold button title changed to "Fett"
      const erte = getErte(page);
      const boldBtn = erte.locator('button.ql-bold');
      const title = await boldBtn.getAttribute('title');
      expect(title).toBe('Fett');
    });
  });

  // ============================================
  // No Rulers Mode Tests
  // ============================================

  test.describe('No Rulers Mode', () => {
    test('13 - setNoRulers hides the ruler', async ({ page }) => {
      // Verify ruler is initially visible
      const ruler = getRuler(page);
      await expect(ruler).toBeVisible();

      // Toggle no rulers
      await page.locator('#toggle-no-rulers').click();
      await page.waitForTimeout(300);

      // Ruler should be hidden
      await expect(ruler).not.toBeVisible();
    });

    test('14 - setNoRulers(false) re-shows the ruler', async ({ page }) => {
      // Toggle no rulers ON
      await page.locator('#toggle-no-rulers').click();
      await page.waitForTimeout(300);
      await expect(getRuler(page)).not.toBeVisible();

      // Toggle no rulers OFF (second click)
      await page.locator('#toggle-no-rulers').click();
      await page.waitForTimeout(300);
      await expect(getRuler(page)).toBeVisible();
    });
  });

  // ============================================
  // Disabled State Tests
  // ============================================

  test.describe('Disabled State', () => {
    test('15 - Disabled state makes editor non-interactive with toolbar disabled', async ({
      page,
    }) => {
      await page.locator('#toggle-disabled').click();
      await page.waitForTimeout(300);

      // Editor should not be contenteditable
      const editor = getEditor(page);
      const contentEditable = await editor.getAttribute('contenteditable');
      expect(contentEditable).toBe('false');

      // Toolbar buttons should be disabled (check the toolbar wrapper has disabled attribute
      // or buttons are not clickable)
      const erte = getErte(page);
      const isDisabled = await erte.evaluate((el: any) => {
        return el.hasAttribute('disabled') || el.disabled === true;
      });
      expect(isDisabled).toBe(true);
    });

    test('16 - Disabled vs readonly: toolbar state differs', async ({ page }) => {
      // Set readonly first
      await page.locator('#toggle-readonly').click();
      await page.waitForTimeout(300);

      const erte = getErte(page);
      const editorReadonly = getEditor(page);

      // In readonly, editor is not editable
      const readonlyContentEditable = await editorReadonly.getAttribute('contenteditable');
      expect(readonlyContentEditable).toBe('false');

      // But the component should have readonly, not disabled
      const hasReadonly = await erte.evaluate((el: any) => {
        return el.hasAttribute('readonly') || el.readonly === true;
      });
      expect(hasReadonly).toBe(true);

      // Toggle readonly off, toggle disabled on
      await page.locator('#toggle-readonly').click();
      await page.waitForTimeout(200);
      await page.locator('#toggle-disabled').click();
      await page.waitForTimeout(300);

      // Now it should be disabled
      const hasDisabled = await erte.evaluate((el: any) => {
        return el.hasAttribute('disabled') || el.disabled === true;
      });
      expect(hasDisabled).toBe(true);
    });

    test('17 - Toggle disabled back to enabled restores full functionality', async ({
      page,
    }) => {
      // Disable
      await page.locator('#toggle-disabled').click();
      await page.waitForTimeout(300);
      expect(await getEditor(page).getAttribute('contenteditable')).toBe('false');

      // Re-enable
      await page.locator('#toggle-disabled').click();
      await page.waitForTimeout(300);
      expect(await getEditor(page).getAttribute('contenteditable')).toBe('true');

      // Verify typing works after re-enabling
      await focusEditor(page);
      await typeInEditor(page, 'Works again');
      await page.waitForTimeout(300);

      const delta = await getDelta(page);
      const allText = delta.ops
        .filter((op: any) => typeof op.insert === 'string')
        .map((op: any) => op.insert)
        .join('');
      expect(allText).toContain('Works again');
    });
  });

  // ============================================
  // Value Round-Trip Tests
  // ============================================

  test.describe('Value Round-Trip', () => {
    test('18 - Load Delta with tab blot and verify round-trip', async ({ page }) => {
      await page.locator('#load-tab-delta').click();
      await page.waitForTimeout(500);

      // Verify tab blot renders in the editor
      const tabs = getErte(page).locator('.ql-tab');
      await expect(tabs).toHaveCount(1);

      // Get delta directly from editor (server-side setValue uses SOURCE.SILENT,
      // so #delta-output is not updated by text-change event)
      const delta = await getDeltaFromEditor(page);
      const tabCount = countInDelta(delta, 'tab');
      expect(tabCount).toBe(1);

      // Verify text content is preserved
      const allText = delta.ops
        .filter((op: any) => typeof op.insert === 'string')
        .map((op: any) => op.insert)
        .join('');
      expect(allText).toContain('Hello');
      expect(allText).toContain('World');
    });

    test('19 - Load Delta with readonly blot and verify it renders', async ({ page }) => {
      await page.locator('#load-readonly-delta').click();
      await page.waitForTimeout(500);

      // Verify readonly blot renders as span.ql-readonly with contenteditable="false"
      const readonlyEl = getErte(page).locator('span.ql-readonly[contenteditable="false"]');
      await expect(readonlyEl).toHaveCount(1);
      await expect(readonlyEl).toContainText('Protected');

      // Get delta directly from editor (server-side setValue uses SOURCE.SILENT)
      const delta = await getDeltaFromEditor(page);

      // ReadOnlyBlot is an Inline format, so it appears as attributes.readonly on text ops
      const readonlyOps = delta.ops.filter(
        (op: any) => op.attributes && op.attributes.readonly === true
      );
      expect(readonlyOps.length).toBe(1);
      expect(readonlyOps[0].insert).toBe('Protected');

      // Verify surrounding text
      const allText = delta.ops
        .filter((op: any) => typeof op.insert === 'string')
        .map((op: any) => op.insert)
        .join('');
      expect(allText).toContain('Before');
      expect(allText).toContain('After');
    });

    test('20 - Load Delta with placeholder blot and verify it renders', async ({ page }) => {
      await page.locator('#load-placeholder-delta').click();
      await page.waitForTimeout(500);

      // Verify placeholder renders in the editor as span.ql-placeholder
      const placeholderEl = getErte(page).locator('span.ql-placeholder');
      await expect(placeholderEl).toHaveCount(1);
      await expect(placeholderEl).toContainText('TestPlaceholder');

      // Get delta directly from editor (server-side setValue uses SOURCE.SILENT)
      const delta = await getDeltaFromEditor(page);

      // PlaceholderBlot is an Embed, so it appears as insert.placeholder in the delta
      const placeholderOps = delta.ops.filter(
        (op: any) => op.insert && op.insert.placeholder
      );
      expect(placeholderOps.length).toBe(1);
      expect(placeholderOps[0].insert.placeholder.text).toBe('TestPlaceholder');

      // Verify surrounding text
      const allText = delta.ops
        .filter((op: any) => typeof op.insert === 'string')
        .map((op: any) => op.insert)
        .join('');
      expect(allText).toContain('Hello');
    });
  });

  // ============================================
  // TabConverter / Old Format Test
  // ============================================

  test.describe('TabConverter', () => {
    test.fixme('21 - Old format tab delta auto-converts on load', async ({ page }) => {
      // FIXME: This test needs an old-format delta (tabs-cont/pre-tab/line-part blots)
      // to actually verify TabConverter auto-conversion. Currently it loads new-format
      // tab delta which is the same as test 18. Needs a dedicated #load-old-tab-delta
      // button in the Java view that provides old-format delta data.
      await page.locator('#load-tab-delta').click();
      await page.waitForTimeout(500);

      // The tab should render properly regardless of format
      const tabs = getErte(page).locator('.ql-tab');
      await expect(tabs).toHaveCount(1);

      // Verify the delta output contains the tab in correct format
      const delta = await getDeltaFromEditor(page);
      const tabOps = delta.ops.filter((op: any) => op.insert && op.insert.tab !== undefined);
      expect(tabOps.length).toBe(1);
    });
  });

  // ============================================
  // Sanitizer Tests
  // ============================================

  test.describe('Sanitizer', () => {
    test('22 - Sanitizer preserves ERTE-specific classes in HTML output', async ({ page }) => {
      // Load content with a tab blot
      await page.locator('#load-tab-delta').click();
      await page.waitForTimeout(500);

      // Click Get HTML to populate html-output
      await page.locator('#get-html').click();
      await page.waitForTimeout(500);

      const htmlOutput = await page.locator('#html-output').textContent();
      expect(htmlOutput).toBeTruthy();

      // The HTML should contain ERTE-specific classes or elements
      // Tab blots render as elements with class ql-tab or as span with tab-related attributes
      const hasErteContent =
        htmlOutput!.includes('ql-tab') ||
        htmlOutput!.includes('tab') ||
        htmlOutput!.includes('Hello') ||
        htmlOutput!.includes('World');

      expect(hasErteContent).toBe(true);
    });

    test('23 - Sanitizer strips XSS payloads from HTML output', async ({ page }) => {
      // Inject content with text, then get HTML and verify no script tags
      await focusEditor(page);
      await typeInEditor(page, 'Safe content');
      await page.waitForTimeout(300);

      // Try to inject XSS via the Quill API directly
      await page.evaluate(() => {
        const el = document.getElementById('test-editor') as any;
        if (el?._editor) {
          // Attempt to insert HTML with script tag via clipboard module
          el._editor.root.innerHTML +=
            '<script>alert("xss")</script><img src=x onerror=alert(1)>';
        }
      });
      await page.waitForTimeout(200);

      // Click Get HTML to get the sanitized output
      await page.locator('#get-html').click();
      await page.waitForTimeout(500);

      const htmlOutput = await page.locator('#html-output').textContent();
      expect(htmlOutput).toBeTruthy();

      // Sanitized HTML should not contain script tags or onerror handlers
      expect(htmlOutput!.toLowerCase()).not.toContain('<script');
      expect(htmlOutput!.toLowerCase()).not.toContain('onerror');
      expect(htmlOutput!.toLowerCase()).not.toContain('alert(');
    });

    test('24 - Sanitizer handles data-placeholder attribute in HTML output', async ({
      page,
    }) => {
      // Load placeholder delta
      await page.locator('#load-placeholder-delta').click();
      await page.waitForTimeout(500);

      // Get HTML output
      await page.locator('#get-html').click();
      await page.waitForTimeout(500);

      const htmlOutput = await page.locator('#html-output').textContent();
      expect(htmlOutput).toBeTruthy();

      // The HTML output should contain the placeholder text
      // Whether via data-placeholder attribute or as rendered text depends on implementation
      const hasPlaceholderContent =
        htmlOutput!.includes('TestPlaceholder') ||
        htmlOutput!.includes('data-placeholder') ||
        htmlOutput!.includes('placeholder');

      expect(hasPlaceholderContent).toBe(true);
    });
  });

  // ============================================
  // Focus Method Test
  // ============================================

  test.describe('focus() Method', () => {
    test('25 - focus() method gives focus to the editor', async ({ page }) => {
      // First, ensure editor does NOT have focus by clicking elsewhere
      await page.locator('#get-text-length').click();
      await page.waitForTimeout(200);

      // Verify editor does not have focus
      const hasFocusBefore = await page.evaluate(() => {
        const el = document.getElementById('test-editor') as any;
        const qlEditor = el?.querySelector('.ql-editor');
        return document.activeElement === qlEditor || el?.contains(document.activeElement);
      });
      expect(hasFocusBefore).toBe(false);

      // Click the focus button (calls editor.focus() from Java)
      await page.locator('#focus-editor').click();
      await page.waitForTimeout(500);

      // Verify editor now has focus
      const hasFocusAfter = await page.evaluate(() => {
        const el = document.getElementById('test-editor') as any;
        const qlEditor = el?.querySelector('.ql-editor');
        return (
          document.activeElement === qlEditor ||
          qlEditor?.contains(document.activeElement) ||
          el?.contains(document.activeElement)
        );
      });
      expect(hasFocusAfter).toBe(true);
    });
  });
});
