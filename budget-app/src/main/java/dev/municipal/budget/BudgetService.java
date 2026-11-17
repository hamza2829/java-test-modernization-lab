package dev.municipal.budget;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import dev.municipal.budget.model.BudgetImport;
import dev.municipal.budget.model.BudgetItem;
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

    public BudgetService(BudgetRepository repository) {
        this.repository = repository;
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
        BigDecimal total = BigDecimal.ZERO;
        BigDecimal education = BigDecimal.ZERO;
        for (BudgetItem item : budget.items()) {
            total = total.add(item.amount());
            if ("EDUCATION".equals(item.category())) {
                education = education.add(item.amount());
            }
        }
        BigDecimal spending = NumberHelper.divide(total, BigDecimal.valueOf(budget.population()));
        BigDecimal share = NumberHelper.divide(education.multiply(BigDecimal.valueOf(100)), total);
        return new KeyFigures(budget.city().code(), budget.city().name(), year, spending, share);
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
