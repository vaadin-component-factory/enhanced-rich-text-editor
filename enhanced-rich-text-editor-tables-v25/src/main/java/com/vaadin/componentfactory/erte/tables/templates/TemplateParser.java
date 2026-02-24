/*-
 * #%L
 * Enhanced Rich Text Editor Tables Extension V25
 * %%
 * Copyright (C) 2025 Vaadin Ltd
 * %%
 * This program is available under Commercial Vaadin Add-On License 3.0
 * (CVALv3).
 *
 * See the file license.html distributed with this software for more
 * information about licensing.
 *
 * You should have received a copy of the CVALv3 along with this program.
 * If not, see <http://vaadin.com/license/cval-3>.
 * #L%
 */
package com.vaadin.componentfactory.erte.tables.templates;

import org.slf4j.LoggerFactory;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.JsonNodeType;
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

    /**
     * Parses the given template and creates a css string out of it.
     * <p/>
     * The given object will NOT be modified in any way beforehand. If you want
     * to strip empty children of it, please apply the method {@link #removeEmptyChildren(ObjectNode)} manually
     * beforehand.
     * @param templates templates
     * @return css string
     */
    public static String convertToCss(ObjectNode templates) {
        return templates == null ? "" : new TemplateParser(templates).toCss();
    }

    /**
     * Creates a new instance working with the given object. Any future changes to the json object will directly be
     * reflected to this instance and the resulting css. To prevent this you may want to pass a copy of your
     * template object instead.
     * <p/>
     * The given object will NOT be modified in any other way. If you want
     * to strip empty children of it, please apply the method {@link #removeEmptyChildren(ObjectNode)} manually
     * beforehand.
     * @param templates templates
     */
    public TemplateParser(ObjectNode templates) {
        this.templates = templates;
    }

    /**
     * Generates the CSS based on the current state of the templates object.
     * @return css string
     */
    public String toCss() {
        builder = new StringBuilder();
        if (templates != null) {
            for (String templateName : templates.propertyNames()) {
                if (!isValidTemplateId(templateName)) {
                    throw new IllegalStateException(templateName + " is not a legal template name. It must match " + PATTERN_TEMPLATE_ID.pattern());
                }

                currentTemplateName = templateName;

                ObjectNode rules = (ObjectNode) templates.get(templateName);

                // check the rules for the different selectors
                // we could do that in a loop, but by doing it manually we can also
                // specify the order of css rules in the resulting sheet (which has an effect on the resulting look)
                if (rules.has(TABLE)) {
                    parseTable((ObjectNode) rules.get(TABLE));
                }

                // cols before rows makes the rows the overriding style when styles "overlap"
                if (rules.has(COLUMNS)) {
                    parseCols((ArrayNode) rules.get(COLUMNS));
                }

                if (rules.has(ROWS)) {
                    parseRows((ArrayNode) rules.get(ROWS));
                }

                if (rules.has(CELLS)) {
                    parseCells((ArrayNode) rules.get(CELLS));
                }
            }
        }
        return builder.toString().trim();
    }

    private void parseTable(ObjectNode rules) {
        // BORDER_CELLS is a special property, that needs a specialized treatment. It is not to be applied
        // to the table selector directly, but to "all" cells
        if (rules.has(P_BORDER_CELLS)) {
            String pBorderCells = rules.get(P_BORDER_CELLS).asText();
            rules.remove(P_BORDER_CELLS);

            appendTableSelectorPart();
            builder.append(" > tr > td");
            openDeclarationBlock();
            appendDeclaration("border", P_BORDER_CELLS, pBorderCells);
            closeDeclarationBlock();
        }

        if (isNotEmpty(rules)) {
            appendTableSelectorPart();
            parseDeclarations(TABLE, rules); // table contains css declarations directly
        }
    }

    private void parseRows(ArrayNode rowsConfigArray) {
        // sort the array by index priority
        // * odd / even
        // * header / footer
        // * specific row numbers
        List<ObjectNode> arrayEvenOdd = new LinkedList<>();
        List<ObjectNode> arrayHeaderFooter = new LinkedList<>();
        List<ObjectNode> arraySpecific = new LinkedList<>();

        for (int i = 0; i < rowsConfigArray.size(); i++) {
            ObjectNode rowsConfig = (ObjectNode) rowsConfigArray.get(i);

            JsonNodeType indexJsonType = Objects.requireNonNull(rowsConfig.get(INDEX).getNodeType());

            if (indexJsonType == JsonNodeType.NUMBER) {
                arraySpecific.add(rowsConfig);
            } else if (indexJsonType == JsonNodeType.STRING) {
                String index = rowsConfig.get(INDEX).asText();
                if (index.startsWith("0n")) {
                    arrayHeaderFooter.add(rowsConfig);
                } else if (index.startsWith("2n")) {
                    arrayEvenOdd.add(rowsConfig);
                } else {
                    arraySpecific.add(rowsConfig);
                }
            } else {
                throw new IllegalStateException("Unexpected value: " + indexJsonType);
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
                builder.append(" > td"); // important to allow rows "override" columns

                parseDeclarations(ROWS, declarations);
            }
        }
    }

    private void parseCols(ArrayNode colsConfigArray) {
        for (int i = 0; i < colsConfigArray.size(); i++) {
            ObjectNode colsConfig = (ObjectNode) colsConfigArray.get(i);
            ObjectNode declarations = (ObjectNode) colsConfig.get(DECLARATIONS);

            if (isNotEmpty(declarations)) {
                // unfortunately colgroup cols do not support all css properties, and on the other hand
                // tds cannot handle everything. "width" for instance is something only supported in cols, while
                // "color" is only supported by tds.

                ObjectNode tdDeclarations = JsonNodeFactory.instance.objectNode();
                if (declarations.has(P_COLOR)) {
                    tdDeclarations.put(P_COLOR, declarations.get(P_COLOR).asText());
                    declarations.remove(P_COLOR);

                    // extend and improve when other properties are added
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
            appendDeclaration(cssProperty, property, value);
        }
        closeDeclarationBlock();
    }

    private StringBuilder closeDeclarationBlock() {
        return builder.append("}\n\n");
    }

    private void openDeclarationBlock() {
        builder.append(" {\n");
    }

    private void appendDeclaration(String cssProperty, String propertyKey, String value) {
        if (!isValidPropertyValue(propertyKey, value)) {
            LoggerFactory.getLogger(TemplateParser.class)
                .warn("Rejected invalid CSS value for property '{}': '{}'", propertyKey, value);
            return;
        }
        builder.append("    ")
                .append(cssProperty)
                .append(": ")
                .append(value)
                .append(";\n");
    }

    private void appendTableSelectorPart() {
        builder.append("table.").append(currentTemplateName);
    }

    private void appendIndex(ObjectNode declarationDef) {
        if (declarationDef.has(INDEX)) {
            boolean fromBottom = declarationDef.has(FROM_BOTTOM) && declarationDef.get(FROM_BOTTOM).asBoolean();
            String nth = fromBottom
                    ? "nth-last-of-type"
                    : "nth-of-type";

            // since index can also be something like "2n + 1" (odd children) we interpret it as string
            JsonNodeType indexJsonType = declarationDef.get(INDEX).getNodeType();

            String index = switch (indexJsonType) {
                case NUMBER -> String.valueOf((int) declarationDef.get(INDEX).asDouble());
                case STRING -> declarationDef.get(INDEX).asText();
                default -> throw new IllegalStateException("Unexpected value: " + indexJsonType);
            };

            builder.append(":")
                    .append(nth)
                    .append("(")
                    .append(index)
                    .append(")");
        }
    }

    private void appendXY(String key, ObjectNode declarationDef) {
        if (declarationDef.has(key)) {
            builder.append(":")
                    .append("nth-of-type")
                    .append("(")
                    .append((int) declarationDef.get(key).asDouble()) // we have concrete coordinates, so always number
                    .append(")");
        }
    }

    private static String mapToCss(String propertyKey) {
        return switch (propertyKey) {
            case P_BACKGROUND -> "background-color";
            case P_COLOR -> "color";
            case P_WIDTH -> "width";
            case P_HEIGHT -> "height";
            case P_BORDER -> "border";
            default -> throw new IllegalStateException("Unsupported property key: " + propertyKey);
        };
    }

    /**
     * Checks, if the given template id is valid. Checks against the static constant {@code PATTERN_TEMPLATE_ID}.
     * @param templateId template id
     * @return matches the pattern
     */
    public static boolean isValidTemplateId(String templateId) {
        return PATTERN_TEMPLATE_ID.asMatchPredicate().test(templateId);
    }

    /**
     * Validates a CSS property value for security and format correctness.
     * @param property property key (e.g., P_BACKGROUND, P_BORDER)
     * @param value value to validate
     * @return true if valid, false otherwise
     */
    public static boolean isValidPropertyValue(String property, String value) {
        if (value == null) return true;

        // Reject values containing CSS injection characters
        if (value.contains(":") || value.contains(";") || value.contains("{") || value.contains("}") ||
            value.contains("/*") || value.contains("*/")) {
            return false;
        }

        return switch (property) {
            case P_BACKGROUND, P_COLOR -> TemplateJsonConstants.isValidColor(value);
            case P_WIDTH, P_HEIGHT -> isValidSize(value);
            case P_BORDER, P_BORDER_CELLS -> isValidBorder(value);
            default -> false;
        };
    }

    /**
     * Validates a CSS size value (e.g., "100px", "50%", "auto").
     * @param value value to validate
     * @return true if valid, false otherwise
     */
    private static boolean isValidSize(String value) {
        return Dimension.STRING_PATTERN.matcher(value.trim()).matches()
            || Set.of("auto", "fit-content", "min-content", "max-content").contains(value.trim());
    }

    /**
     * Validates a CSS border value.
     * @param value value to validate
     * @return true if valid, false otherwise
     */
    private static boolean isValidBorder(String value) {
        // Reject function calls and quotes
        if (value.contains("(") || value.contains(")") || value.contains("\"") || value.contains("'")) {
            return false;
        }

        String[] parts = value.trim().split("\\s+");
        if (parts.length < 1 || parts.length > 3) return false;

        Set<String> validStyles = Set.of("none", "hidden", "dotted", "dashed", "solid",
            "double", "groove", "ridge", "inset", "outset");
        return Arrays.stream(parts).anyMatch(validStyles::contains);
    }

    /**
     * Removes empty children from the given json object. Works recursively and modifies the given object directly.
     * @param container container
     */
    public static void removeEmptyChildren(ObjectNode container) {
        // Copy property names to list since we'll be modifying the container
        List<String> keys = new ArrayList<>(container.propertyNames());

        for (String key : keys) {
            JsonNode value = container.get(key);
            if (value.isArray()) {
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
            } else if (value.isObject()) {
                removeEmptyChildren((ObjectNode) value);
                if (((ObjectNode) value).size() == 0) {
                    container.remove(key);
                }
            } else if (value != null) {
                if (value.isTextual() && (value.asText() == null || value.asText().isBlank())) {
                    container.remove(key);
                } else if (value.isNull()) {
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
    public static ObjectNode searchForIndexedObject(ArrayNode array, String index, boolean indexFromBottom) {
        if (array != null) {
            for (int i = 0; i < array.size(); i++) {
                ObjectNode object = (ObjectNode) array.get(i);
                if ((!indexFromBottom || (object.has(FROM_BOTTOM) && object.get(FROM_BOTTOM).asBoolean()))
                    && index.equals(object.get(INDEX).asText())) {
                    return object;
                }
            }
        }

        return null;
    }

    /**
     * Clones the given template. The returned template will not be modified in any other way.
     * @param templateToClone template to be cloned
     * @return cloned instance
     */
    public static ObjectNode clone(ObjectNode templateToClone) {
        return templateToClone != null ? templateToClone.deepCopy() : null;
    }
}
