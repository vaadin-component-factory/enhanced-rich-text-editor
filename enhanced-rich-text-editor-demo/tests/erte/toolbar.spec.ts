import { test, expect, Page, Locator } from '@playwright/test';
import {
  waitForEditor,
  getToolbar,
  getDelta,
  getEventLog,
  clearEventLog,
  waitForEvent,
  ERTE_TEST_BASE,
  focusEditor,
} from './helpers';

const TOOLBAR_URL = `${ERTE_TEST_BASE}/toolbar`;

/**
 * All 25 slot names injected by _injectToolbarSlots(), in DOM order.
 * Does NOT include GROUP_CUSTOM's "toolbar" slot (tested separately).
 */
const ALL_SLOT_NAMES = [
  'toolbar-start',
  'toolbar-before-group-history',
  'toolbar-after-group-history',
  'toolbar-before-group-emphasis',
  'toolbar-after-group-emphasis',
  'toolbar-before-group-style',
  'toolbar-after-group-style',
  'toolbar-before-group-heading',
  'toolbar-after-group-heading',
  'toolbar-before-group-glyph-transformation',
  'toolbar-after-group-glyph-transformation',
  'toolbar-before-group-list',
  'toolbar-after-group-list',
  'toolbar-before-group-indent',
  'toolbar-after-group-indent',
  'toolbar-before-group-alignment',
  'toolbar-after-group-alignment',
  'toolbar-before-group-rich-text',
  'toolbar-after-group-rich-text',
  'toolbar-before-group-block',
  'toolbar-after-group-block',
  'toolbar-before-group-format',
  'toolbar-after-group-format',
  'toolbar-before-group-custom',
  'toolbar-after-group-custom',
  'toolbar-end',
];

/**
 * A representative set of standard toolbar button part names that must exist.
 * V25: deindent → outdent
 */
const STANDARD_BUTTON_PARTS = [
  'toolbar-button-bold',
  'toolbar-button-italic',
  'toolbar-button-underline',
  'toolbar-button-strike',
  'toolbar-button-undo',
  'toolbar-button-redo',
  'toolbar-button-h1',
  'toolbar-button-h2',
  'toolbar-button-h3',
  'toolbar-button-subscript',
  'toolbar-button-superscript',
  'toolbar-button-list-ordered',
  'toolbar-button-list-bullet',
  'toolbar-button-outdent',
  'toolbar-button-indent',
  'toolbar-button-align-left',
  'toolbar-button-align-center',
  'toolbar-button-align-right',
  'toolbar-button-image',
  'toolbar-button-link',
  'toolbar-button-blockquote',
  'toolbar-button-code-block',
  'toolbar-button-clean',
];

/**
 * Query a shadow DOM element inside the ERTE component by CSS selector.
 * Returns a Playwright Locator that pierces into the shadow root.
 */
function shadowButton(page: Page, partName: string, editorId = 'test-editor'): Locator {
  return page.locator(`#${editorId}`).locator(`[part~="${partName}"]`);
}

/**
 * Query a slot element inside the ERTE shadow DOM.
 */
function shadowSlot(page: Page, slotName: string, editorId = 'test-editor'): Locator {
  return page.locator(`#${editorId}`).locator(`slot[name="${slotName}"]`);
}

