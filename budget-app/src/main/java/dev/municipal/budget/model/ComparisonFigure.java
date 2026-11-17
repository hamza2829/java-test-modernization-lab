package dev.municipal.budget.model;

import java.math.BigDecimal;
import java.util.function.Function;

import dev.municipal.budget.InvalidComparisonException;

/** Defines the supported comparison keys, their display labels, and the values they select. */
public enum ComparisonFigure {
    SPENDING_PER_RESIDENT("spending-per-resident", "Spending per resident", KeyFigures::spendingPerResident),
    EDUCATION_SHARE("education-share", "Education share (%)", KeyFigures::educationShare);

    private final String key;
    private final String label;
    private final Function<KeyFigures, BigDecimal> value;

    ComparisonFigure(String key, String label, Function<KeyFigures, BigDecimal> value) {
        this.key = key;
        this.label = label;
        this.value = value;
    }

    public String key() {
        return key;
    }

    public String label() {
        return label;
    }

    public BigDecimal valueFrom(KeyFigures figures) {
        return value.apply(figures);
    }

    public static ComparisonFigure fromRequest(String key) {
        if (key == null || key.isEmpty()) {
            return SPENDING_PER_RESIDENT;
        }
        for (ComparisonFigure figure : values()) {
            if (figure.key.equals(key)) {
                return figure;
            }
        }
        throw new InvalidComparisonException("Choose spending per resident or education share.");
    }
}
