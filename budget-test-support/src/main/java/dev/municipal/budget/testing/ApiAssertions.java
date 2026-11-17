package dev.municipal.budget.testing;

import java.math.BigDecimal;

import io.restassured.path.json.JsonPath;
import io.restassured.path.json.config.JsonPathConfig;
import io.restassured.response.Response;

import static org.assertj.core.api.Assertions.assertThat;

/** Checks API responses and includes the response body when the HTTP status differs. */
public final class ApiAssertions {
    private ApiAssertions() {
    }

    public static void status(Response response, int expected) {
        assertThat(response.statusCode())
                .as("HTTP status for response body <%s>", response.asString())
                .isEqualTo(expected);
    }

    public static void comparisonRow(Response response, int index, String cityCode, String value) {
        status(response, 200);
        JsonPath json = numbersAsDecimals(response);
        String row = "[" + index + "]";
        assertThat(json.getString(row + ".cityCode")).as("city code in comparison row %d", index)
                .isEqualTo(cityCode);
        decimal(json, row + ".value", cityCode, value);
    }

    static JsonPath numbersAsDecimals(Response response) {
        return response.jsonPath(new JsonPathConfig(JsonPathConfig.NumberReturnType.BIG_DECIMAL));
    }

    static void decimal(JsonPath json, String path, String cityCode, String expected) {
        String decimal = json.getString(path);
        assertThat(decimal).as("%s for city %s", path, cityCode).isNotNull();
        assertThat(new BigDecimal(decimal)).as("%s for city %s", path, cityCode).isEqualByComparingTo(expected);
    }
}
