package dev.municipal.budget;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import dev.municipal.budget.testing.ApiAssertions;
import dev.municipal.budget.testing.BudgetImportBuilder;
import dev.municipal.budget.testing.FigureAssertions;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

class BudgetApiIT extends PostgresApiTest {
    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void importsBudgetsThenCalculatesAndComparesBothCities() {
        importBudget(new BudgetImportBuilder("alder", "Alder Harbor")
                .item("EDUCATION", "60000.00").item("TRANSPORT", "40000.00"));
        importBudget(new BudgetImportBuilder("birch", "Birch Falls").withPopulation(800)
                .item("EDUCATION", "40000.00").item("PARKS", "60000.00"));

        Response cities = given(api()).get("/cities");
        ApiAssertions.status(cities, 200);
        assertThat(cities.jsonPath().getList("name", String.class)).containsExactly("Alder Harbor", "Birch Falls");

        FigureAssertions.figures(figuresFor("alder", 2024), "alder", 2024, "100.00", "60.00");
        FigureAssertions.figures(figuresFor("birch", 2024), "birch", 2024, "125.00", "40.00");

        Response comparison = given(api()).queryParam("city", "alder", "birch")
                .queryParam("year", 2024).queryParam("figure", "education-share").get("/comparison");
        ApiAssertions.comparisonRow(comparison, 0, "alder", "60.00");
        ApiAssertions.comparisonRow(comparison, 1, "birch", "40.00");
        assertThat(comparison.jsonPath().getList("cityCode", String.class)).containsExactly("alder", "birch");
    }

    @Test
    void replacingOneYearPreservesThePreviousYearsBudgetAndPopulation() {
        importBudget(new BudgetImportBuilder("alder", "Alder Harbor").forYear(2023).withPopulation(3)
                .item("EDUCATION", "10.00"));
        importBudget(new BudgetImportBuilder("alder", "Alder Harbor").withPopulation(2)
                .item("EDUCATION", "30.00"));
        importBudget(new BudgetImportBuilder("alder", "Alder Harbor").withPopulation(4)
                .item("PARKS", "20.00"));

        FigureAssertions.figures(figuresFor("alder", 2023), "alder", 2023, "3.33", "100.00");
        FigureAssertions.figures(figuresFor("alder", 2024), "alder", 2024, "5.00", "0.00");
    }

    @Test
    void aYearWithZeroSpendingHasZeroFigures() {
        importBudget(new BudgetImportBuilder("alder", "Alder Harbor").item("EDUCATION", "0.00"));

        FigureAssertions.figures(figuresFor("alder", 2024), "alder", 2024, "0.00", "0.00");
    }

    @Test
    void comparisonFailsIfAnyCityIsUnknown() {
        importBudget(new BudgetImportBuilder("alder", "Alder Harbor").item("EDUCATION", "5.00"));

        Response response = given(api()).queryParam("city", "alder", "unknown").get("/comparison");

        ApiAssertions.problem(response, 404, "CITY_NOT_FOUND", "City unknown was not found.");
    }

    @Test
    void rejectsMalformedImportJson() {
        Response response = given(api()).contentType(ContentType.JSON).body("{").post("/budgets");

        ApiAssertions.problem(response, 400, "INVALID_BODY", "Send a valid budget import JSON object.");
    }

    @Test
    void figuresRejectANonIntegerYear() {
        Response response = given(api()).queryParam("year", "this-year").get("/cities/alder/figures");

        ApiAssertions.problem(response, 400, "INVALID_QUERY",
                "Provide the required query parameters in the expected format.");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidImports")
    void invalidImportsLeaveExistingDataUntouched(String scenario, String key, Object value, String field) {
        importBudget(new BudgetImportBuilder("alder", "Alder Harbor").item("EDUCATION", "100.00"));
        Map<String, Object> replacement = new BudgetImportBuilder("alder", "Renamed city").item("PARKS", "1.00").build();
        replacement.put(key, value);

        ApiAssertions.invalidBudgetField(sendBudget(replacement), field);
        FigureAssertions.figures(figuresFor("alder", 2024), "alder", 2024, "0.10", "100.00");
        assertThat(given(api()).get("/cities").jsonPath().getList("name", String.class))
                .as("catalog after rejected import: %s", scenario)
                .containsExactly("Alder Harbor");
    }

    static Stream<Arguments> invalidImports() {
        return Stream.of(
                Arguments.of("non-slug city code", "cityCode", "bad code", "cityCode"),
                Arguments.of("blank city name", "cityName", " ", "cityName"),
                Arguments.of("zero population", "population", 0, "population"),
                Arguments.of("year outside range", "year", 1999, "year"),
                Arguments.of("empty items", "items", List.of(), "items"),
                Arguments.of("null item", "items", Collections.singletonList(null), "items[0]"),
                Arguments.of("blank category", "items", List.of(Map.of("category", " ", "amount", 1)),
                        "items[0].category"),
                Arguments.of("missing amount", "items", List.of(Map.of("category", "PARKS")), "items[0].amount"),
                Arguments.of("negative amount", "items", List.of(Map.of("category", "PARKS", "amount", -1)),
                        "items[0].amount"),
                Arguments.of("amount exceeds database precision", "items",
                        List.of(Map.of("category", "PARKS", "amount", new BigDecimal("1.001"))), "items[0].amount"));
    }

    @Test
    void fractionalPopulationCannotBeSilentlyTruncated() {
        Map<String, Object> budget = new BudgetImportBuilder("alder", "Alder Harbor").item("EDUCATION", "1.00").build();
        budget.put("population", new BigDecimal("2.5"));

        ApiAssertions.problem(sendBudget(budget), 400, "INVALID_BODY", "Send a valid budget import JSON object.");
    }

    @Test
    void databaseFailureRollsBackTheEntireReplacement() {
        importBudget(new BudgetImportBuilder("alder", "Alder Harbor").withPopulation(2)
                .item("EDUCATION", "100.00"));
        jdbc.execute("ALTER TABLE budget_item ADD CONSTRAINT reject_test_item CHECK (category <> 'DENIED')");
        try {
            Response failure = sendBudget(new BudgetImportBuilder("alder", "Renamed city")
                    .withPopulation(5).item("PARKS", "40.00").item("DENIED", "60.00").build());

            ApiAssertions.problem(failure, 500, "STORAGE_FAILURE", "The budget could not be read or saved.");
            Response figures = figuresFor("alder", 2024);
            FigureAssertions.figures(figures, "alder", 2024, "50.00", "100.00");
            assertThat(figures.jsonPath().getString("cityName")).isEqualTo("Alder Harbor");
        } finally {
            jdbc.execute("ALTER TABLE budget_item DROP CONSTRAINT reject_test_item");
        }
    }

    private void importBudget(BudgetImportBuilder budget) {
        ApiAssertions.status(sendBudget(budget.build()), 204);
    }

    private Response sendBudget(Map<String, Object> body) {
        return given(api()).contentType(ContentType.JSON).body(body).post("/budgets");
    }

    private Response figuresFor(String code, int year) {
        return given(api()).queryParam("year", year).get("/cities/{code}/figures", code);
    }
}
