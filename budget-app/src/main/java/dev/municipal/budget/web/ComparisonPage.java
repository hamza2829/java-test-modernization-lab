package dev.municipal.budget.web;

import java.util.List;

import dev.municipal.budget.BudgetService;
import dev.municipal.budget.CityNotFoundException;
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
                             @RequestParam(defaultValue = "${budget.reporting.default-year}") int year,
                             @RequestParam(defaultValue = "spending-per-resident") String figure,
                             @RequestParam(defaultValue = "false") boolean compare, Model model) {
        List<String> selection = cityCodes == null ? List.of() : cityCodes;
        model.addAttribute("cities", budgets.cities());
        model.addAttribute("selectedCities", selection);
        model.addAttribute("year", year);
        model.addAttribute("figure", figure);
        if (compare) {
            showResults(selection, year, figure, model);
        }
        return "comparison";
    }

    private void showResults(List<String> cityCodes, int year, String figure, Model model) {
        if (cityCodes.isEmpty()) {
            model.addAttribute("error", "Select cities to compare.");
            return;
        }
        try {
            model.addAttribute("rows", budgets.compare(cityCodes, year, figure));
        } catch (CityNotFoundException exception) {
            model.addAttribute("error", exception.getMessage());
        }
    }
}
