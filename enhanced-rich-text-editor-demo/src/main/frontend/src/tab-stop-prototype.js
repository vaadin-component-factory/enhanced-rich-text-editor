import Quill from 'quill';
import 'quill/dist/quill.snow.css';

const Embed = Quill.import('blots/embed');

/**
 * Constants for TabStop behavior
 */
const CONSTANTS = {
    /** Threshold multiplier for line wrap detection (0.8 = 80% of line height) */
    WRAP_DETECTION_MULTIPLIER: 0.8,
    /** Standard-Tab-Breite in Zeichen (wie MS Word) */
    DEFAULT_TAB_CHARS: 8,
    /** Minimale Tab-Breite in Pixeln (verhindert Verschwinden) */
    MIN_TAB_WIDTH: 2,
    /** Fallback width when character measurement fails */
    FIXED_TAB_FALLBACK: 50,
    /** Zero-Width-Space als Tab-Inhalt */
    ZERO_WIDTH_SPACE: '\u200B'
};

/**
 * Block elements that break content measurement
 */
const BLOCK_ELEMENTS = ['P', 'DIV', 'LI', 'H1', 'H2', 'H3', 'H4', 'H5', 'H6',
                        'BLOCKQUOTE', 'PRE', 'OL', 'UL', 'TABLE', 'TR', 'TD', 'TH'];
