import Quill from 'quill';
import 'quill/dist/quill.snow.css';

const Embed = Quill.import('blots/embed');
const Parchment = Quill.import('parchment');

/**
 * 1. Custom Blot for Tab Stops.
 */
class TabBlot extends Embed {
    static create(value) {
        const node = super.create();
        node.setAttribute('contenteditable', 'false');
        node.innerText = '\u200B'; // Zero Width Space
        return node;
    }

    attach() {
        super.attach();
        // Attach event listener only once when blot is attached
        this.domNode.addEventListener('mousedown', this.handleMouseDown);
    }

    detach() {
        this.domNode.removeEventListener('mousedown', this.handleMouseDown);
        super.detach();
    }

    handleMouseDown = (e) => {
        e.preventDefault();
        e.stopPropagation();

        const quill = window._nativeQuill?.quillInstance;
        if (!quill) return;

        const index = quill.getIndex(this);
        const rect = this.domNode.getBoundingClientRect();
        const clickX = e.clientX - rect.left;

        // Smart cursor placement
        const targetIndex = clickX < rect.width / 2 ? index : index + 1;
        quill.setSelection(targetIndex, 0, Quill.sources.USER);
    }
}
TabBlot.blotName = 'tab';
TabBlot.tagName = 'span';
TabBlot.className = 'ql-tab';

/**
 * 2. Custom Attribute for Tab Stops (Paragraph Level).
 */
class TabStopAttribute extends Parchment.Attributor.Attribute {
    add(node, value) {
        const stringValue = typeof value === 'object' ? JSON.stringify(value) : value;
        super.add(node, stringValue);
    }

    value(node) {
        const val = super.value(node);
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
        const node = super.create(value);
        node.innerHTML = '<br>';
        return node;
    }
}
SoftBreakBlot.blotName = 'soft-break';
SoftBreakBlot.tagName = 'span';
SoftBreakBlot.className = 'ql-soft-break';

/**
 * 4. Text Width Measurement Utility (Singleton)
 */
class TextMeasurer {
    constructor() {
        this.measureSpan = null;
        this.cache = new Map(); // Cache für häufige Messungen
    }

    measure(text, referenceNode) {
        if (!text) return 0;

        const computedStyle = window.getComputedStyle(referenceNode);
        const styleKey = `${computedStyle.fontFamily}-${computedStyle.fontSize}-${computedStyle.fontWeight}`;
        const cacheKey = `${styleKey}-${text}`;

        // Cache-Lookup
        if (this.cache.has(cacheKey)) {
            return this.cache.get(cacheKey);
        }

        if (!this.measureSpan) {
            this.measureSpan = document.createElement('span');
            Object.assign(this.measureSpan.style, {
                visibility: 'hidden',
                position: 'absolute',
                whiteSpace: 'pre',
                left: '-9999px'
            });
            document.body.appendChild(this.measureSpan);
        }

        Object.assign(this.measureSpan.style, {
            fontFamily: computedStyle.fontFamily,
            fontSize: computedStyle.fontSize,
            fontWeight: computedStyle.fontWeight,
            fontStyle: computedStyle.fontStyle,
            letterSpacing: computedStyle.letterSpacing
        });

        this.measureSpan.textContent = text;
        const width = this.measureSpan.getBoundingClientRect().width;

        // Cache mit Limit (max 100 Einträge)
        if (this.cache.size > 100) {
            const firstKey = this.cache.keys().next().value;
            this.cache.delete(firstKey);
        }
        this.cache.set(cacheKey, width);

        return width;
    }

    destroy() {
        if (this.measureSpan && this.measureSpan.parentNode) {
            this.measureSpan.parentNode.removeChild(this.measureSpan);
        }
        this.measureSpan = null;
        this.cache.clear();
    }
}

/**
 * 5. Main NativeQuill Class
 */
class NativeQuill {
    constructor() {
        this.DEFAULT_TAB_SIZE = 50;
        this.MIN_TAB_WIDTH = 2;
        this.quillInstance = null;
        this.updateTicking = false;
        this.lastKnownRange = null;
        this.textMeasurer = new TextMeasurer();
        this.containerElement = null;

        // Bound methods for event listeners
        this.boundHandleResize = this.handleResize.bind(this);
        this.boundHandleSelectionChange = this.handleSelectionChange.bind(this);
        this.boundHandleTextChange = this.handleTextChange.bind(this);
    }

