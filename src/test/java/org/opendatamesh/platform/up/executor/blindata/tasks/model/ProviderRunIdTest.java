package org.opendatamesh.platform.up.executor.blindata.tasks.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProviderRunIdTest {

    @Test
    void whenParsingAValidIdThenEnvironmentAndSequenceIdAreRead() {
        ProviderRunId id = ProviderRunId.parse("lab:1234").orElseThrow();

        assertEquals("lab", id.environment());
        assertEquals(1234, id.sequenceId());
        assertEquals("lab:1234", id.value());
    }

    @Test
    void whenParsingAMalformedIdThenEmpty() {
        assertTrue(ProviderRunId.parse(null).isEmpty());
        assertTrue(ProviderRunId.parse("1234").isEmpty());
        assertTrue(ProviderRunId.parse(":1234").isEmpty());
        assertTrue(ProviderRunId.parse("lab:").isEmpty());
        assertTrue(ProviderRunId.parse("lab:x").isEmpty());
        assertTrue(ProviderRunId.parse("lab:-1").isEmpty());
    }
}