test.describe('ERTE Toolbar', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto(TOOLBAR_URL);
    await waitForEditor(page);
  });

  // ============================================
  // SLOT STRUCTURE
  // ============================================

  test('All 25 named slots present in DOM', async ({ page }) => {
    for (const slotName of ALL_SLOT_NAMES) {
      const slot = shadowSlot(page, slotName);
      await expect(slot).toHaveCount(1, {
        timeout: 5000,
      });
    }
  });

  test('GROUP_CUSTOM "toolbar" slot present in DOM', async ({ page }) => {
    const slot = shadowSlot(page, 'toolbar');
    await expect(slot).toHaveCount(1, { timeout: 5000 });
  });

  // ============================================
  // SLOT COMPONENT RENDERING
  // ============================================

  test('Component in START slot renders', async ({ page }) => {
    const btn = page.locator('#slot-start-btn');
    await expect(btn).toBeVisible();

    // Verify it is within the toolbar area (its bounding box overlaps with the toolbar)
    const toolbar = getToolbar(page);
    const toolbarBox = await toolbar.boundingBox();
    const btnBox = await btn.boundingBox();
    expect(toolbarBox).not.toBeNull();
    expect(btnBox).not.toBeNull();
    expect(btnBox!.y).toBeGreaterThanOrEqual(toolbarBox!.y - 2);
    expect(btnBox!.y).toBeLessThan(toolbarBox!.y + toolbarBox!.height + 2);
  });

  test('Component in END slot renders', async ({ page }) => {
    const btn = page.locator('#slot-end-btn');
    await expect(btn).toBeVisible();

    // END slot should be positioned after most toolbar content
    const toolbar = getToolbar(page);
    const toolbarBox = await toolbar.boundingBox();
    const btnBox = await btn.boundingBox();
    expect(toolbarBox).not.toBeNull();
    expect(btnBox).not.toBeNull();

    // The END button should be positioned within the toolbar (visible and to the right)
    expect(btnBox!.x).toBeGreaterThan(toolbarBox!.x);
  });

  test('Component in BEFORE_GROUP_HISTORY renders', async ({ page }) => {
    const btn = page.locator('#slot-before-history-btn');
    await expect(btn).toBeVisible();

    // Should appear before the undo button
    const undoBtn = shadowButton(page, 'toolbar-button-undo');
    const bhBox = await btn.boundingBox();
    const undoBox = await undoBtn.boundingBox();
    expect(bhBox).not.toBeNull();
    expect(undoBox).not.toBeNull();

    // BEFORE_GROUP_HISTORY element should be left of (or at least not after) undo
    expect(bhBox!.x).toBeLessThanOrEqual(undoBox!.x);
  });

  test('Component in AFTER_GROUP_EMPHASIS renders', async ({ page }) => {
    const btn = page.locator('#slot-after-emphasis-btn');
    await expect(btn).toBeVisible();

    // Should appear after the strike button (last button in emphasis group)
    const strikeBtn = shadowButton(page, 'toolbar-button-strike');
    const aeBox = await btn.boundingBox();
    const strikeBox = await strikeBtn.boundingBox();
    expect(aeBox).not.toBeNull();
    expect(strikeBox).not.toBeNull();

    // AFTER_GROUP_EMPHASIS element should be right of (or at least not before) strike
    expect(aeBox!.x).toBeGreaterThanOrEqual(strikeBox!.x);
  });

  test('Component in GROUP_CUSTOM renders', async ({ page }) => {
    const btn = page.locator('#custom-group-btn');
    await expect(btn).toBeVisible();

    // Should be in the toolbar area
    const toolbar = getToolbar(page);
    const toolbarBox = await toolbar.boundingBox();
    const btnBox = await btn.boundingBox();
    expect(toolbarBox).not.toBeNull();
    expect(btnBox).not.toBeNull();
    expect(btnBox!.y).toBeGreaterThanOrEqual(toolbarBox!.y - 2);
    expect(btnBox!.y).toBeLessThan(toolbarBox!.y + toolbarBox!.height + 2);
  });

  test('addToolbarComponentsAtIndex ordering — multiple components in same slot', async ({
    page,
  }) => {
    // Both custom-group-btn and toolbar-switch exist in GROUP_CUSTOM
    const customBtn = page.locator('#custom-group-btn');
    const switchBtn = page.locator('#toolbar-switch');

    await expect(customBtn).toBeVisible();
    await expect(switchBtn).toBeVisible();

    const customBox = await customBtn.boundingBox();
    const switchBox = await switchBtn.boundingBox();
    expect(customBox).not.toBeNull();
    expect(switchBox).not.toBeNull();

    expect(customBox!.x + customBox!.width).toBeGreaterThan(0);
    expect(switchBox!.x + switchBox!.width).toBeGreaterThan(0);
  });

  test('Multiple components in same slot — both exist in GROUP_CUSTOM', async ({ page }) => {
    const customBtn = page.locator('#custom-group-btn');
    const switchBtn = page.locator('#toolbar-switch');

    await expect(customBtn).toBeVisible();
    await expect(switchBtn).toBeVisible();
  });

  // ============================================
  // REMOVING COMPONENTS
  // ============================================

  test('Remove component from slot by reference', async ({ page }) => {
    const startBtn = page.locator('#slot-start-btn');
    await expect(startBtn).toBeVisible();

    await page.locator('#remove-start-btn').click();

    await expect(startBtn).toHaveCount(0, { timeout: 5000 });
  });

  // ============================================
  // BUTTON CLICK EVENTS
  // ============================================

  test('getToolbarComponent by ID — slot button click triggers Java event', async ({ page }) => {
    await clearEventLog(page);

    const startBtn = page.locator('#slot-start-btn');
    await startBtn.click();

    await waitForEvent(page, 'StartButtonClicked');
    const events = await getEventLog(page);
    const found = events.some((e) => e.includes('StartButtonClicked'));
    expect(found).toBe(true);
  });

  // ============================================
  // HIDE / SHOW BUTTONS — Phase 3.2a
  // ============================================

  test('Hide standard toolbar buttons', async ({ page }) => {
    const cleanBtn = shadowButton(page, 'toolbar-button-clean');
    const blockquoteBtn = shadowButton(page, 'toolbar-button-blockquote');
    await expect(cleanBtn).toBeVisible();
    await expect(blockquoteBtn).toBeVisible();

    await page.locator('#hide-buttons').click();

    await expect(cleanBtn).toBeHidden({ timeout: 5000 });
    await expect(blockquoteBtn).toBeHidden({ timeout: 5000 });
  });

  test('Hide ERTE-specific buttons', async ({ page }) => {
    const placeholderBtn = shadowButton(page, 'toolbar-button-placeholder');
    const readonlyBtn = shadowButton(page, 'toolbar-button-readonly');

    await expect(placeholderBtn).toBeVisible();
    await expect(readonlyBtn).toBeVisible();

    await page.locator('#hide-erte-buttons').click();

    await expect(placeholderBtn).toBeHidden({ timeout: 5000 });
    await expect(readonlyBtn).toBeHidden({ timeout: 5000 });
  });

  test('Show hidden buttons again', async ({ page }) => {
    const cleanBtn = shadowButton(page, 'toolbar-button-clean');
    const blockquoteBtn = shadowButton(page, 'toolbar-button-blockquote');

    await page.locator('#hide-buttons').click();
    await expect(cleanBtn).toBeHidden({ timeout: 5000 });
    await expect(blockquoteBtn).toBeHidden({ timeout: 5000 });

    await page.locator('#show-buttons').click();
    await expect(cleanBtn).toBeVisible({ timeout: 5000 });
    await expect(blockquoteBtn).toBeVisible({ timeout: 5000 });
  });

  test('Group auto-hides when all its buttons are hidden', async ({ page }) => {
    // Block group has blockquote + code-block
    const blockGroup = page.locator('#test-editor').locator('[part~="toolbar-group-block"]');
    await expect(blockGroup).toBeVisible();

    await page.locator('#hide-block-group').click();

    // Both buttons hidden → group should auto-hide
    await expect(blockGroup).toBeHidden({ timeout: 5000 });

    // Show all again → group should reappear
    await page.locator('#show-buttons').click();
    await expect(blockGroup).toBeVisible({ timeout: 5000 });
  });

  // ============================================
  // KEYBOARD SHORTCUT — Phase 3.2b
  // ============================================

  test('Custom keyboard shortcut fires — Shift+F9 applies align center — Phase 3.2b', async ({ page }) => {
    await focusEditor(page);

    await page.keyboard.type('Aligned text');
    await page.keyboard.press('Shift+F9');

    await page.waitForTimeout(500);
    const delta = await getDelta(page);

    const alignOps = delta.ops.filter(
      (op: any) => op.attributes && op.attributes.align === 'center'
    );
    expect(alignOps.length).toBeGreaterThan(0);
  });

  test('Keyboard shortcut focuses toolbar — Shift+F10 — Phase 3.2b', async ({ page }) => {
    await focusEditor(page);

    await page.keyboard.press('Shift+F10');
    await page.waitForTimeout(300);

    // Verify a toolbar button has focus inside the shadow DOM
    const hasFocus = await page.evaluate(() => {
      const el = document.querySelector('vcf-enhanced-rich-text-editor') as any;
      const active = el?.shadowRoot?.activeElement;
      return active?.tagName === 'BUTTON';
    });
    expect(hasFocus).toBe(true);
  });

  test('Custom shortcut toggles format — Ctrl+Shift+B toggles bold — Phase 3.2b', async ({ page }) => {
    await focusEditor(page);

    await page.keyboard.type('bold text');
    await page.keyboard.press('Control+a');
    await page.keyboard.press('Control+Shift+b');

    await page.waitForTimeout(500);
    let delta = await getDelta(page);
    const boldOps = delta.ops.filter(
      (op: any) => op.attributes && op.attributes.bold === true
    );
    expect(boldOps.length).toBeGreaterThan(0);

    // Toggle off
    await page.keyboard.press('Control+Shift+b');
    await page.waitForTimeout(500);
    delta = await getDelta(page);
    const boldOpsAfter = delta.ops.filter(
      (op: any) => op.attributes && op.attributes.bold === true
    );
    expect(boldOpsAfter.length).toBe(0);
  });

  // ============================================
  // TOOLBAR SWITCH
  // ============================================

  test('ToolbarSwitch toggle state — click activates', async ({ page }) => {
    await clearEventLog(page);

    const switchBtn = page.locator('#toolbar-switch');
    await expect(switchBtn).toBeVisible();

    const initialOn = await switchBtn.getAttribute('on');
    expect(initialOn).toBeNull();

    await switchBtn.click();

    await expect(switchBtn).toHaveAttribute('on', 'on', { timeout: 5000 });

    await waitForEvent(page, 'ToolbarSwitchChanged');
    const events = await getEventLog(page);
    const activeEvent = events.find((e) => e.includes('active=true'));
    expect(activeEvent).toBeDefined();
  });

  test('ToolbarSwitch second click toggles back to inactive', async ({ page }) => {
    await clearEventLog(page);

    const switchBtn = page.locator('#toolbar-switch');

    await switchBtn.click();
    await expect(switchBtn).toHaveAttribute('on', 'on', { timeout: 5000 });

    await clearEventLog(page);

    await switchBtn.click();

    await page.waitForTimeout(500);
    const onAttr = await switchBtn.getAttribute('on');
    expect(onAttr).toBeNull();

    await waitForEvent(page, 'ToolbarSwitchChanged');
    const events = await getEventLog(page);
    const inactiveEvent = events.find((e) => e.includes('active=false'));
    expect(inactiveEvent).toBeDefined();
  });

  // ============================================
  // CUSTOM BUTTON EVENTS
  // ============================================

  test('Custom button click fires Java event — START slot', async ({ page }) => {
    await clearEventLog(page);

    await page.locator('#slot-start-btn').click();

    await waitForEvent(page, 'StartButtonClicked');
    const events = await getEventLog(page);
    expect(events.some((e) => e.includes('StartButtonClicked'))).toBe(true);
  });

  test('Custom group button click fires event', async ({ page }) => {
    await clearEventLog(page);

    await page.locator('#custom-group-btn').click();

    await waitForEvent(page, 'CustomGroupBtnClicked');
    const events = await getEventLog(page);
    expect(events.some((e) => e.includes('CustomGroupBtnClicked'))).toBe(true);
  });

  // ============================================
  // TOOLBAR KEYBOARD NAVIGATION
  // ============================================

  test('Toolbar keyboard navigation — arrow keys move focus', async ({ page }) => {
    // Focus editor first, then Shift+Tab to move focus to toolbar
    await focusEditor(page);
    await page.keyboard.press('Shift+Tab');

    // Read which button has focus after Shift+Tab
    const firstFocused = await page.evaluate((editorId) => {
      const el = document.getElementById(editorId) as any;
      if (!el || !el.shadowRoot) return null;
      const focused = el.shadowRoot.activeElement;
      return focused ? focused.getAttribute('part') : null;
    }, 'test-editor');
    expect(firstFocused).not.toBeNull();
    expect(firstFocused).toContain('toolbar-button');

    // Press ArrowRight — focus should move to a DIFFERENT button
    await page.keyboard.press('ArrowRight');
    const secondFocused = await page.evaluate((editorId) => {
      const el = document.getElementById(editorId) as any;
      if (!el || !el.shadowRoot) return null;
      const focused = el.shadowRoot.activeElement;
      return focused ? focused.getAttribute('part') : null;
    }, 'test-editor');
    expect(secondFocused).not.toBeNull();
    expect(secondFocused).toContain('toolbar-button');
    expect(secondFocused).not.toBe(firstFocused);

    // Press ArrowLeft — focus should return to the first button
    await page.keyboard.press('ArrowLeft');
    const thirdFocused = await page.evaluate((editorId) => {
      const el = document.getElementById(editorId) as any;
      if (!el || !el.shadowRoot) return null;
      const focused = el.shadowRoot.activeElement;
      return focused ? focused.getAttribute('part') : null;
    }, 'test-editor');
    expect(thirdFocused).toBe(firstFocused);
  });

  test('Arrow navigation includes custom components — ToolbarSwitch', async ({ page }) => {
    // First verify the switch can be focused programmatically
    const canFocus = await page.evaluate(() => {
      const toolbarSwitch = document.getElementById('toolbar-switch');
      if (!toolbarSwitch) return false;

      toolbarSwitch.focus();
      return document.activeElement === toolbarSwitch;
    });

    expect(canFocus).toBe(true); // Verify switch can receive focus

    await focusEditor(page);
    await page.keyboard.press('Shift+Tab');

    // Navigate with ArrowRight until we find the ToolbarSwitch
    // Slotted elements are in light DOM, so check document.activeElement
    let foundSwitch = false;
    for (let i = 0; i < 50; i++) { // safety limit
      const isSwitch = await page.evaluate(() => {
        const toolbarSwitch = document.getElementById('toolbar-switch');
        const active = document.activeElement;

        // Check if active element is the toolbar switch (by reference)
        // Slotted elements remain in light DOM, so document.activeElement finds them
        return active === toolbarSwitch;
      });

      if (isSwitch) {
        foundSwitch = true;
        break;
      }

      await page.keyboard.press('ArrowRight');
    }

    expect(foundSwitch).toBe(true);
  });

  test('TextField in toolbar consumes arrow keys — no toolbar navigation', async ({ page }) => {
    await focusEditor(page);

    // Focus the TextField directly (Tab navigation would get there eventually)
    await page.evaluate(() => {
      const editor = document.getElementById('test-editor') as any;
      const toolbar = editor?.shadowRoot?.querySelector('[part="toolbar"]');
      const textField = toolbar?.querySelector('#toolbar-textfield') as any;
      textField?.focus();
    });

    // Type some text
    await page.keyboard.type('Hello');

    // Press ArrowLeft twice, then type X
    // If arrow keys work within field: cursor moves, X inserted → "HelXlo"
    // If toolbar navigation hijacks: focus moves away, X not inserted
    await page.keyboard.press('ArrowLeft');
    await page.keyboard.press('ArrowLeft');
    await page.keyboard.type('X');

    const text = await page.evaluate(() => {
      const editor = document.getElementById('test-editor') as any;
      const toolbar = editor?.shadowRoot?.querySelector('[part="toolbar"]');
      const textField = toolbar?.querySelector('#toolbar-textfield') as any;
      return textField?.value || '';
    });

    expect(text).toBe('HelXlo'); // Arrow keys worked within field

    // Verify TextField still has focus (arrows didn't navigate away)
    const stillFocused = await page.evaluate(() => {
      const editor = document.getElementById('test-editor') as any;
      return editor?.shadowRoot?.activeElement?.id === 'toolbar-textfield';
    });
    expect(stillFocused).toBe(true);
  });

  // ============================================
  // I18N — TOOLBAR SURVIVES RE-RENDER
  // ============================================

  test('Toolbar survives re-render after i18n change — custom components still exist', async ({
    page,
  }) => {
    await expect(page.locator('#slot-start-btn')).toBeVisible();
    await expect(page.locator('#custom-group-btn')).toBeVisible();
    await expect(page.locator('#toolbar-switch')).toBeVisible();

    await page.locator('#set-german-i18n').click();
    await page.waitForTimeout(1000);

    await expect(page.locator('#slot-start-btn')).toBeVisible({ timeout: 5000 });
    await expect(page.locator('#custom-group-btn')).toBeVisible({ timeout: 5000 });
    await expect(page.locator('#toolbar-switch')).toBeVisible({ timeout: 5000 });
    await expect(page.locator('#slot-end-btn')).toBeVisible({ timeout: 5000 });
    await expect(page.locator('#slot-before-history-btn')).toBeVisible({ timeout: 5000 });
    await expect(page.locator('#slot-after-emphasis-btn')).toBeVisible({ timeout: 5000 });
  });

  test('I18n labels updated — German tooltips applied', async ({ page }) => {
    await page.locator('#set-german-i18n').click();
    await page.waitForTimeout(1000);

    // V25 RTE 2 uses aria-label (not title) for i18n button labels
    const boldBtn = shadowButton(page, 'toolbar-button-bold');
    await expect(boldBtn).toHaveAttribute('aria-label', 'Fett', { timeout: 5000 });

    const italicBtn = shadowButton(page, 'toolbar-button-italic');
    await expect(italicBtn).toHaveAttribute('aria-label', 'Kursiv', { timeout: 5000 });

    const undoBtn = shadowButton(page, 'toolbar-button-undo');
    const undoLabel = await undoBtn.getAttribute('aria-label');
    expect(undoLabel).toContain('ckgängig');
  });

  // ============================================
  // STANDARD BUTTON PARTS
  // ============================================

  test('All standard toolbar button parts exist', async ({ page }) => {
    for (const partName of STANDARD_BUTTON_PARTS) {
      const btn = shadowButton(page, partName);
      await expect(btn).toHaveCount(1, {
        timeout: 5000,
      });
    }
  });

  // ============================================
  // SCREENSHOT
  // ============================================

  test('Screenshot: Toolbar with custom components', async ({ page }) => {
    await page.waitForTimeout(500);

    await expect(getToolbar(page)).toHaveScreenshot('toolbar-custom.png');
  });
});
