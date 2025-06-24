package com.vaadin.componentfactory;

import static org.junit.Assert.assertEquals;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Tests for the {@link EnhancedRichTextEditor}.
 */
public class RichTextEditorTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void setValueNull() {
        EnhancedRichTextEditor rte = new EnhancedRichTextEditor();
        assertEquals("Value should be an empty string", "",
                rte.getValue());

        thrown.expect(NullPointerException.class);
        thrown.expectMessage("Null value is not supported");

        rte.setValue(null);
    }

    @Test
    public void initialValuePropertyValue() {
        EnhancedRichTextEditor rte = new EnhancedRichTextEditor();
        Assert.assertEquals(rte.getEmptyValue(),
                rte.getElement().getProperty("value"));
    }

    // Decoration group sanitization

    @Test
    public void sanitizeStrongTag_StrongTagPersist() {
        EnhancedRichTextEditor rte = new EnhancedRichTextEditor();
        Assert.assertEquals("<strong>Foo</strong>", rte.sanitize("<strong>Foo</strong>"));
    }

    @Test
    public void sanitizeEmTag_EmTagPersist() {
        EnhancedRichTextEditor rte = new EnhancedRichTextEditor();
        Assert.assertEquals("<em>Foo</em>", rte.sanitize("<em>Foo</em>"));
    }

    @Test
    public void sanitizeUTag_UTagPersist() {
        EnhancedRichTextEditor rte = new EnhancedRichTextEditor();
        Assert.assertEquals("<u>Foo</u>", rte.sanitize("<u>Foo</u>"));
    }

    @Test
    public void sanitizeSTag_STagPersist() {
        EnhancedRichTextEditor rte = new EnhancedRichTextEditor();
        Assert.assertEquals("<s>Foo</s>", rte.sanitize("<s>Foo</s>"));
    }

    @Test
    public void sanitizeCombinedDecorationTags_AllTagsPersist() {
        EnhancedRichTextEditor rte = new EnhancedRichTextEditor();
        Assert.assertEquals("<strong><em><s><u>123123</u></s></em></strong>", rte.sanitize("<strong><em><s><u>123123</u></s></em></strong>"));
    }

    // Headers group sanitization

    @Test
    public void sanitizeH1Tag_H1TagPersist() {
        EnhancedRichTextEditor rte = new EnhancedRichTextEditor();
        Assert.assertEquals("<h1>Foo</h1>", rte.sanitize("<h1>Foo</h1>"));
    }

    @Test
    public void sanitizeH2Tag_H2TagPersist() {
        EnhancedRichTextEditor rte = new EnhancedRichTextEditor();
        Assert.assertEquals("<h2>Foo</h2>", rte.sanitize("<h2>Foo</h2>"));
    }

    @Test
    public void sanitizeH3Tag_H3TagPersist() {
        EnhancedRichTextEditor rte = new EnhancedRichTextEditor();
        Assert.assertEquals("<h3>Foo</h3>", rte.sanitize("<h3>Foo</h3>"));
    }

    // Super - / Sub - scripts group sanitization

    @Test
    public void sanitizeSupTag_SupTagPersist() {
        EnhancedRichTextEditor rte = new EnhancedRichTextEditor();
        Assert.assertEquals("<sup>Foo</sup>", rte.sanitize("<sup>Foo</sup>"));
    }

    @Test
    public void sanitizeSubTag_SubTagPersist() {
        EnhancedRichTextEditor rte = new EnhancedRichTextEditor();
        Assert.assertEquals("<sub>Foo</sub>", rte.sanitize("<sub>Foo</sub>"));
    }

    // Lists group sanitization

    @Test
    public void sanitizeOrderedListTag_OrderedListTagPersist() {
        EnhancedRichTextEditor rte = new EnhancedRichTextEditor();
        Assert.assertEquals("<ol>\n Foo\n</ol>", rte.sanitize("<ol>Foo</ol>"));
    }

    @Test
    public void sanitizeBulletListTag_BulletListTagPersist() {
        EnhancedRichTextEditor rte = new EnhancedRichTextEditor();
        Assert.assertEquals("<ul>\n Foo\n</ul>", rte.sanitize("<ul>Foo</ul>"));
    }

    @Test
    public void sanitizeListElementTag_listElementTagPersist() {
        EnhancedRichTextEditor rte = new EnhancedRichTextEditor();
        Assert.assertEquals("<li>Foo</li>", rte.sanitize("<li>Foo</li>"));
    }

    // Alignment group sanitization

    @Test
    public void sanitizeStyleTextAlign_StyleTextAlignPersist() {
        EnhancedRichTextEditor rte = new EnhancedRichTextEditor();
        Assert.assertEquals("<p style=\"text-align: center\">Foo</p>", rte.sanitize("<p style=\"text-align: center\">Foo</p>"));
    }

    // Script sanitization

    @Test
    public void sanitizeScriptTag_scriptTagRemoved() {
        EnhancedRichTextEditor rte = new EnhancedRichTextEditor();
        Assert.assertEquals("", rte.sanitize("<script>alert('Foo')</script>"));
    }

    // Image sanitization

    @Test
    public void sanitizeImgTagWithHttpSource_srcAttributeRemoved() {
        EnhancedRichTextEditor rte = new EnhancedRichTextEditor();
        Assert.assertEquals("<img>", rte.sanitize("<img src='http://vaadin.com'>"));
    }

    @Test
    public void sanitizeImgTagWithHttpsSource_srcAttributeRemoved() {
        EnhancedRichTextEditor rte = new EnhancedRichTextEditor();
        Assert.assertEquals("<img>", rte.sanitize("<img src='https://vaadin.com'>"));
    }

    @Test
    public void sanitizeImgTagWithDataSource_srcAttributePersist() {
        EnhancedRichTextEditor rte = new EnhancedRichTextEditor();
        Assert.assertEquals("<img src=\"data:image/gif;base64,R0lGODlhAQABAIAAAAAAAP///ywAAAAAAQABAAACAUwAOw==\">",
                rte.sanitize("<img src=\"data:image/gif;base64,R0lGODlhAQABAIAAAAAAAP///ywAAAAAAQABAAACAUwAOw==\">"));
    }

    // Blockquote sanitization

    @Test
    public void sanitizeBlockquoteTag_blockquoteTagPersist() {
        EnhancedRichTextEditor rte = new EnhancedRichTextEditor();
        Assert.assertEquals("<blockquote>\n Foo\n</blockquote>", rte.sanitize("<blockquote>Foo</blockquote>"));
    }

    // Code block sanitization

    @Test
    public void sanitizePreTag_preTagPersist() {
        EnhancedRichTextEditor rte = new EnhancedRichTextEditor();
        Assert.assertEquals("<pre>Foo</pre>", rte.sanitize("<pre>Foo</pre>"));
    }
}
