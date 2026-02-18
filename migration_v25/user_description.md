# Migrating ERTE to Vaadin 25

## Terminology

| Term | Meaning |
|------|---------|
| **ERTE** | Enhanced Rich Text Editor — this project |
| **RTE** | Vaadin's built-in Rich Text Editor (commercial component) |
| **RTE 1** | The RTE in Vaadin 24 and earlier, based on Quill 1 |
| **RTE 2** | The RTE starting from Vaadin 25, based on Quill 2 |
| **ERTE 1** | The current (unmigrated) ERTE, built on top of RTE 1 |
| **ERTE 2** | The target: migrated ERTE, built on top of RTE 2 |

## Background

ERTE 1 was originally forked from RTE 1. Over time, the codebase gap between the two has grown — new features and fixes added to RTE 1 were never ported back into ERTE 1. Additionally, Quill 1 is now outdated and RTE 2 has moved to Quill 2.

The goal of this document is to describe the migration from ERTE 1 to ERTE 2, using RTE 2 as the new base.

## Non-Regression

All features currently available in ERTE 1 must remain available in ERTE 2. To ensure this, the complete feature inventory in Step 0 must be reviewed and validated against the migrated result.

A previous attempt simply cut out features like the placeholder dialog. This is not acceptable. For the user, there must be no feature regression like that. If you come to a feature where no clear path of migration is obvious, you are obliged to ASK FOR NEXT STEPS.

## Updatability

A key goal is to avoid repeating the mistake of maintaining a full copy of the RTE codebase. Instead of forking, ERTE 2 should **extend** RTE 2, so that future updates, fixes, and new features in RTE 2 are automatically inherited by ERTE 2.

This means ERTE 2 will not copy and modify the RTE source code. Instead, it should hook into the existing codebase at runtime wherever possible.

For example: the RTE toolbar does not support dynamic modification or custom items. ERTE 1 solved this by maintaining a full copy of the toolbar with hardcoded slots — but this means toolbar updates in RTE never reach ERTE. A better approach would be to inject ERTE's toolbar slots at runtime using DOM selectors and JavaScript, rather than duplicating the entire toolbar template.

This runtime-extension approach will be the most challenging part of the migration, as it requires working around the limited extensibility of RTE 2. But it is essential for long-term maintainability.

## Migration progress
Document your progress, open points and potential issues.

## Migration Steps

### Step 0: Use Case Analysis (prerequisite)

Before any migration work begins, every ERTE 1 feature must be checked against RTE 2 and Quill 2 to determine:

- **Already available in RTE 2 / Quill 2?** → No migration needed, just verify behavior.
- **Not available, clear migration path?** → Document the approach.
- **Not available, no clear path?** → Flag and ask for next steps before proceeding.

#### Complete ERTE 1 Feature Inventory

**Custom Slots / Custom Components**
Allows adding custom Java components (buttons, or arbitrary components) to the editor toolbar via custom slots. This is the primary extensibility mechanism and the architectural foundation for several other features.

**Readonly Mode**
Programmatic control to toggle the editor into a non-editable state.

**Tabstops**
Tabstops can be set in the UI by clicking on a horizontal ruler above the editor. Three directions are supported:
- **Left:** left side of text aligns to right side of tab stop (>text)
- **Right:** right side of text aligns to left side of tab stop (text<)
- **Middle:** text is centered on the tab stop (te|xt)

Clicking the ruler creates a left tabstop. Clicking an existing tabstop cycles through left → right → middle. Tabstops can also be set programmatically via the `tabStops` property.

**Placeholders**
Insertable placeholder tokens with a dedicated dialog. See ERTE 1 demo for details.

**Non-Breaking Space**
Pressing `Shift+Space` inserts a non-breaking space at the caret position.

**Toolbar Button Visibility**
`setToolbarButtonsVisibility` allows showing/hiding individual standard toolbar buttons programmatically.

**Icon Replacement**
Each standard toolbar button includes a slot for its icon, allowing icon replacement from Java.

**Soft Wraps**
Visual indication / control of soft line wraps in the editor.

**Whitespace Indicators**
Visual display of whitespace characters (spaces, tabs, etc.) in the editor content.

**Tables (separate addon)**
Table support is provided by the Table Extension, a separate addon. It originated as a fork of a Quill 2 addon that was backported to Quill 1, then modified for ERTE 1.

### Step 1: Update the Project Base to Vaadin 25

Update to stable Vaadin 25.0.x (no pre-releases). This includes all dependencies, Maven profiles, and non-source configuration files — nothing may be dropped silently.

**Done when:** The V25 modules are created with all dependencies up to date and the project compiles cleanly.

### Step 2: Rebuild ERTE on Top of RTE 2

Keep updatability as the primary constraint. As little copy-and-paste as possible. Find clean ways to extend rather than duplicate, without dirty hacks or unusual quirks.

**RteExtensionBase:** The RTE has some package-protected methods that ERTE needs. Create an `RteExtensionBase` class in the RTE package that elevates visibility from package-protected to protected, so ERTE can extend from its own package.

⚠️ **Maintenance risk:** This class is a coupling point. On every RTE update, `RteExtensionBase` must be reviewed — if Vaadin renames or removes the underlying methods, ERTE 2 will break. Document this explicitly as a recurring maintenance task.

**Styling:** ERTE 1 styles still use the Polymer scheme. ERTE 2 must follow V25 conventions for shadow DOM and theme-relevant styling. Investigate how RTE 2 handles its internal styles and adopt the same approach. Use Lumo theme. Where `--vaadin-*` custom properties exist, use them.

### Step 3: Feature Migration

Migrate features one at a time in the following order. This order is intentional — custom slots come first because they establish the runtime-injection pattern that other features depend on.

1. **Custom Slots / Custom Components** — architectural foundation, must work first
2. **Readonly Mode**
3. **Tabstops**
4. **Placeholders**
5. Remaining features in any order:
    - Non-breaking space
    - Toolbar button visibility
    - Icon replacement
    - Soft wraps
    - Whitespace indicators

For each feature: first check Step 0 analysis. If RTE 2 / Quill 2 already provides it, verify and move on. If not, implement using the runtime-extension approach established in Step 2.

### Step 4: Table Extension (last)

The table addon has a complex lineage. Rather than migrating the adapted ERTE 1 code directly, the preferred approach is to evaluate whether the original Quill 2 addon can be used as a starting point, with ERTE-specific modifications on top.

**Feasibility must be assessed first.** If the original Quill 2 addon is not viable as a base, the fallback is to migrate the existing backported fork directly to Quill 2 / Parchment 3. This fallback is acceptable but less desirable due to the maintenance burden.

### Test Strategy

> ⚠️ TODO: Define how ERTE 2 will be validated against the feature inventory. Existing tests? Manual test cases? Browser automation? This must be clarified before Step 3 begins.