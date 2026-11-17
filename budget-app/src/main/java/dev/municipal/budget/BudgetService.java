package dev.municipal.budget;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import dev.municipal.budget.model.BudgetImport;
import dev.municipal.budget.model.BudgetSnapshot;
import dev.municipal.budget.model.CityOption;
import dev.municipal.budget.model.ComparisonFigure;
import dev.municipal.budget.model.ComparisonRow;
import dev.municipal.budget.model.KeyFigures;
import dev.municipal.budget.persistence.BudgetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Imports budgets and calculates the figures shown to residents. */
@Service
public class BudgetService {
    private static final int MIN_COMPARISON_CITIES = 2;
    private static final int MAX_COMPARISON_CITIES = 10;
    private final BudgetRepository repository;
    private final FigureCalculator calculator;

    public BudgetService(BudgetRepository repository, FigureCalculator calculator) {
        this.repository = repository;
        this.calculator = calculator;
    }

    @Transactional
    public void importBudget(BudgetImport budget) {
        repository.replace(budget);
    }

    public List<CityOption> cities() {
        return repository.cities();
    }

    public KeyFigures figures(String cityCode, int year) {
        BudgetSnapshot budget = repository.findSnapshot(cityCode, year)
                .orElseThrow(() -> new CityNotFoundException(cityCode));
        return calculator.calculate(budget);
    }

    public List<ComparisonRow> compare(List<String> cityCodes, int year, ComparisonFigure figure) {
        validateSelection(cityCodes);
        List<ComparisonRow> result = new ArrayList<>();
        for (String cityCode : cityCodes) {
            KeyFigures figures = figures(cityCode, year);
            result.add(new ComparisonRow(figures.cityCode(), figures.cityName(), figure.valueFrom(figures)));
        }
        return result;
    }

    private void validateSelection(List<String> cityCodes) {
        if (cityCodes.size() < MIN_COMPARISON_CITIES || cityCodes.size() > MAX_COMPARISON_CITIES) {
            throw new InvalidComparisonException(
                    "Select between " + MIN_COMPARISON_CITIES + " and " + MAX_COMPARISON_CITIES + " cities.");
        }
        if (cityCodes.stream().anyMatch(code -> code == null || code.isBlank())) {
            throw new InvalidComparisonException("Supply a city code for each selection.");
        }
        if (new HashSet<>(cityCodes).size() != cityCodes.size()) {
            throw new InvalidComparisonException("Select each city only once.");
        }
    }
}
