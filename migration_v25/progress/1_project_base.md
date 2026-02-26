# Phase 1: Project Base — Progress

## Status: COMPLETE

## Plan Summary

Create V25 Maven module infrastructure, upgrade demo to Vaadin 25 / Spring Boot 4 / Java 21, verify clean compilation. No ERTE features — that's Phase 2+.

### Version Matrix

| Dependency | V24 (current) | V25 (target) |
|-----------|--------------|-------------|
| Java | 17 | **21** |
| Vaadin BOM | 24.9.7 | **25.0.5** |
| Spring Boot | 3.5.9 | **4.0.2** |
| Vaadin artifact | `vaadin-core` | **`vaadin`** (RTE = Pro) |
| Mockito | 1.10.19 + PowerMock | **5.14.2** (no PowerMock) |
| ERTE version | 5.2.0-SNAPSHOT | **6.0.0-SNAPSHOT** |
| Tables version | 1.1.0-SNAPSHOT | **2.0.0-SNAPSHOT** |

### Architecture Decision: RteExtensionBase Pattern

Changed from original plan (ERTE directly in RTE 2 package) to bridge pattern:

- **`RteExtensionBase`** in `com.vaadin.flow.component.richtexteditor` (RTE 2's package)
  - Extends `RichTextEditor`
  - Lifts package-private members to `protected`
  - Only class in the foreign package
- **`EnhancedRichTextEditor`** in `com.vaadin.componentfactory` (ERTE's native package)
  - Extends `RteExtensionBase`
  - All real ERTE logic lives here

**Rationale:** Avoids tearing up the package structure. If RTE 2 changes package-private visibility, only `RteExtensionBase` needs updating.

### Style Rewriting: Phase 2, NOT Phase 1

Polymer→Lit style migration (`<custom-style>`, `registerStyles()` → `static get styles()`) happens in Phase 2 when the JS subclass is created. Phase 1 has no styles.

### Steps

1. Create `enhanced-rich-text-editor/` module (pom.xml, RteExtensionBase stub, ERTE stub, license)
2. Create `enhanced-rich-text-editor-tables/` module (pom.xml, stub, license)
3. Update root `pom.xml` (V25 modules only in reactor)
4. Update demo `pom.xml` (V25 deps, Spring Boot 4, Java 21)
5. Relocate V24-specific demo views to `src-v24/`
6. Update `Application.java` (`@StyleSheet(Lumo.STYLESHEET)`)
7. Create minimal V25 demo view (`RichTextEditor` — not ERTE stub)
8. Update `Dockerfile` (Java 21, V25 modules)
9. Update `v24-build.sh` (standalone builds with `-f`)
10. Update `TEST_INVENTORY.md` (Phase 1 status note)
11. Verification: `mvn clean install -DskipTests`, server start, browser check

### Files

| Action | File |
|--------|------|
| CREATE | `enhanced-rich-text-editor/pom.xml` |
| CREATE | `enhanced-rich-text-editor/.../RteExtensionBase.java` (in RTE 2 package) |
| CREATE | `enhanced-rich-text-editor/.../EnhancedRichTextEditor.java` (in ERTE package) |
| CREATE | `enhanced-rich-text-editor/src/license/` (copy from V24) |
| CREATE | `enhanced-rich-text-editor-tables/pom.xml` |
| CREATE | `enhanced-rich-text-editor-tables/.../EnhancedRichTextEditorTables.java` |
| CREATE | `enhanced-rich-text-editor-tables/src/license/` (copy from V24) |
| CREATE | `enhanced-rich-text-editor-demo/.../V25DemoView.java` |
| EDIT | `pom.xml` (root) |
| EDIT | `enhanced-rich-text-editor-demo/pom.xml` |
| EDIT | `enhanced-rich-text-editor-demo/.../Application.java` |
| EDIT | `Dockerfile` |
| EDIT | `v24-build.sh` |
| EDIT | `tests/TEST_INVENTORY.md` |
| MOVE | 9 demo views → `src-v24/` |

## Completed Steps

### Step 1: V25 ERTE module — DONE
- `enhanced-rich-text-editor/pom.xml` — Vaadin 25.0.5, Java 21, `vaadin` (Pro), Mockito 5.14.2
- `RteExtensionBase.java` in `com.vaadin.flow.component.richtexteditor` (bridge class)
- `EnhancedRichTextEditor.java` in `com.vaadin.componentfactory` (stub extends bridge)
- License files copied from V24

### Step 2: V25 tables module — DONE
- `enhanced-rich-text-editor-tables/pom.xml` — depends on ERTE V25 6.0.0-SNAPSHOT
- `EnhancedRichTextEditorTables.java` stub in `com.vaadin.componentfactory.erte.tables`
- License files copied

### Step 3: Root pom.xml — DONE
- Version → 6.0.0-SNAPSHOT, Java 21
- Modules: V25 ERTE, V25 tables, demo (V24 modules removed from reactor)
- Cleaned up commented-out legacy XML

### Step 4: Demo pom.xml — DONE
- Vaadin 25.0.5, Spring Boot 4.0.2, Java 21
- `vaadin` (Pro) + `vaadin-dev` (optional) + `vaadin-spring-boot-starter`
- ERTE V25 dependency, removed V24 ERTE/tables/flow-component-demo-helpers
- Removed: powermock, nexus-staging, maven-war, commented-out profiles
- Production profile cleaned (no vaadin-core exclusion needed)

### Step 5: V24 views → src-v24/ — DONE
- 7 views + 2 helpers moved
- Only Application.java + V25DemoView.java remain in active src

### Step 6: Application.java — DONE
- `@Theme(themeClass = Lumo.class)` → `@StyleSheet(Lumo.STYLESHEET)`
- Removed `@PageTitle`, removed unused imports

### Step 7: V25DemoView.java — DONE
- `@Route("")`, RichTextEditor (not ERTE stub), minimal Phase 1 page

### Step 8: Dockerfile — DONE
- Java 17 → 21, V24 modules → V25 modules

### Step 9: v24-build.sh — DONE
- Standalone builds with `-f` (V24 modules no longer in reactor)

### Step 10: TEST_INVENTORY.md — DONE
- Phase 1 status note about temporarily non-functional tests

### Step 11: Verification — DONE

All checks passed:

| Check | Result |
|-------|--------|
| `mvn clean install -DskipTests` (root) | BUILD SUCCESS (7.9s) |
| `bash v25-build.sh` | EXIT 0 |
| `bash v25-build-clean.sh` | EXIT 0 |
| `bash v24-build.sh` | EXIT 0 (standalone `-f` builds) |
| `bash v25-server-start.sh` | Server ready on :8082 in 7s |
| Browser: heading visible | "ERTE V25 — Phase 1 (Project Base)" |
| Browser: RTE renders | Full Quill 2 toolbar, Lumo theme |
| Console errors | Vaadin Copilot only (dev mode, not ERTE) |

### Extra fix: v24-build-clean.sh + v25-build-clean.sh
- Fixed `vaadin:clean-frontend` → `com.vaadin:vaadin-maven-plugin:clean-frontend` (plugin prefix not resolved without pluginGroup config)
- v24-build-clean.sh updated to use `-f` standalone builds (same as v24-build.sh)

## Phase 1 COMPLETE — ready for Phase 2 (ERTE Shell)
