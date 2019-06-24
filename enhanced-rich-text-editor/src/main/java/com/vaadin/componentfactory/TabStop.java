package com.vaadin.componentfactory;

/*
 * #%L
 * Vaadin EnhancedRichTextEditor for Vaadin 10
 * %%
 * Copyright (C) 2019 Vaadin Ltd
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

import java.io.Serializable;

/*
  TabStop is class for defining where tab stop should be located on ruler(by setting @position in pixels)
  and how text should be aligned to this tab stop(by setting @direction).
  There are 3 possible directions: Left, Right and Middle.
  When Direction set to Left: then left side of text will be aligned to right side of tab stop (>text)
  When Direction set to Right: then right side of text will be aligned to left side of tab stop  (text<)
  When Direction set to Middle: then text will be centered to tab stop  (te|xt)
 */

public class TabStop implements Serializable {
    private Direction direction;
    private double position;

    public TabStop() {
    }

    public TabStop(Direction direction, double position) {
        this.direction = direction;
        this.position = position;
    }

    public Direction getDirection() {
        return direction;
    }

    public void setDirection(Direction direction) {
        this.direction = direction;
    }

    public double getPosition() {
        return position;
    }

    public void setPosition(double position) {
        this.position = position;
    }

    @Override
    public String toString() {
        return "TabStop{" +
                "direction=" + direction +
                ", position=" + position +
                '}';
    }

    public enum Direction {
        LEFT, RIGHT, MIDDLE
    }
}
