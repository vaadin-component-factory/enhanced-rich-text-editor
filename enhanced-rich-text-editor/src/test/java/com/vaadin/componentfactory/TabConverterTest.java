package com.vaadin.componentfactory;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link TabConverter}.
 */
class TabConverterTest {

    private static final JsonMapper MAPPER = JsonMapper.shared();

    /**
     * Compares two JSON values for semantic equality (key order independent).
     */
    private void assertJsonEquals(String expected, String actual) {
        try {
            JsonNode expectedNode = MAPPER.readTree(expected);
            JsonNode actualNode = MAPPER.readTree(actual);
            assertEquals(expectedNode, actualNode,
                    "Expected:\n" + expected + "\nActual:\n" + actual);
        } catch (Exception e) {
            fail("Failed to parse JSON: " + e.getMessage());
        }
    }

    @Test
    void convertSingleTab() {
        String input = """
            {"ops":[{"attributes":{"tab":"1"},"insert":"\uFEFF"}]}
            """;
        String expected = """
            {"ops":[{"insert":{"tab":true}}]}
            """;
        assertJsonEquals(expected, TabConverter.convertToNewFormat(input));
    }

    @Test
    void convertMultipleLevelTab() {
        String input = """
            {"ops":[{"attributes":{"tab":"3"},"insert":"\uFEFF"}]}
            """;
        String expected = """
            {"ops":[{"insert":{"tab":true}},{"insert":{"tab":true}},{"insert":{"tab":true}}]}
            """;
        assertJsonEquals(expected, TabConverter.convertToNewFormat(input));
    }

