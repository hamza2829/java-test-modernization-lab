package dev.municipal.budget;

import java.sql.SQLException;
import javax.sql.DataSource;

import dev.municipal.budget.testing.DatabaseReset;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
abstract class PostgresApiTest {
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    static {
        POSTGRES.start();
    }

    @Autowired
    private DataSource dataSource;

    @LocalServerPort
    private int port;

    @DynamicPropertySource
    static void useContainer(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @BeforeEach
    void resetDatabase() throws SQLException {
        DatabaseReset.clearBudgets(dataSource);
    }

    RequestSpecification api() {
        return new RequestSpecBuilder()
                .setBaseUri("http://127.0.0.1")
                .setPort(port)
                .setBasePath("/api")
                .build();
    }
}
