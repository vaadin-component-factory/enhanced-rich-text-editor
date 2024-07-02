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
        if (!Quill.__moduleXyzRegistered) {
            // console.info("Register Quill Module XYZ for Enhanced Rich Text Editor");
            // Quill.register('some/module', ...);
            Quill.__moduleXyzRegistered = true;
        }

        options.modules = {
            ...options.modules,
            /* add your registered module here*/
            keyboard: {
                ...options.modules?.keyboard,
                /* add your keybindings here*/
            }
            /* other options*/
        }
    });
}());


