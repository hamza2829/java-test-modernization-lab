package dev.municipal.budget.model;

import java.util.List;

/** Replaces one city's budget and population snapshot for a year. */
public record BudgetImport(String cityCode, String cityName, int population, int year, List<BudgetItem> items) {
}
