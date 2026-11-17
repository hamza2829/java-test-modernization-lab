package dev.municipal.budget.web;

import java.util.List;

import dev.municipal.budget.BudgetService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** Renders the comparison form and its results. */
@Controller
public class ComparisonPage {
    private final BudgetService budgets;

    public ComparisonPage(BudgetService budgets) {
        this.budgets = budgets;
    }

    @GetMapping("/")
    public String comparison(@RequestParam(name = "city", required = false) List<String> cityCodes,
                             @RequestParam(defaultValue = "2024") int year,
                             @RequestParam(defaultValue = "spending-per-resident") String figure,
                             @RequestParam(defaultValue = "false") boolean compare, Model model) {
        model.addAttribute("cities", budgets.cities());
        model.addAttribute("selectedCities", cityCodes == null ? List.of() : cityCodes);
        model.addAttribute("year", year);
        model.addAttribute("figure", figure);
        if (compare) {
            try {
                model.addAttribute("rows", budgets.compare(cityCodes, year, figure));
            } catch (RuntimeException exception) {
                model.addAttribute("error", "Could not compare cities");
            }
        }
        return "comparison";
    }
}
