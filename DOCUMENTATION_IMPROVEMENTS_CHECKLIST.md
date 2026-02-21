# Documentation Improvements — Action Items Checklist

Generated from: `/workspace/DOCUMENTATION_REVIEW_V25_PHASES1-3.md`
Priority: P0 (before GA), P1 (6.0.1 patch), P2 (post-GA)

---

## P0 — CRITICAL (Must fix before GA)

### P0.1 Event Listener Methods — Add Javadocs
**File:** `enhanced-rich-text-editor-v25/src/main/java/com/vaadin/componentfactory/EnhancedRichTextEditor.java`
**Lines:** 441-479
**Tasks:**
- [ ] Add Javadoc to `addPlaceholderButtonClickedListener()`
  - Explain: Fired when user clicks the placeholder button in toolbar
  - Explain: Callback contains cursor position
  - Reference: PlaceholderButtonClickedEvent#insert()
- [ ] Add Javadoc to `addPlaceholderBeforeInsertListener()`
  - Explain: Fired BEFORE insertion begins (can be prevented)
- [ ] Add Javadoc to `addPlaceholderInsertedListener()`
  - Explain: Fired AFTER insertion confirmed
- [ ] Add Javadoc to `addPlaceholderBeforeRemoveListener()`
  - Explain: Fired BEFORE deletion (can be prevented)
- [ ] Add Javadoc to `addPlaceholderRemovedListener()`
  - Explain: Fired AFTER deletion confirmed
- [ ] Add Javadoc to `addPlaceholderSelectedListener()`
  - Explain: When is this fired? (mouse selection? keyboard selection?)
- [ ] Add Javadoc to `addPlaceholderLeaveListener()`
  - Explain: When is this fired? (blur? escape key?)
- [ ] Add Javadoc to `addPlaceholderAppearanceChangedListener()`
  - Explain: Fired when placeholder appearance toggle is called

**Template:**
```java
/**
 * Registers a listener that is invoked when [DESCRIBE EVENT TRIGGER].
 * <p>
 * [DETAILED EXPLANATION OF BEHAVIOR AND USE CASES]
 * <p>
 * Example:
 * <pre>
 * editor.addPlaceholder...Listener(event -> {
 *     // [EXAMPLE USAGE]
 * });
 * </pre>
 *
 * @param listener the listener to register
 * @return a registration that can be used to unregister the listener
 * @see PlaceholderXyzEvent
 */
```

---

### P0.2 Event Classes — Expand Documentation
**File:** `enhanced-rich-text-editor-v25/src/main/java/com/vaadin/componentfactory/EnhancedRichTextEditor.java`
**Lines:** 485-671
**Tasks:**

#### PlaceholderButtonClickedEvent (525-560)
- [ ] Expand class Javadoc (currently only "@DomEvent")
  ```java
  /**
   * Fired when the user clicks the placeholder button in the toolbar.
   * <p>
   * The event provides the current cursor position ({@link #getPosition()})
   * where the placeholder should be inserted (or the position where the user
   * clicked if selecting from the placeholder dialog).
   * <p>
   * Usage: Call {@link #insert(Placeholder)} to insert a placeholder at the
   * current cursor position, or {@link #insert(Placeholder, int)} to insert
   * at a specific position.
   */
  ```
- [ ] Add Javadoc to `getPosition()` method
- [ ] Clarify `insert()` method: "Inserts placeholder at current cursor position immediately"
- [ ] Clarify overloaded `insert(Placeholder, int)`: "Inserts placeholder at specified index"

#### AbstractMultiPlaceholderEvent (488-523)
- [ ] Expand class Javadoc
- [ ] Document `getPlaceholders()` return value: "Returns placeholders matched against master list"

