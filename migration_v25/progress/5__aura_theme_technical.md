# Phase 5.1: Aura Theme Support — Technical Analysis (Post-6.0.0)

**Date:** 2026-02-21
**Updated:** 2026-02-22 (moved from Phase 3.4j to Phase 5.1)
**Status:** Research complete — analysis only, no implementation
**Author:** Claude Code (fullstack-developer agent)

> **Note:** ERTE 6.0.0 focuses on Lumo theme support only. Aura theme support is planned for a post-6.0.0 release (Phase 5.1).

---

## Executive Summary

Vaadin V25 uses TWO independent theme mechanisms:
1. **LumoInjector**: Injects shadow DOM styles via CSS custom properties (theme-agnostic despite the name)
2. **ThemeDetector**: Detects active theme and sets `data-application-theme` attribute (currently unused by any stock components)

**Key Finding:** ERTE V25 already has most of the infrastructure needed for Aura theme support via the `lumoInjector` override. However, ERTE uses 14 hardcoded `--lumo-*` tokens in its `static get styles()` that have NO Aura equivalents. These need to be replaced with theme-agnostic `--vaadin-*` generic tokens or custom ERTE-specific tokens.

**CSS `::part()` selectors:** Aura's `rich-text-editor.css` uses `vaadin-rich-text-editor::part(...)` selectors. These will NOT match ERTE's `vcf-enhanced-rich-text-editor` tag. Aura styling for RTE features will not apply to ERTE unless explicitly added.

---

## 1. LumoInjector Architecture

### How It Works

LumoInjector is **theme-agnostic** despite its name. It works for ANY theme that uses the `@media` module + CSS custom property pattern:

```css
/* Define modules using @media blocks (the class is called LumoInjector,
   but modules can be named anything — e.g., aura_button, theme_field) */
@media lumo_base-field {
  #label {
    color: gray;
  }
}

@media lumo_text-field {
  #input {
    color: yellow;
  }
}

/* Map modules to components via CSS custom properties */
html {
  --_lumo-vaadin-text-field-inject: 1;
  --_lumo-vaadin-text-field-inject-modules: lumo_base-field, lumo_text-field;
}
```

**Flow:**
1. Component connects → `LumoInjectionMixin.connectedCallback()`
2. Registers custom property `--_lumo-{tagName}-inject` (from `lumoInjector.is`)
3. `LumoInjector` observes this property via `CSSPropertyObserver`
4. When property value is `1`, parses all document stylesheets for matching modules
5. Extracts CSS rules from `@media` blocks listed in `--_lumo-{tagName}-inject-modules`
6. Creates a `CSSStyleSheet`, inserts rules, injects via `adoptedStyleSheets` API
7. Styles appear in component's shadow DOM, ordered between base styles and custom themes

**ERTE's override:**
```javascript
static get lumoInjector() {
  return { ...super.lumoInjector, is: 'vaadin-rich-text-editor' };
}
```

This makes ERTE listen for `--_lumo-vaadin-rich-text-editor-inject`, reusing RTE 2's Lumo modules. It works because:
- The injection mechanism is tag-based, not class-based
- ERTE extends RTE 2 → same shadow DOM structure → same CSS selectors work
- Toolbar icons, spacing, colors defined in Lumo's RTE modules apply to ERTE

**Does this work for Aura?**

**NO.** Aura does NOT use `@media` modules. It uses external CSS selectors (`vaadin-rich-text-editor { ... }` and `::part(...)`) that live in the document stylesheet, not injected into shadow DOM. The LumoInjector mechanism is entirely unused by Aura.

---

## 2. Aura CSS Delivery

Aura uses **external selectors** that pierce shadow DOM boundaries via `::part()`:

