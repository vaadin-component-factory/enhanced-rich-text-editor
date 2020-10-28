package com.vaadin.componentfactory;

import com.vaadin.flow.component.JsonSerializable;
import com.vaadin.flow.internal.JsonSerializer;

import elemental.json.JsonObject;
import elemental.json.impl.JreJsonFactory;
import elemental.json.impl.JreJsonObject;

public class Placeholder implements JsonSerializable {
	private String text;
	private JsonObject format;
	private JsonObject altFormat;

	
	public Placeholder() {
        JreJsonFactory factory = new JreJsonFactory();
        altFormat = new JreJsonObject(factory);
        format = new JreJsonObject(factory);
	}

	/**
	 * Construct Placeholder from a JsonObject
	 * 
	 * @param placeholder Placeholder as JsonObject
	 */
	public Placeholder(JsonObject placeholder) {
		readJson(placeholder);
	}

	public String getText() {
		return text;
	}

	/**
	 * Set placeholder text
	 * 
	 * @param text The placeholder text
	 */
	public void setText(String text) {
		this.text = text;
	}

	public JsonObject getFormat() {
		return format;
	}

	/**
	 * Set the format for placeholder appearence
     *
	 * @param format Format in Quill format as JsonObject, like "{ bold: true, italic: false }"
	 */
	public void setFormat(JsonObject format) {
		this.format = format;
	}

	public JsonObject getAltFormat() {
		return altFormat;
	}

	/**
	 * Set the format for placeholder alternative appearence
     *
	 * @param format Format in Quill format as JsonObject, like "{ underline: true, bold: false }"
	 */
	public void setAltFormat(JsonObject altFormat) {
		this.altFormat = altFormat;
	}
	
	@Override
	public JsonObject toJson() {
        JreJsonFactory factory = new JreJsonFactory();
        JsonObject obj = new JreJsonObject(factory);
        obj.put("text", getText());
        obj.put("format", getFormat());
        obj.put("altFormat", getAltFormat());
		return obj;
	}

	@Override
	public JsonSerializable readJson(JsonObject placeholder) {
		this.text = placeholder.hasKey("text") ? placeholder.getString("text") : null;
		this.format = placeholder.hasKey("format") ? placeholder.getObject("format") : null;
		this.altFormat = placeholder.hasKey("altFormat") ? placeholder.getObject("altFormat") : null;
		return this;
	}
}
