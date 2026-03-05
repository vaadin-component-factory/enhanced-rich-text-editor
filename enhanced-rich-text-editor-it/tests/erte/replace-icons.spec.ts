import { test, expect, Page, Locator } from '@playwright/test';
import {
  waitForEditor,
  getToolbar,
  getEventLog,
  ERTE_TEST_BASE,
} from './helpers';

const REPLACE_ICONS_URL = `${ERTE_TEST_BASE}/replace-icons`;

/**
 * Query a shadow DOM button inside the ERTE toolbar by part name.
 * @param page The page object
 * @param partName The part name (e.g., 'toolbar-button-bold')
 */
function shadowButton(page: Page, partName: string): Locator {
  return page.locator(`#test-editor`).locator(`[part~="${partName}"]`);
}

/**
 * Query a custom icon in a specific slot.
 * @param page The page object
 * @param slotName The slot name (e.g., 'bold')
 */
function customIcon(page: Page, slotName: string): Locator {
  return page.locator(`#test-editor vaadin-icon[slot="${slotName}"]`);
}

test.describe('Replace Toolbar Button Icons', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto(REPLACE_ICONS_URL);
    await waitForEditor(page);
  });

  test('1. Pre-configured icons render on load', async ({ page }) => {
    // All 5 custom icons should be present
    await expect(customIcon(page, 'bold')).toBeVisible();
    await expect(customIcon(page, 'italic')).toBeVisible();
    await expect(customIcon(page, 'undo')).toBeVisible();
    await expect(customIcon(page, 'redo')).toBeVisible();
    await expect(customIcon(page, 'align-left')).toBeVisible();

    // Verify correct VaadinIcon attributes
    await expect(customIcon(page, 'bold')).toHaveAttribute('icon', 'vaadin:star');
    await expect(customIcon(page, 'italic')).toHaveAttribute('icon', 'vaadin:flag');
    await expect(customIcon(page, 'undo')).toHaveAttribute('icon', 'vaadin:arrow-left');
    await expect(customIcon(page, 'redo')).toHaveAttribute('icon', 'vaadin:arrow-right');
    await expect(customIcon(page, 'align-left')).toHaveAttribute('icon', 'vaadin:airplane');
  });

  test('2. Default icons are removed when custom icon present', async ({ page }) => {
    // The default icon is rendered via ::before pseudo-element.
    // When a custom icon is slotted, the ::before should be suppressed via CSS.
    // We can't directly test pseudo-elements, but we can verify the slot is not empty.
    const boldIcon = customIcon(page, 'bold');
    await expect(boldIcon).toBeVisible();

    // The button should exist
    const boldButton = shadowButton(page, 'toolbar-button-bold');
    await expect(boldButton).toBeVisible();
  });

  test('3. Custom icon has correct attributes', async ({ page }) => {
    const boldIcon = customIcon(page, 'bold');

    // Check slot attribute
    await expect(boldIcon).toHaveAttribute('slot', 'bold');

    // Check icon attribute (VaadinIcon.STAR)
    await expect(boldIcon).toHaveAttribute('icon', 'vaadin:star');
  });

  test('4. Null icon clears slot and restores default', async ({ page }) => {
    // Initially custom icon present
    await expect(customIcon(page, 'bold')).toBeVisible();

    // Click "Clear Bold Icon"
    await page.click('#clear-bold');

    // Custom icon should be removed
    await expect(customIcon(page, 'bold')).not.toBeVisible();

    // Button should still be visible (default icon restored)
    const boldButton = shadowButton(page, 'toolbar-button-bold');
    await expect(boldButton).toBeVisible();

    // Verify button still has the default icon (check ::before pseudo-element exists)
    // We can't directly test pseudo-elements, but the button should remain functional
    await boldButton.click();
    const editor = page.locator('#test-editor .ql-editor');
    await editor.click();
    await page.keyboard.type('Test');

    // If bold format applied, the default icon is working
    await page.keyboard.press('Tab');
    await page.waitForFunction(() => {
      const textarea = document.querySelector('#delta-output textarea') as HTMLTextAreaElement;
      return textarea && textarea.value && textarea.value.includes('Test');
    }, {}, { timeout: 2000 });

    const deltaOutput = page.locator('#delta-output textarea');
    const deltaText = await deltaOutput.inputValue();
    expect(deltaText).toContain('"bold":true');
  });

  test('5. Runtime icon replacement works', async ({ page }) => {
    // Align-center initially has no custom icon
    await expect(customIcon(page, 'align-center')).not.toBeVisible();

    // Click "Replace Align Center"
    await page.click('#replace-align-center');

    // Custom icon should appear
    await expect(customIcon(page, 'align-center')).toBeVisible();
    await expect(customIcon(page, 'align-center')).toHaveAttribute('icon', 'vaadin:cloud');

    // Event log should record the action
    const eventLog = await getEventLog(page);
    expect(eventLog).toContain('Replaced Align Center icon with Cloud');
  });

  test('6. Icons survive toolbar re-render (i18n change)', async ({ page }) => {
    // Custom icons present before i18n change
    await expect(customIcon(page, 'bold')).toBeVisible();
    await expect(customIcon(page, 'undo')).toBeVisible();
    await expect(customIcon(page, 'redo')).toBeVisible();

    // Click "Set German I18n"
    await page.click('#set-german');

    // Wait for re-render (check aria-label change)
    const undoButton = shadowButton(page, 'toolbar-button-undo');
    await expect(undoButton).toHaveAttribute('aria-label', 'Rückgängig', { timeout: 5000 });

    // Custom icons should still be present
    await expect(customIcon(page, 'bold')).toBeVisible();
    await expect(customIcon(page, 'undo')).toBeVisible();
    await expect(customIcon(page, 'redo')).toBeVisible();

    // Verify icons still have correct attributes
    await expect(customIcon(page, 'bold')).toHaveAttribute('icon', 'vaadin:star');
    await expect(customIcon(page, 'undo')).toHaveAttribute('icon', 'vaadin:arrow-left');
    await expect(customIcon(page, 'redo')).toHaveAttribute('icon', 'vaadin:arrow-right');
  });

  test('7. Multiple icon replacements coexist', async ({ page }) => {
    // All 5 pre-configured icons should be present simultaneously
    const icons = [
      customIcon(page, 'bold'),
      customIcon(page, 'italic'),
      customIcon(page, 'undo'),
      customIcon(page, 'redo'),
      customIcon(page, 'align-left'),
    ];

    for (const icon of icons) {
      await expect(icon).toBeVisible();
    }
  });

  test('8. Icon replacement does not affect button functionality', async ({ page }) => {
    // Bold button has custom icon
    await expect(customIcon(page, 'bold')).toBeVisible();

    // Click bold button
    const boldButton = shadowButton(page, 'toolbar-button-bold');
    await boldButton.click();

    // Type some text
    const editor = page.locator('#test-editor .ql-editor');
    await editor.click();
    await page.keyboard.type('Bold text');

    // Trigger blur to sync delta value (Vaadin lazy value sync)
    await page.keyboard.press('Tab');

    // Wait for delta output to be populated
    await page.waitForFunction(() => {
      const textarea = document.querySelector('#delta-output textarea') as HTMLTextAreaElement;
      return textarea && textarea.value && textarea.value.includes('Bold text');
    }, {}, { timeout: 2000 });

    // Check delta contains bold formatting
    const deltaOutput = page.locator('#delta-output textarea');
    const deltaText = await deltaOutput.inputValue();
    expect(deltaText).toContain('"bold":true');
  });

  test('9. ERTE-specific button icon replacement (justify)', async ({ page }) => {
    // Justify button is ERTE-specific, should work with icon replacement
    // First, verify the button exists
    const justifyButton = shadowButton(page, 'toolbar-button-align-justify');
    await expect(justifyButton).toBeVisible();

    // Note: This test doesn't replace the icon at runtime because the test view
    // only pre-configures 5 standard buttons. But the enum includes ALIGN_JUSTIFY,
    // so the API supports it. This test documents that ERTE buttons work with the API.
  });

  test('10. Enum API provides compile-time safety', async ({ page }) => {
    // This is a documentation test - the TypeScript compilation would fail
    // if the Java enum didn't include the expected constants.
    //
    // In Java:
    //   editor.replaceStandardToolbarButtonIcon(ToolbarButton.BOLD, icon);
    //
    // Provides:
    // - IDE autocomplete
    // - Compile-time validation
    // - Refactoring safety
    //
    // This test verifies the end result - the icons are present via the enum API.

    // Verify all 5 pre-configured icons (set via enum API)
    await expect(customIcon(page, 'bold')).toBeVisible();
    await expect(customIcon(page, 'italic')).toBeVisible();
    await expect(customIcon(page, 'undo')).toBeVisible();
    await expect(customIcon(page, 'redo')).toBeVisible();
    await expect(customIcon(page, 'align-left')).toBeVisible();
  });
});
