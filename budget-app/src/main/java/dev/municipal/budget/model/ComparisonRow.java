package dev.municipal.budget.model;

import java.math.BigDecimal;

/** The selected key figure for one city in a comparison. */
public record ComparisonRow(String cityCode, String cityName, BigDecimal value) {
}
