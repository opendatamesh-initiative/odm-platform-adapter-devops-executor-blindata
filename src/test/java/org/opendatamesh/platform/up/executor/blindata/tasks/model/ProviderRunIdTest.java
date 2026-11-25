package org.opendatamesh.platform.up.executor.blindata.tasks.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProviderRunIdTest {

    @Test
    void whenParsingAValidIdThenEnvironmentAndSequenceIdAreRead() {
        ProviderRunId id = ProviderRunId.parse("dev:1234").orElseThrow();

        assertEquals("dev", id.environment());
        assertEquals(1234, id.sequenceId());
        assertEquals("dev:1234", id.value());
    }

    @Test
    void whenParsingAMalformedIdThenEmpty() {
        assertTrue(ProviderRunId.parse(null).isEmpty());
        assertTrue(ProviderRunId.parse("1234").isEmpty());
        assertTrue(ProviderRunId.parse(":1234").isEmpty());
        assertTrue(ProviderRunId.parse("dev:").isEmpty());
        assertTrue(ProviderRunId.parse("dev:x").isEmpty());
        assertTrue(ProviderRunId.parse("dev:-1").isEmpty());
    }
}
