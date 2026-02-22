# Phase 5.1: Aura Theme Support (Post-6.0.0)

**Status:** NOT STARTED
**Type:** Feature Addition
**Priority:** DEFERRED — Post-6.0.0 release
**Created:** 2026-02-21
**Updated:** 2026-02-22 (moved from Phase 3.4j to Phase 5.1)

> **Note:** ERTE 6.0.0 focuses on Lumo theme support only. Aura theme support is planned for a post-6.0.0 release.

**Prerequisite:** Phase 5 Spike (`5__spike.md`) must be completed first to validate Aura Style Proxy feasibility

---

## Scope

Add support for Vaadin's Aura theme alongside existing Lumo theme support. Currently ERTE is Lumo-only, which limits adoption in Aura-based projects.

---

## Background

### Current State
- ERTE V25 uses 14 `--lumo-*` specific CSS custom properties
- These tokens do NOT exist in Aura theme → features break visually:
  - Readonly sections: invisible (no background/outline)
  - Placeholders: invisible (transparent background)
  - Whitespace indicators: broken in dark mode
- ERTE successfully inherits Lumo RTE styles via `lumoInjector` override
- Aura has NO equivalent injector mechanism (uses external CSS selectors instead)

### Analysis Documents
- **Technical:** `5__aura_theme_technical.md` (fullstack-developer)
- **Design:** `5__aura_theme_design.md` (ui-designer)

---

## Implementation Approach

### 1. Replace Lumo-Specific Tokens with Generic Vaadin Tokens

**Current (Lumo-only):**
```css
.ql-readonly {
  color: var(--lumo-secondary-text-color);
  background-color: var(--lumo-contrast-5pct);
  outline: 1px solid var(--lumo-contrast-10pct);
  border-radius: var(--lumo-border-radius-s);
}
```

**Target (Lumo + Aura):**
```css
.ql-readonly {
  color: var(--vaadin-text-color-secondary);
  background-color: var(--vaadin-background-container);
  outline: 1px solid var(--vaadin-border-color);
  border-radius: var(--vaadin-radius-s);
}
```

**Token Mapping (14 replacements):**
| Current Lumo Token | Generic Vaadin Token | Usage |
|-------------------|---------------------|-------|
| `--lumo-secondary-text-color` | `--vaadin-text-color-secondary` | Readonly text |
| `--lumo-contrast-5pct` | `--vaadin-background-container` | Readonly background |
| `--lumo-contrast-10pct` | `--vaadin-border-color` | Readonly outline |
| `--lumo-border-radius-s` | `--vaadin-radius-s` | Border radius |
| `--lumo-primary-color-10pct` | `color-mix(in srgb, var(--vaadin-primary-color) 10%, transparent)` | Placeholder background |
| `--lumo-font-size-m` | `1rem` | Base font size |
| `--lumo-font-size-s` | `0.875rem` | Small font size |
| `--lumo-contrast-40pct` | Remove hardcoded fallback, rely on `--vaadin-text-color-tertiary` | Indicator colors |
| `--lumo-contrast-30pct` | Remove hardcoded fallback, rely on `--vaadin-text-color-disabled` | Subtle indicators |

**Files to modify:**
- `vcf-enhanced-rich-text-editor.js` (lines 431-578, `static get styles()`)

---

### 2. Implement Aura Style Proxy (Runtime Injection)

**Problem:**
- Aura's RTE-specific styles use selectors like `vaadin-rich-text-editor::part(toolbar)`
- These selectors don't match ERTE's `vcf-enhanced-rich-text-editor` tag
- Hardcoding Aura styles would break updatability (Vaadin updates → ERTE out of sync)

**Solution: Runtime Style Proxy**

Add method to ERTE that:
1. Scans all document stylesheets for `vaadin-rich-text-editor` rules
2. Clones them with `vcf-enhanced-rich-text-editor` selector
3. Injects as new `<style>` element into document head

