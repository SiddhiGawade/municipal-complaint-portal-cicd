package com.municipal.portal;

import static org.junit.jupiter.api.Assertions.assertTrue;

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
                "-cp", classes.toString(), App.class.getName(), Integer.toString(serverPort))
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
        driver.get(baseUrl());
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
            return text.contains("PENDING") ? text : null;
        });
        assertTrue(result.contains("Rahul Deshmukh"));
        assertTrue(result.contains("Roads & Potholes"));
    }

    @Test
    void citizenCanSubmitAndThenTrackComplaint() {
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

        driver.findElement(By.id("track-token")).clear();
        driver.findElement(By.id("track-token")).sendKeys(token);
        driver.findElement(By.xpath("//button[normalize-space()='Track Status']")).click();
        String tracked = wait.until(d -> {
            String text = d.findElement(By.id("track-res")).getText();
            return text.contains(token) ? text : null;
        });

        assertTrue(tracked.contains("Selenium Test Citizen"));
        assertTrue(tracked.contains("Water Supply & Leakage"));
        assertTrue(tracked.contains("PENDING"));
    }

    @Test
    void invalidComplaintSubmissionShowsValidationError() {
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

        driver.findElement(By.id("track-token")).clear();
        driver.findElement(By.id("track-token")).sendKeys(token);
        driver.findElement(By.xpath("//button[normalize-space()='Track Status']")).click();

        String tracked = wait.until(d -> {
            String text = d.findElement(By.id("track-res")).getText();
            return text.contains(token) ? text : null;
        });
        assertTrue(tracked.contains("Pooja Sharma"));
        assertTrue(tracked.contains("Solid Waste Management"));
        assertTrue(tracked.contains("PENDING"));
        assertTrue(tracked.contains("Garbage bins are overflowing"));
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
