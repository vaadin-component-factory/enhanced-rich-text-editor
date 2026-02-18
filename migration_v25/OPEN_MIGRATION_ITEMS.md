# V25 Migration -- Offene Punkte (vollstaendige Liste)

**Datum:** 2026-02-17
**Branch:** v25
**Status:** Spike DONE, Phase 1-3 Implementierung DONE, Feature-Migration UNVOLLSTAENDIG

---

## Uebersicht

| Kategorie | Offen | Erledigt |
|-----------|:-----:|:--------:|
| CRITICAL (Feature kaputt) | 4 | - |
| HIGH (Feature unvollstaendig) | 4 | - |
| MEDIUM (Differenz) | 3 | - |
| LOW/NONE (Kosmetik/Nativ) | - | 10+ |

---

## A. CRITICAL -- Feature existiert im Code, funktioniert aber NICHT

### A1. Tab-Blot-Content via Delta laedt nicht
- **Symptom:** Card 2 (Tab-stops) -- Editor-Inhalt leer, Konsole: `[Parchment] Cannot wrap tab`
- **Ursache:** `asDelta().setValue(deltaJson)` geht durch RTE 2 Value-Pipeline. Die Delta-Ops mit `{"insert":{"tab":"3"}}` und `{"attributes":{"tabs-cont":"TABS-CONT"}}` werden von Quill 2 nicht korrekt verarbeitet.
- **V24:** `rte.setValue(delta)` setzt Delta direkt auf Quill 1 via `_editor.setContents()`
- **V25:** `rte.asDelta().setValue(delta)` geht durch parent `RichTextEditor` -- vermutlich Delta->HTML->Quill roundtrip
- **Fix:** Pruefen wie `asDelta().setValue()` intern arbeitet. Evtl. muss der Delta-Pfad direkt `_editor.setContents()` aufrufen statt ueber HTML.
- **Dateien:** V25 Java `RichTextEditor.asDelta()`, V25 JS TabBlot, V25 JS main `ready()`

### A2. ReadOnly-Embed-Blot rendert nicht
- **Symptom:** Card 6 -- "Some text" und "More text" sichtbar, aber `{"insert":{"readonly":"Some readonly text"}}` fehlt
- **Ursache:** ReadOnlyBlot ist als `Inline` registriert, aber der Delta-Format nutzt Embed-Syntax `{"insert":{"readonly":"text"}}`. In Quill 2 ist die Trennung zwischen Inline-Formats und Embed-Inserts strikt.
- **V24:** Quill 1 war toleranter bei der Format-Zuordnung
- **Fix:** Entweder ReadOnlyBlot als Embed umschreiben (wie PlaceholderBlot) ODER das Delta-Format auf Inline umstellen (`{"attributes":{"readonly":true},"insert":"text"}`)
- **Dateien:** `vcf-enhanced-rich-text-editor-blots.js` ReadOnlyBlot

### A3. PlaceholderBlot -- JSON.parse("undefined")
- **Symptom:** Card 7 -- Placeholder-Inhalt leer, Konsole: `SyntaxError: "undefined" is not valid JSON`
- **Ursache:** `PlaceholderBlot.loadValue(node)` ruft `JSON.parse(node.dataset.placeholder)` auf, aber `dataset.placeholder` ist `undefined`
- **Vermutung:** Wenn der Quill 2 HTML->Delta->DOM Roundtrip laeuft, geht das `data-placeholder` Attribut verloren (HTML-Sanitizer?) oder der `create(value)` Aufruf bekommt kein `value`
- **Fix:** Defensive Pruefung in `loadValue()` + Root-Cause analysieren (Sanitizer? Delta-Pipeline?)
- **Dateien:** `vcf-enhanced-rich-text-editor-blots.js:236`

### A4. Table-Blots -- Render als Einzelspalte
- **Symptom:** Cards 13/14 -- Tabelle zeigt 30 Zeilen x 1 Spalte statt 6x5 Grid
- **Konsole:** `TypeError: Cannot read properties of undefined (reading 'tables')` in `Table.optimize()`, `TypeError: ... 'getAttribute'` in `TableRow.createDefaultChild()`
- **Ursache:** Die Table-Blots wurden im Spike fuer 2x2/3x3 getestet (manuell eingefuegt), aber beim Laden eines VORDEFINIERTEN Deltas mit 30 Zellen treten Parchment 3 Probleme auf
- **Fix:** Table-Blots debuggen: `scroll.tables` Map fehlt, `createDefaultChild()` erhaelt falschen DOM-Knoten
- **Dateien:** `enhanced-rich-text-editor-tables-25/` JS Blot-Dateien

