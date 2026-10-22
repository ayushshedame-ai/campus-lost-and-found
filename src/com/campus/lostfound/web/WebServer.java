package com.campus.lostfound.web;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.campus.lostfound.exception.InvalidInputException;
import com.campus.lostfound.exception.ItemNotFoundException;
import com.campus.lostfound.exception.MatchException;
import com.campus.lostfound.exception.StorageException;
import com.campus.lostfound.model.FoundItem;
import com.campus.lostfound.model.Item;
import com.campus.lostfound.model.ItemCategory;
import com.campus.lostfound.model.ItemStatus;
import com.campus.lostfound.model.LostItem;
import com.campus.lostfound.model.MatchResult;
import com.campus.lostfound.model.Person;
import com.campus.lostfound.service.DemoData;
import com.campus.lostfound.service.LostFoundService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

/**
 * Built-in HTTP REST API Server and Static Web Content Handler.
 *
 * <p>Uses the standard Java {@link com.sun.net.httpserver.HttpServer}.
 * It provides a complete REST backend for the frontend web application without
 * requiring Spring Boot, Tomcat, or third-party Maven dependencies.</p>
 */
public class WebServer {

    private final int port;
    private final LostFoundService service;
    private final File webDir;
    private HttpServer server;

    public WebServer(int port, LostFoundService service, File webDir) {
        this.port = port;
        this.service = service;
        this.webDir = webDir;
    }

