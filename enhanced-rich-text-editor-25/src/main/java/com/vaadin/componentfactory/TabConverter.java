package com.vaadin.componentfactory;

/*
 * #%L
 * EnhancedRichTextEditor for Vaadin 10
 * %%
 * Copyright (C) 2017 - 2019 Vaadin Ltd
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

import com.vaadin.flow.internal.JacksonUtils;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.NullNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * Converter for transforming old ERTE tab delta format to the new prototype format.
 * <p>
 * This class handles the conversion of legacy tab-related blots (tab, line-part, tabs-cont, pre-tab)
 * to the simplified new format while preserving all other content and formatting.
 */
public class TabConverter {

    private static final String ZERO_WIDTH_SPACE = "\uFEFF";

    /**
     * Detects whether a delta JSON string contains old-format tab blots and converts if needed.
     * Returns the value unchanged if it does not contain old-format markers.
     * Handles both array format ([{...}]) and object format ({"ops":[{...}]}).
     *
     * @param deltaJson the delta JSON string (may be old or new format)
     * @return the converted delta JSON string, or the original if no conversion needed
     */
    public static String convertIfNeeded(String deltaJson) {
        if (deltaJson == null || deltaJson.isBlank()) {
            return deltaJson;
        }
        // Quick string check for old-format markers before parsing JSON
        if (deltaJson.contains("\"tabs-cont\"") || deltaJson.contains("\"pre-tab\"")
                || deltaJson.contains("\"line-part\"")
                || (deltaJson.contains("\"tab\"") && deltaJson.contains("\"tab\":\""))
        ) {
            String trimmed = deltaJson.trim();
            if (trimmed.startsWith("[")) {
                // Array format: wrap in {"ops":...}, convert, unwrap
                String wrapped = "{\"ops\":" + trimmed + "}";
                String converted = convertToNewFormat(wrapped);
                // Extract the ops array back out
                JsonNode obj = JacksonUtils.readTree(converted);
                return obj.get("ops").toString();
            } else {
                return convertToNewFormat(deltaJson);
            }
        }
        return deltaJson;
    }

    /**
     * Converts a delta JSON string from the old ERTE tab format to the new prototype format.
     *
     * @param oldDeltaJson the old delta JSON string
     * @return the converted delta JSON string in the new format
     */
    public static String convertToNewFormat(String oldDeltaJson) {
        if (oldDeltaJson == null || oldDeltaJson.isBlank()) {
            return oldDeltaJson;
        }

        JsonNode oldDelta = JacksonUtils.readTree(oldDeltaJson);
        ArrayNode oldOps = (ArrayNode) oldDelta.get("ops");

        if (oldOps == null) {
            return oldDeltaJson;
        }

        ArrayNode newOps = JacksonUtils.createArrayNode();

        for (int i = 0; i < oldOps.size(); i++) {
            ObjectNode op = (ObjectNode) oldOps.get(i);
            convertOp(op, newOps);
        }

        ObjectNode newDelta = JacksonUtils.createObjectNode();
        newDelta.set("ops", newOps);
        return newDelta.toString();
    }

    private static void convertOp(ObjectNode op, ArrayNode newOps) {
        JsonNode insertValue = op.get("insert");
        ObjectNode attributes = op.has("attributes") ? (ObjectNode) op.get("attributes") : null;

        // Handle tab attribute (old format with level)
        if (attributes != null && attributes.has("tab")) {
            String tabValue = attributes.get("tab").asText();
            int tabCount = parseTabLevel(tabValue);

            // Create separate tab embeds for each tab level
            for (int t = 0; t < tabCount; t++) {
                ObjectNode tabEmbed = JacksonUtils.createObjectNode();
                tabEmbed.put("tab", true);

                ObjectNode tabOp = JacksonUtils.createObjectNode();
                tabOp.set("insert", tabEmbed);
                newOps.add(tabOp);
            }
            return;
        }

        // Handle pre-tab (temporary tab, convert to single tab)
        if (attributes != null && attributes.has("pre-tab")) {
            ObjectNode tabEmbed = JacksonUtils.createObjectNode();
            tabEmbed.put("tab", true);

            ObjectNode tabOp = JacksonUtils.createObjectNode();
            tabOp.set("insert", tabEmbed);
            newOps.add(tabOp);
            return;
        }

        // Handle line-part attribute
        if (attributes != null && attributes.has("line-part")) {
            String text = insertValue.isTextual() ? insertValue.asText() : null;

            // If it's just a zero-width space, remove completely
            if (ZERO_WIDTH_SPACE.equals(text)) {
                return;
            }

            // Otherwise, keep the text but remove line-part attribute
            ObjectNode newOp = JacksonUtils.createObjectNode();
            newOp.put("insert", text);

            // Copy other attributes except line-part
            ObjectNode remainingAttributes = removeAttribute(attributes, "line-part");
            if (remainingAttributes != null && remainingAttributes.size() > 0) {
                newOp.set("attributes", remainingAttributes);
            }

            newOps.add(newOp);
            return;
        }

        // Handle tabs-cont block format (convert to normal newline)
        if (attributes != null && attributes.has("tabs-cont")) {
            ObjectNode newOp = JacksonUtils.createObjectNode();
            newOp.put("insert", "\n");

            // Copy other attributes except tabs-cont
            ObjectNode remainingAttributes = removeAttribute(attributes, "tabs-cont");
            if (remainingAttributes != null && remainingAttributes.size() > 0) {
                newOp.set("attributes", remainingAttributes);
            }

            newOps.add(newOp);
            return;
        }

        // Pass through all other ops unchanged (readonly, nbsp, placeholder, standard formatting)
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

    private static ObjectNode removeAttribute(ObjectNode attributes, String attributeToRemove) {
        ObjectNode result = JacksonUtils.createObjectNode();
        for (String key : attributes.propertyNames()) {
            if (!key.equals(attributeToRemove)) {
                result.set(key, attributes.get(key).deepCopy());
            }
        }
        return result;
    }
}
