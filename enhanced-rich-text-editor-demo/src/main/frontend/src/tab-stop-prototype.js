import Quill from 'quill';
import 'quill/dist/quill.snow.css';

const Embed = Quill.import('blots/embed');

/**
 * Constants
 */
const CONSTANTS = {
    WRAP_DETECTION_MULTIPLIER: 0.8,
    DEFAULT_TAB_CHARS: 8,
    MIN_TAB_WIDTH: 2,
    FIXED_TAB_FALLBACK: 50,
    UPDATE_DEBOUNCE_MS: 16, // ~1 frame
    ZERO_WIDTH_SPACE: '\u200B'
};

/**
 * 1. Custom Blot for Tab Stops (with cleanup)
 */
class TabBlot extends Embed {
    static create(value) {
        let node = super.create();
        node.setAttribute('contenteditable', 'false');
        node.innerText = CONSTANTS.ZERO_WIDTH_SPACE;

        // Smart Cursor Placement - store handler for cleanup
        const mouseHandler = (e) => {
            e.preventDefault();
            e.stopPropagation();

            const quill = window._nativeQuill?.quillInstance;
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
        };

        node._mouseHandler = mouseHandler;
        node.addEventListener('mousedown', mouseHandler);

        return node;
    }

    detach() {
        if (this.domNode._mouseHandler) {
            this.domNode.removeEventListener('mousedown', this.domNode._mouseHandler);
            delete this.domNode._mouseHandler;
        }
        super.detach();
    }
}
TabBlot.blotName = 'tab';
TabBlot.tagName = 'span';
TabBlot.className = 'ql-tab';

