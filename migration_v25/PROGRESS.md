# V25 Migration -- Fortschritt

**Letzte Aktualisierung:** 2026-02-17
**Branch:** v25

---

## Gesamtstatus

```
Spike & Architektur    [##########] 100%  -- DONE
Grundstruktur (Java)   [##########] 100%  -- DONE
Grundstruktur (JS)     [##########] 100%  -- DONE (alle 29 Slots vorhanden)
Blots (Custom)         [########--]  80%  -- A1-A3 gefixt, visueller Test offen
Blots (Tables)         [######----]  60%  -- A4 Bug-Fixes implementiert, Test offen
Styles (CSS)           [########--]  80%  -- Table+Readonly Styles hinzugefuegt
Ruler-System           [########--]  80%  -- HTML + Handler portiert, visueller Test offen
Demo (14 Cards)        [##########] 100%  -- DONE (Java-seitig komplett)
Feature-Paritaet       [######----]  60%  -- Fixes implementiert, Verifikation ausstehend
```

---

## Gerade implementierte Fixes (noch nicht visuell verifiziert)

### A1. Delta-Pipeline -- FIX IMPLEMENTIERT
- **Fix:** `ErteDeltaValue` Wrapper ueberschreibt `asDelta()`. Setzt Delta via `value` Property (JS laedt es direkt in Quill). Liest HTML zurueck mit `isDeltaSync` Flag, damit `setPresentationValue` den Client-Push ueberspringt.
- **Dateien:** `EnhancedRichTextEditor.java` (Zeile 164-235, 311)
- **Build:** OK

### A2. ReadOnlyBlot -- FIX IMPLEMENTIERT
- **Fix:** ReadOnlyBlot von `Inline` zu `Embed` umgeschrieben. Text in `data-value` Attribut gespeichert. Sanitizer um `data-value` erweitert.
- **Dateien:** `vcf-enhanced-rich-text-editor-blots.js` ReadOnlyBlot, `EnhancedRichTextEditor.java` sanitizeErte()
- **Build:** OK

### A3. PlaceholderBlot -- FIX IMPLEMENTIERT
- **Fix:** `loadValue()` defensiv: prueft `dataset.placeholder` auf undefined/parse-error. Sanitizer um `data-placeholder` erweitert.
- **Dateien:** `vcf-enhanced-rich-text-editor-blots.js` loadValue(), `EnhancedRichTextEditor.java` sanitizeErte()
- **Build:** OK

### A4. Table-Blots -- FIX IMPLEMENTIERT
- **Fix 1:** `TableRow.createDefaultChild()` Zeile 79: `this.domNode.parent` → `this.domNode.parentNode` (DOM API)
- **Fix 2:** `Table.optimize()`: Guard-Check `quill.table && quill.table.tables &&` vor Zugriff
- **Dateien:** `TableRowBlot.js`, `TableBlot.js`
- **Build:** OK

### B1. Toolbar-Slots -- WAR BEREITS ERLEDIGT
- **Status:** KEIN FIX NOETIG -- V25 hat bereits alle 29 Slots (25 toolbar + 4 dialog/popup)
- **PROGRESS.md war falsch:** "Nur 3 statt 24" stimmte nicht.

### B2. Ruler-System -- IMPLEMENTIERT
- **Fix:** Ruler-HTML in `render()` eingefuegt (horizontal + vertikal). Ruler-Hintergrund-Bilder als Base64-Konstanten. `_addTabStop()`, `_addTabStopIcon()`, `_updateRulerIcons()` portiert. `noRulers` Toggle steuert `display`.
- **Dateien:** `vcf-enhanced-rich-text-editor.js` render(), Ruler-Methoden
- **Build:** OK

### C3. Table-Styles -- IMPLEMENTIERT
- **Fix:** Base-Table-CSS (border-collapse, td border, cell-selected, merge_id hidden, hideBorder) in Content-Styles eingefuegt.
- **Dateien:** `vcf-enhanced-rich-text-editor-content-styles.js`
- **Build:** OK

### Readonly-Styles -- IMPLEMENTIERT
- **Fix:** CSS fuer `.ql-readonly` (Hintergrund, Padding, Border-Radius) in Content-Styles eingefuegt.
- **Dateien:** `vcf-enhanced-rich-text-editor-content-styles.js`
- **Build:** OK

---

## Erledigt (fruehere Sessions)

