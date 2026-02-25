# Phase 6 Spike: Aura Style Proxy Prototype

**Status:** COMPLETE
**Type:** Technical Spike / Proof of Concept
**Priority:** DEFERRED — Post-6.0.0 feature
**Created:** 2026-02-21
**Updated:** 2026-02-25 (renumbered from Phase 5 → Phase 6)

> **Note:** ERTE 6.0.0 focuses on Lumo theme support only. This spike validates the technical approach for Aura theme support, planned for a post-6.0.0 release (Phase 6.1).

---

## Objective

Prototype the Aura Style Proxy mechanism to verify technical feasibility before full implementation in Phase 6.1. Validate that:
1. Runtime stylesheet scanning works reliably
2. Rule cloning and selector replacement functions correctly
3. Performance impact is acceptable
4. Edge cases (CORS, timing, duplicates) are handled properly
5. The approach works with both Lumo and Aura themes

---

## Scope

**Build a minimal working prototype** that demonstrates:
- Scanning document stylesheets for RTE rules
- Cloning rules with ERTE tag name
- Injecting cloned rules into document
- Testing with actual Aura theme in demo app

**Out of scope for spike:**
- Token replacement (`--lumo-*` → `--vaadin-*`) — straightforward, no spike needed
- Production-ready error handling
- Complete documentation

---

## Implementation Steps

### Step 1: Create Spike Test View

Create a dedicated test view in the demo app for spike testing.

**File:** `enhanced-rich-text-editor-demo/src/main/java/com/vaadin/componentfactory/ErteAuraSpikeView.java`

```java
package com.vaadin.componentfactory;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

/**
 * Spike test view for Aura Style Proxy prototype.
 * Tests runtime stylesheet scanning and rule injection.
 */
@Route("erte-spike/aura-proxy")
public class ErteAuraSpikeView extends VerticalLayout {
    public ErteAuraSpikeView() {
        add(new H2("Aura Style Proxy Spike"));
        add(new Paragraph("Testing runtime stylesheet scanning and rule cloning."));

        // Stock RTE (receives Aura styles normally)
        var stockRte = new com.vaadin.flow.component.richtexteditor.RichTextEditor();
        stockRte.setId("stock-rte");
        stockRte.setWidthFull();

        // ERTE (should receive cloned Aura styles via proxy)
        var erteRte = new EnhancedRichTextEditor();
        erteRte.setId("erte-rte");
        erteRte.setWidthFull();

        add(new H2("Stock RTE (vaadin-rich-text-editor)"), stockRte);
        add(new H2("ERTE (vcf-enhanced-rich-text-editor)"), erteRte);

        // Debug output div
        Div debugOutput = new Div();
        debugOutput.setId("debug-output");
        debugOutput.getStyle()
            .set("margin-top", "2em")
            .set("padding", "1em")
            .set("border", "1px solid var(--lumo-contrast-20pct)")
            .set("background", "var(--lumo-contrast-5pct)");
        add(new H2("Debug Output"), debugOutput);
    }
}
```

### Step 2: Add Spike Proxy Method to ERTE

Add prototype `_injectAuraStyleProxy()` to ERTE's JS file.

**File:** `enhanced-rich-text-editor-v25/src/main/resources/META-INF/resources/frontend/vcf-enhanced-rich-text-editor.js`

**Location:** After `ready()` method, before `_applyErteI18n()`

