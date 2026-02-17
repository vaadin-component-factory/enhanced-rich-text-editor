package com.vaadin.componentfactory.erte.tables.templates;

import com.vaadin.flow.internal.JacksonUtils;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.util.*;

import static com.vaadin.componentfactory.erte.tables.templates.TemplateJsonConstants.*;

/**
 * Parses an Tables addon Css JSON Template and creates a css stylesheet out of it.
 */
public final class TemplateParser {
    private final ObjectNode templates;
    private StringBuilder builder;
    private String currentTemplateName;

    public static String convertToCss(ObjectNode templates) {
        return new TemplateParser(templates).toCss();
    }

    public static String convertToCss(String templateJson) {
        return new TemplateParser((ObjectNode) JacksonUtils.readTree(templateJson)).toCss();
    }

    public static ObjectNode parseJson(String templateJson) {
        return (ObjectNode) JacksonUtils.readTree(templateJson);
    }

    public static ObjectNode parseJson(String templateJson, boolean removeEmptyChildren) {
        ObjectNode object = parseJson(templateJson);
        if (removeEmptyChildren) {
            removeEmptyChildren(object);
        }
        return object;
    }

    public TemplateParser(ObjectNode templates) {
        this.templates = templates;
    }

    public String toCss() {
        builder = new StringBuilder();
        if (templates != null) {
            for (String templateName : templates.propertyNames()) {
                if (!isValidTemplateId(templateName)) {
                    throw new IllegalStateException(templateName + " is not a legal template name. It must match " + PATTERN_TEMPLATE_ID.pattern());
                }

                currentTemplateName = templateName;
                ObjectNode rules = (ObjectNode) templates.get(templateName);

                if (rules.has(TABLE)) { parseTable((ObjectNode) rules.get(TABLE)); }
                if (rules.has(COLUMNS)) { parseCols((ArrayNode) rules.get(COLUMNS)); }
                if (rules.has(ROWS)) { parseRows((ArrayNode) rules.get(ROWS)); }
                if (rules.has(CELLS)) { parseCells((ArrayNode) rules.get(CELLS)); }
            }
        }
        return builder.toString().trim();
    }

    private void parseTable(ObjectNode rules) {
        if(rules.has(P_BORDER_CELLS)) {
            String pBorderCells = rules.get(P_BORDER_CELLS).asText();
            rules.remove(P_BORDER_CELLS);
            appendTableSelectorPart();
            builder.append(" > tr > td");
            openDeclarationBlock();
            appendDeclaration("border", pBorderCells);
            closeDeclarationBlock();
        }

        if (isNotEmpty(rules)) {
            appendTableSelectorPart();
            parseDeclarations(TABLE, rules);
        }
    }

    private void parseRows(ArrayNode rowsConfigArray) {
        List<ObjectNode> arrayEvenOdd = new LinkedList<>();
        List<ObjectNode> arrayHeaderFooter = new LinkedList<>();
        List<ObjectNode> arraySpecific = new LinkedList<>();

        for (int i = 0; i < rowsConfigArray.size(); i++) {
            ObjectNode rowsConfig = (ObjectNode) rowsConfigArray.get(i);
            JsonNode indexNode = Objects.requireNonNull(rowsConfig.get(INDEX));

            if (indexNode.isNumber()) {
                arraySpecific.add(rowsConfig);
            } else if (indexNode.isTextual()) {
                String index = indexNode.asText();
                if (index.startsWith("0n")) {
                    arrayHeaderFooter.add(rowsConfig);
                } else if(index.startsWith("2n")){
                    arrayEvenOdd.add(rowsConfig);
                } else {
                    arraySpecific.add(rowsConfig);
                }
            } else {
                throw new IllegalStateException("Unexpected value: " + indexNode.getNodeType());
            }
        }

        LinkedList<ObjectNode> objects = new LinkedList<>(arrayEvenOdd);
        objects.addAll(arrayHeaderFooter);
        objects.addAll(arraySpecific);

        for (ObjectNode rowsConfig : objects) {
            ObjectNode declarations = (ObjectNode) rowsConfig.get(DECLARATIONS);
            if (isNotEmpty(declarations)) {
                appendTableSelectorPart();
                builder.append(" > tr");
                appendIndex(rowsConfig);
                builder.append(" > td");
                parseDeclarations(ROWS, declarations);
            }
        }
    }

    private void parseCols(ArrayNode colsConfigArray) {
        for (int i = 0; i < colsConfigArray.size(); i++) {
            ObjectNode colsConfig = (ObjectNode) colsConfigArray.get(i);
            ObjectNode declarations = (ObjectNode) colsConfig.get(DECLARATIONS);

            if (isNotEmpty(declarations)) {
                ObjectNode tdDeclarations = JacksonUtils.createObjectNode();
                if(declarations.has(P_COLOR)) {
                    tdDeclarations.put(P_COLOR, declarations.get(P_COLOR).asText());
                    declarations.remove(P_COLOR);
                }

                appendTableSelectorPart();
                builder.append(" > colgroup > col");
                appendIndex(colsConfig);
                parseDeclarations(COLUMNS, declarations);

                appendTableSelectorPart();
                builder.append(" > tr > td");
                appendIndex(colsConfig);
                parseDeclarations(COLUMNS, tdDeclarations);
            }
        }
    }

