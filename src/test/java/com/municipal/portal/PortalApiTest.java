package com.municipal.portal;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class PortalApiTest {
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
    private HttpServer server;
    private String base;

    @BeforeEach
    void startServer() throws Exception {
        server = App.createServer(new InetSocketAddress("127.0.0.1", 0), true);
        server.start();
        base = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    private HttpResponse<String> get(String path) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create(base + path)).timeout(Duration.ofSeconds(5)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> post(String path, String body) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create(base + path)).timeout(Duration.ofSeconds(5))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void dashboardSummarizesTheDeterministicDemoRecords() throws Exception {
        HttpResponse<String> response = get("/api/dashboard");
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("\"total\":20"));
        assertTrue(response.body().contains("\"PENDING\":5"));
        assertTrue(response.body().contains("\"RESOLVED\":3"));
        assertTrue(response.body().contains("\"demoMode\":true"));
    }

    @Test
    void statusHistoryRecordsOnlyActualChanges() throws Exception {
        assertEquals(200, post("/api/complaints/status",
                "token=CMP-2026-1001&status=UNDER_REVIEW&remarks=Inspection+started").statusCode());
        String tracked = get("/api/complaints/track?token=cmp-2026-1001").body();
        assertTrue(tracked.contains("\"status\":\"UNDER_REVIEW\""));
        assertTrue(tracked.contains("\"actor\":\"CITIZEN\""));
        assertTrue(tracked.contains("\"actor\":\"DEMO_REVIEWER\""));
        assertTrue(tracked.contains("\"remarks\":\"Inspection started\""));
        assertFalse(tracked.contains("\"actor\":\"FIELD_CREW\""));
    }

    @Test
    void invalidTransitionDoesNotMutateHistory() throws Exception {
        assertEquals(400, post("/api/complaints/status",
                "token=CMP-2026-1001&status=RESOLVED&remarks=Done").statusCode());
        String tracked = get("/api/complaints/track?token=CMP-2026-1001").body();
        assertTrue(tracked.contains("\"status\":\"PENDING\""));
        assertFalse(tracked.contains("\"status\":\"RESOLVED\""));
    }

    @Test
    void userQuotesBackslashesAndNewlinesRemainValidJsonStrings() throws Exception {
        HttpResponse<String> submitted = post("/api/complaints",
                "citizenName=Demo+%22Citizen%22&citizenContact=demo%40example.com&category=Roads+%26+Potholes&ward=Ward+07&description=Damaged+road+%22quoted%22+%5C+surface%0Aneeds+repair.");
        assertEquals(201, submitted.statusCode());
        String tracked = get("/api/complaints/track?token=CMP-2026-1021").body();
        assertTrue(tracked.contains("Demo \\\"Citizen\\\""));
        assertTrue(tracked.contains("\\\\"));
        assertTrue(tracked.contains("\\n"));
        assertFalse(tracked.contains("surface\n"));
    }

    @Test
    void rejectsCrossOriginWritesAndOpaqueOrigins() throws Exception {
        for (String origin : new String[]{"https://example.com", "null"}) {
            HttpResponse<String> response = client.send(HttpRequest.newBuilder(URI.create(base + "/api/complaints/status"))
                    .header("Origin", origin).POST(HttpRequest.BodyPublishers.ofString("token=CMP-2026-1001&status=APPROVED"))
                    .build(), HttpResponse.BodyHandlers.ofString());
            assertEquals(403, response.statusCode());
        }
    }

    @Test
    void boundsRequestSizesAndRejectsWrongContentTypes() throws Exception {
        assertEquals(413, post("/api/complaints", "x=" + "a".repeat(17000)).statusCode());
        HttpResponse<String> response = client.send(HttpRequest.newBuilder(URI.create(base + "/api/complaints"))
                .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString("{}")).build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(415, response.statusCode());
    }

    @Test
    void unknownTokensAndResourcesDoNotFallBackToTheHomepage() throws Exception {
        assertEquals(404, get("/api/complaints/track?token=CMP-2099-9999").statusCode());
        for (String path : new String[]{"/missing.js", "/%2e%2e/pom.xml", "/reports/task-16.docx", "/api/complaints/other"}) {
            assertEquals(404, get(path).statusCode(), path);
        }
    }

    @Test
    void healthIsExplicitlyLocalAndInMemory() throws Exception {
        HttpResponse<String> response = get("/api/health");
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("\"storage\":\"IN_MEMORY\""));
        assertTrue(response.body().contains("\"demoMode\":true"));
        assertTrue(response.headers().firstValue("Content-Type").orElse("").contains("charset=utf-8"));
    }

    @Test
    void demoModeCannotBindToAllNetworkInterfaces() {
        assertThrows(IllegalArgumentException.class, () ->
                App.createServer(new InetSocketAddress("0.0.0.0", 0), true));
    }

    @Test
    void reviewerWritesAreDisabledOutsideExplicitDemoMode() throws Exception {
        HttpServer normal = App.createServer(new InetSocketAddress("127.0.0.1", 0), false);
        normal.start();
        try {
            HttpResponse<String> response = client.send(HttpRequest.newBuilder(URI.create(
                    "http://127.0.0.1:" + normal.getAddress().getPort() + "/api/complaints/status"))
                    .POST(HttpRequest.BodyPublishers.ofString("token=CMP-2026-1001&status=APPROVED")).build(),
                    HttpResponse.BodyHandlers.ofString());
            assertEquals(403, response.statusCode());
        } finally {
            normal.stop(0);
        }
    }

    @Test
    void jsonEncoderHandlesControlCharactersAndSupplementaryUnicode() {
        assertEquals("\"a\\\"b\\\\c\\n\\t\\u0000\"", Json.encode("a\"b\\c\n\t\0"));
        assertEquals("\"\\ud83c\\udfdb\"", Json.encode("\uD83C\uDFDB"));
    }

    @Test
    void servesOnlyBundledAssetsWithSafeHeadersAndHeadSupport() throws Exception {
        for (String path : new String[]{"/", "/app.js", "/devops.js", "/styles.css", "/favicon.svg",
                "/images/Task15_architecture.png", "/images/Task15_release_workflow.png"}) {
            HttpResponse<String> response = get(path);
            assertEquals(200, response.statusCode(), path);
            assertEquals("nosniff", response.headers().firstValue("X-Content-Type-Options").orElse(""));
            assertTrue(response.headers().firstValue("Content-Security-Policy").orElse("").contains("script-src 'self'"));
        }
        HttpResponse<String> head = client.send(HttpRequest.newBuilder(URI.create(base + "/styles.css"))
                .method("HEAD", HttpRequest.BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(200, head.statusCode());
        assertEquals("", head.body());
    }

    @Test
    void allFifteenOriginalReportsArePackagedAndDownloadable() throws Exception {
        for (int task = 1; task <= 15; task++) {
            HttpResponse<byte[]> report = client.send(HttpRequest.newBuilder(
                    URI.create(base + "/reports/task-" + task + ".docx")).GET().build(), HttpResponse.BodyHandlers.ofByteArray());
            assertEquals(200, report.statusCode());
            assertEquals('P', report.body()[0]);
            assertEquals('K', report.body()[1]);
            assertTrue(report.headers().firstValue("Content-Disposition").orElse("").contains("attachment"));
        }
    }

    @Test
    void rejectsUnsupportedMethodsAndMalformedFormEncoding() throws Exception {
        assertEquals(405, get("/api/complaints/status").statusCode());
        HttpResponse<String> response = client.send(HttpRequest.newBuilder(URI.create(base + "/api/health"))
                .PUT(HttpRequest.BodyPublishers.ofString("unused")).build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(405, response.statusCode());
        assertEquals(400, post("/api/complaints", "citizenName=%ZZ").statusCode());
    }
}
