import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/** Summarizes the app's raw Maven reports in the Actions run and its downloadable artifact. */
public final class Metrics {
    private Metrics() {
    }

    public static void main(String[] args) throws Exception {
        Path target = Path.of(args[0]);
        Document coverage = read(target.resolve("site/jacoco/jacoco.xml"));
        List<TestResult> unit = tests(target.resolve("surefire-reports"));
        List<TestResult> integration = tests(target.resolve("failsafe-reports"));
        int browser = count(integration.stream().filter(result -> result.name().endsWith("ComparisonPageIT")).toList());
        int characterization = count(integration.stream()
                .filter(result -> result.name().endsWith("BudgetCharacterizationIT")).toList());
        Document duplication = read(target.resolve("cpd.xml"));
        int duplicateBlocks = duplication.getElementsByTagName("duplication").getLength();

        System.out.println("| Metric | Result |");
        System.out.println("| --- | ---: |");
        row("Unit tests", count(unit));
        row("Characterization tests", characterization);
        row("API integration tests", count(integration) - characterization - browser);
        row("Playwright tests", browser);
        row("Total tests executed", count(unit) + count(integration));
        row("JaCoCo line coverage", coverage(coverage, "LINE"));
        row("JaCoCo branch coverage", coverage(coverage, "BRANCH"));
        row("SpotBugs warnings", occurrences(target, "spotbugsXml.xml", "BugInstance"));
        row("PMD warnings", occurrences(target, "pmd.xml", "violation"));
        row("Checkstyle warnings", occurrences(target, "checkstyle-result.xml", "error"));
        row("PMD CPD duplicate blocks", duplicateBlocks);
        row("Maven test build (seconds)", Files.readString(target.resolve("test-build-seconds.txt")).strip());
    }

    private static List<TestResult> tests(Path directory) throws Exception {
        if (!Files.isDirectory(directory)) {
            return List.of();
        }
        try (var paths = Files.list(directory)) {
            List<Path> reports = paths.filter(path -> path.getFileName().toString().startsWith("TEST-"))
                    .filter(path -> path.getFileName().toString().endsWith(".xml"))
                    .sorted()
                    .toList();
            var results = new java.util.ArrayList<TestResult>();
            for (Path report : reports) {
                Element suite = read(report).getDocumentElement();
                results.add(new TestResult(suite.getAttribute("name"),
                        number(suite, "tests") - number(suite, "skipped")));
            }
            return results;
        }
    }

    private static int count(List<TestResult> results) {
        return results.stream().mapToInt(TestResult::executed).sum();
    }

    private static String coverage(Document report, String type) throws IOException {
        for (Node node = report.getDocumentElement().getFirstChild(); node != null; node = node.getNextSibling()) {
            if (node instanceof Element counter && "counter".equals(counter.getTagName())
                    && type.equals(counter.getAttribute("type"))) {
                int covered = number(counter, "covered");
                int total = covered + number(counter, "missed");
                return String.format(Locale.ROOT, "%.2f%% (%d/%d)", 100.0 * covered / total, covered, total);
            }
        }
        throw new IOException("JaCoCo omitted the " + type + " counter");
    }

    private static int occurrences(Path target, String filename, String tag) throws Exception {
        return read(target.resolve(filename)).getElementsByTagName(tag).getLength();
    }

    private static int number(Element element, String attribute) {
        return Integer.parseInt(element.getAttribute(attribute));
    }

    private static Document read(Path path) throws Exception {
        var factory = DocumentBuilderFactory.newInstance();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        return factory.newDocumentBuilder().parse(path.toFile());
    }

    private static void row(String metric, Object result) {
        System.out.printf("| %s | %s |%n", metric, result);
    }

    private record TestResult(String name, int executed) {
    }
}
