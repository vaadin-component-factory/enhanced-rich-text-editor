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

import jakarta.annotation.Nonnull;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Dimension {
    private final double value;
    private final String unit;

    public static final Pattern UNIT_PATTERN = Pattern.compile("[a-zA-Z]+");
    public static final Pattern STRING_PATTERN = Pattern.compile("(-?\\d+(\\.\\d+)?)\\s*(" + UNIT_PATTERN.pattern() + ")");

    /**
     * Creates a new instance based on the given numerical value and unit. The unit must not be null.
     * <p/>
     * There are no sanity checks regarding the unit, except for that it must fulfill the {@link #UNIT_PATTERN}.
     * @param value value
     * @param unit unit
     */
    public Dimension(double value, @Nonnull String unit) {
        if (unit == null) {
            throw new IllegalArgumentException("Unit cannot be null!");
        }

        if(!UNIT_PATTERN.matcher(unit).matches()) {
            throw new IllegalArgumentException("Invalid unit: " + unit +
                                               ", must match pattern " + UNIT_PATTERN.pattern());
        }

        this.value = value;
        this.unit =  unit;
    }

    /**
     * Creates a new instance based on the given string. The string must fulfill the {@link #STRING_PATTERN}.
     * @param dimension dimension
     */
    public Dimension(String dimension) {
        Matcher matcher = STRING_PATTERN.matcher(Objects.requireNonNull(dimension));
        if (matcher.matches() && matcher.groupCount() == 3) {
            value = Double.parseDouble(matcher.group(1));
            unit = matcher.group(3);
        } else {
            throw new IllegalArgumentException("Invalid dimension: " + dimension +
                                               ", must match pattern " + STRING_PATTERN.pattern());
        }
    }

    /**
     * Returns the numerical value of this instance.
     * @return value
     */
    public double getValue() {
        return value;
    }

    /**
     * Returns the unit of this instance.
     * @return unit
     */
    public String getUnit() {
        return unit;
    }

    /**
     * Returns this instance content as a string, representing the dimension. This string can be passed
     * to the constructor again to create a new instance.
     * @return dimension string
     */
    public String toDimensionString() {
        if (value == (double) (int) value) { // reduce to int if possible
            return (int) value + unit;
        }

        return value + unit; // always assure, that this also fulfills the STRING_PATTERN
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Dimension dimension = (Dimension) o;
        return Double.compare(value, dimension.value) == 0 && Objects.equals(unit, dimension.unit);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, unit);
    }
}
