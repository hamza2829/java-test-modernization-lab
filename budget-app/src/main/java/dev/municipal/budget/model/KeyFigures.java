package dev.municipal.budget.model;

import java.math.BigDecimal;

/**
 * Annual spending per resident and the percentage spent on education.
 *
 * @param cityCode stable city identifier
 * @param cityName display name
 * @param year budget year requested
 * @param spendingPerResident total expenditure divided by population
 * @param educationShare education expenditure as a percentage of total expenditure
 */
public record KeyFigures(String cityCode, String cityName, int year,
                         BigDecimal spendingPerResident, BigDecimal educationShare) {
}