**Implementation:**

```javascript
// In vcf-enhanced-rich-text-editor.js

/** @override */
connectedCallback() {
  super.connectedCallback();
  this._injectAuraStyleProxy();
}

/**
 * Dynamically injects cloned theme styles for ERTE (temporary solution).
 *
 * BACKGROUND: Aura theme (and potentially future themes) use external CSS
 * selectors like `vaadin-rich-text-editor::part(toolbar)` which don't match
 * ERTE's `vcf-enhanced-rich-text-editor` tag. This method scans document
 * stylesheets for vaadin-rich-text-editor rules and creates equivalent rules
 * for vcf-enhanced-rich-text-editor, ensuring ERTE receives all theme-specific
 * RTE styles without hardcoding them.
 *
 * TEMPORARY: This is a workaround until Vaadin provides a native theme
 * injection mechanism for custom RTE extensions (similar to LumoInjector).
 * If Vaadin adds native support (e.g., an "AuraInjector" or generic
 * "ThemeInjector" that works for all themes and allows tag name override),
 * this method should be removed and replaced with the official mechanism.
 *
 * REMOVAL CRITERIA:
 * - Vaadin introduces a theme-agnostic injector (like LumoInjector but for all themes)
 * - OR: Vaadin's Aura package explicitly includes vcf-enhanced-rich-text-editor selectors
 * - OR: Vaadin documents an official pattern for extending RTE with custom tags
 *
 * @private
 */
_injectAuraStyleProxy() {
  // Avoid duplicate injection
  if (document.querySelector('style[data-vcf-erte-theme-proxy]')) {
    return;
  }

  const rteRules = [];

  // Scan all stylesheets
  for (const sheet of document.styleSheets) {
    try {
      for (const rule of sheet.cssRules) {
        if (rule.cssText && rule.cssText.includes('vaadin-rich-text-editor')) {
          rteRules.push(rule.cssText);
        }
      }
    } catch (e) {
      // CORS-protected sheets (CDN) - skip silently
      console.debug('ERTE: Skipping stylesheet (CORS):', sheet.href, e);
    }
  }

  if (rteRules.length === 0) {
    return; // No RTE-specific theme styles found
  }

  // Clone rules with ERTE tag name
  const proxyStyles = rteRules.map(cssText =>
    cssText.replace(/vaadin-rich-text-editor/g, 'vcf-enhanced-rich-text-editor')
  );

  // Inject into document
  const styleEl = document.createElement('style');
  styleEl.setAttribute('data-vcf-erte-theme-proxy', '');
  styleEl.textContent = `/* ERTE Theme Style Proxy (auto-generated) */\n${proxyStyles.join('\n')}`;
  document.head.appendChild(styleEl);

  console.debug(`ERTE: Injected ${rteRules.length} theme style rules`);
}
```

**Advantages:**
- ✅ **Updatability:** ERTE automatically gets Aura updates (on page reload)
- ✅ **Theme-agnostic:** Works for Lumo, Aura, and future themes
- ✅ **No hardcopy:** ERTE code stays clean

**Edge cases handled:**
- CORS-protected stylesheets (try-catch, skip silently)
- Timing (themes load before components in Vaadin)
- Duplicate injection (guard with data attribute)

---

## Design Consideration: Generic Theme Injector

### Current Situation

**Two separate theme injection mechanisms:**
1. **LumoInjector** (existing in Vaadin 25)
   - ERTE overrides `lumoInjector` getter to return `{is: 'vaadin-rich-text-editor'}`
   - This makes ERTE inherit Lumo's RTE styles

2. **AuraInjector** (planned: style proxy above)
   - Runtime scanning of stylesheets
   - Cloning rules with ERTE tag name
   - Injecting as `<style>` element

**Problem:** Both mechanisms do essentially the same thing (inject theme-specific RTE styles for ERTE), just using different technical approaches. Having two separate implementations creates:
- Code duplication
- Maintenance overhead
- Different behavior patterns for different themes

