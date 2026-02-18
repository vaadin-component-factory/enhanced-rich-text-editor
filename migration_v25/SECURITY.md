# ERTE Migration — Security Requirements

These findings are from a security audit of ERTE 1. They must be **fixed during migration**, not reproduced in ERTE 2.

## Critical: PlaceholderBlot XSS

**Location:** `vcf-enhanced-rich-text-editor-blots.js:197`
**Issue:** `node.innerHTML = text` where `text` comes from placeholder data stored in deltas. This is user-controllable and a stored XSS vector.
**Fix:** Use `textContent` instead of `innerHTML`.

## Warning: CSS Injection in Table Colors

**Location:** `EnhancedRichTextEditorTables.java:390-413`
**Issue:** `tableHoverColor`, `cellHoverColor` etc. are concatenated directly into CSS strings without validation. Values like `red; } * { display:none` break out of CSS declarations.
**Fix:** Validate CSS color values against an allowlist or use a CSS sanitizer.

## Warning: Unrestricted `style` Attribute

**Location:** HTML sanitizer configuration
**Issue:** Sanitizer allows `.addAttributes(":all", "style")` on all elements without sanitizing style values. Permits `url()`, `expression()`, `behavior()` in style attributes.
**Fix:** Add a style value sanitizer or restrict allowed style properties.

## Warning: `data:` Protocol on Images

**Location:** HTML sanitizer configuration
**Issue:** `data:` protocol allowed on images without MIME type restriction. Permits `data:text/html` payloads.
**Fix:** Restrict to `data:image/*` MIME types only.

## Warning: TemplateParser CSS Values Not Validated

**Location:** `TemplateParser.java:336-356`
**Issue:** `isValidPropertyValue()` is commented out. CSS property values are not validated.
**Fix:** Re-enable or replace with proper CSS value validation.

## Rules for ERTE 2 Implementation

1. **Never use `innerHTML` with dynamic content.** All DOM manipulation must use `createElement`/`appendChild` or Lit's `html` tagged templates.
2. **Register custom blots synchronously before any `setContents()` call.** Unregistered blots render as raw HTML spans, bypassing format restrictions.
3. **All `executeJs()` calls must not concatenate user input into JS strings.** Use parameter binding (`$0`, `$1`, etc.).
4. **Sanitizer allowlist must be updated** to match Quill 2's HTML output (e.g., `<ol>` with `data-list` instead of `<ul>` for bullet lists).
5. **Security regression tests:** After migration, test the sanitizer against an XSS payload corpus. At minimum, verify that stored deltas cannot inject scripts via placeholder data, table colors, or style attributes.