const BLOCK_SELECTOR = BLOCK_ELEMENTS.map(t => t.toLowerCase()).join(', ');

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
    isBlotRegistered: false,
    lastCommittedDelta: null,

    // Optimization: Cache and reusable resources
    _textWidthCache: new Map(),
    _styleCache: new WeakMap(),
    _cacheMaxSize: 500,
    _measureSpan: null,
    _updateTimeout: null,
    _rafId: null,
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
                                const [line, offset] = quill.getLine(range.index);
                                const lineStartIndex = quill.getIndex(line);

                                // Find boundaries of the VISUAL line (between soft-breaks)
                                let currentBlot = line.children.head;
                                let posInLine = 0;
                                let visualLineStart = 0;  // Position after the last soft-break BEFORE cursor
                                let visualLineEnd = line.length() - 1;  // Block end (without \n)

                                // Traverse all blots to find visual line boundaries
                                while (currentBlot) {
                                    if (currentBlot.statics.blotName === 'soft-break') {
                                        if (posInLine < offset) {
                                            // Soft-break BEFORE cursor -> start of visual line
                                            visualLineStart = posInLine + 1;
                                        } else {
                                            // First soft-break AFTER cursor -> end of visual line
                                            visualLineEnd = posInLine;
                                            break;
                                        }
                                    }
                                    posInLine += currentBlot.length();
                                    currentBlot = currentBlot.next;
                                }

                                // Count tabs between visualLineStart and offset (cursor position)
                                currentBlot = line.children.head;
                                posInLine = 0;
                                let tabsBeforeCursor = 0;

                                while (currentBlot && posInLine < offset) {
                                    if (posInLine >= visualLineStart && currentBlot.statics.blotName === 'tab') {
                                        tabsBeforeCursor++;
                                    }
                                    posInLine += currentBlot.length();
                                    currentBlot = currentBlot.next;
                                }

                                // Insert soft-break at end of VISUAL line (not at cursor position!)
                                // This keeps the current visual line intact
                                const insertIndex = lineStartIndex + visualLineEnd;
                                quill.insertEmbed(insertIndex, 'soft-break', true, Quill.sources.USER);

                                // Limit tabs to copy: max = number of defined tabstops
                                // If cursor is at an "overflow" tab position, only copy up to tabstop count
                                const maxTabstops = window._nativeQuill.EXTERNAL_TAB_STOPS.length;
                                const tabsToCopy = Math.min(tabsBeforeCursor, maxTabstops);

                                // Insert tabs after the soft-break
                                let insertPos = insertIndex + 1;
                                for (let i = 0; i < tabsToCopy; i++) {
                                    quill.insertEmbed(insertPos, 'tab', true, Quill.sources.USER);
                                    insertPos++;
                                }

                                // Use Promise.resolve() for next microtask instead of setTimeout(1)
                                Promise.resolve().then(() => {
                                    quill.setSelection(insertPos, Quill.sources.SILENT);
                                    window._nativeQuill.requestTabUpdate();
                                });
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
            if (typeof initialValue === 'string') {
                try {
                    delta = JSON.parse(initialValue);
                } catch (e) {
                    console.error('Invalid initial value JSON:', e);
                    delta = { ops: [] };
                }
            }
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
            // Invalidate caches on resize
            this._textWidthCache.clear();
            this._styleCache = new WeakMap();
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
        // Clear timeout and RAF
        if (this._updateTimeout) {
            clearTimeout(this._updateTimeout);
            this._updateTimeout = null;
        }
        if (this._rafId) {
            cancelAnimationFrame(this._rafId);
            this._rafId = null;
        }

        // Remove event listeners
        if (this._resizeHandler) {
            window.removeEventListener('resize', this._resizeHandler);
            this._resizeHandler = null;
        }

        if (this.quillInstance && this._textChangeHandler) {
            this.quillInstance.off('text-change', this._textChangeHandler);
            this._textChangeHandler = null;
        }

        // Clean up measure span
        if (this._measureSpan && this._measureSpan.parentNode) {
            this._measureSpan.parentNode.removeChild(this._measureSpan);
        }
        this._measureSpan = null;

        // Clear caches
        this._textWidthCache.clear();
        this._styleCache = new WeakMap();

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
     * Uses event delegation to avoid memory leaks from individual tick listeners
     */
    drawRuler: function () {
        if (!this.quillInstance) return;

        const oldRuler = this.quillInstance.container.parentNode.querySelector('.native-quill-ruler');
        if (oldRuler) oldRuler.remove();

        const ruler = document.createElement('div');
        ruler.className = 'native-quill-ruler';
        ruler.addEventListener('mousedown', (e) => e.preventDefault());

        // Event delegation: single click handler for ruler and all ticks
        ruler.addEventListener('click', (e) => {
            const tick = e.target.closest('.ruler-tick');

            if (tick) {
                // Click on existing tab stop marker
                e.stopPropagation();
                const stopIndex = parseInt(tick.dataset.stopIndex, 10);
                const stopData = this.EXTERNAL_TAB_STOPS[stopIndex];

                if (stopData) {
                    const align = stopData.align || 'left';
                    if (align === 'left') {
                        stopData.align = 'center';
                    } else if (align === 'center') {
                        stopData.align = 'right';
                    } else {
                        // Fix: Use indexOf to find correct index instead of captured index
                        const idx = this.EXTERNAL_TAB_STOPS.indexOf(stopData);
                        if (idx !== -1) {
                            this.EXTERNAL_TAB_STOPS.splice(idx, 1);
                        }
                    }
                    this.drawRuler();
                    this.requestTabUpdate();
                }
            } else if (e.target === ruler) {
                // Click on ruler background -> New Stop
                // Account for scroll position
                const rulerRect = ruler.getBoundingClientRect();
                const newPos = e.clientX - rulerRect.left + ruler.scrollLeft;

                this.EXTERNAL_TAB_STOPS.push({ pos: newPos, align: 'left' });
                this.EXTERNAL_TAB_STOPS.sort((a, b) => a.pos - b.pos);
                this.drawRuler();
                this.requestTabUpdate();
            }
        });

        // Create markers (no individual event listeners needed)
        this.EXTERNAL_TAB_STOPS.forEach((stopData, index) => {
            const pos = stopData.pos;
            const align = stopData.align || 'left';

            const tick = document.createElement('div');
            tick.className = 'ruler-tick';
            tick.style.left = pos + 'px';
            tick.dataset.stopIndex = index;  // Store index for event delegation

            const alignText = document.createElement('span');
            alignText.className = 'ruler-align-text';
            alignText.innerText = (align === 'center') ? 'C' : (align === 'right') ? 'R' : 'L';
            tick.appendChild(alignText);

            ruler.appendChild(tick);
        });

        const qlContainer = this.quillInstance.container;
        qlContainer.parentNode.insertBefore(ruler, qlContainer);
    },

    handleTabPress: function (range) {
        if (!this.quillInstance) return;
        this.quillInstance.insertEmbed(range.index, 'tab', true, Quill.sources.USER);
        // Use Promise.resolve() for next microtask instead of setTimeout(1)
        Promise.resolve().then(() => {
            this.quillInstance.setSelection(range.index + 1, Quill.sources.API);
        });
        this.requestTabUpdate();
    },

    /**
     * Optimized: Pure RAF-based update (fastest response)
     * Coalesces multiple calls into single frame
     */
    requestTabUpdate: function () {
        if (this._rafId) return;  // Already scheduled

        this._rafId = requestAnimationFrame(() => {
            this.updateTabWidths();
            this._rafId = null;
        });
    },

    /**
     * Core Logic for Tab Widths (ITERATIVE)
     * Processes tabs one by one: measure position → calculate width → set width → next
     * This ensures each tab's position is measured AFTER previous tabs have been sized.
     */
    updateTabWidths: function() {
        if (!this.quillInstance) return;

        const editorNode = this.quillInstance.root;
        const tabs = Array.from(editorNode.querySelectorAll('.ql-tab'));

        if (tabs.length === 0) return;

        // These can be cached as they don't change during iteration
        const charWidth8 = this.measureTextWidth("0".repeat(CONSTANTS.DEFAULT_TAB_CHARS), editorNode);
        const fixedTabWidth = charWidth8 > 0 ? charWidth8 : CONSTANTS.FIXED_TAB_FALLBACK;

        // Process each tab iteratively - measure → calculate → set → next
        tabs.forEach(tab => {
            // Measure position AFTER previous tabs have been sized
            const editorRect = editorNode.getBoundingClientRect();
            const tabRect = tab.getBoundingClientRect();
            const parentBlock = tab.closest(BLOCK_SELECTOR) || tab.parentElement;
            const parentRect = parentBlock ? parentBlock.getBoundingClientRect() : null;
            const startPos = tabRect.left - editorRect.left;

            // Line wrap detection
            const isWrappedLine = this._isWrappedLine(tab, tabRect, parentBlock, parentRect);

            // Mark wrapped tabs with CSS class for visual indicator
            if (isWrappedLine) {
                tab.classList.add('ql-auto-wrap');
            } else {
                tab.classList.remove('ql-auto-wrap');
            }

            // Measure content width
            const contentWidth = this._measureContentWidth(tab);

            // Find next tab stop (only if not wrapped)
            let targetStop = null;
            if (!isWrappedLine) {
                targetStop = this.EXTERNAL_TAB_STOPS.find(
                    stop => stop.pos > (startPos + CONSTANTS.MIN_TAB_WIDTH)
                );
            }

            let widthNeeded = 0;

            if (targetStop) {
                // Valid tab stop found - calculate width based on alignment
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
                // No tab stop or wrapped line - use fixed width
                widthNeeded = fixedTabWidth;
            }

            if (widthNeeded < CONSTANTS.MIN_TAB_WIDTH) {
                widthNeeded = CONSTANTS.MIN_TAB_WIDTH;
            }

            // Set width immediately (affects subsequent tab positions)
            tab.style.width = widthNeeded + 'px';
        });
    },

    /**
     * Improved line wrap detection
     * Uses parent line-height instead of tab height (which is 0 due to CSS font-size: 0)
     */
    _isWrappedLine: function(tab, tabRect, parentBlock, parentRect) {
        if (!parentRect || !parentBlock) return false;

        // Check if a soft-break exists before this tab (in the same visual line)
        // If yes, it's NOT an automatic wrap - tabs should use tabstops
        let prevSibling = tab.previousSibling;
        while (prevSibling) {
            if (prevSibling.classList && prevSibling.classList.contains('ql-soft-break')) {
                // Soft-break found - check if it's on the same vertical position
                const softBreakRect = prevSibling.getBoundingClientRect();
                if (Math.abs(softBreakRect.top - tabRect.top) < 5) {
                    return false;  // Soft-break on same line -> not an automatic wrap
                }
            }
            prevSibling = prevSibling.previousSibling;
        }

        // Use parent line-height instead of tab height (tab has font-size: 0)
        const computedStyle = this._getCachedStyle(parentBlock);
        const lineHeight = parseFloat(computedStyle.lineHeight) ||
                           parseFloat(computedStyle.fontSize) * 1.2;

        // Check vertical offset from parent
        const verticalOffset = tabRect.top - parentRect.top;
        const threshold = lineHeight * CONSTANTS.WRAP_DETECTION_MULTIPLIER;

        return verticalOffset > threshold;
    },

    /**
     * Get cached computed style for an element
     */
    _getCachedStyle: function(element) {
        if (!this._styleCache.has(element)) {
            this._styleCache.set(element, window.getComputedStyle(element));
        }
        return this._styleCache.get(element);
    },

    /**
     * Measure content width with caching
     * Recursively measures text nodes to handle nested inline styles
     */
    _measureContentWidth: function(tab) {
        let contentWidth = 0;
        let nextNode = tab.nextSibling;

        while (nextNode) {
            if (this._isBreakingNode(nextNode)) break;

            // Recursively collect and measure all text nodes
            const textNodes = this._getTextNodes(nextNode);
            for (const { text, element } of textNodes) {
                contentWidth += this.measureTextWidth(text, element);
            }

            nextNode = nextNode.nextSibling;
        }

        return contentWidth;
    },

    /**
     * Check if a node breaks the content measurement
     */
    _isBreakingNode: function(node) {
        if (!node) return true;

        // Check for tab or soft-break classes
        if (node.classList && (
            node.classList.contains('ql-tab') ||
            node.classList.contains('ql-soft-break')
        )) {
            return true;
        }

        // Check for block elements
        if (node.tagName && BLOCK_ELEMENTS.includes(node.tagName)) {
            return true;
        }

        return false;
    },

    /**
     * Recursively get all text nodes with their parent elements for style measurement
     */
    _getTextNodes: function(node) {
        const result = [];

        if (node.nodeType === 3) {
            // Text node - use parent for style
            result.push({ text: node.nodeValue, element: node.parentNode });
        } else if (node.childNodes && node.childNodes.length > 0) {
            // Element node - recurse into children
            for (const child of node.childNodes) {
                result.push(...this._getTextNodes(child));
            }
        }

        return result;
    },

    /**
     * OPTIMIZED: Cached text width measurement with LRU eviction
     */
    measureTextWidth: function(text, referenceNode) {
        if (!text) return 0;

        const computedStyle = this._getCachedStyle(referenceNode);
        const cacheKey = `${text}|${computedStyle.fontFamily}|${computedStyle.fontSize}|${computedStyle.fontWeight}`;

        // Check cache first (with LRU re-ordering)
        if (this._textWidthCache.has(cacheKey)) {
            const value = this._textWidthCache.get(cacheKey);
            // Re-insert for LRU ordering (most recently used goes to end)
            this._textWidthCache.delete(cacheKey);
            this._textWidthCache.set(cacheKey, value);
            return value;
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

        // LRU cache: remove oldest entry if at capacity
        if (this._textWidthCache.size >= this._cacheMaxSize) {
            const firstKey = this._textWidthCache.keys().next().value;
            this._textWidthCache.delete(firstKey);
        }
        this._textWidthCache.set(cacheKey, width);

        return width;
    }
};

// TODO: RTL-Support - not implemented
// TODO: CSS Zoom/Transform - not implemented
// TODO: Multi-Editor-Support - Singleton pattern maintained