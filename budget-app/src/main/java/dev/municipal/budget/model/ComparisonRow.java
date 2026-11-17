package dev.municipal.budget.model;

import java.math.BigDecimal;

/**
 * The selected key figure for one city in a comparison.
 *
 * @param cityCode stable city identifier
 * @param cityName display name
 * @param value rounded value of the selected figure
 */
public record ComparisonRow(String cityCode, String cityName, BigDecimal value) {
}
