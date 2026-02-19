package com.vaadin.componentfactory;

import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;
import elemental.json.JsonValue;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link TabConverter}.
 */
class TabConverterTest {

    /**
     * Compares two JSON values for semantic equality (object key order independent, array order dependent).
     */
    private void assertJsonEquals(String expected, String actual) {
        JsonObject expectedObj = Json.parse(expected);
        JsonObject actualObj = Json.parse(actual);
        assertTrue(jsonEquals(expectedObj, actualObj),
                   "Expected:\n" + expected + "\nActual:\n" + actual);
    }

    private boolean jsonEquals(JsonValue expected, JsonValue actual) {
        if (expected == null && actual == null) {
            return true;
        }
        if (expected == null || actual == null) {
            return false;
        }
        if (expected.getType() != actual.getType()) {
            return false;
        }

        switch (expected.getType()) {
            case STRING:
                return expected.asString().equals(actual.asString());
            case NUMBER:
                return expected.asNumber() == actual.asNumber();
            case BOOLEAN:
                return expected.asBoolean() == actual.asBoolean();
            case NULL:
                return true;
            case OBJECT:
                return jsonObjectEquals((JsonObject) expected, (JsonObject) actual);
            case ARRAY:
                return jsonArrayEquals((JsonArray) expected, (JsonArray) actual);
            default:
                return false;
        }
    }

    private boolean jsonObjectEquals(JsonObject expected, JsonObject actual) {
        Set<String> expectedKeys = new HashSet<>();
        for (String key : expected.keys()) {
            expectedKeys.add(key);
        }
        Set<String> actualKeys = new HashSet<>();
        for (String key : actual.keys()) {
            actualKeys.add(key);
        }

        if (!expectedKeys.equals(actualKeys)) {
            return false;
        }

        for (String key : expectedKeys) {
            if (!jsonEquals(expected.get(key), actual.get(key))) {
                return false;
            }
        }
        return true;
    }

    private boolean jsonArrayEquals(JsonArray expected, JsonArray actual) {
        if (expected.length() != actual.length()) {
            return false;
        }
        for (int i = 0; i < expected.length(); i++) {
            if (!jsonEquals(expected.get(i), actual.get(i))) {
                return false;
            }
        }
        return true;
    }

    @Test
    void convertSingleTab() {
        String input = """
            {"ops":[{"attributes":{"tab":"1"},"insert":"﻿"}]}
            """;
        String expected = """
            {"ops":[{"insert":{"tab":true}}]}
            """;
        assertJsonEquals(expected, TabConverter.convertToNewFormat(input));
    }

    @Test
    void convertMultipleLevelTab() {
        String input = """
            {"ops":[{"attributes":{"tab":"3"},"insert":"﻿"}]}
            """;
        String expected = """
            {"ops":[{"insert":{"tab":true}},{"insert":{"tab":true}},{"insert":{"tab":true}}]}
            """;
        assertJsonEquals(expected, TabConverter.convertToNewFormat(input));
    }

    @Test
    void convertLinePartWithZeroWidthSpace() {
        String input = """
            {"ops":[{"attributes":{"line-part":true},"insert":"﻿"}]}
            """;
        String expected = """
            {"ops":[]}
            """;
        assertJsonEquals(expected, TabConverter.convertToNewFormat(input));
    }

    @Test
    void convertLinePartWithText() {
        String input = """
            {"ops":[{"attributes":{"line-part":true},"insert":"Position"}]}
            """;
        String expected = """
            {"ops":[{"insert":"Position"}]}
            """;
        assertJsonEquals(expected, TabConverter.convertToNewFormat(input));
    }

    @Test
    void convertLinePartWithFormatting() {
        String input = """
            {"ops":[{"attributes":{"line-part":true,"bold":true},"insert":"Text"}]}
            """;
        String expected = """
            {"ops":[{"attributes":{"bold":true},"insert":"Text"}]}
            """;
        assertJsonEquals(expected, TabConverter.convertToNewFormat(input));
    }

    @Test
    void convertTabsCont() {
        String input = """
            {"ops":[{"attributes":{"tabs-cont":"TABS-CONT"},"insert":"\\n"}]}
            """;
        String expected = """
            {"ops":[{"insert":"\\n"}]}
            """;
        assertJsonEquals(expected, TabConverter.convertToNewFormat(input));
    }

    @Test
    void convertPreTab() {
        String input = """
            {"ops":[{"attributes":{"pre-tab":true},"insert":"﻿"}]}
            """;
        String expected = """
            {"ops":[{"insert":{"tab":true}}]}
            """;
        assertJsonEquals(expected, TabConverter.convertToNewFormat(input));
    }

