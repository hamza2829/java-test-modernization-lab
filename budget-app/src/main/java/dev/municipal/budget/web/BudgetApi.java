package dev.municipal.budget.web;

import java.util.List;

import javax.validation.Valid;

import dev.municipal.budget.BudgetService;
import dev.municipal.budget.model.BudgetImport;
import dev.municipal.budget.model.CityOption;
import dev.municipal.budget.model.ComparisonRow;
import dev.municipal.budget.model.KeyFigures;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** HTTP endpoints for budget imports and comparisons. */
@RestController
@RequestMapping("/api")
public class BudgetApi {
    private final BudgetService budgets;

    public BudgetApi(BudgetService budgets) {
        this.budgets = budgets;
    }

    @PostMapping("/budgets")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void importBudget(@Valid @RequestBody BudgetImport budget) {
        budgets.importBudget(budget);
    }

    @GetMapping("/cities")
    public List<CityOption> cities() {
        return budgets.cities();
    }

    @GetMapping("/cities/{code}/figures")
    public KeyFigures figures(@PathVariable String code,
                              @RequestParam(defaultValue = "${budget.reporting.default-year}") int year) {
        return budgets.figures(code, year);
    }

    @GetMapping("/comparison")
    public List<ComparisonRow> compare(@RequestParam("city") List<String> cityCodes,
                                       @RequestParam(defaultValue = "${budget.reporting.default-year}") int year,
                                       @RequestParam(defaultValue = "spending-per-resident") String figure) {
        return budgets.compare(cityCodes, year, figure);
    }
}
