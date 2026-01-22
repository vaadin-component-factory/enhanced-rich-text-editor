import Quill from 'quill';
import 'quill/dist/quill.snow.css';

const Embed = Quill.import('blots/embed');

/**
 * 1. Custom Blot for Tab Stops.
 */
class TabBlot extends Embed {
    static create(value) {
        let node = super.create();
        node.setAttribute('contenteditable', 'false');
        node.innerText = '\u200B'; // Zero Width Space

        // Smart Cursor Placement
        node.addEventListener('mousedown', (e) => {
            e.preventDefault();
            e.stopPropagation();

            const quill = window._nativeQuill ? window._nativeQuill.quillInstance : null;
            if (!quill) return;

            const blot = Quill.find(node);
            if (!blot) return;

            const index = quill.getIndex(blot);
            const rect = node.getBoundingClientRect();
            const clickX = e.clientX - rect.left;

            if (clickX < rect.width / 2) {
                quill.setSelection(index, 0, Quill.sources.USER);
            } else {
                quill.setSelection(index + 1, 0, Quill.sources.USER);
            }
        });

        return node;
    }
}
TabBlot.blotName = 'tab';
TabBlot.tagName = 'span';
TabBlot.className = 'ql-tab';

/**
 * 2. Soft Break Blot
 */
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


/**
 * 3. Main Logic
 */
