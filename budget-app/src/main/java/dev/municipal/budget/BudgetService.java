package dev.municipal.budget;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import dev.municipal.budget.model.BudgetImport;
import dev.municipal.budget.model.BudgetSnapshot;
import dev.municipal.budget.model.CityOption;
import dev.municipal.budget.model.ComparisonRow;
import dev.municipal.budget.model.KeyFigures;
import dev.municipal.budget.persistence.BudgetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Imports budgets and calculates the figures shown to residents. */
@Service
public class BudgetService {
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
                .orElseThrow(() -> new IllegalArgumentException("Unknown city: " + cityCode));
        return calculator.calculate(budget);
    }

    public List<ComparisonRow> compare(List<String> cityCodes, int year, String figure) {
        List<ComparisonRow> result = new ArrayList<>();
        for (String cityCode : cityCodes) {
            KeyFigures figures = figures(cityCode, year);
            BigDecimal value = "spending-per-resident".equals(figure)
                    ? figures.spendingPerResident() : figures.educationShare();
            result.add(new ComparisonRow(figures.cityCode(), figures.cityName(), value));
        }
        return result;
    }
}
