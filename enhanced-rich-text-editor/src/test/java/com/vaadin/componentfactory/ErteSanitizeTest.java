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

import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link EnhancedRichTextEditor#erteSanitize(String)}.
 * Same package gives direct access to the protected static method.
 */
class ErteSanitizeTest {

    private static String sanitize(String html) {
        return EnhancedRichTextEditor.erteSanitize(html);
    }

    // ================================================================
    // Null / Empty
    // ================================================================

    @Nested
    @DisplayName("Null and Empty")
    class NullEmpty {

        @Test
        void nullReturnsNull() {
            assertNull(sanitize(null));
        }

        @Test
        void emptyReturnsEmpty() {
            assertEquals("", sanitize(""));
        }
    }

    // ================================================================
    // Style Filtering
    // ================================================================

    @Nested
    @DisplayName("Style Attribute Filtering")
    class StyleFiltering {

        @Test
        void preservesSafeColor() {
            String html = "<p style=\"color: red\">text</p>";
            String result = sanitize(html);
            assertTrue(result.contains("color: red"),
                    "Safe color should be preserved: " + result);
        }

        @Test
        void preservesSafeBackgroundColor() {
            String html = "<p style=\"background-color: #ff0000\">text</p>";
            String result = sanitize(html);
            assertTrue(result.contains("background-color: #ff0000"),
                    "Safe background-color should be preserved: " + result);
        }

        @Test
        void preservesSafeFontSize() {
            String html = "<p style=\"font-size: 14px\">text</p>";
            String result = sanitize(html);
            assertTrue(result.contains("font-size: 14px"),
                    "Safe font-size should be preserved: " + result);
        }

        @Test
        void preservesRgbFunction() {
            String html = "<p style=\"color: rgb(255, 0, 0)\">text</p>";
            String result = sanitize(html);
            assertTrue(result.contains("rgb(255, 0, 0)"),
                    "rgb() function should be allowed: " + result);
        }

        @Test
        void preservesCalcFunction() {
            String html = "<p style=\"width: calc(100% - 20px)\">text</p>";
            String result = sanitize(html);
            assertTrue(result.contains("calc(100% - 20px)"),
                    "calc() function should be allowed: " + result);
        }

        @Test
        void stripsUrlFunction() {
            String html = "<p style=\"background: url(https://evil.com/track.gif)\">text</p>";
            String result = sanitize(html);
            assertFalse(result.contains("url("),
                    "url() function should be stripped: " + result);
        }

        @Test
        void stripsExpressionFunction() {
            String html = "<p style=\"width: expression(alert(1))\">text</p>";
            String result = sanitize(html);
            assertFalse(result.contains("expression"),
                    "expression() function should be stripped: " + result);
        }

        @Test
        void stripsBehaviorFunction() {
            String html = "<p style=\"behavior: url(xss.htc)\">text</p>";
            String result = sanitize(html);
            assertFalse(result.contains("behavior"),
                    "behavior property should be stripped: " + result);
        }

        @Test
        void stripsUnknownProperties() {
            String html = "<p style=\"-moz-binding: url(evil)\">text</p>";
            String result = sanitize(html);
            assertFalse(result.contains("-moz-binding"),
                    "Unknown CSS property should be stripped: " + result);
        }

        @Test
        void mixedSafeAndUnsafe() {
            String html = "<p style=\"color: red; background: url(evil); font-size: 12px\">text</p>";
            String result = sanitize(html);
            assertTrue(result.contains("color: red"), "Safe color kept: " + result);
            assertTrue(result.contains("font-size: 12px"), "Safe font-size kept: " + result);
            assertFalse(result.contains("url("), "url() stripped: " + result);
        }

        @Test
        void removesEmptyStyleAttribute() {
            String html = "<p style=\"behavior: url(xss.htc)\">text</p>";
            String result = sanitize(html);
            assertFalse(result.contains("style=\"\""),
                    "Empty style attribute should be removed: " + result);
        }

        @Test
        void stripsCssComments() {
            String html = "<p style=\"color: red; /* background: url(evil) */ font-size: 12px\">text</p>";
            String result = sanitize(html);
            assertTrue(result.contains("color: red"), "Color kept: " + result);
            assertTrue(result.contains("font-size: 12px"), "Font-size kept: " + result);
            assertFalse(result.contains("url("), "url() inside comment stripped: " + result);
            assertFalse(result.contains("/*"), "Comment syntax stripped: " + result);
        }