    @Test
    void preserveReadOnly() {
        String input = """
            {"ops":[{"insert":{"readonly":"Some readonly text\\n"}}]}
            """;
        String expected = """
            {"ops":[{"insert":{"readonly":"Some readonly text\\n"}}]}
            """;
        assertJsonEquals(expected, TabConverter.convertToNewFormat(input));
    }

    @Test
    void preserveNbsp() {
        String input = """
            {"ops":[{"insert":{"nbsp":true}}]}
            """;
        String expected = """
            {"ops":[{"insert":{"nbsp":true}}]}
            """;
        assertJsonEquals(expected, TabConverter.convertToNewFormat(input));
    }

    @Test
    void preservePlaceholder() {
        String input = """
            {"ops":[{"insert":{"placeholder":{"text":"N-1=Vaadin"}}}]}
            """;
        String expected = """
            {"ops":[{"insert":{"placeholder":{"text":"N-1=Vaadin"}}}]}
            """;
        assertJsonEquals(expected, TabConverter.convertToNewFormat(input));
    }

    @Test
    void preserveStandardFormatting() {
        String input = """
            {"ops":[{"attributes":{"bold":true,"italic":true},"insert":"Formatted text"}]}
            """;
        String expected = """
            {"ops":[{"attributes":{"bold":true,"italic":true},"insert":"Formatted text"}]}
            """;
        assertJsonEquals(expected, TabConverter.convertToNewFormat(input));
    }

    @Test
    void fullDocumentConversion() {
        String input = """
            {"ops":[
                {"attributes":{"tab":"1"},"insert":"﻿"},
                {"attributes":{"line-part":true},"insert":"﻿"},
                {"attributes":{"line-part":true},"insert":"Position"},
                {"attributes":{"tab":"1"},"insert":"﻿"},
                {"attributes":{"line-part":true},"insert":"Beschreibung"},
                {"attributes":{"tabs-cont":"TABS-CONT"},"insert":"\\n"}
            ]}
            """;
        String expected = """
            {"ops":[
                {"insert":{"tab":true}},
                {"insert":"Position"},
                {"insert":{"tab":true}},
                {"insert":"Beschreibung"},
                {"insert":"\\n"}
            ]}
            """;
        assertJsonEquals(expected, TabConverter.convertToNewFormat(input));
    }

    @Test
    void emptyDelta() {
        String input = """
            {"ops":[]}
            """;
        String expected = """
            {"ops":[]}
            """;
        assertJsonEquals(expected, TabConverter.convertToNewFormat(input));
    }

    @Test
    void nullInput() {
        assertNull(TabConverter.convertToNewFormat(null));
    }

    @Test
    void blankInput() {
        assertTrue(TabConverter.convertToNewFormat("").isEmpty());
        assertTrue(TabConverter.convertToNewFormat("   ").isBlank());
    }

    @Test
    void mixedContent() {
        String input = """
            {"ops":[
                {"insert":"Normal text "},
                {"attributes":{"tab":"2"},"insert":"﻿"},
                {"attributes":{"line-part":true,"bold":true},"insert":"Bold"},
                {"attributes":{"tabs-cont":"TABS-CONT"},"insert":"\\n"},
                {"insert":"More text\\n"}
            ]}
            """;
        String expected = """
            {"ops":[
                {"insert":"Normal text "},
                {"insert":{"tab":true}},
                {"insert":{"tab":true}},
                {"attributes":{"bold":true},"insert":"Bold"},
                {"insert":"\\n"},
                {"insert":"More text\\n"}
            ]}
            """;
        assertJsonEquals(expected, TabConverter.convertToNewFormat(input));
    }

    @Test
    void multipleTabsInSequence() {
        String input = """
            {"ops":[
                {"attributes":{"tab":"1"},"insert":"﻿"},
                {"attributes":{"tab":"1"},"insert":"﻿"},
                {"attributes":{"tab":"1"},"insert":"﻿"}
            ]}
            """;
        String expected = """
            {"ops":[
                {"insert":{"tab":true}},
                {"insert":{"tab":true}},
                {"insert":{"tab":true}}
            ]}
            """;
        assertJsonEquals(expected, TabConverter.convertToNewFormat(input));
    }

