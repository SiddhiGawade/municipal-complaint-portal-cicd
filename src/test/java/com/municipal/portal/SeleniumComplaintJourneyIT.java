package com.municipal.portal;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.openqa.selenium.By;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

class SeleniumComplaintJourneyIT {

    private static final Duration WAIT_TIMEOUT = Duration.ofSeconds(10);
    private static final Pattern TOKEN_PATTERN = Pattern.compile("CMP-2026-\\d+");
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static int serverPort;
    private static Process appProcess;

    private WebDriver driver;
    private WebDriverWait wait;

    @RegisterExtension
    final AfterTestExecutionCallback screenshotOnFailure = context -> {
        if (context.getExecutionException().isPresent()) {
            captureFailureScreenshot(context.getRequiredTestMethod().getName());
        }
    };

    @BeforeAll
    static void startPortal() throws Exception {
        serverPort = findFreePort();
        Path classes = Path.of(App.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        Path appLog = Path.of("target", "selenium-app.log");
        Files.createDirectories(appLog.getParent());
        appProcess = new ProcessBuilder(
                Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                "-cp", classes.toString(), App.class.getName(), Integer.toString(serverPort), "--demo")
                .redirectErrorStream(true)
                .redirectOutput(appLog.toFile())
                .start();

        Instant deadline = Instant.now().plus(15, ChronoUnit.SECONDS);
        while (Instant.now().isBefore(deadline)) {
            if (!appProcess.isAlive()) {
                throw new IllegalStateException("Portal process exited; inspect " + appLog);
            }
            try {
                HttpResponse<String> response = HTTP.send(
                        HttpRequest.newBuilder(URI.create(baseUrl() + "/api/health")).GET().build(),
                        HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    return;
                }
            } catch (IOException ignored) {
                Thread.sleep(250);
            }
        }
        throw new IllegalStateException("Portal did not start at " + baseUrl());
    }

    @AfterAll
    static void stopPortal() throws InterruptedException {
        if (appProcess != null) {
            appProcess.destroy();
            if (!appProcess.waitFor(3, java.util.concurrent.TimeUnit.SECONDS)) {
                appProcess.destroyForcibly();
            }
        }
    }

    @BeforeEach
    void openBrowser() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new", "--no-sandbox", "--disable-dev-shm-usage",
                "--window-size=1440,1000");
        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, WAIT_TIMEOUT);
        driver.get(baseUrl() + "/#track");
        wait.until(d -> d.findElements(By.id("track-token")).size() == 1);
    }

    @AfterEach
    void closeBrowser() {
        if (driver != null) {
            driver.quit();
        }
    }

    @Test
    void citizenCanTrackSeededComplaint() {
        assertTrue(driver.getTitle().contains("Municipal Complaint Portal"));

        driver.findElement(By.id("track-token")).clear();
        driver.findElement(By.id("track-token")).sendKeys("CMP-2026-1001");
        driver.findElement(By.xpath("//button[normalize-space()='Track Status']")).click();

        String result = wait.until(d -> {
            String text = d.findElement(By.id("track-res")).getText();
            return text.contains("Pending") ? text : null;
        });
        assertTrue(result.contains("Rahul Deshmukh"));
        assertTrue(result.contains("Roads & potholes"));
    }

    @Test
    void citizenCanSubmitAndThenTrackComplaint() {
        driver.get(baseUrl() + "/#submit");
        driver.findElement(By.id("name")).clear();
        driver.findElement(By.id("name")).sendKeys("Selenium Test Citizen");
        driver.findElement(By.id("contact")).clear();
        driver.findElement(By.id("contact")).sendKeys("9876543210");
        new Select(driver.findElement(By.id("category")))
                .selectByVisibleText("Water Supply & Leakage");
        driver.findElement(By.id("ward")).clear();
        driver.findElement(By.id("ward")).sendKeys("Ward 21");
        driver.findElement(By.id("desc")).clear();
        driver.findElement(By.id("desc"))
                .sendKeys("Water is leaking near the community hall entrance.");
        driver.findElement(By.xpath("//button[normalize-space()='Submit Complaint']")).click();

        String submission = wait.until(d -> {
            String text = d.findElement(By.id("sub-res")).getText();
            return text.contains("SUCCESS") ? text : null;
        });
        Matcher tokenMatch = TOKEN_PATTERN.matcher(submission);
        assertTrue(tokenMatch.find(), "Successful response should include a tracking token");
        String token = tokenMatch.group();

        driver.findElement(By.cssSelector("a[data-route='track']")).click();
        driver.findElement(By.id("track-token")).clear();
        driver.findElement(By.id("track-token")).sendKeys(token);
        driver.findElement(By.xpath("//button[normalize-space()='Track Status']")).click();
        String tracked = wait.until(d -> {
            String text = d.findElement(By.id("track-res")).getText();
            return text.contains(token) ? text : null;
        });

        assertTrue(tracked.contains("Selenium Test Citizen"));
        assertTrue(tracked.contains("Water supply"));
        assertTrue(tracked.contains("Pending"));
    }

    @Test
    void invalidComplaintSubmissionShowsValidationError() {
        driver.get(baseUrl() + "/#submit");
        driver.findElement(By.id("contact")).clear();
        driver.findElement(By.id("contact")).sendKeys("123");
        driver.findElement(By.xpath("//button[normalize-space()='Submit Complaint']")).click();

        String response = wait.until(d -> {
            String text = d.findElement(By.id("sub-res")).getText();
            return text.contains("Invalid complaint input data") ? text : null;
        });
        assertTrue(response.contains("error"));
    }

    @Test
    void unknownTrackingTokenShowsNotFound() {
        driver.findElement(By.id("track-token")).clear();
        driver.findElement(By.id("track-token")).sendKeys("CMP-2099-9999");
        driver.findElement(By.xpath("//button[normalize-space()='Track Status']")).click();

        String response = wait.until(d -> {
            String text = d.findElement(By.id("track-res")).getText();
            return text.contains("Complaint Token Not Found") ? text : null;
        });
        assertTrue(response.contains("Complaint Token Not Found"));
    }

    @Test
    void multiCategorySubmissionAndPersistence() {
        driver.get(baseUrl() + "/#submit");
        driver.findElement(By.id("name")).clear();
        driver.findElement(By.id("name")).sendKeys("Pooja Sharma");
        driver.findElement(By.id("contact")).clear();
        driver.findElement(By.id("contact")).sendKeys("9812345678");
        new Select(driver.findElement(By.id("category")))
                .selectByVisibleText("Solid Waste Management");
        driver.findElement(By.id("ward")).clear();
        driver.findElement(By.id("ward")).sendKeys("Ward 05");
        driver.findElement(By.id("desc")).clear();
        driver.findElement(By.id("desc"))
                .sendKeys("Garbage bins are overflowing near the secondary school playground.");
        driver.findElement(By.xpath("//button[normalize-space()='Submit Complaint']")).click();

        String submission = wait.until(d -> {
            String text = d.findElement(By.id("sub-res")).getText();
            return text.contains("SUCCESS") ? text : null;
        });
        Matcher tokenMatch = TOKEN_PATTERN.matcher(submission);
        assertTrue(tokenMatch.find(), "Submission must return generated tracking token");
        String token = tokenMatch.group();

        driver.findElement(By.cssSelector("a[data-route='track']")).click();
        driver.findElement(By.id("track-token")).clear();
        driver.findElement(By.id("track-token")).sendKeys(token);
        driver.findElement(By.xpath("//button[normalize-space()='Track Status']")).click();

        String tracked = wait.until(d -> {
            String text = d.findElement(By.id("track-res")).getText();
            return text.contains(token) ? text : null;
        });
        assertTrue(tracked.contains("Pooja Sharma"));
        assertTrue(tracked.contains("Waste management"));
        assertTrue(tracked.contains("Pending"));
        assertTrue(tracked.contains("Garbage bins are overflowing"));
    }

    @Test
    void overviewDisplaysRealSessionCounts() throws Exception {
        driver.get(baseUrl() + "/#overview");
        wait.until(d -> d.findElements(By.cssSelector(".stat-value")).size() == 4);
        String dashboard = HTTP.send(HttpRequest.newBuilder(URI.create(baseUrl() + "/api/dashboard")).GET().build(),
                HttpResponse.BodyHandlers.ofString()).body();
        Matcher total = Pattern.compile("\"total\":(\\d+)").matcher(dashboard);
        assertTrue(total.find());
        assertEquals(Integer.parseInt(total.group(1)),
                Integer.parseInt(driver.findElement(By.cssSelector(".stat-value")).getText()));
        assertTrue(driver.findElement(By.cssSelector(".evidence-strip")).getText().contains("Recorded"));
    }

    @Test
    void complaintSearchAndFiltersWorkWithoutReloading() {
        driver.get(baseUrl() + "/#complaints");
        wait.until(d -> d.findElements(By.id("complaint-search")).size() == 1);
        driver.findElement(By.id("complaint-search")).sendKeys("Rahul");
        assertEquals(1, driver.findElements(By.cssSelector("tbody tr")).size());
        assertTrue(driver.findElement(By.cssSelector("tbody")).getText().contains("CMP-2026-1001"));
        driver.findElement(By.id("complaint-search")).clear();
        new Select(driver.findElement(By.id("status-filter"))).selectByValue("RESOLVED");
        assertFalse(driver.findElements(By.cssSelector("tbody .status")).isEmpty());
        assertTrue(driver.findElements(By.cssSelector("tbody .status")).stream().allMatch(e -> e.getText().equals("Resolved")));
        driver.findElement(By.id("complaint-search")).sendKeys("no-such-citizen");
        assertTrue(driver.findElement(By.id("complaint-results")).getText().contains("No complaints found"));
    }

    @Test
    void reviewerCanCompleteAnActualComplaintLifecycle() throws Exception {
        HttpResponse<String> created = HTTP.send(HttpRequest.newBuilder(URI.create(baseUrl() + "/api/complaints"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString("citizenName=Reviewer+Journey&citizenContact=demo%40example.com"
                        + "&category=Roads+%26+Potholes&ward=Ward+07&description=Road+maintenance+needed+near+the+local+school.")).build(),
                HttpResponse.BodyHandlers.ofString());
        Matcher matcher = TOKEN_PATTERN.matcher(created.body());
        assertTrue(matcher.find());
        String token = matcher.group();
        driver.get(baseUrl() + "/#complaints");
        wait.until(d -> d.findElements(By.id("complaint-search")).size() == 1);
        driver.findElement(By.id("complaint-search")).sendKeys(token);
        driver.findElement(By.cssSelector("button.token-link")).click();
        for (String status : new String[]{"APPROVED", "IN_PROGRESS", "RESOLVED"}) {
            wait.until(d -> d.findElements(By.id("review-status")).size() == 1);
            new Select(driver.findElement(By.id("review-status"))).selectByValue(status);
            driver.findElement(By.id("review-remarks")).sendKeys("Recorded action " + status);
            driver.findElement(By.cssSelector("#review-form button[type=submit]")).click();
            wait.until(d -> d.findElement(By.id("complaint-dialog")).getText().contains("Recorded action " + status));
        }
        assertTrue(driver.findElements(By.id("review-form")).isEmpty());
        driver.findElement(By.cssSelector("#complaint-dialog [data-close]")).click();
        driver.get(baseUrl() + "/#track");
        driver.findElement(By.id("track-token")).clear();
        driver.findElement(By.id("track-token")).sendKeys(token);
        driver.findElement(By.cssSelector("#track-form button")).click();
        wait.until(d -> d.findElements(By.cssSelector("#track-res .timeline li")).size() == 4);
        assertTrue(driver.findElement(By.id("track-res")).getText().contains("Resolved"));
    }

    @Test
    void failedQualityGateIllustrationSkipsDeliveryStages() {
        driver.get(baseUrl() + "/#devops");
        new Select(driver.findElement(By.id("scenario"))).selectByValue("failure");
        for (int i = 0; i < 5; i++) driver.findElement(By.cssSelector("[data-lab=next]")).click();
        assertEquals(1, driver.findElements(By.cssSelector(".pipeline-node.failed")).size());
        assertEquals(3, driver.findElements(By.cssSelector(".pipeline-node.skipped")).size());
        assertTrue(driver.findElement(By.cssSelector(".simulation-status")).getText().contains("skipped"));
        assertTrue(driver.findElement(By.id("lab-panel")).getText().contains("not live execution"));
    }

    @Test
    void ansibleRecoveryIsClearlyASeparateIllustration() {
        driver.get(baseUrl() + "/#devops");
        new Select(driver.findElement(By.id("scenario"))).selectByValue("recovery");
        for (int i = 0; i < 5; i++) driver.findElement(By.cssSelector("[data-lab=next]")).click();
        assertEquals(5, driver.findElements(By.cssSelector(".pipeline-node")).size());
        assertTrue(driver.findElement(By.id("lab-panel")).getText().contains("not invoked by Jenkins"));
        assertTrue(driver.findElement(By.cssSelector(".simulation-status")).getText().contains("still reports failure"));
    }

    @Test
    void allReportsCanBeFoundAndFiltered() {
        driver.get(baseUrl() + "/#tasks");
        assertEquals(15, driver.findElements(By.cssSelector(".task-card")).size());
        driver.findElement(By.id("task-search")).sendKeys("Docker");
        int matches = driver.findElements(By.cssSelector(".task-card")).size();
        assertTrue(matches > 0 && matches < 15);
        driver.findElement(By.id("task-search")).clear();
        new Select(driver.findElement(By.id("task-group"))).selectByVisibleText("Infrastructure");
        assertEquals(2, driver.findElements(By.cssSelector(".task-card")).size());
        assertTrue(driver.findElement(By.cssSelector(".task-card a")).getAttribute("href").endsWith("/reports/task-13.docx"));
    }

    @Test
    void devopsTabsSupportKeyboardNavigationAndRecordedEvidence() {
        driver.get(baseUrl() + "/#devops");
        driver.findElement(By.id("tab-pipeline")).sendKeys(Keys.ARROW_RIGHT);
        assertEquals("true", driver.findElement(By.id("tab-architecture")).getAttribute("aria-selected"));
        assertTrue(driver.findElement(By.id("lab-panel")).getText().contains("Separate Ansible path"));
        driver.findElement(By.id("tab-quality")).click();
        assertTrue(driver.findElement(By.id("lab-panel")).getText().contains("17 / 17"));
        assertTrue(driver.findElement(By.id("lab-panel")).getText().contains("Not a current Jenkins"));
    }

    @Test
    void mobileNavigationDoesNotCreatePageOverflow() {
        driver.manage().window().setSize(new org.openqa.selenium.Dimension(390, 844));
        driver.get(baseUrl() + "/#overview");
        wait.until(d -> d.findElements(By.cssSelector(".stat-value")).size() == 4);
        assertFalse((Boolean) ((JavascriptExecutor) driver).executeScript(
                "return document.documentElement.scrollWidth > window.innerWidth"));
        driver.findElement(By.id("menu-toggle")).click();
        assertEquals("true", driver.findElement(By.id("menu-toggle")).getAttribute("aria-expanded"));
        wait.until(d -> ((Number) ((JavascriptExecutor) d).executeScript(
                "return document.querySelector('#sidebar').getBoundingClientRect().left")).doubleValue() >= -1);
        driver.findElement(By.cssSelector("a[data-route='devops']")).click();
        assertTrue(driver.findElement(By.id("lab-panel")).isDisplayed());
        assertEquals("false", driver.findElement(By.id("menu-toggle")).getAttribute("aria-expanded"));
    }

    @Test
    void skipLinkPreservesTheCurrentPageAndDraft() {
        driver.get(baseUrl() + "/#submit");
        driver.findElement(By.id("name")).sendKeys("Unsaved demo citizen");
        ((JavascriptExecutor) driver).executeScript("document.querySelector('.skip-link').focus()");
        driver.findElement(By.cssSelector(".skip-link")).sendKeys(Keys.ENTER);
        assertTrue(driver.getCurrentUrl().endsWith("#submit"));
        assertEquals("Unsaved demo citizen", driver.findElement(By.id("name")).getAttribute("value"));
        assertEquals("main", driver.switchTo().activeElement().getAttribute("id"));
    }

    @Test
    void latestTrackingLookupWinsWhenResponsesArriveOutOfOrder() {
        delaySeededTrackingResponse();
        trackToken("CMP-2026-1001");
        trackToken("CMP-2026-1002");
        wait.until(d -> d.findElement(By.id("track-res")).getText().contains("Sunita Kamble"));
        releaseDelayedResponse();
        assertTrue(driver.findElement(By.id("track-res")).getText().contains("Sunita Kamble"));
        assertFalse(driver.findElement(By.id("track-res")).getText().contains("Rahul Deshmukh"));
    }

    @Test
    void closedDialogCannotOverwriteANewerComplaintWithAnOldResponse() {
        driver.get(baseUrl() + "/#complaints");
        wait.until(d -> d.findElements(By.id("complaint-search")).size() == 1);
        delaySeededTrackingResponse();
        driver.findElement(By.id("complaint-search")).sendKeys("CMP-2026-1001");
        driver.findElement(By.cssSelector("button.token-link")).click();
        wait.until(d -> (Boolean) ((JavascriptExecutor) d).executeScript(
                "return document.querySelector('#complaint-dialog').open"));
        driver.switchTo().activeElement().sendKeys(Keys.ESCAPE);
        driver.findElement(By.id("complaint-search")).clear();
        driver.findElement(By.id("complaint-search")).sendKeys("CMP-2026-1002");
        driver.findElement(By.cssSelector("button.token-link")).click();
        wait.until(d -> d.findElement(By.id("complaint-dialog")).getText().contains("Sunita Kamble"));
        releaseDelayedResponse();
        assertTrue(driver.findElement(By.id("complaint-dialog")).getText().contains("Sunita Kamble"));
        assertFalse(driver.findElement(By.id("complaint-dialog")).getText().contains("Rahul Deshmukh"));
    }

    @Test
    void walkthroughPlayAndPauseRemainKeyboardAccessible() {
        driver.get(baseUrl() + "/#devops");
        driver.findElement(By.cssSelector("[data-lab=play]")).click();
        assertEquals("play", driver.switchTo().activeElement().getAttribute("data-lab"));
        assertTrue(driver.findElement(By.cssSelector("[data-lab=play]")).getText().contains("Pause"));
        driver.switchTo().activeElement().sendKeys(Keys.ENTER);
        assertTrue(driver.findElement(By.cssSelector("[data-lab=play]")).getText().contains("Play"));
    }

    private void trackToken(String token) {
        driver.findElement(By.id("track-token")).clear();
        driver.findElement(By.id("track-token")).sendKeys(token);
        driver.findElement(By.cssSelector("#track-form button")).click();
    }

    private void delaySeededTrackingResponse() {
        ((JavascriptExecutor) driver).executeScript("""
                window.delayedResponseConsumed = false;
                const gate = new Promise(resolve => { window.releaseDelayedResponse = resolve; });
                const originalFetch = window.fetch.bind(window);
                window.fetch = async (...args) => {
                    const response = await originalFetch(...args);
                    if (String(args[0]).includes('/api/complaints/track?token=CMP-2026-1001')) {
                        await gate;
                        const parse = response.json.bind(response);
                        response.json = async () => {
                            const value = await parse();
                            setTimeout(() => { window.delayedResponseConsumed = true; }, 0);
                            return value;
                        };
                    }
                    return response;
                };
                """);
    }

    private void releaseDelayedResponse() {
        ((JavascriptExecutor) driver).executeScript("window.releaseDelayedResponse()");
        wait.until(d -> Boolean.TRUE.equals(((JavascriptExecutor) d).executeScript(
                "return window.delayedResponseConsumed")));
    }

    private static int findFreePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    private static String baseUrl() {
        return "http://127.0.0.1:" + serverPort;
    }

    private void captureFailureScreenshot(String testName) {
        if (!(driver instanceof TakesScreenshot)) {
            return;
        }
        try {
            Path directory = Path.of("target", "selenium-failures");
            Files.createDirectories(directory);
            String safeName = testName.replaceAll("[^A-Za-z0-9._-]", "_");
            Path screenshot = directory.resolve(safeName + "-" + System.currentTimeMillis() + ".png");
            File srcFile = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            Files.copy(srcFile.toPath(), screenshot, StandardCopyOption.REPLACE_EXISTING);
            System.err.println("Failure screenshot saved to " + screenshot.toAbsolutePath());
        } catch (IOException | RuntimeException screenshotError) {
            System.err.println("Unable to capture failure screenshot: " + screenshotError.getMessage());
        }
    }
}