    private void parseCells(ArrayNode cellsArray) {
        for (int i = 0; i < cellsArray.size(); i++) {
            ObjectNode cellConfig = (ObjectNode) cellsArray.get(i);
            appendTableSelectorPart();
            builder.append(" > ").append("tr");
            appendXY(CELL_X, cellConfig);
            builder.append(" > ").append("td");
            appendXY(CELL_Y, cellConfig);
            parseDeclarations(CELLS, (ObjectNode) cellConfig.get(DECLARATIONS));
        }
    }

    private static boolean isNotEmpty(ObjectNode object) {
        return object != null && object.size() > 0;
    }

    private void parseDeclarations(String ruleKey, ObjectNode declarations) {
        openDeclarationBlock();
        for (String property : declarations.propertyNames()) {
            if (!ALLOWED_PROPERTIES.get(ruleKey).contains(property)) {
                throw new IllegalStateException("Unsupported property " + property + " for type " + ruleKey);
            }
            String value = Objects.requireNonNull(declarations.get(property).asText(), "null properties are not allowed!");
            String cssProperty = mapToCss(property);
            appendDeclaration(cssProperty, value);
        }
        closeDeclarationBlock();
    }

    private StringBuilder closeDeclarationBlock() { return builder.append("}\n\n"); }
    private void openDeclarationBlock() { builder.append(" {\n"); }

    private void appendDeclaration(String cssProperty, String value) {
        builder.append("    ").append(cssProperty).append(": ").append(value).append(";\n");
    }

    private void appendTableSelectorPart() {
        builder.append("table.").append(currentTemplateName);
    }

    private void appendIndex(ObjectNode declarationDef) {
        if (declarationDef.has(INDEX)) {
            boolean fromBottom = declarationDef.has(FROM_BOTTOM) && declarationDef.get(FROM_BOTTOM).asBoolean();
            String nth = fromBottom ? "nth-last-of-type" : "nth-of-type";

            JsonNode indexNode = declarationDef.get(INDEX);
            String index;
            if (indexNode.isNumber()) {
                index = String.valueOf(indexNode.asDouble());
            } else if (indexNode.isTextual()) {
                index = indexNode.asText();
            } else {
                throw new IllegalStateException("Unexpected value: " + indexNode.getNodeType());
            }

            builder.append(":").append(nth).append("(").append(index).append(")");
        }
    }

    private void appendXY(String key, ObjectNode declarationDef) {
        if (declarationDef.has(key)) {
            builder.append(":").append("nth-of-type").append("(").append(declarationDef.get(key).asInt()).append(")");
        }
    }

    private static String mapToCss(String propertyKey) {
        switch (propertyKey) {
            case P_BACKGROUND: return "background-color";
            case P_COLOR: return "color";
            case P_WIDTH: return "width";
            case P_HEIGHT: return "height";
            case P_BORDER: return "border";
            default: throw new IllegalStateException("Unsupported property key: " + propertyKey);
        }
    }

    public static boolean isValidTemplateId(String templateId) {
        return PATTERN_TEMPLATE_ID.asMatchPredicate().test(templateId);
    }

    private static boolean isValidSize(String value) { return true; }

    public static boolean isValidColor(String color) {
        return PATTERN_P_COLOR_1.asMatchPredicate().test(color)
               || PATTERN_P_COLOR_2.asMatchPredicate().test(color)
               || PATTERN_P_COLOR_3.asMatchPredicate().test(color)
               || PATTERN_P_COLOR_4.asMatchPredicate().test(color);
    }

    @SuppressWarnings("RedundantCollectionOperation")
    public static void removeEmptyChildren(ObjectNode container) {
        List<String> keys = new ArrayList<>(container.propertyNames());
        for (String key : keys) {
            JsonNode value = container.get(key);
            if (value instanceof ArrayNode) {
                ArrayNode array = (ArrayNode) value;
                for (int i = array.size() - 1; i >= 0; i--) {
                    ObjectNode arrayChild = (ObjectNode) array.get(i);
                    if (arrayChild.has(DECLARATIONS)) {
                        ObjectNode declarations = (ObjectNode) arrayChild.get(DECLARATIONS);
                        removeEmptyChildren(declarations);
                        if (declarations.size() == 0) {
                            array.remove(i);
                        }
                    }
                }
                if (array.size() == 0) {
                    container.remove(key);
                }
            } else if (value instanceof ObjectNode) {
                removeEmptyChildren((ObjectNode) value);
                if (((ObjectNode) value).size() == 0) {
                    container.remove(key);
                }
            } else if (value != null) {
                if (value.isTextual()) {
                    String text = value.asText();
                    if (text == null || text.trim().isEmpty()) {
                        container.remove(key);
                    }
                } else if (value.isNull()) {
                    container.remove(key);
                }
            }
        }
    }

    public static ObjectNode searchForIndexedObject(ArrayNode array, String index, boolean indexFromBottom) {
        if (array != null) {
            for (int i = 0; i < array.size(); i++) {
                ObjectNode object = (ObjectNode) array.get(i);
                if ((!indexFromBottom || (object.has(FROM_BOTTOM) && object.get(FROM_BOTTOM).asBoolean())) && index.equals(object.get(INDEX).asText())) {
                    return object;
                }
            }
        }
        return null;
    }

    public static ObjectNode clone(ObjectNode templateToClone) {
        return (ObjectNode) JacksonUtils.readTree(templateToClone.toString());
    }
}
