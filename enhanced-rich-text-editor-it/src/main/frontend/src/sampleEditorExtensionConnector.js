(function () {
    window.Vaadin ??= {};
    window.Vaadin.Flow ??= {};
    window.Vaadin.Flow.vcfEnhancedRichTextEditor ??= {};
    const ns = window.Vaadin.Flow.vcfEnhancedRichTextEditor;

    // --- extendQuill: register a simple highlight format ---
    ns.extendQuill ??= [];
    ns.extendQuill.push((Quill) => {
        if (!Quill.__highlightRegistered) {
            const Inline = Quill.import('blots/inline');
            class HighlightBlot extends Inline {
                static blotName = 'highlight';
                static tagName = 'MARK';
            }
            Quill.register(HighlightBlot);
            Quill.__highlightRegistered = true;
        }
    });

    // --- extendEditor: verify instance access, set flag ---
    ns.extendEditor ??= [];
    ns.extendEditor.push((editor, Quill) => {
        // Set a flag on the editor root to prove the hook fired
        editor.root.dataset.extendEditorCalled = 'true';
    });
}());
