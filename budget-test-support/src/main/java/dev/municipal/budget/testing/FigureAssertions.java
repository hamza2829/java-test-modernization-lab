package dev.municipal.budget.testing;

import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;

import static org.assertj.core.api.Assertions.assertThat;

/** Compares reported key figures as exact decimals, independently of JSON formatting. */
public final class FigureAssertions {
    private FigureAssertions() {
    }

    public static void figures(Response response, String cityCode, int year, String spending, String educationShare) {
        ApiAssertions.status(response, 200);
        JsonPath json = ApiAssertions.numbersAsDecimals(response);
        assertThat(json.getString("cityCode")).as("city code in figures").isEqualTo(cityCode);
        assertThat(json.getInt("year")).as("figures year for city %s", cityCode).isEqualTo(year);
        ApiAssertions.decimal(json, "spendingPerResident", cityCode, spending);
        ApiAssertions.decimal(json, "educationShare", cityCode, educationShare);
    }
}
