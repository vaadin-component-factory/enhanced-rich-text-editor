package com.vaadin.componentfactory.erte.tables.templates;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Dimension {
    private final double value;
    private final String unit;

    public static final Pattern UNIT_PATTERN = Pattern.compile("[a-zA-Z]+");
    public static final Pattern STRING_PATTERN = Pattern.compile("(-?\\d+(\\.\\d+)?)\\s*(" + UNIT_PATTERN.pattern() + ")");

    public Dimension(double value, String unit) {
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

    public double getValue() { return value; }
    public String getUnit() { return unit; }

    public String toDimensionString() {
        if (value == (double) (int) value) {
            return (int) value + unit;
        }
        return value + unit;
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
