package com.vaadin.componentfactory.erte.tables.templates;

import org.junit.Assert;
import org.junit.Test;

public class DimensionTest {

    @Test
    public void test_stringConstructor() {
        try {
            new Dimension("");
            Assert.fail("Dimension string constructor should not accept empty string!");
        } catch (IllegalArgumentException e) { /* passed */}

        try {
            new Dimension("1");
            Assert.fail("Dimension string constructor should not accept value only string!");
        } catch (IllegalArgumentException e) { /* passed */}

        try {
            new Dimension("px");
            Assert.fail("Dimension string constructor should not accept unit only string!");
        } catch (IllegalArgumentException e) { /* passed */}

        try {
            new Dimension("1 1");
            Assert.fail("Dimension string constructor should not accept numerical unit!");
        } catch (IllegalArgumentException e) { /* passed */}

        try {
            new Dimension("1px-z");
            Assert.fail("Dimension string constructor should only accept letters for the unit!");
        } catch (IllegalArgumentException e) { /* passed */}

        try {
            new Dimension("1px");
        } catch (IllegalArgumentException e) {
            Assert.fail("Dimension string constructor did not accept \"1px\"!");
        }

        try {
            new Dimension("1.px");
            Assert.fail("Dimension string constructor should not accept illegal numerical values (\"1.px\")!");
        } catch (IllegalArgumentException e) { /* passed */}

        try {
            new Dimension("1.1px");
        } catch (IllegalArgumentException e) {
            Assert.fail("Dimension string constructor did not accept \"1.1px\"!");
        }
        try {
            new Dimension("10.10px");
        } catch (IllegalArgumentException e) {
            Assert.fail("Dimension string constructor did not accept \"10.10px\"!");
        }

        try {
            new Dimension("1 px");
        } catch (IllegalArgumentException e) {
            Assert.fail("Dimension string constructor did not accept space between value and unit: \"1 px\"!");
        }

        try {
            new Dimension("1.1 px");
        } catch (IllegalArgumentException e) {
            Assert.fail("Dimension string constructor did not accept space between value and unit: \"1 px\"!");
        }

        try {
            new Dimension("-1px");
        } catch (IllegalArgumentException e) {
            Assert.fail("Dimension string constructor did not accept \"-1px\"!");
        }

        try {
            new Dimension("-1.px");
            Assert.fail("Dimension string constructor should not accept illegal numerical values (\"-1.px\")!");
        } catch (IllegalArgumentException e) { /* passed */}

        try {
            new Dimension("- 1px");
            Assert.fail("Dimension string constructor should not accept illegal numerical values (\"- 1px\")!");
        } catch (IllegalArgumentException e) { /* passed */}

        try {
            new Dimension("-1.1px");
        } catch (IllegalArgumentException e) {
            Assert.fail("Dimension string constructor did not accept \"-1.1px\"!");
        }
        try {
            new Dimension("-10.10px");
        } catch (IllegalArgumentException e) {
            Assert.fail("Dimension string constructor did not accept \"-10.10px\"!");
        }

        try {
            new Dimension("-1 px");
        } catch (IllegalArgumentException e) {
            Assert.fail("Dimension string constructor did not accept space between value and unit: \"1 px\"!");
        }

        try {
            new Dimension("-1.1 px");
        } catch (IllegalArgumentException e) {
            Assert.fail("Dimension string constructor did not accept space between value and unit: \"1 px\"!");
        }


        Dimension dimension = new Dimension("1px");
        Assert.assertEquals(1, dimension.getValue(), 0);
        Assert.assertEquals("px", dimension.getUnit());

        dimension = new Dimension("1PX");
        Assert.assertEquals(1, dimension.getValue(), 0);
        Assert.assertEquals("PX", dimension.getUnit());

        dimension = new Dimension("1.1px");
        Assert.assertEquals(1.1, dimension.getValue(), 0);
        Assert.assertEquals("px", dimension.getUnit());

        dimension = new Dimension("10.10px");
        Assert.assertEquals(10.1, dimension.getValue(), 0);
        Assert.assertEquals("px", dimension.getUnit());

        dimension = new Dimension("-1px");
        Assert.assertEquals(-1, dimension.getValue(), 0);
        Assert.assertEquals("px", dimension.getUnit());

        dimension = new Dimension("-1.1px");
        Assert.assertEquals(-1.1, dimension.getValue(), 0);
        Assert.assertEquals("px", dimension.getUnit());

        dimension = new Dimension("-10.10px");
        Assert.assertEquals(-10.1, dimension.getValue(), 0);
        Assert.assertEquals("px", dimension.getUnit());
    }

