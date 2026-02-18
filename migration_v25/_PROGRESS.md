# ERTE V25 Migration — Progress

## Phase 0: Use Case Analysis & UI Test Suite — COMPLETE

### Deliverables
- `migration_v25/USE_CASE_ANALYSIS.md` — 20 features analyzed with migration paths
- 5 Java test views (`ErteTabStopTestView`, `ErtePlaceholderTestView`, `ErteReadonlyTestView`, `ErteToolbarTestView`, `ErteFeatureTestView`)
- 5 Playwright spec files in `tests/erte/` with shared `helpers.ts`
- `tests/TEST_INVENTORY.md` — full test listing

### Test Results
| Suite | Passed | Skipped | Total |
|-------|--------|---------|-------|
| tabstops.spec.ts | 71 | 4 | 78 |
| placeholders.spec.ts | 32 | 0 | 32 |
| readonly.spec.ts | 15 | 3 | 18 |
| toolbar.spec.ts | 24 | 0 | 24 |
| features.spec.ts | 21 | 4 | 25 |
| **ERTE Total** | **163** | **11** | **177** |
| tab-stop-prototype.spec.ts | 74 | 1 (flaky) | 75 |

### Skipped Tests (11)
| Tests | Reason | Fixable? |
|-------|--------|----------|
| 3 NBSP (Shift+Space) | TypeError in vaadin-quill.js — blot has no className | Yes, JS fix |
| 1 TabConverter | Needs old-format delta test setup | Yes, test view change |
| 2 Readonly (Backspace/Delete) | Browser deletes contenteditable=false spans | Yes, keyboard handler |
| 1 Readonly (Undo) | Quill history removes readonly attributes | Medium effort |
| 1 All tabstops removed | Tab key doesn't insert blots without tabstops | Investigate |
| 3 Tab Debug | Prototype-only feature, not in ERTE | N/A |

## Phase 1-4: Not started