### Spike (Phase 1-5)
- [x] Quill 2.0.3 bestaetigt, `Quill.register()` global funktioniert
- [x] JS-Klasse erweitert RTE 2 Lit-Klasse via `render()` + `ready()`
- [x] Java erweitert `RichTextEditor` im gleichen Package
- [x] `@Tag("vcf-enhanced-rich-text-editor")` Override funktioniert
- [x] Parent-Observer (_onReadonlyChanged etc.) bleiben intakt
- [x] Keyboard-Bindings mit String-Keys (Quill 2)
- [x] Lumo via `@StyleSheet` (nicht auto-loaded in V25)
- [x] `::part()` Selektoren fuer externes Theming
- [x] HTML-Sanitizer erweitert (ql-tab, ql-placeholder, ql-readonly, ql-soft-break)
- [x] Production-Build verifiziert
- [x] Table-Blots Parchment 3 Migration (2x2, 3x3 manuell eingefuegt -- OK)
- [x] 5 Parchment 3 Breaking Changes identifiziert und gefixt

### Java API (V25 EnhancedRichTextEditor.java)
- [x] Konstruktoren (leer, initialValue, listener, beides)
- [x] Theme Variants (`addThemeVariants`, `removeThemeVariants`)
- [x] Value Handling (`getValue`, `setValue`, `setPresentationValue`, `sanitizeErte`)
- [x] `asDelta()` Override mit `ErteDeltaValue` Wrapper (A1 Fix)
- [x] `getHtmlValue()` deprecated Wrapper
- [x] `getHtmlValueString()` (raw, unsanitized)
- [x] `getTextLength()`
- [x] TabStops (`setTabStops`, `getTabStops`)
- [x] Rulers (`setNoRulers`, `isNoRulers`)
- [x] Whitespace (`setShowWhitespace`, `isShowWhitespace`)
- [x] I18n (`setEnhancedI18n`, `getEnhancedI18n`, `EnhancedRichTextEditorI18n` mit ERTE-Keys)
- [x] Toolbar Visibility (`setToolbarButtonsVisibility`, `getToolbarButtonsVisibility`)
- [x] Placeholders (`setPlaceholders`, `getPlaceholders`, `setPlaceholderAltAppearance`, `setPlaceholderAltAppearancePattern`)
- [x] Text Manipulation (`addText(text)`, `addText(text, position)`)
- [x] Toolbar Components (`addToolbarComponents`, `addCustomToolbarComponents`, `removeToolbarComponent`, `getToolbarComponent`)
- [x] Keyboard Shortcuts (`addStandardToolbarButtonShortcut`, `addToolbarFocusShortcut`)
- [x] Icon Replacement (`replaceStandardToolbarButtonIcon`)
- [x] 8 Placeholder Event Listener
- [x] `ChangeEvent`
- [x] `ToolbarButton` Enum (24 Buttons)

### JS Grundstruktur (vcf-enhanced-rich-text-editor.js)
- [x] Lit `render()` Override mit ERTE Toolbar-Buttons + 29 Slots + Ruler
- [x] `ready()` Hook: Quill-Zugriff, Keyboard-Bindings, Event-Listener
- [x] `updated()` fuer Property-Sync (tabStops, placeholders, toolbarButtons, etc.)
- [x] Toolbar-Button-Visibility Toggle
- [x] Placeholder-Button + Appearance-Toggle in Toolbar
- [x] Whitespace-Button in Toolbar
- [x] Standard-Button Keyboard Shortcuts (`addStandardButtonBinding`)
- [x] Toolbar Focus Shortcut (`addToolbarFocusBinding`)
- [x] Icon Replacement (`replaceStandardButtonIcon`)
- [x] Soft-Break Handler (Shift+Enter)
- [x] Tab-Key Handler
- [x] Clipboard-Matchers (TAB, PRE-TAB, TABS-CONT, LINE-PART)
- [x] HTML-Value Override (`__updateHtmlValue` mit ql-class Preservation)
- [x] Ruler-System (horizontal + vertikal, Click-Handler, TabStop-Icons)

### JS Blots (vcf-enhanced-rich-text-editor-blots.js)
- [x] ReadOnlyBlot registriert (Embed, `ql-readonly`, data-value) -- A2 Fix
- [x] TabBlot registriert (Embed, `ql-tab`, Click-Handler)
- [x] SoftBreakBlot registriert (Embed, `ql-soft-break`)
- [x] Nbsp registriert (Inline)
- [x] PlaceholderBlot registriert (Embed, `ql-placeholder`, defensives loadValue) -- A3 Fix