```javascript
/**
 * SPIKE: Aura Style Proxy prototype.
 * Scans document stylesheets and clones RTE rules for ERTE.
 * @private
 */
_injectAuraStyleProxySpike() {
  console.group('ERTE Spike: Aura Style Proxy');

  // Guard: avoid duplicate injection
  if (document.querySelector('style[data-vcf-erte-spike-proxy]')) {
    console.log('Proxy already injected, skipping.');
    console.groupEnd();
    return;
  }

  const startTime = performance.now();
  const stats = {
    totalSheets: 0,
    scannedSheets: 0,
    corsSkipped: 0,
    totalRules: 0,
    matchedRules: 0,
  };

  const rteRules = [];

  // Scan all stylesheets
  for (const sheet of document.styleSheets) {
    stats.totalSheets++;

    try {
      stats.scannedSheets++;
      const rules = Array.from(sheet.cssRules || []);
      stats.totalRules += rules.length;

      for (const rule of rules) {
        if (rule.cssText && rule.cssText.includes('vaadin-rich-text-editor')) {
          rteRules.push({
            original: rule.cssText,
            sheet: sheet.href || '(inline)',
          });
          stats.matchedRules++;
        }
      }
    } catch (e) {
      // CORS-protected sheet
      stats.corsSkipped++;
      console.warn('Skipped stylesheet (CORS):', sheet.href, e.message);
    }
  }

  if (rteRules.length === 0) {
    console.log('No RTE-specific rules found.');
    console.groupEnd();
    return;
  }

  // Clone rules with ERTE tag
  const proxyStyles = rteRules.map(({ original }) =>
    original.replace(/vaadin-rich-text-editor/g, 'vcf-enhanced-rich-text-editor')
  );

  // Inject into document
  const styleEl = document.createElement('style');
  styleEl.setAttribute('data-vcf-erte-spike-proxy', '');
  styleEl.textContent = [
    '/* ERTE Aura Style Proxy (SPIKE) */',
    `/* Generated: ${new Date().toISOString()} */`,
    `/* Cloned ${stats.matchedRules} rules */`,
    '',
    ...proxyStyles,
  ].join('\n');
  document.head.appendChild(styleEl);

  const endTime = performance.now();
  const duration = (endTime - startTime).toFixed(2);

  // Log stats
  console.table(stats);
  console.log(`Duration: ${duration}ms`);
  console.log('Sample cloned rules:', proxyStyles.slice(0, 3));
  console.log('Full injected stylesheet:', styleEl.textContent.substring(0, 500) + '...');
  console.groupEnd();

  // Write debug output to page
  this._writeSpikeDebugOutput(stats, duration, rteRules.length);
}

/**
 * Writes spike debug output to the page (if debug div exists).
 * @private
 */
_writeSpikeDebugOutput(stats, duration, clonedCount) {
  const debugDiv = document.getElementById('debug-output');
  if (!debugDiv) return;

  debugDiv.innerHTML = `
    <h3>Spike Results</h3>
    <ul>
      <li><strong>Total Stylesheets:</strong> ${stats.totalSheets}</li>
      <li><strong>Scanned:</strong> ${stats.scannedSheets}</li>
      <li><strong>CORS Skipped:</strong> ${stats.corsSkipped}</li>
      <li><strong>Total Rules:</strong> ${stats.totalRules}</li>
      <li><strong>Matched RTE Rules:</strong> ${stats.matchedRules}</li>
      <li><strong>Cloned Rules:</strong> ${clonedCount}</li>
      <li><strong>Duration:</strong> ${duration}ms</li>
    </ul>
    <p><em>Check browser console for full details.</em></p>
  `;
}
```

**Call from `ready()`:**
```javascript
ready() {
  super.ready();

  // ... existing ready() code ...

  // SPIKE: Test Aura Style Proxy
  this._injectAuraStyleProxySpike();
}
```

### Step 3: Configure Demo with Aura Theme

**Option A: Application-wide Aura** (simplest for spike)

**File:** `enhanced-rich-text-editor-demo/src/main/java/com/vaadin/componentfactory/Application.java`

```java
@Theme(value = "aura")  // Change from "lumo" to "aura"
public class Application implements AppShellConfigurator {
    // ...
}
```

**Option B: Per-route Aura** (if keeping Lumo for other views)

Use `@Theme` annotation on `ErteAuraSpikeView`:
```java
@Route("erte-spike/aura-proxy")
@Theme("aura")
public class ErteAuraSpikeView extends VerticalLayout {
    // ...
}
```

