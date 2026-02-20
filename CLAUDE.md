# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

---

## ⚠️ BEFORE YOU RESPOND — NOTIFICATION CHECKLIST

**Check these BEFORE every response. Failing to notify = usability failure.**

- [ ] **Using Task tool?** → Is this a long-running operation? → `notify "description"` AFTER completion
- [ ] **Using AskUserQuestion?** → `notify-urgent "question"` BEFORE the tool call
- [ ] **Using EnterPlanMode?** → `notify-urgent "entering plan mode"` BEFORE the tool call
- [ ] **Using ExitPlanMode?** → `notify-urgent "waiting for plan approval"` BEFORE the tool call
- [ ] **Hit an error/blockade?** → `notify-urgent "error occurred"` IMMEDIATELY
- [ ] **Background task completed?** → `notify "task completed"` when you receive task-notification

**Pattern:** Notification = SEPARATE Bash call. NEVER combine with other tools in same message.

**Examples:**
```bash
# ✅ CORRECT - separate notification call before the blocking action
notify-urgent "Waiting for plan approval"
# (then in next tool block: ExitPlanMode)

# ✅ CORRECT - notify after long task completes
notify "Build completed successfully"

# ❌ WRONG - no notification before question
# (AskUserQuestion without prior notify-urgent)

# ❌ WRONG - combined in same message
# (Bash notify + Task in parallel blocks)
```

---

## Project Overview

Enhanced Rich Text Editor (ERTE) for Vaadin — a rich text editor component extending Vaadin's built-in RTE with tabstops, placeholders, non-breaking space, rulers, customizable toolbar, read-only sections, and more.

**Active work:** Migrating from Vaadin 24 / Quill 1 (ERTE 1) to Vaadin 25 / Quill 2 (ERTE 2). The old V24 modules remain in the repo as dead reference code (excluded from build). New V25 modules are created from scratch, using the old code as context.

**Screenshots:** Saved to `.claude/screenshots/` (working directory). The `~/transfer/erte` folder is read-only.

## Notifications — MANDATORY AND VERY IMPORTANT! DON'T IGNORE THIS!!!

**ALWAYS use `notify` or `notify-urgent` in these situations — NO EXCEPTIONS:**

### 1. Questions or clarifications → `notify-urgent` BEFORE
**Trigger:** AskUserQuestion, EnterPlanMode, or any text where you ask the user to decide something.
```bash
notify-urgent "Need clarification on approach"
# Then: AskUserQuestion or text asking for decision
```

### 2. Blockades, errors, interruptions → `notify-urgent` IMMEDIATELY
**Trigger:** Build failed, test failed, command errored, can't proceed.
```bash
notify-urgent "Build failed - need user guidance"
# Then: explain error and ask how to proceed
```

### 3. Waiting for user input → `notify-urgent` BEFORE
**Trigger:** ExitPlanMode (waiting for plan approval), or any situation where you stop and wait.
```bash
notify-urgent "Plan ready for review"
# Then: ExitPlanMode
```

### 4. Long-running tasks completed → `notify` AFTER
**Trigger:** Task tool completes (you receive task-notification), build finishes, tests finish, server starts.
```bash
notify "Tests completed - 185 passed, 5 failed"
# After: receiving task-notification or seeing background task output
```

**Commands:**
- `notify "short description"` — informational (task done, status update)
- `notify-urgent "short description"` — requires user attention (questions, errors, blocks)

**Timing:** Notification = SEPARATE Bash call in its own message. NEVER combine with the triggering action.

The user expects ACTIVE notifications, not just inline text. Failing to notify is a usability failure.

## Root Scripts

Convenience scripts in the repo root for build, server, and test operations. **Always use these instead of running Maven/server commands manually.**

| Script | Purpose |
|--------|---------|
| `v25-build.sh [-q]` | Build V25 modules (`mvn clean install -DskipTests`) |
| `v25-build-clean.sh [-q]` | Same + `vaadin:clean-frontend` (wipes dev bundle) |
| `v25-server-start.sh [port]` | Start demo on port 8080 (default) |
| `v25-server-stop.sh` | Stop demo server |
| `v25-server-logs.sh [-f\|-errors]` | Print server logs |
| `v25-server-status.sh` | Check server status |

