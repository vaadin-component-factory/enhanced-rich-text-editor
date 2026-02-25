# Phase 5: Final QA & Restructuring

**Status:** NOT STARTED

---

## Overview

Final quality assurance and project restructuring before the ERTE 6.0.0 release. This phase covers API audit, demo migration, test infrastructure improvements, and documentation polish.

## Subphases

| Phase | Title | Status |
|-------|-------|--------|
| 5.0 | Public API Breaking Changes Audit | NOT STARTED |
| 5.1 | V24 Sample Views Migration | NOT STARTED |
| 5.2 | Test Views → Module IT | NOT STARTED |
| 5.3 | Documentation Humanization Review | NOT STARTED |

## QA Notes (to investigate)

- **a) TabConverter vs TableDeltaConverter conflict?** — Check whether TabConverter and a potential TableDeltaConverter interfere with each other
- **b) Left Ruler missing** — Issue: the left ruler is not displayed
- **c) Dev docs need drastic reduction** — Keep only ERTE-specific content; remove Quill 2 and RTE 2 content (doesn't belong in ERTE docs)
- **d) TabConverter removal (Breaking Change)** — Remove TabConverter. Document as potential breaking change: only affects Deltas created before ERTE 5.2.0. Investigate whether Delta versioning could automate such migrations in the future.
