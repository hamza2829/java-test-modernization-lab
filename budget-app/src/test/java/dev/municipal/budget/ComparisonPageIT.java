package dev.municipal.budget;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import javax.sql.DataSource;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.AriaRole;
import dev.municipal.budget.testing.BudgetImportBuilder;
import dev.municipal.budget.testing.DatabaseReset;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ComparisonPageIT {
    private static Playwright playwright;
    private static Browser browser;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private TestRestTemplate client;

    @LocalServerPort
    private int port;

    private Page page;

    @BeforeAll
    static void launchBrowser() {
        playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
    }

    @AfterAll
    static void stopBrowser() {
        if (playwright != null) {
            playwright.close();
        }
    }

    @BeforeEach
    void openComparisonPage() throws SQLException {
        DatabaseReset.clearBudgets(dataSource);
        importBudget(new BudgetImportBuilder("alder", "Alder Harbor")
                .item("EDUCATION", "60000.00").item("TRANSPORT", "40000.00"));
        importBudget(new BudgetImportBuilder("birch", "Birch Falls").withPopulation(800)
                .item("EDUCATION", "40000.00").item("PARKS", "60000.00"));
        page = browser.newPage(new Browser.NewPageOptions().setViewportSize(1024, 768));
        page.navigate("http://127.0.0.1:" + port + "/");
    }

    @AfterEach
    void captureScreenshot(TestInfo test) throws IOException {
        if (page != null) {
            try {
                Path screenshots = Files.createDirectories(Path.of("target", "screenshots"));
                String filename = test.getTestMethod().orElseThrow().getName() + ".png";
                page.screenshot(new Page.ScreenshotOptions().setPath(screenshots.resolve(filename)).setFullPage(true));
            } finally {
                page.context().close();
            }
        }
    }

    @Test
    void selectedCitiesAppearWithTheirEducationShares() {
        Locator alder = page.getByRole(AriaRole.CHECKBOX, name("Alder Harbor"));
        Locator birch = page.getByRole(AriaRole.CHECKBOX, name("Birch Falls"));
        alder.check();
        birch.check();
        page.getByLabel("Budget year").fill("2024");
        page.getByLabel("Key figure").selectOption("education-share");

        page.getByRole(AriaRole.BUTTON, name("Compare cities")).click();

        Locator table = page.getByRole(AriaRole.TABLE, name("Comparison for 2024"));
        assertThat(table).isVisible();
        assertThat(table.getByRole(AriaRole.COLUMNHEADER)).hasText(new String[]{"City", "Education share (%)"});
        assertThat(table.getByRole(AriaRole.ROWHEADER)).hasText(new String[]{"Alder Harbor", "Birch Falls"});
        assertThat(table.getByRole(AriaRole.CELL)).hasText(new String[]{"60.00", "40.00"});
        assertThat(alder).isChecked();
        assertThat(birch).isChecked();
    }

    @Test
    void submittingWithoutCitiesShowsAnErrorAndNoTable() {
        page.getByRole(AriaRole.BUTTON, name("Compare cities")).click();

        assertThat(page.getByRole(AriaRole.ALERT)).hasText("Select between 2 and 10 cities.");
        assertThat(page.getByRole(AriaRole.TABLE)).hasCount(0);
    }

    @Test
    void anUnknownCityInASavedComparisonShowsAnError() {
        page.navigate("http://127.0.0.1:" + port + "/?city=alder&city=unknown&compare=true");

        assertThat(page.getByRole(AriaRole.ALERT)).hasText("City unknown was not found.");
        assertThat(page.getByRole(AriaRole.TABLE)).hasCount(0);
    }

    private void importBudget(BudgetImportBuilder budget) {
        var response = client.postForEntity("/api/budgets", budget.build(), Void.class);
        org.assertj.core.api.Assertions.assertThat(response.getStatusCode())
                .as("seed budget for the comparison page").isEqualTo(HttpStatus.NO_CONTENT);
    }

    private Page.GetByRoleOptions name(String name) {
        return new Page.GetByRoleOptions().setName(name).setExact(true);
    }
}