---

## B. HIGH -- Feature fehlt oder unvollstaendig

### B1. Toolbar-Slots -- Nur 3 statt 24
- **V24:** 24 named Slots (`toolbar-start`, `toolbar-end`, `toolbar-before-group-*`, `toolbar-after-group-*` fuer 10 Gruppen)
- **V25:** Nur `toolbar-start`, `toolbar-end`, `toolbar` (GROUP_CUSTOM). Kein `before-group-emphasis`, `after-group-history`, etc.
- **Impact:** Java-API `addToolbarComponents(ToolbarSlot.BEFORE_GROUP_GLYPH_TRANSFORMATION, ...)` funktioniert nicht fuer die meisten Slots
- **Fix:** In V25 `render()` Override die fehlenden Slots einfuegen. Muss kompatibel mit Lit Re-Render sein (Slots ueberleben Re-Render).
- **Dateien:** `vcf-enhanced-rich-text-editor.js` render() Methode

### B2. Ruler-System -- Visuell komplett fehlend
- **V24:** Horizontales Lineal oberhalb des Editors mit klickbaren Tabstop-Icons. `_rulerDisplay()`, `_addTabStop()`, `_addTabStopIcon()`.
- **V25:** `noRulers` Property existiert, aber kein visuelles Lineal gerendert. CSS fuer Ruler fehlt in Content-Styles.
- **Impact:** User kann keine Tabstops per Mausklick hinzufuegen/entfernen/verschieben
- **Fix:** Ruler-HTML in `render()` einfuegen, Click-Handler portieren, CSS aus V24 migrieren
- **Dateien:** V24 main JS Zeilen ~1000-1100 (Ruler-Code), V24 content-styles (Ruler-CSS)

### B3. Custom-Button Dynamic Update APIs -- Entfernt
- **V24:** `setCustomButtonLabel()`, `setCustomButtonTooltip()`, `setCustomButtonIcon()`, `setCustomButtonClickListener()`, `setCustomButtonKeyboardShortcut()` -- 8 Java Methoden + JS Gegenstuecke
- **V25:** Komplett entfernt. Nur `addToolbarComponents()` / `removeToolbarComponent()` existieren.
- **Impact:** Kunden die Buttons nach dem Einfuegen dynamisch aktualisieren, muessen umstellen
- **Fix:** Bewusste Entscheidung im Plan ("Clean break, kein deprecated Ballast"). Dokumentieren als Breaking Change. Alternativ: einfache Java-Wrapper die direkt auf die Component zugreifen.

### B4. Placeholder-Dialog (JS-seitiger) -- Entfernt
- **V24:** Inline-Dialog mit Input-Feld, ComboBox, OK/Cancel, Appearance-Toggle. ~300 Zeilen Template + 13 JS Methoden.
- **V25:** Dialog komplett entfernt. Stattdessen: `placeholder-button-click` Event -> Java -> Dialog in Java -> `_confirmInsertPlaceholders()` Callback.
- **Status:** Java-Events existieren (8 Listener), aber kein Java-seitiger Dialog implementiert. Der Demo-Code nutzt nur `addPlaceholderButtonClickedListener()` mit direktem `evt.insert(p1)`.
- **Impact:** Ohne Java-Dialog gibt es keine UI zum Auswaehlen welcher Placeholder eingefuegt wird
- **Fix:** Java-Dialog bauen (Vaadin Dialog mit ComboBox/Select) oder Kunden ueberlassen (Event-basiert)

---

## C. MEDIUM -- Differenzen die Funktionalitaet beeinflussen

### C1. Content-Styles -- 362 Zeilen weniger
- **V24:** 480 Zeilen (Ruler-CSS, Placeholder-Appearance-Modes, Listen, Blockquotes, Code, Links, Headings)
- **V25:** 118 Zeilen (nur ql-placeholder, ql-tab, ql-soft-break, Whitespace-Indicators)
- **Annahme:** RTE 2 liefert Base-Styles nativ. Aber: Placeholder-Appearance-Modes (2 Modi), Ruler-CSS fehlen komplett.
- **Fix:** Fehlende Placeholder-Appearance CSS und Ruler-CSS portieren

