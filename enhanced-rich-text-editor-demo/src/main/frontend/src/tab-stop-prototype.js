import Quill from 'quill';
import 'quill/dist/quill.snow.css';

const Embed = Quill.import('blots/embed');
const Parchment = Quill.import('parchment');

/**
 * 1. Custom Blot for Tab Stops.
 */
class TabBlot extends Embed {
    static create(value) {
        let node = super.create();
        node.setAttribute('contenteditable', 'false');
        node.innerText = '\u200B'; // Zero Width Space

        // Smart Cursor Placement (Left/Right click detection)
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
 * 2. Custom Attribute for Tab Stops (Paragraph Level).
 * FIX: Robust handling of String vs Object to ensure immediate Delta updates.
 */
class TabStopAttribute extends Parchment.Attributor.Attribute {
    add(node, value) {
        // Quill logic check:
        // Sometimes Quill passes the already stringified value (from Delta import).
        // Sometimes it passes the raw object (from our formatLine call).
        if (typeof value === 'object') {
            value = JSON.stringify(value);
        }
        super.add(node, value);
    }

    value(node) {
        const val = super.value(node);
        // Important: Return undefined if empty, so Quill knows "attribute is removed".
        if (!val) return undefined;
        try {
            return JSON.parse(val);
        } catch (e) {
            return undefined;
        }
    }
}
const TabStops = new TabStopAttribute('tab-stops', 'data-tab-stops', {
    scope: Parchment.Scope.BLOCK
});

/**
 * 3. Soft Break Blot
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
 * 4. Main Logic
 */
window._nativeQuill = {
    DEFAULT_TAB_SIZE: 50,
    quillInstance: null,
    updateTicking: false,
    lastKnownRange: null,

    init: function (containerElement, initialValue) {
        // Register formats
        Quill.register(TabBlot);
        Quill.register(TabStops, true);
        Quill.register(SoftBreakBlot);

        this.quillInstance = new Quill(containerElement, {
            theme: 'snow',
            modules: {
                keyboard: {
                    bindings: {
                        // TAB Key
                        tab: {
                            key: 9,
                            handler: (range) => {
                                this.handleTabPress(range);
                                return false;
                            }
                        },
                        // SHIFT + ENTER (Soft Break)
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
                        // BACKSPACE HANDLER (Fixes Merge Issues & Ctrl+Backspace)
                        'backspace': {
                            key: 8,
                            handler: function(range, context) {
                                // 1. If selection is not empty, allow default delete
                                if (range.length > 0) return true;

                                // 2. Check if we are at the start of a line
                                const quill = this.quill;
                                const [currentLine, offset] = quill.getLine(range.index);

                                if (offset === 0 && range.index > 0) {
                                    // We are merging with previous line.
                                    // Find previous line to see if we need to rescue its tabs.
                                    const [prevLine] = quill.getLine(range.index - 1);

                                    if (prevLine) {
                                        // Capture tabs from previous line BEFORE merge
                                        const prevTabsAttr = prevLine.domNode.getAttribute('data-tab-stops');

                                        // Allow the default Backspace to happen (browser merge)
                                        setTimeout(() => {
                                            // Restore tabs if they existed on the target line
                                            if (prevTabsAttr) {
                                                try {
                                                    const stops = JSON.parse(prevTabsAttr);
                                                    // Re-apply to the new merged line
                                                    const newRange = quill.getSelection();
                                                    if (newRange) {
                                                        quill.formatLine(newRange.index, 0, 'tab-stops', stops, Quill.sources.USER);

                                                        // Update UI
                                                        if (window._nativeQuill) {
                                                            window._nativeQuill.drawRuler();
                                                            window._nativeQuill.requestTabUpdate();
                                                        }
                                                    }
                                                } catch(e) { console.error(e); }
                                            }
                                        }, 0);
                                        return true; // Let Quill/Browser do the delete
                                    }
                                }
                                return true; // Default behavior for other cases
                            }
                        },
                        // ENTER
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

        // --- Event Listeners ---

        // Selection Change
        this.quillInstance.on('selection-change', (range) => {
            if (range) {
                this.lastKnownRange = range;
                this.drawRuler();
            }
        });

        // Text Change
        this.quillInstance.on('text-change', (delta, oldDelta, source) => {
            this.requestTabUpdate();
            this.drawRuler();

            // Dispatch Change Event for Frameworks (Vaadin etc)
            const currentContent = JSON.stringify(this.quillInstance.getContents());
            const event = new CustomEvent('change', {
                detail: { value: currentContent },
                bubbles: true,
                composed: true
            });
            containerElement.dispatchEvent(event);
        });

        window.addEventListener('resize', () => { this.requestTabUpdate(); });

        // Init Draw
        this.drawRuler();
        this.requestTabUpdate();

        // Focus Tracking
        this.quillInstance.root.addEventListener('focus', () => {
            // Optional: Track focus state
        });

        console.log("NativeQuill initialized.");
    },

    /**
     * Helper: Gets tab stops from DOM directly (Robust)
     */
    getCurrentTabStops: function() {
        let range = this.quillInstance.getSelection({ focus: false });
        if (!range) range = this.lastKnownRange;
        if (!range) return [];

        const [lineBlot] = this.quillInstance.getLine(range.index);
        if (!lineBlot || !lineBlot.domNode) return [];

        const attrValue = lineBlot.domNode.getAttribute('data-tab-stops');
        try { return attrValue ? JSON.parse(attrValue) : []; } catch (e) { return []; }
    },

    /**
     * Helper: Writes modified tab stops back to the Delta.
     * FIX: Uses direct Blot formatting to ensure attributes stick to the Block (\n).
     */
    applyTabStopsToSelection: function(newStops) {
        let range = this.quillInstance.getSelection({ focus: false });
        if (!range) range = this.lastKnownRange;

        if (range) {
            // 1. Get the Line Blot (The Block container) directly
            const [line, offset] = this.quillInstance.getLine(range.index);

            if (line) {
                console.log("Applying Tabs to Block Blot:", newStops);

                // 2. Apply format directly to the Blot.
                // This bypasses index calculations and forces the attribute onto the Block.
                // It results in the DOM attribute 'data-tab-stops' being set immediately.
                line.format('tab-stops', newStops, Quill.sources.USER);

                // 3. Force Delta synchronization
                // We call update() to make sure Quill's internal Delta reflects the DOM change.
                this.quillInstance.update(Quill.sources.USER);

                // 4. Update UI
                this.drawRuler();
                this.requestTabUpdate();

                // 5. Dispatch Change Event manually
                const currentContent = JSON.stringify(this.quillInstance.getContents());
                const event = new CustomEvent('change', {
                    detail: { value: currentContent },
                    bubbles: true,
                    composed: true
                });
                this.quillInstance.root.dispatchEvent(event);
            }
        } else {
            console.warn("NativeQuill: No selection found, cannot save tab stops.");
        }
    },

    /**
     * Ruler Logic
     */
    drawRuler: function () {
        if (!this.quillInstance) return;

        let currentStops = JSON.parse(JSON.stringify(this.getCurrentTabStops()));

        const oldRuler = this.quillInstance.container.parentNode.querySelector('.native-quill-ruler');
        if (oldRuler) oldRuler.remove();

        const ruler = document.createElement('div');
        ruler.className = 'native-quill-ruler';
        ruler.addEventListener('mousedown', (e) => e.preventDefault());

        // Background Click -> New Tab
        ruler.addEventListener('click', (e) => {
            if (e.target.closest('.ruler-tick')) return;
            const newPos = e.offsetX;
            currentStops.push({ pos: newPos, align: 'left' });
            currentStops.sort((a, b) => a.pos - b.pos);
            this.applyTabStopsToSelection(currentStops);
        });

        // Markers
        currentStops.forEach((stopData, index) => {
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
                else currentStops.splice(index, 1);

                this.applyTabStopsToSelection(currentStops);
            });

            ruler.appendChild(tick);
        });

        const qlContainer = this.quillInstance.container;
        qlContainer.parentNode.insertBefore(ruler, qlContainer);
    },

    /**
     * Tab Key Handler
     */
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
     * Update Widths Logic
     */
    updateTabWidths: function() {
        if (!this.quillInstance) return;
        const MIN_TAB_WIDTH = 2;
        const editorNode = this.quillInstance.root;
        const editorRect = editorNode.getBoundingClientRect();
        const tabs = Array.from(editorNode.querySelectorAll('.ql-tab'));

        tabs.forEach(tab => {
            const parentBlock = tab.closest('[data-tab-stops]');
            let localStops = [];
            if (parentBlock && parentBlock.getAttribute('data-tab-stops')) {
                try { localStops = JSON.parse(parentBlock.getAttribute('data-tab-stops')); } catch(e) {}
            }

            const tabRect = tab.getBoundingClientRect();
            const startPos = tabRect.left - editorRect.left;

            let contentWidth = 0;
            let nextNode = tab.nextSibling;
            while (nextNode) {
                if (nextNode.classList && (nextNode.classList.contains('ql-tab') || nextNode.classList.contains('ql-soft-break'))) break;
                if (nextNode.tagName && ['P', 'DIV', 'LI', 'H1'].includes(nextNode.tagName)) break;

                if (nextNode.nodeType === 3) contentWidth += this.measureTextWidth(nextNode.nodeValue, tab.parentNode);
                else if (nextNode.innerText) contentWidth += this.measureTextWidth(nextNode.innerText, nextNode);
                nextNode = nextNode.nextSibling;
            }

            let targetStop = localStops.find(stop => stop.pos > (startPos + MIN_TAB_WIDTH));
            let stopPos = 0;
            let alignment = 'left';

            if (!targetStop) {
                let potentialPos = Math.ceil(startPos / this.DEFAULT_TAB_SIZE) * this.DEFAULT_TAB_SIZE;
                if (potentialPos - startPos < MIN_TAB_WIDTH) potentialPos += this.DEFAULT_TAB_SIZE;
                stopPos = potentialPos;
            } else {
                stopPos = targetStop.pos;
                alignment = targetStop.align || 'left';
            }

            let widthNeeded = (stopPos - startPos);
            if (alignment === 'right') widthNeeded -= contentWidth;
            else if (alignment === 'center') widthNeeded -= (contentWidth / 2);
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