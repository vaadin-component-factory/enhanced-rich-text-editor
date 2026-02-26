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

import java.io.Serializable;
import java.util.Objects;

/**
 * TabStop defines where a tab stop is located on the ruler (by setting
 * {@code position} in pixels) and how text is aligned to this tab stop
 * (by setting {@code direction}).
 * <p>
 * There are 3 possible directions: Left, Right and Middle.
 * <ul>
 *   <li>LEFT: left side of text aligns to right side of tab stop (&gt;text)</li>
 *   <li>RIGHT: right side of text aligns to left side of tab stop (text&lt;)</li>
 *   <li>MIDDLE: text is centered at the tab stop (te|xt)</li>
 * </ul>
 */
public class TabStop implements Serializable {

    private final Direction direction;
    private final double position;

    public TabStop(Direction direction, double position) {
        this.direction = direction;
        this.position = position;
    }

    public Direction getDirection() {
        return direction;
    }

    public double getPosition() {
        return position;
    }

    @Override
    public String toString() {
        return "TabStop{" + "direction=" + direction + ", position=" + position
                + '}';
    }

    public enum Direction {
        LEFT, RIGHT, MIDDLE
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        TabStop tabStop = (TabStop) o;
        return Double.compare(position, tabStop.position) == 0
                && direction == tabStop.direction;
    }

    @Override
    public int hashCode() {
        return Objects.hash(direction, position);
    }
}
