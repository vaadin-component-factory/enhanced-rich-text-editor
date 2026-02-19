# Phase 2: ERTE Shell — Progress

## Status: PLANNING

## Goal

Stock RTE 2 wrapped in ERTE's own web component tag. **Zero visual difference** to plain RichTextEditor after Phase 2. The shell proves the extension mechanism works; features are added in Phase 3+.

## Scope

### In scope
- JS subclass extending RTE 2's web component (`vcf-enhanced-rich-text-editor`)
- `render() { return super.render(); }` — override mechanism in place, passes through to stock RTE
- `static get styles()` — Polymer→Lit migration of style mechanism (empty/passthrough for now)
- `customElements.define('vcf-enhanced-rich-text-editor', ...)`
- Java: `@Tag("vcf-enhanced-rich-text-editor")` + `@JsModule` on `EnhancedRichTextEditor`
- Java: `RteExtensionBase` — lift only the package-private members needed for the shell
- Lifecycle verification: `_editor` (Quill instance) available, content settable via Java API
- Demo view using `EnhancedRichTextEditor` instead of stock `RichTextEditor`

### NOT in scope (Phase 3+)
- Sanitizer override (Phase 3, when blots need whitelisted classes)
- Custom toolbar buttons (Phase 3, per feature)
- Slots for custom components (Phase 3, Custom Slots feature)
- Rulers, tabstops, placeholders, readonly (Phase 3, individual features)
- Any visual or behavioral difference from stock RTE

## Acceptance Criteria

1. `EnhancedRichTextEditor` renders identically to `RichTextEditor`
2. Tag in DOM is `vcf-enhanced-rich-text-editor` (not `vaadin-rich-text-editor`)
3. All standard RTE 2 functionality works (typing, formatting, undo/redo)
4. Content settable via Java API (`asHtml().setValue()`, `asDelta()`)
5. Dev bundle builds without errors
6. `mvn clean install -DskipTests` succeeds

## Open Questions

| # | Question | Status |
|---|----------|--------|
| 1 | Which package-private members need lifting in `RteExtensionBase`? | Open — inspect RTE 2 source during implementation |
| 2 | ~~Does `super.render()` work in Lit subclass?~~ | **Confirmed: YES** — `super.render()` works |
| 3 | ~~Is `@NpmPackage` inherited from parent?~~ | **Confirmed: YES** — inherited, no own `@NpmPackage` needed |

## Completed Steps

(none yet)
