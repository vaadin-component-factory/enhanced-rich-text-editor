# Tab-Stop Prototype - Playwright Tests

This document describes the Playwright tests for the tab-stop prototype feature.

## Running Tests

```bash
# Prerequisites
mvn clean package -DskipTests
mvn -pl enhanced-rich-text-editor-demo spring-boot:run

# Run all tests
cd enhanced-rich-text-editor-demo
npx playwright test

# Run with UI
npx playwright test --ui

# Run specific test category
npx playwright test -g "Soft-Break"
npx playwright test -g "Ruler"
```

## Test Summary

**Total: 63 Tests**

| Category | Tests |
|----------|-------|
| Hard-Break (Enter) | 2 |
| Soft-Break (Shift+Enter) | 6 |
| Automatic Wrap | 2 |
| Combined Scenarios | 3 |
| Tab Alignment | 5 |
| Overflow Tabs + Soft-Break | 3 |
| Ruler/Tabstop Manipulation | 4 |
| Edge Cases | 5 |
| Mixed Break Types | 4 |
| Stress Tests | 2 |
| Undo/Redo | 4 |
| Selection Operations | 3 |
| Backspace/Delete at Boundaries | 3 |
| Tab After Soft-Break | 2 |
| Formatted Text with Tabs | 2 |
| Cursor Navigation | 3 |
| Multiple Paragraphs with Soft-Breaks | 2 |
| Overflow Tab Limit (Customer Requirement) | 4 |
| Browser Resize | 1 |
| Focus/Blur | 1 |
| Tab at Tabstop Boundary | 1 |
| Empty Visual Line | 1 |

---

## 1. Hard-Break (Enter) - 2 Tests

| Test | Description | Expected |
|------|-------------|----------|
| Tab in new paragraph aligns to first tabstop | Tab → Text → Enter → Tab | Both tabs align to same tabstop |
| Hard-Break does NOT copy tabs | 3 Tabs → Text → Enter | New paragraph has 0 tabs |

---

## 2. Soft-Break (Shift+Enter) - 6 Tests

| Test | Description | Expected |
|------|-------------|----------|
| Copies tabs up to cursor position | 3 Tabs, cursor after 1st, Shift+Enter | Line 1: 3 tabs, Line 2: 1 tab |
| Cursor at line start copies no tabs | 2 Tabs, cursor at pos 0, Shift+Enter | Line 1: 2 tabs, Line 2: 0 tabs |
| Cursor after all tabs copies all | 2 Tabs + Text, Shift+Enter | Line 1: 2 tabs, Line 2: 2 tabs |
| Multiple soft-breaks maintain count | 5 Tabs, cursor after 3rd, 3x Shift+Enter | Line 1: 5, Lines 2-4: 3 each |
| Preserves original line content | Tab + Text, Shift+Enter mid-text | Both text parts preserved |
| Visual line boundary respected | Soft-break after previous soft-break | Counts only current visual line tabs |

---

## 3. Automatic Wrap - 2 Tests

| Test | Description | Expected |
|------|-------------|----------|
| Tab after auto-wrap uses fixed width | Long text wraps, then Tab | Tab uses fixed width (~50px) |
| Tabs before wrap align to tabstops | Tab → short text | Tab aligns to tabstop |

---

## 4. Combined Scenarios - 3 Tests

| Test | Description | Expected |
|------|-------------|----------|
| Hard-break after soft-break | Tabs → Soft-break → Enter | Creates new paragraph |
| Tabs align after soft-break | Tab + Text → Soft-break → Tab | All tabs align to tabstops |
| Soft-break preserves text | Tab + Text, Shift+Enter mid-text | Both text parts preserved |

---

## 5. Tab Alignment - 5 Tests

