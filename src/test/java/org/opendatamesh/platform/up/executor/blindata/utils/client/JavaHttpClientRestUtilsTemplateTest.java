package org.opendatamesh.platform.up.executor.blindata.utils.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.opendatamesh.platform.up.executor.blindata.exceptions.client.ClientException;
import org.opendatamesh.platform.up.executor.blindata.exceptions.client.ClientFailures;
import org.opendatamesh.platform.up.executor.blindata.exceptions.client.ClientTransportException;
import org.opendatamesh.platform.up.executor.blindata.utils.client.http.HttpEntity;
import org.opendatamesh.platform.up.executor.blindata.utils.client.http.HttpMethod;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A failed call is an HTTP error only when an answer came back; otherwise it is a transport failure, never "HTTP 500". */
class JavaHttpClientRestUtilsTemplateTest {

    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void whenNothingListensThenTransportFailureWordedAsNoConnection() throws IOException {
        int port;
        try (ServerSocket socket = new ServerSocket(0)) {
            port = socket.getLocalPort();
        }

        ClientException failure = assertThrows(ClientException.class, () -> post("http://localhost:" + port + "/run"));

        ClientTransportException transport = assertInstanceOf(ClientTransportException.class, failure);
        assertFalse(transport.isResponseTimeout());
        String described = ClientFailures.describe(failure, 300);
        assertTrue(described.startsWith(": no connection"), described);
        assertFalse(described.contains("HTTP 500"), described);
    }

    @Test
    void whenNoAnswerInTimeThenTransportFailureWordedAsTimeout() throws IOException {
        String url = serve(exchange -> {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        ClientException failure = assertThrows(ClientException.class,
                () -> template(Duration.ofMillis(300)).exchange(url, HttpMethod.POST, new HttpEntity<>("{}", List.of()), JsonNode.class, Map.of()));

        ClientTransportException transport = assertInstanceOf(ClientTransportException.class, failure);
        assertTrue(transport.isResponseTimeout());
        assertTrue(ClientFailures.describe(failure, 300).startsWith(": no answer in time"));
    }

    @Test
    void whenErrorStatusThenHttpStatusAndBodyAreKept() throws IOException {
        String url = serve(exchange -> respond(exchange, 500, "boom"));

        ClientException failure = assertThrows(ClientException.class, () -> post(url));

        assertFalse(failure instanceof ClientTransportException);
        assertEquals(": HTTP 500: boom", ClientFailures.describe(failure, 300));
    }

    @Test
    void whenAnswerIsNotJsonThenHttpStatusIsKept() throws IOException {
        String url = serve(exchange -> respond(exchange, 200, "<html>login</html>"));

        ClientException failure = assertThrows(ClientException.class, () -> post(url));

        assertFalse(failure instanceof ClientTransportException);
        assertEquals(200, failure.getCode());
        assertTrue(failure.getResponseBody().startsWith("Unreadable response"));
    }

    private JsonNode post(String url) {
        return template(Duration.ofSeconds(5)).exchange(url, HttpMethod.POST, new HttpEntity<>("{}", List.of()), JsonNode.class, Map.of());
    }

    private static JavaHttpClientRestUtilsTemplate template(Duration requestTimeout) {
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
        return JavaHttpClientRestUtilsTemplate.wrap(client, new ObjectMapper(), requestTimeout);
    }

    private String serve(com.sun.net.httpserver.HttpHandler handler) throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/run", handler);
        server.start();
        return "http://localhost:" + server.getAddress().getPort() + "/run";
    }

    private static void respond(com.sun.net.httpserver.HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }
}
