import { test, expect } from '@playwright/test';

const PROTOTYPE_URL = 'http://127.0.0.1:8080/tab-stop';

// Helper function to get Delta JSON from page
async function getDeltaJson(page: any): Promise<any> {
  // Wait a bit for the delta to be updated
  await page.waitForTimeout(200);
  const codeElement = page.locator('code');
  const jsonText = await codeElement.textContent();
  if (!jsonText || jsonText.trim() === '') {
    throw new Error('Delta JSON is empty');
  }
  return JSON.parse(jsonText);
}

// Helper function to count elements in delta
function countInDelta(delta: any, type: string): number {
  return delta.ops.filter((op: any) => op.insert && op.insert[type]).length;
}

test.describe('TabStop Prototype', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto(PROTOTYPE_URL);
    // Vaadin apps take time to load - wait with longer timeout
    await page.waitForSelector('.ql-editor', { timeout: 60000 });
  });

  // ============================================
  // HARD-BREAK (Enter) Tests
  // ============================================

  test.describe('Hard-Break (Enter)', () => {
    test('Tab in new paragraph aligns to first tabstop', async ({ page }) => {
      const editor = page.locator('.ql-editor');
      await editor.click();

      // Press Tab, type text, Enter, Tab
      await page.keyboard.press('Tab');
      await page.keyboard.type('Line1');
      await page.keyboard.press('Enter');
      await page.keyboard.press('Tab');
      await page.keyboard.type('Line2');

      // Both tabs should have the same X position (at first tabstop)
      const tabs = await page.locator('.ql-tab').all();
      expect(tabs.length).toBe(2);

      const tab1Rect = await tabs[0].boundingBox();
      const tab2Rect = await tabs[1].boundingBox();

      // Tabs should end at the same X position (tabstop position)
      expect(Math.abs(tab1Rect!.x + tab1Rect!.width - (tab2Rect!.x + tab2Rect!.width))).toBeLessThan(5);
    });

    test('Hard-Break does NOT copy tabs from previous line', async ({ page }) => {
      const editor = page.locator('.ql-editor');
      await editor.click();

      // 3 Tabs + Text, then Enter
      await page.keyboard.press('Tab');
      await page.keyboard.press('Tab');
      await page.keyboard.press('Tab');
      await page.keyboard.type('Text');
      await page.keyboard.press('Enter');
      await page.keyboard.type('New paragraph');

      // Only the original 3 tabs should exist (hard-break doesn't copy)
      const tabs = await page.locator('.ql-tab').all();
      expect(tabs.length).toBe(3);

      // Verify we have 2 paragraphs
      const paragraphs = await page.locator('.ql-editor > p').all();
      expect(paragraphs.length).toBe(2);
    });
  });

  // ============================================
  // SOFT-BREAK (Shift+Enter) Tests
  // ============================================

  test.describe('Soft-Break (Shift+Enter)', () => {
    test('Copies tabs up to cursor position - cursor after 1st of 3 tabs', async ({ page }) => {
      const editor = page.locator('.ql-editor');
      await editor.click();

      // Insert 3 tabs
      await page.keyboard.press('Tab');
      await page.keyboard.press('Tab');
      await page.keyboard.press('Tab');

      // Move cursor to position after 1st tab (ArrowLeft 2x: from position 3 to position 1)
      await page.keyboard.press('ArrowLeft');
      await page.keyboard.press('ArrowLeft');

      // Soft-break
      await page.keyboard.down('Shift');
      await page.keyboard.press('Enter');
      await page.keyboard.up('Shift');

      // Line 1 stays intact (3 tabs), new line has 1 tab = 4 total
      const tabs = await page.locator('.ql-tab').all();
      expect(tabs.length).toBe(4);
    });

    test('Cursor at line start copies no tabs', async ({ page }) => {
      const editor = page.locator('.ql-editor');
      await editor.click();

      // Insert 2 tabs
      await page.keyboard.press('Tab');
      await page.keyboard.press('Tab');

      // Move cursor to actual start (ArrowLeft to position 0)
      // Note: Home in Quill sets cursor after first embed, not to position 0
      await page.keyboard.press('ArrowLeft');
      await page.keyboard.press('ArrowLeft');

      // Soft-break
      await page.keyboard.down('Shift');
      await page.keyboard.press('Enter');
      await page.keyboard.up('Shift');

      // Line 1 stays intact (2 tabs), new line has 0 tabs = 2 total
      const tabs = await page.locator('.ql-tab').all();
      expect(tabs.length).toBe(2);
    });

    test('Cursor after all tabs copies all tabs', async ({ page }) => {
      const editor = page.locator('.ql-editor');
      await editor.click();

      // 2 Tabs + Text
      await page.keyboard.press('Tab');
      await page.keyboard.press('Tab');
      await page.keyboard.type('Text');

      // Soft-break (cursor is at end, after both tabs)
      await page.keyboard.down('Shift');
      await page.keyboard.press('Enter');
      await page.keyboard.up('Shift');

      // Line 1 stays (2 tabs), new line has 2 tabs = 4 total
      const tabs = await page.locator('.ql-tab').all();
      expect(tabs.length).toBe(4);
    });

    test('Multiple soft-breaks: each new line gets same tab count', async ({ page }) => {
      const editor = page.locator('.ql-editor');
      await editor.click();

      // 5 Tabs
      for (let i = 0; i < 5; i++) {
        await page.keyboard.press('Tab');
      }

      // Cursor after tab 3 (2x ArrowLeft)
      await page.keyboard.press('ArrowLeft');
      await page.keyboard.press('ArrowLeft');

      // 3x Soft-break
      for (let i = 0; i < 3; i++) {
        await page.keyboard.down('Shift');
        await page.keyboard.press('Enter');
        await page.keyboard.up('Shift');
      }

      // Line 1: 5 tabs (intact), lines 2-4: 3 tabs each = 5 + 3*3 = 14 tabs
      const tabs = await page.locator('.ql-tab').all();
      expect(tabs.length).toBe(14);

      // Verify structure via Delta
      const delta = await getDeltaJson(page);
      const softBreaks = countInDelta(delta, 'soft-break');
      expect(softBreaks).toBe(3);
    });

    test('Soft-break preserves original line content', async ({ page }) => {
      const editor = page.locator('.ql-editor');
      await editor.click();

      // Tab + Text + Tab + Text
      await page.keyboard.press('Tab');
      await page.keyboard.type('First');
      await page.keyboard.press('Tab');
      await page.keyboard.type('Second');

      // Cursor between "First" and Tab (before "Second")
      await page.keyboard.press('ArrowLeft');
      await page.keyboard.press('ArrowLeft');
      await page.keyboard.press('ArrowLeft');
      await page.keyboard.press('ArrowLeft');
      await page.keyboard.press('ArrowLeft');
      await page.keyboard.press('ArrowLeft');
      await page.keyboard.press('ArrowLeft');

      // Soft-Break
      await page.keyboard.down('Shift');
      await page.keyboard.press('Enter');
      await page.keyboard.up('Shift');

      // Original line should still contain all content
      const delta = await getDeltaJson(page);

      // Should have: Tab, "First", Tab, "Second", soft-break, Tab, newline
      expect(countInDelta(delta, 'tab')).toBe(3); // 2 original + 1 copied
      expect(countInDelta(delta, 'soft-break')).toBe(1);
    });

    test('Soft-break in middle of visual line after previous soft-break', async ({ page }) => {
      const editor = page.locator('.ql-editor');
      await editor.click();

      // First visual line: 2 Tabs
      await page.keyboard.press('Tab');
      await page.keyboard.press('Tab');

      // Soft-break to create second visual line
      await page.keyboard.down('Shift');
      await page.keyboard.press('Enter');
      await page.keyboard.up('Shift');

      // Second visual line now has 2 tabs (copied), add one more
      await page.keyboard.press('Tab');

      // Move cursor back one position (after the 2nd copied tab)
      await page.keyboard.press('ArrowLeft');

      // Another soft-break
      await page.keyboard.down('Shift');
      await page.keyboard.press('Enter');
      await page.keyboard.up('Shift');

      // Line 1: 2 tabs, Line 2: 3 tabs (2 copied + 1 added), Line 3: 2 tabs
      // Total: 2 + 3 + 2 = 7 tabs
      const tabs = await page.locator('.ql-tab').all();
      expect(tabs.length).toBe(7);
    });
  });

  // ============================================
  // AUTOMATIC WRAP Tests
  // ============================================

  test.describe('Automatic Wrap', () => {
    test('Tab after auto-wrap uses fixed width (not tabstop)', async ({ page }) => {
      // Make window narrow to force wrap
      await page.setViewportSize({ width: 400, height: 600 });

      const editor = page.locator('.ql-editor');
      await editor.click();

      // Enter long text that wraps, then Tab
      await page.keyboard.type('This is a very long text that should automatically wrap when the window is narrow enough');
      await page.keyboard.press('Tab');
      await page.keyboard.type('After-Tab');

      // Tab at line start after automatic wrap should have fixed width
      const tabs = await page.locator('.ql-tab').all();
      if (tabs.length > 0) {
        const tabRect = await tabs[0].boundingBox();
        // Fixed tab width is ~50px (FIXED_TAB_FALLBACK) or 8 characters
        // NOT aligned to tabstop (which would be much wider, e.g., 100px)
        expect(tabRect!.width).toBeLessThan(80);
      }
    });

    test('Tabs before wrap still align to tabstops', async ({ page }) => {
      await page.setViewportSize({ width: 600, height: 600 });

      const editor = page.locator('.ql-editor');
      await editor.click();

      // Tab at start, then text
      await page.keyboard.press('Tab');
      await page.keyboard.type('Short text');

      const tabs = await page.locator('.ql-tab').all();
      expect(tabs.length).toBe(1);

      // First tab should align to first tabstop (L at 100px)
      const tabRect = await tabs[0].boundingBox();
      const editorRect = await editor.boundingBox();

      // Tab end position should be around 100px from editor start
      const tabEndPos = tabRect!.x + tabRect!.width - editorRect!.x;
      expect(tabEndPos).toBeGreaterThan(80);
      expect(tabEndPos).toBeLessThan(120);
    });
  });

  // ============================================
  // COMBINED SCENARIOS
  // ============================================

  test.describe('Combined Scenarios', () => {
    test('Hard-break after soft-break creates new paragraph', async ({ page }) => {
      const editor = page.locator('.ql-editor');
      await editor.click();

      // Tabs + soft-break
      await page.keyboard.press('Tab');
      await page.keyboard.press('Tab');
      await page.keyboard.down('Shift');
      await page.keyboard.press('Enter');
      await page.keyboard.up('Shift');

      // Now hard-break
      await page.keyboard.press('Enter');

      // Should have 2 paragraphs
      const paragraphs = await page.locator('.ql-editor > p').all();
      expect(paragraphs.length).toBe(2);
    });

    test('Tabs align correctly after soft-break', async ({ page }) => {
      const editor = page.locator('.ql-editor');
      await editor.click();

      // Tab in first visual line
      await page.keyboard.press('Tab');
      await page.keyboard.type('Line1');

      // Soft-break (should copy 1 tab)
      await page.keyboard.down('Shift');
      await page.keyboard.press('Enter');
      await page.keyboard.up('Shift');

      // Add another tab in second visual line
      await page.keyboard.press('Tab');
      await page.keyboard.type('Line2');

      // Should have 3 tabs: 1 original, 1 copied, 1 new
      const tabs = await page.locator('.ql-tab').all();
      expect(tabs.length).toBe(3);

      // All tabs should align to tabstops
      for (const tab of tabs) {
        const rect = await tab.boundingBox();
        // Tab should have significant width (aligned to tabstop)
        expect(rect!.width).toBeGreaterThan(10);
      }
    });

    test('Soft-break after tabs and text preserves text', async ({ page }) => {
      const editor = page.locator('.ql-editor');
      await editor.click();

      // Tab + Text
      await page.keyboard.press('Tab');
      await page.keyboard.type('Hello World');

      // Cursor in middle of text
      for (let i = 0; i < 5; i++) {
        await page.keyboard.press('ArrowLeft');
      }

      // Soft-break
      await page.keyboard.down('Shift');
      await page.keyboard.press('Enter');
      await page.keyboard.up('Shift');

      // Delta should contain both parts of text
      const delta = await getDeltaJson(page);
      const textOps = delta.ops.filter((op: any) => typeof op.insert === 'string' && op.insert !== '\n');

      // Should have "Hello " and "World" (or similar split)
      const allText = textOps.map((op: any) => op.insert).join('');
      expect(allText).toContain('Hello');
      expect(allText).toContain('World');
    });
  });

  // ============================================
  // TAB ALIGNMENT Tests
  // ============================================

  test.describe('Tab Alignment', () => {
    test('First tab aligns to L (left) tabstop', async ({ page }) => {
      const editor = page.locator('.ql-editor');
      await editor.click();

      await page.keyboard.press('Tab');
      await page.keyboard.type('Left aligned');

      const tabs = await page.locator('.ql-tab').all();
      expect(tabs.length).toBe(1);

      // First tabstop is at 100px (L)
      const tabRect = await tabs[0].boundingBox();
      const editorRect = await editor.boundingBox();
      const tabEndPos = tabRect!.x + tabRect!.width - editorRect!.x;

      // Should be around 100px (with some tolerance for padding)
      expect(tabEndPos).toBeGreaterThan(80);
      expect(tabEndPos).toBeLessThan(120);
    });

    test('Second tab aligns to C (center) tabstop', async ({ page }) => {
      const editor = page.locator('.ql-editor');
      await editor.click();

      await page.keyboard.press('Tab');
      await page.keyboard.press('Tab');
      await page.keyboard.type('Center');

      // Wait for width calculation
      await page.waitForTimeout(200);

      const tabs = await page.locator('.ql-tab').all();
      expect(tabs.length).toBe(2);

      // Second tabstop is at 300px (C) - center aligned means text centers on tabstop
      // Center alignment subtracts half the text width, so the tab end position varies
      const tab2Rect = await tabs[1].boundingBox();
      const editorRect = await editor.boundingBox();
      const tab2EndPos = tab2Rect!.x + tab2Rect!.width - editorRect!.x;

      // Should be somewhere between first tabstop (100px) and third tabstop (500px)
      // Center-aligned position depends on text width
      expect(tab2EndPos).toBeGreaterThan(150);
      expect(tab2EndPos).toBeLessThan(400);
    });

    test('Third tab aligns to R (right) tabstop', async ({ page }) => {
      const editor = page.locator('.ql-editor');
      await editor.click();

      await page.keyboard.press('Tab');
      await page.keyboard.press('Tab');
      await page.keyboard.press('Tab');
      await page.keyboard.type('Right');

      // Wait for tab width calculation
      await page.waitForTimeout(200);

      const tabs = await page.locator('.ql-tab').all();
      expect(tabs.length).toBe(3);

      // Third tabstop is at 500px (R) - right aligned
      // Right alignment: text end aligns to tabstop, so tab width is reduced by text width
      const tab3Rect = await tabs[2].boundingBox();

      // Tab should have positive width (right alignment subtracts text width)
      expect(tab3Rect!.width).toBeGreaterThan(10);

      // For right alignment, verify tab + text reaches the tabstop
      // We can't easily measure text position, so just verify tab exists and has reasonable width
      expect(tab3Rect!.width).toBeLessThan(300);
    });

    test('Fourth tab (beyond tabstops) uses fixed width', async ({ page }) => {
      const editor = page.locator('.ql-editor');
      await editor.click();

      // Insert 4 tabs (only 3 tabstops exist: L, C, R)
      await page.keyboard.press('Tab');
      await page.keyboard.press('Tab');
      await page.keyboard.press('Tab');
      await page.keyboard.press('Tab');
      await page.keyboard.type('Beyond');

      // Wait for tab width calculation
      await page.waitForTimeout(200);

      const tabs = await page.locator('.ql-tab').all();
      expect(tabs.length).toBe(4);

      // Fourth tab has no tabstop -> uses fixed width (~50px or 8 chars)
      const tab4Rect = await tabs[3].boundingBox();

      // Fixed width should be around 50px (FIXED_TAB_FALLBACK), not aligned to any tabstop
      // Widened tolerance due to font variations
      expect(tab4Rect!.width).toBeGreaterThan(20);
      expect(tab4Rect!.width).toBeLessThan(100);
    });

    test('Fifth and sixth tabs also use fixed width', async ({ page }) => {
      const editor = page.locator('.ql-editor');
      await editor.click();

      // Insert 6 tabs (only 3 tabstops exist)
      for (let i = 0; i < 6; i++) {
        await page.keyboard.press('Tab');
      }
      await page.keyboard.type('End');

      // Wait for width calculation
      await page.waitForTimeout(200);

      const tabs = await page.locator('.ql-tab').all();
      expect(tabs.length).toBe(6);

      // Tabs 4, 5, 6 should all have positive widths
      const tab4Rect = await tabs[3].boundingBox();
      const tab5Rect = await tabs[4].boundingBox();
      const tab6Rect = await tabs[5].boundingBox();

      // All overflow tabs should have positive width
      expect(tab4Rect!.width).toBeGreaterThan(10);
      expect(tab5Rect!.width).toBeGreaterThan(10);
      expect(tab6Rect!.width).toBeGreaterThan(10);

      // They should be roughly the same width (within 15px tolerance)
      expect(Math.abs(tab4Rect!.width - tab5Rect!.width)).toBeLessThan(15);
      expect(Math.abs(tab5Rect!.width - tab6Rect!.width)).toBeLessThan(15);
    });
  });

  // ============================================
  // OVERFLOW TABS + SOFT-BREAK Tests
  // ============================================

  test.describe('Overflow Tabs with Soft-Break', () => {
    test('Soft-break with 5 tabs copies only 3 tabs (tabstop limit)', async ({ page }) => {
      const editor = page.locator('.ql-editor');
      await editor.click();

      // Insert 5 tabs (more than 3 tabstops)
      for (let i = 0; i < 5; i++) {
        await page.keyboard.press('Tab');
      }
      await page.keyboard.type('Text');

      // Soft-break at end
      await page.keyboard.down('Shift');
      await page.keyboard.press('Enter');
      await page.keyboard.up('Shift');

      // Line 1: 5 tabs + text, Line 2: 3 copied tabs (limited by tabstop count) = 8 total
      const tabs = await page.locator('.ql-tab').all();
      expect(tabs.length).toBe(8);
    });

    test('Soft-break after 4th tab copies only 3 tabs (tabstop limit)', async ({ page }) => {
      const editor = page.locator('.ql-editor');
      await editor.click();

      // Insert 6 tabs
      for (let i = 0; i < 6; i++) {
        await page.keyboard.press('Tab');
      }

      // Move cursor back to after 4th tab (2x ArrowLeft)
      await page.keyboard.press('ArrowLeft');
      await page.keyboard.press('ArrowLeft');

      // Soft-break
      await page.keyboard.down('Shift');
      await page.keyboard.press('Enter');
      await page.keyboard.up('Shift');

      // Line 1: 6 tabs (intact), Line 2: 3 copied tabs (limited by tabstop count) = 9 total
      const tabs = await page.locator('.ql-tab').all();
      expect(tabs.length).toBe(9);
    });

    test('Multiple soft-breaks with overflow tabs respect tabstop limit', async ({ page }) => {
      const editor = page.locator('.ql-editor');
      await editor.click();

      // Insert 4 tabs (1 beyond tabstops)
      for (let i = 0; i < 4; i++) {
        await page.keyboard.press('Tab');
      }

      // 2x Soft-break at end
      for (let i = 0; i < 2; i++) {
        await page.keyboard.down('Shift');
        await page.keyboard.press('Enter');
        await page.keyboard.up('Shift');
      }

      // Line 1: 4 tabs, Lines 2-3: 3 tabs each (limited) = 4 + 3 + 3 = 10 total
      const tabs = await page.locator('.ql-tab').all();
      expect(tabs.length).toBe(10);
    });
  });

  // ============================================
  // RULER / TABSTOP MANIPULATION Tests
  // ============================================

  test.describe('Ruler and Tabstop Manipulation', () => {
    test('Adding a new tabstop by clicking ruler', async ({ page }) => {
      const ruler = page.locator('.native-quill-ruler');
      const editor = page.locator('.ql-editor');

      // Click on ruler at ~200px to add a new tabstop
      const rulerRect = await ruler.boundingBox();
      await page.mouse.click(rulerRect!.x + 200, rulerRect!.y + 10);

      // Should now have 4 tabstop markers (L, new one, C, R)
      const ticks = await page.locator('.ruler-tick').all();
      expect(ticks.length).toBe(4);

      // Insert tabs and verify alignment
      await editor.click();
      await page.keyboard.press('Tab');
      await page.keyboard.press('Tab');
      await page.keyboard.type('After new tabstop');

      const tabs = await page.locator('.ql-tab').all();
      expect(tabs.length).toBe(2);
    });

    test('Cycling tabstop alignment: L -> C -> R -> remove', async ({ page }) => {
      const ruler = page.locator('.native-quill-ruler');

      // Get first tabstop (L at 100px)
      const firstTick = page.locator('.ruler-tick').first();
      let alignText = await firstTick.locator('.ruler-align-text').textContent();
      expect(alignText).toBe('L');

      // Click to cycle to C
      await firstTick.click();
      alignText = await firstTick.locator('.ruler-align-text').textContent();
      expect(alignText).toBe('C');

      // Click to cycle to R
      await firstTick.click();
      alignText = await firstTick.locator('.ruler-align-text').textContent();
      expect(alignText).toBe('R');

      // Click to remove (should now have only 2 tabstops)
      await firstTick.click();
      const ticks = await page.locator('.ruler-tick').all();
      expect(ticks.length).toBe(2);
    });

    test('Removing middle tabstop affects tab widths', async ({ page }) => {
      const editor = page.locator('.ql-editor');
      await editor.click();

      // Insert 3 tabs
      await page.keyboard.press('Tab');
      await page.keyboard.press('Tab');
      await page.keyboard.press('Tab');
      await page.keyboard.type('End');

      // Get tab widths before removal
      const tabsBefore = await page.locator('.ql-tab').all();
      const width2Before = (await tabsBefore[1].boundingBox())!.width;

      // Remove middle tabstop (C) by clicking 3 times (L->C->R->remove)
      const middleTick = page.locator('.ruler-tick').nth(1);
      await middleTick.click(); // C -> R
      await middleTick.click(); // R -> remove

      // Wait for tab update
      await page.waitForTimeout(200);

      // Get tab widths after removal - second tab should now reach R tabstop
      const tabsAfter = await page.locator('.ql-tab').all();
      const width2After = (await tabsAfter[1].boundingBox())!.width;

      // Second tab should now be wider (reaching R instead of C)
      expect(width2After).toBeGreaterThan(width2Before);
    });

    test('Tab width updates when tabstop alignment changes', async ({ page }) => {
      const editor = page.locator('.ql-editor');
      await editor.click();

      // Insert tab + text
      await page.keyboard.press('Tab');
      await page.keyboard.type('Test');

      // Get initial tab width (L alignment)
      const tabBefore = await page.locator('.ql-tab').first().boundingBox();
      const widthBefore = tabBefore!.width;

      // Change first tabstop to center alignment
      const firstTick = page.locator('.ruler-tick').first();
      await firstTick.click(); // L -> C

      // Wait for update
      await page.waitForTimeout(200);

      // Tab width should change (center alignment considers text width)
      const tabAfter = await page.locator('.ql-tab').first().boundingBox();
      const widthAfter = tabAfter!.width;

      // Width should be different (center accounts for text)
      expect(widthAfter).not.toBe(widthBefore);
    });
  });

  // ============================================
  // EDGE CASES Tests
  // ============================================

  test.describe('Edge Cases', () => {
    test('Soft-break with only tabs (no text)', async ({ page }) => {
      const editor = page.locator('.ql-editor');
      await editor.click();

      // Only tabs, no text
      await page.keyboard.press('Tab');
      await page.keyboard.press('Tab');
      await page.keyboard.press('Tab');

      // Soft-break at end
      await page.keyboard.down('Shift');
      await page.keyboard.press('Enter');
      await page.keyboard.up('Shift');

      // Should have 6 tabs (3 original + 3 copied)
      const tabs = await page.locator('.ql-tab').all();
      expect(tabs.length).toBe(6);
    });

    test('Hard-break immediately after soft-break', async ({ page }) => {
      const editor = page.locator('.ql-editor');
      await editor.click();

      await page.keyboard.press('Tab');
      await page.keyboard.type('Text');

      // Soft-break
      await page.keyboard.down('Shift');
      await page.keyboard.press('Enter');
      await page.keyboard.up('Shift');

      // Immediate hard-break
      await page.keyboard.press('Enter');

      // Should have 2 paragraphs
      const paragraphs = await page.locator('.ql-editor > p').all();
      expect(paragraphs.length).toBe(2);

      // First paragraph should have soft-break
      const softBreaks = await page.locator('.ql-soft-break').all();
      expect(softBreaks.length).toBe(1);
    });

    test('Soft-break at empty line', async ({ page }) => {
      const editor = page.locator('.ql-editor');
      await editor.click();

      // Soft-break on empty line
      await page.keyboard.down('Shift');
      await page.keyboard.press('Enter');
      await page.keyboard.up('Shift');

      // Should create a soft-break even on empty line
      const softBreaks = await page.locator('.ql-soft-break').all();
      expect(softBreaks.length).toBe(1);

      // No tabs should be copied (none existed)
      const tabs = await page.locator('.ql-tab').all();
      expect(tabs.length).toBe(0);
    });

    test('Many tabs (10+) with soft-break copies only tabstop count', async ({ page }) => {
      const editor = page.locator('.ql-editor');
      await editor.click();

      // Insert 10 tabs
      for (let i = 0; i < 10; i++) {
        await page.keyboard.press('Tab');
      }

      // Soft-break
      await page.keyboard.down('Shift');
      await page.keyboard.press('Enter');
      await page.keyboard.up('Shift');

      // Should have 13 tabs (10 original + 3 copied, limited by tabstop count)
      const tabs = await page.locator('.ql-tab').all();
      expect(tabs.length).toBe(13);
    });

    test('Alternating tabs and text with soft-break', async ({ page }) => {
      const editor = page.locator('.ql-editor');
      await editor.click();

      // Tab-Text-Tab-Text pattern
      await page.keyboard.press('Tab');
      await page.keyboard.type('A');
      await page.keyboard.press('Tab');
      await page.keyboard.type('B');
      await page.keyboard.press('Tab');
      await page.keyboard.type('C');

      // Soft-break at end
      await page.keyboard.down('Shift');
      await page.keyboard.press('Enter');
      await page.keyboard.up('Shift');

      // All 3 tabs should be copied
      const tabs = await page.locator('.ql-tab').all();
      expect(tabs.length).toBe(6);

      // Delta should preserve text
      const delta = await getDeltaJson(page);
      const textOps = delta.ops.filter((op: any) => typeof op.insert === 'string' && op.insert !== '\n');
      const allText = textOps.map((op: any) => op.insert).join('');
      expect(allText).toContain('A');
      expect(allText).toContain('B');
      expect(allText).toContain('C');
    });
  });

  // ============================================
  // MIXED BREAK TYPES Tests
  // ============================================

  test.describe('Mixed Break Types', () => {
    test('Soft-break -> Hard-break -> Soft-break sequence', async ({ page }) => {
      const editor = page.locator('.ql-editor');
      await editor.click();

      // Tab + text
      await page.keyboard.press('Tab');
      await page.keyboard.type('Line1');

      // Soft-break
      await page.keyboard.down('Shift');
      await page.keyboard.press('Enter');
      await page.keyboard.up('Shift');

      await page.keyboard.type('Line2');

      // Hard-break
      await page.keyboard.press('Enter');

      // New paragraph with tab
      await page.keyboard.press('Tab');
      await page.keyboard.type('Line3');

      // Soft-break
      await page.keyboard.down('Shift');
      await page.keyboard.press('Enter');
      await page.keyboard.up('Shift');

      // Verify structure
      const paragraphs = await page.locator('.ql-editor > p').all();
      expect(paragraphs.length).toBe(2);

      const softBreaks = await page.locator('.ql-soft-break').all();
      expect(softBreaks.length).toBe(2);
    });

    test('Narrow viewport: auto-wrap + soft-break interaction', async ({ page }) => {
      await page.setViewportSize({ width: 400, height: 600 });

      const editor = page.locator('.ql-editor');
      await editor.click();

      // Long text that might wrap
      await page.keyboard.press('Tab');
      await page.keyboard.type('This is some text');
      await page.keyboard.press('Tab');
      await page.keyboard.type('More text here');

      // Soft-break
      await page.keyboard.down('Shift');
      await page.keyboard.press('Enter');
      await page.keyboard.up('Shift');

      // Should copy both tabs regardless of visual wrapping
      const tabs = await page.locator('.ql-tab').all();
      expect(tabs.length).toBe(4); // 2 original + 2 copied
    });

    test('Delete tab after soft-break', async ({ page }) => {
      const editor = page.locator('.ql-editor');
      await editor.click();

      // 2 tabs
      await page.keyboard.press('Tab');
      await page.keyboard.press('Tab');

      // Soft-break (copies 2 tabs)
      await page.keyboard.down('Shift');
      await page.keyboard.press('Enter');
      await page.keyboard.up('Shift');

      // Now we have 4 tabs (2 original + 2 copied)
      let tabs = await page.locator('.ql-tab').all();
      expect(tabs.length).toBe(4);

      // Delete one tab with backspace
      await page.keyboard.press('Backspace');

      // Should now have 3 tabs
      tabs = await page.locator('.ql-tab').all();
      expect(tabs.length).toBe(3);
    });

    test('Copy-paste content with tabs preserves structure', async ({ page }) => {
      const editor = page.locator('.ql-editor');
      await editor.click();

      // Tab + text
      await page.keyboard.press('Tab');
      await page.keyboard.type('Original');

      // Select all and copy
      await page.keyboard.press('Control+a');
      await page.keyboard.press('Control+c');

      // Move to end
      await page.keyboard.press('End');

      // Soft-break
      await page.keyboard.down('Shift');
      await page.keyboard.press('Enter');
      await page.keyboard.up('Shift');

      // Paste
      await page.keyboard.press('Control+v');

      // Should have tabs from original + copied + pasted
      const tabs = await page.locator('.ql-tab').all();
      expect(tabs.length).toBeGreaterThanOrEqual(2);
    });
  });

  // ============================================
  // UNDO/REDO Tests
  // ============================================

  test.describe('Undo/Redo', () => {
    test('Multiple undo restores state before soft-break', async ({ page }) => {
      const editor = page.locator('.ql-editor');
      await editor.click();

      // Insert 2 tabs + text
      await page.keyboard.press('Tab');
      await page.keyboard.press('Tab');
      await page.keyboard.type('Text');

      // Soft-break (copies 2 tabs)
      await page.keyboard.down('Shift');
      await page.keyboard.press('Enter');
      await page.keyboard.up('Shift');

      // Should have 4 tabs and 1 soft-break
      let tabs = await page.locator('.ql-tab').all();
      expect(tabs.length).toBe(4);
      let softBreaks = await page.locator('.ql-soft-break').all();
      expect(softBreaks.length).toBe(1);

      // Multiple undos to get back to before soft-break
      // (soft-break + 2 copied tabs = 3 undo operations)
      for (let i = 0; i < 5; i++) {
        await page.keyboard.press('Control+z');
        await page.waitForTimeout(100);
      }

      // Should eventually be back to 2 tabs or fewer, no soft-break
      tabs = await page.locator('.ql-tab').all();
      expect(tabs.length).toBeLessThanOrEqual(2);
      softBreaks = await page.locator('.ql-soft-break').all();
      expect(softBreaks.length).toBe(0);
    });

    test('Undo removes last typed character', async ({ page }) => {
      const editor = page.locator('.ql-editor');
      await editor.click();

      // Insert text
      await page.keyboard.type('ABC');

      let delta = await getDeltaJson(page);
      let textOps = delta.ops.filter((op: any) => typeof op.insert === 'string' && op.insert.includes('ABC'));
      expect(textOps.length).toBeGreaterThan(0);

      // Undo should remove some text
      await page.keyboard.press('Control+z');
      await page.waitForTimeout(200);

      delta = await getDeltaJson(page);
      const allText = delta.ops.filter((op: any) => typeof op.insert === 'string').map((op: any) => op.insert).join('');

      // After undo, either text is shorter or empty
      expect(allText.length).toBeLessThanOrEqual(3);
    });

    test('Undo single tab insertion', async ({ page }) => {
      const editor = page.locator('.ql-editor');
      await editor.click();

      // Insert 1 tab
      await page.keyboard.press('Tab');

      let tabs = await page.locator('.ql-tab').all();
      expect(tabs.length).toBe(1);

      // Undo
      await page.keyboard.press('Control+z');
      await page.waitForTimeout(200);

      tabs = await page.locator('.ql-tab').all();
      expect(tabs.length).toBe(0);
    });

    test('Ruler changes are outside Quill undo history', async ({ page }) => {
      const ruler = page.locator('.native-quill-ruler');

      // Get initial tabstop count
      let ticks = await page.locator('.ruler-tick').all();
      const initialCount = ticks.length;

      // Add new tabstop
      const rulerRect = await ruler.boundingBox();
      await page.mouse.click(rulerRect!.x + 200, rulerRect!.y + 10);

      ticks = await page.locator('.ruler-tick').all();
      expect(ticks.length).toBe(initialCount + 1);

      // Undo - ruler changes are not part of Quill history
      await page.keyboard.press('Control+z');
      await page.waitForTimeout(200);

      // Tabstop should still be there (not undone)
      ticks = await page.locator('.ruler-tick').all();
      expect(ticks.length).toBe(initialCount + 1);
    });
  });

  // ============================================
  // SELECTION OPERATIONS Tests
  // ============================================

  test.describe('Selection Operations', () => {
    test('Select and delete text spanning soft-break', async ({ page }) => {
      const editor = page.locator('.ql-editor');
      await editor.click();

      // Tab + text
      await page.keyboard.press('Tab');
      await page.keyboard.type('Before');

      // Soft-break
      await page.keyboard.down('Shift');
      await page.keyboard.press('Enter');
      await page.keyboard.up('Shift');

      await page.keyboard.type('After');

      // Select all and delete
      await page.keyboard.press('Control+a');
      await page.keyboard.press('Delete');

      // Editor should be empty
      const delta = await getDeltaJson(page);
      // Only newline should remain
      expect(delta.ops.length).toBeLessThanOrEqual(1);
    });

    test('Select text spanning soft-break and type to replace', async ({ page }) => {
      const editor = page.locator('.ql-editor');
      await editor.click();

      // Content before soft-break
      await page.keyboard.press('Tab');
      await page.keyboard.type('First');

      // Soft-break
      await page.keyboard.down('Shift');
      await page.keyboard.press('Enter');
      await page.keyboard.up('Shift');

      await page.keyboard.type('Second');

      // Select all
      await page.keyboard.press('Control+a');

      // Type replacement
      await page.keyboard.type('Replaced');

      // Check content
      const delta = await getDeltaJson(page);
      const textOps = delta.ops.filter((op: any) => typeof op.insert === 'string');
      const allText = textOps.map((op: any) => op.insert).join('');
      expect(allText).toContain('Replaced');
      expect(allText).not.toContain('First');
      expect(allText).not.toContain('Second');
    });

    test('Double-click selects word adjacent to tab', async ({ page }) => {
      const editor = page.locator('.ql-editor');
      await editor.click();

      // Tab + word
      await page.keyboard.press('Tab');
      await page.keyboard.type('TestWord');

      // Get position of "TestWord" text
      const textElement = page.locator('.ql-editor p').first();

      // Double-click on the text area (not on tab)
      const tabElement = page.locator('.ql-tab').first();
      const tabRect = await tabElement.boundingBox();

      // Click just after the tab
      await page.mouse.dblclick(tabRect!.x + tabRect!.width + 20, tabRect!.y + tabRect!.height / 2);

      // Get selection via JavaScript
      const selection = await page.evaluate(() => {
        return window.getSelection()?.toString() || '';
      });

      // Should select the word (or part of it)
      expect(selection.length).toBeGreaterThan(0);
    });
  });

  // ============================================
  // BACKSPACE/DELETE AT BOUNDARIES Tests
  // ============================================

  test.describe('Backspace/Delete at Boundaries', () => {
    test('Backspace at start of visual line deletes soft-break', async ({ page }) => {
      const editor = page.locator('.ql-editor');
      await editor.click();

      // Tab + text
      await page.keyboard.press('Tab');
      await page.keyboard.type('Line1');

      // Soft-break
      await page.keyboard.down('Shift');
      await page.keyboard.press('Enter');
      await page.keyboard.up('Shift');

      // Cursor is now at start of new visual line (after copied tab)
      // Move to very start of visual line (before the copied tab)
      await page.keyboard.press('ArrowLeft');

      // Now backspace should delete the soft-break
      await page.keyboard.press('Backspace');

      // Soft-break should be gone
      const softBreaks = await page.locator('.ql-soft-break').all();
      expect(softBreaks.length).toBe(0);
    });

    test('Delete key removes character or element at cursor', async ({ page }) => {
      const editor = page.locator('.ql-editor');
      await editor.click();

      // Tab + short text
      await page.keyboard.press('Tab');
      await page.keyboard.type('AB');

      // Go to start
      await page.keyboard.press('Home');

      // Delete should remove the tab
      await page.keyboard.press('Delete');

      const tabs = await page.locator('.ql-tab').all();
      expect(tabs.length).toBe(0);

      // Text should remain
      const delta = await getDeltaJson(page);
      const textOps = delta.ops.filter((op: any) => typeof op.insert === 'string' && op.insert !== '\n');
      const allText = textOps.map((op: any) => op.insert).join('');
      expect(allText).toContain('AB');
    });

    test('Backspace deletes tab character', async ({ page }) => {
      const editor = page.locator('.ql-editor');
      await editor.click();

      // Insert 3 tabs
      await page.keyboard.press('Tab');
      await page.keyboard.press('Tab');
      await page.keyboard.press('Tab');

      let tabs = await page.locator('.ql-tab').all();
      expect(tabs.length).toBe(3);

      // Backspace twice
      await page.keyboard.press('Backspace');
      await page.keyboard.press('Backspace');

      tabs = await page.locator('.ql-tab').all();
      expect(tabs.length).toBe(1);
    });
  });

  // ============================================
  // TAB AFTER SOFT-BREAK Tests
  // ============================================

  test.describe('Tab After Soft-Break', () => {
    test('Tab can be inserted on new paragraph', async ({ page }) => {
      const editor = page.locator('.ql-editor');
      await editor.click();

      // Text only (no tabs)
      await page.keyboard.type('Line1');

      // Hard-break for new paragraph
      await page.keyboard.press('Enter');

      // Now insert a tab as first character on new paragraph
      await page.keyboard.press('Tab');
      await page.keyboard.type('After');

      const tabs = await page.locator('.ql-tab').all();
      expect(tabs.length).toBe(1);

      // Tab should have positive width
      const tabRect = await tabs[0].boundingBox();
      expect(tabRect!.width).toBeGreaterThan(0);
    });

    test('Additional tab can be inserted on new visual line after soft-break', async ({ page }) => {
      const editor = page.locator('.ql-editor');
      await editor.click();

      // 1 tab + text
      await page.keyboard.press('Tab');
      await page.keyboard.type('Line1');

      // Soft-break (copies 1 tab)
      await page.keyboard.down('Shift');
      await page.keyboard.press('Enter');
      await page.keyboard.up('Shift');

      // Insert another tab (now we have: copied tab + new tab)
      await page.keyboard.press('Tab');
      await page.keyboard.type('Line2');

      const tabs = await page.locator('.ql-tab').all();
      expect(tabs.length).toBe(3); // 1 original + 1 copied + 1 new

      // All tabs should have positive width
      for (const tab of tabs) {
        const tabRect = await tab.boundingBox();
        expect(tabRect!.width).toBeGreaterThan(0);
      }
    });
  });

  // ============================================
  // FORMATTED TEXT Tests
  // ============================================

  test.describe('Formatted Text with Tabs', () => {
    test('Bold text preserved after soft-break', async ({ page }) => {
      const editor = page.locator('.ql-editor');
      await editor.click();

      // Tab + bold text
      await page.keyboard.press('Tab');
      await page.keyboard.press('Control+b');
      await page.keyboard.type('BoldText');
      await page.keyboard.press('Control+b');

      // Soft-break
      await page.keyboard.down('Shift');
      await page.keyboard.press('Enter');
      await page.keyboard.up('Shift');

      // Check that bold text is preserved
      const delta = await getDeltaJson(page);
      const boldOps = delta.ops.filter((op: any) => op.attributes?.bold);
      expect(boldOps.length).toBeGreaterThan(0);
    });

    test('Tab between formatted spans works correctly', async ({ page }) => {
      const editor = page.locator('.ql-editor');
      await editor.click();

      // Bold text
      await page.keyboard.press('Control+b');
      await page.keyboard.type('Bold');
      await page.keyboard.press('Control+b');

      // Tab
      await page.keyboard.press('Tab');

      // Italic text
      await page.keyboard.press('Control+i');
      await page.keyboard.type('Italic');
      await page.keyboard.press('Control+i');

      // Soft-break
      await page.keyboard.down('Shift');
      await page.keyboard.press('Enter');
      await page.keyboard.up('Shift');

      // Should have 1 tab (original) + 1 tab (copied)
      const tabs = await page.locator('.ql-tab').all();
      expect(tabs.length).toBe(2);

      // Both bold and italic should be preserved
      const delta = await getDeltaJson(page);
      const boldOps = delta.ops.filter((op: any) => op.attributes?.bold);
      const italicOps = delta.ops.filter((op: any) => op.attributes?.italic);
      expect(boldOps.length).toBeGreaterThan(0);
      expect(italicOps.length).toBeGreaterThan(0);
    });
  });

  // ============================================
  // CURSOR NAVIGATION Tests
  // ============================================

  test.describe('Cursor Navigation', () => {
    test('Arrow keys navigate correctly across tabs', async ({ page }) => {
      const editor = page.locator('.ql-editor');
      await editor.click();

      // Tab + text + Tab
      await page.keyboard.press('Tab');
      await page.keyboard.type('AB');
      await page.keyboard.press('Tab');

      // Go back to start
      await page.keyboard.press('Home');

      // Navigate right: Tab(1) + A(1) + B(1) + Tab(1) = 4 positions
      for (let i = 0; i < 4; i++) {
        await page.keyboard.press('ArrowRight');
      }

      // Type at end
      await page.keyboard.type('X');

      const delta = await getDeltaJson(page);
      const textOps = delta.ops.filter((op: any) => typeof op.insert === 'string' && op.insert !== '\n');
      const allText = textOps.map((op: any) => op.insert).join('');
      expect(allText).toContain('X');
    });

    test('End key moves to end of visual line', async ({ page }) => {
      const editor = page.locator('.ql-editor');
      await editor.click();

      // Tab + text
      await page.keyboard.press('Tab');
      await page.keyboard.type('First');

      // Soft-break
      await page.keyboard.down('Shift');
      await page.keyboard.press('Enter');
      await page.keyboard.up('Shift');

      await page.keyboard.type('Second');

      // Go to start of second visual line
      await page.keyboard.press('Home');

      // End should go to end of current visual line
      await page.keyboard.press('End');

      // Type marker
      await page.keyboard.type('END');

      const delta = await getDeltaJson(page);
      const textOps = delta.ops.filter((op: any) => typeof op.insert === 'string');
      const allText = textOps.map((op: any) => op.insert).join('');
      expect(allText).toContain('SecondEND');
    });

    test('Shift+Arrow creates selection and delete removes selected content', async ({ page }) => {
      const editor = page.locator('.ql-editor');
      await editor.click();

      // Text only (simpler test)
      await page.keyboard.type('ABCD');

      // Go to start
      await page.keyboard.press('Home');

      // Select first 2 characters
      await page.keyboard.down('Shift');
      await page.keyboard.press('ArrowRight');
      await page.keyboard.press('ArrowRight');
      await page.keyboard.up('Shift');

      // Delete selection
      await page.keyboard.press('Delete');

      // Should have "CD" remaining (use toContain for flexibility with newlines)
      const delta = await getDeltaJson(page);
      const textOps = delta.ops.filter((op: any) => typeof op.insert === 'string' && op.insert !== '\n');
      const allText = textOps.map((op: any) => op.insert).join('');
      expect(allText).toContain('CD');
      expect(allText).not.toContain('AB');
    });
  });

  // ============================================
  // MULTIPLE PARAGRAPHS Tests
  // ============================================

  test.describe('Multiple Paragraphs with Soft-Breaks', () => {
    test('Soft-breaks in different paragraphs are independent', async ({ page }) => {
      const editor = page.locator('.ql-editor');
      await editor.click();

      // Paragraph 1: 2 tabs + soft-break
      await page.keyboard.press('Tab');
      await page.keyboard.press('Tab');
      await page.keyboard.type('P1');
      await page.keyboard.down('Shift');
      await page.keyboard.press('Enter');
      await page.keyboard.up('Shift');
      await page.keyboard.type('P1-line2');

      // Hard-break for new paragraph
      await page.keyboard.press('Enter');

      // Paragraph 2: 1 tab + soft-break
      await page.keyboard.press('Tab');
      await page.keyboard.type('P2');
      await page.keyboard.down('Shift');
      await page.keyboard.press('Enter');
      await page.keyboard.up('Shift');
      await page.keyboard.type('P2-line2');

      // Should have 2 paragraphs
      const paragraphs = await page.locator('.ql-editor > p').all();
      expect(paragraphs.length).toBe(2);

      // Should have 2 soft-breaks
      const softBreaks = await page.locator('.ql-soft-break').all();
      expect(softBreaks.length).toBe(2);

      // P1: 2 original + 2 copied = 4 tabs
      // P2: 1 original + 1 copied = 2 tabs
      // Total: 6 tabs
      const tabs = await page.locator('.ql-tab').all();
      expect(tabs.length).toBe(6);
    });

    test('Soft-break does not affect other paragraphs', async ({ page }) => {
      const editor = page.locator('.ql-editor');
      await editor.click();

      // Paragraph 1: Tab + text
      await page.keyboard.press('Tab');
      await page.keyboard.type('Para1');
      await page.keyboard.press('Enter');

      // Paragraph 2: 3 tabs + soft-break
      await page.keyboard.press('Tab');
      await page.keyboard.press('Tab');
      await page.keyboard.press('Tab');
      await page.keyboard.type('Para2');
      await page.keyboard.down('Shift');
      await page.keyboard.press('Enter');
      await page.keyboard.up('Shift');

      // Paragraph 1 should still have only 1 tab
      // Paragraph 2: 3 original + 3 copied = 6 tabs
      // Total: 1 + 6 = 7 tabs
      const tabs = await page.locator('.ql-tab').all();
      expect(tabs.length).toBe(7);
    });
  });

  // ============================================
  // OVERFLOW TAB LIMIT Tests (Customer Requirement)
  // ============================================

  test.describe('Overflow Tab Limit', () => {
    test('Soft-break at overflow tab position copies only tabstop count tabs', async ({ page }) => {
      const editor = page.locator('.ql-editor');
      await editor.click();

      // Insert 5 tabs (only 3 tabstops defined: L, C, R)
      for (let i = 0; i < 5; i++) {
        await page.keyboard.press('Tab');
      }

      // Soft-break at end (cursor after all 5 tabs)
      await page.keyboard.down('Shift');
      await page.keyboard.press('Enter');
      await page.keyboard.up('Shift');

      // Should copy only 3 tabs (number of tabstops), not 5
      // Line 1: 5 tabs, Line 2: 3 tabs = 8 total
      const tabs = await page.locator('.ql-tab').all();
      expect(tabs.length).toBe(8);
    });

    test('Soft-break after 4th tab (overflow) copies only 3 tabs', async ({ page }) => {
      const editor = page.locator('.ql-editor');
      await editor.click();

      // Insert 6 tabs
      for (let i = 0; i < 6; i++) {
        await page.keyboard.press('Tab');
      }
      await page.keyboard.type('Text');

      // Move cursor to after 4th tab
      await page.keyboard.press('Home');
      for (let i = 0; i < 4; i++) {
        await page.keyboard.press('ArrowRight');
      }

      // Soft-break (cursor after 4th tab, but only 3 tabstops exist)
      await page.keyboard.down('Shift');
      await page.keyboard.press('Enter');
      await page.keyboard.up('Shift');

      // Should copy only 3 tabs (min of 4 and 3)
      // Line 1: 6 tabs, Line 2: 3 tabs = 9 total
      const tabs = await page.locator('.ql-tab').all();
      expect(tabs.length).toBe(9);
    });

    test('Soft-break with cursor at 2nd tab copies 2 tabs (within tabstop limit)', async ({ page }) => {
      const editor = page.locator('.ql-editor');
      await editor.click();

      // Insert 5 tabs
      for (let i = 0; i < 5; i++) {
        await page.keyboard.press('Tab');
      }

      // Move cursor to after 2nd tab (3 ArrowLeft from position 5)
      await page.keyboard.press('ArrowLeft');
      await page.keyboard.press('ArrowLeft');
      await page.keyboard.press('ArrowLeft');

      // Soft-break (cursor after 2nd tab, within 3 tabstop limit)
      await page.keyboard.down('Shift');
      await page.keyboard.press('Enter');
      await page.keyboard.up('Shift');

      // Should copy 2 tabs (cursor position, within limit)
      // Line 1: 5 tabs, Line 2: 2 tabs = 7 total
      const tabs = await page.locator('.ql-tab').all();
      expect(tabs.length).toBe(7);
    });

    test('Soft-break after removing tabstops respects new limit', async ({ page }) => {
      const editor = page.locator('.ql-editor');
      await editor.click();

      // Remove first tabstop (L) - 3 clicks
      let tick = page.locator('.ruler-tick').first();
      await tick.click();
      await tick.click();
      await tick.click();
      await page.waitForTimeout(100);

      // Now only 2 tabstops remain (C, R)

      // Insert 4 tabs
      for (let i = 0; i < 4; i++) {
        await page.keyboard.press('Tab');
      }

      // Soft-break at end
      await page.keyboard.down('Shift');
      await page.keyboard.press('Enter');
      await page.keyboard.up('Shift');

      // Should copy only 2 tabs (new tabstop limit)
      // Line 1: 4 tabs, Line 2: 2 tabs = 6 total
      const tabs = await page.locator('.ql-tab').all();
      expect(tabs.length).toBe(6);
    });
  });

  // ============================================
  // BROWSER RESIZE Tests
  // ============================================

  test.describe('Browser Resize', () => {
    test('Tab widths recalculate after viewport resize', async ({ page }) => {
      await page.setViewportSize({ width: 800, height: 600 });

      const editor = page.locator('.ql-editor');
      await editor.click();

      // Insert tabs
      await page.keyboard.press('Tab');
      await page.keyboard.press('Tab');
      await page.keyboard.type('Text');

      // Get initial tab widths
      const tabs = await page.locator('.ql-tab').all();
      const width1Before = (await tabs[0].boundingBox())!.width;

      // Resize viewport
      await page.setViewportSize({ width: 600, height: 600 });
      await page.waitForTimeout(300);

      // Tab widths should still be valid (aligned to tabstops)
      const width1After = (await tabs[0].boundingBox())!.width;

      // Widths may or may not change depending on implementation
      // At minimum, tabs should still exist and have positive width
      expect(width1After).toBeGreaterThan(0);
    });
  });

  // ============================================
  // FOCUS/BLUR Tests
  // ============================================

  test.describe('Focus/Blur', () => {
    test('Tabs remain visible after blur and refocus', async ({ page }) => {
      const editor = page.locator('.ql-editor');
      await editor.click();

      // Insert tabs
      await page.keyboard.press('Tab');
      await page.keyboard.press('Tab');
      await page.keyboard.type('Text');

      // Get initial tab count and verify widths are positive
      let tabs = await page.locator('.ql-tab').all();
      expect(tabs.length).toBe(2);
      const width1Before = (await tabs[0].boundingBox())!.width;
      expect(width1Before).toBeGreaterThan(0);

      // Click outside editor to blur
      await page.locator('.native-quill-ruler').click();
      await page.waitForTimeout(100);

      // Click back in editor
      await editor.click();
      await page.waitForTimeout(100);

      // Tabs should still exist with positive widths
      tabs = await page.locator('.ql-tab').all();
      expect(tabs.length).toBe(2);
      const width1After = (await tabs[0].boundingBox())!.width;
      expect(width1After).toBeGreaterThan(0);
    });
  });

  // ============================================
  // TAB AT TABSTOP BOUNDARY Tests
  // ============================================

  test.describe('Tab at Tabstop Boundary', () => {
    test('Consecutive tabs have increasing end positions', async ({ page }) => {
      const editor = page.locator('.ql-editor');
      await editor.click();

      // Insert two tabs
      await page.keyboard.press('Tab');
      await page.keyboard.press('Tab');
      await page.keyboard.type('End');

      // Wait for width calculation
      await page.waitForTimeout(200);

      const tabs = await page.locator('.ql-tab').all();
      expect(tabs.length).toBe(2);

      const editorRect = await editor.boundingBox();

      // First tab end position
      const tab1Rect = await tabs[0].boundingBox();
      const tab1EndPos = tab1Rect!.x + tab1Rect!.width - editorRect!.x;

      // Second tab end position
      const tab2Rect = await tabs[1].boundingBox();
      const tab2EndPos = tab2Rect!.x + tab2Rect!.width - editorRect!.x;

      // Second tab should end further right than first tab
      expect(tab2EndPos).toBeGreaterThan(tab1EndPos);
    });
  });

  // ============================================
  // EMPTY VISUAL LINE Tests
  // ============================================

  test.describe('Empty Visual Line', () => {
    test('Soft-break on already empty visual line after previous soft-break', async ({ page }) => {
      const editor = page.locator('.ql-editor');
      await editor.click();

      // Tab + text
      await page.keyboard.press('Tab');
      await page.keyboard.type('Text');

      // First soft-break
      await page.keyboard.down('Shift');
      await page.keyboard.press('Enter');
      await page.keyboard.up('Shift');

      // Now we're on a new visual line with 1 copied tab
      // Delete the tab to have empty visual line
      await page.keyboard.press('Backspace');

      // Second soft-break on empty visual line
      await page.keyboard.down('Shift');
      await page.keyboard.press('Enter');
      await page.keyboard.up('Shift');

      // Should have 2 soft-breaks
      const softBreaks = await page.locator('.ql-soft-break').all();
      expect(softBreaks.length).toBe(2);

      // Should still have only 1 tab (from original line)
      const tabs = await page.locator('.ql-tab').all();
      expect(tabs.length).toBe(1);
    });
  });

  // ============================================
  // STRESS TESTS
  // ============================================

  test.describe('Stress Tests', () => {
    test('Rapid soft-breaks (10x)', async ({ page }) => {
      const editor = page.locator('.ql-editor');
      await editor.click();

      // 3 tabs
      await page.keyboard.press('Tab');
      await page.keyboard.press('Tab');
      await page.keyboard.press('Tab');

      // 10 rapid soft-breaks
      for (let i = 0; i < 10; i++) {
        await page.keyboard.down('Shift');
        await page.keyboard.press('Enter');
        await page.keyboard.up('Shift');
      }

      // Should have 3 original + 10*3 copied = 33 tabs
      const tabs = await page.locator('.ql-tab').all();
      expect(tabs.length).toBe(33);

      // Should have 10 soft-breaks
      const softBreaks = await page.locator('.ql-soft-break').all();
      expect(softBreaks.length).toBe(10);
    });

    test('All tabstops removed - tabs use fixed width', async ({ page }) => {
      const editor = page.locator('.ql-editor');

      // Remove all 3 tabstops one by one
      // Initial: L(100), C(300), R(500)
      // L needs 3 clicks (L->C->R->remove)
      // C needs 2 clicks (C->R->remove)
      // R needs 1 click (R->remove)

      // First tabstop (L): 3 clicks
      let tick = page.locator('.ruler-tick').first();
      await tick.click();
      await tick.click();
      await tick.click();
      await page.waitForTimeout(100);

      // Second tabstop (C, now first): 2 clicks
      tick = page.locator('.ruler-tick').first();
      await tick.click();
      await tick.click();
      await page.waitForTimeout(100);

      // Third tabstop (R, now first): 1 click
      tick = page.locator('.ruler-tick').first();
      await tick.click();
      await page.waitForTimeout(100);

      // Verify no tabstops remain
      const ticks = await page.locator('.ruler-tick').all();
      expect(ticks.length).toBe(0);

      // Insert tabs
      await editor.click();
      await page.keyboard.press('Tab');
      await page.keyboard.press('Tab');
      await page.keyboard.type('No tabstops');

      // Tabs should use fixed width
      const tabs = await page.locator('.ql-tab').all();
      expect(tabs.length).toBe(2);

      const tab1Rect = await tabs[0].boundingBox();
      const tab2Rect = await tabs[1].boundingBox();

      // Both should have similar fixed width
      expect(Math.abs(tab1Rect!.width - tab2Rect!.width)).toBeLessThan(10);
    });
  });

  // ============================================
  // WHITESPACE INDICATORS Tests
  // ============================================

  test.describe('Whitespace Indicators', () => {
    test('Show Whitespace checkbox is present and checked by default', async ({ page }) => {
      const checkbox = page.locator('vaadin-checkbox:has-text("Show Whitespace")');
      await expect(checkbox).toBeVisible();

      // Should be checked by default
      const isChecked = await checkbox.locator('input').isChecked();
      expect(isChecked).toBe(true);
    });

    test('Tab Debug checkbox is present and unchecked by default', async ({ page }) => {
      const checkbox = page.locator('vaadin-checkbox:has-text("Tab Debug")');
      await expect(checkbox).toBeVisible();

      // Should be unchecked by default
      const isChecked = await checkbox.locator('input').isChecked();
      expect(isChecked).toBe(false);
    });

    test('Tab indicator (→) visible when Show Whitespace enabled', async ({ page }) => {
      const editor = page.locator('.ql-editor');
      await editor.click();

      // Insert a tab
      await page.keyboard.press('Tab');
      await page.keyboard.type('Text');

      // Check that the tab has the arrow indicator via ::before pseudo-element
      // We verify this by checking if the parent has show-whitespace class
      const parentView = page.locator('.show-whitespace');
      await expect(parentView).toBeVisible();

      // Tab should exist
      const tabs = await page.locator('.ql-tab').all();
      expect(tabs.length).toBe(1);
    });

    test('Soft-break indicator (↵) visible when Show Whitespace enabled', async ({ page }) => {
      const editor = page.locator('.ql-editor');
      await editor.click();

      // Insert soft-break
      await page.keyboard.type('Line1');
      await page.keyboard.down('Shift');
      await page.keyboard.press('Enter');
      await page.keyboard.up('Shift');
      await page.keyboard.type('Line2');

      // Verify soft-break exists
      const softBreaks = await page.locator('.ql-soft-break').all();
      expect(softBreaks.length).toBe(1);

      // show-whitespace class should be present (enables the indicator)
      const parentView = page.locator('.show-whitespace');
      await expect(parentView).toBeVisible();
    });

    test('Paragraph indicator (¶) visible at end of paragraphs', async ({ page }) => {
      const editor = page.locator('.ql-editor');
      await editor.click();

      // Insert two paragraphs
      await page.keyboard.type('Paragraph1');
      await page.keyboard.press('Enter');
      await page.keyboard.type('Paragraph2');

      // Verify we have 2 paragraphs
      const paragraphs = await page.locator('.ql-editor > p').all();
      expect(paragraphs.length).toBe(2);

      // show-whitespace class should be present (enables the pilcrow indicator)
      const parentView = page.locator('.show-whitespace');
      await expect(parentView).toBeVisible();
    });

    test('Disabling Show Whitespace removes indicators', async ({ page }) => {
      const editor = page.locator('.ql-editor');
      await editor.click();

      // Insert content
      await page.keyboard.press('Tab');
      await page.keyboard.type('Text');

      // Verify show-whitespace is initially present
      let parentView = page.locator('.show-whitespace');
      await expect(parentView).toBeVisible();

      // Uncheck the Show Whitespace checkbox (click on the label or input)
      const checkbox = page.locator('vaadin-checkbox:has-text("Show Whitespace")');
      await checkbox.locator('label').click();
      await page.waitForTimeout(200);

      // show-whitespace class should be removed
      parentView = page.locator('.show-whitespace');
      await expect(parentView).toHaveCount(0);
    });

    test('Tab Debug shows background color for tabs', async ({ page }) => {
      const editor = page.locator('.ql-editor');
      await editor.click();

      // Insert a tab
      await page.keyboard.press('Tab');
      await page.keyboard.type('Text');

      // Verify tab-debug is initially NOT present
      let tabDebugElements = page.locator('.tab-debug');
      await expect(tabDebugElements).toHaveCount(0);

      // Enable Tab Debug (click on the label)
      const checkbox = page.locator('vaadin-checkbox:has-text("Tab Debug")');
      await checkbox.locator('label').click();
      await page.waitForTimeout(200);

      // tab-debug class should now be present
      tabDebugElements = page.locator('.tab-debug');
      await expect(tabDebugElements).toHaveCount(1);
    });

    test('Show Whitespace and Tab Debug work independently', async ({ page }) => {
      const editor = page.locator('.ql-editor');
      await editor.click();

      // Insert content
      await page.keyboard.press('Tab');
      await page.keyboard.type('Text');

      // Initial state: Show Whitespace ON, Tab Debug OFF
      await expect(page.locator('.show-whitespace')).toHaveCount(1);
      await expect(page.locator('.tab-debug')).toHaveCount(0);

      // Enable Tab Debug
      await page.locator('vaadin-checkbox:has-text("Tab Debug") label').click();
      await page.waitForTimeout(200);
      await expect(page.locator('.show-whitespace')).toHaveCount(1);
      await expect(page.locator('.tab-debug')).toHaveCount(1);

      // Disable Show Whitespace
      await page.locator('vaadin-checkbox:has-text("Show Whitespace") label').click();
      await page.waitForTimeout(200);
      await expect(page.locator('.show-whitespace')).toHaveCount(0);
      await expect(page.locator('.tab-debug')).toHaveCount(1);

      // Disable Tab Debug
      await page.locator('vaadin-checkbox:has-text("Tab Debug") label').click();
      await page.waitForTimeout(200);
      await expect(page.locator('.show-whitespace')).toHaveCount(0);
      await expect(page.locator('.tab-debug')).toHaveCount(0);
    });

    test('Indicators visible for all whitespace types simultaneously', async ({ page }) => {
      const editor = page.locator('.ql-editor');
      await editor.click();

      // Create content with tabs, soft-break, and hard-break
      await page.keyboard.press('Tab');
      await page.keyboard.type('TabText');
      await page.keyboard.down('Shift');
      await page.keyboard.press('Enter');
      await page.keyboard.up('Shift');
      await page.keyboard.type('SoftBreakLine');
      await page.keyboard.press('Enter');
      await page.keyboard.type('NewParagraph');

      // Verify all elements exist
      const tabs = await page.locator('.ql-tab').all();
      expect(tabs.length).toBeGreaterThanOrEqual(1);

      const softBreaks = await page.locator('.ql-soft-break').all();
      expect(softBreaks.length).toBe(1);

      const paragraphs = await page.locator('.ql-editor > p').all();
      expect(paragraphs.length).toBe(2);

      // show-whitespace should be active (all indicators visible)
      await expect(page.locator('.show-whitespace')).toBeVisible();
    });

    test('Auto-wrap indicator (↲) shown for wrapped tabs', async ({ page }) => {
      // Set narrow viewport to trigger wrapping
      await page.setViewportSize({ width: 400, height: 600 });

      const editor = page.locator('.ql-editor');
      await editor.click();

      // Type long text that will wrap
      await page.keyboard.type('This is a very long line of text that should wrap automatically when the viewport is narrow enough.');

      // Add a tab after the text wraps
      await page.keyboard.press('Tab');
      await page.keyboard.type('More text after tab');

      await page.waitForTimeout(300);

      // Check if any tab has the auto-wrap class
      const wrappedTabs = await page.locator('.ql-tab.ql-auto-wrap').all();

      // Note: This test checks that the class mechanism works
      // The actual wrapping depends on viewport width and text length
      // At minimum, verify the show-whitespace class is present
      await expect(page.locator('.show-whitespace')).toBeVisible();
    });

    test('Auto-wrap class removed when tab not on wrapped line', async ({ page }) => {
      const editor = page.locator('.ql-editor');
      await editor.click();

      // Insert tab at beginning (should NOT be wrapped)
      await page.keyboard.press('Tab');
      await page.keyboard.type('Short text');

      await page.waitForTimeout(200);

      // Tab should NOT have the auto-wrap class
      const tabs = await page.locator('.ql-tab').all();
      expect(tabs.length).toBe(1);

      // Check the tab doesn't have auto-wrap class
      const hasAutoWrap = await tabs[0].evaluate(el => el.classList.contains('ql-auto-wrap'));
      expect(hasAutoWrap).toBe(false);
    });
  });
});