        @Test
        void stripsImportDirective() {
            String html = "<p style=\"color: @import 'evil.css'\">text</p>";
            String result = sanitize(html);
            assertFalse(result.contains("@import"),
                    "@import should be stripped: " + result);
        }

        @Test
        void stripsUnknownCssFunction() {
            String html = "<p style=\"color: var(--custom-prop)\">text</p>";
            String result = sanitize(html);
            assertFalse(result.contains("var("),
                    "Unknown CSS function var() should be stripped: " + result);
        }

        @Test
        void backgroundUrlShorthandBlocked() {
            String html = "<p style=\"background: url(https://evil.com/img.png) no-repeat\">text</p>";
            String result = sanitize(html);
            assertFalse(result.contains("url("),
                    "background: url() shorthand should be blocked: " + result);
        }

        @Test
        void preservesFloat() {
            String html = "<img style=\"float: right\" />";
            String result = sanitize(html);
            assertTrue(result.contains("float: right"),
                    "float should be preserved for Quill 2 image alignment: " + result);
        }
    }

    // ================================================================
    // Data URL Filtering
    // ================================================================

    @Nested
    @DisplayName("Data URL Filtering")
    class DataUrlFiltering {

        @Test
        void allowsDataImagePng() {
            String html = "<img src=\"data:image/png;base64,iVBOR\" />";
            String result = sanitize(html);
            assertTrue(result.contains("data:image/png"),
                    "data:image/png should be allowed: " + result);
        }

        @Test
        void allowsDataImageJpeg() {
            String html = "<img src=\"data:image/jpeg;base64,/9j/4\" />";
            String result = sanitize(html);
            assertTrue(result.contains("data:image/jpeg"),
                    "data:image/jpeg should be allowed: " + result);
        }

        @Test
        void allowsDataImageGif() {
            String html = "<img src=\"data:image/gif;base64,R0lGOD\" />";
            String result = sanitize(html);
            assertTrue(result.contains("data:image/gif"),
                    "data:image/gif should be allowed: " + result);
        }

        @Test
        void allowsDataImageWebp() {
            String html = "<img src=\"data:image/webp;base64,UklG\" />";
            String result = sanitize(html);
            assertTrue(result.contains("data:image/webp"),
                    "data:image/webp should be allowed: " + result);
        }

        @Test
        void blocksSvgXml() {
            String html = "<img src=\"data:image/svg+xml;base64,PHN2Zy...\" />";
            String result = sanitize(html);
            assertFalse(result.contains("data:image/svg+xml"),
                    "data:image/svg+xml should be blocked (SVG can contain scripts): " + result);
        }

        @Test
        void blocksTextHtml() {
            String html = "<img src=\"data:text/html,<script>alert(1)</script>\" />";
            String result = sanitize(html);
            assertFalse(result.contains("data:text/html"),
                    "data:text/html should be blocked: " + result);
        }

        @Test
        void blocksApplicationJavascript() {
            String html = "<img src=\"data:application/javascript,alert(1)\" />";
            String result = sanitize(html);
            assertFalse(result.contains("data:application/javascript"),
                    "data:application/javascript should be blocked: " + result);
        }

        @Test
        void httpsSrcUnaffected() {
            String html = "<img src=\"https://example.com/image.png\" />";
            String result = sanitize(html);
            assertTrue(result.contains("https://example.com/image.png"),
                    "HTTPS src should be unaffected: " + result);
        }

        @Test
        void caseInsensitiveMime() {
            String html = "<img src=\"data:IMAGE/PNG;base64,iVBOR\" />";
            String result = sanitize(html);
            assertTrue(result.contains("data:IMAGE/PNG"),
                    "MIME type check should be case-insensitive: " + result);
        }

        @Test
        void whitespaceAfterData() {
            String html = "<img src=\"data: image/png;base64,iVBOR\" />";
            String result = sanitize(html);
            assertTrue(result.contains("data:"),
                    "Optional whitespace after data: should be handled: " + result);
        }
    }

    // ================================================================
    // Class Filtering (regression)
    // ================================================================