    /** Starts the HTTP server and registers all API & static file routes. */
    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);

        // API context routes
        server.createContext("/api/stats", new StatsHandler());
        server.createContext("/api/items", new ItemsHandler());
        server.createContext("/api/lost", new ReportLostHandler());
        server.createContext("/api/found", new ReportFoundHandler());
        server.createContext("/api/matches", new MatchesHandler());
        server.createContext("/api/matches/confirm", new ConfirmMatchHandler());
        server.createContext("/api/claim", new ClaimHandler());
        server.createContext("/api/demo", new DemoHandler());
        server.createContext("/api/export", new ExportHandler());

        // Static frontend files (HTML, CSS, JS)
        server.createContext("/", new StaticFileHandler());

        server.setExecutor(null); // default executor
        server.start();
        System.out.println("=====================================================");
        System.out.println("  CAMPUS LOST & FOUND - FULL STACK WEB SERVER");
        System.out.println("=====================================================");
        System.out.println("  Web UI running at : http://localhost:" + port);
        System.out.println("  REST API available at: http://localhost:" + port + "/api/items");
        System.out.println("  Frontend files served from: " + webDir.getAbsolutePath());
        System.out.println("=====================================================");
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    // =========================================================================
    // HTTP Helper Methods
    // =========================================================================

    /** Sends CORS headers to allow cross-origin requests from any browser. */
    private static void setCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");
    }

    /** Handles preflight OPTIONS requests. */
    private static boolean handleOptions(HttpExchange exchange) throws IOException {
        setCorsHeaders(exchange);
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return true;
        }
        return false;
    }

    /** Sends a JSON response with status code and UTF-8 encoding. */
    private static void sendJson(HttpExchange exchange, int statusCode, String json) throws IOException {
        setCorsHeaders(exchange);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    /** Reads the request body as a UTF-8 String. */
    private static String readBody(HttpExchange exchange) throws IOException {
        InputStream is = exchange.getRequestBody();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int len;
        while ((len = is.read(buffer)) != -1) {
            baos.write(buffer, 0, len);
        }
        return baos.toString(StandardCharsets.UTF_8.name());
    }

    /** Parses URL query parameters (e.g. "?q=wallet&type=lost"). */
    private static Map<String, String> parseQueryParams(String query) {
        Map<String, String> params = new HashMap<String, String>();
        if (query == null || query.trim().isEmpty()) {
            return params;
        }
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf('=');
            if (idx > 0) {
                try {
                    String key = URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8.name());
                    String val = URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8.name());
                    params.put(key, val);
                } catch (Exception ignored) {
                }
            }
        }
        return params;
    }

    // =========================================================================
    // REST API Handlers
    // =========================================================================

    /** GET /api/stats -> Returns summary counts */
    private class StatsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handleOptions(exchange)) return;
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJson(exchange, 405, SimpleJson.errorJson("Method Not Allowed"));
                return;
            }

            List<LostItem> lost = service.getAllLostItems();
            List<FoundItem> found = service.getAllFoundItems();

            int openLost = 0, matchedLost = 0, claimedLost = 0;
            for (LostItem item : lost) {
                if (item.getStatus() == ItemStatus.OPEN) openLost++;
                else if (item.getStatus() == ItemStatus.MATCHED) matchedLost++;
                else if (item.getStatus() == ItemStatus.CLAIMED) claimedLost++;
            }

            int openFound = 0, matchedFound = 0, claimedFound = 0;
            for (FoundItem item : found) {
                if (item.getStatus() == ItemStatus.OPEN) openFound++;
                else if (item.getStatus() == ItemStatus.MATCHED) matchedFound++;
                else if (item.getStatus() == ItemStatus.CLAIMED) claimedFound++;
            }

            String json = "{"
                    + "\"totalLost\":" + lost.size() + ","
                    + "\"openLost\":" + openLost + ","
                    + "\"matchedLost\":" + matchedLost + ","
                    + "\"claimedLost\":" + claimedLost + ","
                    + "\"totalFound\":" + found.size() + ","
                    + "\"openFound\":" + openFound + ","
                    + "\"matchedFound\":" + matchedFound + ","
                    + "\"claimedFound\":" + claimedFound + ","
                    + "\"totalItems\":" + (lost.size() + found.size())
                    + "}";

            sendJson(exchange, 200, json);
        }
    }

    /** GET /api/items -> List all items, with optional query filters (?type=..., ?status=..., ?category=..., ?q=...) */
    private class ItemsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handleOptions(exchange)) return;
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJson(exchange, 405, SimpleJson.errorJson("Method Not Allowed"));
                return;
            }

            String path = exchange.getRequestURI().getPath();
            // Check if asking for specific ID: /api/items/L001
            if (path.length() > "/api/items/".length()) {
                String id = path.substring("/api/items/".length());
                try {
                    Item item = service.getItemById(id);
                    sendJson(exchange, 200, SimpleJson.toJson(item));
                    return;
                } catch (ItemNotFoundException e) {
                    sendJson(exchange, 404, SimpleJson.errorJson(e.getMessage()));
                    return;
                }
            }

            Map<String, String> query = parseQueryParams(exchange.getRequestURI().getRawQuery());
            String type = query.get("type");
            String status = query.get("status");
            String category = query.get("category");
            String q = query.get("q");

            List<Item> all;
            if (q != null && !q.trim().isEmpty()) {
                all = service.search(q);
            } else {
                all = new ArrayList<Item>();
                if (type == null || "ALL".equalsIgnoreCase(type) || "LOST".equalsIgnoreCase(type)) {
                    all.addAll(service.getAllLostItems());
                }
                if (type == null || "ALL".equalsIgnoreCase(type) || "FOUND".equalsIgnoreCase(type)) {
                    all.addAll(service.getAllFoundItems());
                }
            }

            // Apply filters
            List<Item> filtered = new ArrayList<Item>();
            for (Item item : all) {
                if (type != null && !"ALL".equalsIgnoreCase(type) && !item.getType().equalsIgnoreCase(type)) {
                    continue;
                }
                if (status != null && !"ALL".equalsIgnoreCase(status) && !item.getStatus().name().equalsIgnoreCase(status)) {
                    continue;
                }
                if (category != null && !"ALL".equalsIgnoreCase(category) && !item.getCategory().name().equalsIgnoreCase(category)) {
                    continue;
                }
                filtered.add(item);
            }

            sendJson(exchange, 200, SimpleJson.itemsToJson(filtered));
        }
    }

    /** POST /api/lost -> Report a Lost Item */
    private class ReportLostHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handleOptions(exchange)) return;
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJson(exchange, 405, SimpleJson.errorJson("Method Not Allowed"));
                return;
            }

            try {
                String body = readBody(exchange);
                Map<String, String> map = SimpleJson.parseObject(body);

                String name = map.get("name");
                String description = map.get("description");
                String location = map.get("location");
                String dateStr = map.get("dateLost");
                String catStr = map.get("category");

                String pName = map.get("personName");
                String studentId = map.get("studentId");
                String phone = map.get("phone");
                String email = map.get("email");

                ItemCategory category = ItemCategory.fromInput(catStr);
                LocalDate date = (dateStr != null && !dateStr.isEmpty()) ? LocalDate.parse(dateStr) : LocalDate.now();
                Person owner = new Person(pName, studentId, phone, email);

                LostItem item = service.reportLost(name, description, category, location, date, owner);
                sendJson(exchange, 201, SimpleJson.toJson(item));
            } catch (InvalidInputException e) {
                sendJson(exchange, 400, SimpleJson.errorJson(e.getMessage()));
            } catch (StorageException e) {
                sendJson(exchange, 500, SimpleJson.errorJson("Storage error: " + e.getMessage()));
            } catch (Exception e) {
                sendJson(exchange, 400, SimpleJson.errorJson("Invalid request: " + e.getMessage()));
            }
        }
    }

    /** POST /api/found -> Report a Found Item */
    private class ReportFoundHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handleOptions(exchange)) return;
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJson(exchange, 405, SimpleJson.errorJson("Method Not Allowed"));
                return;
            }

            try {
                String body = readBody(exchange);
                Map<String, String> map = SimpleJson.parseObject(body);

                String name = map.get("name");
                String description = map.get("description");
                String location = map.get("location");
                String dateStr = map.get("dateFound");
                String catStr = map.get("category");
                String storedAt = map.get("storedAt");

                String pName = map.get("personName");
                String studentId = map.get("studentId");
                String phone = map.get("phone");
                String email = map.get("email");

                ItemCategory category = ItemCategory.fromInput(catStr);
                LocalDate date = (dateStr != null && !dateStr.isEmpty()) ? LocalDate.parse(dateStr) : LocalDate.now();
                Person finder = new Person(pName, studentId, phone, email);

                FoundItem item = service.reportFound(name, description, category, location, date, finder, storedAt);
                sendJson(exchange, 201, SimpleJson.toJson(item));
            } catch (InvalidInputException e) {
                sendJson(exchange, 400, SimpleJson.errorJson(e.getMessage()));
            } catch (StorageException e) {
                sendJson(exchange, 500, SimpleJson.errorJson("Storage error: " + e.getMessage()));
            } catch (Exception e) {
                sendJson(exchange, 400, SimpleJson.errorJson("Invalid request: " + e.getMessage()));
            }
        }
    }

    /** GET /api/matches?id=L001 -> Get match suggestions */
    private class MatchesHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handleOptions(exchange)) return;
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJson(exchange, 405, SimpleJson.errorJson("Method Not Allowed"));
                return;
            }

            Map<String, String> query = parseQueryParams(exchange.getRequestURI().getRawQuery());
            String id = query.get("id");
            if (id == null || id.trim().isEmpty()) {
                sendJson(exchange, 400, SimpleJson.errorJson("Item ID ('id') query parameter is required."));
                return;
            }

            try {
                Item item = service.getItemById(id);
                List<MatchResult> matches;
                if (item instanceof LostItem) {
                    matches = service.findMatchesForLost(id);
                } else {
                    matches = service.findMatchesForFound(id);
                }
                sendJson(exchange, 200, SimpleJson.matchesToJson(matches));
            } catch (ItemNotFoundException e) {
                sendJson(exchange, 404, SimpleJson.errorJson(e.getMessage()));
            } catch (Exception e) {
                sendJson(exchange, 500, SimpleJson.errorJson("Error calculating matches: " + e.getMessage()));
            }
        }
    }

    /** POST /api/matches/confirm -> Confirm match between lost and found item */
    private class ConfirmMatchHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handleOptions(exchange)) return;
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJson(exchange, 405, SimpleJson.errorJson("Method Not Allowed"));
                return;
            }

            try {
                String body = readBody(exchange);
                Map<String, String> map = SimpleJson.parseObject(body);
                String lostId = map.get("lostId");
                String foundId = map.get("foundId");

                if (lostId == null || foundId == null) {
                    sendJson(exchange, 400, SimpleJson.errorJson("Both 'lostId' and 'foundId' are required."));
                    return;
                }

                service.confirmMatch(lostId, foundId);
                sendJson(exchange, 200, SimpleJson.successJson("Match confirmed between " + lostId + " and " + foundId + "."));
            } catch (ItemNotFoundException | MatchException e) {
                sendJson(exchange, 400, SimpleJson.errorJson(e.getMessage()));
            } catch (StorageException e) {
                sendJson(exchange, 500, SimpleJson.errorJson("Storage error: " + e.getMessage()));
            }
        }
    }

    /** POST /api/claim -> Mark an item as claimed with Student ID verification */
    private class ClaimHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handleOptions(exchange)) return;
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJson(exchange, 405, SimpleJson.errorJson("Method Not Allowed"));
                return;
            }

            try {
                String body = readBody(exchange);
                Map<String, String> map = SimpleJson.parseObject(body);
                String lostId = map.get("lostId");
                String studentId = map.get("studentId");

                if (lostId == null || studentId == null) {
                    sendJson(exchange, 400, SimpleJson.errorJson("'lostId' and 'studentId' are required."));
                    return;
                }

                service.markClaimed(lostId, studentId);
                sendJson(exchange, 200, SimpleJson.successJson("Item " + lostId + " successfully claimed and returned to student!"));
            } catch (ItemNotFoundException | MatchException e) {
                sendJson(exchange, 400, SimpleJson.errorJson(e.getMessage()));
            } catch (StorageException e) {
                sendJson(exchange, 500, SimpleJson.errorJson("Storage error: " + e.getMessage()));
            }
        }
    }

    /** POST /api/demo -> Load sample demo data */
    private class DemoHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handleOptions(exchange)) return;
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJson(exchange, 405, SimpleJson.errorJson("Method Not Allowed"));
                return;
            }

            try {
                DemoData.load(service);
                sendJson(exchange, 200, SimpleJson.successJson("Sample demo data loaded successfully!"));
            } catch (Exception e) {
                sendJson(exchange, 500, SimpleJson.errorJson("Could not load demo data: " + e.getMessage()));
            }
        }
    }

    /** GET /api/export -> Trigger report export */
    private class ExportHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handleOptions(exchange)) return;
            try {
                String reportFile = "data" + File.separator + "report.txt";
                service.exportReport(reportFile);
                sendJson(exchange, 200, SimpleJson.successJson("Report exported to " + new File(reportFile).getAbsolutePath()));
            } catch (StorageException e) {
                sendJson(exchange, 500, SimpleJson.errorJson("Export failed: " + e.getMessage()));
            }
        }
    }

    // =========================================================================
    // Static Frontend File Handler
    // =========================================================================

    private class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            setCorsHeaders(exchange);
            String path = exchange.getRequestURI().getPath();
            if (path == null || path.equals("/") || path.isEmpty()) {
                path = "/index.html";
            }

            // Prevent path traversal
            if (path.contains("..")) {
                send404(exchange);
                return;
            }

            File file = new File(webDir, path.replace('/', File.separatorChar));
            if (!file.exists() || file.isDirectory()) {
                send404(exchange);
                return;
            }

            String mime = getMimeType(file.getName());
            exchange.getResponseHeaders().set("Content-Type", mime);

            long length = file.length();
            exchange.sendResponseHeaders(200, length);
            try (InputStream is = new FileInputStream(file);
                 OutputStream os = exchange.getResponseBody()) {
                byte[] buffer = new byte[4096];
                int read;
                while ((read = is.read(buffer)) != -1) {
                    os.write(buffer, 0, read);
                }
            }
        }

        private void send404(HttpExchange exchange) throws IOException {
            String msg = "404 Not Found";
            byte[] bytes = msg.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(404, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }

        private String getMimeType(String filename) {
            String lower = filename.toLowerCase();
            if (lower.endsWith(".html") || lower.endsWith(".htm")) return "text/html; charset=UTF-8";
            if (lower.endsWith(".css")) return "text/css; charset=UTF-8";
            if (lower.endsWith(".js")) return "application/javascript; charset=UTF-8";
            if (lower.endsWith(".json")) return "application/json; charset=UTF-8";
            if (lower.endsWith(".svg")) return "image/svg+xml";
            if (lower.endsWith(".png")) return "image/png";
            if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
            if (lower.endsWith(".ico")) return "image/x-icon";
            return "text/plain; charset=UTF-8";
        }
    }
}