### C2. Toolbar-Styles -- 121 Zeilen weniger
- **V24:** 167 Zeilen (Button-Base-Styles, Icon-Sizing, Focus-Navigation, Mobile-Responsive)
- **V25:** 46 Zeilen (nur Slotted-Button-Styling, ERTE-Icon-Stroke-Color)
- **Annahme:** RTE 2 liefert Base-Toolbar-Styles. Muss visuell verifiziert werden.
- **Fix:** Visuell vergleichen, fehlende Styles nachportieren

### C3. Table-Styles -- Fehlen komplett
- **V24:** `vcf-enhanced-rich-text-editor-table-styles.js` (10KB, 280 Zeilen)
- **V25:** Datei fehlt komplett im V25-Modul
- **Fix:** Pruefen ob die Styles im Tables-25 Modul sind, sonst portieren

---

## D. LOW / ERLEDIGT / NICHT NOETIG

### Erledigt (funktioniert in V25):
- [x] Basic Editor rendering
- [x] Toolbar button visibility toggle (`setToolbarButtonsVisibility`)
- [x] Custom toolbar components via Slots (START, END, GROUP_CUSTOM)
- [x] Keyboard shortcuts (`addStandardToolbarButtonShortcut`, `addToolbarFocusShortcut`)
- [x] Icon replacement (`replaceStandardToolbarButtonIcon`)
- [x] noRulers Property (Toggle funktioniert, nur kein visuelles Lineal)
- [x] Whitespace indicators (Show/Hide toggle)
- [x] I18n (EnhancedRichTextEditorI18n mit ERTE-spezifischen Keys)
- [x] Theme Variants
- [x] HTML Sanitizer (ql-tab, ql-placeholder, ql-readonly, ql-soft-break erlaubt)
- [x] Value Change Mode
- [x] Text insertion (`addText()`)
- [x] HTML Value round-trip (`getValue()` / `setValue()`)
- [x] Tables extension enable/disable
- [x] Tables i18n
- [x] Tables style templates dialog

### Nativ durch RTE 2 (nicht portiert, nicht noetig):
- [x] Link-Dialog (RTE 2 nativ)
- [x] Image-Upload (RTE 2 nativ)
- [x] Text-Color / Background-Color (neu in RTE 2)
- [x] Fokus-Management (RTE 2 nativ)

### Bewusst entfernt (Clean Break):
- [x] `setPlacehoderAltAppearance` Typo -> `setPlaceholderAltAppearance` (korrigiert)
- [x] `addToobarFocusShortcut` Typo -> `addToolbarFocusShortcut` (korrigiert)
- [x] Polymer-spezifischer Code
- [x] `SpringDemoView` / `DemoView` Abhaengigkeit
- [x] `sampleEditorExtensionConnector.js`

---

## E. Priorisierte Reihenfolge fuer die Implementierung

| Prio | Item | Aufwand | Grund |
|:----:|------|:-------:|-------|
| 1 | A1: Delta-Pipeline fixen (asDelta().setValue) | MEDIUM | Blockiert Tab-stops, Readonly, Placeholders, Tables |
| 2 | A2: ReadOnlyBlot Embed vs Inline fixen | LOW | Haengt evtl. von A1 ab |
| 3 | A3: PlaceholderBlot loadValue() fixen | LOW | Defensive Pruefung + Root-Cause |
| 4 | A4: Table-Blot Rendering fixen | HIGH | 30-Zellen Delta triggert Parchment 3 Edge-Cases |
| 5 | B1: Toolbar-Slots erweitern | MEDIUM | Breaking Change fuer Slot-basierte Kunden |
| 6 | B2: Ruler-System portieren | HIGH | Visuell wichtig, viel Code |
| 7 | C1+C2+C3: Fehlende Styles | MEDIUM | Visuell wichtig |
| 8 | B4: Placeholder-Dialog | MEDIUM | UX-relevant aber Event-API existiert |
| 9 | B3: Custom-Button APIs | LOW | Dokumentieren als Breaking Change reicht evtl. |
