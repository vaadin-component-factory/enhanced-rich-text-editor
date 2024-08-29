package com.vaadin.componentfactory.erte.tables.templates;

import elemental.json.*;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

import static com.vaadin.componentfactory.erte.tables.templates.TemplateJsonConstants.*;

/**
 * Parses an Tables addon Css JSON Template and creates a css stylesheet out of it.
 */
public final class TemplateParser {
    private final JsonObject templates;
    private StringBuilder builder;
    private String currentTemplateName;

    /**
     * Parses the given template and creates a css string out of it.
     * <p/>
     * The given object will NOT be modified in any way beforehand. If you want
     * to strip empty children of it, please apply the methode {@link #removeEmptyChildren(JsonObject)} manually
     * beforehand.
     * @param templates templates
     * @return css string
     */
    public static String convertToCss(JsonObject templates) {
        return new TemplateParser(templates).toCss();
    }

    /**
     * Parses the given string and creates a css string out of it. The given string will be interpreted as a
     * stringified json object, containing templates.
     * @param templateJson templates as string
     * @return css string
     */
    public static String convertToCss(String templateJson) {
        return new TemplateParser(Json.parse(templateJson)).toCss();
    }

    /**
     * Parses the given template json and create a json object from it. The given json is taken as it is without
     * any modifications before or after the parse.
     * @param templateJson json string
     * @return json object
     */
    public static JsonObject parseJson(String templateJson) {
        return Json.parse(templateJson);
    }

    /**
     * Parses the given template json and create a json object from it. The given json is taken as it is without
     * any modifications before the parse. However, the method will remove any empty children from the
     * created json object, if the given boolean parameter is true.
     * @param templateJson json string
     * @param removeEmptyChildren remove empty parts
     * @return json object
     */
    public static JsonObject parseJson(String templateJson, boolean removeEmptyChildren) {
        JsonObject object = parseJson(templateJson);
        if (removeEmptyChildren) {
            removeEmptyChildren(object);
        }
        return object;
    }

    /**
     * Creates a new instance working with the given object. Any future changes to the json object will directly be
     * reflected to this instance and the resulting css. To prevent this you may want to pass a copy of your
     * template object instead.
     * <p/>
     * The given object will NOT be modified in any other way. If you want
     * to strip empty children of it, please apply the methode {@link #removeEmptyChildren(JsonObject)} manually
     * beforehand.
     * @param templates templates
     */
    public TemplateParser(JsonObject templates) {
        this.templates = templates;
    }

    /**
     * Generates the CSS bases on the current state of the templates object.
     * @return css string
     */
    public String toCss() {
        builder = new StringBuilder();
        if (templates != null) {
            for (String templateName : templates.keys()) {
                if (!isValidTemplateName(templateName)) {
                    throw new IllegalStateException(templateName + " is not a legal template name. It must match " + PATTERN_TEMPLATE_NAME.pattern());
                }

                currentTemplateName = templateName;

                JsonObject rules = templates.getObject(templateName);

                // check the rules for the different selectors
                // we could do that in a loop, but by doing it manually we can also
                // specify the order of css rules in the resulting sheet (which has an effect on the resulting look)
                if (rules.hasKey(TABLE)) {
                    parseTable(rules.getObject(TABLE));
                }

                // cols before rows makes the rows the overriding style when styles "overlap"
                if (rules.hasKey(COLUMNS)) {
                    parseCols(rules.getArray(COLUMNS));
                }

                if (rules.hasKey(ROWS)) {
                    parseRows(rules.getArray(ROWS));
                }

                if (rules.hasKey(CELLS)) {
                    parseCells(rules.getArray(CELLS));
                }
            }
        }
        return builder.toString().trim();
    }

    private void parseTable(JsonObject rules) {

        // BORDER_CELLS is a special property, that needs a specialized treatment. It is not to be applied
        // to the table selector directly, but to "all" cells
        if(rules.hasKey(P_BORDER_CELLS)) {
            String pBorderCells = rules.getString(P_BORDER_CELLS);
            rules.remove(P_BORDER_CELLS);

            appendTableSelectorPart();
            builder.append(" > tr > td");
            openDeclarationBlock();
            appendDeclaration("border", pBorderCells);
            closeDeclarationBlock();
        }

        if (isNotEmpty(rules)) {
            appendTableSelectorPart();
            parseDeclarations(TABLE, rules); // table contains is css declarations directly
        }
    }

