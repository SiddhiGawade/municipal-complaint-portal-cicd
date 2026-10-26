package com.municipal.portal;

import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.municipal.portal.model.Complaint;
import com.municipal.portal.model.ComplaintStatus;
import com.municipal.portal.service.ComplaintService;
import com.municipal.portal.service.DemoData;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

public class App {

    private static final int DEFAULT_PORT = 8085;
    private final ComplaintService service = new ComplaintService();
    private final boolean demo;

    private App(boolean demo) {
        this.demo = demo;
        DemoData.seed(service, demo);
    }

    private static int resolvePort(String[] args) {
        if (args != null && args.length > 0) {
            try {
                return Integer.parseInt(args[0]);
            } catch (NumberFormatException ignored) {}
        }
        String envPort = System.getenv("PORT");
        if (envPort != null && !envPort.isBlank()) {
            try {
                return Integer.parseInt(envPort.trim());
            } catch (NumberFormatException ignored) {}
        }
        String propPort = System.getProperty("PORT");
        if (propPort != null && !propPort.isBlank()) {
            try {
                return Integer.parseInt(propPort.trim());
            } catch (NumberFormatException ignored) {}
        }
        return DEFAULT_PORT;
    }

    public static void main(String[] args) throws IOException {
        int port = resolvePort(args);
        boolean demo = Arrays.asList(args).contains("--demo") || Boolean.parseBoolean(System.getenv("DEMO_MODE"));
        String host = System.getenv().getOrDefault("BIND_ADDRESS", "127.0.0.1");

        // Seed initial sample complaints for reviewer workflow demonstration
        HttpServer server = createServer(new InetSocketAddress(host, port), demo);
        server.start();
        System.out.println("=================================================");
        System.out.println(" Municipal Complaint Portal - CivicFlow");
        System.out.println(" Server running at: http://localhost:" + port);
        System.out.println(" Health Check:      http://localhost:" + port + "/api/health");
        System.out.println(" Demo mode:         " + demo + " (in-memory data)");
        System.out.println("=================================================");
    }

    public static HttpServer createServer(InetSocketAddress address, boolean demo) throws IOException {
        if (demo && !address.getAddress().isLoopbackAddress()) {
            throw new IllegalArgumentException("Demo reviewer mode requires a loopback bind address");
        }
        App app = new App(demo);
        HttpServer server = HttpServer.create(address, 0);
        // API Endpoints
        server.createContext("/", app::handle);
        server.setExecutor(null); // default executor
        return server;
    }