**Workflow:** After changing addon code (ERTE JS/Java), always `v25-build.sh` before `v25-server-start.sh`. Tests require a running server.

**Always stop the server** after tests/explorations are done (`v25-server-stop.sh`). The server runs inside a container and is not useful to the user — don't leave it running.

**Background tasks** (`run_in_background: true`): Use for complex, longer-running operations like plan reviews, feature implementation, and test runs. Do NOT use for quick, simple tasks (e.g., a single file read, a status check, a small edit).

## Build Commands

```bash
# Build + server (preferred — use root scripts)
bash v25-build.sh
bash v25-server-start.sh

# Run Playwright tests (server must be running first)
cd enhanced-rich-text-editor-demo
npx playwright test

# Run only ERTE tests
npx playwright test tests/erte/

# Playwright with UI for debugging
npx playwright test --ui

# Maven commands (fallback if scripts unavailable)
mvn clean install -DskipTests
mvn -pl enhanced-rich-text-editor-demo spring-boot:run
mvn verify
mvn clean package -Pproduction -DskipTests
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

### Git Workflow

- **NEVER merge to `master`** — `master` is the V24 production branch.
- Working branch: **`v25`**. All migration work lives here.
- Feature/fix branches merge into `v25` (via PR or local merge), never into `master`.

### Golden Rules

1. **No feature regression.** Every ERTE 1 feature must exist in ERTE 2. If you encounter a feature where no clear migration path exists, STOP and ask.
2. **Updatability over convenience.** Never copy RTE 2 source code. Extend at runtime. The only acceptable "fork" is the `render()` override in the JS subclass.
3. **One feature at a time.** Complete and verify each feature before starting the next.
4. **Security fixes are mandatory.** See `SECURITY.md` — known XSS vectors from ERTE 1 must be fixed during migration, not reproduced.

### Architecture Decisions (confirmed by spike — all PASS)

- **JS extension strategy:** ES class extension of RTE 2's web component (not composition, not prototype patching). See `implementation_notes.md` section 6.
- **Java packages:** `RteExtensionBase` in `com.vaadin.flow.component.richtexteditor` (bridge lifting package-private → protected), `EnhancedRichTextEditor` in `com.vaadin.componentfactory` (all ERTE logic). Only the bridge class lives in the foreign package.
- **Tag:** `vcf-enhanced-rich-text-editor` (own tag, own web component registration).
- **Value format:** HTML-primary (matching RTE 2). Delta access via `asDelta()` wrapper. Clean break from ERTE 1's Delta-primary API.
- **Theme:** Lumo. Use `--vaadin-*` custom properties where they exist. Loads via `@StyleSheet(Lumo.STYLESHEET)` on host app.
- **Blot registration:** Global `Quill.register()` before element creation (proven pattern, used by RTE 2 itself).
- **Toolbar:** `super.render()` passthrough + DOM injection of `<slot>` elements in `ready()`. CONFIRMED: injected slots survive all Lit re-renders (i18n change, readonly toggle, requestUpdate). No template copy — updatability preserved. See `migration_v25/progress/3.1a_custom_slots.md` for investigation results.

### Migration Order

Follow this exact sequence. Do not skip ahead. Full spec in `user_description.md`.

1. **Step 0: Use Case Analysis** — Cross-reference each ERTE 1 feature with `feature_comparison.md`
2. **Step 1: Project Base** — Vaadin 25.0.x, all dependencies, Maven profiles, Dockerfile
3. **Step 2: ERTE Shell** — JS subclass + Java subclass, `render()` override, verify Lit lifecycle
4. **Step 3: Feature Migration** — one subphase per feature, lettered 3a–3q:
    **Work through subphases sequentially (one at a time).** Complete and verify
    each subphase before starting the next. Tier 1 → Tier 2 → Tier 3.

    **Phase 3 execution: always per subphase.** Each subphase (3.1a–3.3h) is planned,
    implemented, tested, and committed independently. Do NOT create a "masterplan" for
    all of Phase 3. Start each session by reading the next NOT STARTED progress file and
    planning just that subphase. This keeps context small and allows learnings from one
    subphase to inform the next.

    **Cross-cutting concerns:** When implementing a subphase, if something is discovered
    that affects later subphases, document it in the respective progress file(s) under a
    "Cross-cutting notes from phase X.Y" section.

    **Mandatory plan review:** Every plan created for a migration (sub)phase MUST be
    automatically reviewed by the `agents-manager` before implementation begins. The
    review MUST include at minimum the `fullstack-developer` and `ui-designer` agents.
    Do NOT start implementing until the review feedback has been incorporated.

    **CRITICAL: Planning and review in plan mode:** ALL planning and review activities
    for migration phases MUST be performed in plan mode (triggered by EnterPlanMode).
    This includes:
    - Creating implementation plans for new phases
    - Having plans reviewed by agents-manager
    - Incorporating review feedback
    - Any design or architecture discussions
    The ONLY exception is when a plan file already exists at `migration_v25/progress/PHASE__plan.md` —
    in that case, the plan was previously prepared and reviewed in plan mode, so direct
    implementation can proceed without re-entering plan mode.

## Implementation Delegation

**Plan execution and presumably non-trivial tasks** MUST be delegated to the
`agents-manager` (default/task-assignment mode). The agents-manager selects the
appropriate agents for each step. Examples: implementing a reviewed plan, fixing a bug,
adding a feature. Counter-examples: creating a commit, updating a status file, answering
a question.

- **Parallel execution preferred:** Independent steps (e.g., Java backend + JS frontend,
  or test view + test spec) SHOULD be delegated to separate agents running concurrently.
- **Background tasks:** Use `run_in_background: true` for agents where possible, so
  multiple workstreams proceed simultaneously. The orchestrator monitors progress and
  coordinates results.
- **Sequential only when required:** Only truly dependent steps (e.g., build before test,
  blot registration before keyboard bindings that use the blot) should block on each other.

    **Phase plan files:** Before starting a phase, check if a plan file exists at
    `migration_v25/progress/PHASE__plan.md` (e.g., `3.1c__plan.md`). If present, use
    it as the **primary basis** for implementation — it was previously prepared and
    reviewed. No additional review necessary, unless the user explicitly asks for it.
    After successful implementation and verification of the phase, **delete**
    the plan file. Plan files are working documents, not permanent records.

    **Writing plan files:** After planning a phase (including agents-manager reviews),
    ALWAYS write the plan to `migration_v25/progress/PHASE__plan.md`. The plan must be
    **self-contained and clear enough to be executed from a clean context** (no reliance
    on conversation history). Each plan MUST include a **complexity recommendation**:
    whether the `agents-manager` should handle implementation or the orchestrator can
    do it directly. The agents-manager makes the final call on delegation.

    **Tier 1 — Core Differentiators (fixed order):**
    - **3.1a** Custom Slots / Toolbar Slot System (Feature 8)
    - **3.1b** Readonly Sections (Feature 4) — *also establishes sanitizer override structure*
    - **3.1c** Tabstops (Feature 1)
    - **3.1d** Rulers (Feature 2)
    - **3.1e** Soft-Break + Tab Copying (Feature 3)
    - **3.1f** Placeholders (Feature 5)
    - **3.1g** extendOptions Hook (Feature 16)

    **Tier 2 — Important (fixed order):**
    - **3.2a** Toolbar Button Visibility (Feature 9)
    - **3.2b** Custom Keyboard Shortcuts (Feature 10)

    **Tier 3 — Remaining (any order):**
    - **3.3a** Non-Breaking Space (Feature 6)
    - **3.3b** Whitespace Indicators (Feature 7)
    - **3.3c** Security Hardening — Sanitization (Feature 11)
    - **3.3d** I18n (Feature 12)
    - **3.3e** Programmatic Text Insertion (Feature 14)
    - **3.3f** Align Justify (Feature 18)
    - **3.3g** Replace Toolbar Button Icons (Feature 19)
    - **3.3h** Arrow Navigation (Feature 20)
    - **3.4** Open Issues - fixing minor issues, left from the feature impl steps

    *Tier 0 (Features 13, 15, 17) — inherited, no migration needed.*

    **Sanitizer strategy:** Basic override structure in 3.1b, each feature adds its classes
    to the whitelist incrementally. Phase 3.3c = security hardening only.

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
- Do not set `contenteditable="false"` on Embed blot outer `domNode` — Quill 2's guard nodes (zero-width text inside the domNode) must remain editable for cursor placement. The inner `contentNode` already has `contenteditable="false"`. V24 pattern was safe (no guard nodes), V25 pattern breaks cursor.

## MCP Tools — Use Before Asking

**Always use available MCP tools to look things up instead of guessing or asking the user.**

- **Vaadin:** `search_vaadin_docs`, `get_component_java_api`, `get_component_web_component_api`, `get_component_styling`, `get_full_document` — for component APIs, docs, styling, internals
- **Playwright:** `browser_snapshot`, `browser_navigate`, `browser_evaluate`, etc. — for verifying UI state, testing interactions, inspecting DOM
- **General:** `WebSearch`, `WebFetch` — for any external docs, Quill/Parchment APIs, npm packages

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
| `migration_v25/STATUS.md` | **Start here** — single-file dashboard showing which phase is next |
| `migration_v25/progress/` | **Always update** after completing a task — one file per phase (e.g., `0_use_cases_tests.md`, `1_project_base.md`) |
| `migration_v25/USE_CASE_ANALYSIS.md` | Feature inventory with migration paths for all 20 ERTE features |
| `tests/TEST_INVENTORY.md` | Full test listing grouped by feature (update when adding/removing tests) |

## Playwright Tests

255 total tests: 75 prototype + 180 ERTE. Full listing in [TEST_INVENTORY.md](enhanced-rich-text-editor-demo/tests/TEST_INVENTORY.md).

**Running tests:**
```bash
# Build first
mvn clean package -DskipTests
# Start server (from demo dir)
cd enhanced-rich-text-editor-demo && bash server-start.sh
# Run all tests
npx playwright test
# Run only ERTE tests
npx playwright test tests/erte/
# Stop server
bash server-stop.sh
```

### ERTE Test Suite (180 tests in `tests/erte/`)

| Spec File | Tests | Covers |
|-----------|-------|--------|
| `tabstops.spec.ts` | 75 | Tabstops, rulers, soft-break, whitespace indicators |
| `placeholders.spec.ts` | 32 | Placeholder dialog, events, appearance, keyboard |
| `readonly.spec.ts` | 18 | Readonly sections, protection, whole-editor readonly |
| `toolbar.spec.ts` | 24 | Slot system, visibility, shortcuts, icons, keyboard nav |
| `extend-options.spec.ts` | 4 | extendQuill/extendEditor hooks, V24 deprecation |
| `features.spec.ts` | 24 | NBSP, addText, align, indent, i18n, sanitizer, focus |

**Test views** (Java, in `com.vaadin.componentfactory`): `ErteTabStopTestView`, `ErtePlaceholderTestView`, `ErteReadonlyTestView`, `ErteToolbarTestView`, `ErteExtendOptionsTestView`, `ErteFeatureTestView`. Each provides a single editor (`id="test-editor"`), delta/HTML output elements, event log, and a ready indicator.

**Side navigation:** `ErteTestLayout.java` provides an `AppLayout` with `SideNav` listing all phases. When implementing a new phase, always update this layout: change the `disabled(...)` entry to `new SideNavItem("label", "erte-test/route", icon)` so the link becomes active.

**Shared helpers** (`tests/erte/helpers.ts`): `waitForEditor()`, `getDelta()`, `getDeltaFromEditor()`, `getRuler()`, `getRulerMarkers()`, etc.

### Prototype Tests (75 tests in `tab-stop-prototype.spec.ts`)

Original tabstop tests against the prototype view at `/tab-stop`. See [prototype_tests.md](enhanced-rich-text-editor-demo/prototype_tests.md).

### Key Test Patterns
- Shadow DOM: Playwright locators pierce it, but `page.evaluate()`/`waitForFunction` do NOT — use `el.shadowRoot.querySelector()`
- Ready indicator (`#test-ready`) has `display:none` — use `state: 'attached'`
- Readonly blot is a FORMAT attribute `{"attributes":{"readonly":true}}`, NOT an embed
- Placeholder dialog: use `[aria-label="Placeholders"]` to disambiguate from Link dialog
- Ruler markers are `vaadin-icon` elements inside `[part~="horizontalRuler"]`

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
