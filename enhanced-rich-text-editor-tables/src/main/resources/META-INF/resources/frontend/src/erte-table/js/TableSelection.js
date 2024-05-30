import TableTrick from "./TableTrick";

class TableSelection {
  static focusedCell = null;
  static isMouseDown = false;
  static selectionStartElement = null;
  static selectionEndElement = null;
  static previousSelection = [];

  static dblClickTimeout = null;
  static clickedCellTimeout = null;
  static preventMouseDown = true;
  static cellSelectionOnClick = false;

  static mouseDown(quill, e, inCellSelectionOnClick) {
    if (inCellSelectionOnClick !== undefined) { // we may have no options set for onClick
      TableSelection.cellSelectionOnClick = inCellSelectionOnClick;
    }

    if (e.which !== 1 || (quill.container.classList.contains('ql-bubble') && TableSelection.isInsideToolbar(e))) {
      // do nothing with center or right click
      return;
    }

    // reset cell selection, even if it is cursor click, keeps everyone in check
    TableSelection.previousSelection = [TableSelection.selectionStartElement, TableSelection.selectionEndElement];
    TableSelection.selectionStartElement = TableSelection.selectionEndElement = null;
    TableSelection.resetSelection();

    if ((!TableSelection.cellSelectionOnClick && (e.ctrlKey || e.metaKey)) || TableSelection.cellSelectionOnClick) {
      TableSelection.isMouseDown = true;

      const targetCell = TableSelection.getTargetCell(e);
      if (!targetCell) {
        // default mouse down event when clicking outside a cell
        TableSelection.focusedCell = null;
        return;
      }

      if ((!TableSelection.preventMouseDown && targetCell === TableSelection.clickedCellTimeout) || TableSelection.focusedCell === targetCell) {
        // default mouse down event when multiple click in less than 500ms in the same cell or if the cell is already focused
        TableSelection.focusedCell = targetCell;
        return;
      }

      // single mouse left click = start selection
      e.preventDefault();
      TableSelection.focusedCell = null;

      clearTimeout(TableSelection.dblClickTimeout);
      TableSelection.dblClickTimeout = setTimeout(() => {
        TableSelection.preventMouseDown = true;
        TableSelection.clickedCellTimeout = null;
      }, 500);
      TableSelection.preventMouseDown = false;

      TableSelection.selectionStartElement = TableSelection.clickedCellTimeout = targetCell;

      if (TableSelection.selectionStartElement) {
        TableSelection.selectionStartElement.classList.add('ql-cell-selected');
      }
    }
  }

  static mouseMove(quill, e) {
    if (TableSelection.isMouseDown && TableSelection.selectionStartElement) {
      const previousSelectionEndElement = TableSelection.selectionEndElement;
      TableSelection.selectionEndElement = TableSelection.getTargetCell(e);
      // Update selection if: mouse button is down, selection changed, start and end element exist and are in the same table
      if (
          TableSelection.selectionEndElement &&
          TableSelection.selectionEndElement !== previousSelectionEndElement &&
          TableSelection.selectionStartElement.closest('table') === TableSelection.selectionEndElement.closest('table')
      ) {
        TableSelection.resetSelection();

        // set new selection
        const coords = TableSelection.getSelectionCoords();
        for (let y = coords.minY; y <= coords.maxY; y++) {
          for (let x = coords.minX; x <= coords.maxX; x++) {
            let cell = TableSelection.getCellAt(x, y);
            if (cell) {
              cell.classList.add('ql-cell-selected');
            }
          }
        }
      }
    }
  }

  static mouseUp(quill, e) {
    TableSelection.isMouseDown = false;
    if (!TableSelection.selectionEndElement) {
      TableSelection.selectionEndElement = TableSelection.selectionStartElement;
    }

    if (
        TableSelection.previousSelection[0] !== TableSelection.selectionStartElement &&
        TableSelection.previousSelection[1] !== TableSelection.selectionEndElement
    ) {
      TableSelection.selectionChange(quill);
    }
  }

  static cellSelectionChange(quill, range = null, oldRange = null) {
    //  Does currently not check for a table change, but col/row indices only.
    //  In the current use cases, the table change should be catched already by the table selection event, but there
    //  might be use cases in future, where an additionl check is necessary.

    const rte = TableTrick.getRichTextEditorInstance(quill);
    let selectedCell = quill.__selectedTableCell;
    let selectedRow = quill.__selectedTableRow;

    // lookup the current column and row index
    let colIndex, rowIndex;
    if (quill.__selectedTable && selectedRow) {
      const table = quill.__selectedTable;
      for (let i = 0; i < table.childNodes.length && !rowIndex; i++) {
        if (table.childNodes[i] === selectedRow) {
          rowIndex = i;
        }
      }

      if (selectedCell) {
        for (let i = 0; i < selectedRow.childNodes.length && !colIndex; i++) {
          if (selectedRow.childNodes[i] === selectedCell) {
            colIndex = i;
          }
        }
      }
    }

    if (quill.__selectedTableRowIndex !== rowIndex || quill.__selectedTableColIndex !== colIndex) {
      quill.__selectedTableRowIndex = rowIndex;
      quill.__selectedTableColIndex = colIndex;

      rte.dispatchEvent(new CustomEvent("table-cell-changed", {
        detail: {
          colIndex,
          rowIndex
        }
      }));
    }

  }



