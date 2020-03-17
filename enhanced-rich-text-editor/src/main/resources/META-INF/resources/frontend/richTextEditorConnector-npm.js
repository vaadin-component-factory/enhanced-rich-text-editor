window['setToolbarButtons'] = function(rte, stringValue) {
  var toolbarButtons = JSON.parse(stringValue);
  rte.toolbarButtons = toolbarButtons;
}
