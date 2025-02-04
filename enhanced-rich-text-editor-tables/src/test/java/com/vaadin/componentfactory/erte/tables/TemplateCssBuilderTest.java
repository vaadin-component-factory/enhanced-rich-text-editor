package com.vaadin.componentfactory.erte.tables;

import com.vaadin.componentfactory.erte.tables.templates.TemplateParser;
import elemental.json.Json;
import elemental.json.JsonObject;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class TemplateCssBuilderTest {

    @Test
    public void test_parseSampleFile() throws IOException {
        Path pathJson = Path.of("sample.json");
        Assert.assertTrue("File not found: " + pathJson.toAbsolutePath(), Files.exists(pathJson));

        Path pathCss = Path.of("sample.css");
        Assert.assertTrue("File not found: " + pathCss.toAbsolutePath(), Files.exists(pathCss));

        // assure test files can be read
        String jsonString = Files.readString(pathJson);
        String cssString = Files.readString(pathCss).replace("\r\n", "\n"); // assure same type of line breaks

        // assure it is a valid json, if not we throw an exception, since not part of the test
        JsonObject json = Json.parse(jsonString);

        TemplateParser cssBuilder = new TemplateParser(json);
        String generated = cssBuilder.toCss();

        Assert.assertEquals(cssString, generated);
    }

}