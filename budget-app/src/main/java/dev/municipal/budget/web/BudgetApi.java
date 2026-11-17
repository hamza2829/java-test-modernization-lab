package dev.municipal.budget.web;

import java.util.List;

import dev.municipal.budget.BudgetService;
import dev.municipal.budget.model.BudgetImport;
import dev.municipal.budget.model.CityOption;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    public void importBudget(@RequestBody BudgetImport budget) {
        budgets.importBudget(budget);
    }

    @GetMapping("/cities")
    public List<CityOption> cities() {
        return budgets.cities();
    }

    @GetMapping("/cities/{code}/figures")
    public ResponseEntity<?> figures(@PathVariable String code, @RequestParam(defaultValue = "2024") int year) {
        try {
            return ResponseEntity.ok(budgets.figures(code, year));
        } catch (RuntimeException exception) {
            return ResponseEntity.badRequest().body("Could not load figures");
        }
    }

    @GetMapping("/comparison")
    public ResponseEntity<?> compare(@RequestParam("city") List<String> cityCodes,
                                     @RequestParam(defaultValue = "2024") int year,
                                     @RequestParam(defaultValue = "spending-per-resident") String figure) {
        try {
            return ResponseEntity.ok(budgets.compare(cityCodes, year, figure));
        } catch (RuntimeException exception) {
            return ResponseEntity.badRequest().body("Could not compare cities");
        }
    }
}
