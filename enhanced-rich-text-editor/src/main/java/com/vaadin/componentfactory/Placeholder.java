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

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import com.vaadin.flow.internal.JacksonUtils;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * Represents a placeholder token that can be inserted into the
 * Enhanced Rich Text Editor.
 */
public class Placeholder implements Serializable {

    private String text;
    private Map<String, Object> format = new LinkedHashMap<>();
    private Map<String, Object> altFormat = new LinkedHashMap<>();
    private int index = -1;

    public Placeholder() {
    }

    public Placeholder(String text) {
        this();
        this.text = text;
    }

    /**
     * Construct Placeholder from a Jackson JsonNode (for @EventData parsing).
     *
     * @param json the JSON node representing a placeholder
     */
    public Placeholder(JsonNode json) {
        this();
        if (json == null) return;
        if (json.has("text")) {
            text = json.get("text").asText();
        }
        if (json.has("format") && json.get("format").isObject()) {
            json.get("format").properties()
                    .forEach(e -> format.put(e.getKey(),
                            nodeToValue(e.getValue())));
        }
        if (json.has("altFormat") && json.get("altFormat").isObject()) {
            json.get("altFormat").properties()
                    .forEach(e -> altFormat.put(e.getKey(),
                            nodeToValue(e.getValue())));
        }
    }

    private static Object nodeToValue(JsonNode n) {
        if (n.isBoolean()) return n.asBoolean();
        if (n.isInt()) return n.asInt();
        if (n.isDouble()) return n.asDouble();
        return n.asText();
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    /**
     * Get the format for placeholder appearance.
     *
     * @return format map (e.g. "italic" -> true, "bold" -> true)
     */
    public Map<String, Object> getFormat() {
        return format;
    }

    public void setFormat(Map<String, Object> format) {
        this.format = format;
    }

    /**
     * Get the format for placeholder alternative appearance.
     *
     * @return alt format map
     */
    public Map<String, Object> getAltFormat() {
        return altFormat;
    }

    public void setAltFormat(Map<String, Object> altFormat) {
        this.altFormat = altFormat;
    }

    /**
     * Get the last insertion index of the placeholder, populated in events.
     *
     * @return index value, or -1 if not set
     */
    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    /**
     * Serialize this placeholder to a Jackson ObjectNode.
     *
     * @return JSON representation
     */
    public ObjectNode toJson() {
        ObjectNode obj = JacksonUtils.getMapper().createObjectNode();
        obj.put("text", text);
        ObjectNode fmtNode = obj.putObject("format");
        format.forEach((k, v) -> putTypedValue(fmtNode, k, v));
        ObjectNode altFmtNode = obj.putObject("altFormat");
        altFormat.forEach((k, v) -> putTypedValue(altFmtNode, k, v));
        return obj;
    }

    private static void putTypedValue(ObjectNode node, String key, Object v) {
        if (v instanceof Boolean b) node.put(key, b);
        else if (v instanceof Integer i) node.put(key, i);
        else if (v instanceof Double d) node.put(key, d);
        else node.put(key, String.valueOf(v));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Placeholder p)) return false;
        return Objects.equals(text, p.text);
    }

    @Override
    public int hashCode() {
        return Objects.hash(text);
    }
}