#### PlaceholderBeforeInsertEvent (562-581)
- [ ] **CRITICAL:** Expand class Javadoc
  ```java
  /**
   * Fired BEFORE placeholders are inserted into the editor.
   * <p>
   * This is a cancellable event. Placeholders will only be inserted
   * if {@link #insert()} is called. If this method is NOT called,
   * the insertion is prevented.
   * <p>
   * This allows listeners to validate or modify the placeholders
   * before insertion. For example, to prevent empty placeholders
   * or to normalize text values.
   */
  ```
- [ ] Add Javadoc to `insert()` method: Clarify return type and behavior

#### PlaceholderBeforeRemoveEvent (594-613)
- [ ] Similar to BeforeInsertEvent: "BEFORE removal", "cancellable", "call remove() to confirm"

#### PlaceholderRemovedEvent (615-624)
- [ ] Expand: "Fired AFTER placeholders are removed"

#### PlaceholderSelectedEvent (626-635)
- [ ] Expand: "Fired when user selects placeholder(s) (e.g., via mouse or keyboard)"

#### PlaceholderLeaveEvent (637-645)
- [ ] Expand: "Fired when user exits placeholder (blur, escape key, or navigation)"

#### PlaceholderAppearanceChangedEvent (647-671)
- [ ] **CRITICAL:** Add class Javadoc explaining event trigger
- [ ] **CRITICAL:** Document getters:
  ```java
  /**
   * Returns the new alt-appearance state (null if not changed).
   * When true, placeholder displays in "alt mode" (typically shows only
   * the matched regex group). When false, shows full placeholder text.
   */
  public Boolean getAltAppearance() { ... }

  /**
   * Returns the appearance label (null if not changed).
   * This corresponds to the UI label selected by the user
   * (e.g., "Plain" or "Value" in English).
   */
  public String getAppearanceLabel() { ... }
  ```

---

### P0.3 Regex Pattern Constants — Document
**File:** `enhanced-rich-text-editor-v25/src/main/java/com/vaadin/flow/component/richtexteditor/RteExtensionBase.java`
**Lines:** 90-104
**Tasks:**
- [ ] Add Javadoc above CLASS_ATTR_PATTERN (line 90)
  ```java
  /** Regex to extract class attribute values (e.g., class="foo bar") */
  ```
- [ ] Add Javadoc above STYLE_ATTR_PATTERN (line 93)
  ```java
  /** Regex to extract style attribute values (e.g., style="color: red; ...") */
  ```
- [ ] Add Javadoc above CSS_COMMENT_PATTERN (line 96)
  ```java
  /** Regex to match CSS comments (/* ... */) for removal */
  ```
- [ ] Add Javadoc above CSS_FUNCTION_PATTERN (line 99)
  ```java
  /** Regex to match CSS function calls like rgb(, calc(, etc. */
  ```
- [ ] Add Javadoc above DATA_SRC_PATTERN (line 102)
  ```java
  /** Regex to extract MIME type from data: URLs in src attributes */
  ```

---

### P0.4 Placeholder Configuration Workflow — Document
**File:** `enhanced-rich-text-editor-v25/src/main/java/com/vaadin/componentfactory/EnhancedRichTextEditor.java`
**Lines:** 358-437 (Placeholder API section)
**Tasks:**
- [ ] Add section comment before `setPlaceholders()`:
  ```java
  // ---- Placeholder System Configuration ----
  // To configure placeholders, follow this sequence:
  // 1. Define placeholder list: setPlaceholders(List.of(new Placeholder(...)))
  // 2. Optionally set display tags: setPlaceholderTags("@", "")
  // 3. Optionally configure alt-appearance: setPlaceholderAltAppearancePattern("[a-z]+")
  // 4. Toggle alt mode if needed: setPlaceholderAltAppearance(true)
  // 5. Listen for events: addPlaceholderInsertedListener(...)
  ```
