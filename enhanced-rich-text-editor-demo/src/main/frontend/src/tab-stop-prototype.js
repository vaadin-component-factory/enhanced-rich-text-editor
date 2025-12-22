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


window._nativeQuill = {
    EXTERNAL_TAB_STOPS: [100, 250, 450, 600, 750],
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
                        }
                    }
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
    drawRuler: function () {
        if (!this.quillInstance) return;

        const ruler = document.createElement('div');
        ruler.className = 'native-quill-ruler';

        this.EXTERNAL_TAB_STOPS.forEach(pos => {
            const tick = document.createElement('div');
            tick.className = 'ruler-tick';
            tick.style.left = pos + 'px';

            const label = document.createElement('span');
            label.className = 'ruler-label';
            label.innerText = pos;
            tick.appendChild(label);

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

        // CRITICAL: Must stay synced with CSS 'min-width' to prevent layout thrashing.
        const MIN_TAB_WIDTH = 2;

        const editorNode = this.quillInstance.root;
        const editorRect = editorNode.getBoundingClientRect();
        const tabs = editorNode.querySelectorAll('.ql-tab');

        tabs.forEach(tab => {
            const tabRect = tab.getBoundingClientRect();
            const startPos = tabRect.left - editorRect.left;

            // Find the next defined stop that offers enough clearance (MIN_TAB_WIDTH).
            let nextStop = this.EXTERNAL_TAB_STOPS.find(stop => (stop - startPos) >= MIN_TAB_WIDTH);

            // Fallback: Infinite Grid logic
            if (nextStop === undefined) {
                let potentialStop = Math.ceil(startPos / this.DEFAULT_TAB_SIZE) * this.DEFAULT_TAB_SIZE;

                if ((potentialStop - startPos) < MIN_TAB_WIDTH) {
                    potentialStop += this.DEFAULT_TAB_SIZE;
                }
                nextStop = potentialStop;
            }

            const widthNeeded = nextStop - startPos;
            const newWidthStr = widthNeeded + 'px';

            // Only touch DOM if value actually changed
            if (tab.style.width !== newWidthStr) {
                tab.style.width = newWidthStr;
            }
        });
    }
};