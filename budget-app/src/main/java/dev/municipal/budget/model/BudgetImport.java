package dev.municipal.budget.model;

import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Positive;
import javax.validation.constraints.Size;

/**
 * Replaces one city's budget and population snapshot for a year.
 *
 * @param cityCode lowercase city identifier
 * @param cityName display name
 * @param population population for the budget year
 * @param year reporting year
 * @param items expenditures in the replacement snapshot
 */
public record BudgetImport(
        @NotBlank @Size(max = 24) @Pattern(regexp = "[a-z0-9]+(?:-[a-z0-9]+)*") String cityCode,
        @NotBlank @Size(max = 100) String cityName,
        @Positive int population,
        @Min(BudgetImport.FIRST_SUPPORTED_YEAR) @Max(BudgetImport.LAST_SUPPORTED_YEAR) int year,
        @NotEmpty @Size(max = 200) List<@NotNull @Valid BudgetItem> items) {
    public static final int FIRST_SUPPORTED_YEAR = 2000;
    public static final int LAST_SUPPORTED_YEAR = 2100;
}
