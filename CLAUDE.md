# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

---

## ⚠️ CRITICAL PRINCIPLES

**SEI GRÜNDLICH und deiner selbst nie zu sicher!**

- **Always verify your changes actually work** — Never assume success without testing
- **Check that your edits actually land in the built artifacts** — Source changes don't matter if they don't reach the runtime
- **Test end-to-end** — from source edit → build → browser → visual verification
- **When something doesn't work, verify EACH step** — Don't skip ahead assuming your fix worked

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

**Themes:** Lumo and Aura, auto-detected via `ThemeDetectionMixin` (sets `data-application-theme` attribute on host).

**Screenshots:** Saved to `.claude/screenshots/` (working directory). The `~/transfer/erte` folder is read-only.

**Project Language:** English for all code, documentation, comments, commit messages, and technical content. User communication is in German.

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

**What NOT to use notify-urgent for:**
- Intermediate status updates ("running tests", "building", "checking files") — these don't need notifications at all
- Routine progress ("test 1 of 3 complete") — too noisy, just do the work
- Only use notify-urgent for truly important/blocking situations: questions, errors, waiting for approval

**Timing:** Notification = SEPARATE Bash call in its own message. NEVER combine with the triggering action.

The user expects ACTIVE notifications, not just inline text. Failing to notify is a usability failure.

## Root Scripts

Convenience scripts in the repo root for build, server, and test operations. **Always use these instead of running Maven/server commands manually.**

| Script | Purpose |
|--------|---------|
| `build.sh [-q]` | Build V25 modules (`mvn clean install -DskipTests`) |
| `build-clean.sh [-q]` | Same + `vaadin:clean-frontend` (wipes dev bundle) |
| `build-it.sh [-q]` | Build V25 modules + IT module |
| `server-start.sh [port]` | Start demo on port 8080 (default) |
| `server-stop.sh` | Stop demo server |
| `server-logs.sh [-f\|-errors]` | Print server logs |
| `server-status.sh` | Check server status |
| `it-server-start.sh [port]` | Start IT server on port 8081 (default) |
| `it-server-stop.sh` | Stop IT server |
| `it-server-logs.sh [-f\|-errors]` | Print IT server logs |
| `it-server-status.sh` | Check IT server status |

**Workflow:** After changing addon code (ERTE JS/Java), always `build.sh` before `server-start.sh`. Tests require a running server.

**Always stop the server** after tests/explorations are done (`server-stop.sh`). The server runs inside the devcontainer and is not accessible to the user — don't leave it running.

**Background tasks** (`run_in_background: true`): Use for complex, longer-running operations like plan reviews, feature implementation, and test runs. Do NOT use for quick, simple tasks (e.g., a single file read, a status check, a small edit).

## Build Commands

