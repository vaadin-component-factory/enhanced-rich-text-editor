# V25 Migration Status

**Current: Phase 5 — Final QA & Restructuring**

> **Note:** ERTE 6.0.0 focuses on Lumo theme support only. Multi-theme support (Aura, Material) is planned for post-6.0.0 releases (Phase 6).

| Phase    | Feature                            | Status                                 |
|----------|------------------------------------|----------------------------------------|
| 0        | Use Case Analysis                  | COMPLETE                               |
| 1        | Project Base                       | COMPLETE                               |
| 2        | ERTE Shell                         | COMPLETE                               |
| **3.1a** | Custom Slots / Toolbar Slot System | COMPLETE                               |
| **3.1b** | Readonly Sections                  | COMPLETE                               |
| **3.1c** | Tabstops + Rulers + Soft-Break     | COMPLETE                               |
| ~~3.1d~~ | ~~Rulers~~                         | *(merged into 3.1c)*                   |
| ~~3.1e~~ | ~~Soft-Break + Tab Copying~~       | *(merged into 3.1c)*                   |
| **3.1f** | Placeholders                       | COMPLETE                               |
| **3.1g** | extendOptions Hook                 | COMPLETE                               |
| **3.2a** | Toolbar Button Visibility          | COMPLETE                               |
| **3.2b** | Custom Keyboard Shortcuts          | COMPLETE                               |
| **3.3a** | Non-Breaking Space                 | COMPLETE                               |
| **3.3b** | Whitespace Indicators              | COMPLETE                               |
| **3.3c** | Security Hardening — Sanitization  | COMPLETE                               |
| **3.3d** | I18n                               | COMPLETE                               |
| **3.3e** | Programmatic Text Insertion        | COMPLETE                               |
| **3.3f** | Align Justify                      | COMPLETE                               |
| **3.3g** | Replace Toolbar Button Icons       | COMPLETE                               |
| **3.3h** | Arrow Navigation                   | COMPLETE *(impl. in 3.1c)*             |
| **3.4**  | **Open Issues (Tier 4)**           | COMPLETE                               |
| 3.4a     | Placeholder Button Active State    | COMPLETE                               |
| 3.4b     | Toolbar Arrow-Key Navigation       | COMPLETE                               |
| 3.4c     | Whitespace Indicators (Spaces)     | COMPLETE                               |
| 3.4d     | Fix focus() Method                 | COMPLETE                               |
| 3.4e     | Toolbar Button Sync                | NOT SOLVABLE *(documented)*            |
| **3.4f** | Slotted Toolbar Button Styles      | COMPLETE                               |
| **3.4g** | ToolbarPopover                     | COMPLETE                               |
| **3.4h** | ToolbarSelectPopup                 | COMPLETE                               |
| **3.4i** | ToolbarDialog                      | COMPLETE                               |
| **3.4k** | Custom Properties for ERTE Styles  | COMPLETE                               |
| **3.5**  | **Documentation (Core)**           | COMPLETE                               |
| 3.5a     | Upgrade Guide ERTE 1→2             | COMPLETE                               |
| 3.5b     | User Documentation                 | COMPLETE                               |
| 3.5c     | Developer Documentation            | COMPLETE                               |
| 3.5d     | README & Project Docs              | COMPLETE                               |
| **4**    | **Tables Addon**                   | COMPLETE                               |
| 4.1      | Project Setup & Base               | COMPLETE                               |
| 4.2      | Blot Migration                     | COMPLETE                               |
| **4.3a** | Table Operations, CSS, Java API    | COMPLETE                               |
| **4.3b** | Template System                    | COMPLETE                               |
| **4.4**  | **Tables Tests**                   | COMPLETE *(82 tests: 71 pass, 11 fixme)* |
| 4.5      | Documentation                      | COMPLETE                               |
| 4.6      | Custom Properties for Tables       | COMPLETE                               |
| ~~4.7~~  | ~~Demo Migration (Tables)~~        | SKIPPED *(covered by Phase 5.1 + playground)* |
| **5**    | **Final QA & Restructuring**       | NOT STARTED                            |
| 5.0      | Public API Breaking Changes Audit  | NOT STARTED                            |
| 5.1      | V24 Sample Views Migration         | NOT STARTED *(migrate V24 demo views to V25 demo placeholder)* |
| 5.2      | Test Views → Module IT             | NOT STARTED *(move test views + Playwright tests from demo into module test infrastructure)* |
| 5.3      | Documentation Humanization Review  | NOT STARTED *(rewrite docs for human readers, reduce machine-generated tone)* |

### Phase 5 QA Notes (zu untersuchen)

- **a) TabConverter vs TableDeltaConverter Konflikt?** — Prüfen ob sich TabConverter und ein potentieller TableDeltaConverter gegenseitig stören
- **b) Left Ruler fehlt** — Issue: der linke Ruler wird nicht angezeigt
- **c) Dev-Doku dramatisch einkürzen** — Nur ERTE-spezifische Inhalte behalten; Quill 2- und RTE 2-Inhalte rausnehmen (gehören nicht in ERTE-Doku)
- **d) TabConverter entfernen (Breaking Change)** — TabConverter rausnehmen. Als potentieller Breaking Change dokumentieren: betrifft nur Deltas die vor ERTE 5.2.0 erstellt wurden. Prüfen ob Delta-Versionierung möglich wäre, um solche Migrationen in Zukunft automatisieren zu können.
| **6**    | **Theme Support (Post-6.0.0)**     | NOT STARTED                            |
| 6.1      | Aura Theme Support                 | NOT STARTED                            |
| 6.2      | Material Theme Support             | NOT STARTED                            |
