import { test, expect } from '@playwright/test';
import {
  waitForEditor,
  getEditor,
  getErte,
  getDelta,
  getDeltaFromEditor,
  getHtmlOutput,
  getEventLog,
  clearEventLog,
  waitForEvent,
  getLastEvent,
  focusEditor,
  pressKey,
  typeInEditor,
  ERTE_TEST_BASE,
  // Table helpers
  getTable,
  getTableCells,
  getTableCellByText,
  getCellText,
  getTableRows,
  clickCell,
  ctrlClickCell,
  ctrlDragCells,
  getSelectedCellCount,
  getRowCount,
  getColCount,
  getTdOps,
  parseTdMetadata,
  hasFocusedCell,
  openModifyTableMenu,
  clickModifyTableMenuItem,
  getAddTableButton,
  getModifyTableButton,
  getStyleTemplatesButton,
} from './helpers';

const TABLES_URL = `${ERTE_TEST_BASE}/tables`;

test.describe('ERTE Tables', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto(TABLES_URL);
    await waitForEditor(page);
  });

  // ============================================
  // 3.1 Table Structure (Initial State)
  // ============================================
  test.describe('Table Structure', () => {
    test('Pre-loaded table has 6 rows and 5 columns', async ({ page }) => {
      const rowCount = await getRowCount(page);
      const colCount = await getColCount(page);
      expect(rowCount).toBe(6);
      expect(colCount).toBe(5);
    });

    test('Cells contain numbered text 1-30', async ({ page }) => {
      const cellCount = await getTableCells(page).count();
      expect(cellCount).toBe(30);

      // Check first, middle, and last cells
      expect(await getCellText(page, 0, 0)).toBe('1');
      expect(await getCellText(page, 2, 2)).toBe('13');
      expect(await getCellText(page, 5, 4)).toBe('30');
    });

    test('All cells have table_id, row_id, cell_id attributes', async ({ page }) => {
      const delta = await getDeltaFromEditor(page);
      const tdOps = getTdOps(delta);
      expect(tdOps.length).toBe(30);

      for (const op of tdOps) {
        const meta = parseTdMetadata(op.attributes.td);
        expect(meta.tableId).not.toBe('');
        expect(meta.rowId).not.toBe('');
        expect(meta.cellId).not.toBe('');
      }
    });

    test('All cells share same table_id', async ({ page }) => {
      const delta = await getDeltaFromEditor(page);
      const tdOps = getTdOps(delta);
      const tableIds = tdOps.map(op => parseTdMetadata(op.attributes.td).tableId);
      const uniqueTableIds = new Set(tableIds);
      expect(uniqueTableIds.size).toBe(1);
    });

    test('Cells in same row share row_id', async ({ page }) => {
      const delta = await getDeltaFromEditor(page);
      const tdOps = getTdOps(delta);

      // First row (cells 1-5)
      const firstRowIds = tdOps.slice(0, 5).map(op => parseTdMetadata(op.attributes.td).rowId);
      expect(new Set(firstRowIds).size).toBe(1);

      // Second row (cells 6-10)
      const secondRowIds = tdOps.slice(5, 10).map(op => parseTdMetadata(op.attributes.td).rowId);
      expect(new Set(secondRowIds).size).toBe(1);

      // First and second row should have different IDs
      expect(firstRowIds[0]).not.toBe(secondRowIds[0]);
    });

    test('Table has colgroup with 5 col elements', async ({ page }) => {
      const colCount = await getColCount(page);
      expect(colCount).toBe(5);

      const colgroupExists = await page.locator('#test-editor').locator('.ql-editor table colgroup').count();
      expect(colgroupExists).toBe(1);
    });
  });

  // ============================================
  // 3.2 Table Creation
  // ============================================
  test.describe('Table Creation', () => {
    test('Insert table via toolbar popover (3x3)', async ({ page }) => {
      // Click below the existing table to position cursor
      await getEditor(page).locator('p').last().click();

      // Open Add Table popover
      await getAddTableButton(page).click();

      // Fill in rows and columns (default is 3)
      const rowsField = page.locator('vaadin-integer-field').first();
      const colsField = page.locator('vaadin-integer-field').nth(1);
      await expect(rowsField.locator('input')).toHaveValue('3');
      await expect(colsField.locator('input')).toHaveValue('3');

      // Click the Plus button to insert (inside popover overlay)
      await page.getByRole('dialog').getByRole('button').click();

      // Wait for second table to appear
      await expect(page.locator('#test-editor').locator('.ql-editor table')).toHaveCount(2);

      // Verify new table has 3 rows and 3 columns
      const tables = await page.locator('#test-editor').locator('.ql-editor table').all();
      const newTableRows = await tables[1].locator('tr').count();
      const newTableCols = await tables[1].locator('colgroup col').count();
      expect(newTableRows).toBe(3);
      expect(newTableCols).toBe(3);
    });

    test('New table cells have unique IDs', async ({ page }) => {
      // Get IDs from first table
      const delta1 = await getDeltaFromEditor(page);
      const tdOps1 = getTdOps(delta1);
      const tableId1 = parseTdMetadata(tdOps1[0].attributes.td).tableId;

      // Insert second table
      await getEditor(page).locator('p').last().click();
      await getAddTableButton(page).click();
      await page.getByRole('dialog').getByRole('button').click();
      await page.waitForTimeout(500); // Wait for insertion

      const delta2 = await getDeltaFromEditor(page);
      const tdOps2 = getTdOps(delta2);
      const newTableOps = tdOps2.slice(30); // Skip first 30 from original table
      const tableId2 = parseTdMetadata(newTableOps[0].attributes.td).tableId;

      // Table IDs should be different
      expect(tableId1).not.toBe(tableId2);

      // All cells in new table share the same table ID
      const newTableIds = newTableOps.map(op => parseTdMetadata(op.attributes.td).tableId);
      expect(new Set(newTableIds).size).toBe(1);
    });

    test('Table with template class at creation', async ({ page }) => {
      // This requires the templates to be pre-loaded and a template to be selected
      // For now, we verify that a new table CAN have a template applied after creation
      await getEditor(page).locator('p').last().click();
      await getAddTableButton(page).click();
      await page.getByRole('dialog').getByRole('button').click();
      await page.waitForTimeout(500);

      // Click into the new table
      const tables = await page.locator('#test-editor').locator('.ql-editor table').all();
      await tables[1].locator('td').first().click();

      // Open Style Templates dialog
      await getStyleTemplatesButton(page).click();
      await page.waitForTimeout(300);

      // Select a template from the "Current Template" combo box
      // Display names from JSON: template2 = "Alternating Rows", template3 = "Modern Table"
      await page.getByRole('combobox', { name: 'Current Template' }).click();
      await page.getByRole('option', { name: 'Alternating Rows' }).click();
      await page.waitForTimeout(300);

      // Close dialog via close button in header
      await page.getByLabel('Close dialog').click();

      // Verify template class in delta — ID is "template2", display name is "Alternating Rows"
      const delta = await getDeltaFromEditor(page);
      const tdOps = getTdOps(delta);
      const newTableOps = tdOps.slice(30);
      const templateClass = parseTdMetadata(newTableOps[0].attributes.td).tableClassName;
      expect(templateClass).toBe('template2');
    });

    test('Add Table popover fields accept valid values', async ({ page }) => {
      await getEditor(page).locator('p').last().click();
      await getAddTableButton(page).click();

      const rowsField = page.locator('vaadin-integer-field').first();
      const colsField = page.locator('vaadin-integer-field').nth(1);

      // Change values
      await rowsField.locator('input').fill('5');
      await colsField.locator('input').fill('4');

      await page.getByRole('dialog').getByRole('button').click();
      await page.waitForTimeout(500);

      // Verify new table has 5 rows and 4 columns
      const tables = await page.locator('#test-editor').locator('.ql-editor table').all();
      const newTableRows = await tables[1].locator('tr').count();
      const newTableCols = await tables[1].locator('colgroup col').count();
      expect(newTableRows).toBe(5);
      expect(newTableCols).toBe(4);
    });
  });

  // ============================================
  // 3.3 Row Operations
  // ============================================
  test.describe('Row Operations', () => {
    test('Add row below current cell', async ({ page }) => {
      await clickCell(page, '13'); // Middle of table
      await openModifyTableMenu(page);
      await page.getByRole('menuitem', { name: 'Append row below' }).click();
      await page.waitForTimeout(300);

      const rowCount = await getRowCount(page);
      expect(rowCount).toBe(7);
    });

    test('Add row above current cell', async ({ page }) => {
      await clickCell(page, '1'); // First cell
      await openModifyTableMenu(page);
      await page.getByRole('menuitem', { name: 'Append row above' }).click();
      await page.waitForTimeout(300);

      const rowCount = await getRowCount(page);
      expect(rowCount).toBe(7);
    });

    test('Remove current row', async ({ page }) => {
      await clickCell(page, '16'); // Fourth row
      await openModifyTableMenu(page);
      await page.getByRole('menuitem', { name: 'Remove row' }).click();
      await page.waitForTimeout(300);

      const rowCount = await getRowCount(page);
      expect(rowCount).toBe(5);
    });

    test('Row IDs preserved after add', async ({ page }) => {
      const deltaBefore = await getDeltaFromEditor(page);
      const firstRowIdBefore = parseTdMetadata(getTdOps(deltaBefore)[0].attributes.td).rowId;

      await clickCell(page, '13');
      await openModifyTableMenu(page);
      await page.getByRole('menuitem', { name: 'Append row below' }).click();
      await page.waitForTimeout(300);

      const deltaAfter = await getDeltaFromEditor(page);
      const firstRowIdAfter = parseTdMetadata(getTdOps(deltaAfter)[0].attributes.td).rowId;

      // First row ID should remain the same
      expect(firstRowIdAfter).toBe(firstRowIdBefore);
    });

    test('Content preserved in adjacent rows after removal', async ({ page }) => {
      const cell1Before = await getCellText(page, 0, 0);
      const cell26Before = await getCellText(page, 5, 0);

      await clickCell(page, '13'); // Third row
      await openModifyTableMenu(page);
      await page.getByRole('menuitem', { name: 'Remove row' }).click();
      await page.waitForTimeout(300);

      const cell1After = await getCellText(page, 0, 0);
      const cell26After = await getCellText(page, 4, 0); // Now at row 4

      expect(cell1After).toBe(cell1Before);
      expect(cell26After).toBe(cell26Before);
    });
  });

  // ============================================
  // 3.4 Column Operations
  // ============================================
  test.describe('Column Operations', () => {
    test('Add column after current cell', async ({ page }) => {
      await clickCell(page, '13');
      await openModifyTableMenu(page);
      await page.getByRole('menuitem', { name: 'Append column after' }).click();
      await page.waitForTimeout(300);

      const colCount = await getColCount(page);
      expect(colCount).toBe(6);
    });

    test('Add column before current cell', async ({ page }) => {
      await clickCell(page, '1');
      await openModifyTableMenu(page);
      await page.getByRole('menuitem', { name: 'Append column before' }).click();
      await page.waitForTimeout(300);

      const colCount = await getColCount(page);
      expect(colCount).toBe(6);
    });

    test('Remove current column', async ({ page }) => {
      await clickCell(page, '3'); // Third column
      await openModifyTableMenu(page);
      await page.getByRole('menuitem', { name: 'Remove column' }).click();
      await page.waitForTimeout(300);

      const colCount = await getColCount(page);
      expect(colCount).toBe(4);
    });

    test('Colgroup updates after column add', async ({ page }) => {
      await clickCell(page, '13');
      await openModifyTableMenu(page);
      await page.getByRole('menuitem', { name: 'Append column after' }).click();
      await page.waitForTimeout(300);

      const colCount = await getColCount(page);
      expect(colCount).toBe(6);

      // Verify delta reflects new column count
      const delta = await getDeltaFromEditor(page);
      const tdOps = getTdOps(delta);
      expect(tdOps.length).toBe(36); // 6 rows * 6 cols
    });

    test('Colgroup updates after column remove', async ({ page }) => {
      await clickCell(page, '3');
      await openModifyTableMenu(page);
      await page.getByRole('menuitem', { name: 'Remove column' }).click();
      await page.waitForTimeout(300);

      const colCount = await getColCount(page);
      expect(colCount).toBe(4);

      const delta = await getDeltaFromEditor(page);
      const tdOps = getTdOps(delta);
      expect(tdOps.length).toBe(24); // 6 rows * 4 cols
    });
  });

  // ============================================
  // 3.5 Cell Merge and Split
  // ============================================
  test.describe('Cell Merge and Split', () => {
    test('Merge 2x1 horizontal', async ({ page }) => {
      await clickCell(page, '1'); // Enable Modify button via TableSelected
      await ctrlDragCells(page, '1', '2');
      await openModifyTableMenu(page);
      await page.getByRole('menuitem', { name: 'Merge selected cells' }).click();
      await page.waitForTimeout(300);

      // Check delta for merged cell
      const delta = await getDeltaFromEditor(page);
      const tdOps = getTdOps(delta);
      const firstCell = parseTdMetadata(tdOps[0].attributes.td);
      expect(firstCell.colspan).toBe('2');
      expect(Number(firstCell.rowspan)).toBeLessThanOrEqual(1); // '1' or '' both mean no row span
    });

    test('Merge 1x2 vertical', async ({ page }) => {
      await clickCell(page, '1'); // Enable Modify button via TableSelected
      await ctrlDragCells(page, '1', '6');
      await openModifyTableMenu(page);
      await page.getByRole('menuitem', { name: 'Merge selected cells' }).click();
      await page.waitForTimeout(300);

      const delta = await getDeltaFromEditor(page);
      const tdOps = getTdOps(delta);
      const firstCell = parseTdMetadata(tdOps[0].attributes.td);
      expect(Number(firstCell.colspan)).toBeLessThanOrEqual(1); // '1' or '' both mean no col span
      expect(firstCell.rowspan).toBe('2');
    });

    test('Merge 2x2 rectangle', async ({ page }) => {
      await clickCell(page, '1'); // Enable Modify button via TableSelected
      await ctrlDragCells(page, '1', '7');
      await openModifyTableMenu(page);
      await page.getByRole('menuitem', { name: 'Merge selected cells' }).click();
      await page.waitForTimeout(300);

      const delta = await getDeltaFromEditor(page);
      const tdOps = getTdOps(delta);
      const firstCell = parseTdMetadata(tdOps[0].attributes.td);
      expect(firstCell.colspan).toBe('2');
      expect(firstCell.rowspan).toBe('2');
    });

    test.fixme('Split merged cell', async ({ page }) => {
      // BUG: Split doesn't reset colspan on the original merged cell
      // First merge
      await clickCell(page, '1'); // Enable Modify button via TableSelected
      await ctrlDragCells(page, '1', '2');
      await openModifyTableMenu(page);
      await page.getByRole('menuitem', { name: 'Merge selected cells' }).click();
      await page.waitForTimeout(300);

      // Then split — click by position since merged cell text changed
      await getTableCells(page).first().click();
      await openModifyTableMenu(page);
      await page.getByRole('menuitem', { name: 'Split cell' }).click();
      await page.waitForTimeout(300);

      const delta = await getDeltaFromEditor(page);
      const tdOps = getTdOps(delta);
      const firstCell = parseTdMetadata(tdOps[0].attributes.td);
      expect(Number(firstCell.colspan)).toBeLessThanOrEqual(1);
      expect(Number(firstCell.rowspan)).toBeLessThanOrEqual(1);
    });

    test('Merged cell content combines sources', async ({ page }) => {
      await clickCell(page, '1'); // Enable Modify button via TableSelected
      await ctrlDragCells(page, '1', '2');
      await openModifyTableMenu(page);
      await page.getByRole('menuitem', { name: 'Merge selected cells' }).click();
      await page.waitForTimeout(300);

      // Merged cell should contain both "1" and "2"
      const mergedCellText = await getCellText(page, 0, 0);
      expect(mergedCellText).toContain('1');
      expect(mergedCellText).toContain('2');
    });

    test('Merge menu disabled without multi-cell selection', async ({ page }) => {
      await clickCell(page, '1'); // Single cell, no ctrl
      await openModifyTableMenu(page);

      // Merge menu item should be disabled
      const mergeItem = page.getByRole('menuitem', { name: 'Merge selected cells' });
      await expect(mergeItem).toBeDisabled();
    });
  });

  // ============================================
  // 3.6 Cell Selection
  // ============================================
  test.describe('Cell Selection', () => {
    test('Ctrl+Click selects single cell', async ({ page }) => {
      await ctrlClickCell(page, '13');
      await page.waitForTimeout(200);

      const selectedCount = await getSelectedCellCount(page);
      expect(selectedCount).toBe(1);
    });

    test('Ctrl+Drag selects rectangle', async ({ page }) => {
      await ctrlDragCells(page, '1', '7');
      await page.waitForTimeout(200);

      const selectedCount = await getSelectedCellCount(page);
      expect(selectedCount).toBe(4); // 2x2 = 4 cells
    });

    test('Selection highlight visible', async ({ page }) => {
      await ctrlClickCell(page, '13');
      await page.waitForTimeout(200);

      const cell = getTableCellByText(page, '13');
      const hasClass = await cell.evaluate(el => el.classList.contains('ql-cell-selected'));
      expect(hasClass).toBe(true);
    });

    test.fixme('Escape clears selection', async ({ page }) => {
      // Note: Escape may not clear cell selection in current implementation
      await ctrlClickCell(page, '13');
      await page.waitForTimeout(200);
      expect(await getSelectedCellCount(page)).toBe(1);

      await page.keyboard.press('Escape');
      await page.waitForTimeout(200);

      expect(await getSelectedCellCount(page)).toBe(0);
    });

    test('Click outside table clears selection', async ({ page }) => {
      await ctrlClickCell(page, '13');
      await page.waitForTimeout(200);
      expect(await getSelectedCellCount(page)).toBe(1);

      await getEditor(page).locator('p').last().click();
      await page.waitForTimeout(200);

      expect(await getSelectedCellCount(page)).toBe(0);
    });

    test('Ctrl key shows cell cursor', async ({ page }) => {
      await clickCell(page, '13');
      await page.keyboard.down('Control');
      await page.waitForTimeout(200);

      // Verify erte-ctrl-select class on .ql-container (not host element)
      const hasCtrlClass = await page.evaluate((elId) => {
        const el = document.getElementById(elId) as any;
        return el?._editor?.container?.classList.contains('erte-ctrl-select');
      }, 'test-editor');
      expect(hasCtrlClass).toBe(true);

      await page.keyboard.up('Control');
    });

    test('Click in cell shows focused-cell indicator', async ({ page }) => {
      await clickCell(page, '13');
      await page.waitForTimeout(200);

      const hasFocused = await hasFocusedCell(page);
      expect(hasFocused).toBe(true);
    });

    test.fixme('Selection fires TableSelected with cellSelection=true', async ({ page }) => {
      // Note: The table-selected event may fire before cell selection completes
      await clearEventLog(page);
      await ctrlClickCell(page, '13');
      await page.waitForTimeout(300);

      const events = await getEventLog(page);
      const selectionEvent = events.find(e => e.includes('cellSelection=true'));
      expect(selectionEvent).toBeTruthy();
    });
  });

  // ============================================
  // 3.7 Keyboard Navigation
  // ============================================
  test.describe('Keyboard Navigation', () => {
    test('Tab moves to next cell', async ({ page }) => {
      await clickCell(page, '1');
      await pressKey(page, 'Tab');
      await page.waitForTimeout(200);

      // Focus should now be on cell 2
      const cell2 = getTableCellByText(page, '2');
      const isFocused = await cell2.evaluate(el => el.classList.contains('focused-cell'));
      expect(isFocused).toBe(true);
    });

    test('Tab at end of row wraps to next row', async ({ page }) => {
      await clickCell(page, '5');
      await pressKey(page, 'Tab');
      await page.waitForTimeout(200);

      const cell6 = getTableCellByText(page, '6');
      const isFocused = await cell6.evaluate(el => el.classList.contains('focused-cell'));
      expect(isFocused).toBe(true);
    });

    test('Shift+Tab moves to previous cell', async ({ page }) => {
      await clickCell(page, '7');
      await pressKey(page, 'Shift+Tab');
      await page.waitForTimeout(200);

      const cell6 = getTableCellByText(page, '6');
      const isFocused = await cell6.evaluate(el => el.classList.contains('focused-cell'));
      expect(isFocused).toBe(true);
    });

    test('Shift+Tab at row start wraps to previous row', async ({ page }) => {
      await clickCell(page, '6');
      await pressKey(page, 'Shift+Tab');
      await page.waitForTimeout(200);

      const cell5 = getTableCellByText(page, '5');
      const isFocused = await cell5.evaluate(el => el.classList.contains('focused-cell'));
      expect(isFocused).toBe(true);
    });

    test('Tab at last cell exits table', async ({ page }) => {
      await clickCell(page, '30');
      await pressKey(page, 'Tab');
      await page.waitForTimeout(200);

      // Cursor should be after table (verify by checking no focused-cell)
      const hasFocused = await hasFocusedCell(page);
      expect(hasFocused).toBe(false);
    });

    test.fixme('Backspace at cell start does not cross boundary', async ({ page }) => {
      // Known issue: Backspace at cell start crosses cell boundary in Quill 2
      await clickCell(page, '13');
      // Move cursor to start of cell
      await page.keyboard.press('Home');
      await page.keyboard.press('Backspace');
      await page.waitForTimeout(200);

      // Cell at row 2, col 2 should still contain "13"
      const cellText = await getCellText(page, 2, 2);
      expect(cellText).toBe('13');
    });

    test('Delete at cell end does not cross boundary', async ({ page }) => {
      await clickCell(page, '13');
      // Move cursor to end of cell
      await page.keyboard.press('End');
      await page.keyboard.press('Delete');
      await page.waitForTimeout(200);

      // Cell content should still be "13"
      const cellText = await getCellText(page, 2, 2);
      expect(cellText).toBe('13');
    });

    test('Ctrl+A selects cell text only', async ({ page }) => {
      await clickCell(page, '13');
      await page.keyboard.press('Control+a');
      await page.waitForTimeout(200);

      // Type to replace selection
      await typeInEditor(page, 'NEW');
      await page.waitForTimeout(200);

      // Only cell 13 should be replaced
      const cellText = await getCellText(page, 2, 2);
      expect(cellText).toBe('NEW');

      // Other cells should be unchanged
      const cell12Text = await getCellText(page, 2, 1);
      expect(cell12Text).toBe('12');
    });
  });

  // ============================================
  // 3.8 Toolbar Controls
  // ============================================
  test.describe('Toolbar Controls', () => {
    test('Add Table button enabled when not in table', async ({ page }) => {
      await getEditor(page).locator('p').last().click();
      await page.waitForTimeout(200);

      const isEnabled = await getAddTableButton(page).isEnabled();
      expect(isEnabled).toBe(true);
    });

    test('Add Table button disabled when in table', async ({ page }) => {
      await clickCell(page, '13');
      await page.waitForTimeout(200);

      const isDisabled = await getAddTableButton(page).isDisabled();
      expect(isDisabled).toBe(true);
    });

    test('Modify Table enabled when in table', async ({ page }) => {
      await clickCell(page, '13');
      await page.waitForTimeout(200);

      const isEnabled = await getModifyTableButton(page).isEnabled();
      expect(isEnabled).toBe(true);
    });

    test('Modify Table disabled when not in table', async ({ page }) => {
      await getEditor(page).locator('p').last().click();
      await page.waitForTimeout(200);

      const isDisabled = await getModifyTableButton(page).isDisabled();
      expect(isDisabled).toBe(true);
    });

    test('Style Templates enabled when in table', async ({ page }) => {
      await clickCell(page, '13');
      await page.waitForTimeout(200);

      const isEnabled = await getStyleTemplatesButton(page).isEnabled();
      expect(isEnabled).toBe(true);
    });

    test('Style Templates disabled when not in table', async ({ page }) => {
      await getEditor(page).locator('p').last().click();
      await page.waitForTimeout(200);

      const isDisabled = await getStyleTemplatesButton(page).isDisabled();
      expect(isDisabled).toBe(true);
    });
  });

  // ============================================
  // 3.9 Template System
  // ============================================
  test.describe('Template System', () => {
    test('Pre-loaded table has template1 class', async ({ page }) => {
      const delta = await getDeltaFromEditor(page);
      const tdOps = getTdOps(delta);
      const firstCellMeta = parseTdMetadata(tdOps[0].attributes.td);
      expect(firstCellMeta.tableClassName).toBe('template1');
    });

    test('Template CSS applied (background colors)', async ({ page }) => {
      const firstRowCell = getTableCellByText(page, '1');
      const bgColor = await firstRowCell.evaluate(el => window.getComputedStyle(el).backgroundColor);

      // Template1 first row should have dark background (#333 = rgb(51, 51, 51))
      expect(bgColor).toBe('rgb(51, 51, 51)');
    });

    test('Read Templates returns JSON with 3 templates', async ({ page }) => {
      await page.locator('#read-templates-btn').click();
      await page.waitForTimeout(300);

      const templateOutput = await page.locator('#template-output textarea').inputValue();
      const templates = JSON.parse(templateOutput);

      expect(Object.keys(templates)).toHaveLength(3);
      expect(templates).toHaveProperty('template1');
      expect(templates).toHaveProperty('template2');
      expect(templates).toHaveProperty('template3');
    });

    test('Template class in delta td attribute', async ({ page }) => {
      const delta = await getDeltaFromEditor(page);
      const tdOps = getTdOps(delta);
      expect(tdOps.length).toBe(30);

      // Only the first cell of the first row carries the table class
      const firstMeta = parseTdMetadata(tdOps[0].attributes.td);
      expect(firstMeta.tableClassName).toBe('template1');

      // Other cells have empty tableClassName
      for (let i = 1; i < tdOps.length; i++) {
        const meta = parseTdMetadata(tdOps[i].attributes.td);
        expect(meta.tableClassName).toBe('');
      }
    });

    test('Template class preserved in HTML output', async ({ page }) => {
      const html = await getHtmlOutput(page);
      expect(html).toContain('class="template1"');
    });
  });

  // ============================================
  // 3.10 Template Dialog
  // ============================================
  test.describe('Template Dialog', () => {
    test('Style Templates button opens template dialog', async ({ page }) => {
      await clickCell(page, '13');
      await getStyleTemplatesButton(page).click();

      // Dialog overlay renders separately — check for the heading
      await expect(page.getByRole('heading', { name: 'Table Templates' })).toBeVisible({ timeout: 5000 });
      // Close it
      await page.getByLabel('Close dialog').click();
    });

    test('Template ComboBox lists all templates', async ({ page }) => {
      await clickCell(page, '13');
      await getStyleTemplatesButton(page).click();
      await page.waitForTimeout(300);

      // Open combobox dropdown
      const comboBox = page.getByRole('combobox', { name: 'Current Template' });
      await comboBox.click();
      await page.waitForTimeout(200);

      // Check for template options — display names from JSON name field
      const options = page.getByRole('option');
      await expect(options).toHaveCount(3);
      await expect(page.getByRole('option', { name: 'Template 1' })).toBeVisible();
      await expect(page.getByRole('option', { name: 'Alternating Rows' })).toBeVisible();
      await expect(page.getByRole('option', { name: 'Modern Table' })).toBeVisible();

      await page.getByLabel('Close dialog').click();
    });

    test('Change template for current table', async ({ page }) => {
      await clickCell(page, '13');
      await getStyleTemplatesButton(page).click();
      await page.waitForTimeout(300);

      // Select "Alternating Rows" (= template2)
      await page.getByRole('combobox', { name: 'Current Template' }).click();
      await page.getByRole('option', { name: 'Alternating Rows' }).click();
      await page.waitForTimeout(300);

      // Close dialog
      await page.getByLabel('Close dialog').click();
      await page.waitForTimeout(300);

      // Verify template changed in delta (template2 = "Alternating Rows")
      const delta = await getDeltaFromEditor(page);
      const tdOps = getTdOps(delta);
      const firstCellMeta = parseTdMetadata(tdOps[0].attributes.td);
      expect(firstCellMeta.tableClassName).toBe('template2');
    });

    test('Close dialog with close button', async ({ page }) => {
      await clickCell(page, '13');
      await getStyleTemplatesButton(page).click();

      // Wait for dialog to open — check heading
      await expect(page.getByRole('heading', { name: 'Table Templates' })).toBeVisible({ timeout: 5000 });

      await page.getByLabel('Close dialog').click();
      await page.waitForTimeout(300);

      await expect(page.getByRole('heading', { name: 'Table Templates' })).not.toBeVisible();
    });
  });

  // ============================================
  // 3.11 Events
  // ============================================
  test.describe('Events', () => {
    test('TableSelected fires on click into table', async ({ page }) => {
      await clearEventLog(page);
      await clickCell(page, '13');
      await page.waitForTimeout(300);

      const events = await getEventLog(page);
      const selectedEvent = events.find(e => e.includes('TableSelected: selected=true'));
      expect(selectedEvent).toBeTruthy();
    });

    test('TableSelected fires on leaving table', async ({ page }) => {
      await clickCell(page, '13');
      await page.waitForTimeout(200);
      await clearEventLog(page);

      await getEditor(page).locator('p').last().click();
      await page.waitForTimeout(300);

      const events = await getEventLog(page);
      const deselectedEvent = events.find(e => e.includes('TableSelected: selected=false'));
      expect(deselectedEvent).toBeTruthy();
    });

    test('CellChanged fires with row/col indices', async ({ page }) => {
      await clearEventLog(page);
      await clickCell(page, '1');
      await page.waitForTimeout(200);
      await clickCell(page, '7');
      await page.waitForTimeout(300);

      const events = await getEventLog(page);
      const cellChangedEvent = events.find(e => e.includes('CellChanged:'));
      expect(cellChangedEvent).toBeTruthy();
      expect(cellChangedEvent).toMatch(/row=\d+, col=\d+/);
    });

    test('CellChanged includes old coordinates', async ({ page }) => {
      await clearEventLog(page);
      await clickCell(page, '1'); // row=0, col=0
      await page.waitForTimeout(200);
      await clickCell(page, '13'); // row=2, col=2
      await page.waitForTimeout(300);

      const events = await getEventLog(page);
      const cellChangedEvent = events.find(e => e.includes('CellChanged:') && e.includes('oldRow='));
      expect(cellChangedEvent).toBeTruthy();
    });

    test('TableSelected reports template class', async ({ page }) => {
      await clearEventLog(page);
      await clickCell(page, '13');
      await page.waitForTimeout(300);

      const events = await getEventLog(page);
      const selectedEvent = events.find(e => e.includes('TableSelected:'));
      expect(selectedEvent).toContain('template=template1');
    });
  });

  // ============================================
  // 3.12 Undo/Redo
  // ============================================
  test.describe('Undo/Redo', () => {
    test('Undo add row', async ({ page }) => {
      await clickCell(page, '13');
      await openModifyTableMenu(page);
      await page.getByRole('menuitem', { name: 'Append row below' }).click();
      await page.waitForTimeout(300);

      expect(await getRowCount(page)).toBe(7);

      await page.keyboard.press('Control+z');
      await page.waitForTimeout(300);

      expect(await getRowCount(page)).toBe(6);
    });

    test.fixme('Undo remove table', async ({ page }) => {
      // Known issue: DOM history may differ slightly
      await clickCell(page, '13');
      await openModifyTableMenu(page);
      await page.getByRole('menuitem', { name: 'Remove table' }).click();
      await page.waitForTimeout(300);

      expect(await getTable(page).count()).toBe(0);

      await page.keyboard.press('Control+z');
      await page.waitForTimeout(300);

      expect(await getTable(page).count()).toBe(1);
    });

    test.fixme('Redo after undo', async ({ page }) => {
      // Known issue: Quill table redo may not fully restore table state
      await clickCell(page, '13');
      await openModifyTableMenu(page);
      await page.getByRole('menuitem', { name: 'Append row below' }).click();
      await page.waitForTimeout(300);

      await page.keyboard.press('Control+z');
      await page.waitForTimeout(300);
      expect(await getRowCount(page)).toBe(6);

      await page.keyboard.press('Control+y');
      await page.waitForTimeout(300);
      expect(await getRowCount(page)).toBe(7);
    });

    test.fixme('Undo merge', async ({ page }) => {
      // Known issue: Quill history doesn't fully restore merged cell state
      await clickCell(page, '1');
      await ctrlDragCells(page, '1', '2');
      await openModifyTableMenu(page);
      await page.getByRole('menuitem', { name: 'Merge selected cells' }).click();
      await page.waitForTimeout(300);

      const deltaBefore = await getDeltaFromEditor(page);
      const firstCellBefore = parseTdMetadata(getTdOps(deltaBefore)[0].attributes.td);
      expect(firstCellBefore.colspan).toBe('2');

      await page.keyboard.press('Control+z');
      await page.waitForTimeout(300);

      const deltaAfter = await getDeltaFromEditor(page);
      const firstCellAfter = parseTdMetadata(getTdOps(deltaAfter)[0].attributes.td);
      expect(firstCellAfter.colspan).toBe('');
    });

    test.fixme('Multiple undo steps', async ({ page }) => {
      // Known issue: Multiple undo steps with column operations may not restore correct count
      await clickCell(page, '13');

      // Step 1: Add row
      await openModifyTableMenu(page);
      await page.getByRole('menuitem', { name: 'Append row below' }).click();
      await page.waitForTimeout(300);

      // Step 2: Add column
      await openModifyTableMenu(page);
      await page.getByRole('menuitem', { name: 'Append column after' }).click();
      await page.waitForTimeout(300);

      expect(await getRowCount(page)).toBe(7);
      expect(await getColCount(page)).toBe(6);

      // Undo column add
      await page.keyboard.press('Control+z');
      await page.waitForTimeout(300);
      expect(await getColCount(page)).toBe(5);

      // Undo row add
      await page.keyboard.press('Control+z');
      await page.waitForTimeout(300);
      expect(await getRowCount(page)).toBe(6);
    });
  });

  // ============================================
  // 3.13 Value Round-Trip
  // ============================================
  test.describe('Value Round-Trip', () => {
    test('Delta has 30 td ops for 6x5 table', async ({ page }) => {
      const delta = await getDeltaFromEditor(page);
      const tdOps = getTdOps(delta);
      expect(tdOps.length).toBe(30);
    });

    test('Each td op has valid metadata format', async ({ page }) => {
      const delta = await getDeltaFromEditor(page);
      const tdOps = getTdOps(delta);

      for (const op of tdOps) {
        const tdValue = op.attributes.td;
        expect(tdValue).toMatch(/^[a-zA-Z0-9]+\|[a-zA-Z0-9]+\|[a-zA-Z0-9]+\|[a-zA-Z0-9]*\|[a-zA-Z0-9]*\|[a-zA-Z0-9]*\|[a-zA-Z0-9-]*$/);
      }
    });

    test('HTML output contains table structure', async ({ page }) => {
      const html = await getHtmlOutput(page);
      expect(html).toContain('<table');
      expect(html).toContain('<tr');
      expect(html).toContain('<td');
      expect(html).toContain('class="td-q"');
    });

    test('Load delta restores table', async ({ page }) => {
      // Read current delta
      await page.locator('#read-delta-btn').click();
      await page.waitForTimeout(300);

      const originalDelta = await page.locator('#delta-input textarea').inputValue();

      // Remove table
      await clickCell(page, '13');
      await openModifyTableMenu(page);
      await page.getByRole('menuitem', { name: 'Remove table' }).click();
      await page.waitForTimeout(300);

      expect(await getTable(page).count()).toBe(0);

      // Restore from delta
      await page.locator('#delta-input textarea').fill(originalDelta);
      await page.locator('#load-delta-btn').click();
      await page.waitForTimeout(500);

      expect(await getTable(page).count()).toBe(1);
      expect(await getRowCount(page)).toBe(6);
      expect(await getColCount(page)).toBe(5);
    });

    test('Template class survives delta round-trip', async ({ page }) => {
      await page.locator('#read-delta-btn').click();
      await page.waitForTimeout(300);

      const deltaJson = await page.locator('#delta-input textarea').inputValue();
      expect(deltaJson).toContain('template1');

      // Load it back
      await page.locator('#delta-input textarea').fill(deltaJson);
      await page.locator('#load-delta-btn').click();
      await page.waitForTimeout(500);

      const delta = await getDeltaFromEditor(page);
      const tdOps = getTdOps(delta);
      const firstCellMeta = parseTdMetadata(tdOps[0].attributes.td);
      expect(firstCellMeta.tableClassName).toBe('template1');
    });
  });

  // ============================================
  // 3.14 Border Toggle
  // ============================================
  test.describe('Border Toggle', () => {
    test.fixme('Border toggle hides table borders', async ({ page }) => {
      // Note: Border toggle has no UI button yet - action exists but not in menu
      // This test requires executeJs to call the connector action directly
    });

    test.fixme('Border toggle restores table borders', async ({ page }) => {
      // Note: Border toggle has no UI button yet
    });
  });

  // ============================================
  // 3.15 Edge Cases
  // ============================================
  test.describe('Edge Cases', () => {
    test('Type text inside cell', async ({ page }) => {
      await clickCell(page, '13');
      await typeInEditor(page, ' EXTRA');
      await page.waitForTimeout(300);

      const cellText = await getCellText(page, 2, 2);
      expect(cellText).toBe('13 EXTRA');
    });

    test('Bold text inside cell', async ({ page }) => {
      await clickCell(page, '13');
      await page.keyboard.press('Control+a');
      await page.keyboard.press('Control+b');
      await page.waitForTimeout(300);

      const delta = await getDeltaFromEditor(page);
      const boldOps = delta.ops.filter((op: any) => op.attributes?.bold);
      expect(boldOps.length).toBeGreaterThan(0);
    });

    test('Remove table clears table content', async ({ page }) => {
      await clickCell(page, '13');
      await openModifyTableMenu(page);
      await page.getByRole('menuitem', { name: 'Remove table' }).click();
      await page.waitForTimeout(300);

      const tableCount = await getTable(page).count();
      expect(tableCount).toBe(0);

      const delta = await getDeltaFromEditor(page);
      const tdOps = getTdOps(delta);
      expect(tdOps.length).toBe(0);
    });

    test('Table survives editor readonly mode', async ({ page }) => {
      await page.evaluate((elId) => {
        const el = document.getElementById(elId) as any;
        el.readonly = true;
      }, 'test-editor');
      await page.waitForTimeout(300);

      const tableCount = await getTable(page).count();
      expect(tableCount).toBe(1);
    });

    test('Table cells not editable in readonly mode', async ({ page }) => {
      await page.evaluate((elId) => {
        const el = document.getElementById(elId) as any;
        el.readonly = true;
      }, 'test-editor');
      await page.waitForTimeout(300);

      await clickCell(page, '13');
      await typeInEditor(page, 'TEST');
      await page.waitForTimeout(300);

      // Text should not have changed
      const cellText = await getCellText(page, 2, 2);
      expect(cellText).toBe('13');
    });

    test.fixme('Delete last row removes entire table', async ({ page }) => {
      // BUG: Removing last row should remove the entire table but currently doesn't
      // Remove rows until only one left
      for (let i = 0; i < 5; i++) {
        await clickCell(page, await getCellText(page, 0, 0)); // Always click first cell
        await openModifyTableMenu(page);
        await page.getByRole('menuitem', { name: 'Remove row' }).click();
        await page.waitForTimeout(300);
      }

      expect(await getRowCount(page)).toBe(1);

      // Remove last row
      await clickCell(page, await getCellText(page, 0, 0));
      await openModifyTableMenu(page);
      await page.getByRole('menuitem', { name: 'Remove row' }).click();
      await page.waitForTimeout(300);

      const tableCount = await getTable(page).count();
      expect(tableCount).toBe(0);
    });

    test('Single-cell table operations', async ({ page }) => {
      await getEditor(page).locator('p').last().click();
      await getAddTableButton(page).click();

      const rowsField = page.locator('vaadin-integer-field').first();
      const colsField = page.locator('vaadin-integer-field').nth(1);
      await rowsField.locator('input').fill('1');
      await colsField.locator('input').fill('1');

      await page.getByRole('dialog').getByRole('button').click();
      await page.waitForTimeout(500);

      const tables = await page.locator('#test-editor').locator('.ql-editor table').all();
      const newTableRows = await tables[1].locator('tr').count();
      const newTableCols = await tables[1].locator('colgroup col').count();
      expect(newTableRows).toBe(1);
      expect(newTableCols).toBe(1);
    });

    test('Empty cell remains functional', async ({ page }) => {
      await clickCell(page, '13');
      await page.keyboard.press('Control+a');
      await page.keyboard.press('Delete');
      await page.waitForTimeout(300);

      const cellText = await getCellText(page, 2, 2);
      expect(cellText).toBe('');

      // Should still be able to type
      await typeInEditor(page, 'NEW');
      await page.waitForTimeout(300);
      expect(await getCellText(page, 2, 2)).toBe('NEW');
    });
  });
});
