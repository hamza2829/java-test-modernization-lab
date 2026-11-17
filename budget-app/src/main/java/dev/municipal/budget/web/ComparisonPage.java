package dev.municipal.budget.web;

import java.util.List;

import dev.municipal.budget.BudgetService;
import dev.municipal.budget.CityNotFoundException;
import dev.municipal.budget.InvalidComparisonException;
import dev.municipal.budget.model.ComparisonFigure;
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
                             @RequestParam(name = "figure", required = false) String figureKey,
                             @RequestParam(defaultValue = "false") boolean compare, Model model) {
        List<String> selection = cityCodes == null ? List.of() : cityCodes;
        model.addAttribute("cities", budgets.cities());
        model.addAttribute("selectedCities", selection);
        model.addAttribute("year", year);
        model.addAttribute("figureOptions", ComparisonFigure.values());
        try {
            ComparisonFigure figure = ComparisonFigure.fromRequest(figureKey);
            model.addAttribute("figure", figure.key());
            if (compare) {
                model.addAttribute("rows", budgets.compare(selection, year, figure));
                model.addAttribute("figureLabel", figure.label());
            }
        } catch (CityNotFoundException | InvalidComparisonException exception) {
            model.addAttribute("error", exception.getMessage());
        }
        return "comparison";
    }
}