/**
 * 2. Soft Break Blot (with cleanup)
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
 * 3. Main Logic (Optimized)
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

    // Optimization: Cache and reusable resources
    _textWidthCache: new Map(),
    _measureSpan: null,
    _updateTimeout: null,
    _resizeHandler: null,
    _textChangeHandler: null,

    init: function (containerElement, initialValue) {
        // Register blots once
        if (!this.isBlotRegistered) {
            Quill.register(TabBlot);
            Quill.register(SoftBreakBlot);
            this.isBlotRegistered = true;
        }

        // Create reusable measure span
        this._createMeasureSpan();

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
                            key: 13,
                            shiftKey: true,
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
                        'enter': {
                            key: 13,
                            shiftKey: false,
                            handler: function() { return true; }
                        }
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
            try {
                if (typeof initialValue === 'string') delta = JSON.parse(initialValue);
            } catch(e){}
            this.quillInstance.setContents(delta);
        }

        this.lastCommittedDelta = JSON.stringify(this.quillInstance.getContents());

        // Event Listeners (stored for cleanup)
        this._textChangeHandler = () => {
            this.requestTabUpdate();
            const currentContent = JSON.stringify(this.quillInstance.getContents());
            const event = new CustomEvent('change', {
                detail: { value: currentContent },
                bubbles: true,
                composed: true
            });
            containerElement.dispatchEvent(event);
        };

        this._resizeHandler = () => {
            this._textWidthCache.clear(); // Clear cache on resize
            this.requestTabUpdate();
        };

        this.quillInstance.on('text-change', this._textChangeHandler);
        window.addEventListener('resize', this._resizeHandler);

        this.drawRuler();
        this.requestTabUpdate();

        console.log("NativeQuill initialized (optimized).");
    },

    /**
     * Cleanup method to prevent memory leaks
     */
    destroy: function() {
        // Clear timeout
        if (this._updateTimeout) {
            clearTimeout(this._updateTimeout);
        }

        // Remove event listeners
        if (this._resizeHandler) {
            window.removeEventListener('resize', this._resizeHandler);
        }

        if (this.quillInstance && this._textChangeHandler) {
            this.quillInstance.off('text-change', this._textChangeHandler);
        }

        // Clean up measure span
        if (this._measureSpan && this._measureSpan.parentNode) {
            this._measureSpan.parentNode.removeChild(this._measureSpan);
        }

        // Clear cache
        this._textWidthCache.clear();

        // Remove ruler
        if (this.quillInstance) {
            const oldRuler = this.quillInstance.container.parentNode?.querySelector('.native-quill-ruler');
            if (oldRuler) oldRuler.remove();
        }

        console.log("NativeQuill destroyed.");
    },

    /**
     * Create reusable measure span (optimization)
     */
    _createMeasureSpan: function() {
        if (this._measureSpan) return;

        this._measureSpan = document.createElement('span');
        this._measureSpan.style.cssText = 'visibility:hidden;position:absolute;white-space:pre;left:-9999px;top:-9999px';
        document.body.appendChild(this._measureSpan);
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

    /**
     * Optimized: Debounced requestAnimationFrame
     */
    requestTabUpdate: function () {
        clearTimeout(this._updateTimeout);
        this._updateTimeout = setTimeout(() => {
            if (!this.updateTicking) {
                window.requestAnimationFrame(() => {
                    this.updateTabWidths();
                    this.updateTicking = false;
                });
                this.updateTicking = true;
            }
        }, CONSTANTS.UPDATE_DEBOUNCE_MS);
    },

    /**
     * Core Logic for Tab Widths (OPTIMIZED)
     * - Batch DOM measurements
     * - Cached text width calculations
     * - Improved line wrap detection
     */
    updateTabWidths: function() {
        if (!this.quillInstance) return;

        const editorNode = this.quillInstance.root;
        const tabs = Array.from(editorNode.querySelectorAll('.ql-tab'));

        if (tabs.length === 0) return;

        // OPTIMIZATION: Batch all measurements first (avoid layout thrashing)
        const editorRect = editorNode.getBoundingClientRect();
        const charWidth8 = this.measureTextWidth("0".repeat(CONSTANTS.DEFAULT_TAB_CHARS), editorNode);
        const fixedTabWidth = charWidth8 > 0 ? charWidth8 : CONSTANTS.FIXED_TAB_FALLBACK;

        const measurements = tabs.map(tab => {
            const tabRect = tab.getBoundingClientRect();
            const parentBlock = tab.closest('p, div, li, h1, h2, h3') || tab.parentElement;
            const parentRect = parentBlock ? parentBlock.getBoundingClientRect() : null;

            return {
                tab,
                rect: tabRect,
                parentRect,
                startPos: tabRect.left - editorRect.left
            };
        });

        // OPTIMIZATION: Then apply all updates (single layout pass)
        measurements.forEach(({ tab, rect, parentRect, startPos }) => {
            // Improved Line Wrap Detection
            const isWrappedLine = this._isWrappedLine(tab, rect, parentRect);

            // Measure Content (Look Ahead) - with caching
            const contentWidth = this._measureContentWidth(tab);

            // Find Tab Stop (only if not wrapped)
            let targetStop = null;
            if (!isWrappedLine) {
                targetStop = this.EXTERNAL_TAB_STOPS.find(
                    stop => stop.pos > (startPos + CONSTANTS.MIN_TAB_WIDTH)
                );
            }

            let widthNeeded = 0;

            if (targetStop) {
                // Scenario A: Valid Tab Stop found
                const stopPos = targetStop.pos;
                const alignment = targetStop.align || 'left';
                const rawDistance = stopPos - startPos;

                if (alignment === 'right') {
                    widthNeeded = rawDistance - contentWidth;
                } else if (alignment === 'center') {
                    widthNeeded = rawDistance - (contentWidth / 2);
                } else {
                    widthNeeded = rawDistance;
                }
            } else {
                // Scenario B: No Tab Stop OR Line Wrapped
                widthNeeded = fixedTabWidth;
            }

            if (widthNeeded < CONSTANTS.MIN_TAB_WIDTH) {
                widthNeeded = CONSTANTS.MIN_TAB_WIDTH;
            }

            // Only update if changed (avoid unnecessary reflows)
            const newWidth = widthNeeded + 'px';
            if (tab.style.width !== newWidth) {
                tab.style.width = newWidth;
            }
        });
    },

    /**
     * Improved line wrap detection
     */
    _isWrappedLine: function(tab, tabRect, parentRect) {
        if (!parentRect) return false;

        // Check vertical offset from parent
        const verticalOffset = tabRect.top - parentRect.top;
        const threshold = tabRect.height * CONSTANTS.WRAP_DETECTION_MULTIPLIER;

        return verticalOffset > threshold;
    },

    /**
     * Measure content width with caching
     */
    _measureContentWidth: function(tab) {
        let contentWidth = 0;
        let nextNode = tab.nextSibling;

        while (nextNode) {
            if (nextNode.classList && (
                nextNode.classList.contains('ql-tab') ||
                nextNode.classList.contains('ql-soft-break')
            )) break;

            if (nextNode.tagName && ['P', 'DIV', 'LI', 'H1', 'H2', 'H3'].includes(nextNode.tagName)) break;

            if (nextNode.nodeType === 3) {
                contentWidth += this.measureTextWidth(nextNode.nodeValue, tab.parentNode);
            } else if (nextNode.innerText) {
                contentWidth += this.measureTextWidth(nextNode.innerText, nextNode);
            }

            nextNode = nextNode.nextSibling;
        }

        return contentWidth;
    },

    /**
     * OPTIMIZED: Cached text width measurement
     */
    measureTextWidth: function(text, referenceNode) {
        if (!text) return 0;

        const computedStyle = window.getComputedStyle(referenceNode);
        const cacheKey = `${text}|${computedStyle.fontFamily}|${computedStyle.fontSize}|${computedStyle.fontWeight}`;

        // Check cache first
        if (this._textWidthCache.has(cacheKey)) {
            return this._textWidthCache.get(cacheKey);
        }

        // Measure using reusable span
        const measureSpan = this._measureSpan;
        measureSpan.style.fontFamily = computedStyle.fontFamily;
        measureSpan.style.fontSize = computedStyle.fontSize;
        measureSpan.style.fontWeight = computedStyle.fontWeight;
        measureSpan.style.fontStyle = computedStyle.fontStyle;
        measureSpan.style.letterSpacing = computedStyle.letterSpacing;
        measureSpan.textContent = text;

        const width = measureSpan.getBoundingClientRect().width;

        // Cache result (limit cache size)
        if (this._textWidthCache.size > 1000) {
            const firstKey = this._textWidthCache.keys().next().value;
            this._textWidthCache.delete(firstKey);
        }
        this._textWidthCache.set(cacheKey, width);

        return width;
    }
};