    init(containerElement, initialValue) {
        this.containerElement = containerElement;

        // Register formats
        Quill.register(TabBlot);
        Quill.register(TabStops, true);
        Quill.register(SoftBreakBlot);

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
                        softBreak: {
                            key: 13,
                            shiftKey: true,
                            handler: this.handleSoftBreak.bind(this)
                        },
                        backspace: {
                            key: 8,
                            handler: this.handleBackspace.bind(this)
                        },
                        enter: { key: 13, shiftKey: false, handler: () => true }
                    }
                },
                toolbar: [
                    ['bold', 'italic', 'underline'],
                    [{ 'header': 1 }, { 'header': 2 }],
                    [{ 'indent': '-1' }, { 'indent': '+1' }],
                    ['clean']
                ]
            }
        });

        if (initialValue) {
            const delta = typeof initialValue === 'string'
                ? this.safeParse(initialValue)
                : initialValue;
            if (delta) this.quillInstance.setContents(delta);
        }

        // Event Listeners
        this.quillInstance.on('selection-change', this.boundHandleSelectionChange);
        this.quillInstance.on('text-change', this.boundHandleTextChange);
        window.addEventListener('resize', this.boundHandleResize);

        // Initial draw
        this.drawRuler();
        this.requestTabUpdate();

        console.log("NativeQuill initialized.");
    }

    handleResize() {
        this.requestTabUpdate();
    }

    handleSelectionChange(range) {
        if (range) {
            this.lastKnownRange = range;
            this.drawRuler();
        }
    }

    handleTextChange(delta, oldDelta, source) {
        this.requestTabUpdate();
        this.drawRuler();
        this.dispatchChangeEvent();
    }

    handleSoftBreak(range) {
        const quill = this.quillInstance;
        const [line] = quill.getLine(range.index);

        // Count leading tabs
        let leadingTabsCount = 0;
        let currentBlot = line.children.head;
        while (currentBlot && currentBlot.statics.blotName === 'tab') {
            leadingTabsCount++;
            currentBlot = currentBlot.next;
        }

        quill.insertEmbed(range.index, 'soft-break', true, Quill.sources.USER);

        // Insert tabs
        let insertPos = range.index + 1;
        for (let i = 0; i < leadingTabsCount; i++) {
            quill.insertEmbed(insertPos, 'tab', true, Quill.sources.USER);
            insertPos++;
        }

        // Deferred selection update
        requestAnimationFrame(() => {
            quill.setSelection(insertPos, Quill.sources.SILENT);
            this.requestTabUpdate();
        });

        return false;
    }

    handleBackspace(range, context) {
        if (range.length > 0) return true;

        const quill = this.quillInstance;
        const [currentLine, offset] = quill.getLine(range.index);

        if (offset === 0 && range.index > 0) {
            const [prevLine] = quill.getLine(range.index - 1);

            if (prevLine) {
                const prevTabsAttr = prevLine.domNode.getAttribute('data-tab-stops');

                requestAnimationFrame(() => {
                    if (prevTabsAttr) {
                        const stops = this.safeParse(prevTabsAttr);
                        if (stops) {
                            const newRange = quill.getSelection();
                            if (newRange) {
                                quill.formatLine(newRange.index, 0, 'tab-stops', stops, Quill.sources.USER);
                                this.drawRuler();
                                this.requestTabUpdate();
                            }
                        }
                    }
                });
            }
        }
        return true;
    }

    safeParse(str) {
        try {
            return JSON.parse(str);
        } catch (e) {
            console.error('Parse error:', e);
            return null;
        }
    }

    getCurrentTabStops() {
        let range = this.quillInstance.getSelection({ focus: false }) || this.lastKnownRange;
        if (!range) return [];

        const [lineBlot] = this.quillInstance.getLine(range.index);
        if (!lineBlot?.domNode) return [];

        const attrValue = lineBlot.domNode.getAttribute('data-tab-stops');
        return attrValue ? this.safeParse(attrValue) || [] : [];
    }

    applyTabStopsToSelection(newStops) {
        let range = this.quillInstance.getSelection({ focus: false }) || this.lastKnownRange;
        if (!range) {
            console.warn("NativeQuill: No selection found, cannot save tab stops.");
            return;
        }

        const [line] = this.quillInstance.getLine(range.index);
        if (!line) return;

        console.log("Applying Tabs to Block Blot:", newStops);

        line.format('tab-stops', newStops, Quill.sources.USER);
        this.quillInstance.update(Quill.sources.USER);

        this.drawRuler();
        this.requestTabUpdate();
        this.dispatchChangeEvent();
    }

    drawRuler() {
        if (!this.quillInstance) return;

        const currentStops = [...this.getCurrentTabStops()];

        // Remove old ruler
        const oldRuler = this.quillInstance.container.parentNode.querySelector('.native-quill-ruler');
        if (oldRuler) oldRuler.remove();

        const ruler = document.createElement('div');
        ruler.className = 'native-quill-ruler';

        // Prevent text selection
        ruler.addEventListener('mousedown', (e) => e.preventDefault());

        // Background click -> New Tab
        ruler.addEventListener('click', (e) => {
            if (e.target.closest('.ruler-tick')) return;
            currentStops.push({ pos: e.offsetX, align: 'left' });
            currentStops.sort((a, b) => a.pos - b.pos);
            this.applyTabStopsToSelection(currentStops);
        });

        // Create markers
        currentStops.forEach((stopData, index) => {
            const tick = this.createRulerTick(stopData, index, currentStops);
            ruler.appendChild(tick);
        });

        const qlContainer = this.quillInstance.container;
        qlContainer.parentNode.insertBefore(ruler, qlContainer);
    }

    createRulerTick(stopData, index, currentStops) {
        const { pos, align = 'left' } = stopData;

        const tick = document.createElement('div');
        tick.className = 'ruler-tick';
        tick.style.left = `${pos}px`;

        const alignText = document.createElement('span');
        alignText.className = 'ruler-align-text';
        alignText.innerText = align === 'center' ? 'C' : align === 'right' ? 'R' : 'L';
        tick.appendChild(alignText);

        tick.addEventListener('click', (e) => {
            e.stopPropagation();

            if (align === 'left') {
                stopData.align = 'center';
            } else if (align === 'center') {
                stopData.align = 'right';
            } else {
                currentStops.splice(index, 1);
            }

            this.applyTabStopsToSelection(currentStops);
        });

        return tick;
    }

    handleTabPress(range) {
        if (!this.quillInstance) return;
        this.quillInstance.insertEmbed(range.index, 'tab', true, Quill.sources.USER);
        requestAnimationFrame(() => {
            this.quillInstance.setSelection(range.index + 1, Quill.sources.API);
        });
        this.requestTabUpdate();
    }

    requestTabUpdate() {
        if (!this.updateTicking) {
            requestAnimationFrame(() => {
                this.updateTabWidths();
                this.updateTicking = false;
            });
            this.updateTicking = true;
        }
    }

    updateTabWidths() {
        if (!this.quillInstance) return;

        const editorNode = this.quillInstance.root;
        const editorRect = editorNode.getBoundingClientRect();
        const tabs = editorNode.querySelectorAll('.ql-tab');

        tabs.forEach(tab => {
            const parentBlock = tab.closest('[data-tab-stops]');
            const localStops = parentBlock
                ? this.safeParse(parentBlock.getAttribute('data-tab-stops')) || []
                : [];

            const tabRect = tab.getBoundingClientRect();
            const startPos = tabRect.left - editorRect.left;

            const contentWidth = this.measureContentAfterTab(tab);

            const { stopPos, alignment } = this.findTargetStop(localStops, startPos);

            let widthNeeded = this.calculateTabWidth(stopPos, startPos, alignment, contentWidth);

            // Apply width only if changed
            const newWidth = `${widthNeeded}px`;
            if (tab.style.width !== newWidth) {
                tab.style.width = newWidth;
            }
        });
    }

    measureContentAfterTab(tab) {
        let contentWidth = 0;
        let nextNode = tab.nextSibling;

        while (nextNode) {
            if (nextNode.classList?.contains('ql-tab') ||
                nextNode.classList?.contains('ql-soft-break') ||
                ['P', 'DIV', 'LI', 'H1'].includes(nextNode.tagName)) {
                break;
            }

            if (nextNode.nodeType === 3) {
                contentWidth += this.textMeasurer.measure(nextNode.nodeValue, tab.parentNode);
            } else if (nextNode.innerText) {
                contentWidth += this.textMeasurer.measure(nextNode.innerText, nextNode);
            }
            nextNode = nextNode.nextSibling;
        }

        return contentWidth;
    }

    findTargetStop(localStops, startPos) {
        const targetStop = localStops.find(stop => stop.pos > startPos + this.MIN_TAB_WIDTH);

        if (!targetStop) {
            let potentialPos = Math.ceil(startPos / this.DEFAULT_TAB_SIZE) * this.DEFAULT_TAB_SIZE;
            if (potentialPos - startPos < this.MIN_TAB_WIDTH) {
                potentialPos += this.DEFAULT_TAB_SIZE;
            }
            return { stopPos: potentialPos, alignment: 'left' };
        }

        return { stopPos: targetStop.pos, alignment: targetStop.align || 'left' };
    }

    calculateTabWidth(stopPos, startPos, alignment, contentWidth) {
        let widthNeeded = stopPos - startPos;

        if (alignment === 'right') {
            widthNeeded -= contentWidth;
        } else if (alignment === 'center') {
            widthNeeded -= contentWidth / 2;
        }

        return Math.max(widthNeeded, this.MIN_TAB_WIDTH);
    }

    dispatchChangeEvent() {
        if (!this.containerElement) return;

        const currentContent = JSON.stringify(this.quillInstance.getContents());
        const event = new CustomEvent('change', {
            detail: { value: currentContent },
            bubbles: true,
            composed: true
        });
        this.containerElement.dispatchEvent(event);
    }

    destroy() {
        // Remove event listeners
        if (this.quillInstance) {
            this.quillInstance.off('selection-change', this.boundHandleSelectionChange);
            this.quillInstance.off('text-change', this.boundHandleTextChange);
        }
        window.removeEventListener('resize', this.boundHandleResize);

        // Cleanup text measurer
        this.textMeasurer.destroy();

        // Remove ruler
        const ruler = this.quillInstance?.container.parentNode.querySelector('.native-quill-ruler');
        if (ruler) ruler.remove();

        // Clear references
        this.quillInstance = null;
        this.containerElement = null;
        this.lastKnownRange = null;

        console.log("NativeQuill destroyed.");
    }
}

// Export as singleton
window._nativeQuill = new NativeQuill();