    @Nested
    @DisplayName("Class Filtering")
    class ClassFiltering {

        @Test
        void preservesErteClasses() {
            String html = "<span class=\"ql-readonly\">text</span>";
            String result = sanitize(html);
            assertTrue(result.contains("ql-readonly"),
                    "ERTE class ql-readonly should be preserved: " + result);
        }

        @Test
        void preservesQuillAlignClasses() {
            String html = "<p class=\"ql-align-center\">text</p>";
            String result = sanitize(html);
            assertTrue(result.contains("ql-align-center"),
                    "Quill alignment class should be preserved: " + result);
        }

        @Test
        void stripsUnknownClasses() {
            String html = "<span class=\"evil-class\">text</span>";
            String result = sanitize(html);
            assertFalse(result.contains("evil-class"),
                    "Unknown class should be stripped: " + result);
        }

        @Test
        void preservesMultipleErteClasses() {
            String html = "<span class=\"ql-tab ql-soft-break\">text</span>";
            String result = sanitize(html);
            assertTrue(result.contains("ql-tab"), "ql-tab preserved: " + result);
            assertTrue(result.contains("ql-soft-break"), "ql-soft-break preserved: " + result);
        }
    }

    // ================================================================
    // XSS Stripping (regression)
    // ================================================================

    @Nested
    @DisplayName("XSS Stripping")
    class XssStripping {

        @Test
        void stripsScriptTags() {
            String html = "<p>safe</p><script>alert(1)</script>";
            String result = sanitize(html);
            assertFalse(result.contains("<script"),
                    "Script tags should be stripped: " + result);
            assertTrue(result.contains("safe"), "Safe content preserved: " + result);
        }

        @Test
        void stripsOnerror() {
            String html = "<img src=x onerror=alert(1)>";
            String result = sanitize(html);
            assertFalse(result.contains("onerror"),
                    "onerror handler should be stripped: " + result);
        }

        @Test
        void stripsContenteditableTrue() {
            String html = "<span contenteditable=\"true\">editable</span>";
            String result = sanitize(html);
            assertFalse(result.contains("contenteditable=\"true\""),
                    "contenteditable=true should be stripped: " + result);
        }

        @Test
        void stripsContenteditableEmpty() {
            String html = "<span contenteditable=\"\">editable</span>";
            String result = sanitize(html);
            assertFalse(result.contains("contenteditable=\"\""),
                    "contenteditable='' (empty) should be stripped: " + result);
        }
    }

    // ================================================================
    // Real-World Formatting (Quill 2 output)
    // ================================================================

    @Nested
    @DisplayName("Real-World Formatting")
    class RealWorldFormatting {

        @Test
        void quillColorSurvivesRoundTrip() {
            String html = "<p><span style=\"color: rgb(230, 0, 0)\">red text</span></p>";
            String result = sanitize(html);
            assertTrue(result.contains("color: rgb(230, 0, 0)"),
                    "Quill color format should survive: " + result);
        }

        @Test
        void quillBackgroundSurvivesRoundTrip() {
            String html = "<p><span style=\"background-color: rgb(255, 255, 0)\">highlighted</span></p>";
            String result = sanitize(html);
            assertTrue(result.contains("background-color: rgb(255, 255, 0)"),
                    "Quill background-color should survive: " + result);
        }

        @Test
        void quillFontFamilySurvives() {
            String html = "<p><span style=\"font-family: monospace\">code</span></p>";
            String result = sanitize(html);
            assertTrue(result.contains("font-family: monospace"),
                    "Quill font-family should survive: " + result);
        }

        @Test
        void quillDirectionSurvives() {
            String html = "<p style=\"direction: rtl\">right to left</p>";
            String result = sanitize(html);
            assertTrue(result.contains("direction: rtl"),
                    "Quill direction should survive: " + result);
        }

        @Test
        void quillImageFloatSurvives() {
            String html = "<img src=\"https://example.com/img.png\" style=\"float: left\" />";
            String result = sanitize(html);
            assertTrue(result.contains("float: left"),
                    "Quill image float alignment should survive: " + result);
        }
    }

    // ================================================================
    // Dynamic Allowed Classes
    // ================================================================

    @Nested
    @DisplayName("Dynamic Allowed Classes")
    class DynamicAllowedClasses {

