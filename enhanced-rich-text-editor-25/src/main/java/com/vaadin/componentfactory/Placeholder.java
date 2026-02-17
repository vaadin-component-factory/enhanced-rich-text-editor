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

import com.vaadin.flow.component.JsonSerializable;
import com.vaadin.flow.internal.JacksonUtils;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

public class Placeholder implements JsonSerializable {
    private String text;
    private ObjectNode format;
    private ObjectNode altFormat;
    private int index = -1;

    public Placeholder() {
        altFormat = JacksonUtils.createObjectNode();
        format = JacksonUtils.createObjectNode();
    }

    /**
     * Construct Placeholder from a JsonNode
     *
     * @param placeholder
     *            Placeholder as ObjectNode
     */
    public Placeholder(ObjectNode placeholder) {
        readJson(placeholder);
    }

    public String getText() {
        return text;
    }

    /**
     * Set placeholder text
     *
     * @param text
     *            The placeholder text
     */
    public void setText(String text) {
        this.text = text;
    }

    public ObjectNode getFormat() {
        return format;
    }

    /**
     * Set the format for placeholder appearence
     *
     * @param format
     *            Format in Quill format as ObjectNode, like "{ bold: true,
     *            italic: false }"
     */
    public void setFormat(ObjectNode format) {
        this.format = format;
    }

    public ObjectNode getAltFormat() {
        return altFormat;
    }

    /**
     * Set the format for placeholder alternative appearence
     *
     * @param altFormat
     *            Format in Quill format as ObjectNode, like "{ underline: true,
     *            bold: false }"
     */
    public void setAltFormat(ObjectNode altFormat) {
        this.altFormat = altFormat;
    }

    @Override
    public ObjectNode toJson() {
        ObjectNode obj = JacksonUtils.createObjectNode();
        obj.put("text", getText());
        obj.set("format", getFormat());
        obj.set("altFormat", getAltFormat());
        return obj;
    }

    @Override
    public JsonSerializable readJson(JsonNode placeholder) {
        this.text = placeholder.has("text") ? placeholder.get("text").asText()
                : null;
        this.format = placeholder.has("format")
                ? (ObjectNode) placeholder.get("format")
                : null;
        this.altFormat = placeholder.has("altFormat")
                ? (ObjectNode) placeholder.get("altFormat")
                : null;
        return this;
    }

    /**
     * Get the last insertion index of the placeholder, if there are multiple
     * occurrences of the placeholder. Populated in
     * PlaceholderBeforeInsertEvent.
     *
     * @return int value
     */
    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }
}
