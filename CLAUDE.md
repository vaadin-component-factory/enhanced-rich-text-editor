# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Enhanced Rich Text Editor (ERTE) for Vaadin — a rich text editor component extending Vaadin's built-in RTE with tabstops, placeholders, non-breaking space, rulers, customizable toolbar, read-only sections, and more.

**Active work:** Migrating from Vaadin 24 / Quill 1 (ERTE 1) to Vaadin 25 / Quill 2 (ERTE 2). The old V24 modules remain in the repo as dead reference code (excluded from build). New V25 modules are created from scratch, using the old code as context.

## Build Commands

```bash
# Full build (skip tests for faster iteration)
mvn clean install -DskipTests

# Run demo locally (Spring Boot)
mvn -pl enhanced-rich-text-editor-demo spring-boot:run
# Then visit: http://127.0.0.1:8080/enhanced-rich-text-editor

# Run tests
mvn verify

# Run single test class
mvn -pl enhanced-rich-text-editor test -Dtest=RichTextEditorTest

# Production build
mvn clean package -Pproduction -DskipTests

# Run Playwright tests (server must be running first)
cd enhanced-rich-text-editor-demo
npx playwright test

# Playwright with UI for debugging
npx playwright test --ui
```

## Project Structure

Multi-module Maven project. V25 target: Java 21+, Vaadin 25.0.x, Spring Boot 4.x.

