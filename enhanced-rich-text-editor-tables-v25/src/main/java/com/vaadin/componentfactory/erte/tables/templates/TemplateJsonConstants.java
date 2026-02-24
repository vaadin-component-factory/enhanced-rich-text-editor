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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Contains constants definitions for the template json.
 */
public final class TemplateJsonConstants {
    public static final String P_BACKGROUND = "bgColor";
    public static final String P_COLOR = "color";
    public static final String P_WIDTH = "width";
    public static final String P_HEIGHT = "height";
    public static final String P_BORDER = "border";
    public static final String P_BORDER_CELLS = "borderCells";

    public static final String TABLE = "table";
    public static final String ROWS = "rows";
    public static final String CELLS = "cells";
    public static final String COLUMNS = "cols";

    public static final String CELL_X = "x";
    public static final String CELL_Y = "y";
    public static final String INDEX = "index";
    public static final String NAME = "name";
    public static final String FROM_BOTTOM = "last";
    public static final String DECLARATIONS = "declarations";

    public static final Pattern PATTERN_TEMPLATE_ID = Pattern.compile("[A-Za-z][A-Za-z0-9\\-]*");

    // source https://stackoverflow.com/a/63856391/11016302
    /** Hexcode pattern for colors */
    public static final Pattern PATTERN_P_COLOR_1 = Pattern.compile("#[a-f\\d]{3}(?:[a-f\\d]?|(?:[a-f\\d]{3}(?:[a-f\\d]{2})?)?)\\b");
    /** Text pattern for colors (names, like "red", "green", "salmon", etc. does NOT test for valid names) */
    public static final Pattern PATTERN_P_COLOR_2 = Pattern.compile("[a-zA-Z]+");
    /** hsla pattern for colors */
    public static final Pattern PATTERN_P_COLOR_3 = Pattern.compile("hsla?\\((?:(-?\\d+(?:deg|g?rad|turn)?),\\s*((?:\\d{1,2}|100)%),\\s*((?:\\d{1,2}|100)%)(?:,\\s*((?:\\d{1,2}|100)%|0(?:\\.\\d+)?|1))?|(-?\\d+(?:deg|g?rad|turn)?)\\s+((?:\\d{1,2}|100)%)\\s+((?:\\d{1,2}|100)%)(?:\\s+((?:\\d{1,2}|100)%|0(?:\\.\\d+)?|1))?)\\)"); // names
    /** rgb(a)*/
    public static final Pattern PATTERN_P_COLOR_4 = Pattern.compile("rgba?\\((?:(25[0-5]|2[0-4]\\d|1?\\d{1,2}|(?:\\d{1,2}|100)%),\\s*(25[0-5]|2[0-4]\\d|1?\\d{1,2}|(?:\\d{1,2}|100)%),\\s*(25[0-5]|2[0-4]\\d|1?\\d{1,2}|(?:\\d{1,2}|100)%)(?:,\\s*((?:\\d{1,2}|100)%|0(?:\\.\\d+)?|1))?|(25[0-5]|2[0-4]\\d|1?\\d{1,2}|(?:\\d{1,2}|100)%)\\s+(25[0-5]|2[0-4]\\d|1?\\d{1,2}|(?:\\d{1,2}|100)%)\\s+(25[0-5]|2[0-4]\\d|1?\\d{1,2}|(?:\\d{1,2}|100)%)(?:\\s+((?:\\d{1,2}|100)%|0(?:\\.\\d+)?|1))?)\\)"); // names
    /** CSS custom properties (var(--name)) */
    public static final Pattern PATTERN_P_COLOR_5 = Pattern.compile("var\\(--[a-zA-Z][a-zA-Z0-9-]*\\)");

    public static final Map<String, Set<String>> ALLOWED_PROPERTIES;

    static {
        Map<String, Set<String>> map = new HashMap<>();
        map.put(TABLE, Set.of(P_BACKGROUND, P_COLOR, P_WIDTH, P_HEIGHT, P_BORDER));
        map.put(ROWS, Set.of(P_BACKGROUND, P_COLOR, P_HEIGHT, P_BORDER));
        map.put(COLUMNS, Set.of(P_BACKGROUND, P_COLOR, P_WIDTH, P_BORDER));
        map.put(CELLS, Set.of(P_BACKGROUND, P_COLOR, P_BORDER));
        ALLOWED_PROPERTIES = Collections.unmodifiableMap(map);
    }

    /**
     * Validates a CSS color value against known patterns.
     * @param color color string to validate, null is allowed (disables feature)
     * @return true if valid or null, false otherwise
     */
    public static boolean isValidColor(String color) {
        if (color == null) return true; // null = disable feature
        color = color.trim();
        return PATTERN_P_COLOR_1.asMatchPredicate().test(color) ||
               PATTERN_P_COLOR_2.asMatchPredicate().test(color) ||
               PATTERN_P_COLOR_3.asMatchPredicate().test(color) ||
               PATTERN_P_COLOR_4.asMatchPredicate().test(color) ||
               PATTERN_P_COLOR_5.asMatchPredicate().test(color);
    }

}