- [ ] OR create new section comment at line 358 explaining the complete flow
- [ ] Expand `setPlaceholderAltAppearancePattern()` Javadoc with example:
  ```java
  /**
   * Sets the regex pattern used to extract alt appearance text from
   * placeholder text. Groups matched by this pattern are shown in alt mode.
   * <p>
   * Example: Pattern "[a-z]+" extracts first sequence of lowercase letters.
   * If placeholder text is "FirstName_LastName", pattern matches "FirstName".
   *
   * @param pattern the regex pattern (e.g. "[a-z]+", "\\([^)]+\\)")
   */
  ```
- [ ] Add cross-reference Javadoc from `setPlaceholders()` to explain this is step 1

---

## P1 — IMPORTANT (6.0.1 patch or documentation portal)

### P1.1 Keyboard Shortcut API — Add Examples
**File:** `enhanced-rich-text-editor-v25/src/main/java/com/vaadin/componentfactory/EnhancedRichTextEditor.java`
**Lines:** 246-278
**Tasks:**
- [ ] Expand `addStandardToolbarButtonShortcut()` Javadoc
  ```java
  /**
   * Binds a keyboard shortcut to a standard toolbar button. When the
   * shortcut is pressed, the button is clicked, triggering its native
   * handler (format toggle, dialog open, undo/redo, etc.).
   * <p>
   * Key names follow Quill 2 conventions. Common examples:
   * <ul>
   *   <li>"b" for single character</li>
   *   <li>"F9" for function keys</li>
   *   <li>"Tab", "Enter", "Escape" for special keys</li>
   * </ul>
   * <p>
   * Example: Bind Ctrl+Alt+P to Placeholder button:
   * <pre>
   * editor.addStandardToolbarButtonShortcut(
   *     ToolbarButton.PLACEHOLDER, "p", true, false, true);
   * </pre>
   * <p>
   * For complete key name reference, see
   * <a href="https://quilljs.com/docs/modules/keyboard/">Quill 2 Keyboard Module</a>
   *
   * @param toolbarButton the toolbar button to trigger
   * @param key Quill 2 key name (e.g. "F9", "p", "Tab")
   * @param shortKey true for Ctrl (Win/Linux) or Cmd (Mac)
   * @param shiftKey true for Shift modifier
   * @param altKey true for Alt modifier
   */
  ```
- [ ] Similar expansion for `addToolbarFocusShortcut()`

---

### P1.2 Complex Filter Logic — Add Inline Comments
**File:** `enhanced-rich-text-editor-v25/src/main/java/com/vaadin/flow/component/richtexteditor/RteExtensionBase.java`
**Lines:** 170-197 (filterErteClasses)
**Tasks:**
- [ ] Add inline comment before loop (line 176):
  ```java
  // Extract individual class names (space-separated)
  String[] classes = classValue.split("\\s+");
  ```
- [ ] Add comment before class checking loop (line 177):
  ```java
  // Filter classes: keep Quill standard classes and known ERTE classes
  ```
- [ ] Add comment in condition (line 180):
  ```java
  // Quill standard: alignment (ql-align-left) and indent (ql-indent-1) classes
  ```
- [ ] Add comment before unknown classes (line 190):
  ```java
  // Strip unknown classes (security)
  ```

**Lines:** 205-253 (filterStyleAttributes)
**Tasks:**
- [ ] Add comment before forEach (line 212):
  ```java
  // Split CSS declarations by semicolon
  ```
- [ ] Add comment before property check (line 223):
  ```java
  // Only allow whitelisted CSS properties (prevent injection attacks)
  ```
- [ ] Add comment before function check (line 227):
  ```java
  // Only allow safe CSS functions like rgb(), calc() — prevent evil url() etc.
  ```

---

### P1.3 Private Helper Methods — Document
**File:** `enhanced-rich-text-editor-v25/src/main/java/com/vaadin/componentfactory/Placeholder.java`
**Lines:** 71-76 (nodeToValue), 140-145 (putTypedValue)
**Tasks:**
- [ ] Add Javadoc to `nodeToValue()`:
  ```java
  /**
   * Converts a Jackson JsonNode to a typed Java object.
   * Returns: boolean if node is boolean, int if node is int,
   * double if node is double, otherwise String.
   */
  ```
