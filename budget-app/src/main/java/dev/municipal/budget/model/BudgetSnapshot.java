package dev.municipal.budget.model;

import java.util.List;

/**
 * A city's stored budget and population for a requested year.
 *
 * @param city the city identity and display name
 * @param year the requested budget year
 * @param population the population for the year, or zero when the budget is missing
 * @param items the stored expenditure items for the year
 */
public record BudgetSnapshot(CityOption city, int year, int population, List<BudgetItem> items) {
    public BudgetSnapshot {
        items = List.copyOf(items);
    }
}
