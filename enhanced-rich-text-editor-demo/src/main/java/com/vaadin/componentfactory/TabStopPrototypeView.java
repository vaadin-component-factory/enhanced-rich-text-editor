package com.vaadin.componentfactory;

import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dependency.*;
import com.vaadin.flow.component.html.Code;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.dom.Style;
import com.vaadin.flow.router.Route;
import elemental.json.Json;
import elemental.json.JsonObject;

@Route("tab-stop")
@NpmPackage(value = "quill", version = "1.3.6")
@CssImport("quill/dist/quill.snow.css") // Lädt das CSS direkt aus dem npm paket
@StyleSheet("tab-stop-prototype.css")
@JsModule("./src/tab-stop-prototype.js")
public class TabStopPrototypeView extends VerticalLayout {

    private static final String INITIAL_VALUE = """
            {
                "ops":[
                {"insert":"111"},{"insert":{"tab":true}},{"insert":"2222"},{"insert":{"tab":true}},{"insert":"33333"},{"attributes":{"tab-stops":[{"pos":211,"align":"left"},{"pos":376,"align":"right"}]},"insert":"\\n"}
                ]
            }
            
            """;

    public TabStopPrototypeView() {
        setSizeFull();
        setAlignItems(Alignment.STRETCH);

        Div editorPlaceholder = new Div();
        editorPlaceholder.getStyle().setFlexGrow("1");

        VerticalLayout editorContainer = new VerticalLayout(editorPlaceholder);
        editorContainer.setPadding(false);
        editorContainer.setSpacing(false);
        editorContainer.setAlignItems(Alignment.STRETCH);
        editorContainer.getStyle().set("flex", "1 0 50%").setBoxSizing(Style.BoxSizing.BORDER_BOX);

        Code deltaOut = new Code(Json.parse(INITIAL_VALUE).toJson());
        deltaOut.getStyle().setOverflow(Style.Overflow.AUTO);

        Checkbox showTabs = new Checkbox("Show tabs");
        showTabs.addValueChangeListener(event -> getClassNames().set("debug", event.getValue()));
        showTabs.setValue(true);

        add(showTabs, editorContainer, deltaOut);

        getElement().executeJs("window._nativeQuill.init($0, $1)", editorPlaceholder, INITIAL_VALUE);
        getElement().addEventListener("change", event -> {
            JsonObject eventData = event.getEventData();
            String string = eventData.getString("event.detail.value");

            deltaOut.setText(string);
        }).addEventData("event.detail.value");
    }
}