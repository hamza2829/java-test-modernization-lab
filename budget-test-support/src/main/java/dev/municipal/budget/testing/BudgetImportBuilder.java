package dev.municipal.budget.testing;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Builds independent JSON payloads for the budget import endpoint. */
public final class BudgetImportBuilder {
    private final String cityCode;
    private final String cityName;
    private final List<Map<String, Object>> items = new ArrayList<>();
    private int population = 1_000;
    private int year = 2024;

    public BudgetImportBuilder(String cityCode, String cityName) {
        this.cityCode = cityCode;
        this.cityName = cityName;
    }

    public BudgetImportBuilder forYear(int year) {
        this.year = year;
        return this;
    }

    public BudgetImportBuilder withPopulation(int population) {
        this.population = population;
        return this;
    }

    public BudgetImportBuilder item(String category, String amount) {
        items.add(Map.of("category", category, "amount", new BigDecimal(amount)));
        return this;
    }

    public Map<String, Object> build() {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("cityCode", cityCode);
        request.put("cityName", cityName);
        request.put("population", population);
        request.put("year", year);
        request.put("items", List.copyOf(items));
        return request;
    }
}
