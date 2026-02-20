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

import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;
import elemental.json.JsonType;
import elemental.json.JsonValue;

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
                JsonObject obj = Json.parse(converted);
                return obj.getArray("ops").toJson();
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

        JsonObject oldDelta = Json.parse(oldDeltaJson);
        JsonArray oldOps = oldDelta.getArray("ops");

        if (oldOps == null) {
            return oldDeltaJson;
        }

        JsonArray newOps = Json.createArray();

        for (int i = 0; i < oldOps.length(); i++) {
            JsonObject op = oldOps.getObject(i);
            convertOp(op, newOps);
        }

        JsonObject newDelta = Json.createObject();
        newDelta.put("ops", newOps);
        return newDelta.toJson();
    }

    private static void convertOp(JsonObject op, JsonArray newOps) {
        JsonValue insertValue = op.get("insert");
        JsonObject attributes = op.hasKey("attributes") ? op.getObject("attributes") : null;

        // Handle tab attribute (old format with level)
        if (attributes != null && attributes.hasKey("tab")) {
            String tabValue = attributes.getString("tab");
            int tabCount = parseTabLevel(tabValue);

            // Create separate tab embeds for each tab level
            for (int t = 0; t < tabCount; t++) {
                JsonObject tabEmbed = Json.createObject();
                tabEmbed.put("tab", true);

                JsonObject tabOp = Json.createObject();
                tabOp.put("insert", tabEmbed);
                newOps.set(newOps.length(), tabOp);
            }
            return;
        }

        // Handle pre-tab (temporary tab, convert to single tab)
        if (attributes != null && attributes.hasKey("pre-tab")) {
            JsonObject tabEmbed = Json.createObject();
            tabEmbed.put("tab", true);

            JsonObject tabOp = Json.createObject();
            tabOp.put("insert", tabEmbed);
            newOps.set(newOps.length(), tabOp);
            return;
        }

        // Handle line-part attribute
        if (attributes != null && attributes.hasKey("line-part")) {
            String text = insertValue.getType() == JsonType.STRING ? insertValue.asString() : null;

            // If it's just a zero-width space, remove completely
            if (ZERO_WIDTH_SPACE.equals(text)) {
                return;
            }

            // Otherwise, keep the text but remove line-part attribute
            JsonObject newOp = Json.createObject();
            newOp.put("insert", text);

            // Copy other attributes except line-part
            JsonObject remainingAttributes = removeAttribute(attributes, "line-part");
            if (remainingAttributes != null && remainingAttributes.keys().length > 0) {
                newOp.put("attributes", remainingAttributes);
            }

            newOps.set(newOps.length(), newOp);
            return;
        }

        // Handle tabs-cont block format (convert to normal newline)
        if (attributes != null && attributes.hasKey("tabs-cont")) {
            JsonObject newOp = Json.createObject();
            newOp.put("insert", "\n");

            // Copy other attributes except tabs-cont
            JsonObject remainingAttributes = removeAttribute(attributes, "tabs-cont");
            if (remainingAttributes != null && remainingAttributes.keys().length > 0) {
                newOp.put("attributes", remainingAttributes);
            }

            newOps.set(newOps.length(), newOp);
            return;
        }

        // Pass through all other ops unchanged (readonly, nbsp, placeholder, standard formatting)
        newOps.set(newOps.length(), copyJsonObject(op));
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

    private static JsonObject removeAttribute(JsonObject attributes, String attributeToRemove) {
        JsonObject result = Json.createObject();
        for (String key : attributes.keys()) {
            if (!key.equals(attributeToRemove)) {
                result.put(key, copyJsonValue(attributes.get(key)));
            }
        }
        return result;
    }

    private static JsonObject copyJsonObject(JsonObject original) {
        JsonObject copy = Json.createObject();
        for (String key : original.keys()) {
            copy.put(key, copyJsonValue(original.get(key)));
        }
        return copy;
    }

    private static JsonValue copyJsonValue(JsonValue value) {
        if (value == null) {
            return Json.createNull();
        }

        switch (value.getType()) {
            case STRING:
                return Json.create(value.asString());
            case NUMBER:
                return Json.create(value.asNumber());
            case BOOLEAN:
                return Json.create(value.asBoolean());
            case OBJECT:
                return copyJsonObject((JsonObject) value);
            case ARRAY:
                JsonArray arrayCopy = Json.createArray();
                JsonArray original = (JsonArray) value;
                for (int i = 0; i < original.length(); i++) {
                    arrayCopy.set(i, copyJsonValue(original.get(i)));
                }
                return arrayCopy;
            case NULL:
            default:
                return Json.createNull();
        }
    }
}