    private void parseRows(JsonArray rowsConfigArray) {
        // sort the array by index priority
        // * odd / even
        // * header / footer
        // * specific row numbers
        List<JsonObject> arrayEvenOdd = new LinkedList<>();
        List<JsonObject> arrayHeaderFooter = new LinkedList<>();
        List<JsonObject> arraySpecific = new LinkedList<>();

        for (int i = 0; i < rowsConfigArray.length(); i++) {
            JsonObject rowsConfig = rowsConfigArray.getObject(i);

            JsonType indexJsonType = Objects.requireNonNull(rowsConfig.get(INDEX).getType());

            if (indexJsonType == JsonType.NUMBER) {
                arraySpecific.add(rowsConfig);
            } else if (indexJsonType == JsonType.STRING) {
                String index = rowsConfig.getString(INDEX);
                if (index.startsWith("0n")) {
                    arrayHeaderFooter.add(rowsConfig);
                } else if(index.startsWith("2n")){
                    arrayEvenOdd.add(rowsConfig);
                } else {
                    arraySpecific.add(rowsConfig);
                }
            } else {
                throw new IllegalStateException("Unexpected value: " + indexJsonType);
            }
        }

        LinkedList<JsonObject> objects = new LinkedList<>(arrayEvenOdd);
        objects.addAll(arrayHeaderFooter);
        objects.addAll(arraySpecific);

        for (JsonObject rowsConfig : objects) {
            JsonObject declarations = rowsConfig.getObject(DECLARATIONS);

            if (isNotEmpty(declarations)) {
                appendTableSelectorPart();
                builder.append(" > tr");
                appendIndex(rowsConfig);
                builder.append(" > td"); // important to allow rows "override" columns

                parseDeclarations(ROWS, declarations);
            }
        }
    }

    private void parseCols(JsonArray colsConfigArray) {

        for (int i = 0; i < colsConfigArray.length(); i++) {
            JsonObject colsConfig = colsConfigArray.getObject(i);
            JsonObject declarations = colsConfig.getObject(DECLARATIONS);

            if (isNotEmpty(declarations)) {
                appendTableSelectorPart();
                builder.append(" > tr > td");
                appendIndex(colsConfig);
                parseDeclarations(COLUMNS, declarations);
            }
        }
    }

    private void parseCells(JsonArray cellsArray) {

        for (int i = 0; i < cellsArray.length(); i++) {
            JsonObject cellConfig = cellsArray.getObject(i);

            appendTableSelectorPart();
            builder.append(" > ").append("tr");
            appendXY(CELL_X, cellConfig);
            builder.append(" > ").append("td");
            appendXY(CELL_Y, cellConfig);

            parseDeclarations(CELLS, cellConfig.getObject(DECLARATIONS));
        }
    }

    private static boolean isNotEmpty(JsonObject object) {
        return object != null && object.keys().length > 0;
    }

    private void parseDeclarations(String ruleKey, JsonObject declarations) {
        openDeclarationBlock();
        for (String property : declarations.keys()) {
            if (!ALLOWED_PROPERTIES.get(ruleKey).contains(property)) {
                throw new IllegalStateException("Unsupported property " + property + " for type " + ruleKey);
            }

            String value = Objects.requireNonNull(declarations.getString(property), "null properties are not allowed!");

            String cssProperty = mapToCss(property);
            appendDeclaration(cssProperty, value);
        }
        closeDeclarationBlock();
    }

    private StringBuilder closeDeclarationBlock() {
        return builder.append("}\n\n");
    }

    private void openDeclarationBlock() {
        builder.append(" {\n");
    }

    private void appendDeclaration(String cssProperty, String value) {
        builder.append("    ")
                .append(cssProperty)
                .append(": ")
                .append(value)
                .append(";\n");
    }

    private void appendTableSelectorPart() {
        builder.append("table.").append(currentTemplateName);
    }

    private void appendIndex(JsonObject declarationDef) {
        if (declarationDef.hasKey(INDEX)) {
            boolean fromBottom = declarationDef.hasKey(FROM_BOTTOM) && declarationDef.getBoolean(FROM_BOTTOM);
            String nth = fromBottom
                    ? "nth-last-of-type"
                    : "nth-of-type";

            // since index can also be something like "2n + 1" (odd children) we interprete it as string
            JsonType indexJsonType = declarationDef.get(INDEX).getType();

            String index = switch (indexJsonType) {
                case NUMBER -> String.valueOf(declarationDef.getNumber(INDEX));
                case STRING -> declarationDef.getString(INDEX);
                default -> throw new IllegalStateException("Unexpected value: " + indexJsonType);
            };

            builder.append(":")
                    .append(nth)
                    .append("(")
                    .append(index)
                    .append(")");
        }
    }

