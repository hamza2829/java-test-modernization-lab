package dev.municipal.budget.model;

import java.math.BigDecimal;

/** Annual spending per resident and the percentage spent on education. */
public record KeyFigures(String cityCode, String cityName, int year,
                         BigDecimal spendingPerResident, BigDecimal educationShare) {
}
