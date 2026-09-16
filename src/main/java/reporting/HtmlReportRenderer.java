package reporting;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import enums.TestStatus;
import reporting.model.StepLog;
import reporting.model.SuiteRecord;
import reporting.model.TestAttempt;
import reporting.model.TestRecord;

/**
 * Renders a SuiteRecord into a self-contained custom HTML report (summary page
 * + one detail page per test).
 */
public final class HtmlReportRenderer {

    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private HtmlReportRenderer() {
    }

    public static void render(SuiteRecord suite, String reportDir) {
        try {
            Path root = Paths.get(reportDir);
            Path testsDir = root.resolve("tests");
            Files.createDirectories(testsDir);
            Files.writeString(root.resolve("style.css"), STYLE, StandardCharsets.UTF_8);
            Files.writeString(root.resolve("index.html"), renderIndex(suite), StandardCharsets.UTF_8);
            for (TestRecord test : suite.getTests()) {
                Files.writeString(testsDir.resolve(testFileName(test)), renderTestPage(test), StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to render HTML report", e);
        }
    }

    private static String testFileName(TestRecord test) {
        return sanitize(test.getTestName()) + ".html";
    }

    private static String sanitize(String value) {
        return value.replaceAll("[^a-zA-Z0-9_-]", "_");
    }

    private static String renderIndex(SuiteRecord suite) {
        List<TestRecord> tests = suite.getTests();
        long pass = tests.stream().filter(t -> t.getFinalStatus() == TestStatus.PASS).count();
        long fail = tests.stream().filter(t -> t.getFinalStatus() == TestStatus.FAIL).count();
        long skip = tests.stream().filter(t -> t.getFinalStatus() == TestStatus.SKIP).count();
        int maxAttempts = tests.stream().mapToInt(t -> t.getAttempts().size()).max().orElse(1);
        String title = reportTitle(suite);

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head><meta charset=\"UTF-8\">")
                .append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">")
                .append("<title>").append(escape(title))
                .append("</title><link rel=\"stylesheet\" href=\"style.css\"></head><body><div class=\"container\">");

        html.append("<header class=\"report-header\"><h1>").append(escape(title)).append("</h1></header>");

        html.append("<div class=\"table-card meta-card\"><table class=\"meta-table\">");
        appendMetaRow(html, "Platform Version", suite.getPlatformVersion());
        appendMetaRow(html, "App Under Test", suite.getAppPath());
        appendMetaRow(html, "Device UDID(s)", suite.getDeviceUdids());
        appendMetaRow(html, "Start Date/Time", format(suite.getStartTime()));
        appendMetaRow(html, "End Date/Time", format(suite.getEndTime()));
        appendMetaRow(html, "Total Test Duration", formatDuration(suite.getStartTime(), suite.getEndTime()));
        appendMetaRow(html, "Total Tests Planned to Run", String.valueOf(suite.getPlannedCount()));
        appendMetaRow(html, "Total Ran", String.valueOf(tests.size()));
        appendMetaRow(html, "Total Pass", String.valueOf(pass) + " (" + formatPercent(pass, tests.size()) + ")");
        appendMetaRow(html, "Total Fail", String.valueOf(fail) + " (" + formatPercent(fail, tests.size()) + ")");
        appendMetaRow(html, "Total Skip", String.valueOf(skip));
        html.append("</table></div>");

        html.append("<div class=\"progress-bar\">");
        if (tests.isEmpty()) {
            html.append("<div class=\"progress-bar-fill progress-empty\" style=\"width:100%\"></div>");
        } else {
            appendProgressSegment(html, "pass", pass, tests.size());
            appendProgressSegment(html, "fail", fail, tests.size());
            appendProgressSegment(html, "skip", skip, tests.size());
        }
        html.append("</div>");

        html.append(
                "<div class=\"table-card\"><table class=\"tests\"><thead><tr><th>Test Name</th><th>Platform</th><th>Duration</th><th>Result</th>");
        for (int i = 1; i < maxAttempts; i++) {
            html.append("<th>Retry #").append(i).append("</th>");
        }
        html.append("</tr></thead><tbody>");
        for (TestRecord test : tests) {
            html.append("<tr>");
            html.append("<td><a class=\"test-link\" href=\"tests/").append(escape(testFileName(test))).append("\">")
                    .append(escape(test.getTestName())).append("</a></td>");
            html.append("<td>").append(escape(test.getPlatform())).append("</td>");
            html.append("<td>").append(formatDuration(test.getStartTime(), test.getEndTime())).append("</td>");
            html.append(statusCell(test.getFinalStatus().name()));

            List<TestAttempt> attempts = test.getAttempts();
            for (int i = 1; i < maxAttempts; i++) {
                html.append(i < attempts.size() ? statusCell(attempts.get(i).getStatus().name()) : "<td>&ndash;</td>");
            }
            html.append("</tr>");
        }
        html.append("</tbody></table></div>");
        html.append("<a href=\"#\" class=\"back-to-top\" title=\"Back to top\" "
                + "onclick=\"window.scrollTo({top:0,behavior:'smooth'});return false;\">&uarr;</a>");
        html.append("</div></body></html>");
        return html.toString();
    }

