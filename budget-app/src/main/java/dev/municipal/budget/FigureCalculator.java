package dev.municipal.budget;

import java.math.BigDecimal;
import java.math.RoundingMode;

import dev.municipal.budget.model.BudgetItem;
import dev.municipal.budget.model.BudgetSnapshot;
import dev.municipal.budget.model.KeyFigures;
import org.springframework.stereotype.Component;

/** Calculates key figures independently of the database and web layer. */
@Component
public class FigureCalculator {
    public KeyFigures calculate(BudgetSnapshot budget) {
        BigDecimal total = BigDecimal.ZERO;
        BigDecimal education = BigDecimal.ZERO;
        for (BudgetItem item : budget.items()) {
            total = total.add(item.amount());
            if ("EDUCATION".equals(item.category())) {
                education = education.add(item.amount());
            }
        }
        BigDecimal spending = ratio(total, BigDecimal.valueOf(budget.population()));
        BigDecimal share = ratio(education.multiply(BigDecimal.valueOf(100)), total);
        return new KeyFigures(budget.city().code(), budget.city().name(), budget.year(), spending, share);
    }

    private BigDecimal ratio(BigDecimal numerator, BigDecimal denominator) {
        if (denominator.signum() == 0) {
            return new BigDecimal("0.00");
        }
        return numerator.divide(denominator, 2, RoundingMode.HALF_UP);
    }
}