### Step 4: Build and Test

```bash
# Build ERTE module
bash v25-build.sh

# Start demo
bash v25-server-start.sh

# Navigate to: http://localhost:8080/erte-spike/aura-proxy
```

---

## Validation Criteria

### 1. **Functional Test**

Open spike view in browser and verify:
- ✅ Stock RTE appears with Aura styling (Aura toolbar colors/spacing)
- ✅ ERTE appears with **matching** Aura styling (via proxy)
- ✅ No JavaScript errors in console
- ✅ Console log shows proxy stats (rules found, duration)
- ✅ Debug output on page shows reasonable stats

**Expected stats:**
- Total Stylesheets: 5-10 (Vaadin, Lumo/Aura, app styles)
- Scanned: 5-10 (same, unless CORS)
- CORS Skipped: 0 (in local dev)
- Total Rules: 500-2000 (depends on theme)
- Matched RTE Rules: 10-30 (Aura's RTE-specific rules)
- Cloned Rules: Same as matched
- Duration: <10ms

### 2. **Visual Comparison**

Compare Stock RTE and ERTE side-by-side:
- Toolbar button colors → Should match
- Toolbar spacing/padding → Should match
- Icon styles → Should match
- Part borders/shadows → Should match

### 3. **Performance Test**

- ✅ Duration < 20ms (acceptable cold start)
- ✅ No visible delay when loading page
- ✅ Single injection only (guard works)

### 4. **Edge Case Tests**

**Test A: Reload page**
- Proxy should inject again (previous injection is lost)
- Stats should be consistent

**Test B: Multiple ERTE instances**
- Add second ERTE to spike view
- Proxy should inject only once (guard prevents duplicates)

**Test C: Switch back to Lumo**
- Change `@Theme` to "lumo"
- Rebuild, restart
- ERTE should still work (Lumo via lumoInjector, proxy finds no Aura rules)

---

## Success Criteria

- ✅ Prototype code implemented
- ✅ Spike view functional
- ✅ Visual comparison: ERTE matches Stock RTE under Aura
- ✅ Performance: Duration < 20ms
- ✅ Edge cases tested (reload, multiple instances, Lumo)
- ✅ No CORS errors in local dev
- ✅ Approach validated as feasible

---

## Risks & Mitigations

| Risk | Likelihood | Mitigation |
|------|-----------|------------|
| Rules not found (timing) | Low | Test with `requestAnimationFrame()` delay if needed |
| CORS issues | Low | Test with external CDN stylesheet if possible |
| Performance too slow | Very Low | 5-10ms expected, acceptable up to 50ms |
| Cloned rules don't apply | Low | Verify specificity, use `!important` if needed (spike only) |
| Multiple injections | Low | Guard with data attribute (already in code) |

---

## Next Steps After Spike

**If successful:**
1. Clean up prototype code (remove debug logging, extract to dedicated method)
2. Proceed with Phase 6.1 implementation
3. Add production error handling
4. Add comprehensive documentation

**If issues found:**
1. Document the issue in spike report
2. Adjust approach (e.g., mutation observer for timing, specificity boost for cloned rules)
3. Re-run spike with adjusted implementation
4. Update Phase 6.1 plan based on findings

---

## Spike Report

**Date:** 2026-02-21
**Executed by:** agents-manager (fullstack-developer + ui-explorer)

### Results

- **Visual Test:** PARTIAL PASS -- ERTE matches Stock RTE under Aura for layout, spacing, icons, borders, and shadows. One difference found: toolbar button text color opacity (see Issues below).
- **Performance:** 1.20 ms average (5 runs: 1.1, 1.2, 1.2, 1.2, 1.3 ms). Target: <20ms. **PASS**
- **Stats (Aura theme):**
  - Total Stylesheets: 11
  - Scanned: 11
  - CORS Skipped: 0
  - Total Rules: 268
  - Matched RTE Rules: 11
  - Cloned Rules: 11
  - Duration: 1.1-1.5ms
- **Stats (Lumo theme, Edge Case C):**
  - Matched RTE Rules: 2 (Lumo injector rules, harmless)
  - Duration: 6.8ms (slightly higher due to larger rule set scanning)
- **Edge Cases:**
  - **A (Reload):** PASS -- proxy re-injects correctly, stats consistent
  - **B (Duplicate guard):** PASS -- exactly 1 proxy style element after load
  - **C (Switch to Lumo):** PASS -- ERTE works normally, proxy finds 2 Lumo rules (harmless), no errors
- **Regression Test:** 214 ERTE tests pass (10 skipped/fixme), 0 failures. Spike code does not break anything.

### Detailed Visual Comparison (Aura Theme)

| Property | Stock RTE | ERTE | Match |
|----------|-----------|------|-------|
| Host box-shadow | `oklch(0.19 0.0005 248 / 0.2) 0px 1px 4px -2px` | Same | YES |
| Host background | `rgba(0, 0, 0, 0)` | Same | YES |
| Host border-radius | `0px` | Same | YES |
| Toolbar background-color | `rgba(0, 0, 0, 0)` (transparent) | Same | YES |
| Toolbar padding | Matching | Same | YES |
| Toolbar gap | Matching | Same | YES |
| Toolbar border-bottom | Matching | Same | YES |
| Toolbar contain | `paint` | Same | YES |
| Button background-color | Matching | Same | YES |
| **Button text color** | **`oklch(0.15 0.0038 248 / 0.33)`** | **`oklch(0.15 0.0038 248 / 0.65)`** | **NO** |
| Button border-radius | Matching | Same | YES |
| Button padding | Matching | Same | YES |
| Button transition | Matching | Same | YES |
| Group gap | Matching | Same | YES |
| Group align-items | `center` | Same | YES |
| Icon visual size | `90%` | Same | YES |
| Aura surface level | `4` | Same | YES |

### Issues Encountered

#### Issue 1: `@Theme("aura")` Does Not Work

**Problem:** The spike plan suggested `@Theme("aura")` annotation, but Vaadin 25 does not support named themes via `@Theme` (only custom theme directories). The error: "could not find the theme directory...for 'aura'".

**Solution:** Use `@StyleSheet(Aura.STYLESHEET)` from `com.vaadin.flow.theme.aura.Aura` instead of `@StyleSheet(Lumo.STYLESHEET)`. The `vaadin-aura-theme` JAR is already a transitive dependency of `com.vaadin:vaadin`.

**Impact on Phase 6.1:** The Aura theme is activated by replacing the `@StyleSheet` annotation, not `@Theme`. This is the user's responsibility, not ERTE's. ERTE only needs to proxy the resulting CSS rules. No ERTE code change needed for theme activation.

#### Issue 2: CSS Custom Property Name Collision (CRITICAL)

**Problem:** The naive `replace(/vaadin-rich-text-editor/g, 'vcf-enhanced-rich-text-editor')` replaces ALL occurrences of the string, including:
1. **Selectors** (what we want): `vaadin-rich-text-editor::part(toolbar)` -> `vcf-enhanced-rich-text-editor::part(toolbar)`
2. **CSS custom property names** (what we do NOT want): `--vaadin-rich-text-editor-toolbar-button-text-color` -> `--vcf-enhanced-rich-text-editor-toolbar-button-text-color`

**Root cause of button color difference:**
- Aura CSS sets: `vaadin-rich-text-editor:not(:focus-within) { --vaadin-rich-text-editor-toolbar-button-text-color: var(--vaadin-text-color-disabled); }`
- After naive replace, the cloned rule uses: `--vcf-enhanced-rich-text-editor-toolbar-button-text-color: var(--vaadin-text-color-disabled);`
- But the shadow DOM internal CSS references: `color: var(--vaadin-rich-text-editor-toolbar-button-text-color, ...)`
- The renamed `--vcf-*` variable is NOT what the shadow DOM reads, so the disabled state is never applied.

**Fix for Phase 6.1:** Use a smarter replacement strategy:
```javascript
// Only replace tag name in selectors, NOT in CSS custom property names
// Option A: Negative lookbehind for --
original.replace(/(?<!--)vaadin-rich-text-editor/g, 'vcf-enhanced-rich-text-editor')

// Option B: Two-pass approach
// 1. Replace tag names in selectors (at start of rule or after comma/space)
// 2. Keep CSS custom property names (--vaadin-rich-text-editor-*) unchanged
```

**Impact:** This is the most significant finding. The replacement regex must be refined. The fix is straightforward (negative lookbehind regex) and will be the primary change in Phase 6.1.

#### Issue 3: Lumo Proxy Injects Unnecessary Rules

**Problem:** Under Lumo theme, the proxy finds 2 rules (Lumo injector's `@media lumo_components_rich-text-editor { :host { ... } }`) and clones them. These are `:host`-scoped shadow DOM rules that only affect the component's shadow root, so the cloned document-level rules are harmless but unnecessary.

**Fix for Phase 6.1:** Consider detecting the active theme (`--vaadin-aura-theme: 1` CSS property) and only running the proxy when Aura is detected. This also improves performance (no unnecessary scanning under Lumo).

### Recommendations

**PROCEED with Phase 5.1 as planned.** The approach is validated as feasible with one required adjustment:

1. **MUST FIX:** Use selective replacement regex (negative lookbehind `(?<!--)`) to avoid renaming CSS custom property names. This is the only blocking issue.

2. **SHOULD ADD:** Theme detection check -- only run proxy when `--vaadin-aura-theme: 1` is set on `:root`. Skip under Lumo (ERTE already handles Lumo via `lumoInjector`).

3. **NICE TO HAVE:** Consider `MutationObserver` or `requestAnimationFrame` for production to handle late-loading stylesheets, though the spike showed all styles are available at `ready()` time.

4. **Performance:** 1.2ms average is excellent. Even with the regex fix, expect <5ms. Well within the 20ms target.

5. **Production cleanup needed:**
   - Remove console.group/console.table debug logging
   - Remove `_writeSpikeDebugOutput()` and debug-output div
   - Extract to dedicated file or keep as method on the class
   - Add JSDoc documentation
   - Handle the Lumo-only edge case (skip proxy)

---

### Fixes Applied (2026-02-21)

Both critical findings from the spike have been applied and committed to the codebase:

1. **Regex Fix:** Changed to `replace(/(?<!--)vaadin-rich-text-editor/g, ...)` with negative lookbehind to prevent CSS custom property name collision. This fixes Issue 2 (toolbar button color opacity difference).

2. **Aura Stylesheet Fix:** `ErteAuraSpikeView.java` uses `@StyleSheet(Aura.STYLESHEET)` (correct for Vaadin 25, not `@Theme("aura")` which doesn't exist).

**Regression tests:** 214 pass, 10 fixme, 0 fail - all baseline results maintained.

**Spike code remains in codebase** for potential future use (test view at `/erte-spike/aura-proxy` and methods `_injectAuraStyleProxySpike()` + `_writeSpikeDebugOutput()` in ERTE JS).

**Performance validated:** 1.2ms average (spike measurements), well within <20ms target.

**Next step:** Proceed with Phase 6.1 full implementation based on validated approach. Theme detection (Aura vs Lumo) and production cleanup recommended but not blocking.

---

## Estimated Effort

**Total: 2-3 hours**
- Test view setup: 30 minutes
- Prototype implementation: 1 hour
- Testing (functional, performance, edge cases): 1 hour
- Documentation: 30 minutes

---

## Dependencies

- Vaadin 25 demo app with Aura theme configured
- Enhanced Rich Text Editor V25 module built
- Browser with dev tools (Chrome/Firefox/Safari)
