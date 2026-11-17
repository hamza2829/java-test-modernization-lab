package dev.municipal.budget.testing;

import java.sql.SQLException;
import javax.sql.DataSource;

/** Restores an empty schema in the isolated PostgreSQL database used by a test suite. */
public final class DatabaseReset {
    private DatabaseReset() {
    }

    public static void clearBudgets(DataSource testDataSource) throws SQLException {
        try (var connection = testDataSource.getConnection(); var statement = connection.createStatement()) {
            statement.execute("TRUNCATE budget_item, annual_budget, city RESTART IDENTITY");
        }
    }
}
