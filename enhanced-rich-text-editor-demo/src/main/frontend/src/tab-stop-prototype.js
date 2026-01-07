import Quill from 'quill';
import 'quill/dist/quill.snow.css';

const Embed = Quill.import('blots/embed');

/**
 * Custom Blot for Tab Stops.
 * Renders as a span but acts like a character.
 */
class TabBlot extends Embed {
    static create(value) {
        let node = super.create();
        node.setAttribute('contenteditable', 'false');

        // Insert Zero Width Space (\u200B).
        // This trick ensures the browser treats the element as a distinct character
        // for cursor navigation and deletion, preventing it from being skipped.
        node.innerText = '\u200B';
        return node;
    }
}

TabBlot.blotName = 'tab';
TabBlot.tagName = 'span';
TabBlot.className = 'ql-tab';

class SoftBreakBlot extends Embed {
    static create(value) {
        let node = super.create(value);
        node.innerHTML = '<br>';

        return node;
    }
}

SoftBreakBlot.blotName = 'soft-break';
SoftBreakBlot.tagName = 'span';
SoftBreakBlot.className = 'ql-soft-break';

window._nativeQuill = {
    EXTERNAL_TAB_STOPS: [
        { pos: 100, align: 'left' },
        { pos: 300, align: 'center' },
        { pos: 500, align: 'right' }
    ],
    DEFAULT_TAB_SIZE: 50,

    quillInstance: null,
    updateTicking: false,
    isBlotRegistered: false,
    lastCommittedDelta: null, // Stores JSON string of delta for comparison

    /**
     * Initializes the Quill instance.
     * @param {HTMLElement} containerElement - The wrapper element from Vaadin/DOM.
     * @param {object|string} initialValue - Delta object or JSON string to initialize.
     */
    init: function (containerElement, initialValue) {
        if (!this.isBlotRegistered) {
            Quill.register(TabBlot);
            Quill.register(SoftBreakBlot);
            this.isBlotRegistered = true;
        }

        this.quillInstance = new Quill(containerElement, {
            theme: 'snow',
            modules: {
                keyboard: {
                    bindings: {
                        tab: {
                            key: 9,
                            handler: (range, context) => {
                                this.handleTabPress(range);
                                return false; // Prevent default focus loss
                            }
                        },
                        'softBreak': {
                            key: 13,
                            shiftKey: true,
                            handler: function(range) {
                                const quill = this.quill;

                                // 1. Aktuelle Zeile (Block) und Start-Index ermitteln
                                const [line, offset] = quill.getLine(range.index);

                                // 2. Zählen, wie viele Tabs am Anfang dieser Zeile stehen
                                let leadingTabsCount = 0;
                                let currentBlot = line.children.head; // Der erste Teil der Zeile

                                // Wir laufen durch die Blots, solange es Tabs sind
                                while (currentBlot && currentBlot.statics.blotName === 'tab') {
                                    leadingTabsCount++;
                                    currentBlot = currentBlot.next;
                                }

                                // 3. Den Soft-Break einfügen
                                quill.insertEmbed(range.index, 'soft-break', true, Quill.sources.USER);

                                // 4. Die gezählten Tabs direkt dahinter wieder einfügen
                                // Wir fügen sie nacheinander ein. Index verschiebt sich jedes Mal um 1.
                                let insertPos = range.index + 1;

                                for (let i = 0; i < leadingTabsCount; i++) {
                                    quill.insertEmbed(insertPos, 'tab', true, Quill.sources.USER);
                                    insertPos++;
                                }

                                // 5. Cursor hinter die neu eingefügten Tabs setzen
                                // Timeout ist wichtig wegen Rendering
                                setTimeout(() => {
                                    quill.setSelection(insertPos, Quill.sources.SILENT);

                                    // WICHTIG: Da wir neue Tabs eingefügt haben, müssen wir
                                    // die Breitenberechnung triggern!
                                    window._nativeQuill.requestTabUpdate();
                                }, 1);

                                return false;
                            }
                        }                    }
                },
                toolbar: [
                    ['bold', 'italic', 'underline'],
                    [{'header': 1}, {'header': 2}],
                    ['clean']
                ]
            }
        });

        // Initialize Content with Delta
        if (initialValue) {
            let delta = initialValue;
            // Handle JSON string input if passed from Java as string
            if (typeof initialValue === 'string') {
                try {
                    delta = JSON.parse(initialValue);
                } catch (e) {
                    console.error("NativeQuill: Could not parse initialValue JSON", e);
                }
            }
            this.quillInstance.setContents(delta);
        }

        // Store initial state as stringified JSON for easy comparison
        this.lastCommittedDelta = JSON.stringify(this.quillInstance.getContents());

        this.drawRuler();

        // Update layouts on text change or resize
        this.quillInstance.on('text-change', () => {
            this.requestTabUpdate();
        });

        window.addEventListener('resize', () => {
            this.requestTabUpdate();
        });

        // Mimic standard <input> behavior:
        // Track value on focus, check for changes on blur, and dispatch event.
        this.quillInstance.root.addEventListener('focus', () => {
            this.lastCommittedDelta = JSON.stringify(this.quillInstance.getContents());
        });

        this.quillInstance.root.addEventListener('blur', () => {
            const currentDelta = this.quillInstance.getContents();
            const currentDeltaString = JSON.stringify(currentDelta);

            if (currentDeltaString !== this.lastCommittedDelta) {
                this.lastCommittedDelta = currentDeltaString;

                const event = new CustomEvent('change', {
                    detail: { value: currentDeltaString }, // Payload is the Delta Object
                    bubbles: true,
                    composed: true
                });
                containerElement.dispatchEvent(event);
            }
        });

        this.requestTabUpdate();

        console.log("NativeQuill initialized.");
    },

    /**
     * Renders a static ruler visualization between the toolbar and the editor area.
     */
    /**
     * Renders a static ruler visualization with alignment icons.
     */
    /**
     * Renders a static ruler visualization with simple text alignment icons (L, C, R).
     */
    drawRuler: function () {
        if (!this.quillInstance) return;

        // Altes Lineal entfernen
        const oldRuler = this.quillInstance.container.parentNode.querySelector('.native-quill-ruler');
        if (oldRuler) oldRuler.remove();

        const ruler = document.createElement('div');
        ruler.className = 'native-quill-ruler';

        // ---------------------------------------------------------
        // A) Klick auf Hintergrund -> NEUEN STOP ERSTELLEN
        // ---------------------------------------------------------
        ruler.addEventListener('click', (e) => {
            // Nur feuern, wenn man direkt auf das Lineal klickt (nicht auf einen existierenden Marker)
            if (e.target !== ruler) return;

            const newPos = e.offsetX; // Klick-Position relativ zum Lineal

            // Optional: Einrasten auf 10er Schritte (Snap to grid)
            // const snappedPos = Math.round(newPos / 10) * 10;

            // Neuen Stop hinzufügen (Left ist default)
            this.EXTERNAL_TAB_STOPS.push({ pos: newPos, align: 'left' });

            // Array sortieren (wichtig für die Logik!)
            this.EXTERNAL_TAB_STOPS.sort((a, b) => a.pos - b.pos);

            // Update alles
            this.drawRuler();
            this.updateTabWidths();
        });

        // ---------------------------------------------------------
        // B) MARKER ZEICHNEN
        // ---------------------------------------------------------
        this.EXTERNAL_TAB_STOPS.forEach(stopData => {
            // Normalize Data
            const pos = (typeof stopData === 'object') ? stopData.pos : stopData;
            let align = (typeof stopData === 'object') ? (stopData.align || 'left') : 'left';

            const tick = document.createElement('div');
            tick.className = 'ruler-tick';
            tick.style.left = pos + 'px';

            // 1. Align Text (L, C, R)
            const alignText = document.createElement('span');
            alignText.className = 'ruler-align-text';
            alignText.innerText = (align === 'center') ? 'C' : (align === 'right') ? 'R' : 'L';
            tick.appendChild(alignText);

            // 2. KLICK HANDLER: Zyklus L -> C -> R -> Delete
            tick.addEventListener('click', (e) => {
                e.stopPropagation(); // Verhindert, dass das Lineal darunter reagiert

                const index = this.EXTERNAL_TAB_STOPS.indexOf(stopData);
                if (index === -1) return; // Sollte nicht passieren

                if (align === 'left') {
                    stopData.align = 'center'; // L -> C
                } else if (align === 'center') {
                    stopData.align = 'right';  // C -> R
                } else {
                    // R -> Löschen (aus Array entfernen)
                    this.EXTERNAL_TAB_STOPS.splice(index, 1);
                }

                // Update alles
                this.drawRuler();
                this.updateTabWidths();
            });

            ruler.appendChild(tick);
        });

        const qlContainer = this.quillInstance.container;
        qlContainer.parentNode.insertBefore(ruler, qlContainer);
    },

    /**
     * Inserts the tab blot and manages cursor position.
     */
    handleTabPress: function (range) {
        if (!this.quillInstance) return;

        this.quillInstance.insertEmbed(range.index, 'tab', true, Quill.sources.USER);

        // A minimal timeout is required to allow the browser to render the DOM node
        // before we attempt to set the selection after it.
        setTimeout(() => {
            this.quillInstance.setSelection(range.index + 1, Quill.sources.API);
        }, 1);

        this.requestTabUpdate();
    },

    /**
     * Debounces the width calculation using requestAnimationFrame.
     */
    requestTabUpdate: function () {
        if (!this.updateTicking) {
            window.requestAnimationFrame(() => {
                this.updateTabWidths();
                this.updateTicking = false;
            });
            this.updateTicking = true;
        }
    },

    /**
     * Core Logic: Calculates and applies the correct pixel width for every tab stop
     * based on its current X-position relative to the editor.
     */
    updateTabWidths: function() {
        if (!this.quillInstance) return;

        const MIN_TAB_WIDTH = 2; // Mindestbreite
        const editorNode = this.quillInstance.root;
        const editorRect = editorNode.getBoundingClientRect();

        // Alle Tabs holen
        const tabs = Array.from(editorNode.querySelectorAll('.ql-tab'));

        tabs.forEach(tab => {
            const tabRect = tab.getBoundingClientRect();
            // Startposition relativ zum Editor-Rand
            const startPos = tabRect.left - editorRect.left;

            // 1. Zugehörigen Text ermitteln (Look-Ahead)
            // Wir suchen alles, was nach dem Tab kommt, bis zum nächsten Tab, Break oder Block-Ende
            let contentWidth = 0;
            let nextNode = tab.nextSibling;

            // Wir schauen uns die nächsten Nodes an (TextNodes oder Elements)
            while (nextNode) {
                // Abbruchbedingungen:
                // - Nächster Node ist selbst ein Tab
                if (nextNode.classList && nextNode.classList.contains('ql-tab')) break;
                // - Nächster Node ist ein Soft-Break (<br class="ql-soft-break">)
                if (nextNode.classList && nextNode.classList.contains('ql-soft-break')) break;
                // - Nächster Node ist ein Block-Element (sollte in Quill p/h1 kaum passieren inline)
                if (nextNode.tagName && ['P', 'DIV', 'LI'].includes(nextNode.tagName)) break;

                // Messen
                if (nextNode.nodeType === 3) { // TextNode
                    contentWidth += this.measureTextWidth(nextNode.nodeValue, tab.parentNode);
                } else if (nextNode.innerText) { // Element (z.B. strong, em)
                    // Bei Elementen können wir direkt deren Breite nehmen oder auch textContent messen
                    // Messen ist sicherer wegen padding/margin styles
                    contentWidth += this.measureTextWidth(nextNode.innerText, nextNode);
                }

                nextNode = nextNode.nextSibling;
            }

            // 2. Passenden Tabstop finden
            // Wir suchen den nächsten Stop, der nach unserer Startposition kommt.
            // Bei Right/Center muss der Stop aber "Platz" für den Text haben.

            let targetStop = this.EXTERNAL_TAB_STOPS.find(stop => {
                // Einfache Logik: Ist der Stop weiter rechts als mein Start?
                // (Verfeinerung: Man könnte hier prüfen, ob er "erreichbar" ist)
                return stop.pos > (startPos + MIN_TAB_WIDTH);
            });

            // Fallback: Default Grid (immer Left align)
            let alignment = 'left';
            let stopPos = 0;

            if (!targetStop) {
                // Infinite Grid Logic (wie gehabt)
                let potentialPos = Math.ceil(startPos / this.DEFAULT_TAB_SIZE) * this.DEFAULT_TAB_SIZE;
                if (potentialPos - startPos < MIN_TAB_WIDTH) potentialPos += this.DEFAULT_TAB_SIZE;
                stopPos = potentialPos;
            } else {
                stopPos = targetStop.pos;
                alignment = targetStop.align || 'left';
            }

            // 3. Breite berechnen basierend auf Alignment
            let widthNeeded = 0;
            const rawDistance = stopPos - startPos;

            if (alignment === 'right') {
                // Stop ist rechts, Text wächst nach links.
                // Tab schrumpft, je breiter der Text wird.
                widthNeeded = rawDistance - contentWidth;
            } else if (alignment === 'center') {
                // Stop ist die Mitte des Textes.
                widthNeeded = rawDistance - (contentWidth / 2);
            } else {
                // Left (Standard)
                widthNeeded = rawDistance;
            }

            // Sicherheitsnetz: Tab darf nicht negativ werden oder verschwinden
            // Wenn der Text zu lang ist, schiebt er halt über den Tabstop hinaus.
            if (widthNeeded < MIN_TAB_WIDTH) widthNeeded = MIN_TAB_WIDTH;

            // DOM Update
            const newWidthStr = widthNeeded + 'px';
            if (tab.style.width !== newWidthStr) {
                tab.style.width = newWidthStr;
            }
        });
    },

    /**
     * Misst die Pixelbreite eines Strings mit den exakten Styles des Editors.
     */
    measureTextWidth: function(text, referenceNode) {
        if (!text) return 0;

        // Wir erstellen (oder recyceln) ein unsichtbares Span zum Messen
        let measureSpan = document.getElementById('ql-measure-span');
        if (!measureSpan) {
            measureSpan = document.createElement('span');
            measureSpan.id = 'ql-measure-span';
            measureSpan.style.visibility = 'hidden';
            measureSpan.style.position = 'absolute';
            measureSpan.style.whiteSpace = 'pre';
            measureSpan.style.left = '-9999px';
            document.body.appendChild(measureSpan);
        }

        // Styles vom aktuellen Kontext kopieren (wichtig für bold/italic/font-size)
        const computedStyle = window.getComputedStyle(referenceNode);
        measureSpan.style.fontFamily = computedStyle.fontFamily;
        measureSpan.style.fontSize = computedStyle.fontSize;
        measureSpan.style.fontWeight = computedStyle.fontWeight;
        measureSpan.style.fontStyle = computedStyle.fontStyle;
        measureSpan.style.letterSpacing = computedStyle.letterSpacing;

        measureSpan.textContent = text;
        return measureSpan.getBoundingClientRect().width;
    },
};