    private void handle(HttpExchange exchange) throws IOException {
        try {
            String path = exchange.getRequestURI().getPath();
            String method = exchange.getRequestMethod();
            if (exchange.getLocalAddress().getAddress().isLoopbackAddress()) {
                String host = URI.create("http://" + exchange.getRequestHeaders().getFirst("Host")).getHost();
                if (!Set.of("localhost", "127.0.0.1", "[::1]").contains(host == null ? "" : host.toLowerCase(Locale.ROOT))) {
                    json(exchange, 403, Map.of("error", "Unrecognized local host"));
                    return;
                }
            }
            if ("POST".equals(method) && Set.of("/api/complaints", "/api/complaints/status").contains(path)) {
                if (!sameOrigin(exchange)) {
                    json(exchange, 403, Map.of("error", "Cross-origin writes are not allowed"));
                    return;
                }
                if (path.endsWith("/status") && !demo) {
                    json(exchange, 403, Map.of("error", "Reviewer actions require local --demo mode; production authentication is not configured"));
                    return;
                }
                String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
                if (contentType != null && !contentType.startsWith("application/x-www-form-urlencoded")) {
                    json(exchange, 415, Map.of("error", "Use form-encoded input"));
                    return;
                }
                byte[] body = exchange.getRequestBody().readNBytes(16385);
                if (body.length > 16384) {
                    json(exchange, 413, Map.of("error", "Request is too large"));
                    return;
                }
                // Simple query-param or form parsing
                Map<String, String> form = parameters(new String(body, StandardCharsets.UTF_8));
                if (path.endsWith("/status")) {
                    String token = form.getOrDefault("token", "");
                    ComplaintStatus status = ComplaintStatus.valueOf(form.getOrDefault("status", "").toUpperCase(Locale.ROOT));
                    if (!service.updateStatus(token, status, form.get("remarks"), "DEMO_REVIEWER")) {
                        json(exchange, 404, Map.of("error", "Complaint Token Not Found"));
                        return;
                    }
                    json(exchange, 200, Map.of("status", "UPDATED", "token", token.trim().toUpperCase(Locale.ROOT)));
                } else {
                    if (service.getTotalCount() >= 1000) {
                        json(exchange, 429, Map.of("error", "Demo capacity reached; restart the local demo to clear in-memory records"));
                        return;
                    }
                    Complaint created = service.submitComplaint(form.get("citizenName"), form.get("citizenContact"),
                            form.get("category"), form.get("ward"), form.get("description"), null);
                    json(exchange, 201, Map.of("token", created.getToken(), "status", "SUCCESS", "message", "Complaint Registered Successfully"));
                }
                return;
            }
            if (!"GET".equals(method) && !"HEAD".equals(method)) {
                exchange.getResponseHeaders().set("Allow", Set.of("/api/complaints", "/api/complaints/status").contains(path) ? "GET, POST" : "GET, HEAD");
                json(exchange, 405, Map.of("error", "Method Not Allowed"));
                return;
            }
            switch (path) {
                case "/api/complaints/status" -> {
                    exchange.getResponseHeaders().set("Allow", "POST");
                    json(exchange, 405, Map.of("error", "Method Not Allowed"));
                }
                case "/api/health" -> json(exchange, 200, Map.of("status", "UP", "service", "Municipal Complaint Portal",
                        "version", "1.0.0-MVP", "demoMode", demo, "storage", "IN_MEMORY", "checkedAt", LocalDateTime.now().toString()));
                case "/api/complaints" -> json(exchange, 200, complaints().stream().map(this::complaintJson).toList());
                case "/api/complaints/track" -> {
                    Complaint found = service.getComplaintByToken(parameters(exchange.getRequestURI().getRawQuery()).get("token")).orElse(null);
                    if (found == null) json(exchange, 404, Map.of("error", "Complaint Token Not Found"));
                    else json(exchange, 200, complaintJson(found));
                }
                case "/api/dashboard" -> {
                    List<Complaint> records = complaints();
                    Map<String, Long> counts = new LinkedHashMap<>();
                    for (ComplaintStatus status : ComplaintStatus.values()) {
                        counts.put(status.name(), records.stream().filter(c -> c.getStatus() == status).count());
                    }
                    json(exchange, 200, Map.of("total", records.size(), "statuses", counts, "demoMode", demo,
                            "categories", records.stream().collect(Collectors.groupingBy(Complaint::getCategory, Collectors.counting())),
                            "wards", records.stream().collect(Collectors.groupingBy(Complaint::getWard, Collectors.counting()))));
                }
                default -> resource(exchange, path);
            }
        } catch (IllegalArgumentException exception) {
            json(exchange, 400, Map.of("error", exception.getMessage() == null ? "Invalid input" : exception.getMessage()));
        } catch (Exception exception) {
            json(exchange, 500, Map.of("error", "The request could not be completed"));
        }
    }

    private List<Complaint> complaints() {
        return service.getAllComplaints().stream().sorted(Comparator.comparing(Complaint::getUpdatedAt).reversed()).toList();
    }