- **enhanced-rich-text-editor/** — Core component (V24, reference only, excluded from build)
- **enhanced-rich-text-editor-tables/** — Tables addon (V24, reference only, excluded from build)
- **enhanced-rich-text-editor-demo/** — Demo application
- **enhanced-rich-text-editor-v25/** — *(new)* V25 core component, extends Vaadin's RTE 2
- **enhanced-rich-text-editor-tables-v25/** — *(new)* V25 tables addon

### Data Format
Content is stored as Quill Delta JSON. Tables encode cell metadata in a pipe-separated format within the `td` attribute:
`{tableId}|{rowId}|{cellId}|{rowspan}|{colspan}|{templateId}|{uniqueId}`

The V25 primary value format is HTML (matching RTE 2), with Delta access via `asDelta()` wrapper.

## Migration Rules

### Golden Rules

1. **No feature regression.** Every ERTE 1 feature must exist in ERTE 2. If you encounter a feature where no clear migration path exists, STOP and ask.
2. **Updatability over convenience.** Never copy RTE 2 source code. Extend at runtime. The only acceptable "fork" is the `render()` override in the JS subclass.
3. **One feature at a time.** Complete and verify each feature before starting the next.
4. **Security fixes are mandatory.** See `SECURITY.md` — known XSS vectors from ERTE 1 must be fixed during migration, not reproduced.

### Architecture Decisions (confirmed by spike — all PASS)

- **JS extension strategy:** ES class extension of RTE 2's web component (not composition, not prototype patching). See `implementation_notes.md` section 6.
- **Java package:** `com.vaadin.flow.component.richtexteditor` (same package as RTE 2, for access to package-private methods).
- **Tag:** `vcf-enhanced-rich-text-editor` (own tag, own web component registration).
- **Value format:** HTML-primary (matching RTE 2). Delta access via `asDelta()` wrapper. Clean break from ERTE 1's Delta-primary API.
- **Theme:** Lumo. Use `--vaadin-*` custom properties where they exist. Loads via `@StyleSheet(Lumo.STYLESHEET)` on host app.
- **Blot registration:** Global `Quill.register()` before element creation (proven pattern, used by RTE 2 itself).
- **Toolbar:** `render()` override in JS subclass. Standard RTE 2 groups + ERTE-specific groups + `<slot>` elements for custom components. No MutationObserver hacks.

### Migration Order

Follow this exact sequence. Do not skip ahead. Full spec in `user_description.md`.

1. **Step 0: Use Case Analysis** — Cross-reference each ERTE 1 feature with `feature_comparison.md`
2. **Step 1: Project Base** — Vaadin 25.0.x, all dependencies, Maven profiles, Dockerfile
3. **Step 2: ERTE Shell** — JS subclass + Java subclass, `render()` override, verify Lit lifecycle
4. **Step 3: Feature Migration** (in order):
    1. Custom Slots / Custom Components
    2. Readonly Mode
    3. Tabstops (+ Rulers, Soft Wraps)
    4. Placeholders
    5. Remaining features (any order)
5. **Step 4: Table Extension** (last) — Rewrite blots for Quill 2 / Parchment 3

### Confirmed Patterns (from Spike)

These patterns were validated in the spike (`SPIKE_RESULTS.md`). Use them as-is.

- **Keyboard binding priority:** `addBinding()` appends to END of handler array. To override defaults, clear the array first, add ERTE handler, then re-add originals. See Item 7.
- **Guard nodes:** Quill 2 Embed guard nodes (`\uFEFF`) are INSIDE the embed element. Measure the OUTER `.ql-tab` rect for tab widths, NOT `contentNode`. See Item 20.
- **Sanitizer strips `class`:** RTE 2's `sanitize()` strips `class` and `contenteditable` from `<span>`. Override `sanitize()` to whitelist ERTE classes, or prefer `asDelta().setValue()`. See Item 19.
- **Lumo loading:** V25 requires `@StyleSheet(Lumo.STYLESHEET)` on `AppShellConfigurator`. No ERTE-specific theme registration needed. See Item 11.
- **Lifecycle:** `_editor` available immediately after `super.ready()`. Content set in `ready()` gets overwritten by Java value sync — always set content via Java API. See Item 14.
- **Parchment 3 table blots:** 5 critical API changes required. See Phase 5.

### What NOT to Do

- Do not use `innerHTML` with dynamic content — use `createElement`/`appendChild`
- Do not access `Quill.imports.delta` — use `Quill.import('delta')`
- Do not use numeric keyCodes for keyboard bindings — use string key names (`'Tab'`, `'Enter'`, etc.). Numeric codes create bindings under wrong keys and silently fail.
- Do not access `domNode.__quill` — use `Quill.find(domNode)`
- Do not access `domNode.__blot` — use `Quill.find(domNode)`
- Do not use `Parchment.create()` — removed in Parchment 3, use `this.scroll.create(blotName, value)`
- Do not copy RTE 2 toolbar HTML — override `render()` in the JS subclass
- Do not use Polymer patterns — this is Lit
- Do not set content in `ready()` — it gets overwritten by Java value sync

## Tech Stack (V25)

- Java 21+
- Vaadin 25.0.x (stable, no pre-releases) — RTE is now a **Pro component** (`vaadin` artifact, not `vaadin-core`)
- Spring Boot 4.x / Spring Framework 7
- Quill 2.0.3 (vendored by RTE 2)
- Parchment 3.x
- Jackson 3 (`tools.jackson`, replaces `com.fasterxml.jackson`)
- Mockito 5.x (replaces PowerMock + Mockito 1.x)
- JUnit 5

## Reference Documents

Use these as needed, do NOT try to load all of them as context simultaneously.

| Document | When to consult |
|----------|----------------|
| `user_description.md` | Always — primary migration spec with full feature inventory |
| `SPIKE_RESULTS.md` | Architecture/lifecycle questions, confirmed patterns, Parchment 3 breaking changes |
| `feature_comparison.md` | Before implementing any feature — check what RTE 2 already provides |
| `implementation_notes.md` | JS/Java architecture, shadow DOM structure, pre-spike design notes |
| `quill_v1_to_v2_api_diff.md` | When touching any Quill API — keyboard bindings, blots, history, clipboard |
| `SECURITY.md` | When implementing blots, sanitization, or any DOM manipulation |

## Playwright Tests

75 existing Playwright tests for the tab-stop prototype. See [prototype_tests.md](enhanced-rich-text-editor-demo/prototype_tests.md) for detailed documentation.

**Important:** Before running Playwright tests:
1. Build with `mvn clean package -DskipTests`
2. Start server with `mvn -pl enhanced-rich-text-editor-demo spring-boot:run`

### Test Summary (75 Tests)

| Category | Tests | Description |
|----------|-------|-------------|
| Hard-Break (Enter) | 2 | New paragraph creation, tabs don't copy |
| Soft-Break (Shift+Enter) | 6 | Tab copying based on cursor position |
| Automatic Wrap | 2 | Fixed width for wrapped lines |
| Combined Scenarios | 3 | Mixed hard/soft breaks with tabs |
| Tab Alignment | 5 | L/C/R tabstops + overflow tabs (fixed width) |
| Overflow Tabs + Soft-Break | 3 | More tabs than tabstops with soft-breaks |
| Ruler/Tabstop Manipulation | 4 | Add/remove/cycle tabstops via ruler |
| Edge Cases | 5 | Empty lines, many tabs, alternating patterns |
| Mixed Break Types | 4 | Complex break sequences, copy-paste |
| Stress Tests | 2 | Rapid operations, all tabstops removed |
| Undo/Redo | 4 | Undo operations for tabs and soft-breaks |
| Selection Operations | 3 | Select, delete, replace across soft-breaks |
| Backspace/Delete | 3 | Delete at boundaries and tab characters |
| Tab After Soft-Break | 2 | Tab insertion on new visual lines |
| Formatted Text | 2 | Bold/italic preserved with tabs |
| Cursor Navigation | 3 | Arrow keys and selection around tabs |
| Multiple Paragraphs | 2 | Independent soft-breaks per paragraph |
| Overflow Tab Limit | 4 | Limit copies to tabstop count |
| Browser Resize | 1 | Tab recalculation after viewport change |
| Focus/Blur | 1 | Tab visibility after focus changes |
| Tab at Tabstop Boundary | 1 | Consecutive tabs positioning |
| Empty Visual Line | 1 | Soft-break on empty visual line |
| Whitespace Indicators | 12 | Show/hide whitespace symbols (→↵¶↲), legend |

### Key Soft-Break Behavior
- Soft-break inserts at end of visual line (line stays intact)
- Tabs are copied from visual line start to cursor position
- Tabs copied are limited to the number of defined tabstops (customer requirement)
- Multiple soft-breaks maintain consistent tab count per visual line

## Custom Agents

Custom Claude Code agents in `.claude/agents/`. The **agents-manager** orchestrates them (tech stack discovery, project-specific injection, task delegation).

| Agent | Purpose |
|-------|---------|
| **agents-manager** | Delegates tasks to agents, discovers tech stack, injects project patterns |
| **fullstack-developer** | End-to-end feature implementation (backend + frontend) |
| **code-reviewer** | Quick code review during development (no builds/tests) |
| **qa-tester** | Comprehensive QA: code review + build + tests + responsive checks |
| **ui-explorer** | Playwright-based visual verification of running app |
| **ui-designer** | UI design review: styling, accessibility, responsiveness |
| **docs-engineer** | Documentation creation, updates, and review |
| **security-reviewer** | Security audit: auth, injection, access control, secrets |
| **architecture-guard** | Architectural compliance, import violations, structural patterns |
| **performance-auditor** | Performance issues: N+1 queries, memory leaks, rendering |
| **dependency-auditor** | Dependency vulnerabilities, outdated versions, licenses |
| **migration-auditor** | Database migration safety and backward compatibility |
| **devcontainer-auditor** | Devcontainer/Docker setup review |
| **requirements-reviewer** | Requirements review before implementation |
| **housekeeper** | Cleanup: servers, Docker, temp files, screenshots |

**Usage:** Agents are launched via the `Task` tool with `subagent_type`. The agents-manager has three modes:
- **Update mode:** Run after CLAUDE.md changes or `/init` to inject project-specific patterns
- **Review mode:** Evaluate whether the agent suite fits the project
- **Task assignment (default):** Delegate a task to the right agent(s)

## License

CVALv3 (Commercial Vaadin Add-On License). License headers are enforced on Java files.