    @Test
    void convertLinePartWithZeroWidthSpace() {
        String input = """
            {"ops":[{"attributes":{"line-part":true},"insert":"\uFEFF"}]}
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
            {"ops":[{"attributes":{"pre-tab":true},"insert":"\uFEFF"}]}
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
                {"attributes":{"tab":"1"},"insert":"\uFEFF"},
                {"attributes":{"line-part":true},"insert":"\uFEFF"},
                {"attributes":{"line-part":true},"insert":"Position"},
                {"attributes":{"tab":"1"},"insert":"\uFEFF"},
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
                {"attributes":{"tab":"2"},"insert":"\uFEFF"},
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
                {"attributes":{"tab":"1"},"insert":"\uFEFF"},
                {"attributes":{"tab":"1"},"insert":"\uFEFF"},
                {"attributes":{"tab":"1"},"insert":"\uFEFF"}
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
    void tabWithTrueValue() {
        String input = """
            {"ops":[{"attributes":{"tab":"true"},"insert":"\uFEFF"}]}
            """;
        String expected = """
            {"ops":[{"insert":{"tab":true}}]}
            """;
        assertJsonEquals(expected, TabConverter.convertToNewFormat(input));
    }

    @Test
    void preserveUnderlineFormatting() {
        String input = """
            {"ops":[
                {"attributes":{"tab":"3"},"insert":"\uFEFF"},
                {"attributes":{"line-part":true},"insert":"\uFEFF"},
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
        String input = """
            {"ops":[
                {"attributes":{"tab":"1"},"insert":"\uFEFF"},
                {"attributes":{"bold":true,"line-part":true},"insert":"Product"},
                {"attributes":{"tab":"1"},"insert":"\uFEFF"},
                {"attributes":{"bold":true,"line-part":true},"insert":"Price"},
                {"attributes":{"tab":"1"},"insert":"\uFEFF"},
                {"attributes":{"bold":true,"line-part":true},"insert":"Quantity"},
                {"attributes":{"tabs-cont":"TABS-CONT"},"insert":"\\n"},
                {"attributes":{"tab":"1"},"insert":"\uFEFF"},
                {"attributes":{"line-part":true},"insert":"Apples"},
                {"attributes":{"tab":"1"},"insert":"\uFEFF"},
                {"attributes":{"line-part":true},"insert":"2.00"},
                {"attributes":{"tab":"1"},"insert":"\uFEFF"},
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
        String newFormat = "{\"ops\":[{\"insert\":{\"tab\":true}},{\"insert\":\"Hello\"},{\"insert\":\"\\n\"}]}";
        String result = TabConverter.convertIfNeeded(newFormat);
        assertEquals(newFormat, result);
    }

    @Test
    void convertIfNeeded_oldFormatObjectConverted() {
        String input = "{\"ops\":[{\"attributes\":{\"tab\":\"1\"},\"insert\":\"\\uFEFF\"},{\"attributes\":{\"line-part\":true},\"insert\":\"Hello\"},{\"attributes\":{\"tabs-cont\":\"TABS-CONT\"},\"insert\":\"\\n\"}]}";
        String result = TabConverter.convertIfNeeded(input);
        assertTrue(result.contains("\"tab\":true"), "Should contain new tab format");
        assertFalse(result.contains("\"tabs-cont\""), "Should not contain old tabs-cont");
        assertFalse(result.contains("\"line-part\""), "Should not contain old line-part");
    }

    @Test
    void convertIfNeeded_oldFormatArrayConverted() {
        String input = "[{\"attributes\":{\"tab\":\"2\"},\"insert\":\"\\uFEFF\"},{\"attributes\":{\"line-part\":true},\"insert\":\"text\"},{\"attributes\":{\"tabs-cont\":\"TABS-CONT\"},\"insert\":\"\\n\"}]";
        String result = TabConverter.convertIfNeeded(input);
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
        String plain = "{\"ops\":[{\"insert\":\"Hello world\\n\"}]}";
        String result = TabConverter.convertIfNeeded(plain);
        assertEquals(plain, result);
    }

    @Test
    void convertIfNeeded_preTabDetected() {
        String input = "{\"ops\":[{\"attributes\":{\"pre-tab\":true},\"insert\":\"\\uFEFF\"},{\"insert\":\"\\n\"}]}";
        String result = TabConverter.convertIfNeeded(input);
        assertTrue(result.contains("\"tab\":true"), "pre-tab should be converted to tab embed");
    }

    @Test
    void convertIfNeeded_demoViewOldFormatArray() {
        String input = "[{\"attributes\":{\"tab\":\"1\"},\"insert\":\"\\uFEFF\"},"
            + "{\"attributes\":{\"line-part\":true},\"insert\":\"\\uFEFF\"},"
            + "{\"attributes\":{\"line-part\":true},\"insert\":\"Position\"},"
            + "{\"attributes\":{\"tab\":\"1\"},\"insert\":\"\\uFEFF\"},"
            + "{\"attributes\":{\"line-part\":true},\"insert\":\"Beschreibung\"},"
            + "{\"attributes\":{\"tabs-cont\":\"TABS-CONT\"},\"insert\":\"\\n\"}]";

        String result = TabConverter.convertIfNeeded(input);

        // Must return array format (starts with [)
        assertTrue(result.trim().startsWith("["), "Should return array format");

        // Count tab embeds
        try {
            JsonNode ops = MAPPER.readTree(result);
            int tabCount = 0;
            for (JsonNode op : ops) {
                JsonNode insert = op.get("insert");
                if (insert != null && insert.isObject() && insert.has("tab")) {
                    tabCount++;
                }
            }
            assertEquals(2, tabCount, "Should have exactly 2 tab embeds");
        } catch (Exception e) {
            fail("Failed to parse result JSON: " + e.getMessage());
        }

        // Verify text content survived
        assertTrue(result.contains("Position"), "Should preserve 'Position' text");
        assertTrue(result.contains("Beschreibung"), "Should preserve 'Beschreibung' text");

        // Verify old-format markers are gone
        assertFalse(result.contains("tabs-cont"), "Should not contain old tabs-cont");
        assertFalse(result.contains("line-part"), "Should not contain old line-part");
    }
}