    private Map<String, Object> complaintJson(Complaint complaint) {
        synchronized (service) {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("token", complaint.getToken());
            result.put("citizen", complaint.getCitizenName());
            result.put("category", complaint.getCategory());
            result.put("ward", complaint.getWard());
            result.put("status", complaint.getStatus());
            result.put("remarks", complaint.getReviewerRemarks() == null ? "" : complaint.getReviewerRemarks());
            result.put("desc", complaint.getDescription());
            result.put("createdAt", complaint.getCreatedAt().toString());
            result.put("updatedAt", complaint.getUpdatedAt().toString());
            result.put("allowedStatuses", ComplaintService.allowedTransitions(complaint.getStatus()).stream().sorted().toList());
            result.put("history", complaint.getHistory().stream().map(event -> Map.of("status", event.status(),
                    "remarks", event.remarks(), "actor", event.actor(), "at", event.at().toString())).toList());
            return result;
        }
    }

    private void resource(HttpExchange exchange, String path) throws IOException {
        String resource;
        String type;
        if (path.matches("/reports/task-([1-9]|1[0-5])\\.docx")) {
            resource = path.substring(1);
            type = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            exchange.getResponseHeaders().set("Content-Disposition", "attachment; filename=\"" + path.substring(9) + "\"");
        } else if (Set.of("/", "/index.html", "/styles.css", "/app.js", "/devops.js", "/favicon.svg",
                "/images/Task15_architecture.png", "/images/Task15_release_workflow.png",
                "/images/jenkins_quality_gate_comparison_task10.png").contains(path)) {
            resource = "web/" + ("/".equals(path) ? "index.html" : path.substring(1));
            type = path.endsWith(".css") ? "text/css" : path.endsWith(".js") ? "text/javascript"
                    : path.endsWith(".svg") ? "image/svg+xml" : path.endsWith(".png") ? "image/png" : "text/html";
        } else {
            json(exchange, 404, Map.of("error", "Resource Not Found"));
            return;
        }
        try (InputStream input = App.class.getResourceAsStream("/" + resource)) {
            if (input == null) json(exchange, 404, Map.of("error", "Resource Not Found"));
            else sendResponse(exchange, 200, type, input.readAllBytes());
        }
    }

    private static boolean sameOrigin(HttpExchange exchange) {
        if ("cross-site".equals(exchange.getRequestHeaders().getFirst("Sec-Fetch-Site"))) return false;
        String origin = exchange.getRequestHeaders().getFirst("Origin");
        if (origin == null) return true;
        URI uri = URI.create(origin);
        return ("http".equals(uri.getScheme()) || "https".equals(uri.getScheme())) && uri.getRawAuthority() != null
                && uri.getRawAuthority().equalsIgnoreCase(exchange.getRequestHeaders().getFirst("Host"));
    }

    private static void json(HttpExchange exchange, int code, Object value) throws IOException {
        sendResponse(exchange, code, "application/json", Json.encode(value).getBytes(StandardCharsets.UTF_8));
    }

    private static void sendResponse(HttpExchange exchange, int code, String contentType, byte[] bytes) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", contentType + (contentType.startsWith("text/") || contentType.equals("application/json") ? "; charset=utf-8" : ""));
        exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        exchange.getResponseHeaders().set("Referrer-Policy", "no-referrer");
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.getResponseHeaders().set("Content-Security-Policy", "default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self'; connect-src 'self'; object-src 'none'; base-uri 'none'; frame-ancestors 'none'; form-action 'self'");
        if ("HEAD".equals(exchange.getRequestMethod())) {
            exchange.getResponseHeaders().set("Content-Length", Integer.toString(bytes.length));
            exchange.sendResponseHeaders(code, -1);
            exchange.close();
            return;
        }
        exchange.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static Map<String, String> parameters(String body) {
        Map<String, String> result = new LinkedHashMap<>();
        if (body == null || body.isBlank()) return result;
        for (String pair : body.split("&")) {
            String[] parts = pair.split("=", 2);
            result.put(URLDecoder.decode(parts[0], StandardCharsets.UTF_8),
                    parts.length == 2 ? URLDecoder.decode(parts[1], StandardCharsets.UTF_8) : "");
        }
        return result;
    }
}
