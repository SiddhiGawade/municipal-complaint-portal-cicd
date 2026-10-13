package com.municipal.portal;

import com.municipal.portal.model.Complaint;
import com.municipal.portal.model.ComplaintStatus;
import com.municipal.portal.service.ComplaintService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

public class App {

    private static final int PORT = 8080;
    private static final ComplaintService service = new ComplaintService();

    public static void main(String[] args) throws IOException {
        // Seed initial sample complaints for reviewer workflow demonstration
        service.submitComplaint("Rahul Deshmukh", "9820011223", "Roads & Potholes", "Ward 07",
                "Deep pothole near SV Road junction causing hazard to two-wheelers.", "road_pothole.jpg");
        service.submitComplaint("Sunita Kamble", "9833445566", "Solid Waste Management", "Ward 12",
                "Uncollected municipal garbage bins overflowing for 3 days.", null);

        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);

        // API Endpoints
        server.createContext("/api/health", new HealthHandler());
        server.createContext("/api/complaints", new ComplaintHandler());
        server.createContext("/api/complaints/track", new TrackHandler());
        server.createContext("/api/complaints/status", new StatusUpdateHandler());
        server.createContext("/", new StaticUIHandler());

        server.setExecutor(null); // default executor
        System.out.println("=================================================");
        System.out.println(" Municipal Complaint Portal - Working Local Setup");
        System.out.println(" Server running at: http://localhost:" + PORT);
        System.out.println(" Health Check:      http://localhost:" + PORT + "/api/health");
        System.out.println("=================================================");
        server.start();
    }

    static class HealthHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String response = "{\"status\":\"UP\",\"service\":\"Municipal Complaint Portal\",\"version\":\"1.0.0-MVP\"}";
            sendResponse(exchange, 200, "application/json", response);
        }
    }

    static class ComplaintHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();
            if ("GET".equalsIgnoreCase(method)) {
                List<Complaint> list = service.getAllComplaints();
                StringBuilder json = new StringBuilder("[");
                for (int i = 0; i < list.size(); i++) {
                    Complaint c = list.get(i);
                    json.append(String.format("{\"token\":\"%s\",\"citizen\":\"%s\",\"category\":\"%s\",\"ward\":\"%s\",\"status\":\"%s\",\"remarks\":\"%s\"}",
                            c.getToken(), c.getCitizenName(), c.getCategory(), c.getWard(), c.getStatus(), c.getReviewerRemarks() != null ? c.getReviewerRemarks() : ""));
                    if (i < list.size() - 1) json.append(",");
                }
                json.append("]");
                sendResponse(exchange, 200, "application/json", json.toString());
            } else if ("POST".equalsIgnoreCase(method)) {
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                // Simple query-param or form parsing
                String name = getParam(body, "citizenName");
                String contact = getParam(body, "citizenContact");
                String category = getParam(body, "category");
                String ward = getParam(body, "ward");
                String desc = getParam(body, "description");

                try {
                    Complaint created = service.submitComplaint(name, contact, category, ward, desc, null);
                    String resp = String.format("{\"token\":\"%s\",\"status\":\"SUCCESS\",\"message\":\"Complaint Registered Successfully\"}", created.getToken());
                    sendResponse(exchange, 201, "application/json", resp);
                } catch (Exception e) {
                    sendResponse(exchange, 400, "application/json", "{\"error\":\"" + e.getMessage() + "\"}");
                }
            } else {
                sendResponse(exchange, 405, "text/plain", "Method Not Allowed");
            }
        }
    }

    static class TrackHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String query = exchange.getRequestURI().getQuery();
            String token = null;
            if (query != null && query.contains("token=")) {
                token = query.split("token=")[1].split("&")[0];
            }
            if (token != null) {
                Optional<Complaint> found = service.getComplaintByToken(token);
                if (found.isPresent()) {
                    Complaint c = found.get();
                    String resp = String.format("{\"token\":\"%s\",\"citizen\":\"%s\",\"category\":\"%s\",\"ward\":\"%s\",\"status\":\"%s\",\"remarks\":\"%s\",\"desc\":\"%s\"}",
                            c.getToken(), c.getCitizenName(), c.getCategory(), c.getWard(), c.getStatus(),
                            c.getReviewerRemarks() != null ? c.getReviewerRemarks() : "", c.getDescription());
                    sendResponse(exchange, 200, "application/json", resp);
                    return;
                }
            }
            sendResponse(exchange, 404, "application/json", "{\"error\":\"Complaint Token Not Found\"}");
        }
    }

    static class StatusUpdateHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                String token = getParam(body, "token");
                String statusStr = getParam(body, "status");
                String remarks = getParam(body, "remarks");
                try {
                    ComplaintStatus status = ComplaintStatus.valueOf(statusStr.toUpperCase());
                    boolean ok = service.updateStatus(token, status, remarks, "OFFICER_ADMIN");
                    if (ok) {
                        sendResponse(exchange, 200, "application/json", "{\"status\":\"UPDATED\",\"token\":\"" + token + "\"}");
                        return;
                    }
                } catch (Exception e) {
                    sendResponse(exchange, 400, "application/json", "{\"error\":\"" + e.getMessage() + "\"}");
                    return;
                }
            }
            sendResponse(exchange, 400, "application/json", "{\"error\":\"Update failed\"}");
        }
    }

    static class StaticUIHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String html = "<!DOCTYPE html><html lang='en'><head><meta charset='UTF-8'><title>Municipal Complaint Portal</title>"
                    + "<style>body{font-family:'Segoe UI',sans-serif;margin:0;padding:20px;background:#f0f4f8;color:#333;}"
                    + ".header{background:#1F497D;color:white;padding:20px;border-radius:8px;margin-bottom:20px;text-align:center;}"
                    + ".card{background:white;padding:20px;border-radius:8px;box-shadow:0 2px 8px rgba(0,0,0,0.1);margin-bottom:20px;}"
                    + "h2{color:#1F497D;margin-top:0;}button{background:#1F497D;color:white;border:none;padding:10px 18px;border-radius:4px;cursor:pointer;font-weight:bold;}"
                    + "input,select,textarea{width:100%;padding:8px;margin:8px 0 16px 0;border:1px solid #ccc;border-radius:4px;box-sizing:border-box;}"
                    + ".badge{padding:4px 8px;border-radius:4px;font-weight:bold;font-size:12px;background:#E2E8F0;}</style></head><body>"
                    + "<div class='header'><h1>🏛️ Municipal Complaint Portal</h1><p>Civic Grievance Redressal • DevOps CI/CD Showcase</p></div>"
                    + "<div class='card'><h2>Submit Civic Complaint</h2>"
                    + "<label>Citizen Name</label><input id='name' placeholder='e.g. Ramesh Kumar' value='Ramesh Kumar'>"
                    + "<label>Contact (Phone/Email)</label><input id='contact' placeholder='10-digit Mobile' value='9876543210'>"
                    + "<label>Category</label><select id='category'><option>Roads & Potholes</option><option>Solid Waste Management</option><option>Water Supply & Leakage</option><option>Streetlights & Electricity</option></select>"
                    + "<label>Ward Number</label><input id='ward' value='Ward 14'>"
                    + "<label>Description</label><textarea id='desc'>Deep pothole on SV road causing traffic congestion.</textarea>"
                    + "<button onclick='submitComplaint()'>Submit Complaint</button><p id='sub-res'></p></div>"
                    + "<div class='card'><h2>Track Complaint Status</h2>"
                    + "<input id='track-token' placeholder='Enter Token (e.g. CMP-2026-1001)' value='CMP-2026-1001'>"
                    + "<button onclick='trackComplaint()'>Track Status</button><div id='track-res' style='margin-top:12px;'></div></div>"
                    + "<script>"
                    + "function submitComplaint(){fetch('/api/complaints',{method:'POST',body:'citizenName='+encodeURIComponent(document.getElementById('name').value)+'&citizenContact='+encodeURIComponent(document.getElementById('contact').value)+'&category='+encodeURIComponent(document.getElementById('category').value)+'&ward='+encodeURIComponent(document.getElementById('ward').value)+'&description='+encodeURIComponent(document.getElementById('desc').value)}).then(r=>r.json()).then(d=>document.getElementById('sub-res').innerHTML='<b>Result:</b> '+JSON.stringify(d));}"
                    + "function trackComplaint(){var t=document.getElementById('track-token').value;fetch('/api/complaints/track?token='+t).then(r=>r.json()).then(d=>document.getElementById('track-res').innerHTML='<pre>'+JSON.stringify(d,null,2)+'</pre>');}"
                    + "</script></body></html>";
            sendResponse(exchange, 200, "text/html", html);
        }
    }

    private static void sendResponse(HttpExchange exchange, int code, String contentType, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static String getParam(String body, String name) {
        for (String pair : body.split("&")) {
            String[] kv = pair.split("=");
            if (kv.length == 2 && kv[0].equals(name)) {
                return java.net.URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
            }
        }
        return "";
    }
}