```bash
# Build + server (preferred — use root scripts)
bash build.sh
bash server-start.sh

# Build + IT server (for running ERTE tests)
bash build-it.sh
bash it-server-start.sh

# Run Playwright ERTE tests (IT server must be running on 8081)
cd enhanced-rich-text-editor-it
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

Multi-module Maven project. Java 21+, Vaadin 25.0.x, Spring Boot 4.x.

- **enhanced-rich-text-editor-demo/** — Demo application (user-facing views only)
- **enhanced-rich-text-editor-it/** — Integration tests (test views + Playwright specs, port 8081)
- **enhanced-rich-text-editor/** — Core component, extends Vaadin's RTE 2
- **enhanced-rich-text-editor-tables/** — Tables addon

### Data Format
Content is stored as Quill Delta JSON. Tables encode cell metadata in a pipe-separated format within the `td` attribute:
`{tableId}|{rowId}|{cellId}|{mergeId}|{colspan}|{rowspan}|{tableClass}`

The primary value format is HTML (matching RTE 2), with Delta access via `asDelta()` wrapper.

## Architecture

- **JS extension strategy:** ES class extension of RTE 2's web component (not composition, not prototype patching).
- **Java:** `EnhancedRichTextEditor` in `com.vaadin.componentfactory` extends `RichTextEditor` directly.
- **Tag:** `vcf-enhanced-rich-text-editor` (own tag, own web component registration).
- **Value format:** HTML-primary (matching RTE 2). Delta access via `asDelta()` wrapper.
- **Theme:** Lumo + Aura via `ThemeDetectionMixin`. CSS uses `:host(:where([data-application-theme="lumo"]))` / `"aura"` blocks. Base block uses only `--vaadin-*` shared tokens.
- **Blot registration:** Global `Quill.register()` before element creation.
- **Toolbar:** `super.render()` passthrough + DOM injection of `<slot>` elements in `ready()`. Survives all Lit re-renders.

### Coding Guidelines

- Do not use `innerHTML` with dynamic content — use `createElement`/`appendChild`
- Do not access `Quill.imports.delta` — use `Quill.import('delta')`
- Do not use numeric keyCodes for keyboard bindings — use string key names (`'Tab'`, `'Enter'`, etc.)
- Do not access `domNode.__quill` or `domNode.__blot` — use `Quill.find(domNode)`
- Do not use `Parchment.create()` — removed in Parchment 3, use `this.scroll.create(blotName, value)`
- Do not copy RTE 2 toolbar HTML — override `render()` in the JS subclass
- Do not use Polymer patterns — this is Lit
- Do not set content in `ready()` — it gets overwritten by Java value sync
- Do not set `contenteditable="false"` on Embed blot outer `domNode` — Quill 2's guard nodes must remain editable for cursor placement. The inner `contentNode` already has `contenteditable="false"`.

## Git Workflow

- Working branch: **`v25`**.
- `master` is the V24 production branch.

## MCP Tools & Vaadin Skills — IMPORTANT, Use Before Asking

**Always use available MCP tools and skills to look things up instead of guessing or asking the user.**

### Vaadin Expert Agent (FIRST choice for Vaadin questions)

For ANY Vaadin-related question (components, APIs, theming, patterns, best practices), launch the **`vaadin-expert`** subagent FIRST. It uses official documentation and community sources to give accurate, up-to-date answers. Only fall back to MCP tools directly if the expert agent is insufficient.

### Vaadin MCP Plugin (always available)

- `search_vaadin_docs` — Documentation search (hybrid: semantic + keyword)
- `get_full_document` — Fetch complete documentation pages
- `get_component_java_api` — Java API docs for any Vaadin component
- `get_component_web_component_api` — Web Component/TypeScript API docs
- `get_component_react_api` — React API docs
- `get_component_styling` — Styling/theming docs per component
- `get_vaadin_primer` — Primer for modern Vaadin development
- `get_vaadin_version` — Current stable Vaadin version
- `get_components_by_version` — Component list per version

### Vaadin Skills (invoke via Skill tool when relevant)

These skills load specialized context for Vaadin development tasks. **Use them proactively** when implementing Vaadin features — they contain up-to-date patterns and best practices.

- `vaadin-claude:views-and-navigation` — `@Route`, AppLayout, `@Layout`, SideNav, navigation
- `vaadin-claude:vaadin-layouts` — HorizontalLayout, VerticalLayout, alignment, flex
- `vaadin-claude:forms-and-validation` — Binder, validation, converters
- `vaadin-claude:data-providers` — Grid/ComboBox data binding, lazy loading
- `vaadin-claude:theming` — Aura/Lumo themes, design tokens, dark mode
- `vaadin-claude:frontend-design` — Visual polish, animations, styling beyond defaults
- `vaadin-claude:responsive-layouts` — Responsive design, breakpoints, container queries
- `vaadin-claude:reusable-components` — Component structure, Composite, extraction
- `vaadin-claude:security` — Spring Security, login, OAuth2, `@RolesAllowed`
- `vaadin-claude:signals` — Reactive state with ValueSignal, NumberSignal, ListSignal
- `vaadin-claude:client-side-views` — React/Hilla views, `@BrowserCallable`
- `vaadin-claude:third-party-components` — Web Components/React from npm
- `vaadin-claude:testbench-testing` — End-to-end browser tests with TestBench
- `vaadin-claude:ui-unit-testing` — Browser-free UI unit tests

### Other MCP Tools

- **Playwright:** `browser_snapshot`, `browser_navigate`, `browser_evaluate`, etc. — for verifying UI state, testing interactions, inspecting DOM
- **General:** `WebSearch`, `WebFetch` — for any external docs, Quill/Parchment APIs, npm packages

## Tech Stack

- Java 21+
- Vaadin 25.0.x — RTE is a **Pro component** (`vaadin` artifact, not `vaadin-core`)
- Spring Boot 4.x / Spring Framework 7
- Quill 2.0.3 (vendored by RTE 2)
- Parchment 3.x
- Jackson 3 (`tools.jackson`)
- Mockito 5.x
- JUnit 5

## Playwright Tests

381 total tests: 75 prototype + 306 ERTE. Full listing in [TEST_INVENTORY.md](enhanced-rich-text-editor-it/tests/TEST_INVENTORY.md).

**Running ERTE tests:**
```bash
# Build IT module first
bash build-it.sh
# Start IT server (port 8081)
bash it-server-start.sh
# Run ERTE tests
cd enhanced-rich-text-editor-it && npx playwright test tests/erte/
# Stop IT server
bash it-server-stop.sh
```

### ERTE Test Suite (306 tests in `enhanced-rich-text-editor-it/tests/erte/`)

| Spec File | Tests | Covers |
|-----------|-------|--------|
| `tabstops.spec.ts` | 86 | Tabstops, rulers, soft-break, whitespace indicators |
| `tables.spec.ts` | 82 | Table operations, templates, undo/redo, value round-trip |
| `features.spec.ts` | 36 | NBSP, addText, align, indent, i18n, sanitizer, focus |
| `toolbar.spec.ts` | 32 | Slot system, visibility, shortcuts, icons, keyboard nav |
| `placeholders.spec.ts` | 32 | Placeholder dialog, events, appearance, keyboard |
| `readonly.spec.ts` | 18 | Readonly sections, protection, whole-editor readonly |
| `replace-icons.spec.ts` | 10 | Replace standard toolbar button icons |
| `erte-shell.spec.ts` | 6 | Shell basics, Lit lifecycle, value sync |
| `extend-options.spec.ts` | 4 | extendQuill/extendEditor hooks, V24 deprecation |

**Test views** (Java, in `enhanced-rich-text-editor-it`, package `com.vaadin.componentfactory`): `ErteTabStopTestView`, `ErtePlaceholderTestView`, `ErteReadonlyTestView`, `ErteToolbarTestView`, `ErteExtendOptionsTestView`, `ErteFeatureTestView`, `ErteShellTestView`, `ErteReplaceIconTestView`, `ErteTablesTestView`, `ErteToolbarDialogTestView`, `ErteToolbarPopoverTestView`, `ErteToolbarSelectPopupTestView`. Each provides a single editor (`id="test-editor"`), delta/HTML output elements, event log, and a ready indicator.

**Side navigation:** `ErteTestLayout.java` provides an `AppLayout` with `SideNav` listing all test phases.

**Shared helpers** (`enhanced-rich-text-editor-it/tests/erte/helpers.ts`): `waitForEditor()`, `getDelta()`, `getDeltaFromEditor()`, `getRuler()`, `getRulerMarkers()`, etc.

### Prototype Tests (75 tests in `tab-stop-prototype.spec.ts`)

Original tabstop tests against the prototype view at `/tab-stop`. Remain in demo module.

### Key Test Patterns
- Shadow DOM: Playwright locators pierce it, but `page.evaluate()`/`waitForFunction` do NOT — use `el.shadowRoot.querySelector()`
- Ready indicator (`#test-ready`) has `display:none` — use `state: 'attached'`
- Readonly blot is a FORMAT attribute `{"attributes":{"readonly":true}}`, NOT an embed
- Placeholder dialog: use `[aria-label="Placeholders"]` to disambiguate from Link dialog
- Ruler markers are `vaadin-icon` elements inside `[part~="horizontalRuler"]`

## Custom Agents

Custom Claude Code agents in `.claude/agents/`. The orchestrator launches them directly via the `Task` tool.

| Agent | Purpose |
|-------|---------|
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
| **vaadin-expert** | Vaadin technical questions using official docs (read-only) |
| **end-user-reviewer** | Non-technical review of docs, UI text, error messages, UX flows |
| **housekeeper** | Cleanup: servers, Docker, temp files, screenshots |

**Usage:** Agents are launched via the `Task` tool with `subagent_type`. The orchestrator selects the appropriate agent(s) for each task, composes detailed prompts, and launches them directly — in parallel when independent, sequentially when dependent.

## License

CVALv3 (Commercial Vaadin Add-On License). License headers are enforced on Java files.
