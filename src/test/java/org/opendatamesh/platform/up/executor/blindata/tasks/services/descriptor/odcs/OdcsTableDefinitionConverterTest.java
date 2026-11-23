package org.opendatamesh.platform.up.executor.blindata.tasks.services.descriptor.odcs;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.opendatamesh.platform.up.executor.blindata.tasks.services.descriptor.odcs.model.OdcsQualityRule;
import org.opendatamesh.platform.up.executor.blindata.tasks.services.descriptor.odcs.model.OdcsTableDefinition;
import org.opendatamesh.platform.up.executor.blindata.testsupport.Fixtures;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The ODCS plugin of the descriptor parser: which table definitions it reads, and what it keeps of a rule. */
class OdcsTableDefinitionConverterTest {

    private final OdcsTableDefinitionConverter converter = new OdcsTableDefinitionConverter();

    @Test
    void whenTheTableIsJsonSchemaOneThenThePluginReadsIt() {
        assertTrue(converter.supports("json-schema", "1"));
        assertTrue(converter.supports("JSON-SCHEMA", "1.0.0"));
        assertFalse(converter.supports("json-schema", "2.0.0"));
        assertFalse(converter.supports("avro", "1"));
        assertFalse(converter.supports("json-schema", null));
    }

    /** Scenario: typed fields are read, every other ODCS key is kept as written (operators, arguments, catalog keys). */
    @Test
    void whenDeserializingThenRulesKeepTheirOdcsKeys() throws Exception {
        JsonNode definition = Fixtures.ordersDescriptor()
                .at("/interfaceComponents/outputPorts/0/promises/api/definition/schema/tables/0/definition");

        OdcsTableDefinition table = converter.deserialize(Fixtures.JSON, definition);

        assertEquals("orders", table.getName());
        OdcsQualityRule notEmpty = table.getQuality().get(0);
        assertEquals("orders_not_empty", notEmpty.getId());
        assertEquals("rowCount", notEmpty.getMetric());
        assertEquals(0, notEmpty.getAdditionalProperties().get("mustBeGreaterThan").asInt());
        assertEquals("completeness", notEmpty.getAdditionalProperties().get("dimension").asText());
        assertEquals("status_valid", table.getProperties().get("status").getQuality().get(0).getId());
    }
}
