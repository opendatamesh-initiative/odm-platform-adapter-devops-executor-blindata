package org.opendatamesh.platform.up.executor.blindata.tasks.services.descriptor.odcs;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.opendatamesh.dpds.datastoreapi.v1.extensions.DataStoreApiStandardDefinitionConverter;

import java.util.regex.Pattern;

/**
 * Plugin of the descriptor parser: turns the {@code json-schema} 1.x definition of a datastore API table into an
 * {@link OdcsTableDefinition} carrying its ODCS data contract. Registered on the datastore API parser, it is applied
 * to every table while the descriptor is parsed; tables of other specifications keep their raw definition.
 */
public class OdcsTableDefinitionConverter implements DataStoreApiStandardDefinitionConverter<OdcsTableDefinition> {

    public static final String SPECIFICATION = "json-schema";
    private static final Pattern VERSION = Pattern.compile("^1(\\.\\d+){0,2}$");

    @Override
    public boolean supports(String specification, String specificationVersion) {
        return SPECIFICATION.equalsIgnoreCase(specification)
                && specificationVersion != null && VERSION.matcher(specificationVersion).matches();
    }

    @Override
    public OdcsTableDefinition deserialize(ObjectMapper defaultMapper, JsonNode jsonNode) throws JacksonException {
        return defaultMapper.treeToValue(jsonNode, OdcsTableDefinition.class);
    }

    @Override
    public JsonNode serialize(ObjectMapper defaultMapper, OdcsTableDefinition value) {
        return defaultMapper.valueToTree(value);
    }
}
