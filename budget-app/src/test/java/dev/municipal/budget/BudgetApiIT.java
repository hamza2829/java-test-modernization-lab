package dev.municipal.budget;

import java.util.Map;

import dev.municipal.budget.testing.ApiAssertions;
import dev.municipal.budget.testing.BudgetImportBuilder;
import dev.municipal.budget.testing.FigureAssertions;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;
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