| Test | Description | Expected |
|------|-------------|----------|
| First tab → L (left) | 1 Tab + Text | Tab ends at ~100px |
| Second tab → C (center) | 2 Tabs + Text | Tab 2 ends at ~300px |
| Third tab → R (right) | 3 Tabs + Text | Text right-aligned to 500px |
| Fourth tab → fixed width | 4 Tabs + Text | Tab 4 uses fixed width |
| Fifth/sixth → fixed width | 6 Tabs + Text | Tabs 4-6 all fixed width |

---

## 6. Overflow Tabs + Soft-Break - 3 Tests

| Test | Description | Expected |
|------|-------------|----------|
| 5 tabs copies only 3 (tabstop limit) | 5 Tabs + Text → Shift+Enter | Line 1: 5, Line 2: 3 |
| After 4th tab copies only 3 | 6 Tabs, cursor after 4th → Shift+Enter | Line 1: 6, Line 2: 3 |
| Multiple soft-breaks respect limit | 4 Tabs → 2x Shift+Enter | Line 1: 4, Lines 2-3: 3 each |

---

## 7. Ruler/Tabstop Manipulation - 4 Tests

| Test | Description | Expected |
|------|-------------|----------|
| Adding tabstop by clicking ruler | Click ruler at 200px | 4 tabstop markers (new one added) |
| Cycling alignment: L→C→R→remove | Click same tabstop 4x | Cycles through, then removed |
| Removing middle tabstop affects widths | Remove C tabstop | Second tab now reaches R |
| Alignment change updates tab width | Change L to C | Tab width adjusts |

---

## 8. Edge Cases - 5 Tests

| Test | Description | Expected |
|------|-------------|----------|
| Soft-break with only tabs (no text) | 3 Tabs → Shift+Enter | 6 tabs total |
| Hard-break immediately after soft-break | Soft-break → Enter | 2 paragraphs, 1 soft-break |
| Soft-break at empty line | Empty → Shift+Enter | Soft-break created, 0 tabs |
| Many tabs (10+) with soft-break | 10 Tabs → Shift+Enter | 13 tabs total (10 + 3 limited) |
| Alternating tabs and text | Tab-A-Tab-B-Tab-C → Shift+Enter | 6 tabs, all text preserved |

---

## 9. Mixed Break Types - 4 Tests

| Test | Description | Expected |
|------|-------------|----------|
| Soft→Hard→Soft sequence | Complex multi-break | 2 paragraphs, 2 soft-breaks |
| Narrow viewport: wrap + soft-break | 400px width, tabs + soft-break | Tabs copied regardless of wrap |
| Delete tab after soft-break | 2 Tabs → Shift+Enter → Backspace | 3 tabs remain |
| Copy-paste with tabs | Tab+Text → Select → Copy → Paste | Structure preserved |

---

## 10. Stress Tests - 2 Tests

| Test | Description | Expected |
|------|-------------|----------|
| Rapid soft-breaks (10x) | 3 Tabs → 10x Shift+Enter | 33 tabs, 10 soft-breaks |
| All tabstops removed | Remove L, C, R | All tabs use fixed width |

---

## 11. Undo/Redo - 4 Tests

| Test | Description | Expected |
|------|-------------|----------|
| Multiple undo restores state | Tabs + Soft-break → Multiple Undo | Restores to before soft-break |
| Undo removes last typed character | Type "ABC" → Undo | Text is shorter or empty |
| Undo single tab insertion | Tab → Undo | Tab is removed |
| Ruler changes outside Quill history | Add tabstop → Undo | Tabstop remains (not undone) |

---

## 12. Selection Operations - 3 Tests

| Test | Description | Expected |
|------|-------------|----------|
| Select and delete spanning soft-break | Select all → Delete | Editor is empty |
| Select and replace spanning soft-break | Select all → Type | Old content replaced |
| Double-click selects word adjacent to tab | Tab + Word → Double-click | Word selected |

---

## 13. Backspace/Delete at Boundaries - 3 Tests

| Test | Description | Expected |
|------|-------------|----------|
| Backspace at visual line start | Soft-break → Backspace | Soft-break deleted |
| Delete key removes element at cursor | Tab + Text → Home → Delete | Tab removed, text remains |
| Backspace deletes tab character | 3 Tabs → 2x Backspace | 1 tab remains |