    @Test
    void tabsInMultipleParagraphs() {
        String input = """
            {"ops":[
                {"attributes":{"tab":"1"},"insert":"﻿"},
                {"attributes":{"line-part":true},"insert":"Line 1"},
                {"attributes":{"tabs-cont":"TABS-CONT"},"insert":"\\n"},
                {"insert":"Normal paragraph\\n"},
                {"attributes":{"tab":"2"},"insert":"﻿"},
                {"attributes":{"line-part":true},"insert":"Line 2"},
                {"attributes":{"tabs-cont":"TABS-CONT"},"insert":"\\n"}
            ]}
            """;
        String expected = """
            {"ops":[
                {"insert":{"tab":true}},
                {"insert":"Line 1"},
                {"insert":"\\n"},
                {"insert":"Normal paragraph\\n"},
                {"insert":{"tab":true}},
                {"insert":{"tab":true}},
                {"insert":"Line 2"},
                {"insert":"\\n"}
            ]}
            """;
        assertJsonEquals(expected, TabConverter.convertToNewFormat(input));
    }

    @Test
    void tabWithTrueValue() {
        // Old format sometimes uses "true" as tab value (for single tabs)
        String input = """
            {"ops":[{"attributes":{"tab":"true"},"insert":"﻿"}]}
            """;
        String expected = """
            {"ops":[{"insert":{"tab":true}}]}
            """;
        assertJsonEquals(expected, TabConverter.convertToNewFormat(input));
    }

    @Test
    void preserveUnderlineFormatting() {
        // From the demo: underline formatting should be preserved
        String input = """
            {"ops":[
                {"attributes":{"tab":"3"},"insert":"﻿"},
                {"attributes":{"line-part":true},"insert":"﻿"},
                {"attributes":{"underline":true,"line-part":true},"insert":"3rd tab-stop"},
                {"attributes":{"tabs-cont":"TABS-CONT"},"insert":"\\n"}
            ]}
            """;
        String expected = """
            {"ops":[
                {"insert":{"tab":true}},
                {"insert":{"tab":true}},
                {"insert":{"tab":true}},
                {"attributes":{"underline":true},"insert":"3rd tab-stop"},
                {"insert":"\\n"}
            ]}
            """;
        assertJsonEquals(expected, TabConverter.convertToNewFormat(input));
    }

    @Test
    void complexTableLikeStructure() {
        // Mimics the table structure from createEditorWithTabstops()
        String input = """
            {"ops":[
                {"attributes":{"tab":"1"},"insert":"﻿"},
                {"attributes":{"bold":true,"line-part":true},"insert":"Product"},
                {"attributes":{"tab":"1"},"insert":"﻿"},
                {"attributes":{"bold":true,"line-part":true},"insert":"Price"},
                {"attributes":{"tab":"1"},"insert":"﻿"},
                {"attributes":{"bold":true,"line-part":true},"insert":"Quantity"},
                {"attributes":{"tabs-cont":"TABS-CONT"},"insert":"\\n"},
                {"attributes":{"tab":"1"},"insert":"﻿"},
                {"attributes":{"line-part":true},"insert":"Apples"},
                {"attributes":{"tab":"1"},"insert":"﻿"},
                {"attributes":{"line-part":true},"insert":"2.00"},
                {"attributes":{"tab":"1"},"insert":"﻿"},
                {"attributes":{"line-part":true},"insert":"5"},
                {"attributes":{"tabs-cont":"TABS-CONT"},"insert":"\\n"}
            ]}
            """;
        String expected = """
            {"ops":[
                {"insert":{"tab":true}},
                {"attributes":{"bold":true},"insert":"Product"},
                {"insert":{"tab":true}},
                {"attributes":{"bold":true},"insert":"Price"},
                {"insert":{"tab":true}},
                {"attributes":{"bold":true},"insert":"Quantity"},
                {"insert":"\\n"},
                {"insert":{"tab":true}},
                {"insert":"Apples"},
                {"insert":{"tab":true}},
                {"insert":"2.00"},
                {"insert":{"tab":true}},
                {"insert":"5"},
                {"insert":"\\n"}
            ]}
            """;
        assertJsonEquals(expected, TabConverter.convertToNewFormat(input));
    }

    // ---- Tests for convertIfNeeded() ----

    @Test
    void convertIfNeeded_newFormatPassthrough() {
        // New-format delta should pass through unchanged
        String newFormat = "{\"ops\":[{\"insert\":{\"tab\":true}},{\"insert\":\"Hello\"},{\"insert\":\"\\n\"}]}";
        String result = TabConverter.convertIfNeeded(newFormat);
        assertEquals(newFormat, result);
    }

