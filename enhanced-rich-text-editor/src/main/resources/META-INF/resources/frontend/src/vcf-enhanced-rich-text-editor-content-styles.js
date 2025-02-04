const documentContainer = document.createElement('template');

documentContainer.innerHTML = `
  <dom-module id="vcf-enhanced-rich-text-editor-content-styles">
    <template>
      <style>
        [part="content"] {
          box-sizing: border-box;
          position: relative;
          flex: auto;
          display: flex;
          flex-direction: column;
          overflow: hidden;
        }

        /*
          Quill core styles.
          CSS selectors removed: margin & padding reset, check list, video, colors, h1-6, anchor
        */
        .ql-clipboard {
          left: -100000px;
          height: 1px;
          overflow-y: hidden;
          position: absolute;
          top: 50%;
        }

        .ql-clipboard p {
          margin: 0;
          padding: 0;
        }

        .ql-editor {
          box-sizing: border-box;
          line-height: 1.42;
          height: 100%;
          outline: none;
          overflow-y: auto;
          padding: 0.75em 1em;
          tab-size: 4;
          -moz-tab-size: 4;
          text-align: left;
          white-space: pre-wrap;
          word-wrap: break-word;
          flex: 1;
        }

        .ql-editor > * {
          cursor: text;
        }

        .ql-direction-rtl {
          direction: rtl;
          text-align: inherit;
        }

        .ql-align-center {
          text-align: center;
        }

        .ql-align-justify {
          text-align: justify;
        }

        .ql-align-right {
          text-align: right;
        }

        .ql-indent-1:not(.ql-direction-rtl) {
          padding-left: 3em;
        }

        li.ql-indent-1:not(.ql-direction-rtl) {
          padding-left: 4.5em;
        }

        .ql-indent-1.ql-direction-rtl.ql-align-right {
          padding-right: 3em;
        }

        li.ql-indent-1.ql-direction-rtl.ql-align-right {
          padding-right: 4.5em;
        }

        .ql-indent-2:not(.ql-direction-rtl) {
          padding-left: 6em;
        }

        li.ql-indent-2:not(.ql-direction-rtl) {
          padding-left: 7.5em;
        }

        .ql-indent-2.ql-direction-rtl.ql-align-right {
          padding-right: 6em;
        }

        li.ql-indent-2.ql-direction-rtl.ql-align-right {
          padding-right: 7.5em;
        }

        .ql-indent-3:not(.ql-direction-rtl) {
          padding-left: 9em;
        }
        
        li.ql-indent-3:not(.ql-direction-rtl) {
          padding-left: 10.5em;
        }
        
        .ql-indent-3.ql-direction-rtl.ql-align-right {
          padding-right: 9em;
        }
        
        li.ql-indent-3.ql-direction-rtl.ql-align-right {
          padding-right: 10.5em;
        }
        
        .ql-indent-4:not(.ql-direction-rtl) {
          padding-left: 12em;
        }

        li.ql-indent-4:not(.ql-direction-rtl) {
          padding-left: 13.5em;
        }

        .ql-indent-4.ql-direction-rtl.ql-align-right {
          padding-right: 12em;
        }

        li.ql-indent-4.ql-direction-rtl.ql-align-right {
          padding-right: 13.5em;
        }

        .ql-indent-5:not(.ql-direction-rtl) {
          padding-left: 15em;
        }
        
        li.ql-indent-5:not(.ql-direction-rtl) {
          padding-left: 16.5em;
        }

        .ql-indent-5.ql-direction-rtl.ql-align-right {
          padding-right: 15em;
        }

        li.ql-indent-5.ql-direction-rtl.ql-align-right {
          padding-right: 16.5em;
        }

        .ql-indent-6:not(.ql-direction-rtl) {
          padding-left: 18em;
        }

        li.ql-indent-6:not(.ql-direction-rtl) {
          padding-left: 19.5em;
        }

        .ql-indent-6.ql-direction-rtl.ql-align-right {
          padding-right: 18em;
        }

        li.ql-indent-6.ql-direction-rtl.ql-align-right {
          padding-right: 19.5em;
        }

        .ql-indent-7:not(.ql-direction-rtl) {
          padding-left: 21em;
        }

        li.ql-indent-7:not(.ql-direction-rtl) {
          padding-left: 22.5em;
        }

        .ql-indent-7.ql-direction-rtl.ql-align-right {
          padding-right: 21em;
        }

        li.ql-indent-7.ql-direction-rtl.ql-align-right {
          padding-right: 22.5em;
        }

        .ql-indent-8:not(.ql-direction-rtl) {
          padding-left: 24em;
        }

        li.ql-indent-8:not(.ql-direction-rtl) {
          padding-left: 25.5em;
        }

        .ql-indent-8.ql-direction-rtl.ql-align-right {
          padding-right: 24em;
        }

        li.ql-indent-8.ql-direction-rtl.ql-align-right {
          padding-right: 25.5em;
        }

        .ql-indent-9:not(.ql-direction-rtl) {
          padding-left: 27em;
        }
        li.ql-indent-9:not(.ql-direction-rtl) {
          padding-left: 28.5em;
        }

        .ql-indent-9.ql-direction-rtl.ql-align-right {
          padding-right: 27em;
        }

        li.ql-indent-9.ql-direction-rtl.ql-align-right {
          padding-right: 28.5em;
        }

        ol,
        ul {
          counter-reset: list-1 list-2 list-3 list-4 list-5 list-6 list-7 list-8 list-9;
        }

        ol li {
          counter-reset: list-1 list-2 list-3 list-4 list-5 list-6 list-7 list-8 list-9;
          counter-increment: list-0;
        }

        ol li:before {
          content: counter(list-0, decimal) '. ';
        }

        ol li.ql-indent-1 {
          counter-increment: list-1;
        }

        ol li.ql-indent-1:before {
          content: counter(list-1, lower-alpha) '. ';
        }

        ol li.ql-indent-1 {
          counter-reset: list-2 list-3 list-4 list-5 list-6 list-7 list-8 list-9;
        }

        ol li.ql-indent-2 {
          counter-increment: list-2;
        }

        ol li.ql-indent-2:before {
          content: counter(list-2, lower-roman) '. ';
        }

        ol li.ql-indent-2 {
          counter-reset: list-3 list-4 list-5 list-6 list-7 list-8 list-9;
        }

        ol li.ql-indent-3 {
          counter-increment: list-3;
        }

        ol li.ql-indent-3:before {
          content: counter(list-3, decimal) '. ';
        }

        ol li.ql-indent-3 {
          counter-reset: list-4 list-5 list-6 list-7 list-8 list-9;
        }

        ol li.ql-indent-4 {
          counter-increment: list-4;
        }

        ol li.ql-indent-4:before {
          content: counter(list-4, lower-alpha) '. ';
        }

        ol li.ql-indent-4 {
          counter-reset: list-5 list-6 list-7 list-8 list-9;
        }

        ol li.ql-indent-5 {
          counter-increment: list-5;
        }

        ol li.ql-indent-5:before {
          content: counter(list-5, lower-roman) '. ';
        }

        ol li.ql-indent-5 {
          counter-reset: list-6 list-7 list-8 list-9;
        }

        ol li.ql-indent-6 {
          counter-increment: list-6;
        }

        ol li.ql-indent-6:before {
          content: counter(list-6, decimal) '. ';
        }

        ol li.ql-indent-6 {
          counter-reset: list-7 list-8 list-9;
        }

        ol li.ql-indent-7 {
          counter-increment: list-7;
        }

        ol li.ql-indent-7:before {
          content: counter(list-7, lower-alpha) '. ';
        }

        ol li.ql-indent-7 {
          counter-reset: list-8 list-9;
        }

        ol li.ql-indent-8 {
          counter-increment: list-8;
        }

        ol li.ql-indent-8:before {
          content: counter(list-8, lower-roman) '. ';
        }

        ol li.ql-indent-8 {
          counter-reset: list-9;
        }

        ol li.ql-indent-9 {
          counter-increment: list-9;
        }

        ol li.ql-indent-9:before {
          content: counter(list-9, decimal) '. ';
        }

        ol > li, 
        ul > li {
          list-style-type: none;
        }

        ul > li::before {
          content: '\\25cf';
        }

        li::before {
          display: inline-block;
          white-space: nowrap;
          width: 1.2em;
        }

        li:not(.ql-direction-rtl)::before {
          margin-left: -1.5em;
          margin-right: 0.3em;
          text-align: right;
        }

        li.ql-direction-rtl::before {
          margin-left: 0.3em;
          margin-right: -1.5em;
        }
        
        /* quill core end */

        blockquote {
          border-left: 0.25em solid #ccc;
          margin-bottom: 0.3125em;
          margin-top: 0.3125em;
          padding-left: 1em;
        }

        code,
        pre {
          background-color: #f0f0f0;
          border-radius: 0.1875em;
        }

        pre {
          white-space: pre-wrap;
          margin-bottom: 0.3125em;
          margin-top: 0.3125em;
          padding: 0.3125em 0.625em;
        }

        code {
          font-size: 85%;
          padding: 0.125em 0.25em;
        }

        img {
          max-width: 100%;
        }
        
        .ql-placeholder {
          padding: 0 var(--lumo-space-xs);
          display: inline-block;
          background-color: var(--lumo-primary-color-10pct);
          color: var(--lumo-primary-color);
          border-radius: var(--lumo-border-radius);
        }
      </style>
    </template>
  </dom-module>
`;

document.head.appendChild(documentContainer.content);
