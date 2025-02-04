package com.vaadin.componentfactory.erte.tables;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Set;

public class EnhancedRichTextEditorTablesTest {

    @Test
    public void test_getAssignedTemplateIds_empty() throws IOException {
        String delta = readResourceAsString("assigned-template-ids-delta-empty.json");
        Set<String> strings = EnhancedRichTextEditorTables.getAssignedTemplateIds(delta);

        Assert.assertNotNull(strings);
        Assert.assertTrue(strings.isEmpty());
    }

    @Test
    public void test_getAssignedTemplateIds_one() throws IOException {
        String delta = readResourceAsString("assigned-template-ids-delta-one.json");
        Set<String> strings = EnhancedRichTextEditorTables.getAssignedTemplateIds(delta);

        Assert.assertNotNull(strings);
        Assert.assertEquals(strings.size(), 1);
        Assert.assertTrue(strings.contains("template1"));
    }

    @Test
    public void test_getAssignedTemplateIds_multiple() throws IOException {
        String delta = readResourceAsString("assigned-template-ids-delta-multiple.json");
        Set<String> strings = EnhancedRichTextEditorTables.getAssignedTemplateIds(delta);

        Assert.assertNotNull(strings);
        Assert.assertEquals(strings.size(), 2);
        Assert.assertTrue(strings.contains("template1"));
        Assert.assertTrue(strings.contains("template3"));
    }

    private String readResourceAsString(String resourceName) throws IOException {
        try (InputStream stream = getClass().getClassLoader().getResourceAsStream(resourceName)) {
            if (stream == null) {
                throw new FileNotFoundException("src/test/resources/" + resourceName);
            }

            String string = IOUtils.toString(stream, Charset.defaultCharset());
            return string;
        }
    }
}