package org.opendatamesh.platform.up.executor.blindata.testsupport;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

public final class Fixtures {

    public static final ObjectMapper JSON = new ObjectMapper();
    public static final String ORDERS_FQN = "urn:dpds:acme:dataproducts:orders:1";

    private Fixtures() {
    }

    public static JsonNode json(String resource) {
        try (InputStream in = Fixtures.class.getResourceAsStream("/" + resource)) {
            if (in == null) {
                throw new IllegalArgumentException("Missing test resource " + resource);
            }
            return JSON.readTree(in);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static JsonNode ordersDescriptor() {
        return json("descriptors/orders-1.2.0.json");
    }

    public static JsonNode ordersDescriptorWithBlockingPattern() {
        return json("descriptors/orders-1.3.0-blocking-pattern.json");
    }
}