```css
/* Aura RTE styles (from aura/src/components/rich-text-editor.css) */

/* Host-level styles (applied to <vaadin-rich-text-editor>) */
vaadin-rich-text-editor {
  --vaadin-icon-visual-size: 90%;
  --vaadin-rich-text-editor-background: var(--aura-surface-color) padding-box;
  --aura-surface-level: 4;
  --aura-surface-opacity: 0.7 !important;
  box-shadow: var(--aura-shadow-xs);

  &:not(:focus-within) {
    --vaadin-rich-text-editor-toolbar-button-text-color: var(--vaadin-text-color-disabled);
  }
}

/* Part-level styles (applied to shadow DOM parts) */
vaadin-rich-text-editor::part(toolbar) {
  contain: paint;
  border-bottom: 1px solid var(--vaadin-border-color-secondary);
}

vaadin-rich-text-editor::part(toolbar-group)::before {
  content: '';
  position: absolute;
  width: 1px;
  height: 1lh;
  background: var(--vaadin-border-color-secondary);
  translate: calc(var(--vaadin-gap-m) / -2);
}

vaadin-rich-text-editor::part(toolbar-button) {
  transition: color 80ms, background-color 80ms, scale 180ms;
  outline-offset: calc(var(--vaadin-focus-ring-width) * -1);
  position: relative;
}

vaadin-rich-text-editor::part(toolbar-button-pressed) {
  --vaadin-rich-text-editor-toolbar-button-background: transparent;
}

vaadin-rich-text-editor:focus-within::part(toolbar-button-pressed) {
  --vaadin-rich-text-editor-toolbar-button-background: var(--vaadin-background-container-strong) padding-box;
  --vaadin-rich-text-editor-toolbar-button-text-color: var(--vaadin-text-color);
  border-color: var(--vaadin-text-color-disabled);
}

@media (any-hover: hover) {
  vaadin-rich-text-editor::part(toolbar-button):hover {
    --vaadin-rich-text-editor-toolbar-button-background: var(--vaadin-background-container);
    --vaadin-rich-text-editor-toolbar-button-text-color: var(--vaadin-text-color);
  }
}
```

**Impact on ERTE:**

- All selectors target `vaadin-rich-text-editor`
- ERTE uses tag `vcf-enhanced-rich-text-editor`
- **None of these rules will match ERTE**

**Why `::part()` selectors won't work:**

CSS `::part()` selectors match the **tag name** of the host element, not the inheritance chain:
```css
/* This matches <vaadin-rich-text-editor> */
vaadin-rich-text-editor::part(toolbar) { ... }

/* This does NOT match <vcf-enhanced-rich-text-editor> */
/* Even though ERTE extends RTE 2 in JS */
```

**Conclusion:** ERTE will NOT receive Aura's RTE-specific styling unless we add duplicate selectors for `vcf-enhanced-rich-text-editor`.

---

## 3. ThemeDetectionMixin

### What It Does

`ThemeDetectionMixin` detects which theme is active and sets a `data-application-theme` attribute on the host element:

```javascript
// From vaadin-theme-detection-mixin.js
__applyDetectedTheme() {
  const themes = this.__themeDetector.themes;
  if (themes.aura) {
    this.dataset.applicationTheme = 'aura';
  } else if (themes.lumo) {
    this.dataset.applicationTheme = 'lumo';
  } else {
    delete this.dataset.applicationTheme;
  }
}
```

Theme detection works by observing CSS custom properties:
```css
/* Aura sets this */
:root { --vaadin-aura-theme: 1; }

/* Lumo sets this */
:root { --vaadin-lumo-theme: 1; }
```

### Which Components Use It?

**None of the stock Vaadin components use ThemeDetectionMixin.** Searching the entire `web-components` reference codebase finds only:
- The mixin definition itself
- Test file for the mixin

**Does RTE 2 use it?** NO.

RTE 2's mixin chain (from `vaadin-rich-text-editor.js`):
```javascript
class RichTextEditor extends
  ElementMixin(
    ThemableMixin(
      LumoInjectionMixin(
        RichTextEditorMixin(
          PolylitMixin(LitElement)
        )
      )
    )
  )
```

