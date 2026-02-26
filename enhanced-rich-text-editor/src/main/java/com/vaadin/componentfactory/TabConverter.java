/*-
 * #%L
 * Enhanced Rich Text Editor V25
 * %%
 * Copyright (C) 2019 - 2025 Vaadin Ltd
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
package com.vaadin.componentfactory;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * Converter for transforming old ERTE tab delta format (pre-5.2.0) to the
 * current format.
 * <p>
 * This class handles the conversion of legacy tab-related blots
 * ({@code tab}, {@code line-part}, {@code tabs-cont}, {@code pre-tab})
 * to the simplified current format while preserving all other content and
 * formatting.
 * <p>
 * <b>Breaking change in ERTE 6.0:</b> This converter is no longer applied
 * automatically in {@code setValue()}. Applications that store deltas created
 * with ERTE versions prior to 5.2.0 must call {@link #convertIfNeeded(String)}
 * explicitly before passing them to the editor.
 *
 * @since 5.2.0
 */
public class TabConverter {

    private static final String ZERO_WIDTH_SPACE = "\uFEFF";
    private static final JsonMapper MAPPER = JsonMapper.shared();

    private TabConverter() {
        // Utility class
    }

    /**
     * Detects whether a delta JSON string contains old-format tab blots and
     * converts if needed. Returns the value unchanged if it does not contain
     * old-format markers.
     * <p>
     * Handles both array format ({@code [{...}]}) and object format
     * ({@code {"ops":[{...}]}}).
     *
     * @param deltaJson the delta JSON string (may be old or new format)
     * @return the converted delta JSON string, or the original if no
     *         conversion needed
     */
    public static String convertIfNeeded(String deltaJson) {
        if (deltaJson == null || deltaJson.isBlank()) {
            return deltaJson;
        }
        // Quick string check for old-format markers before parsing JSON
        if (deltaJson.contains("\"tabs-cont\"")
                || deltaJson.contains("\"pre-tab\"")
                || deltaJson.contains("\"line-part\"")
                || (deltaJson.contains("\"tab\"")
                        && deltaJson.contains("\"tab\":\""))) {
            try {
                String trimmed = deltaJson.trim();
                if (trimmed.startsWith("[")) {
                    // Array format: wrap in {"ops":...}, convert, unwrap
                    String wrapped = "{\"ops\":" + trimmed + "}";
                    String converted = convertToNewFormat(wrapped);
                    JsonNode obj = MAPPER.readTree(converted);
                    return MAPPER.writeValueAsString(obj.get("ops"));
                } else {
                    return convertToNewFormat(deltaJson);
                }
            } catch (Exception e) {
                // If JSON parsing fails, return unchanged
                return deltaJson;
            }
        }
        return deltaJson;
    }

    /**
     * Converts a delta JSON string from the old ERTE tab format to the new
     * format.
     *
     * @param oldDeltaJson the old delta JSON string
     * @return the converted delta JSON string in the new format
     */
    public static String convertToNewFormat(String oldDeltaJson) {
        if (oldDeltaJson == null || oldDeltaJson.isBlank()) {
            return oldDeltaJson;
        }

        try {
            JsonNode oldDelta = MAPPER.readTree(oldDeltaJson);
            JsonNode oldOps = oldDelta.get("ops");

            if (oldOps == null || !oldOps.isArray()) {
                return oldDeltaJson;
            }

            ArrayNode newOps = MAPPER.createArrayNode();

            for (JsonNode op : oldOps) {
                convertOp(op, newOps);
            }

            ObjectNode newDelta = MAPPER.createObjectNode();
            newDelta.set("ops", newOps);
            return MAPPER.writeValueAsString(newDelta);
        } catch (Exception e) {
            return oldDeltaJson;
        }
    }

    private static void convertOp(JsonNode op, ArrayNode newOps) {
        JsonNode insertValue = op.get("insert");
        JsonNode attributes = op.get("attributes");

        // Handle tab attribute (old format with level)
        if (attributes != null && attributes.has("tab")) {
            String tabValue = attributes.get("tab").asText();
            int tabCount = parseTabLevel(tabValue);

            for (int t = 0; t < tabCount; t++) {
                ObjectNode tabOp = MAPPER.createObjectNode();
                ObjectNode tabEmbed = MAPPER.createObjectNode();
                tabEmbed.put("tab", true);
                tabOp.set("insert", tabEmbed);
                newOps.add(tabOp);
            }
            return;
        }

        // Handle pre-tab (temporary tab, convert to single tab)
        if (attributes != null && attributes.has("pre-tab")) {
            ObjectNode tabOp = MAPPER.createObjectNode();
            ObjectNode tabEmbed = MAPPER.createObjectNode();
            tabEmbed.put("tab", true);
            tabOp.set("insert", tabEmbed);
            newOps.add(tabOp);
            return;
        }

        // Handle line-part attribute
        if (attributes != null && attributes.has("line-part")) {
            String text = insertValue != null && insertValue.isTextual()
                    ? insertValue.asText() : null;

            // If it's just a zero-width space, remove completely
            if (ZERO_WIDTH_SPACE.equals(text)) {
                return;
            }

            // Otherwise, keep the text but remove line-part attribute
            ObjectNode newOp = MAPPER.createObjectNode();
            newOp.put("insert", text);

            // Copy other attributes except line-part
            ObjectNode remaining = removeAttribute(attributes, "line-part");
            if (remaining != null && !remaining.isEmpty()) {
                newOp.set("attributes", remaining);
            }

            newOps.add(newOp);
            return;
        }

        // Handle tabs-cont block format (convert to normal newline)
        if (attributes != null && attributes.has("tabs-cont")) {
            ObjectNode newOp = MAPPER.createObjectNode();
            newOp.put("insert", "\n");

            // Copy other attributes except tabs-cont
            ObjectNode remaining = removeAttribute(attributes, "tabs-cont");
            if (remaining != null && !remaining.isEmpty()) {
                newOp.set("attributes", remaining);
            }

            newOps.add(newOp);
            return;
        }

        // Pass through all other ops unchanged
        newOps.add(op.deepCopy());
    }

    private static int parseTabLevel(String tabValue) {
        if ("true".equals(tabValue)) {
            return 1;
        }
        try {
            return Integer.parseInt(tabValue);
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    private static ObjectNode removeAttribute(JsonNode attributes,
            String attributeToRemove) {
        ObjectNode result = MAPPER.createObjectNode();
        var it = attributes.properties().iterator();
        while (it.hasNext()) {
            var entry = it.next();
            if (!entry.getKey().equals(attributeToRemove)) {
                result.set(entry.getKey(), entry.getValue().deepCopy());
            }
        }
        return result;
    }
}
