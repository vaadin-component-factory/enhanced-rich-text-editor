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
@CssImport("quill/dist/quill.snow.css") // Loads CSS directly from the npm package
@StyleSheet("tab-stop-prototype.css")
@JsModule("./src/tab-stop-prototype.js")
public class TabStopPrototypeView extends VerticalLayout {

    private static final String INITIAL_VALUE = """
            {
              "ops": [
                { "insert": "Bestellübersicht 2024" },
                { "attributes": { "header": 1 }, "insert": "\\n" },
            
                { "attributes": { "bold": true }, "insert": "Pos." },
                { "insert": { "tab": true } },
                { "attributes": { "bold": true }, "insert": "Artikelbeschreibung" },
                { "insert": { "tab": true } },
                { "insert": { "tab": true } },
                { "attributes": { "bold": true }, "insert": "Menge" },
                { "insert": { "tab": true } },
                { "attributes": { "bold": true }, "insert": "Einzelpreis" },
                { "insert": { "tab": true } },
                { "attributes": { "bold": true }, "insert": "Gesamt\\n" },
            
                { "insert": "001" },
                { "insert": { "tab": true } },
                { "insert": "High-End Grafikkarte RTX 4090" },
                { "insert": { "tab": true } },
                { "insert": "2 Stk." },
                { "insert": { "tab": true } },
                { "insert": "1.899,00 €" },
                { "insert": { "tab": true } },
                { "insert": "3.798,00 €\\n" },
            
                { "insert": "002" },
                { "insert": { "tab": true } },
                { "insert": "USB-C Kabel (2m, Schwarz)" },
                { "insert": { "tab": true } },
                { "insert": "50 Stk." },
                { "insert": { "tab": true } },
                { "insert": "9,99 €" },
                { "insert": { "tab": true } },
                { "insert": "499,50 €\\n" },
            
                { "insert": "003" },
                { "insert": { "tab": true } },
                { "insert": "Servicepauschale Installation" },
                { "insert": { "tab": true } },
                { "insert": "1 Pausch." },
                { "insert": { "tab": true } },
                { "insert": "150,00 €" },
                { "insert": { "tab": true } },
                { "insert": "150,00 €\\n" },
            
                { "insert": "\\nSumme Netto:" },
                { "insert": { "tab": true } },
                { "insert": { "tab": true } },
                { "insert": { "tab": true } },
                { "insert": { "tab": true } },
                { "attributes": { "underline": true, "bold": true }, "insert": "4.447,50 €\\n" }
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

        Code deltaOut = new Code(/*Json.parse(INITIAL_VALUE).toJson()*/);
        deltaOut.getStyle().setOverflow(Style.Overflow.AUTO);

        Checkbox showTabs = new Checkbox("Show tabs");
        showTabs.addValueChangeListener(event -> getClassNames().set("debug", event.getValue()));
        showTabs.setValue(true);

        add(showTabs, editorContainer, deltaOut);

        getElement().executeJs("window._nativeQuill.init($0, $1)", editorPlaceholder, /*INITIAL_VALUE*/"");
        getElement().addEventListener("change", event -> {
            JsonObject eventData = event.getEventData();
            String string = eventData.getString("event.detail.value");

            deltaOut.setText(string);
        }).addEventData("event.detail.value");
    }
}