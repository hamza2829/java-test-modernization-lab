package dev.municipal.budget.model;

import java.math.BigDecimal;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Digits;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * One expenditure item in the fictional budget.
 *
 * @param category case-sensitive expenditure category
 * @param amount item value in budget currency units
 */
public record BudgetItem(
        @NotBlank @Size(max = 40) String category,
        @NotNull @DecimalMin("0.00") @Digits(integer = 16, fraction = 2) BigDecimal amount) {
}