### Demo (V25, 14-Card View)
- [x] `EnhancedRichTextEditorView.java` mit allen 14 Cards (migriert von V24)
- [x] `EnhancedRichTextEditorDemo25View.java` auf Route `/test` verschoben
- [x] Build erfolgreich (`v25-build.sh`)
- [x] Alle 14 Cards rendern mit korrekten Headings und Toolbars

### Tables Extension (V25)
- [x] Java: `EnhancedRichTextEditorTables.enable()`, `setTemplates()`, `getStyleTemplatesDialog()`
- [x] Java: `TablesI18n` + `TemplatesI18n`
- [x] JS: Table-Blots registriert + A4 Bug-Fixes (TableBlot, TableRowBlot)
- [x] JS: Toolbar-Buttons (Insert, Modify, Templates)

---

## Noch offen

### B3. Custom-Button Dynamic Update APIs -- Bewusst entfernt (Breaking Change)
- **Status:** DOKUMENTIEREN
- **V24:** 8 Methoden (`setCustomButtonLabel/Tooltip/Icon/ClickListener/KeyboardShortcut`)
- **V25:** Entfernt (Clean Break). Kunden nutzen Vaadin Component API direkt.
- **Fix:** Als Breaking Change dokumentieren

### B4. Placeholder-Dialog -- JS-Dialog entfernt
- **Status:** BEWUSSTE DESIGN-ENTSCHEIDUNG
- **V24:** Inline-Dialog im JS (~300 Zeilen Template + 13 Methoden)
- **V25:** Event-basiert. Java Events existieren (8 Listener). Kunden bauen eigenen Dialog.
- **Demo:** Nutzt `addPlaceholderButtonClickedListener()` -- funktioniert, aber kein Auswahl-UI
- **Fix:** Optional: Java-Dialog-Helfer oder als Event-Only dokumentieren

### C1+C2. Content-/Toolbar-Styles -- Visuell verifizieren
- **Status:** MUSS VISUELL GEPRUEFT WERDEN
- **Annahme:** RTE 2 liefert Base-Styles (Listen, Blockquotes, Code, Links, Headings, Toolbar-Buttons) nativ
- **Fix:** Fehlende Styles nach visuellem Abgleich nachportieren

### Visuelle Verifikation -- AUSSTEHEND
- **Status:** Alle Fixes implementiert und Build OK, aber KEIN visueller Test durchgefuehrt
- **Naechster Schritt:** Server starten, alle 14 Cards visuell pruefen, Konsole auf Fehler pruefen

---

## Priorisierte naechste Schritte

| Prio | Item | Status |
|:----:|------|:------:|
| 1 | Visuelle Verifikation aller 14 Cards | AUSSTEHEND |
| 2 | V24 vs V25 Feature-Abgleich durch Agents | AUSSTEHEND |
| 3 | C1+C2: Fehlende Styles nach visuellem Test | AUSSTEHEND |
| 4 | B3: Custom-Button APIs dokumentieren | OFFEN |
| 5 | B4: Placeholder-Dialog Entscheidung | OFFEN |

---

## Referenz-Dokumente

| Dokument | Inhalt |
|----------|--------|
| `migration_v25/OPEN_MIGRATION_ITEMS.md` | Detaillierte technische Analyse aller Gaps |
| `migration_v25/FEATURE_PARITY_REPORT.md` | Frueherer Parity-Test (Test-Harness, NICHT echte Deltas!) |
| `migration_v25/implementation_notes.md` | Technische Analyse, Spike-Checklist |
| `migration_v25/feature_comparison.md` | 20-Feature Matrix (V24 vs RTE 2) |
| `migration_v25/spike/SPIKE_RESULTS.md` | Spike-Ergebnisse (Phase 1-5) |
| `migration_v25/agents_analysis_ANSWERED.md` | Architektur-Entscheidungen |

## Modul-Uebersicht

| Modul | Zweck | Port |
|-------|-------|:----:|
| `enhanced-rich-text-editor-25/` | Core-Komponente (Java + JS) | -- |
| `enhanced-rich-text-editor-tables-25/` | Tables-Addon | -- |
| `enhanced-rich-text-editor-demo-25/` | Demo-App (14 Cards + Test-View) | 8082 |
| `migration_v25/spike/` | Spike-Projekt (archiviert) | 8081 |

## Build & Server

```bash
# Build
bash v25-build.sh

# Server starten/stoppen/status
bash v25-server-start.sh          # Port 8082
bash v25-server-stop.sh
bash enhanced-rich-text-editor-demo-25/server-status.sh
```