---

## 14. Tab After Soft-Break - 2 Tests

| Test | Description | Expected |
|------|-------------|----------|
| Tab on new paragraph | Text → Enter → Tab | Tab inserted with positive width |
| Additional tab on new visual line | Tab + Soft-break → Tab | 3 tabs, all with positive width |

---

## 15. Formatted Text with Tabs - 2 Tests

| Test | Description | Expected |
|------|-------------|----------|
| Bold text preserved after soft-break | Tab + Bold text → Soft-break | Formatting preserved |
| Tab between formatted spans | Bold + Tab + Italic → Soft-break | All formatting preserved |

---

## 16. Cursor Navigation - 3 Tests

| Test | Description | Expected |
|------|-------------|----------|
| Arrow keys navigate across tabs | Tab + Text + Tab → Navigate | Correct cursor positioning |
| End key moves to line end | Multiple visual lines → End | Cursor at visual line end |
| Shift+Arrow selection | Text → Select → Delete | Selected content removed |

---

## 17. Multiple Paragraphs with Soft-Breaks - 2 Tests

| Test | Description | Expected |
|------|-------------|----------|
| Soft-breaks in different paragraphs | P1: soft-break, P2: soft-break | Independent behavior |
| Soft-break doesn't affect other paragraphs | P1: tab, P2: tabs + soft-break | P1 unchanged |

---

## 18. Overflow Tab Limit (Customer Requirement) - 4 Tests

| Test | Description | Expected |
|------|-------------|----------|
| Soft-break at overflow copies only tabstop count | 5 Tabs → Shift+Enter | Line 2: 3 tabs (not 5) |
| Soft-break after 4th tab copies only 3 | 6 Tabs, cursor after 4th | Line 2: 3 tabs (limited) |
| Cursor at 2nd tab copies 2 (within limit) | 5 Tabs, cursor after 2nd | Line 2: 2 tabs |
| After removing tabstops, limit changes | Remove 1 tabstop, 4 tabs → soft-break | Line 2: 2 tabs (new limit) |

---

## 19. Browser Resize - 1 Test

| Test | Description | Expected |
|------|-------------|----------|
| Tab widths after resize | Insert tabs → Resize viewport | Tabs recalculated |

---

## 20. Focus/Blur - 1 Test

| Test | Description | Expected |
|------|-------------|----------|
| Tabs remain visible after focus change | Tabs → Blur → Refocus | Tabs still visible |

---

## 21. Tab at Tabstop Boundary - 1 Test

| Test | Description | Expected |
|------|-------------|----------|
| Consecutive tabs have increasing positions | Tab → Tab | Second tab ends further right |

---

## 22. Empty Visual Line - 1 Test

| Test | Description | Expected |
|------|-------------|----------|
| Soft-break on empty visual line | Tab → Soft-break → Delete tab → Soft-break | 2 soft-breaks, 1 tab |

---

## Key Behaviors

### Soft-Break Logic
1. Finds boundaries of current **visual line** (between soft-breaks)
2. Counts tabs from visual line start to cursor position
3. **Limits tab count to number of defined tabstops** (customer requirement)
4. Inserts soft-break at **end of visual line** (keeps line intact)
5. Copies limited tabs to new visual line
6. Positions cursor after copied tabs

### Tab Width Calculation
- **Within tabstops**: Width calculated to reach next tabstop
- **Beyond tabstops**: Fixed width (~50px or 8 characters)
- **After auto-wrap**: Fixed width (not aligned to tabstop)

### Tabstop Alignments
- **L (Left)**: Text starts at tabstop position
- **C (Center)**: Text centers on tabstop position
- **R (Right)**: Text ends at tabstop position

### Ruler Interaction
- **Click on ruler**: Adds new L-aligned tabstop at click position
- **Click on tabstop marker**: Cycles L → C → R → remove