        @Test
        void extraClassPreserved() {
            String html = "<table class=\"template1\"><tr><td>text</td></tr></table>";
            String result = EnhancedRichTextEditor.erteSanitize(html,
                    Set.of("template1"));
            assertTrue(result.contains("template1"),
                    "Dynamic class preserved: " + result);
        }

        @Test
        void extraClassStrippedWithoutRegistration() {
            String html = "<table class=\"template1\"><tr><td>text</td></tr></table>";
            String result = sanitize(html);
            assertFalse(result.contains("class=\"template1\""),
                    "Unregistered class stripped: " + result);
        }

        @Test
        void multipleExtraClasses() {
            String html = "<table class=\"template1 template2\"><tr><td>text</td></tr></table>";
            String result = EnhancedRichTextEditor.erteSanitize(html,
                    Set.of("template1", "template2"));
            assertTrue(result.contains("template1"),
                    "template1 preserved: " + result);
            assertTrue(result.contains("template2"),
                    "template2 preserved: " + result);
        }

        @Test
        void mixedStaticAndDynamic() {
            String html = "<span class=\"ql-readonly template1\">text</span>";
            String result = EnhancedRichTextEditor.erteSanitize(html,
                    Set.of("template1"));
            assertTrue(result.contains("ql-readonly"),
                    "Static class preserved: " + result);
            assertTrue(result.contains("template1"),
                    "Dynamic class preserved: " + result);
        }

        @Test
        void emptyExtraSetBehavesLikeSingleArg() {
            String html = "<span class=\"ql-readonly evil-class\">text</span>";
            String noExtras = sanitize(html);
            String emptySet = EnhancedRichTextEditor.erteSanitize(html,
                    Set.of());
            assertEquals(noExtras, emptySet);
        }

        @Test
        void caseSensitive() {
            String html = "<table class=\"Template1\"><tr><td>text</td></tr></table>";
            String result = EnhancedRichTextEditor.erteSanitize(html,
                    Set.of("template1"));
            assertFalse(result.contains("Template1"),
                    "Case-sensitive: 'Template1' not matched by 'template1': "
                            + result);
        }

        @Test
        void classOnTdElement() {
            String html = "<table><tr><td class=\"template1\">text</td></tr></table>";
            String result = EnhancedRichTextEditor.erteSanitize(html,
                    Set.of("template1"));
            assertTrue(result.contains("template1"),
                    "Dynamic class on td preserved: " + result);
        }
    }

    // ================================================================
    // Class Name Validation
    // ================================================================

    @Nested
    @DisplayName("Class Name Validation")
    class ClassNameValidation {

        @Test
        void rejectsQlPrefix() {
            assertThrows(IllegalArgumentException.class,
                    () -> EnhancedRichTextEditor.validateClassName(
                            "ql-custom"));
        }

        @Test
        void rejectsNull() {
            assertThrows(IllegalArgumentException.class,
                    () -> EnhancedRichTextEditor.validateClassName(null));
        }

        @Test
        void rejectsEmpty() {
            assertThrows(IllegalArgumentException.class,
                    () -> EnhancedRichTextEditor.validateClassName(""));
        }

        @Test
        void rejectsSpaces() {
            assertThrows(IllegalArgumentException.class,
                    () -> EnhancedRichTextEditor.validateClassName(
                            "has spaces"));
        }

        @Test
        void rejectsStartingWithDigit() {
            assertThrows(IllegalArgumentException.class,
                    () -> EnhancedRichTextEditor.validateClassName(
                            "1template"));
        }

        @Test
        void rejectsUnderscore() {
            assertThrows(IllegalArgumentException.class,
                    () -> EnhancedRichTextEditor.validateClassName(
                            "my_template"));
        }

        @Test
        void acceptsValidNames() {
            assertDoesNotThrow(
                    () -> EnhancedRichTextEditor.validateClassName(
                            "template1"));
            assertDoesNotThrow(
                    () -> EnhancedRichTextEditor.validateClassName(
                            "my-template"));
            assertDoesNotThrow(
                    () -> EnhancedRichTextEditor.validateClassName("T"));
            assertDoesNotThrow(
                    () -> EnhancedRichTextEditor.validateClassName("A-1"));
        }
    }
}