    private void appendXY(String key, JsonObject declarationDef) {
        if (declarationDef.hasKey(key)) {
            builder.append(":")
                    .append("nth-of-type")
                    .append("(")
                    .append((int) declarationDef.getNumber(key)) // we have concrete coordinates, so always number
                    .append(")");
        }
    }

    private static String mapToCss(String propertyKey) {
        switch (propertyKey) {
            case P_BACKGROUND:
                return "background-color";
            case P_COLOR:
                return "color";
            case P_WIDTH:
                return "width";
            case P_HEIGHT:
                return "height";
            case P_BORDER:
                return "border";
            default:
                throw new IllegalStateException("Unsupported property key: " + propertyKey);
        }
    }

    /**
     * Checks, if the given template name is valid. Checks again the static constant {@code PATTERN_TEMPLATE_NAME}.
     * @param templateName template name
     * @return matches the pattern
     */
    public static boolean isValidTemplateName(String templateName) {
        return PATTERN_TEMPLATE_NAME.asMatchPredicate().test(templateName);
    }

//    // for later use maybe?
//    public static boolean isValidPropertyValue(String property, String value) {
//        if (value.contains(":") || value.contains(";") || value.contains("{") || value.contains("}")) {
//            return false;
//        }
//
////        switch (property) {
////            case P_BACKGROUND:
////            case P_COLOR:
////                return isValidColor(value);
////            case P_WIDTH:
////            case P_MIN_WIDTH:
////            case P_MAX_WIDTH:
////                return isValidSize(value);
////                case P_BORDER:
////        }
////
////        return false;
//
//        return true;
//    }

    // for later use maybe?
    private static boolean isValidSize(String value) {
        // TODO implement
        return true;
    }

    // for later use maybe?
    public static boolean isValidColor(String color) {
        return PATTERN_P_COLOR_1.asMatchPredicate().test(color)
               || PATTERN_P_COLOR_2.asMatchPredicate().test(color)
               || PATTERN_P_COLOR_3.asMatchPredicate().test(color)
               || PATTERN_P_COLOR_4.asMatchPredicate().test(color);
    }

    /**
     * Removes empty children from the given json object. Works recursivley and modifies the given object directly.
     * @param container container
     */
    @SuppressWarnings("RedundantCollectionOperation")
    public static void removeEmptyChildren(JsonObject container) {
        List<String> keys = Arrays.asList(container.keys()); // copy of keys since we remove them
        for (String key : keys) {
            JsonValue value = container.get(key);
            if (value instanceof JsonArray) {
                JsonArray array = (JsonArray) value;

                for (int i = array.length() - 1; i >= 0; i--) {
                    JsonObject arrayChild = array.getObject(i);
                    if (arrayChild.hasKey(DECLARATIONS)) {
                        JsonObject declarations = arrayChild.getObject(DECLARATIONS);
                        removeEmptyChildren(declarations);
                        if (declarations.keys().length == 0) {
                            array.remove(i);
                        }
                    }
                }

                if (array.length() == 0) {
                    container.remove(key);
                }
            } else if (value instanceof JsonObject) {
                removeEmptyChildren((JsonObject) value);
                if (((JsonObject) value).keys().length == 0) {
                    container.remove(key);
                }
            } else if (value != null) {
                JsonType type = value.getType();
                if (type == JsonType.STRING && StringUtils.trimToNull(value.asString()) == null) {
                    container.remove(key);
                } else if (type == JsonType.NULL) {
                    container.remove(key);
                }

                // TODO implement other types if needed (but atM we just have Strings)
            }
        }
    }

    /**
     * Searches for a row or column with the given css index inside the given array.
     * <p/>
     * The first parameter is the array to search. The second parameter needs to be a valid css index for
     * {@code :nth-of-type} (and related selectors), e.g. "1", "2n", "even", and so on. The third parameter
     * indicates, if the index should be interpreted as the {@code :last-of-type} selector ({@code true}) or
     * the default {@code :nth-of-type} selector.
     * @param array array to search
     * @param index index to lookup
     * @param indexFromBottom search for normal or "last of" selectors
     * @return json object or null
     */
    public static JsonObject searchForIndexedObject(JsonArray array, String index, boolean indexFromBottom) {
        if (array != null) {
            for (int i = 0; i < array.length(); i++) {
                JsonObject object = array.getObject(i);
                if ((!indexFromBottom || (object.hasKey(FROM_BOTTOM) && object.getBoolean(FROM_BOTTOM))) && index.equals(object.getString(INDEX))) {
                    return object;
                }
            }
        }

        return null;
    }
}