- [ ] Add Javadoc to `putTypedValue()`:
  ```java
  /**
   * Stores a typed value into a Jackson ObjectNode, preserving type
   * (boolean, integer, double, or string). Inverse of nodeToValue().
   */
  ```

---

### P1.4 Keyboard Binding Priority — Document
**File:** (Spike-related documentation)
**Tasks:**
- [ ] Add comment to memory: Keyboard binding priority pattern (from SPIKE_RESULTS.md)
  - Already documented in project, but worth linking from API Javadocs
- [ ] Reference this in `addStandardToolbarButtonShortcut()` Javadoc if relevant

---

## P2 — NICE-TO-HAVE (Post-GA or documentation portal)

### P2.1 Developer Guides (Separate Documents)
**Tasks:**
- [ ] Create: `TOOLBAR_CUSTOMIZATION_GUIDE.md`
  - How to add custom toolbar buttons
  - How to replace icons
  - How to control visibility
  - How to bind shortcuts
- [ ] Create: `PLACEHOLDER_CONFIGURATION_GUIDE.md`
  - Complete placeholder setup example
  - Event handling patterns
  - Alt-appearance configuration
- [ ] Create: `EVENT_HANDLING_GUIDE.md`
  - Event lifecycle and timing
  - Preventing default behavior
  - Common patterns

### P2.2 JavaScript API Documentation
**File:** `enhanced-rich-text-editor-v25/src/main/resources/META-INF/resources/frontend/vcf-enhanced-rich-text-editor.js`
**Tasks:**
- [ ] Add class-level JSDoc to VcfEnhancedRichTextEditor
- [ ] Document `ready()` method (lifecycle hook)
- [ ] Document key internal methods with signatures:
  - `_onTabStopsChanged()`
  - `_injectToolbarSlots()`
  - `_onReadonlyChanged()`
  - Etc.

### P2.3 Code Examples on Class Level
**Tasks:**
- [ ] Add "Quick Start" example to EnhancedRichTextEditor class Javadoc
  ```java
  /**
   * Example:
   * <pre>
   * EnhancedRichTextEditor editor = new EnhancedRichTextEditor();
   * editor.setPlaceholders(List.of(new Placeholder("Name"), ...));
   * editor.setTabStops(List.of(new TabStop(TabStop.Direction.LEFT, 100)));
   * add(editor);
   * </pre>
   */
  ```

---

## Tracking & Sign-Off

| Task | Assignee | Status | Date | Notes |
|------|----------|--------|------|-------|
| P0.1 Event Listeners | — | ⬜ TODO | | 8 methods |
| P0.2 Event Classes | — | ⬜ TODO | | 8 classes |
| P0.3 Regex Patterns | — | ⬜ TODO | | 5 constants |
| P0.4 Placeholder Workflow | — | ⬜ TODO | | Section comment |
| **P0 TOTAL** | | | | **Effort: 3-4h** |
| P1.1 Keyboard Shortcut Examples | — | ⬜ TODO | | Quill 2 reference |
| P1.2 Inline Comments (filters) | — | ⬜ TODO | | 2 methods |
| P1.3 Private Helpers | — | ⬜ TODO | | 2 methods |
| P1.4 Keyboard Binding | — | ⬜ TODO | | Spike reference |
| **P1 TOTAL** | | | | **Effort: 2h** |
| **P0+P1 TOTAL** | | | | **Effort: 5-6h** |

---

## Sign-Off Checklist (Before GA)

- [ ] All P0 items completed and reviewed
- [ ] Javadocs validate with `mvn javadoc:javadoc` (no warnings)
- [ ] Event documentation matches actual event firing logic in JavaScript
- [ ] Placeholder workflow documentation tested against actual code flow
- [ ] Code examples compile and run without errors
- [ ] No TODO/FIXME placeholders left in documentation