    @Test
    public void test_constructor() {
        try {
            new Dimension(0, null);
            Assert.fail("Dimension constructor should not accept null as unit!");
        } catch (IllegalArgumentException nullPointerException) {
            /* passed */
        }

        try {
            new Dimension(0, "");
            Assert.fail("Dimension constructor should not accept an empty string as unit!");
        } catch (IllegalArgumentException nullPointerException) {
            /* passed */
        }

        try {
            new Dimension(0, "1");
            Assert.fail("Dimension constructor should only accept letters!");
        } catch (IllegalArgumentException nullPointerException) {
            /* passed */
        }

        try {
            new Dimension(0, "a-b");
            Assert.fail("Dimension constructor should only accept letters!");
        } catch (IllegalArgumentException nullPointerException) {
            /* passed */
        }

        Dimension dimension = new Dimension(0, "px");
        Assert.assertEquals(0, dimension.getValue(), 0);
        Assert.assertEquals("px", dimension.getUnit());

        dimension = new Dimension(0, "PX"); // no case conversion
        Assert.assertEquals(0, dimension.getValue(), 0);
        Assert.assertEquals("PX", dimension.getUnit());

        dimension = new Dimension(1, "px");
        Assert.assertEquals(1, dimension.getValue(), 0);
        Assert.assertEquals("px", dimension.getUnit());

        dimension = new Dimension(1.1, "px");
        Assert.assertEquals(1.1, dimension.getValue(), 0);
        Assert.assertEquals("px", dimension.getUnit());

        dimension = new Dimension(-1, "px");
        Assert.assertEquals(-1, dimension.getValue(), 0);
        Assert.assertEquals("px", dimension.getUnit());

        dimension = new Dimension(-1.1, "px");
        Assert.assertEquals(-1.1, dimension.getValue(), 0);
        Assert.assertEquals("px", dimension.getUnit());
    }

    @Test
    public void test_toDimensionStringCorrectStringPattern() {
        Dimension dimension = new Dimension("1px");
        Assert.assertEquals("1px", dimension.toDimensionString());
        dimension = new Dimension(dimension.toDimensionString()); // should not throw anything
        Assert.assertEquals("1px", dimension.toDimensionString());

        dimension = new Dimension("1PX");
        Assert.assertEquals("1PX", dimension.toDimensionString());
        dimension = new Dimension(dimension.toDimensionString()); // should not throw anything
        Assert.assertEquals("1PX", dimension.toDimensionString());

        dimension = new Dimension("1.1px");
        Assert.assertEquals("1.1px", dimension.toDimensionString());
        dimension = new Dimension(dimension.toDimensionString()); // should not throw anything
        Assert.assertEquals("1.1px", dimension.toDimensionString());

        dimension = new Dimension("10.10px");
        Assert.assertEquals("10.1px", dimension.toDimensionString());
        dimension = new Dimension(dimension.toDimensionString()); // should not throw anything
        Assert.assertEquals("10.1px", dimension.toDimensionString());

        dimension = new Dimension("-1px");
        Assert.assertEquals("-1px", dimension.toDimensionString());
        dimension = new Dimension(dimension.toDimensionString()); // should not throw anything
        Assert.assertEquals("-1px", dimension.toDimensionString());

        dimension = new Dimension("-1.1px");
        Assert.assertEquals("-1.1px", dimension.toDimensionString());
        dimension = new Dimension(dimension.toDimensionString()); // should not throw anything
        Assert.assertEquals("-1.1px", dimension.toDimensionString());

        dimension = new Dimension("-10.10px");
        Assert.assertEquals("-10.1px", dimension.toDimensionString());
        dimension = new Dimension(dimension.toDimensionString()); // should not throw anything
        Assert.assertEquals("-10.1px", dimension.toDimensionString());
    }

}