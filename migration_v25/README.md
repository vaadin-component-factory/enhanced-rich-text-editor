# V25 Migration — ERTE auf Vaadin 25 / Quill 2

## Analyse-Dokumente

| Datei | Inhalt |
|-------|--------|
| `user_description.md` | Ausgangslage: Kundenbeschreibung der 20 ERTE-Features, die migriert werden müssen |
| `agents_analysis.md` | Analyse aller 20 Features mit Aufwandsbewertung (FREE / CUSTOM / REWRITE) |
| `feature_comparison.md` | Detailvergleich ERTE 1 vs. RTE 2: was ist vorhanden, was fehlt, was sich geaendert hat |
| `implementation_notes.md` | Architektur-Entscheidungen, offene Fragen, und die 23 Spike-Items (Abschnitt 10) |
| `quill_v1_to_v2_api_diff.md` | API-Diff Quill 1.3.6 → 2.0.3: Blots, Keyboard, Delta, Parchment-Aenderungen |

## Spike-Projekt (`spike/`)

Lauffaehiges Vaadin-25-Projekt zum Validieren aller Architektur-Entscheidungen.

| Datei / Ordner | Inhalt |
|----------------|--------|
| `SPIKE_RESULTS.md` | Ergebnisse aller 23 Spike-Items + Table-Spike (T1–T7) |
| `pom.xml` | Maven-Projekt: Vaadin 25.0.4, Spring Boot 4.0.2 |
| `src/main/java/.../EnhancedRichTextEditor.java` | Java-Subclass von RTE 2 mit eigenem `@Tag` |
| `src/main/java/.../SpikeView.java` | Test-UI mit Buttons fuer alle Spike-Items |
| `src/main/frontend/vcf-enhanced-rich-text-editor.js` | JS-Klasse: Lit-Extension, Toolbar, Tab-Blot, Table-Blots (Parchment 3) |
| `server-start.sh` / `server-stop.sh` | Server starten/stoppen (Port 8081) |
| `print-server-logs.sh` | Logs anzeigen (`-f` follow, `-state` Status, `-errors` Fehler) |
| `screenshots/` | Playwright-Screenshots der visuellen Verifikation (Phase 4) |
| `console-*.log` | Konsolen-Mitschnitte aus Debug-Sessions |

## Sonstiges

| Datei | Inhalt |
|-------|--------|
| `MEMORY_2026-02-16T165113.md` | Snapshot des Claude-Code-Arbeitsspeichers (Kontext-Sicherung) |