### Alternative: Generic VaadinThemeInjector

**Proposal:** Create a unified `VaadinThemeInjector` that encapsulates both mechanisms:

```javascript
/**
 * Generic theme injection mechanism for ERTE.
 * Handles both Lumo (via injector override) and Aura/other themes (via style proxy).
 */
class VaadinThemeInjector {
  constructor(component) {
    this.component = component;
  }

  /**
   * Returns theme-specific injector config (for Lumo-style injection)
   */
  getInjectorConfig() {
    return { is: 'vaadin-rich-text-editor' };
  }

  /**
   * Injects theme styles via runtime proxy (for Aura-style injection)
   */
  injectStyleProxy() {
    // Implementation from _injectAuraStyleProxy() above
  }

  /**
   * Initialize theme injection (called from connectedCallback)
   */
  init() {
    // Try injector-based approach first (Lumo)
    // Fall back to style proxy (Aura, others)
    this.injectStyleProxy();
  }
}
```

**Usage in ERTE:**
```javascript
class VcfEnhancedRichTextEditor extends RteBase {
  constructor() {
    super();
    this._themeInjector = new VaadinThemeInjector(this);
  }

  /** @override */
  static get lumoInjector() {
    return this._themeInjector?.getInjectorConfig() || { is: 'vaadin-rich-text-editor' };
  }

  /** @override */
  connectedCallback() {
    super.connectedCallback();
    this._themeInjector?.init();
  }
}
```

### Advantages

✅ **Single responsibility:** One class handles all theme injection
✅ **Consistency:** Same behavior pattern for all themes
✅ **Extensibility:** Easy to add support for future themes (Material, custom themes)
✅ **Maintainability:** Changes to theme injection logic happen in one place
✅ **Cleaner ERTE code:** Component delegates theme injection to specialized class

### Disadvantages

⚠️ **Added complexity:** Introduces new abstraction layer
⚠️ **Overhead:** Extra object creation (negligible performance impact)
⚠️ **Testing:** Need to test injector class separately

### Recommendation

**IMPORTANT:** This design consideration should be evaluated during Phase 5.1 planning:

1. **If Lumo + Aura are the ONLY themes ERTE will support:**
   - Current approach (override `lumoInjector` + add style proxy) is simpler
   - No need for abstraction layer

2. **If future themes are expected (Material, custom themes):**
   - Generic `VaadinThemeInjector` makes sense
   - Invest in abstraction now to save refactoring later

3. **If Vaadin plans to provide official theme injection API:**
   - Wait for official solution, don't over-engineer
   - Use temporary style proxy for now

**Decision point:** Include this evaluation in the planning phase before implementation begins.

---

### Source Code Analysis (v25.1.0-alpha8)

