// import '../vendor/vaadin-quill.js';
import TableModule from "./index";
import TableTrick from "./js/TableTrick.js";
import TableSelection from "./js/TableSelection.js";


(function () {
    if (typeof window.Vaadin.Flow.vcfEnhancedRichTextEditor !== "object") {
        window.Vaadin.Flow.vcfEnhancedRichTextEditor = {};
    }

    // update the options passed into the new Quill instance
    if (!Array.isArray(window.Vaadin.Flow.vcfEnhancedRichTextEditor.extendOptions)) {
        window.Vaadin.Flow.vcfEnhancedRichTextEditor.extendOptions = [];
    }

    window.Vaadin.Flow.vcfEnhancedRichTextEditor.extendOptions.push((options, Quill) => {
        // extend quill with your module - since Quill is a global object, assure to only register it once
        if (!Quill.__tablesRegistered) {
            console.info("Register Quill Table Module for Enhanced Rich Text Editor");
            Quill.register('modules/table', TableModule);
            Quill.__tablesRegistered = true;
        }

        options.modules = {
            ...options.modules,
            table: true,
            keyboard: {
                ...options.modules?.keyboard,
                bindings: TableModule.keyBindings
            }
        }
    });

    if (!window.Vaadin.Flow.vcfEnhancedRichTextEditor.extensions) {
        window.Vaadin.Flow.vcfEnhancedRichTextEditor.extensions = {};
    }

    window.Vaadin.Flow.vcfEnhancedRichTextEditor.extensions.tables = {
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
                rte.__ignoreSelect = true;
                rte._editor.focus();
                delete rte.__ignoreSelect;
            }
        },

        _setStyles(rte, styles) {
            let s = rte.shadowRoot.querySelector('#table-template-styles')
            if (!s) {
                s = document.createElement('style');
                s.id = 'table-template-styles';
                rte.shadowRoot.append(s);
            }
            s.innerHTML = styles;
        },

        _getSelectedTable(rte) {
            return rte._editor.__selectedTable;
        },

        setTemplate(rte, template) {
            const classList = this._getSelectedTable(rte)?.classList;
            if (classList) {
                classList.remove(...classList);
                classList.add(template);
            }
        },
    };
}());