**No `ThemeDetectionMixin` in the chain.**

### Should ERTE Use It?

**Only if ERTE needs theme-conditional JavaScript behavior.** For example:
- Different keyboard shortcuts per theme
- Different feature defaults (e.g., Aura enables placeholders by default, Lumo doesn't)
- Theme-specific blot behavior

For **styling only**, `ThemeDetectionMixin` is NOT needed. Use CSS custom properties and `::part()` selectors instead.

**Current assessment:** ERTE does NOT need theme-conditional JS behavior. Skip this mixin.

---

## 4. ERTE `--lumo-*` Token Inventory

ERTE's `static get styles()` uses **14 `--lumo-*` tokens** across 5 features:

### 4.1 Readonly Sections (3 tokens)

```css
.ql-readonly {
  color: var(--lumo-secondary-text-color);
  background-color: var(--lumo-contrast-5pct);
  border-radius: var(--lumo-border-radius-s);
  outline: 1px solid var(--lumo-contrast-10pct);
}
```

**Aura equivalents:**
- `--lumo-secondary-text-color` → `--vaadin-text-color-secondary` ✅ (generic token, both themes define it)
- `--lumo-contrast-5pct` → **NO Aura equivalent** ❌
- `--lumo-border-radius-s` → `--vaadin-radius-s` ✅ (generic token)
- `--lumo-contrast-10pct` → **NO Aura equivalent** ❌

**CSS fallback:** None. Will render with browser default if token is undefined.

### 4.2 Placeholders (2 tokens)

```css
.ql-placeholder {
  background-color: var(--lumo-primary-color-10pct);
  border-radius: var(--lumo-border-radius-s);
}
```

**Aura equivalents:**
- `--lumo-primary-color-10pct` → **NO Aura equivalent** ❌
- `--lumo-border-radius-s` → `--vaadin-radius-s` ✅

**CSS fallback:** None for `--lumo-primary-color-10pct`. Placeholder will have no background.

### 4.3 Whitespace Indicators (9 tokens)

```css
/* Tab indicator */
.show-whitespace span.ql-tab::after {
  font-size: var(--lumo-font-size-m, 1rem);
  color: var(--lumo-contrast-40pct, rgba(0, 0, 0, 0.38));
}

/* Soft-break indicator */
.show-whitespace span.ql-soft-break::before {
  font-size: var(--lumo-font-size-s, 0.875rem);
  color: var(--lumo-contrast-40pct, rgba(0, 0, 0, 0.38));
}

/* Paragraph indicator */
.show-whitespace p:not(:last-child)::after {
  font-size: var(--lumo-font-size-s, 0.875rem);
  color: var(--lumo-contrast-30pct, rgba(0, 0, 0, 0.26));
}

/* NBSP indicator */
.show-whitespace span.ql-nbsp::before {
  font-size: var(--lumo-font-size-m, 1rem);
  color: var(--lumo-contrast-40pct, rgba(0, 0, 0, 0.38));
}
```

**Aura equivalents:**
- `--lumo-font-size-m` → **NO Aura equivalent** (has hardcoded CSS fallback `1rem`) ⚠️
- `--lumo-font-size-s` → **NO Aura equivalent** (has hardcoded CSS fallback `0.875rem`) ⚠️
- `--lumo-contrast-40pct` → **NO Aura equivalent** (has hardcoded CSS fallback `rgba(0,0,0,0.38)`) ⚠️
- `--lumo-contrast-30pct` → **NO Aura equivalent** (has hardcoded CSS fallback `rgba(0,0,0,0.26)`) ⚠️

**CSS fallback:** Present for all tokens. Will render with hardcoded values in Aura.

**Problem with fallbacks:** Hardcoded `rgba(0, 0, 0, ...)` values don't adapt to dark mode. In Aura dark theme, these will render as black text on dark background → invisible.

### Summary Table

| Token | Feature | Aura Equivalent | Fallback | Status |
|-------|---------|-----------------|----------|--------|
| `--lumo-secondary-text-color` | Readonly | `--vaadin-text-color-secondary` | None | ✅ Can migrate |
| `--lumo-contrast-5pct` | Readonly | **None** | None | ❌ Need custom token |
| `--lumo-contrast-10pct` | Readonly | **None** | None | ❌ Need custom token |
| `--lumo-border-radius-s` | Readonly, Placeholder | `--vaadin-radius-s` | None | ✅ Can migrate |
| `--lumo-primary-color-10pct` | Placeholder | **None** | None | ❌ Need custom token |
| `--lumo-font-size-m` | Whitespace | **None** | `1rem` | ⚠️ Fallback OK |
| `--lumo-font-size-s` | Whitespace | **None** | `0.875rem` | ⚠️ Fallback OK |
| `--lumo-contrast-40pct` | Whitespace | **None** | `rgba(0,0,0,0.38)` | ❌ Dark mode broken |
| `--lumo-contrast-30pct` | Whitespace | **None** | `rgba(0,0,0,0.26)` | ❌ Dark mode broken |

**Tokens needing replacement:** 5 (`--lumo-contrast-*` and `--lumo-primary-color-10pct`)

---

## 5. Aura Surface/Shadow System

ERTE's `lumoInjector` override uses `is: 'vaadin-rich-text-editor'`. Does this help with Aura?

**NO.** Aura does NOT use the LumoInjector mechanism at all. Aura CSS is delivered as external selectors in the document stylesheet (`aura.css`), not as shadow DOM injected modules.

The `lumoInjector` override is **Lumo-specific**. It reuses Lumo's `@media lumo_rich-text-editor { ... }` modules. Aura has no equivalent modules because Aura doesn't use the injection system.

**Aura surface system:**

Aura defines custom properties for layered backgrounds:
```css
vaadin-rich-text-editor {
  --vaadin-rich-text-editor-background: var(--aura-surface-color) padding-box;
  --aura-surface-level: 4;
  --aura-surface-opacity: 0.7 !important;
  box-shadow: var(--aura-shadow-xs);
}
```

This selector targets `vaadin-rich-text-editor`, NOT `vcf-enhanced-rich-text-editor`. ERTE will NOT receive these styles.

---

## 6. File-by-File Change List

### Approach Options

**Option A: Replace `--lumo-*` with generic `--vaadin-*` tokens (recommended)**

- **Pro:** Works for both Lumo and Aura with one codebase
- **Pro:** Aligns with Vaadin's cross-theme design token strategy
- **Con:** Some tokens have no `--vaadin-*` equivalent (need custom ERTE tokens)

**Option B: Separate Aura styles using conditional CSS**

- **Pro:** Can use theme-specific tokens
- **Con:** Duplicates styles, harder to maintain
- **Con:** Requires `ThemeDetectionMixin` or `data-application-theme` attribute

**Option C: Define custom ERTE tokens with theme-specific values**

- **Pro:** Clean abstraction, single source of truth
- **Con:** Requires CSS in BOTH themes' stylesheets (Lumo AND Aura)

**Recommended:** **Option A + C hybrid** — Replace with generic tokens where they exist, define custom ERTE tokens for the rest.

---

### Files Requiring Modification

#### 6.1 `vcf-enhanced-rich-text-editor.js`

**Change:** Replace Lumo-specific tokens with generic/custom tokens in `static get styles()`.

**Before:**
```css
.ql-readonly {
  color: var(--lumo-secondary-text-color);
  background-color: var(--lumo-contrast-5pct);
  border-radius: var(--lumo-border-radius-s);
  outline: 1px solid var(--lumo-contrast-10pct);
}
```

**After:**
```css
.ql-readonly {
  color: var(--vaadin-text-color-secondary);
  background-color: var(--vcf-erte-readonly-background, rgba(0, 0, 0, 0.05));
  border-radius: var(--vaadin-radius-s);
  outline: 1px solid var(--vcf-erte-readonly-outline, rgba(0, 0, 0, 0.1));
}
```

**Token mapping:**

| Old Token | New Token | Notes |
|-----------|-----------|-------|
| `--lumo-secondary-text-color` | `--vaadin-text-color-secondary` | Generic token, both themes define it |
| `--lumo-border-radius-s` | `--vaadin-radius-s` | Generic token |
| `--lumo-contrast-5pct` | `--vcf-erte-readonly-background` | Custom ERTE token with fallback |
| `--lumo-contrast-10pct` | `--vcf-erte-readonly-outline` | Custom ERTE token with fallback |
| `--lumo-primary-color-10pct` | `--vcf-erte-placeholder-background` | Custom ERTE token with fallback |
| `--lumo-font-size-m` | `1rem` | Hardcoded, already has fallback |
| `--lumo-font-size-s` | `0.875rem` | Hardcoded, already has fallback |
| `--lumo-contrast-40pct` | `--vcf-erte-indicator-color` | Custom token, light/dark aware |
| `--lumo-contrast-30pct` | `--vcf-erte-indicator-color-subtle` | Custom token, light/dark aware |

**CSS fallback strategy:**

For custom tokens, provide light/dark fallbacks using `light-dark()`:
```css
.ql-readonly {
  background-color: var(
    --vcf-erte-readonly-background,
    light-dark(rgba(0, 0, 0, 0.05), rgba(255, 255, 255, 0.05))
  );
}

.show-whitespace span.ql-tab::after {
  color: var(
    --vcf-erte-indicator-color,
    light-dark(rgba(0, 0, 0, 0.38), rgba(255, 255, 255, 0.38))
  );
}
```

**Note:** `light-dark()` is a CSS Color Level 5 feature, supported in modern browsers. If older browser support is needed, use media queries:
```css
@media (prefers-color-scheme: light) {
  .ql-readonly {
    background-color: var(--vcf-erte-readonly-background, rgba(0, 0, 0, 0.05));
  }
}
@media (prefers-color-scheme: dark) {
  .ql-readonly {
    background-color: var(--vcf-erte-readonly-background, rgba(255, 255, 255, 0.05));
  }
}
```

**Total changes:** 14 token replacements across 5 feature blocks.

---

#### 6.2 Lumo Theme Stylesheet (optional)

If we want to preserve exact Lumo appearance, define custom ERTE tokens in a Lumo-specific stylesheet:

**File:** `enhanced-rich-text-editor-v25/src/main/resources/META-INF/resources/frontend/styles/erte-lumo-tokens.css`

```css
:root {
  --vcf-erte-readonly-background: var(--lumo-contrast-5pct);
  --vcf-erte-readonly-outline: var(--lumo-contrast-10pct);
  --vcf-erte-placeholder-background: var(--lumo-primary-color-10pct);
  --vcf-erte-indicator-color: var(--lumo-contrast-40pct);
  --vcf-erte-indicator-color-subtle: var(--lumo-contrast-30pct);
}
```

**Import:** Add `@CssImport` to `EnhancedRichTextEditor.java`:
```java
@CssImport("./styles/erte-lumo-tokens.css")
public class EnhancedRichTextEditor extends RteExtensionBase {
```

**Alternative:** Use `@media` modules for Lumo injection (follows Vaadin's pattern):
```css
@media lumo_enhanced-rich-text-editor {
  :host {
    --vcf-erte-readonly-background: var(--lumo-contrast-5pct);
    --vcf-erte-readonly-outline: var(--lumo-contrast-10pct);
    --vcf-erte-placeholder-background: var(--lumo-primary-color-10pct);
    --vcf-erte-indicator-color: var(--lumo-contrast-40pct);
    --vcf-erte-indicator-color-subtle: var(--lumo-contrast-30pct);
  }
}

html {
  --_lumo-vcf-enhanced-rich-text-editor-inject: 1;
  --_lumo-vcf-enhanced-rich-text-editor-inject-modules: lumo_enhanced-rich-text-editor;
}
```

**Then update `lumoInjector`:**
```javascript
static get lumoInjector() {
  // Keep parent's tag for toolbar icons + add own tag for ERTE-specific tokens
  return {
    ...super.lumoInjector,
    is: 'vcf-enhanced-rich-text-editor',  // Changed from 'vaadin-rich-text-editor'
    inheritFrom: 'vaadin-rich-text-editor'  // New property (need to implement)
  };
}
```

**Problem:** LumoInjector doesn't support `inheritFrom`. Would need custom implementation.

**Simpler approach:** Keep two separate `lumoInjector` mechanisms:
1. Reuse RTE 2's tag for toolbar (current pattern)
2. Add second injection for ERTE-specific tokens

This is complex. **Stick with `@CssImport` for simplicity.**

---

#### 6.3 Aura Theme Stylesheet (NEW)

Create Aura-specific ERTE token definitions:

**File:** `aura-erte-tokens.css` (in Vaadin's Aura package, NOT in ERTE repo)

```css
/* Aura definitions for ERTE custom tokens */
:root {
  --vcf-erte-readonly-background: color-mix(in oklch, var(--vaadin-text-color) 5%, transparent);
  --vcf-erte-readonly-outline: color-mix(in oklch, var(--vaadin-text-color) 10%, transparent);
  --vcf-erte-placeholder-background: color-mix(in oklch, var(--aura-accent-color) 10%, transparent);
  --vcf-erte-indicator-color: color-mix(in oklch, var(--vaadin-text-color) 40%, transparent);
  --vcf-erte-indicator-color-subtle: color-mix(in oklch, var(--vaadin-text-color) 30%, transparent);
}
```

**Location:** This file belongs in Vaadin's `@vaadin/aura` package, NOT in ERTE. It should be imported by `aura.css` alongside other component stylesheets.

**ERTE cannot ship this file** because:
1. ERTE is a standalone addon, not part of Vaadin platform
2. Aura is a Pro theme, ERTE is open source
3. Circular dependency: Aura would need to import from ERTE

**Solution:** Document the required tokens in ERTE's README with recommended Aura definitions. Users must add these tokens to their project's theme CSS if using Aura.

---

#### 6.4 Aura RTE Selectors (NEW)

Duplicate RTE 2's Aura selectors for ERTE's tag name:

**File:** `aura-erte-component.css` (in Vaadin's Aura package)

```css
/* Copy from aura/src/components/rich-text-editor.css, replace tag name */
vcf-enhanced-rich-text-editor {
  --vaadin-icon-visual-size: 90%;
  --vaadin-rich-text-editor-background: var(--aura-surface-color) padding-box;
  --aura-surface-level: 4;
  --aura-surface-opacity: 0.7 !important;
  box-shadow: var(--aura-shadow-xs);

  &:not(:focus-within) {
    --vaadin-rich-text-editor-toolbar-button-text-color: var(--vaadin-text-color-disabled);
  }
}

vcf-enhanced-rich-text-editor::part(toolbar) {
  contain: paint;
  border-bottom: 1px solid var(--vaadin-border-color-secondary);
}

vcf-enhanced-rich-text-editor::part(toolbar-group) {
  gap: 1px;
  align-items: center;
}

vcf-enhanced-rich-text-editor::part(toolbar-group)::before {
  content: '';
  position: absolute;
  width: 1px;
  height: 1lh;
  background: var(--vaadin-border-color-secondary);
  translate: calc(var(--vaadin-gap-m) / -2);
}

vcf-enhanced-rich-text-editor::part(toolbar-button) {
  transition: color 80ms, background-color 80ms, scale 180ms;
  outline-offset: calc(var(--vaadin-focus-ring-width) * -1);
  position: relative;
}

vcf-enhanced-rich-text-editor::part(toolbar-button-pressed) {
  --vaadin-rich-text-editor-toolbar-button-background: transparent;
}

vcf-enhanced-rich-text-editor:focus-within::part(toolbar-button-pressed) {
  --vaadin-rich-text-editor-toolbar-button-background: var(--vaadin-background-container-strong) padding-box;
  --vaadin-rich-text-editor-toolbar-button-text-color: var(--vaadin-text-color);
  border-color: var(--vaadin-text-color-disabled);
}

vcf-enhanced-rich-text-editor::part(toolbar-button):active {
  scale: 0.95;
  transition-duration: 80ms, 80ms, 50ms;
}

@media (any-hover: hover) {
  vcf-enhanced-rich-text-editor::part(toolbar-button):hover {
    --vaadin-rich-text-editor-toolbar-button-background: var(--vaadin-background-container);
    --vaadin-rich-text-editor-toolbar-button-text-color: var(--vaadin-text-color);
  }
}
```

**Same issue:** This file belongs in Vaadin's Aura package, not ERTE.

---

#### 6.5 Documentation (ERTE README)

Add Aura theme support section:

```markdown
## Aura Theme Support

ERTE V25 is compatible with Vaadin's Aura theme, but requires additional CSS token definitions.

### Required Tokens

Add these custom properties to your application's theme CSS (e.g., in `styles.css` or a custom theme module):

\`\`\`css
:root {
  /* Readonly sections */
  --vcf-erte-readonly-background: color-mix(in oklch, var(--vaadin-text-color) 5%, transparent);
  --vcf-erte-readonly-outline: color-mix(in oklch, var(--vaadin-text-color) 10%, transparent);

  /* Placeholders */
  --vcf-erte-placeholder-background: color-mix(in oklch, var(--aura-accent-color) 10%, transparent);

  /* Whitespace indicators */
  --vcf-erte-indicator-color: color-mix(in oklch, var(--vaadin-text-color) 40%, transparent);
  --vcf-erte-indicator-color-subtle: color-mix(in oklch, var(--vaadin-text-color) 30%, transparent);
}
\`\`\`

### Aura Component Styling

To match Aura's RTE styling, add these selectors to your theme:

\`\`\`css
vcf-enhanced-rich-text-editor {
  --vaadin-rich-text-editor-background: var(--aura-surface-color) padding-box;
  --aura-surface-level: 4;
  box-shadow: var(--aura-shadow-xs);
}

vcf-enhanced-rich-text-editor::part(toolbar) {
  border-bottom: 1px solid var(--vaadin-border-color-secondary);
}

/* Full selectors: see documentation/aura-theme-example.css */
\`\`\`

Alternatively, if you have access to Vaadin's platform source, contribute these definitions to `@vaadin/aura` package.
```

---

## 7. Implementation Recommendations

### Minimal Viable Aura Support (Low Effort)

**Goal:** ERTE works in Aura theme with reasonable defaults, no visual glitches.

**Changes:**
1. Replace 4 tokens in `vcf-enhanced-rich-text-editor.js`:
   - `--lumo-secondary-text-color` → `--vaadin-text-color-secondary`
   - `--lumo-border-radius-s` → `--vaadin-radius-s`
2. Add `light-dark()` fallbacks for remaining 5 custom tokens:
   ```css
   background-color: var(--vcf-erte-readonly-background, light-dark(rgba(0,0,0,0.05), rgba(255,255,255,0.05)));
   ```
3. Document required tokens in README

**Result:**
- ERTE features render correctly in both light and dark Aura
- Visual style won't match Aura's RTE exactly (missing surface effects, toolbar styling)
- Users can customize via custom tokens if desired

**Estimated effort:** 2-3 hours (token replacement + testing)

---

### Full Aura Integration (High Effort)

**Goal:** ERTE looks identical to RTE 2 in Aura theme.

**Changes:**
1. All changes from Minimal approach
2. Create `erte-lumo-tokens.css` with Lumo-specific token values
3. Create example Aura stylesheet (`aura-erte-example.css`) with full RTE selectors
4. Contact Vaadin team to integrate ERTE support into official Aura package
5. Add `ThemeDetectionMixin` if theme-conditional behavior is needed (currently not)

**Result:**
- Pixel-perfect Aura styling
- Official Vaadin integration (if accepted)
- Future Aura updates automatically apply to ERTE

**Estimated effort:** 2-3 weeks (includes Vaadin team coordination, testing, PR reviews)

---

### Recommended Path

**Start with Minimal approach** for Phase 3.4j completion. Full integration can be deferred to Phase 5 (post-Tables) or a future major version.

**Rationale:**
1. Aura adoption is still growing — most users likely use Lumo
2. Custom token fallbacks provide acceptable UX
3. Avoids external dependencies (waiting for Vaadin team)
4. Allows ERTE to ship without blocking on platform changes

If Aura usage grows and users request better integration, upgrade to Full approach in a later release.

---

## 8. Testing Strategy

### Lumo Theme Tests (Already Passing)

Current test suite (186 tests) runs against Lumo theme. No changes needed.

### Aura Theme Tests (New)

**Manual testing approach:**
1. Create test application with `@Theme(value = Aura.class)`
2. Run full ERTE test suite against Aura-themed app
3. Visual regression: Compare screenshots of Lumo vs Aura for each feature
4. Dark mode: Test all features in Aura dark theme

**Automated testing:**
- Playwright tests can run against both themes by changing `@Theme` annotation
- Add theme parameter to test configuration
- Run full suite twice: once with Lumo, once with Aura

**Visual validation:**
- Readonly sections: Background visible, not transparent
- Placeholders: Background visible in both light/dark
- Whitespace indicators: Visible in dark mode (not black on black)
- Toolbar: Matches Aura's button/group styling (if full integration)

---

## 9. Open Questions

1. **Vaadin platform integration:** Should ERTE coordinate with Vaadin team to add official Aura support to the platform package, or ship as addon-only with documentation?

2. **`lumoInjector` renaming:** The property is called `lumoInjector` but works for all themes. Should this be renamed to `themeInjector` or kept as-is for backward compatibility?

3. **Custom token namespace:** Use `--vcf-erte-*` prefix (matches addon namespace) or `--vaadin-erte-*` (matches Vaadin convention)?

4. **Fallback strategy:** Use `light-dark()` (modern, clean) or media queries (broader compatibility)?

5. **Toolbar icon inheritance:** ERTE currently reuses RTE 2's Lumo toolbar icons via `is: 'vaadin-rich-text-editor'`. Should this be changed to `is: 'vcf-enhanced-rich-text-editor'` with separate icon definitions, or kept for maintainability?

---

## 10. Conclusion

**Aura theme support is achievable with moderate effort.** The main challenges are:

1. **No Aura equivalents for 5 Lumo-specific tokens** → Requires custom ERTE tokens with fallbacks
2. **CSS selectors target wrong tag name** → Requires either user-provided CSS or Vaadin platform integration
3. **No LumoInjector usage by Aura** → ERTE's current injection override doesn't help with Aura

**Recommended implementation:**
- **Phase 3.4j:** Replace 4 tokens with generic `--vaadin-*` tokens, add `light-dark()` fallbacks for 5 custom tokens, document Aura setup in README
- **Phase 5 or later:** Full Aura integration with coordinated Vaadin platform PR

**Critical finding:** Hardcoded `rgba(0, 0, 0, ...)` fallbacks in whitespace indicators will break in dark mode. This must be fixed regardless of Aura support timeline.

---

**Next Steps:**
1. Review this analysis with user
2. Decide on Minimal vs Full approach
3. Create implementation plan for chosen approach
4. Update Phase 3.4j progress file with implementation tasks