**Date checked:** 2026-02-22
**Version:** [v25.1.0-alpha8](https://github.com/vaadin/web-components/tree/v25.1.0-alpha8)

**Findings:**

✅ **LumoInjector exists** (confirmed in `vaadin-rich-text-editor.js`):
```javascript
static get lumoInjector() {
  return { ...super.lumoInjector, includeBaseStyles: true };
}
```

❌ **No AuraInjector found** - Aura theme does not have equivalent injection mechanism

❌ **No generic ThemeInjector found** - System is Lumo-specific:
- `LumoInjectionMixin` from `@vaadin/vaadin-themable-mixin/lumo-injection-mixin.js`
- Hardcoded `new LumoInjector(root)` - no theme selection parameter
- No abstraction layer for alternative themes

**Structure:**
- RTE package: Only base styles (`src/styles/vaadin-rich-text-editor-base-styles.js`)
- No Lumo/Aura theme files within RTE package itself
- Theme styles managed externally via injection mixin

**Conclusion for Point 3:**
Vaadin **does not** appear to be planning a generic theme injection API in the v25.1 timeline. The LumoInjector remains Lumo-specific with no visible roadmap for abstraction. Therefore:
- **Do not wait** for official multi-theme injection API
- **Implement style proxy** as planned (it's the only viable option for Aura)
- **Consider generic VaadinThemeInjector** if future theme support is desired (Material, custom themes)

---

## Tasks

### Step 1: Token Replacement
- [ ] Replace 14 `--lumo-*` tokens with `--vaadin-*` equivalents in `static get styles()`
- [ ] Use `color-mix()` for placeholder background (maintains accent color semantic)
- [ ] Remove hardcoded `rgba(0,0,0,...)` fallbacks (cause dark mode issues)
- [ ] Verify CSS syntax (especially `color-mix()` browser support: Safari 16.4+, Chrome 111+)

### Step 2: Aura Style Proxy
- [ ] Add `_injectAuraStyleProxy()` method to ERTE
- [ ] Call from `connectedCallback()`
- [ ] Add duplicate injection guard
- [ ] Add debug logging (console.debug)
- [ ] Handle CORS errors gracefully

### Step 3: Testing
- [ ] Manual test: Create demo view with Aura theme
- [ ] Verify all features work visually:
  - Readonly sections (background, outline, text color)
  - Placeholders (accent-based background)
  - Whitespace indicators (proper colors in light/dark mode)
  - Tab stops
  - Toolbar styling
- [ ] Test Lumo theme (ensure no regression)
- [ ] Test dark mode for both themes
- [ ] Verify WCAG AA contrast ratios (use browser dev tools)

### Step 4: Documentation
- [ ] Update README.md: Add Aura to supported themes list
- [ ] Update CONFIGURATION.md: Add Aura-specific configuration notes (if any)
- [ ] Update UPGRADE_GUIDE.md: Note that Aura support is planned for post-6.0.0 releases
- [ ] Add theme switching example (if relevant)

---

## Browser Compatibility

**`color-mix()` support:**
- Safari: 16.4+ (March 2023)
- Chrome/Edge: 111+ (March 2023)
- Firefox: 113+ (May 2023)

**Fallback strategy:**
If supporting older browsers is needed, use:
```css
background-color: var(--vaadin-primary-color-10pct,
  color-mix(in srgb, var(--vaadin-primary-color) 10%, transparent));
```

---

## Known Limitations

1. **Style Proxy Timing:** Proxy injection happens once at component connection. If theme stylesheets are loaded dynamically AFTER ERTE, those styles won't be proxied. (Very rare in production Vaadin apps)

2. **CORS:** Stylesheets from external domains (CDN) cannot be read. These will be skipped silently. (Uncommon in Vaadin setups)

3. **No SSR/Pre-render Support:** Style proxy runs in browser only. Server-side rendering won't include proxied styles. (Not relevant for Vaadin Flow apps)

---

## Success Criteria

- [ ] All 14 Lumo tokens replaced with generic Vaadin tokens
- [ ] Aura style proxy implemented and tested
- [ ] ERTE works correctly under Aura theme:
  - All features visible and functional
  - Proper colors in light and dark mode
  - WCAG AA contrast maintained
- [ ] No regressions under Lumo theme
- [ ] Documentation updated
- [ ] Manual testing complete
- [ ] Progress file updated to COMPLETE
- [ ] Commit created: "Add Aura theme support with runtime style proxy (Phase 5.1)"

---

## Estimated Effort

**Total: 4-6 hours**
- Token replacement: 1-2 hours
- Aura style proxy: 2-3 hours (implementation + edge cases)
- Testing (manual, both themes, light/dark): 1-2 hours
- Documentation: 30 minutes

---

## Dependencies

- No blocking dependencies
- Should be done BEFORE 3.5 documentation review (to capture Aura in docs)
- Independent of Phase 4 (Tables) — can be done before or after