window._nativeQuill = {
    // GLOBAL Tab Stops
    EXTERNAL_TAB_STOPS: [
        { pos: 100, align: 'left' },
        { pos: 300, align: 'center' },
        { pos: 500, align: 'right' }
    ],

    quillInstance: null,
    updateTicking: false,
    isBlotRegistered: false,
    lastCommittedDelta: null,

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
                            handler: (range) => {
                                this.handleTabPress(range);
                                return false;
                            }
                        },
                        'softBreak': {
                            key: 13, shiftKey: true,
                            handler: function(range) {
                                const quill = this.quill;
                                const [line] = quill.getLine(range.index);
                                let leadingTabsCount = 0;
                                let currentBlot = line.children.head;
                                while (currentBlot && currentBlot.statics.blotName === 'tab') {
                                    leadingTabsCount++;
                                    currentBlot = currentBlot.next;
                                }

                                quill.insertEmbed(range.index, 'soft-break', true, Quill.sources.USER);

                                let insertPos = range.index + 1;
                                for (let i = 0; i < leadingTabsCount; i++) {
                                    quill.insertEmbed(insertPos, 'tab', true, Quill.sources.USER);
                                    insertPos++;
                                }

                                setTimeout(() => {
                                    quill.setSelection(insertPos, Quill.sources.SILENT);
                                    window._nativeQuill.requestTabUpdate();
                                }, 1);
                                return false;
                            }
                        },
                        'enter': { key: 13, shiftKey: false, handler: function() { return true; } }
                    }
                },
                toolbar: [
                    ['bold', 'italic', 'underline'],
                    [{'header': 1}, {'header': 2}],
                    [{ 'indent': '-1'}, { 'indent': '+1' }],
                    ['clean']
                ]
            }
        });

        if (initialValue) {
            let delta = initialValue;
            try { if (typeof initialValue === 'string') delta = JSON.parse(initialValue); } catch(e){}
            this.quillInstance.setContents(delta);
        }

        this.lastCommittedDelta = JSON.stringify(this.quillInstance.getContents());

        // Event Listeners
        this.quillInstance.on('text-change', () => {
            this.requestTabUpdate();
            // Fire change event
            const currentContent = JSON.stringify(this.quillInstance.getContents());
            const event = new CustomEvent('change', {
                detail: { value: currentContent },
                bubbles: true,
                composed: true
            });
            containerElement.dispatchEvent(event);
        });

        window.addEventListener('resize', () => { this.requestTabUpdate(); });

        this.drawRuler();
        this.requestTabUpdate();

        console.log("NativeQuill initialized.");
    },

    /**
     * Ruler Logic using EXTERNAL_TAB_STOPS
     */
    drawRuler: function () {
        if (!this.quillInstance) return;

        const oldRuler = this.quillInstance.container.parentNode.querySelector('.native-quill-ruler');
        if (oldRuler) oldRuler.remove();

        const ruler = document.createElement('div');
        ruler.className = 'native-quill-ruler';
        ruler.addEventListener('mousedown', (e) => e.preventDefault());

        // Background Click -> New Stop
        ruler.addEventListener('click', (e) => {
            if (e.target !== ruler) return;
            const newPos = e.offsetX;
            this.EXTERNAL_TAB_STOPS.push({ pos: newPos, align: 'left' });
            this.EXTERNAL_TAB_STOPS.sort((a, b) => a.pos - b.pos);
            this.drawRuler();
            this.requestTabUpdate();
        });

        // Markers
        this.EXTERNAL_TAB_STOPS.forEach((stopData, index) => {
            const pos = stopData.pos;
            let align = stopData.align || 'left';

            const tick = document.createElement('div');
            tick.className = 'ruler-tick';
            tick.style.left = pos + 'px';

            const alignText = document.createElement('span');
            alignText.className = 'ruler-align-text';
            alignText.innerText = (align === 'center') ? 'C' : (align === 'right') ? 'R' : 'L';
            tick.appendChild(alignText);

            tick.addEventListener('click', (e) => {
                e.stopPropagation();
                if (align === 'left') stopData.align = 'center';
                else if (align === 'center') stopData.align = 'right';
                else this.EXTERNAL_TAB_STOPS.splice(index, 1);

                this.drawRuler();
                this.requestTabUpdate();
            });

            ruler.appendChild(tick);
        });

        const qlContainer = this.quillInstance.container;
        qlContainer.parentNode.insertBefore(ruler, qlContainer);
    },

    handleTabPress: function (range) {
        if (!this.quillInstance) return;
        this.quillInstance.insertEmbed(range.index, 'tab', true, Quill.sources.USER);
        setTimeout(() => {
            this.quillInstance.setSelection(range.index + 1, Quill.sources.API);
        }, 1);
        this.requestTabUpdate();
    },

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
     * Core Logic for Tab Widths.
     * 1. Detects Line Wrapping (ignores stops if wrapped).
     * 2. Uses fixed 8-char width for fallback (no grid).
     */
    updateTabWidths: function() {
        if (!this.quillInstance) return;

        const MIN_TAB_WIDTH = 2;
        const editorNode = this.quillInstance.root;
        const editorRect = editorNode.getBoundingClientRect();

        // Measure 8-char width ("00000000") dynamically
        const charWidth8 = this.measureTextWidth("00000000", editorNode);
        // Fallback to 50px if font not ready
        const fixedTabWidth = charWidth8 > 0 ? charWidth8 : 50;

        const tabs = Array.from(editorNode.querySelectorAll('.ql-tab'));

        tabs.forEach(tab => {
            const tabRect = tab.getBoundingClientRect();
            const startPos = tabRect.left - editorRect.left;

            // --- Detect Line Wrap ---
            // We check if the tab is significantly lower than the start of its parent paragraph.
            // If it is, it means the line wrapped, and we should IGNORE global tab stops.
            const parentBlock = tab.closest('p, div, li, h1, h2, h3') || tab.parentElement;
            let isWrappedLine = false;

            if (parentBlock) {
                const parentRect = parentBlock.getBoundingClientRect();
                // Heuristic: If tab top is > (parent top + tab height), it's likely on line 2+
                // Using a 1.2 multiplier to be safe against slight misalignments/padding.
                if ((tabRect.top - parentRect.top) > (tabRect.height * 1.2)) {
                    isWrappedLine = true;
                }
            }
            // ------------------------

            // Measure Content (Look Ahead)
            let contentWidth = 0;
            let nextNode = tab.nextSibling;
            while (nextNode) {
                if (nextNode.classList && (nextNode.classList.contains('ql-tab') || nextNode.classList.contains('ql-soft-break'))) break;
                if (nextNode.tagName && ['P', 'DIV', 'LI', 'H1'].includes(nextNode.tagName)) break;

                if (nextNode.nodeType === 3) contentWidth += this.measureTextWidth(nextNode.nodeValue, tab.parentNode);
                else if (nextNode.innerText) contentWidth += this.measureTextWidth(nextNode.innerText, nextNode);
                nextNode = nextNode.nextSibling;
            }

            // Find Tab Stop
            let targetStop = null;

            // Only look for global stops if we are NOT on a wrapped line
            if (!isWrappedLine) {
                targetStop = this.EXTERNAL_TAB_STOPS.find(stop => stop.pos > (startPos + MIN_TAB_WIDTH));
            }

            let widthNeeded = 0;

            if (targetStop) {
                // Scenario A: Valid Tab Stop found (and not wrapped)
                const stopPos = targetStop.pos;
                const alignment = targetStop.align || 'left';
                const rawDistance = stopPos - startPos;

                if (alignment === 'right') widthNeeded = rawDistance - contentWidth;
                else if (alignment === 'center') widthNeeded = rawDistance - (contentWidth / 2);
                else widthNeeded = rawDistance;

            } else {
                // Scenario B: No Tab Stop OR Line Wrapped
                // Rule: "A tab outside of tab stops should be fixed as 8 characters defined"
                // No grid snapping, just add fixed width.
                widthNeeded = fixedTabWidth;
            }

            if (widthNeeded < MIN_TAB_WIDTH) widthNeeded = MIN_TAB_WIDTH;

            if (tab.style.width !== (widthNeeded + 'px')) {
                tab.style.width = widthNeeded + 'px';
            }
        });
    },

    measureTextWidth: function(text, referenceNode) {
        if (!text) return 0;
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
        const computedStyle = window.getComputedStyle(referenceNode);
        measureSpan.style.fontFamily = computedStyle.fontFamily;
        measureSpan.style.fontSize = computedStyle.fontSize;
        measureSpan.style.fontWeight = computedStyle.fontWeight;
        measureSpan.style.fontStyle = computedStyle.fontStyle;
        measureSpan.style.letterSpacing = computedStyle.letterSpacing;
        measureSpan.textContent = text;
        return measureSpan.getBoundingClientRect().width;
    }
};