  static selectionChange(quill, range = null, oldRange = null) {
    let host = TableTrick.getRichTextEditorInstance(quill);

    // check, if the current "global" selection is in the editor, otherwise do not fire a table selection event,
    // as it will result in "null" for any clicks outside the editor:
    const selection = host.shadowRoot.getSelection ? host.shadowRoot.getSelection() : document.getSelection();
    const selectedNode = selection.anchorNode ? (selection.anchorNode.nodeType === Node.TEXT_NODE ? selection.anchorNode.parentElement : selection.anchorNode) : null;
    const editor = selectedNode?.closest(".ql-editor");
    if (editor) { // we are in quill context, so fine to fire events
      let isInTable = TableSelection.selectionStartElement != null || TableSelection.selectionEndElement != null;
      let tableTemplate = "";

      delete quill.__selectedTable;
      if (TableSelection.selectionStartElement) {
        // obtain table and class from "cell selection"
        const table = TableSelection.selectionStartElement.closest("table");
        tableTemplate = table?.classList?.toString();
        quill.__selectedTable = table;
      } else {
        // obtain table and class from normal text selection
        const selection = quill.getSelection();
        if (selection) {
          const leaf = quill.getLeaf(selection.index)?.[0]?.domNode;
          if (leaf) {
            const table = (leaf.tagName ? leaf : leaf.parentElement).closest("table");

            if (!isInTable) { // set flag if necessary
              isInTable = table != null;
            }
            tableTemplate = table?.classList?.toString();
            quill.__selectedTable = table;
          }
        }
      }

      const selectedCell = selectedNode?.closest("td");
      const selectedRow = selectedNode?.closest("tr");

      quill.__selectedTableCell = selectedCell;
      quill.__selectedTableRow = selectedRow;

      host.dispatchEvent(new CustomEvent("table-selected", {
        detail: {
          selected: isInTable,
          cellSelectionActive: TableSelection.selectionStartElement != null,
          template: tableTemplate
        }
      }));

      TableSelection.cellSelectionChange(quill);

      if (!isInTable && quill.table.isInTable) {
        quill.table.isInTable = false;
      }

      if (isInTable && !quill.table.isInTable) {
        quill.table.isInTable = true;

      }
    }
  }
  static getSelectionCoords() {
    if (TableSelection.selectionStartElement && TableSelection.selectionEndElement) {
      const coords = [
        [
          Array.prototype.indexOf.call(TableSelection.selectionStartElement.parentElement.children, TableSelection.selectionStartElement),
          Array.prototype.indexOf.call(TableSelection.selectionStartElement.parentElement.parentElement.children, TableSelection.selectionStartElement.parentElement)
        ],
        [
          Array.prototype.indexOf.call(TableSelection.selectionEndElement.parentElement.children, TableSelection.selectionEndElement),
          Array.prototype.indexOf.call(TableSelection.selectionEndElement.parentElement.parentElement.children, TableSelection.selectionEndElement.parentElement)
        ]
      ];

      return {
        coords,
        minX: Math.min(coords[0][0], coords[1][0]),
        maxX: Math.max(coords[0][0], coords[1][0]),
        minY: Math.min(coords[0][1], coords[1][1]),
        maxY: Math.max(coords[0][1], coords[1][1])
      };
    }
    return null;
  }

  static getCellAt(x, y) {
    const currentTable = TableSelection.selectionStartElement.closest('table');
    if (currentTable) {
      if (typeof currentTable.children[y] !== 'undefined' && typeof currentTable.children[y].children[x] !== 'undefined') {
        return currentTable.children[y].children[x];
      }
    }
    return null;
  }

  static getTargetCell(e) {
    let element = e.target;
    let cell = null;
    do {
      if (['td', 'th'].includes(element.tagName.toLowerCase())) {
        cell = element;
        break;
      }
      element = element.parentNode;
    } while (element && element !== e.currentTarget);
    return cell;
  }

  static isInsideToolbar(e) {
    return e.target.closest('.ql-toolbar');
  }

  static resetSelection(container) {
    // NOTE: not sure why the container is thrown in here, it was never used before, thus we do not use it neither

    // reset selection for all instances
    // since in the ERTE the quill is inside the shadow root, we cannot access it directly via the document, but the
    // editor only
    document.querySelectorAll("vcf-enhanced-rich-text-editor").forEach(rte => {
      rte._editor.container.querySelectorAll('.ql-editor td.ql-cell-selected').forEach(cell => {
        cell.classList.remove('ql-cell-selected');
      });
    });
  }
}

export default TableSelection;