# V25 Migration — ERTE to Vaadin 25 / Quill 2

## Analysis Documents

| File | Description |
|------|-------------|
| `user_description.md` | Starting point: description of all 20 ERTE features that need to be migrated |
| `agents_analysis.md` | Analysis of all 20 features with effort ratings (FREE / CUSTOM / REWRITE) |
| `feature_comparison.md` | Detailed comparison of ERTE 1 vs. RTE 2: what exists, what's missing, what changed |
| `implementation_notes.md` | Architecture decisions, open questions, and the 23 spike items (section 10) |
| `quill_v1_to_v2_api_diff.md` | API diff Quill 1.3.6 → 2.0.3: Blots, Keyboard, Delta, Parchment changes |

## Spike Project (`spike/`)

Runnable Vaadin 25 project to validate all architecture decisions.

| File / Directory | Description |
|------------------|-------------|
| `SPIKE_RESULTS.md` | Results for all 23 spike items + Table Spike (T1–T7) |
| `pom.xml` | Maven project: Vaadin 25.0.4, Spring Boot 4.0.2 |
| `src/main/java/.../EnhancedRichTextEditor.java` | Java subclass of RTE 2 with custom `@Tag` |
| `src/main/java/.../SpikeView.java` | Test UI with buttons for all spike items |
| `src/main/frontend/vcf-enhanced-rich-text-editor.js` | JS class: Lit extension, toolbar, Tab blot, Table blots (Parchment 3) |
| `server-start.sh` / `server-stop.sh` | Start/stop the server (port 8081) |
| `print-server-logs.sh` | View logs (`-f` follow, `-state` status, `-errors` errors only) |
| `screenshots/` | Playwright screenshots from visual verification (Phase 4) |
| `console-*.log` | Console captures from debug sessions |

## Other

| File | Description |
|------|-------------|
| `MEMORY_2026-02-16T165113.md` | Snapshot of Claude Code working memory (context backup) |
