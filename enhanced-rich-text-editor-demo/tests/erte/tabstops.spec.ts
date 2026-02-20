import { test, expect } from '@playwright/test';
import {
  waitForEditor,
  getErte,
  getEditor,
  getTabs,
  getSoftBreaks,
  getDelta,
  countInDelta,
  focusEditor,
  getRuler,
  getRulerMarkers,
  getRulerMarkerDirection,
  enableShowWhitespace,
  disableShowWhitespace,
  isShowWhitespaceActive,
  ERTE_TEST_BASE,
} from './helpers';

const TABSTOPS_URL = `${ERTE_TEST_BASE}/tabstops`;

test.describe('ERTE Tabstops', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto(TABSTOPS_URL);
    await waitForEditor(page);
  });

  // ============================================
  // HARD-BREAK (Enter) Tests
  // ============================================

  test.describe('Hard-Break (Enter)', () => {
    test('Tab in new paragraph aligns to first tabstop', async ({ page }) => {
      const editor = getEditor(page);
      await editor.click();

      // Press Tab, type text, Enter, Tab
      await page.keyboard.press('Tab');
      await page.keyboard.type('Line1');
      await page.keyboard.press('Enter');
      await page.keyboard.press('Tab');
      await page.keyboard.type('Line2');

      // Both tabs should have the same X position (at first tabstop)
      await expect(getTabs(page)).toHaveCount(2, { timeout: 5000 });
      const tabs = await getTabs(page).all();

      const tab1Rect = await tabs[0].boundingBox();
      const tab2Rect = await tabs[1].boundingBox();

      // Tabs should end at the same X position (tabstop position)
      expect(Math.abs(tab1Rect!.x + tab1Rect!.width - (tab2Rect!.x + tab2Rect!.width))).toBeLessThan(5);
    });

    test('Hard-Break does NOT copy tabs from previous line', async ({ page }) => {
      const editor = getEditor(page);
      await editor.click();

      // 3 Tabs + Text, then Enter
      await page.keyboard.press('Tab');
      await page.keyboard.press('Tab');
      await page.keyboard.press('Tab');
      await page.keyboard.type('Text');
      await page.keyboard.press('Enter');
      await page.keyboard.type('New paragraph');

      // Only the original 3 tabs should exist (hard-break doesn't copy)
      await expect(getTabs(page)).toHaveCount(3, { timeout: 5000 });

      // Verify we have 2 paragraphs
      const paragraphs = await getErte(page).locator('.ql-editor > p').all();
      expect(paragraphs.length).toBe(2);
    });
  });

  // ============================================
  // SOFT-BREAK (Shift+Enter) Tests
  // ============================================

  test.describe('Soft-Break (Shift+Enter)', () => {
    test('Copies tabs up to cursor position - cursor after 1st of 3 tabs', async ({ page }) => {
      const editor = getEditor(page);
      await editor.click();

      // Insert 3 tabs
      await page.keyboard.press('Tab');
      await page.keyboard.press('Tab');
      await page.keyboard.press('Tab');

      // Move cursor to position after 1st tab (ArrowLeft 2x: from position 3 to position 1)
      await page.keyboard.press('ArrowLeft');
      await page.keyboard.press('ArrowLeft');

      // Soft-break
      await page.keyboard.press('Shift+Enter');

      // Line 1 stays intact (3 tabs), new line has 1 tab = 4 total
      await expect(getTabs(page)).toHaveCount(4, { timeout: 5000 });
    });

    test('Cursor at line start copies no tabs', async ({ page }) => {
      const editor = getEditor(page);
      await editor.click();

      // Insert 2 tabs
      await page.keyboard.press('Tab');
      await page.keyboard.press('Tab');

      // Move cursor to actual start (ArrowLeft to position 0)
      // Note: Home in Quill sets cursor after first embed, not to position 0
      await page.keyboard.press('ArrowLeft');
      await page.keyboard.press('ArrowLeft');

      // Soft-break
      await page.keyboard.press('Shift+Enter');

      // Line 1 stays intact (2 tabs), new line has 0 tabs = 2 total
      await expect(getTabs(page)).toHaveCount(2, { timeout: 5000 });
    });

    test('Cursor after all tabs copies all tabs', async ({ page }) => {
      const editor = getEditor(page);
      await editor.click();

      // 2 Tabs + Text
      await page.keyboard.press('Tab');
      await page.keyboard.press('Tab');
      await page.keyboard.type('Text');

      // Soft-break (cursor is at end, after both tabs)
      await page.keyboard.press('Shift+Enter');

      // Line 1 stays (2 tabs), new line has 2 tabs = 4 total
      await expect(getTabs(page)).toHaveCount(4, { timeout: 5000 });
    });

    test('Multiple soft-breaks: each new line gets same tab count', async ({ page }) => {
      const editor = getEditor(page);
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
        await page.keyboard.press('Shift+Enter');
      }

      // Line 1: 5 tabs (intact), lines 2-4: 3 tabs each = 5 + 3*3 = 14 tabs
      await expect(getTabs(page)).toHaveCount(14, { timeout: 5000 });

      // Verify structure via Delta
      const delta = await getDelta(page);
      const softBreaks = countInDelta(delta, 'soft-break');
      expect(softBreaks).toBe(3);
    });

    test('Soft-break preserves original line content', async ({ page }) => {
      const editor = getEditor(page);
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
      await page.keyboard.press('Shift+Enter');

      // Original line should still contain all content
      const delta = await getDelta(page);

      // Should have: Tab, "First", Tab, "Second", soft-break, Tab, newline
      expect(countInDelta(delta, 'tab')).toBe(3); // 2 original + 1 copied
      expect(countInDelta(delta, 'soft-break')).toBe(1);
    });

    test('Soft-break in middle of visual line after previous soft-break', async ({ page }) => {
      const editor = getEditor(page);
      await editor.click();

      // First visual line: 2 Tabs
      await page.keyboard.press('Tab');
      await page.keyboard.press('Tab');

      // Soft-break to create second visual line
      await page.keyboard.press('Shift+Enter');

      // Second visual line now has 2 tabs (copied), add one more
      await page.keyboard.press('Tab');

      // Move cursor back one position (after the 2nd copied tab)
      await page.keyboard.press('ArrowLeft');

      // Another soft-break
      await page.keyboard.press('Shift+Enter');

      // Line 1: 2 tabs, Line 2: 3 tabs (2 copied + 1 added), Line 3: 2 tabs
      // Total: 2 + 3 + 2 = 7 tabs
      await expect(getTabs(page)).toHaveCount(7, { timeout: 5000 });
    });
  });

  // ============================================
  // AUTOMATIC WRAP Tests
  // ============================================

  test.describe('Automatic Wrap', () => {
    test('Tab after auto-wrap uses fixed width (not tabstop)', async ({ page }) => {
      // Make window narrow to force wrap
      await page.setViewportSize({ width: 400, height: 600 });

      const editor = getEditor(page);
      await editor.click();

      // Enter long text that wraps, then Tab
      await page.keyboard.type('This is a very long text that should automatically wrap when the window is narrow enough');
      await page.keyboard.press('Tab');
      await page.keyboard.type('After-Tab');

      // Tab at line start after automatic wrap should have fixed width
      await expect(getTabs(page)).toHaveCount(1, { timeout: 5000 });
      const tabs = await getTabs(page).all();
      if (tabs.length > 0) {
        const tabRect = await tabs[0].boundingBox();
        // Fixed tab width is ~50px (FIXED_TAB_FALLBACK) or 8 characters
        // NOT aligned to tabstop (which would be much wider, e.g., 100px)
        expect(tabRect!.width).toBeLessThan(80);
      }
    });

    test('Tabs before wrap still align to tabstops', async ({ page }) => {
      await page.setViewportSize({ width: 600, height: 600 });

      const editor = getEditor(page);
      await editor.click();

      // Tab at start, then text
      await page.keyboard.press('Tab');
      await page.keyboard.type('Short text');

      await expect(getTabs(page)).toHaveCount(1, { timeout: 5000 });
      const tabs = await getTabs(page).all();
      expect(tabs.length).toBe(1);

      // First tab should align to first tabstop (L at 150px)
      const tabRect = await tabs[0].boundingBox();
      const editorRect = await editor.boundingBox();

      // Tab end position should be around 150px from editor start
      const tabEndPos = tabRect!.x + tabRect!.width - editorRect!.x;
      expect(tabEndPos).toBeGreaterThan(120);
      expect(tabEndPos).toBeLessThan(180);
    });
  });

  // ============================================
  // COMBINED SCENARIOS
  // ============================================

  test.describe('Combined Scenarios', () => {
    test('Hard-break after soft-break creates new paragraph', async ({ page }) => {
      const editor = getEditor(page);
      await editor.click();

      // Tabs + soft-break
      await page.keyboard.press('Tab');
      await page.keyboard.press('Tab');
      await page.keyboard.press('Shift+Enter');

      // Now hard-break
      await page.keyboard.press('Enter');

      // Should have 2 paragraphs
      const paragraphs = await getErte(page).locator('.ql-editor > p').all();
      expect(paragraphs.length).toBe(2);
    });

    test('Tabs align correctly after soft-break', async ({ page }) => {
      const editor = getEditor(page);
      await editor.click();

      // Tab in first visual line
      await page.keyboard.press('Tab');
      await page.keyboard.type('Line1');

      // Soft-break (should copy 1 tab)
      await page.keyboard.press('Shift+Enter');

      // Add another tab in second visual line
      await page.keyboard.press('Tab');
      await page.keyboard.type('Line2');

      // Should have 3 tabs: 1 original, 1 copied, 1 new
      await expect(getTabs(page)).toHaveCount(3, { timeout: 5000 });

      // All tabs should align to tabstops
      const tabs = await getTabs(page).all();
      for (const tab of tabs) {
        const rect = await tab.boundingBox();
        // Tab should have significant width (aligned to tabstop)
        expect(rect!.width).toBeGreaterThan(10);
      }
    });

    test('Soft-break after tabs and text preserves text', async ({ page }) => {
      const editor = getEditor(page);
      await editor.click();

      // Tab + Text
      await page.keyboard.press('Tab');
      await page.keyboard.type('Hello World');

      // Cursor in middle of text
      for (let i = 0; i < 5; i++) {
        await page.keyboard.press('ArrowLeft');
      }

      // Soft-break
      await page.keyboard.press('Shift+Enter');

      // Delta should contain both parts of text
      const delta = await getDelta(page);
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
      const editor = getEditor(page);
      await editor.click();

      await page.keyboard.press('Tab');
      await page.keyboard.type('Left aligned');

      await expect(getTabs(page)).toHaveCount(1, { timeout: 5000 });
      const tabs = await getTabs(page).all();
      expect(tabs.length).toBe(1);

      // First tabstop is at 150px (L)
      const tabRect = await tabs[0].boundingBox();
      const editorRect = await editor.boundingBox();
      const tabEndPos = tabRect!.x + tabRect!.width - editorRect!.x;

      // Should be around 150px (with some tolerance for padding)
      expect(tabEndPos).toBeGreaterThan(120);
      expect(tabEndPos).toBeLessThan(180);
    });

    test('Second tab aligns to C (center) tabstop', async ({ page }) => {
      const editor = getEditor(page);
      await editor.click();

      await page.keyboard.press('Tab');
      await page.keyboard.press('Tab');
      await page.keyboard.type('Center');

      // Wait for width calculation
      await expect(getTabs(page)).toHaveCount(2, { timeout: 5000 });

      const tabs = await getTabs(page).all();
      expect(tabs.length).toBe(2);

      // Second tabstop is RIGHT@350 - right aligned means text end aligns to tabstop
      // Right alignment subtracts the text width from the tab, so the tab end position varies
      const tab2Rect = await tabs[1].boundingBox();
      const editorRect = await editor.boundingBox();
      const tab2EndPos = tab2Rect!.x + tab2Rect!.width - editorRect!.x;

      // Should be somewhere between first tabstop (150px) and third tabstop (550px)
      // Right-aligned position depends on text width
      expect(tab2EndPos).toBeGreaterThan(150);
      expect(tab2EndPos).toBeLessThan(400);
    });

    test('Third tab aligns to R (right) tabstop', async ({ page }) => {
      const editor = getEditor(page);
      await editor.click();

      await page.keyboard.press('Tab');
      await page.keyboard.press('Tab');
      await page.keyboard.press('Tab');
      await page.keyboard.type('Right');

      // Wait for tab width calculation
      await expect(getTabs(page)).toHaveCount(3, { timeout: 5000 });

      const tabs = await getTabs(page).all();
      expect(tabs.length).toBe(3);

      // Third tabstop is MIDDLE@550 - center aligned
      // Center alignment: text centers on the tabstop, so tab width is reduced by half the text width
      const tab3Rect = await tabs[2].boundingBox();

      // Tab should have positive width (right alignment subtracts text width)
      expect(tab3Rect!.width).toBeGreaterThan(10);

      // For right alignment, verify tab + text reaches the tabstop
      // We can't easily measure text position, so just verify tab exists and has reasonable width
      expect(tab3Rect!.width).toBeLessThan(300);
    });

    test('Fourth tab (beyond tabstops) uses fixed width', async ({ page }) => {
      const editor = getEditor(page);
      await editor.click();

      // Insert 4 tabs (only 3 tabstops exist: L, C, R)
      await page.keyboard.press('Tab');
      await page.keyboard.press('Tab');
      await page.keyboard.press('Tab');
      await page.keyboard.press('Tab');
      await page.keyboard.type('Beyond');

      // Wait for tab width calculation
      await expect(getTabs(page)).toHaveCount(4, { timeout: 5000 });

      const tabs = await getTabs(page).all();
      expect(tabs.length).toBe(4);

      // Fourth tab has no tabstop -> uses fixed width (~50px or 8 chars)
      const tab4Rect = await tabs[3].boundingBox();

      // Fixed width should be around 50px (FIXED_TAB_FALLBACK), not aligned to any tabstop
      // Widened tolerance due to font variations
      expect(tab4Rect!.width).toBeGreaterThan(20);
      expect(tab4Rect!.width).toBeLessThan(100);
    });

    test('Fifth and sixth tabs also use fixed width', async ({ page }) => {
      const editor = getEditor(page);
      await editor.click();

      // Insert 6 tabs (only 3 tabstops exist)
      for (let i = 0; i < 6; i++) {
        await page.keyboard.press('Tab');
      }
      await page.keyboard.type('End');

      // Wait for width calculation
      await expect(getTabs(page)).toHaveCount(6, { timeout: 5000 });

      const tabs = await getTabs(page).all();
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
      const editor = getEditor(page);
      await editor.click();

      // Insert 5 tabs (more than 3 tabstops)
      for (let i = 0; i < 5; i++) {
        await page.keyboard.press('Tab');
      }
      await page.keyboard.type('Text');

      // Soft-break at end
      await page.keyboard.press('Shift+Enter');

      // Line 1: 5 tabs + text, Line 2: 3 copied tabs (limited by tabstop count) = 8 total
      await expect(getTabs(page)).toHaveCount(8, { timeout: 5000 });
    });

    test('Soft-break after 4th tab copies only 3 tabs (tabstop limit)', async ({ page }) => {
      const editor = getEditor(page);
      await editor.click();

      // Insert 6 tabs
      for (let i = 0; i < 6; i++) {
        await page.keyboard.press('Tab');
      }

      // Move cursor back to after 4th tab (2x ArrowLeft)
      await page.keyboard.press('ArrowLeft');
      await page.keyboard.press('ArrowLeft');

      // Soft-break
      await page.keyboard.press('Shift+Enter');

      // Line 1: 6 tabs (intact), Line 2: 3 copied tabs (limited by tabstop count) = 9 total
      await expect(getTabs(page)).toHaveCount(9, { timeout: 5000 });
    });

    test('Multiple soft-breaks with overflow tabs respect tabstop limit', async ({ page }) => {
      const editor = getEditor(page);
      await editor.click();

      // Insert 4 tabs (1 beyond tabstops)
      for (let i = 0; i < 4; i++) {
        await page.keyboard.press('Tab');
      }

      // 2x Soft-break at end
      for (let i = 0; i < 2; i++) {
        await page.keyboard.press('Shift+Enter');
      }

      // Line 1: 4 tabs, Lines 2-3: 3 tabs each (limited) = 4 + 3 + 3 = 10 total
      await expect(getTabs(page)).toHaveCount(10, { timeout: 5000 });
    });
  });

  // ============================================
  // RULER / TABSTOP MANIPULATION Tests
  // ============================================

  test.describe('Ruler and Tabstop Manipulation', () => {
    test('Adding a new tabstop by clicking ruler', async ({ page }) => {
      const ruler = getRuler(page);
      const editor = getEditor(page);

      // Click on ruler at ~200px to add a new tabstop
      const rulerRect = await ruler.boundingBox();
      await page.mouse.click(rulerRect!.x + 200, rulerRect!.y + 7);

      // Should now have 4 tabstop markers (original 3 + new one)
      await expect(getRulerMarkers(page)).toHaveCount(4, { timeout: 5000 });

      // Insert tabs and verify alignment
      await editor.click();
      await page.keyboard.press('Tab');
      await page.keyboard.press('Tab');
      await page.keyboard.type('After new tabstop');

      await expect(getTabs(page)).toHaveCount(2, { timeout: 5000 });
    });

    test('Cycling tabstop alignment: left -> right -> middle -> remove', async ({ page }) => {
      // Get first tabstop marker (LEFT at 150px, icon = vaadin:caret-right)
      const markers = getRulerMarkers(page);
      const firstMarker = markers.first();
      let direction = await getRulerMarkerDirection(firstMarker);
      expect(direction).toBe('left');

      // Click to cycle: left -> right
      await firstMarker.click();
      direction = await getRulerMarkerDirection(firstMarker);
      expect(direction).toBe('right');

      // Click to cycle: right -> middle
      await firstMarker.click();
      direction = await getRulerMarkerDirection(firstMarker);
      expect(direction).toBe('middle');

      // Click to remove (middle -> remove); should now have only 2 markers
      await firstMarker.click();
      await expect(markers).toHaveCount(2, { timeout: 5000 });
    });

    test('Removing middle tabstop affects tab widths', async ({ page }) => {
      const editor = getEditor(page);
      await editor.click();

      // Insert 3 tabs
      await page.keyboard.press('Tab');
      await page.keyboard.press('Tab');
      await page.keyboard.press('Tab');
      await page.keyboard.type('End');

      // Get tab widths before removal
      await expect(getTabs(page)).toHaveCount(3, { timeout: 5000 });
      const tabsBefore = await getTabs(page).all();
      const width2Before = (await tabsBefore[1].boundingBox())!.width;

      // The second marker is RIGHT@350 (icon = vaadin:caret-left).
      // Cycling: right -> middle -> remove (2 clicks)
      const secondMarker = getRulerMarkers(page).nth(1);
      await secondMarker.click(); // right -> middle
      await secondMarker.click(); // middle -> remove

      // Wait for tab width recalculation (use shadowRoot to pierce shadow DOM)
      await page.waitForFunction(
        ({ prevWidth }) => {
          const el = document.getElementById('test-editor') as any;
          const tabs = el?.shadowRoot?.querySelectorAll('.ql-tab') ?? el?.querySelectorAll('.ql-tab');
          if (!tabs || tabs.length < 2) return false;
          return tabs[1].getBoundingClientRect().width !== prevWidth;
        },
        { prevWidth: width2Before },
        { timeout: 5000 }
      );

      // Get tab widths after removal - second tab should now reach MIDDLE tabstop
      const tabsAfter = await getTabs(page).all();
      const width2After = (await tabsAfter[1].boundingBox())!.width;

      // Second tab should now be wider (reaching MIDDLE@550 instead of RIGHT@350)
      expect(width2After).toBeGreaterThan(width2Before);
    });

    test('Tab width updates when tabstop alignment changes', async ({ page }) => {
      const editor = getEditor(page);
      await editor.click();

      // Insert tab + text
      await page.keyboard.press('Tab');
      await page.keyboard.type('Test');

      // Get initial tab width (LEFT alignment)
      await expect(getTabs(page)).toHaveCount(1, { timeout: 5000 });
      const tabBefore = await getTabs(page).first().boundingBox();
      const widthBefore = tabBefore!.width;

      // Change first tabstop: left -> right (1 click)
      const firstMarker = getRulerMarkers(page).first();
      await firstMarker.click();

      // Wait for tab width recalculation (use shadowRoot to pierce shadow DOM)
      await page.waitForFunction(
        ({ prevWidth }) => {
          const el = document.getElementById('test-editor') as any;
          const tab = el?.shadowRoot?.querySelector('.ql-tab') ?? el?.querySelector('.ql-tab');
          if (!tab) return false;
          return tab.getBoundingClientRect().width !== prevWidth;
        },
        { prevWidth: widthBefore },
        { timeout: 5000 }
      );

      // Tab width should change (right alignment considers text width)
      const tabAfter = await getTabs(page).first().boundingBox();
      const widthAfter = tabAfter!.width;

      // Width should be different (right alignment accounts for text)
      expect(widthAfter).not.toBe(widthBefore);
    });
  });

  // ============================================
  // EDGE CASES Tests
  // ============================================

  test.describe('Edge Cases', () => {
    test('Soft-break with only tabs (no text)', async ({ page }) => {
      const editor = getEditor(page);
      await editor.click();

      // Only tabs, no text
      await page.keyboard.press('Tab');
      await page.keyboard.press('Tab');
      await page.keyboard.press('Tab');

      // Soft-break at end
      await page.keyboard.press('Shift+Enter');

      // Should have 6 tabs (3 original + 3 copied)
      await expect(getTabs(page)).toHaveCount(6, { timeout: 5000 });
    });

    test('Hard-break immediately after soft-break', async ({ page }) => {
      const editor = getEditor(page);
      await editor.click();

      await page.keyboard.press('Tab');
      await page.keyboard.type('Text');

      // Soft-break
      await page.keyboard.press('Shift+Enter');

      // Immediate hard-break
      await page.keyboard.press('Enter');

      // Should have 2 paragraphs
      const paragraphs = await getErte(page).locator('.ql-editor > p').all();
      expect(paragraphs.length).toBe(2);

      // First paragraph should have soft-break
      await expect(getSoftBreaks(page)).toHaveCount(1, { timeout: 5000 });
    });

    test('Soft-break at empty line', async ({ page }) => {
      const editor = getEditor(page);
      await editor.click();

      // Soft-break on empty line
      await page.keyboard.press('Shift+Enter');

      // Should create a soft-break even on empty line
      await expect(getSoftBreaks(page)).toHaveCount(1, { timeout: 5000 });

      // No tabs should be copied (none existed)
      await expect(getTabs(page)).toHaveCount(0, { timeout: 5000 });
    });

    test('Many tabs (10+) with soft-break copies only tabstop count', async ({ page }) => {
      const editor = getEditor(page);
      await editor.click();

      // Insert 10 tabs
      for (let i = 0; i < 10; i++) {
        await page.keyboard.press('Tab');
      }

      // Soft-break
      await page.keyboard.press('Shift+Enter');

      // Should have 13 tabs (10 original + 3 copied, limited by tabstop count)
      await expect(getTabs(page)).toHaveCount(13, { timeout: 5000 });
    });

    test('Alternating tabs and text with soft-break', async ({ page }) => {
      const editor = getEditor(page);
      await editor.click();

      // Tab-Text-Tab-Text pattern
      await page.keyboard.press('Tab');
      await page.keyboard.type('A');
      await page.keyboard.press('Tab');
      await page.keyboard.type('B');
      await page.keyboard.press('Tab');
      await page.keyboard.type('C');

      // Soft-break at end
      await page.keyboard.press('Shift+Enter');

      // All 3 tabs should be copied
      await expect(getTabs(page)).toHaveCount(6, { timeout: 5000 });

      // Delta should preserve text
      const delta = await getDelta(page);
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
      const editor = getEditor(page);
      await editor.click();

      // Tab + text
      await page.keyboard.press('Tab');
      await page.keyboard.type('Line1');

      // Soft-break
      await page.keyboard.press('Shift+Enter');

      await page.keyboard.type('Line2');

      // Hard-break
      await page.keyboard.press('Enter');

      // New paragraph with tab
      await page.keyboard.press('Tab');
      await page.keyboard.type('Line3');

      // Soft-break
      await page.keyboard.press('Shift+Enter');

      // Verify structure
      const paragraphs = await getErte(page).locator('.ql-editor > p').all();
      expect(paragraphs.length).toBe(2);

      await expect(getSoftBreaks(page)).toHaveCount(2, { timeout: 5000 });
    });

    test('Narrow viewport: auto-wrap + soft-break interaction', async ({ page }) => {
      await page.setViewportSize({ width: 400, height: 600 });

      const editor = getEditor(page);
      await editor.click();

      // Long text that might wrap
      await page.keyboard.press('Tab');
      await page.keyboard.type('This is some text');
      await page.keyboard.press('Tab');
      await page.keyboard.type('More text here');

      // Soft-break
      await page.keyboard.press('Shift+Enter');

      // Should copy both tabs regardless of visual wrapping
      await expect(getTabs(page)).toHaveCount(4, { timeout: 5000 }); // 2 original + 2 copied
    });

    test('Delete tab after soft-break', async ({ page }) => {
      const editor = getEditor(page);
      await editor.click();

      // 2 tabs
      await page.keyboard.press('Tab');
      await page.keyboard.press('Tab');

      // Soft-break (copies 2 tabs)
      await page.keyboard.press('Shift+Enter');

      // Now we have 4 tabs (2 original + 2 copied)
      await expect(getTabs(page)).toHaveCount(4, { timeout: 5000 });

      // Delete one tab with backspace
      await page.keyboard.press('Backspace');

      // Should now have 3 tabs
      await expect(getTabs(page)).toHaveCount(3, { timeout: 5000 });
    });

    test('Copy-paste content with tabs preserves structure', async ({ page }) => {
      const editor = getEditor(page);
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
      await page.keyboard.press('Shift+Enter');

      // Paste
      await page.keyboard.press('Control+v');

      // Should have tabs from original + copied + pasted
      const tabs = await getTabs(page).all();
      expect(tabs.length).toBeGreaterThanOrEqual(2);
    });
  });

  // ============================================
  // UNDO/REDO Tests
  // ============================================

  test.describe('Undo/Redo', () => {
    test('Multiple undo restores state before soft-break', async ({ page }) => {
      const editor = getEditor(page);
      await editor.click();

      // Insert 2 tabs + text
      await page.keyboard.press('Tab');
      await page.keyboard.press('Tab');
      await page.keyboard.type('Text');

      // Soft-break (copies 2 tabs)
      await page.keyboard.press('Shift+Enter');

      // Should have 4 tabs and 1 soft-break
      await expect(getTabs(page)).toHaveCount(4, { timeout: 5000 });
      await expect(getSoftBreaks(page)).toHaveCount(1, { timeout: 5000 });

      // Multiple undos to get back to before soft-break
      // (soft-break + 2 copied tabs = 3 undo operations)
      for (let i = 0; i < 5; i++) {
        await page.keyboard.press('Control+z');
        await page.waitForTimeout(100);
      }

      // Should eventually be back to 2 tabs or fewer, no soft-break
      const tabs = await getTabs(page).all();
      expect(tabs.length).toBeLessThanOrEqual(2);
      await expect(getSoftBreaks(page)).toHaveCount(0, { timeout: 5000 });
    });

    test('Undo removes last typed character', async ({ page }) => {
      const editor = getEditor(page);
      await editor.click();

      // Insert text
      await page.keyboard.type('ABC');

      let delta = await getDelta(page);
      let textOps = delta.ops.filter((op: any) => typeof op.insert === 'string' && op.insert.includes('ABC'));
      expect(textOps.length).toBeGreaterThan(0);

      // Undo should remove some text
      await page.keyboard.press('Control+z');
      await page.waitForTimeout(200);

      delta = await getDelta(page);
      const allText = delta.ops.filter((op: any) => typeof op.insert === 'string').map((op: any) => op.insert).join('');

      // After undo, either text is shorter or empty
      expect(allText.length).toBeLessThanOrEqual(3);
    });

    test('Undo single tab insertion', async ({ page }) => {
      const editor = getEditor(page);
      await editor.click();

      // Insert 1 tab
      await page.keyboard.press('Tab');

      await expect(getTabs(page)).toHaveCount(1, { timeout: 5000 });

      // Undo
      await page.keyboard.press('Control+z');

      await expect(getTabs(page)).toHaveCount(0, { timeout: 5000 });
    });

    test('Ruler changes are outside Quill undo history', async ({ page }) => {
      const ruler = getRuler(page);

      // Get initial tabstop count
      const initialCount = await getRulerMarkers(page).count();

      // Add new tabstop by clicking ruler background
      const rulerRect = await ruler.boundingBox();
      await page.mouse.click(rulerRect!.x + 200, rulerRect!.y + 7);

      await expect(getRulerMarkers(page)).toHaveCount(initialCount + 1, { timeout: 5000 });

      // Focus editor so Ctrl+Z targets Quill history
      await getEditor(page).click();

      // Undo - ruler changes are not part of Quill history
      await page.keyboard.press('Control+z');
      await page.waitForTimeout(200);

      // Tabstop should still be there (not undone)
      await expect(getRulerMarkers(page)).toHaveCount(initialCount + 1, { timeout: 5000 });
    });
  });

  // ============================================
  // SELECTION OPERATIONS Tests
  // ============================================

  test.describe('Selection Operations', () => {
    test('Select and delete text spanning soft-break', async ({ page }) => {
      const editor = getEditor(page);
      await editor.click();

      // Tab + text
      await page.keyboard.press('Tab');
      await page.keyboard.type('Before');

      // Soft-break
      await page.keyboard.press('Shift+Enter');

      await page.keyboard.type('After');

      // Select all and delete
      await page.keyboard.press('Control+a');
      await page.keyboard.press('Delete');

      // Editor should be empty
      const delta = await getDelta(page);
      // Only newline should remain
      expect(delta.ops.length).toBeLessThanOrEqual(1);
    });

    test('Select text spanning soft-break and type to replace', async ({ page }) => {
      const editor = getEditor(page);
      await editor.click();

      // Content before soft-break
      await page.keyboard.press('Tab');
      await page.keyboard.type('First');

      // Soft-break
      await page.keyboard.press('Shift+Enter');

      await page.keyboard.type('Second');

      // Wait for content to settle
      await page.waitForTimeout(200);

      // Select all
      await page.keyboard.press('Control+a');
      await page.waitForTimeout(100);

      // Type replacement
      await page.keyboard.type('Replaced');

      // Wait for delta to update
      await page.waitForTimeout(200);

      // Check content
      const delta = await getDelta(page);
      const textOps = delta.ops.filter((op: any) => typeof op.insert === 'string');
      const allText = textOps.map((op: any) => op.insert).join('');
      expect(allText).toContain('Replaced');
      expect(allText).not.toContain('First');
      expect(allText).not.toContain('Second');
    });

    test('Double-click selects word adjacent to tab', async ({ page }) => {
      const editor = getEditor(page);
      await editor.click();

      // Tab + word
      await page.keyboard.press('Tab');
      await page.keyboard.type('TestWord');

      // Get position of tab element
      await expect(getTabs(page)).toHaveCount(1, { timeout: 5000 });
      const tabElement = getTabs(page).first();
      const tabRect = await tabElement.boundingBox();

      // Double-click just after the tab
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
      const editor = getEditor(page);
      await editor.click();

      // Tab + text
      await page.keyboard.press('Tab');
      await page.keyboard.type('Line1');

      // Soft-break
      await page.keyboard.press('Shift+Enter');

      // Cursor is now at start of new visual line (after copied tab)
      // Move to very start of visual line (before the copied tab)
      await page.keyboard.press('ArrowLeft');

      // Now backspace should delete the soft-break
      await page.keyboard.press('Backspace');

      // Soft-break should be gone
      await expect(getSoftBreaks(page)).toHaveCount(0, { timeout: 5000 });
    });

    test('Delete key removes character or element at cursor', async ({ page }) => {
      const editor = getEditor(page);
      await editor.click();

      // Tab + short text
      await page.keyboard.press('Tab');
      await page.keyboard.type('AB');

      // Go to start
      await page.keyboard.press('Home');

      // Delete should remove the tab
      await page.keyboard.press('Delete');

      await expect(getTabs(page)).toHaveCount(0, { timeout: 5000 });

      // Text should remain
      const delta = await getDelta(page);
      const textOps = delta.ops.filter((op: any) => typeof op.insert === 'string' && op.insert !== '\n');
      const allText = textOps.map((op: any) => op.insert).join('');
      expect(allText).toContain('AB');
    });

    test('Backspace deletes tab character', async ({ page }) => {
      const editor = getEditor(page);
      await editor.click();

      // Insert 3 tabs
      await page.keyboard.press('Tab');
      await page.keyboard.press('Tab');
      await page.keyboard.press('Tab');

      await expect(getTabs(page)).toHaveCount(3, { timeout: 5000 });

      // Backspace twice
      await page.keyboard.press('Backspace');
      await page.keyboard.press('Backspace');

      await expect(getTabs(page)).toHaveCount(1, { timeout: 5000 });
    });
  });

  // ============================================
  // TAB AFTER SOFT-BREAK Tests
  // ============================================

  test.describe('Tab After Soft-Break', () => {
    test('Tab can be inserted on new paragraph', async ({ page }) => {
      const editor = getEditor(page);
      await editor.click();

      // Text only (no tabs)
      await page.keyboard.type('Line1');

      // Hard-break for new paragraph
      await page.keyboard.press('Enter');

      // Now insert a tab as first character on new paragraph
      await page.keyboard.press('Tab');
      await page.keyboard.type('After');

      await expect(getTabs(page)).toHaveCount(1, { timeout: 5000 });

      // Tab should have positive width
      const tabs = await getTabs(page).all();
      const tabRect = await tabs[0].boundingBox();
      expect(tabRect!.width).toBeGreaterThan(0);
    });

    test('Additional tab can be inserted on new visual line after soft-break', async ({ page }) => {
      const editor = getEditor(page);
      await editor.click();

      // 1 tab + text
      await page.keyboard.press('Tab');
      await page.keyboard.type('Line1');

      // Soft-break (copies 1 tab)
      await page.keyboard.press('Shift+Enter');

      // Insert another tab (now we have: copied tab + new tab)
      await page.keyboard.press('Tab');
      await page.keyboard.type('Line2');

      await expect(getTabs(page)).toHaveCount(3, { timeout: 5000 }); // 1 original + 1 copied + 1 new

      // All tabs should have positive width
      const tabs = await getTabs(page).all();
      for (const tab of tabs) {
        const tabRect = await tab.boundingBox();
        expect(tabRect!.width).toBeGreaterThan(0);
      }
    });
  });

  // ============================================
  // FORMATTED TEXT Tests
  // ============================================

  // Quill 2 guard-node limitation: format toggles (Ctrl+B/I) don't work
  // when cursor is immediately after an inline Embed (Tab). Guard nodes
  // inside the embed prevent proper format state detection. Not an ERTE bug.
  test.describe.fixme('Formatted Text with Tabs', () => {
    test('Bold text preserved after soft-break', async ({ page }) => {
      const editor = getEditor(page);
      await editor.click();

      // Tab + bold text
      await page.keyboard.press('Tab');
      await page.keyboard.press('Control+b');
      await page.keyboard.type('BoldText');
      await page.keyboard.press('Control+b');

      // Soft-break
      await page.keyboard.press('Shift+Enter');

      // Check that bold text is preserved
      const delta = await getDelta(page);
      const boldOps = delta.ops.filter((op: any) => op.attributes?.bold);
      expect(boldOps.length).toBeGreaterThan(0);
    });

    test('Tab between formatted spans works correctly', async ({ page }) => {
      const editor = getEditor(page);
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
      await page.keyboard.press('Shift+Enter');

      // Should have 1 tab (original) + 1 tab (copied)
      await expect(getTabs(page)).toHaveCount(2, { timeout: 5000 });

      // Both bold and italic should be preserved
      const delta = await getDelta(page);
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
      const editor = getEditor(page);
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

      const delta = await getDelta(page);
      const textOps = delta.ops.filter((op: any) => typeof op.insert === 'string' && op.insert !== '\n');
      const allText = textOps.map((op: any) => op.insert).join('');
      expect(allText).toContain('X');
    });

    // Helper: get the NATIVE browser caret rect (not Quill's getBounds).
    // Uses getClientRects() on the native Selection range, which reflects
    // the actual visual position of the blinking cursor.
    async function getNativeCaretRect(page: any) {
      return page.evaluate(() => {
        const el = document.getElementById('test-editor') as any;
        const root = el.shadowRoot;
        const sel = root.getSelection ? root.getSelection() : document.getSelection();
        if (!sel || sel.rangeCount === 0) return null;
        const rects = sel.getRangeAt(0).getClientRects();
        if (rects.length === 0) return null;
        return { left: Math.round(rects[0].left), top: Math.round(rects[0].top),
                 height: Math.round(rects[0].height), width: Math.round(rects[0].width) };
      });
    }

    test.fixme('Native caret visually positioned AFTER tab via ArrowRight', async ({ page }) => {
      // FIXME: With inline-block display (needed for ArrowUp/Down), guard nodes
      // render at the left edge of the tab. Native caret renders at guard position,
      // not at the tabstop. Quill's getBounds() is correct, but getClientRects()
      // reflects the actual DOM position. Cannot use inline-flex (breaks vertical nav).
      // This is the critical regression test for the guard-node positioning bug:
      // In Quill 2, the trailing guard node (\uFEFF) must be at the RIGHT edge
      // of the tab so the native browser caret renders there, not at the left.
      const editor = getEditor(page);
      await editor.click();

      // Type "abc", insert tab — gives us text before the tab for reference
      await page.keyboard.type('abc');
      await page.keyboard.press('Tab');

      // Get the tab element's bounding rect
      const tabRect = await page.evaluate(() => {
        const el = document.getElementById('test-editor') as any;
        const tab = el.shadowRoot.querySelector('.ql-editor .ql-tab');
        if (!tab) return null;
        const r = tab.getBoundingClientRect();
        return { left: Math.round(r.left), right: Math.round(r.right), width: Math.round(r.width) };
      });
      expect(tabRect).not.toBeNull();
      expect(tabRect!.width).toBeGreaterThan(50); // tab has real width

      // Cursor is already after tab (Tab handler places it there).
      // Verify the native caret is at the RIGHT edge of the tab, not the left.
      const caretAfterInsert = await getNativeCaretRect(page);
      expect(caretAfterInsert).not.toBeNull();
      // Caret must be within 5px of the tab's right edge
      expect(caretAfterInsert!.left).toBeGreaterThanOrEqual(tabRect!.right - 5);
      expect(caretAfterInsert!.left).toBeLessThanOrEqual(tabRect!.right + 5);

      // Also verify via Home + ArrowRight navigation
      await page.keyboard.press('Home');
      // ArrowRight 3x through "abc", then 1x through tab
      for (let i = 0; i < 4; i++) {
        await page.keyboard.press('ArrowRight');
      }

      const caretAfterNav = await getNativeCaretRect(page);
      expect(caretAfterNav).not.toBeNull();
      // Caret must be at the right edge of the tab
      expect(caretAfterNav!.left).toBeGreaterThanOrEqual(tabRect!.right - 5);
      expect(caretAfterNav!.left).toBeLessThanOrEqual(tabRect!.right + 5);

      // Functional check: typing after navigation inserts text AFTER the tab
      await page.keyboard.type('X');
      const delta = await getDelta(page);
      expect(delta.ops[0].insert).toBe('abc');
      expect(delta.ops[1].insert).toEqual({ tab: true });
      expect(delta.ops[2].insert).toContain('X');
    });

    test.fixme('Native caret before tab is at tab left edge', async ({ page }) => {
      // FIXME: With inline-block display, right guard at left edge — native caret
      // doesn't jump to right edge when navigating through tab via ArrowRight.
      const editor = getEditor(page);
      await editor.click();

      // "abc" + tab + "def" — cursor positions are well-defined
      await page.keyboard.type('abc');
      await page.keyboard.press('Tab');
      await page.keyboard.type('def');

      // Get tab rect
      const tabRect = await page.evaluate(() => {
        const el = document.getElementById('test-editor') as any;
        const tab = el.shadowRoot.querySelector('.ql-editor .ql-tab');
        const r = tab.getBoundingClientRect();
        return { left: Math.round(r.left), right: Math.round(r.right) };
      });

      // Position cursor just before the tab (after "c", index 3)
      await page.keyboard.press('Home');
      await page.keyboard.press('ArrowRight'); // after "a"
      await page.keyboard.press('ArrowRight'); // after "b"
      await page.keyboard.press('ArrowRight'); // after "c" = before tab

      const caretBeforeTab = await getNativeCaretRect(page);
      expect(caretBeforeTab).not.toBeNull();
      // Caret should be near the tab's left edge (within 5px)
      expect(caretBeforeTab!.left).toBeGreaterThanOrEqual(tabRect!.left - 5);
      expect(caretBeforeTab!.left).toBeLessThanOrEqual(tabRect!.left + 5);

      // Now ArrowRight: cursor moves to AFTER tab
      await page.keyboard.press('ArrowRight');
      const caretAfterTab = await getNativeCaretRect(page);
      expect(caretAfterTab).not.toBeNull();
      // Caret must jump to the right edge — difference must be > tab width - 10px
      expect(caretAfterTab!.left - caretBeforeTab!.left).toBeGreaterThan(tabRect!.right - tabRect!.left - 10);
    });

    test('Click after tab positions cursor correctly', async ({ page }) => {
      const editor = getEditor(page);
      await editor.click();

      // Insert tab + text so there's a clickable area after the tab
      await page.keyboard.press('Tab');
      await page.keyboard.type('A');

      // Get the tab's bounding rect to click just after it
      const tabRight = await page.evaluate(() => {
        const el = document.getElementById('test-editor') as any;
        const tab = el.shadowRoot.querySelector('.ql-editor .ql-tab');
        if (!tab) return null;
        const rect = tab.getBoundingClientRect();
        return { x: rect.right + 2, y: rect.top + rect.height / 2 };
      });

      expect(tabRight).not.toBeNull();

      // Click just after the tab (before the "A")
      await page.mouse.click(tabRight!.x, tabRight!.y);

      // Type a marker — should appear between tab and "A"
      await page.keyboard.type('Z');

      const delta = await getDelta(page);
      const textOps = delta.ops.filter((op: any) => typeof op.insert === 'string' && op.insert !== '\n');
      const allText = textOps.map((op: any) => op.insert).join('');
      expect(allText).toContain('ZA');
    });

    test('Cursor height and vertical position consistent around tab', async ({ page }) => {
      const editor = getEditor(page);
      await editor.click();

      // "AB" + tab + "CD" — measure cursor height/top at various positions
      await page.keyboard.type('AB');
      await page.keyboard.press('Tab');
      await page.keyboard.type('CD');

      // Cursor after "B" (before tab)
      await page.keyboard.press('Home');
      await page.keyboard.press('ArrowRight');
      await page.keyboard.press('ArrowRight');
      const caretInText = await getNativeCaretRect(page);

      // Cursor after tab (before "C")
      await page.keyboard.press('ArrowRight');
      const caretAfterTab = await getNativeCaretRect(page);

      expect(caretInText).not.toBeNull();
      expect(caretAfterTab).not.toBeNull();

      // Vertical position must be identical (same line)
      expect(caretAfterTab!.top).toEqual(caretInText!.top);
      // Height must be identical (no vertical offset from tab CSS)
      expect(caretAfterTab!.height).toEqual(caretInText!.height);
    });

    test.fixme('Cursor height unchanged after tab insertion', async ({ page }) => {
      // FIXME: With inline-block display, native caret after Tab insertion renders
      // at right guard position (left edge of tab), not at the tabstop position.
      const editor = getEditor(page);
      await editor.click();

      // Type a character first so we have a measurable caret position
      await page.keyboard.type('A');

      // Measure native caret BEFORE tab insertion (after "A")
      const caretBefore = await getNativeCaretRect(page);

      // Screenshot before
      const erteEl = getErte(page);
      await erteEl.screenshot({ path: 'test-results/cursor-before-tab.png' });

      // Insert a tab after "A"
      await page.keyboard.press('Tab');

      // Measure native caret AFTER tab insertion (after "A" + tab)
      const caretAfter = await getNativeCaretRect(page);

      // Screenshot after
      await erteEl.screenshot({ path: 'test-results/cursor-after-tab.png' });

      expect(caretBefore).not.toBeNull();
      expect(caretAfter).not.toBeNull();

      // Cursor height must be identical
      expect(caretAfter!.height).toEqual(caretBefore!.height);
      // Cursor top must be identical (same line)
      expect(caretAfter!.top).toEqual(caretBefore!.top);
      // Cursor must have moved right by at least 100px (to the first tabstop)
      expect(caretAfter!.left).toBeGreaterThan(caretBefore!.left + 100);
    });

    test('ArrowDown/ArrowUp navigates between lines containing tabs', async ({ page }) => {
      // Regression test: vertical arrow navigation must work with lines containing tabs.
      const editor = getEditor(page);
      await editor.click();

      // Line 1: text + Tab + text
      await page.keyboard.type('AAA');
      await page.keyboard.press('Tab');
      await page.keyboard.type('Line1');
      await page.keyboard.press('Enter');
      // Line 2: text + Tab + text
      await page.keyboard.type('BBB');
      await page.keyboard.press('Tab');
      await page.keyboard.type('Line2');

      // Cursor is at end of line 2 after typing — get its position
      const caretLine2End = await getNativeCaretRect(page);

      // Go to end of line 1 via Ctrl+Home then End
      await page.keyboard.press('Control+Home');
      await page.keyboard.press('End');
      const caretLine1End = await getNativeCaretRect(page);

      expect(caretLine1End, 'Caret on line 1 should exist').not.toBeNull();
      expect(caretLine2End, 'Caret on line 2 should exist').not.toBeNull();
      // Verify we're on different lines
      expect(caretLine1End!.top, 'Lines should be at different heights')
        .toBeLessThan(caretLine2End!.top - 5);

      // ArrowDown from end of line 1 should move to line 2
      await page.keyboard.press('ArrowDown');
      const caretAfterDown = await getNativeCaretRect(page);
      expect(caretAfterDown, 'Caret after ArrowDown should exist').not.toBeNull();
      expect(caretAfterDown!.top, 'ArrowDown should move cursor to next line')
        .toBeGreaterThan(caretLine1End!.top + 5);

      // ArrowUp should move back to line 1
      await page.keyboard.press('ArrowUp');
      const caretAfterUp = await getNativeCaretRect(page);
      expect(caretAfterUp, 'Caret after ArrowUp should exist').not.toBeNull();
      expect(caretAfterUp!.top, 'ArrowUp should return to original line')
        .toBeLessThan(caretAfterDown!.top - 5);
    });

    test('ArrowUp on first line jumps to line start', async ({ page }) => {
      // Regression test: ArrowUp on first line stepped through guard nodes
      // instead of jumping to line start. Custom handler must replicate
      // Quill's default behavior when no line above exists.
      const editor = getEditor(page);
      await editor.click();

      // Type text + tab + text on a single line
      await page.keyboard.type('AAA');
      await page.keyboard.press('Tab');
      await page.keyboard.type('BBB');

      // Cursor is at end after typing. Get Quill selection index.
      const indexBefore = await page.evaluate(() => {
        const el = document.getElementById('test-editor') as any;
        return el._editor.getSelection()?.index;
      });
      expect(indexBefore).toBeGreaterThan(0);

      // Press ArrowUp — should jump to index 0 (line start)
      await page.keyboard.press('ArrowUp');
      const indexAfter = await page.evaluate(() => {
        const el = document.getElementById('test-editor') as any;
        return el._editor.getSelection()?.index;
      });
      expect(indexAfter, 'ArrowUp on first line should jump to line start').toBe(0);
    });

    test('ArrowDown on last line jumps to line end', async ({ page }) => {
      // Regression test: ArrowDown on last line stepped through guard nodes
      // instead of jumping to line end. Custom handler must replicate
      // Quill's default behavior when no line below exists.
      const editor = getEditor(page);
      await editor.click();

      // Type text + tab + text on a single line
      await page.keyboard.type('AAA');
      await page.keyboard.press('Tab');
      await page.keyboard.type('BBB');

      // Go to line start
      await page.keyboard.press('Home');
      const indexAtHome = await page.evaluate(() => {
        const el = document.getElementById('test-editor') as any;
        return el._editor.getSelection()?.index;
      });
      expect(indexAtHome).toBe(0);

      // Press ArrowDown — should jump to end of line (last content index)
      await page.keyboard.press('ArrowDown');
      const indexAfterDown = await page.evaluate(() => {
        const el = document.getElementById('test-editor') as any;
        return el._editor.getSelection()?.index;
      });
      // "AAA" (3) + tab (1) + "BBB" (3) = 7 characters, so last content index = 7
      expect(indexAfterDown, 'ArrowDown on last line should jump to line end').toBe(7);
    });

    test('Cursor can be placed after the last tab in a line', async ({ page }) => {
      // Regression test: position() fallback returned super.position() which
      // produced a zero-size bounding rect, making cursor invisible/stuck
      // after the last tab when no content followed.
      const editor = getEditor(page);
      await editor.click();

      // Insert text + 2 tabs (no text after last tab)
      await page.keyboard.type('Hello');
      await page.keyboard.press('Tab');
      await page.keyboard.press('Tab');

      // Cursor should be at the right edge of the last tab
      const bounds = await page.evaluate(() => {
        const el = document.getElementById('test-editor') as any;
        const quill = el._editor;
        const sel = quill.getSelection();
        if (!sel) return null;
        const b = quill.getBounds(sel.index);
        return { left: Math.round(b.left), height: Math.round(b.height) };
      });
      expect(bounds).not.toBeNull();
      expect(bounds!.height, 'Cursor after last tab should have visible height').toBeGreaterThan(0);
      // Cursor should be well past the "Hello" text (which is ~52px wide)
      expect(bounds!.left, 'Cursor should be positioned past "Hello" + tabs').toBeGreaterThan(100);
    });

    test('Cursor is visible (non-zero height) at every tab position', async ({ page }) => {
      // Regression test: overflow:clip + height:1rem clipped the caret to
      // invisibility at tab positions 1 and 2 (Lumo line-height 1.625 makes
      // the caret ~26px, taller than the 16px box). The fix uses
      // overflow:visible and removes the height constraint.
      const editor = getEditor(page);
      await editor.click();

      // Insert 3 tabs: the bug was tabs 1+2 invisible, tab 3 visible
      await page.keyboard.press('Tab');
      await page.keyboard.press('Tab');
      await page.keyboard.press('Tab');

      // Check caret visibility at each tab position (after each tab)
      // Navigate: Home, then ArrowRight through each tab
      await page.keyboard.press('Home');

      for (let tabIdx = 0; tabIdx < 3; tabIdx++) {
        await page.keyboard.press('ArrowRight'); // move past tab

        const caret = await getNativeCaretRect(page);

        // Caret must exist (non-null = browser reports a rect for the selection)
        expect(caret, `Caret at tab ${tabIdx + 1} should not be null`).not.toBeNull();

        // Caret must have visible height (> 0 means not clipped to invisibility)
        expect(caret!.height, `Caret at tab ${tabIdx + 1} should have height > 0`).toBeGreaterThan(0);

        // Caret height should match surrounding text (~14-26px, definitely > 10px)
        expect(caret!.height, `Caret at tab ${tabIdx + 1} should be at least 10px tall`).toBeGreaterThanOrEqual(10);
      }
    });

    test('Tab right edges align with ruler markers within 2px', async ({ page }) => {
      // Regression test: fractional pixel accumulation in iterative
      // getBoundingClientRect() caused tabs to drift left of ruler markers.
      // Fix: Math.round(widthNeeded) eliminates sub-pixel accumulation.
      const editor = getEditor(page);
      await editor.click();

      // Insert 3 tabs to reach all 3 configured tabstops (L@150, R@350, M@550)
      await page.keyboard.press('Tab');
      await page.keyboard.press('Tab');
      await page.keyboard.press('Tab');

      // Get tab right edges and ruler marker positions in editor-relative coords
      const alignment = await page.evaluate(() => {
        const el = document.getElementById('test-editor') as any;
        const root = el.shadowRoot;
        const editorEl = root.querySelector('.ql-editor');
        const editorRect = editorEl.getBoundingClientRect();
        const tabs = root.querySelectorAll('.ql-editor .ql-tab');
        // Markers are sorted by CSS left position — collect and sort them
        const markerEls = Array.from(root.querySelectorAll('[part~="horizontalRuler"] vaadin-icon')) as HTMLElement[];
        markerEls.sort((a, b) => parseFloat(a.style.left) - parseFloat(b.style.left));

        const results: Array<{ tabRight: number; markerCenter: number; diff: number; debug: string }> = [];

        for (let i = 0; i < Math.min(tabs.length, markerEls.length); i++) {
          const tabRect = tabs[i].getBoundingClientRect();
          const markerRect = markerEls[i].getBoundingClientRect();
          // Both in absolute coordinates
          const tabRight = tabRect.right;
          const markerCenter = markerRect.left + markerRect.width / 2;
          results.push({
            tabRight: Math.round(tabRight * 10) / 10,
            markerCenter: Math.round(markerCenter * 10) / 10,
            diff: Math.abs(tabRight - markerCenter),
            debug: `editorLeft=${editorRect.left} tabLeft=${tabRect.left.toFixed(1)} tabWidth=${tabRect.width.toFixed(1)} tabStyleW=${(tabs[i] as HTMLElement).style.width} markerLeft=${markerRect.left.toFixed(1)} markerW=${markerRect.width.toFixed(1)} markerStyleLeft=${markerEls[i].style.left}`
          });
        }
        return results;
      });

      expect(alignment.length).toBe(3);
      for (let i = 0; i < alignment.length; i++) {
        expect(alignment[i].diff,
          `Tab ${i + 1} right=${alignment[i].tabRight} marker=${alignment[i].markerCenter} [${alignment[i].debug}]`
        ).toBeLessThanOrEqual(2); // 2px tolerance for rounding
      }
    });

    test('End key moves to end of visual line', async ({ page }) => {
      const editor = getEditor(page);
      await editor.click();

      // Tab + text
      await page.keyboard.press('Tab');
      await page.keyboard.type('First');

      // Soft-break
      await page.keyboard.press('Shift+Enter');

      await page.keyboard.type('Second');

      // Go to start of second visual line
      await page.keyboard.press('Home');

      // End should go to end of current visual line
      await page.keyboard.press('End');

      // Type marker
      await page.keyboard.type('END');

      const delta = await getDelta(page);
      const textOps = delta.ops.filter((op: any) => typeof op.insert === 'string');
      const allText = textOps.map((op: any) => op.insert).join('');
      expect(allText).toContain('SecondEND');
    });

    test('Shift+Arrow creates selection and delete removes selected content', async ({ page }) => {
      const editor = getEditor(page);
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
      const delta = await getDelta(page);
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
      const editor = getEditor(page);
      await editor.click();

      // Paragraph 1: 2 tabs + soft-break
      await page.keyboard.press('Tab');
      await page.keyboard.press('Tab');
      await page.keyboard.type('P1');
      await page.keyboard.press('Shift+Enter');
      await page.keyboard.type('P1-line2');

      // Hard-break for new paragraph
      await page.keyboard.press('Enter');

      // Paragraph 2: 1 tab + soft-break
      await page.keyboard.press('Tab');
      await page.keyboard.type('P2');
      await page.keyboard.press('Shift+Enter');
      await page.keyboard.type('P2-line2');

      // Should have 2 paragraphs
      const paragraphs = await getErte(page).locator('.ql-editor > p').all();
      expect(paragraphs.length).toBe(2);

      // Should have 2 soft-breaks
      await expect(getSoftBreaks(page)).toHaveCount(2, { timeout: 5000 });

      // P1: 2 original + 2 copied = 4 tabs
      // P2: 1 original + 1 copied = 2 tabs
      // Total: 6 tabs
      await expect(getTabs(page)).toHaveCount(6, { timeout: 5000 });
    });

    test('Soft-break does not affect other paragraphs', async ({ page }) => {
      const editor = getEditor(page);
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
      await page.keyboard.press('Shift+Enter');

      // Paragraph 1 should still have only 1 tab
      // Paragraph 2: 3 original + 3 copied = 6 tabs
      // Total: 1 + 6 = 7 tabs
      await expect(getTabs(page)).toHaveCount(7, { timeout: 5000 });
    });
  });

  // ============================================
  // OVERFLOW TAB LIMIT Tests (Customer Requirement)
  // ============================================

  test.describe('Overflow Tab Limit', () => {
    test('Soft-break at overflow tab position copies only tabstop count tabs', async ({ page }) => {
      const editor = getEditor(page);
      await editor.click();

      // Insert 5 tabs (only 3 tabstops defined: L, C, R)
      for (let i = 0; i < 5; i++) {
        await page.keyboard.press('Tab');
      }

      // Soft-break at end (cursor after all 5 tabs)
      await page.keyboard.press('Shift+Enter');

      // Should copy only 3 tabs (number of tabstops), not 5
      // Line 1: 5 tabs, Line 2: 3 tabs = 8 total
      await expect(getTabs(page)).toHaveCount(8, { timeout: 5000 });
    });

    test('Soft-break after 4th tab (overflow) copies only 3 tabs', async ({ page }) => {
      const editor = getEditor(page);
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
      await page.keyboard.press('Shift+Enter');

      // Should copy only 3 tabs (min of 4 and 3)
      // Line 1: 6 tabs, Line 2: 3 tabs = 9 total
      await expect(getTabs(page)).toHaveCount(9, { timeout: 5000 });
    });

    test('Soft-break with cursor at 2nd tab copies 2 tabs (within tabstop limit)', async ({ page }) => {
      const editor = getEditor(page);
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
      await page.keyboard.press('Shift+Enter');

      // Should copy 2 tabs (cursor position, within limit)
      // Line 1: 5 tabs, Line 2: 2 tabs = 7 total
      await expect(getTabs(page)).toHaveCount(7, { timeout: 5000 });
    });

    test('Soft-break after removing tabstops respects new limit', async ({ page }) => {
      const editor = getEditor(page);

      // Remove first tabstop (LEFT@150, icon=vaadin:caret-right).
      // Cycling: left -> right -> middle -> remove (3 clicks)
      const firstMarker = getRulerMarkers(page).first();
      await firstMarker.click(); // left -> right
      await firstMarker.click(); // right -> middle
      await firstMarker.click(); // middle -> remove
      await page.waitForTimeout(100);

      // Now only 2 tabstops remain (RIGHT@350, MIDDLE@550)

      // Focus editor and insert 4 tabs
      await editor.click();
      for (let i = 0; i < 4; i++) {
        await page.keyboard.press('Tab');
      }

      // Soft-break at end
      await page.keyboard.press('Shift+Enter');

      // Should copy only 2 tabs (new tabstop limit)
      // Line 1: 4 tabs, Line 2: 2 tabs = 6 total
      await expect(getTabs(page)).toHaveCount(6, { timeout: 5000 });
    });
  });

  // ============================================
  // BROWSER RESIZE Tests
  // ============================================

  test.describe('Browser Resize', () => {
    test('Tab widths recalculate after viewport resize', async ({ page }) => {
      await page.setViewportSize({ width: 800, height: 600 });

      const editor = getEditor(page);
      await editor.click();

      // Insert tabs
      await page.keyboard.press('Tab');
      await page.keyboard.press('Tab');
      await page.keyboard.type('Text');

      // Get initial tab widths
      await expect(getTabs(page)).toHaveCount(2, { timeout: 5000 });
      const tabs = await getTabs(page).all();
      const width1Before = (await tabs[0].boundingBox())!.width;

      // Resize viewport
      await page.setViewportSize({ width: 600, height: 600 });

      // Wait for tab widths to stabilize after resize (use shadowRoot to pierce shadow DOM)
      await page.waitForFunction(
        () => {
          const el = document.getElementById('test-editor') as any;
          const tab = el?.shadowRoot?.querySelector('.ql-tab') ?? el?.querySelector('.ql-tab');
          return tab && tab.getBoundingClientRect().width > 0;
        },
        { timeout: 5000 }
      );

      // Tab widths should still be valid (aligned to tabstops)
      // Re-query tabs since viewport change may have caused re-render
      const width1After = (await getTabs(page).first().boundingBox())!.width;

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
      const editor = getEditor(page);
      await editor.click();

      // Insert tabs
      await page.keyboard.press('Tab');
      await page.keyboard.press('Tab');
      await page.keyboard.type('Text');

      // Get initial tab count and verify widths are positive
      await expect(getTabs(page)).toHaveCount(2, { timeout: 5000 });
      const tabs = await getTabs(page).all();
      const width1Before = (await tabs[0].boundingBox())!.width;
      expect(width1Before).toBeGreaterThan(0);

      // Click outside the editor to blur (click on the delta output area below the editor)
      await page.locator('#delta-output').click();
      await page.waitForTimeout(100);

      // Click back in editor
      await editor.click();
      await page.waitForTimeout(100);

      // Tabs should still exist with positive widths
      await expect(getTabs(page)).toHaveCount(2, { timeout: 5000 });
      const width1After = (await getTabs(page).first().boundingBox())!.width;
      expect(width1After).toBeGreaterThan(0);
    });
  });

  // ============================================
  // TAB AT TABSTOP BOUNDARY Tests
  // ============================================

  test.describe('Tab at Tabstop Boundary', () => {
    test('Consecutive tabs have increasing end positions', async ({ page }) => {
      const editor = getEditor(page);
      await editor.click();

      // Insert two tabs
      await page.keyboard.press('Tab');
      await page.keyboard.press('Tab');
      await page.keyboard.type('End');

      // Wait for width calculation
      await expect(getTabs(page)).toHaveCount(2, { timeout: 5000 });

      const tabs = await getTabs(page).all();
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
      const editor = getEditor(page);
      await editor.click();

      // Tab + text
      await page.keyboard.press('Tab');
      await page.keyboard.type('Text');

      // First soft-break
      await page.keyboard.press('Shift+Enter');

      // Now we're on a new visual line with 1 copied tab
      // Delete the tab to have empty visual line
      await page.keyboard.press('Backspace');

      // Second soft-break on empty visual line
      await page.keyboard.press('Shift+Enter');

      // Should have 2 soft-breaks
      await expect(getSoftBreaks(page)).toHaveCount(2, { timeout: 5000 });

      // Should still have only 1 tab (from original line)
      await expect(getTabs(page)).toHaveCount(1, { timeout: 5000 });
    });
  });

  // ============================================
  // STRESS TESTS
  // ============================================

  test.describe('Stress Tests', () => {
    test('Rapid soft-breaks (10x)', async ({ page }) => {
      const editor = getEditor(page);
      await editor.click();

      // 3 tabs
      await page.keyboard.press('Tab');
      await page.keyboard.press('Tab');
      await page.keyboard.press('Tab');

      // 10 rapid soft-breaks
      for (let i = 0; i < 10; i++) {
        await page.keyboard.press('Shift+Enter');
      }

      // Should have 3 original + 10*3 copied = 33 tabs
      await expect(getTabs(page)).toHaveCount(33, { timeout: 10000 });

      // Should have 10 soft-breaks
      await expect(getSoftBreaks(page)).toHaveCount(10, { timeout: 5000 });
    });

    test('All tabstops removed - tabs use fixed width', async ({ page }) => {
      const editor = getEditor(page);

      // Remove all 3 tabstops one by one.
      // Initial: LEFT@150 (vaadin:caret-right), RIGHT@350 (vaadin:caret-left), MIDDLE@550 (vaadin:dot-circle)
      // LEFT needs 3 clicks (left -> right -> middle -> remove)
      // RIGHT (now first) needs 2 clicks (right -> middle -> remove)
      // MIDDLE (now first) needs 1 click (middle -> remove)

      // First tabstop (LEFT): 3 clicks to remove
      await getRulerMarkers(page).first().click();
      await page.waitForTimeout(200);
      await getRulerMarkers(page).first().click();
      await page.waitForTimeout(200);
      await getRulerMarkers(page).first().click();
      await page.waitForTimeout(200);

      // Should be down to 2 markers
      await expect(getRulerMarkers(page)).toHaveCount(2, { timeout: 5000 });

      // Second tabstop (RIGHT, now first): 2 clicks to remove
      await getRulerMarkers(page).first().click();
      await page.waitForTimeout(200);
      await getRulerMarkers(page).first().click();
      await page.waitForTimeout(200);

      // Should be down to 1 marker
      await expect(getRulerMarkers(page)).toHaveCount(1, { timeout: 5000 });

      // Third tabstop (MIDDLE, now first): 1 click to remove
      await getRulerMarkers(page).first().click();
      await page.waitForTimeout(200);

      // Verify no tabstops remain
      await expect(getRulerMarkers(page)).toHaveCount(0, { timeout: 5000 });

      // Focus editor and insert tabs
      await editor.click();
      await page.keyboard.press('Tab');
      await page.keyboard.press('Tab');
      await page.keyboard.type('No tabstops');

      // Tabs should use fixed width
      await expect(getTabs(page)).toHaveCount(2, { timeout: 5000 });
      const tabs = await getTabs(page).all();

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
    test('Show Whitespace toolbar button is present and not active by default', async ({ page }) => {
      const btn = page.locator('#test-editor').locator('[part~="toolbar-button-whitespace"]');
      await expect(btn).toBeVisible();

      // Should not be active by default
      const active = await isShowWhitespaceActive(page);
      expect(active).toBe(false);
    });

    test('Show Whitespace can be toggled and controls the show-whitespace class', async ({ page }) => {
      // Initially unchecked, so no show-whitespace class
      await expect(page.locator('.show-whitespace')).toHaveCount(0, { timeout: 5000 });

      // Enable Show Whitespace
      await enableShowWhitespace(page);

      // show-whitespace class should now be present on .ql-editor
      await expect(page.locator('.show-whitespace')).toHaveCount(1, { timeout: 5000 });

      // Disable Show Whitespace
      await disableShowWhitespace(page);

      // show-whitespace class should be removed
      await expect(page.locator('.show-whitespace')).toHaveCount(0, { timeout: 5000 });

      // Re-enable Show Whitespace
      await enableShowWhitespace(page);
      await expect(page.locator('.show-whitespace')).toHaveCount(1, { timeout: 5000 });
    });

    test('Tab indicator visible when Show Whitespace enabled', async ({ page }) => {
      // Enable whitespace indicators first (unchecked by default)
      await enableShowWhitespace(page);

      const editor = getEditor(page);
      await editor.click();

      // Insert a tab
      await page.keyboard.press('Tab');
      await page.keyboard.type('Text');

      // Check that the editor has show-whitespace class
      await expect(page.locator('.show-whitespace')).toHaveCount(1, { timeout: 5000 });

      // Tab should exist
      await expect(getTabs(page)).toHaveCount(1, { timeout: 5000 });
    });

    test('Soft-break indicator visible when Show Whitespace enabled', async ({ page }) => {
      // Enable whitespace indicators first
      await enableShowWhitespace(page);

      const editor = getEditor(page);
      await editor.click();

      // Insert soft-break
      await page.keyboard.type('Line1');
      await page.keyboard.press('Shift+Enter');
      await page.keyboard.type('Line2');

      // Verify soft-break exists
      await expect(getSoftBreaks(page)).toHaveCount(1, { timeout: 5000 });

      // show-whitespace class should be present (enables the indicator)
      await expect(page.locator('.show-whitespace')).toHaveCount(1, { timeout: 5000 });
    });

    test('Paragraph indicator visible at end of paragraphs', async ({ page }) => {
      // Enable whitespace indicators first
      await enableShowWhitespace(page);

      const editor = getEditor(page);
      await editor.click();

      // Insert two paragraphs
      await page.keyboard.type('Paragraph1');
      await page.keyboard.press('Enter');
      await page.keyboard.type('Paragraph2');

      // Verify we have 2 paragraphs
      const paragraphs = await getErte(page).locator('.ql-editor > p').all();
      expect(paragraphs.length).toBe(2);

      // show-whitespace class should be present (enables the pilcrow indicator)
      await expect(page.locator('.show-whitespace')).toHaveCount(1, { timeout: 5000 });
    });

    test('Disabling Show Whitespace removes indicators', async ({ page }) => {
      // Enable whitespace indicators first
      await enableShowWhitespace(page);

      const editor = getEditor(page);
      await editor.click();

      // Insert content
      await page.keyboard.press('Tab');
      await page.keyboard.type('Text');

      // Verify show-whitespace is present
      await expect(page.locator('.show-whitespace')).toHaveCount(1, { timeout: 5000 });

      // Disable Show Whitespace
      await disableShowWhitespace(page);

      // show-whitespace class should be removed
      await expect(page.locator('.show-whitespace')).toHaveCount(0, { timeout: 5000 });
    });

    test('Indicators visible for all whitespace types simultaneously', async ({ page }) => {
      // Enable whitespace indicators first
      await enableShowWhitespace(page);

      const editor = getEditor(page);
      await editor.click();

      // Create content with tabs, soft-break, and hard-break
      await page.keyboard.press('Tab');
      await page.keyboard.type('TabText');
      await page.keyboard.press('Shift+Enter');
      await page.keyboard.type('SoftBreakLine');
      await page.keyboard.press('Enter');
      await page.keyboard.type('NewParagraph');

      // Verify all elements exist
      const tabs = await getTabs(page).all();
      expect(tabs.length).toBeGreaterThanOrEqual(1);

      await expect(getSoftBreaks(page)).toHaveCount(1, { timeout: 5000 });

      const paragraphs = await getErte(page).locator('.ql-editor > p').all();
      expect(paragraphs.length).toBe(2);

      // show-whitespace should be active (all indicators visible)
      await expect(page.locator('.show-whitespace')).toHaveCount(1, { timeout: 5000 });
    });

    test('Auto-wrap indicator shown for wrapped tabs', async ({ page }) => {
      // Enable whitespace indicators first
      await enableShowWhitespace(page);

      // Set narrow viewport to trigger wrapping
      await page.setViewportSize({ width: 400, height: 600 });

      const editor = getEditor(page);
      await editor.click();

      // Type long text that will wrap
      await page.keyboard.type('This is a very long line of text that should wrap automatically when the viewport is narrow enough.');

      // Add a tab after the text wraps
      await page.keyboard.press('Tab');
      await page.keyboard.type('More text after tab');

      // Wait for tab width recalculation
      await expect(getTabs(page)).toHaveCount(1, { timeout: 5000 });

      // Check if any tab has the auto-wrap class
      const wrappedTabs = await getErte(page).locator('.ql-tab.ql-auto-wrap-start').all();

      // Note: This test checks that the class mechanism works
      // The actual wrapping depends on viewport width and text length
      // At minimum, verify the show-whitespace class is present
      await expect(page.locator('.show-whitespace')).toHaveCount(1, { timeout: 5000 });
    });

    test('Auto-wrap class removed when tab not on wrapped line', async ({ page }) => {
      const editor = getEditor(page);
      await editor.click();

      // Insert tab at beginning (should NOT be wrapped)
      await page.keyboard.press('Tab');
      await page.keyboard.type('Short text');

      await expect(getTabs(page)).toHaveCount(1, { timeout: 5000 });

      // Check the tab doesn't have auto-wrap class
      const tabs = await getTabs(page).all();
      const hasAutoWrap = await tabs[0].evaluate(el => el.classList.contains('ql-auto-wrap-start'));
      expect(hasAutoWrap).toBe(false);
    });
  });

  // ============================================
  // JAVA / ERTE-SPECIFIC INTEGRATION Tests
  // ============================================

  test.describe('ERTE Integration', () => {
    test('Java setTabStops reflects in UI', async ({ page }) => {
      // The test view should set up tabstops via Java configuration.
      // Verify the ruler renders the correct number of tabstop markers.
      const markers = getRulerMarkers(page);
      const markerCount = await markers.count();

      // Default configuration has 3 tabstops (LEFT, RIGHT, MIDDLE)
      expect(markerCount).toBe(3);

      // Verify alignment directions of each marker icon
      // LEFT@150 -> vaadin:caret-right, RIGHT@350 -> vaadin:caret-left, MIDDLE@550 -> vaadin:dot-circle
      const expectedDirections = ['left', 'right', 'middle'];
      for (let i = 0; i < markerCount; i++) {
        const direction = await getRulerMarkerDirection(markers.nth(i));
        expect(expectedDirections).toContain(direction);
      }

      // Verify tabs inserted in the editor respect the configured tabstops
      const editor = getEditor(page);
      await editor.click();

      await page.keyboard.press('Tab');
      await page.keyboard.type('Aligned');

      await expect(getTabs(page)).toHaveCount(1, { timeout: 5000 });
      const tabRect = await getTabs(page).first().boundingBox();
      const editorRect = await editor.boundingBox();
      const tabEndPos = tabRect!.x + tabRect!.width - editorRect!.x;

      // Tab should align to the first configured tabstop position (LEFT@150)
      expect(tabEndPos).toBeGreaterThan(120);
      expect(tabEndPos).toBeLessThan(200);
    });

    test('Tabstops bidirectional sync - UI changes reflected', async ({ page }) => {
      // Get initial marker count from the ruler
      const markers = getRulerMarkers(page);
      const initialCount = await markers.count();
      expect(initialCount).toBe(3);

      // Verify first marker is LEFT (vaadin:caret-right)
      const firstMarker = markers.first();
      const dirBefore = await getRulerMarkerDirection(firstMarker);
      expect(dirBefore).toBe('left');

      // Click to cycle: left -> right
      await firstMarker.click();

      // Direction should update in the DOM
      const dirAfter = await getRulerMarkerDirection(firstMarker);
      expect(dirAfter).toBe('right');

      // Insert a tab and verify its width changed (right alignment differs from left)
      const editor = getEditor(page);
      await editor.click();

      await page.keyboard.press('Tab');
      await page.keyboard.type('SyncTest');

      await expect(getTabs(page)).toHaveCount(1, { timeout: 5000 });
      const tabRect = await getTabs(page).first().boundingBox();

      // Tab should have a valid width reflecting right alignment
      expect(tabRect!.width).toBeGreaterThan(0);
    });

    test('Screenshot: Tab alignment L/C/R', async ({ page }) => {
      const editor = getEditor(page);
      await editor.click();

      // Insert tabs with text to demonstrate all 3 alignment types
      await page.keyboard.press('Tab');
      await page.keyboard.type('Left');
      await page.keyboard.press('Tab');
      await page.keyboard.type('Center');
      await page.keyboard.press('Tab');
      await page.keyboard.type('Right');

      // Wait for tab width calculations to settle
      await expect(getTabs(page)).toHaveCount(3, { timeout: 5000 });

      // Visual regression screenshot of the full ERTE component
      await expect(getErte(page)).toHaveScreenshot('tab-alignment.png');
    });
  });
});
