package dev.municipal.budget;

import java.math.BigDecimal;
import java.util.List;

import dev.municipal.budget.model.BudgetImport;
import dev.municipal.budget.model.BudgetItem;
import dev.municipal.budget.model.ComparisonRow;
import dev.municipal.budget.model.KeyFigures;
import dev.municipal.budget.web.ApiError;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BudgetCharacterizationIT {
    @Autowired
    private TestRestTemplate client;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void clearBudgets() {
        jdbc.execute("TRUNCATE budget_item, annual_budget, city RESTART IDENTITY");
    }

    @Test
    void knownCityWithoutBudgetInRequestedYearHasZeroFigures() {
        importCity("alder", 10, "EDUCATION");

        KeyFigures figures = client.getForObject("/api/cities/alder/figures?year=2023", KeyFigures.class);

        assertThat(figures.year()).isEqualTo(2023);
        assertThat(figures.cityName()).isEqualTo("City alder");
        assertThat(figures.spendingPerResident()).isEqualByComparingTo("0.00");
        assertThat(figures.educationShare()).isEqualByComparingTo("0.00");
    }

    @Test
    void existingZeroPopulationStillReportsZeroSpendingPerResident() {
        importCity("alder", 10, "EDUCATION");
        jdbc.update("UPDATE annual_budget SET population = 0 WHERE city_code = ? AND budget_year = ?", "alder", 2024);

        KeyFigures figures = client.getForObject("/api/cities/alder/figures", KeyFigures.class);

        assertThat(figures.spendingPerResident()).isEqualByComparingTo("0.00");
        assertThat(figures.educationShare()).isEqualByComparingTo("25.00");
    }

    @Test
    void countsOnlyTheExactEducationCategoryTowardEducationShare() {
        importCity("alder", 10, "education");

        KeyFigures figures = client.getForObject("/api/cities/alder/figures", KeyFigures.class);

        assertThat(figures.year()).isEqualTo(2024);
        assertThat(figures.spendingPerResident()).isEqualByComparingTo("8.00");
        assertThat(figures.educationShare()).isEqualByComparingTo("0.00");
    }

    @Test
    void comparisonPreservesTheRequestedCityOrder() {
        importCity("alder", 10, "EDUCATION");
        importCity("birch", 20, "EDUCATION");

        ComparisonRow[] rows = client.getForObject("/api/comparison?city=birch&city=alder", ComparisonRow[].class);

        assertThat(rows).extracting(ComparisonRow::cityCode).containsExactly("birch", "alder");
        assertThat(rows[0].value()).isEqualByComparingTo("4.00");
        assertThat(rows[1].value()).isEqualByComparingTo("8.00");
    }

    @Test
    void comparisonRepeatsRowsForDuplicateCityCodes() {
        importCity("alder", 10, "EDUCATION");
        importCity("birch", 20, "EDUCATION");

        ComparisonRow[] rows = client.getForObject(
                "/api/comparison?city=alder&city=birch&city=alder", ComparisonRow[].class);

        assertThat(rows).extracting(ComparisonRow::cityCode).containsExactly("alder", "birch", "alder");
    }

    @Test
    void unknownFigureFallsBackToEducationShare() {
        importCity("alder", 10, "EDUCATION");
        importCity("birch", 20, "EDUCATION");

        ComparisonRow[] rows = client.getForObject(
                "/api/comparison?city=alder&city=birch&figure=typo", ComparisonRow[].class);

        assertThat(rows[0].value()).isEqualByComparingTo("25.00");
        assertThat(rows[1].value()).isEqualByComparingTo("25.00");
    }

    @Test
    void unknownCityReturnsAnExplicitNotFoundError() {
        var response = client.getForEntity("/api/cities/unknown/figures", ApiError.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isEqualTo(new ApiError("CITY_NOT_FOUND", "City unknown was not found."));
    }

    private void importCity(String code, int population, String category) {
        var budget = new BudgetImport(code, "City " + code, population, 2024,
                List.of(new BudgetItem(category, new BigDecimal("20.00")),
                        new BudgetItem("PARKS", new BigDecimal("60.00"))));
        assertThat(client.postForEntity("/api/budgets", budget, Void.class).getStatusCode())
                .as("legacy import for %s", code)
                .isEqualTo(HttpStatus.NO_CONTENT);
    }
}
