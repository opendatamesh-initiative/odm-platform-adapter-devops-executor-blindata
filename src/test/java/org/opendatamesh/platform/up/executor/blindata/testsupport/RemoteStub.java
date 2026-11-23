package org.opendatamesh.platform.up.executor.blindata.testsupport;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Scriptable fake of every remote system the executor calls, served by the JDK HTTP server: ODM Registry v2, the
 * Blindata agent (runner, history, logs), an OAuth2 token endpoint and ODM DevOps v2 (task results).
 *
 * <p>Every request is recorded with its headers. Replies are scripted by method and path (query included when the
 * script has one); an unscripted request gets 404. {@link #reset()} installs the defaults: the orders data product with
 * versions v1.2.0 and v1.3.0, job 42 started by the runner, a token endpoint and a DevOps results endpoint.
 */
public final class RemoteStub implements AutoCloseable {

    public static final long SEQUENCE_ID = 42;
    public static final String TOKEN = "prd-access-token";

    public record Recorded(String method, String pathWithQuery, String body, Map<String, String> headers) {

        public String header(String name) {
            return headers.get(name.toLowerCase());
        }

        public JsonNode json() {
            try {
                return Fixtures.JSON.readTree(body);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    private record Reply(int status, String body) {
    }

    private final HttpServer server;
    private final Map<String, Reply> replies = new ConcurrentHashMap<>();
    private final List<Recorded> recorded = new CopyOnWriteArrayList<>();

    public RemoteStub() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", this::handle);
        server.start();
        reset();
    }

    public String baseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    public void reset() {
        replies.clear();
        recorded.clear();
        reply("GET", "/api/v2/pp/registry/products?fqn=" + encode(Fixtures.ORDERS_FQN), 200,
                page(Fixtures.JSON.createObjectNode().put("uuid", "dp-1").put("fqn", Fixtures.ORDERS_FQN)));
        version("v-120", "v1.2.0", "1.2.0", Fixtures.ordersDescriptor());
        version("v-130", "v1.3.0", "1.3.0", Fixtures.ordersDescriptorWithBlockingPattern());
        reply("POST", "/agent-api/v1/jobs/runner/async", 200,
                Fixtures.JSON.createObjectNode().put("sequenceId", SEQUENCE_ID).put("runStatus", "RUNNING").toString());
        reply("POST", "/oauth2/token", 200,
                Fixtures.JSON.createObjectNode().put("access_token", TOKEN).put("expires_in", 3600).toString());
        reply("POST", "/api/v2/pp/devops/activities/tasks/results", 200, "{}");
        reply("GET", "/agent-api/v1/jobs/history/" + SEQUENCE_ID + "/logs", 200,
                Fixtures.JSON.createObjectNode().put("sequenceId", SEQUENCE_ID).put("logContent", "agent: launching job\ragent: 5 checks executed").toString());
    }

    public void reply(String method, String pathWithQuery, int status, String body) {
        replies.put(method + " " + pathWithQuery, new Reply(status, body));
    }

    /**
     * Scripts the history of job 42 as the agent would return it: the job last started on the runner, with the given
     * run status and result rows (score per check code; a null score with a message makes an error row).
     */
    public void history(String runStatus, Map<String, Integer> scores, Map<String, String> errors) {
        JsonNode job = lastRequest("POST", "/agent-api/v1/jobs/runner/async").json();
        ObjectNode history = Fixtures.JSON.createObjectNode();
        history.put("sequenceId", SEQUENCE_ID).put("runStatus", runStatus)
                .put("startedAt", "2026-11-23T10:00:00.000+00:00").put("finishedAt", "2026-11-23T10:01:00.000+00:00");
        history.set("jobDetails", job);
        ObjectNode results = history.putObject("jobResults");
        ArrayNode rows = results.putArray("results");
        scores.forEach((code, score) -> rows.addObject().put("qualityCheckCode", code).put("metric", score > 0 ? 0 : 3)
                .put("totalElements", 100).put("score", score));
        ArrayNode errorRows = results.putArray("errors");
        errors.forEach((code, error) -> errorRows.addObject().put("qualityCheckCode", code).put("error", error));
        reply("GET", "/agent-api/v1/jobs/history/" + SEQUENCE_ID, 200, history.toString());
    }

    public List<Recorded> requests(String method, String path) {
        List<Recorded> matching = new ArrayList<>();
        for (Recorded request : recorded) {
            if (request.method().equals(method) && pathOf(request.pathWithQuery()).equals(path)) {
                matching.add(request);
            }
        }
        return matching;
    }

    public Recorded lastRequest(String method, String path) {
        List<Recorded> matching = requests(method, path);
        if (matching.isEmpty()) {
            throw new IllegalStateException("No " + method + " " + path + " received");
        }
        return matching.get(matching.size() - 1);
    }

    @Override
    public void close() {
        server.stop(0);
    }

    private void version(String uuid, String tag, String number, JsonNode descriptor) {
        reply("GET", "/api/v2/pp/registry/products-versions?dataProductUuid=dp-1&tag=" + encode(tag), 200,
                page(Fixtures.JSON.createObjectNode().put("uuid", uuid).put("tag", tag).put("versionNumber", number)));
        ObjectNode version = Fixtures.JSON.createObjectNode().put("uuid", uuid).put("tag", tag).put("versionNumber", number);
        version.set("content", descriptor);
        reply("GET", "/api/v2/pp/registry/products-versions/" + uuid, 200, version.toString());
    }

    private void handle(HttpExchange exchange) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        String pathWithQuery = exchange.getRequestURI().getRawPath()
                + (exchange.getRequestURI().getRawQuery() == null ? "" : "?" + exchange.getRequestURI().getRawQuery());
        Map<String, String> headers = new ConcurrentHashMap<>();
        exchange.getRequestHeaders().forEach((name, values) -> headers.put(name.toLowerCase(), String.join(",", values)));
        recorded.add(new Recorded(exchange.getRequestMethod(), pathWithQuery, body, headers));
        Reply reply = replies.getOrDefault(exchange.getRequestMethod() + " " + pathWithQuery,
                replies.getOrDefault(exchange.getRequestMethod() + " " + pathOf(pathWithQuery), new Reply(404, "{\"error\":\"not found\"}")));
        byte[] bytes = reply.body().getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(reply.status(), bytes.length == 0 ? -1 : bytes.length);
        if (bytes.length > 0) {
            exchange.getResponseBody().write(bytes);
        }
        exchange.close();
    }

    private static String page(ObjectNode item) {
        ObjectNode page = Fixtures.JSON.createObjectNode();
        page.putArray("content").add(item);
        return page.toString();
    }

    private static String pathOf(String pathWithQuery) {
        int query = pathWithQuery.indexOf('?');
        return query < 0 ? pathWithQuery : pathWithQuery.substring(0, query);
    }

    private static String encode(String value) {
        return java.net.URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
