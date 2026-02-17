// Quill 2 / Parchment 3 version of connector
// Changes from V1:
// 1. Removed vendor import (Quill 2 globally available)
// 2. __blot.blot -> Quill.find(domNode) in setTemplate
// 3. V25: Register blots at top-level + init TableModule on existing Quill instance
//    (V24 used extendOptions callback consumed by ERTE's own Quill constructor;
//     V25's RTE 2 parent creates Quill, so extendOptions is never consumed)

// Import RTE 2 first to ensure window.Quill is set before any blot files access it.
// Without this, the Vite bundler may load this module before the ERTE core module,
// causing a fatal TypeError: Cannot read properties of undefined (reading 'import').
import '@vaadin/rich-text-editor/src/vaadin-rich-text-editor.js';

import TableModule from "./index";
import TableTrick from "./js/TableTrick.js";
import TableSelection from "./js/TableSelection.js";

const Quill = window.Quill;

// V25: Register table blots globally at module top-level, BEFORE any Quill instance
// is created. This ensures the Quill registry (and scroll.create()) knows about
// table/tr/td/contain blots when TableTrick.insertTable() is called.
if (!Quill.__tablesRegistered) {
    console.info("Register Quill Table Module for Enhanced Rich Text Editor");
    TableModule.register(); // Registers TableCell, TableRow, Table, Contain blots
    Quill.register('modules/table', TableModule);
    Quill.__tablesRegistered = true;
}

(function () {
    if (typeof window.Vaadin.Flow.vcfEnhancedRichTextEditor !== "object") {
        window.Vaadin.Flow.vcfEnhancedRichTextEditor = {};
    }

    if (!window.Vaadin.Flow.vcfEnhancedRichTextEditor.extensions) {
        window.Vaadin.Flow.vcfEnhancedRichTextEditor.extensions = {};
    }

    window.Vaadin.Flow.vcfEnhancedRichTextEditor.extensions.tables = {
        init(rte) {
            rte.shadowRoot.append(
                this._createStyleElement('table-template-custom-styles-1'),
                this._createStyleElement('table-template-styles'),
                this._createStyleElement('table-template-custom-styles-2')
            );

            // V25: Initialize table module on the existing Quill instance.
            // In V24, this happened via extendOptions before Quill construction.
            // In V25, RTE 2 creates Quill, so we initialize the module manually.
            const quill = rte._editor;
            if (quill && !quill.__tableModuleInitialized) {
                // Instantiate the table module (sets up mouse events, clipboard
                // matchers, selection handling, history stack)
                new TableModule(quill, {});

                // Prepend table keyboard bindings BEFORE ERTE's bindings.
                // Table Tab must run first: if cursor is in a table cell,
                // Tab navigates to next cell instead of inserting a tab-stop.
                const keyboard = quill.keyboard;
                const bindings = TableModule.keyBindings;

                for (const [name, binding] of Object.entries(bindings)) {
                    // Get the key string that Quill uses for the bindings map
                    const keyStr = binding.key;

                    if (name === 'tab' || name === 'shiftTab') {
                        // For Tab/Shift+Tab: prepend to run before ERTE's tab handler.
                        // Do NOT use the table module's fallback \t insertion --
                        // return true to delegate to ERTE's tabStopBinding when outside a table.
                        const tableKeyName = name === 'tab' ? 'tab' : 'shiftTab';
                        const tableBinding = {
                            key: binding.key,
                            shiftKey: name === 'shiftTab' ? true : false,
                            handler: function(range, keycontext) {
                                return TableModule.keyboardHandler(quill, tableKeyName, range, keycontext);
                            }
                        };
                        if (keyboard.bindings[keyStr]) {
                            keyboard.bindings[keyStr].unshift(tableBinding);
                        } else {
                            keyboard.addBinding(tableBinding);
                        }
                    } else {
                        // For other keys (Backspace, Delete, SelectAll, Undo, Redo, Copy):
                        // prepend to the existing bindings so table handling runs first
                        if (keyboard.bindings[keyStr]) {
                            keyboard.bindings[keyStr].unshift(binding);
                        } else {
                            keyboard.addBinding(binding);
                        }
                    }
                }

                quill.__tableModuleInitialized = true;
            }
        },

        _createStyleElement(id) {
            let s = document.createElement('style');
            s.id = id;
            return s;
        },

        insert(rte, rows, cols, template) {
            this._assureFocus(rte);

            const row_count = Number.parseInt(rows);
            const col_count = Number.parseInt(cols);
            TableTrick.insertTable(rte._editor, col_count, row_count, template);
            TableSelection.selectionChange(rte._editor);
        },

        action(rte, action) {
            this._assureFocus(rte);
            TableTrick.table_handler(action, rte._editor);
        },

        _assureFocus(rte) {
            if (!rte._editor.hasFocus()) {
                rte._editor.focus();
            }
        },

        _setStyles(rte, styles) {
            const s = rte.shadowRoot.querySelector('#table-template-styles')
            s.innerHTML = styles;
        },

        _setCustomStyles(rte, styles, beforeGenerated) {
            const s = rte.shadowRoot.querySelector(`#table-template-custom-styles-${beforeGenerated ? '1' : '2'}`);
            s.innerHTML = styles;
        },


        _getSelectedTable(rte) {
            return rte._editor.__selectedTable;
        },

        setTemplate(rte, template) {
            const selectedTable = this._getSelectedTable(rte);
            if (selectedTable) {
                const classList = selectedTable?.classList;
                if (classList) {
                    classList.remove(...classList);
                    if (template) {
                        classList.add(template);
                    }
                }

                // Change 2: __blot.blot -> Quill.find(domNode)
                const tableBlot = Quill.find(selectedTable);
                const firstRow = tableBlot?.children?.head;
                let firstCell = firstRow?.children?.head;
                if (firstRow?.domNode && firstCell?.domNode) {
                    // this shall trigger a value change for the server to notify it about the new template.
                    // i did not found a different way of doing this without messing in the internal of delta.
                    // therefore, this is the easier and (for me) cleaner way.
                    const cNode = firstCell.domNode;
                    cNode.remove();
                    firstRow.domNode.prepend(cNode);
                } else {
                    console.error("First table cell reference not found, could not fully apply template style.");
                }
            }
        },
    };
}());
