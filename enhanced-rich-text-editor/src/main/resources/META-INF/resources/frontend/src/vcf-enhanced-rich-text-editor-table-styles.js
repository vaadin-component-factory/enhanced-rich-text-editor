// const documentContainer = document.createElement('template');
//
// documentContainer.innerHTML = `
//   <dom-module id="vcf-enhanced-rich-text-editor-table-styles">
//     <template>
//       <style>
//         .ql-editor table {
//           width: 100%;
//           border-collapse: collapse;
//           table-layout: fixed;
//           overflow:hidden;
//           white-space: nowrap;
//         }
//
//         .ql-editor table td {
//           border: 1px solid black;
//           padding: 2px 5px;
//           height: 25px;
//           vertical-align: top;
//           white-space: pre-wrap; /* https://github.com/quilljs/quill/issues/1760 */
//         }
//
//         .ql-editor__table--hideBorder td {
//           border: none !important;
//         }
//
//         .ql-editor table td[rowspan="2"] {
//           height: 50px;
//         }
//
//         .ql-editor table td[rowspan="3"] {
//           height: 75px;
//         }
//
//         .ql-editor table td[rowspan="4"] {
//           height: 100px;
//         }
//
//         .ql-editor table td[rowspan="5"] {
//           height: 125px;
//         }
//
//         .ql-editor table td[rowspan="6"] {
//           height: 150px;
//         }
//
//         .ql-editor table td[rowspan="7"] {
//           height: 175px;
//         }
//
//         .ql-editor table td[rowspan="8"] {
//           height: 200px;
//         }
//
//         .ql-editor table td[rowspan="9"] {
//           height: 225px;
//         }
//
//         .ql-editor table td.ql-cell-selected {
//           background-color: #cce0f8;
//         }
//
//         .ql-editor table td[merge_id] {
//           display: none;
//         }
//
//         .quill-better-table-wrapper {
//           overflow-x: auto;
//         }
//
//         .ql-picker.ql-table {
//           width: auto !important;
//           margin-right: 0;
//         }
//
//         .ql-picker.ql-table .ql-picker-label svg {
//           display: none;
//         }
//
//         /* .ql-formats button.ql-table::after, */
//         .ql-picker.ql-table .ql-picker-label::before {
//           display: block;
//           font-size: 14px;
//         }
//
//         .ql-bubble .ql-toolbar .ql-picker.ql-table .ql-picker-label {
//           color: #ccc;
//         }
//
//         .ql-bubble .ql-toolbar .ql-picker.ql-table .ql-picker-label:hover {
//           color: #fff;
//         }
//
//         .ql-picker.ql-table-grid .ql-picker-label::before {
//           content: "\\f0ce";
//         }
//
//         .ql-picker.ql-table .ql-picker-label::before {
//           font-family: "Font Awesome 6 Free";
//           font-weight: 900;
//           padding-top: 2px;
//           line-height: 1em;
//         }
//
//         .ql-picker-item[data-value="insert"]::after {
//           content: "Insert table";
//         }
//
//         .ql-picker-item[data-value="append-col"]::after {
//           content: "Add column";
//         }
//
//         .ql-picker-item[data-value="append-col-before"]::after {
//           content: "Add column before";
//         }
//
//         .ql-picker-item[data-value="append-col-after"]::after {
//           content: "Add column after";
//         }
//
//         .ql-picker-item[data-value="remove-col"]::after {
//           content: "Remove column";
//         }
//
//         .ql-picker-item[data-value="append-row"]::after {
//           content: "Add row";
//         }
//
//         .ql-picker-item[data-value="append-row-above"]::after {
//           content: "Add row above";
//         }
//
//         .ql-picker-item[data-value="append-row-below"]::after {
//           content: "Add row below";
//         }
//
//         .ql-picker-item[data-value="remove-row"]::after {
//           content: "Remove row";
//         }
//
//         .ql-picker-item[data-value="split-cell"]::after {
//           content: "Split cell";
//         }
//
//         .ql-picker-item[data-value="merge-selection"]::after {
//           content: "Merge selection";
//         }
//
//         .ql-picker-item[data-value="remove-cell"]::after {
//           content: "Remove cell";
//         }
//
//         .ql-picker-item[data-value="remove-selection"]::after {
//           content: "Remove selection";
//         }
//
//         .ql-picker-item[data-value="undo"]::after {
//           content: "Undo";
//         }
//
//         .ql-picker-item[data-value="redo"]::after {
//           content: "Redo";
//         }
//
//         .ql-picker-item[data-value="remove-table"]:before {
//           content: "Remove table";
//         }
//
//         .ql-picker-item[data-value="hide-border"]::after {
//           content: "Hide Border";
//         }
//
//         .ql-picker-item[data-value="show-border"]::after {
//           content: "Show Border";
//         }
//
//         .ql-table-grid,
//         .ql-contain {
//           width: 90px;
//           margin-right: 0;
//         }
//
//         .ql-picker.ql-table-grid {
//           font-size: 11px;
//           font-weight: normal;
//         }
//
//         .ql-picker.ql-table .ql-picker-label {
//           padding: 2px 3px;
//           width: 23px;
//         }
//
//         .ql-picker.ql-table-grid .ql-picker-options {
//           width: 180px;
//         }
//
//         .ql-picker.ql-table-grid .ql-picker-item {
//           display: block;
//           float: left;
//           width: 30px;
//           height: 30px;
//           line-height: 30px;
//           text-align: center;
//           padding: 0px;
//           margin: 1px;
//         }
//
//         .ql-toolbar .ql-picker.ql-table .ql-picker-item {
//           display: none;
//         }
//
//         .ql-toolbar .ql-picker.ql-table .ql-picker-item.enabled {
//           display: block;
//         }
//
//         .ql-picker.ql-table-listing {
//           display: flex;
//           width: 145px;
//         }
//
//         .ql-picker.ql-table-listing .ql-picker-label::before {
//           content: "\\f850";
//         }
//
//         .ql-picker.ql-table-listing {
//           color: #444;
//         }
//
//         .ql-picker.ql-table-grid .ql-picker-item {
//           border: 1px solid #444;
//           color: #444;
//         }
//
//         .ql-bubble .ql-picker.ql-table-grid .ql-picker-item {
//           border: 1px solid #ccc;
//           color: #ccc;
//         }
//
//         .ql-bubble .ql-picker.ql-table-grid .ql-picker-item:hover {
//           border: 1px solid #fff;
//           color: #fff;
//         }
//
//         .ql-table-listing .ql-picker-label.ql-active {
//           color: #444 !important;
//         }
//
//         .ql-toolbar .ql-picker-item.ql-selected:before, .ql-picker.ql-table-listing .ql-picker-item {
//           color: #444;
//         }
//
//         .ql-bubble .ql-toolbar .ql-picker-item.ql-selected:before, .ql-bubble .ql-picker.ql-table-listing .ql-picker-item {
//           color: #ccc;
//         }
//
//         .ql-bubble .ql-picker.ql-table-listing .ql-picker-item:hover {
//           color: #fff;
//         }
//
//         .ql-picker-label.ql-active .ql-stroke {
//           stroke: #444 !important;
//         }
//
//         .ql-picker-item[data-value="remove-table"] {
//           border: none !important;
//           width: 100%;
//         }
//
//         .ql-picker-item[data-value=newtable_1_1]:before {
//           content: "1x1";
//         }
//
//         .ql-picker-item[data-value=newtable_1_2]:before {
//           content: "1x2";
//         }
//
//         .ql-picker-item[data-value=newtable_1_3]:before {
//           content: "1x3";
//         }
//
//         .ql-picker-item[data-value=newtable_1_4]:before {
//           content: "1x4";
//         }
//
//         .ql-picker-item[data-value=newtable_1_5]:before {
//           content: "1x5";
//         }
//
//         .ql-picker-item[data-value=newtable_2_1]:before {
//           content: "2x1";
//         }
//
//         .ql-picker-item[data-value=newtable_2_2]:before {
//           content: "2x2";
//         }
//
//         .ql-picker-item[data-value=newtable_2_3]:before {
//           content: "2x3";
//         }
//
//         .ql-picker-item[data-value=newtable_2_4]:before {
//           content: "2x4";
//         }
//
//         .ql-picker-item[data-value=newtable_2_5]:before {
//           content: "2x5";
//         }
//
//         .ql-picker-item[data-value=newtable_3_1]:before {
//           content: "3x1";
//         }
//
//         .ql-picker-item[data-value=newtable_3_2]:before {
//           content: "3x2";
//         }
//
//         .ql-picker-item[data-value=newtable_3_3]:before {
//           content: "3x3";
//         }
//
//         .ql-picker-item[data-value=newtable_3_4]:before {
//           content: "3x4";
//         }
//
//         .ql-picker-item[data-value=newtable_3_5]:before {
//           content: "3x5";
//         }
//
//         .ql-picker-item[data-value=newtable_4_1]:before {
//           content: "4x1";
//         }
//        
//         .ql-picker-item[data-value=newtable_4_2]:before {
//           content: "4x2";
//         }
//
//         .ql-picker-item[data-value=newtable_4_3]:before {
//           content: "4x3";
//         }
//
//         .ql-picker-item[data-value=newtable_4_4]:before {
//           content: "4x4";
//         }
//
//         .ql-picker-item[data-value=newtable_4_5]:before {
//           content: "4x5";
//         }
//
//         .ql-picker-item[data-value=newtable_5_1]:before {
//           content: "5x1";
//         }
//
//         .ql-picker-item[data-value=newtable_5_2]:before {
//           content: "5x2";
//         }
//
//         .ql-picker-item[data-value=newtable_5_3]:before {
//           content: "5x3";
//         }
//
//         .ql-picker-item[data-value=newtable_5_4]:before {
//           content: "5x4";
//         }
//
//         .ql-picker-item[data-value=newtable_5_5]:before {
//           content: "5x5";
//         }
//
//       </style>
//     </template>
//   </dom-module>
// `;
//
// document.head.appendChild(documentContainer.content);
