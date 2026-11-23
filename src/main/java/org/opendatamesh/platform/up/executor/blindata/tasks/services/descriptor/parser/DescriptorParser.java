package org.opendatamesh.platform.up.executor.blindata.tasks.services.descriptor.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.opendatamesh.dpds.datastoreapi.v1.DataStoreApiDefinitionConverter;
import org.opendatamesh.dpds.datastoreapi.v1.parser.DataStoreApiParser;
import org.opendatamesh.dpds.datastoreapi.v1.parser.DataStoreApiParserFactory;
import org.opendatamesh.dpds.model.DataProductVersion;
import org.opendatamesh.dpds.parser.Parser;
import org.opendatamesh.dpds.parser.ParserFactory;
import org.opendatamesh.platform.up.executor.blindata.tasks.services.descriptor.odcs.OdcsTableDefinitionConverter;

import java.io.IOException;

/**
 * The ODM descriptor parser with the plugins the contract needs: the library's datastore API converter for the port
 * definitions, and the ODCS plugin ({@link OdcsTableDefinitionConverter}) for the table definitions.
 */
public final class DescriptorParser {

    private final Parser parser;

    public DescriptorParser(ObjectMapper json) {
        DataStoreApiParser dataStoreApiParser = DataStoreApiParserFactory.getParser(json)
                .register(new OdcsTableDefinitionConverter());
        this.parser = ParserFactory.getParser(json).register(new DataStoreApiDefinitionConverter(dataStoreApiParser));
    }

    public DataProductVersion parse(JsonNode descriptor) throws IOException {
        return parser.deserialize(descriptor);
    }
}
