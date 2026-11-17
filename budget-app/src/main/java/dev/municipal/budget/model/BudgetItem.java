package dev.municipal.budget.model;

import java.math.BigDecimal;

/** One expenditure item in the fictional budget. */
public record BudgetItem(String category, BigDecimal amount) {
}