    private static String renderTestPage(TestRecord test) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head><meta charset=\"UTF-8\">")
                .append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">")
                .append("<title>").append(escape(test.getTestName()))
                .append("</title><link rel=\"stylesheet\" href=\"../style.css\"></head><body><div class=\"container\">");
        html.append("<a class=\"btn-back\" href=\"../index.html\">&larr; Back to summary</a>");
        html.append("<header class=\"report-header test-header\"><h1>").append(escape(test.getTestName()))
                .append("</h1>")
                .append("<p class=\"header-meta\">").append(statusPill(test.getFinalStatus().name()))
                .append(" <span class=\"platform-pill\">").append(escape(test.getPlatform()))
                .append("</span></p></header>");

        for (TestAttempt attempt : test.getAttempts()) {
            String label = attempt.getAttemptNumber() == 1
                    ? "Attempt #1 (initial run)"
                    : "Retry attempt #" + (attempt.getAttemptNumber() - 1);
            html.append("<details class=\"attempt-card\" open><summary>")
                    .append("<span class=\"attempt-label\">").append(escape(label)).append("</span>")
                    .append(statusPill(attempt.getStatus().name()))
                    .append("<span class=\"time\">").append(format(attempt.getStartTime())).append(" &rarr; ")
                    .append(format(attempt.getEndTime())).append("</span>")
                    .append("</summary>");
            html.append("<div class=\"attempt-body\">");
            if (attempt.getErrorMessage() != null) {
                html.append("<p class=\"error\">").append(escape(attempt.getErrorMessage())).append("</p>");
            }
            html.append("<ul class=\"steps\">");
            for (StepLog step : attempt.getSteps()) {
                html.append("<li class=\"step-").append(step.getStatus().name().toLowerCase()).append("\">");
                html.append("<div class=\"step-line\">");
                html.append("<span class=\"time\">").append(format(step.getTimestamp())).append("</span>");
                html.append("<span class=\"badge badge-").append(step.getStatus().name().toLowerCase()).append("\">")
                        .append(step.getStatus()).append("</span>");
                html.append(stepMessage(step.getMessage()));
                html.append("</div>");
                if (step.getScreenshotPath() != null) {
                    String src = escape(relativeScreenshotPath(step.getScreenshotPath()));
                    html.append("<a href=\"").append(src).append("\" target=\"_blank\">")
                            .append("<img class=\"screenshot\" src=\"").append(src).append("\"/></a>");
                }
                html.append("</li>");
            }
            html.append("</ul></div></details>");
        }
        html.append("</div></body></html>");
        return html.toString();
    }

    /**
     * Multi-line messages (e.g. a stack trace) are collapsed behind an expandable
     * summary; single-line ones show as-is.
     */
    private static String stepMessage(String message) {
        int newlineIndex = message.indexOf('\n');
        if (newlineIndex < 0) {
            return "<span class=\"step-message\">" + escape(message) + "</span>";
        }
        String summary = message.substring(0, newlineIndex);
        return "<details class=\"step-details\"><summary class=\"step-message\">" + escape(summary) + "</summary>"
                + "<pre class=\"stack-trace\">" + escape(message) + "</pre></details>";
    }

    private static String relativeScreenshotPath(String absolutePath) {
        // Screenshots live under <reportDir>/screenshots; test detail pages live under
        // <reportDir>/tests/.
        return "../screenshots/" + Paths.get(absolutePath).getFileName();
    }

    private static String statusCell(String status) {
        return "<td>" + statusPill(status) + "</td>";
    }

    private static String statusPill(String status) {
        return "<span class=\"pill pill-" + status.toLowerCase() + "\">" + status + "</span>";
    }

    private static void appendMetaRow(StringBuilder html, String label, String value) {
        html.append("<tr><td class=\"meta-label\">").append(escape(label)).append("</td><td class=\"meta-value\">")
                .append(escape(value == null || value.isEmpty() ? "-" : value)).append("</td></tr>");
    }

    private static String formatPercent(long count, long total) {
        return total <= 0 ? "0%" : String.format("%.0f%%", count * 100.0 / total);
    }

    private static void appendProgressSegment(StringBuilder html, String type, long count, long total) {
        if (count <= 0) {
            return;
        }
        double percent = count * 100.0 / total;
        html.append("<div class=\"progress-bar-fill progress-").append(type).append("\" style=\"width:")
                .append(String.format("%.2f", percent)).append("%\"></div>");
    }

    private static String reportTitle(SuiteRecord suite) {
        return String.join(" - ", suite.getProductName(), suite.getTeamName(), suite.getRunType(),
                suite.getEnvironment());
    }

    private static String format(ZonedDateTime time) {
        return time == null ? "-" : time.format(TIMESTAMP_FORMAT);
    }

    private static String formatDuration(ZonedDateTime start, ZonedDateTime end) {
        if (start == null || end == null) {
            return "-";
        }
        Duration duration = Duration.between(start, end);
        return String.format("%02d:%02d:%02d", duration.toHours(), duration.toMinutesPart(), duration.toSecondsPart());
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }

    private static final String STYLE = ":root { --pass: #1e7e34; --fail: #c0392b; --skip: #b8860b; --info: #2c3e50; --accent: #3f6ad8; }"
            + "* { box-sizing: border-box; }"
            + "body { font-family: 'Segoe UI', Roboto, Arial, sans-serif; margin: 0; padding: 24px 0 48px; "
            + "background: #f2f4f7; color: #1f2937; }"
            + ".container { max-width: 80%; margin: 0 auto; padding: 0 32px; }"
            + "a { color: var(--accent); text-decoration: none; }"
            + "a:hover { text-decoration: underline; }"

            + ".report-header { background: linear-gradient(135deg, #2c3e50, #3f6ad8); color: #fff; "
            + "padding: 24px 28px; border-radius: 10px; box-shadow: 0 4px 14px rgba(0,0,0,0.12); margin-bottom: 20px; }"
            + ".report-header h1 { margin: 0 0 6px; font-size: 1.5rem; }"
            + ".header-meta { margin: 0; opacity: 0.9; font-size: 0.9rem; }"
            + ".test-header .header-meta { display: flex; align-items: center; gap: 8px; }"

            + ".btn-back { display: inline-block; margin-bottom: 14px; padding: 6px 14px; background: #fff; "
            + "border-radius: 6px; box-shadow: 0 1px 4px rgba(0,0,0,0.15); font-size: 0.85rem; }"

            + ".platform-pill { background: rgba(255,255,255,0.2); padding: 3px 10px; border-radius: 10px; font-size: 0.8rem; }"

            + ".progress-bar { display: flex; height: 10px; border-radius: 6px; overflow: hidden; background: #e5e7eb; "
            + "margin-bottom: 22px; }"
            + ".progress-bar-fill { height: 100%; }"
            + ".progress-pass { background: var(--pass); }"
            + ".progress-fail { background: var(--fail); }"
            + ".progress-skip { background: var(--skip); }"
            + ".progress-empty { background: #d1d5db; }"

            + ".table-card { background: #fff; border-radius: 10px; overflow: hidden; box-shadow: 0 1px 4px rgba(0,0,0,0.08); }"
            + ".meta-card { display: inline-block; padding: 4px 20px; margin-bottom: 16px; text-align: left; }"
            + ".meta-table { width: auto; }"
            + ".meta-table td { padding: 5px 10px; border: none; font-size: 0.88rem; text-align: left; }"
            + ".meta-table td.meta-label { color: #6b7280; font-weight: 600; padding-right: 24px; white-space: nowrap; }"
            + ".meta-table td.meta-value { color: #1f2937; font-weight: 500; }"
            + ".meta-table tr:nth-child(odd) { background: #f8fafc; }"
            + "table { border-collapse: collapse; width: 100%; }"
            + "table.tests th, table.tests td { padding: 10px 14px; border-bottom: 1px solid #f0f1f3; text-align: left; font-size: 0.9rem; }"
            + "table.tests th { background: var(--info); color: #fff; font-weight: 600; position: sticky; top: 0; }"
            + "table.tests tbody tr:hover { background: #f8fafc; }"
            + "table.tests tbody tr:last-child td { border-bottom: none; }"
            + "a.test-link { font-weight: 600; }"

            + ".pill { display: inline-block; padding: 3px 11px; border-radius: 12px; font-size: 0.75rem; "
            + "font-weight: 700; text-transform: uppercase; letter-spacing: 0.02em; }"
            + ".pill-pass { background: #e6f4ea; color: var(--pass); }"
            + ".pill-fail { background: #fdecea; color: var(--fail); }"
            + ".pill-skip { background: #fff4e0; color: var(--skip); }"

            + ".attempt-card { background: #fff; border-radius: 10px; box-shadow: 0 1px 4px rgba(0,0,0,0.08); "
            + "margin-bottom: 16px; overflow: hidden; }"
            + ".attempt-card summary { list-style: none; cursor: pointer; padding: 14px 18px; display: flex; "
            + "align-items: center; gap: 10px; font-weight: 600; background: #f8fafc; }"
            + ".attempt-card summary::-webkit-details-marker { display: none; }"
            + ".attempt-card summary::before { content: '\\25B6'; font-size: 0.7rem; color: #6b7280; transition: transform 0.15s; }"
            + ".attempt-card[open] summary::before { transform: rotate(90deg); }"
            + ".attempt-label { margin-right: 4px; }"
            + ".attempt-body { padding: 6px 18px 16px; }"

            + "ul.steps { list-style: none; padding-left: 0; margin: 0; }"
            + "ul.steps li { padding: 8px 10px; border-left: 3px solid #e5e7eb; margin-bottom: 4px; border-radius: 4px; background: #fafafa; }"
            + ".step-line { display: flex; align-items: flex-start; gap: 8px; flex-wrap: wrap; }"
            + ".step-fail { border-left-color: var(--fail); background: #fdecea; }"
            + ".step-warn { border-left-color: var(--skip); background: #fff8e1; }"
            + ".step-pass { border-left-color: var(--pass); }"
            + ".step-info { border-left-color: #93a2b8; }"
            + ".badge { font-weight: 700; font-size: 0.7rem; padding: 2px 8px; border-radius: 10px; text-transform: uppercase; }"
            + ".badge-pass { background: #e6f4ea; color: var(--pass); }"
            + ".badge-fail { background: #fdecea; color: var(--fail); }"
            + ".badge-warn { background: #fff4e0; color: var(--skip); }"
            + ".badge-info { background: #e8edf5; color: var(--info); }"
            + ".step-message { flex: 1; white-space: pre-wrap; }"
            + ".step-details { flex: 1; }"
            + ".step-details summary { cursor: pointer; list-style: none; display: flex; align-items: center; gap: 6px; }"
            + ".step-details summary::-webkit-details-marker { display: none; }"
            + ".step-details summary::marker { content: ''; }"
            + ".step-details summary::before { content: '\\25B6'; font-size: 0.65rem; color: #6b7280; "
            + "display: inline-block; transition: transform 0.15s; }"
            + ".step-details[open] summary::before { transform: rotate(90deg); }"
            + ".stack-trace { margin: 8px 0 0; padding: 10px 12px; background: #1f2937; color: #e5e7eb; "
            + "border-radius: 6px; font-size: 0.78rem; overflow-x: auto; white-space: pre; }"
            + ".time { color: #888; font-size: 0.8rem; white-space: nowrap; }"
            + ".screenshot { max-width: 320px; display: block; margin-top: 8px; border: 1px solid #ddd; border-radius: 6px; "
            + "box-shadow: 0 1px 4px rgba(0,0,0,0.1); }"
            + ".error { color: var(--fail); background: #fdecea; padding: 8px 12px; border-radius: 6px; margin: 0 0 10px; }"
            + ".back-to-top { position: fixed; bottom: 24px; right: 24px; width: 44px; height: 44px; border-radius: 50%; "
            + "background: var(--accent); color: #fff; display: flex; align-items: center; justify-content: center; "
            + "font-size: 1.3rem; font-weight: 700; box-shadow: 0 2px 8px rgba(0,0,0,0.25); text-decoration: none; }"
            + ".back-to-top:hover { background: #2f52ad; text-decoration: none; }";
}