    @Test
    void convertIfNeeded_oldFormatObjectConverted() {
        // Old-format object {"ops":[...]} should be detected and converted
        String input = "{\"ops\":[{\"attributes\":{\"tab\":\"1\"},\"insert\":\"\\uFEFF\"},{\"attributes\":{\"line-part\":true},\"insert\":\"Hello\"},{\"attributes\":{\"tabs-cont\":\"TABS-CONT\"},\"insert\":\"\\n\"}]}";
        String result = TabConverter.convertIfNeeded(input);
        assertTrue(result.contains("\"tab\":true"), "Should contain new tab format");
        assertFalse(result.contains("\"tabs-cont\""), "Should not contain old tabs-cont");
        assertFalse(result.contains("\"line-part\""), "Should not contain old line-part");
    }

    @Test
    void convertIfNeeded_oldFormatArrayConverted() {
        // Old-format array [{...}] should be detected, wrapped, converted, and unwrapped
        String input = "[{\"attributes\":{\"tab\":\"2\"},\"insert\":\"\\uFEFF\"},{\"attributes\":{\"line-part\":true},\"insert\":\"text\"},{\"attributes\":{\"tabs-cont\":\"TABS-CONT\"},\"insert\":\"\\n\"}]";
        String result = TabConverter.convertIfNeeded(input);
        // Result should be an array (starts with [)
        assertTrue(result.trim().startsWith("["), "Should return array format");
        assertTrue(result.contains("\"tab\":true"), "Should contain new tab format");
        // Two tabs from tab level 2
        int firstTab = result.indexOf("\"tab\":true");
        int secondTab = result.indexOf("\"tab\":true", firstTab + 1);
        assertTrue(secondTab > firstTab, "Should have two tab embeds for level 2");
    }

    @Test
    void convertIfNeeded_nullAndEmpty() {
        assertNull(TabConverter.convertIfNeeded(null));
        assertEquals("", TabConverter.convertIfNeeded(""));
        assertEquals("  ", TabConverter.convertIfNeeded("  "));
    }

    @Test
    void convertIfNeeded_plainTextPassthrough() {
        // Plain text without any tab markers should pass through unchanged
        String plain = "{\"ops\":[{\"insert\":\"Hello world\\n\"}]}";
        String result = TabConverter.convertIfNeeded(plain);
        assertEquals(plain, result);
    }

    @Test
    void convertIfNeeded_preTabDetected() {
        // pre-tab marker should trigger conversion
        String input = "{\"ops\":[{\"attributes\":{\"pre-tab\":true},\"insert\":\"\\uFEFF\"},{\"insert\":\"\\n\"}]}";
        String result = TabConverter.convertIfNeeded(input);
        assertTrue(result.contains("\"tab\":true"), "pre-tab should be converted to tab embed");
    }

    @Test
    void convertIfNeeded_demoViewOldFormatArray() {
        // Exact delta from the standard demo view (array format, as passed by setValue)
        // Previously tested only via Playwright (features.spec.ts test 21)
        String input = "[{\"attributes\":{\"tab\":\"1\"},\"insert\":\"\\uFEFF\"},"
            + "{\"attributes\":{\"line-part\":true},\"insert\":\"\\uFEFF\"},"
            + "{\"attributes\":{\"line-part\":true},\"insert\":\"Position\"},"
            + "{\"attributes\":{\"tab\":\"1\"},\"insert\":\"\\uFEFF\"},"
            + "{\"attributes\":{\"line-part\":true},\"insert\":\"Beschreibung\"},"
            + "{\"attributes\":{\"tabs-cont\":\"TABS-CONT\"},\"insert\":\"\\n\"}]";

        String result = TabConverter.convertIfNeeded(input);

        // Must return array format (starts with [)
        assertTrue(result.trim().startsWith("["), "Should return array format");

        // Parse via wrapping (elemental Json.parse only handles objects)
        JsonObject wrapped = Json.parse("{\"ops\":" + result + "}");
        JsonArray ops = wrapped.getArray("ops");

        // Count tab embeds — should be exactly 2
        int tabCount = 0;
        for (int i = 0; i < ops.length(); i++) {
            JsonObject op = ops.getObject(i);
            JsonValue insert = op.get("insert");
            if (insert.getType() == elemental.json.JsonType.OBJECT) {
                JsonObject insertObj = (JsonObject) insert;
                if (insertObj.hasKey("tab")) {
                    tabCount++;
                }
            }
        }
        assertEquals(2, tabCount, "Should have exactly 2 tab embeds");

        // Verify text content survived
        assertTrue(result.contains("Position"), "Should preserve 'Position' text");
        assertTrue(result.contains("Beschreibung"), "Should preserve 'Beschreibung' text");

        // Verify old-format markers are gone
        assertFalse(result.contains("tabs-cont"), "Should not contain old tabs-cont");
        assertFalse(result.contains("line-part"), "Should not contain old line-part");
    }
}
