package org.opendatamesh.platform.up.executor.blindata.tasks.services.descriptor;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import org.opendatamesh.dpds.datastoreapi.v1.DataStoreApiDefinitionConverter;
import org.opendatamesh.dpds.datastoreapi.v1.parser.DataStoreApiParser;
import org.opendatamesh.dpds.datastoreapi.v1.parser.DataStoreApiParserFactory;
import org.opendatamesh.dpds.model.DataProductVersion;
import org.opendatamesh.dpds.parser.Parser;
import org.opendatamesh.dpds.parser.ParserFactory;
import org.opendatamesh.platform.up.executor.blindata.config.BlindataProperties;
import org.opendatamesh.platform.up.executor.blindata.exceptions.BadRequestException;
import org.opendatamesh.platform.up.executor.blindata.tasks.model.ContractRules;
import org.opendatamesh.platform.up.executor.blindata.tasks.model.DataProductVersionDescriptor;
import org.opendatamesh.platform.up.executor.blindata.tasks.services.descriptor.odcs.OdcsTableDefinitionConverter;

import java.io.IOException;

/**
 * Reads the ODCS data contract of a descriptor with the ODM descriptor parser library.
 *
 * <ul>
 *   <li>The descriptor parser turns each port's {@code datastoreapi} 1.x definition into a {@code DataStoreApi}
 *   ({@code DataStoreApiDefinitionConverter}).</li>
 *   <li>The ODCS plugin ({@link OdcsTableDefinitionConverter}) turns each {@code json-schema} 1.x table definition into
 *   an {@code OdcsTableDefinition} with its quality rules.</li>
 *   <li>Visitors walk the parsed model, data product → port → datastore API → table, and hand each rule to the
 *   {@link ContractRulesCollector}, which applies the observer's mapping.</li>
 * </ul>
 */
@ApplicationScoped
public class DescriptorContractRulesReader {

    private final ObjectMapper json;
    private final Parser parser;
    private final String blockingProperty;
    private final String connectionProperty;

    public DescriptorContractRulesReader(BlindataProperties properties) {
        this.json = new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL);
        DataStoreApiParser dataStoreApiParser = DataStoreApiParserFactory.getParser(json)
                .register(new OdcsTableDefinitionConverter());
        this.parser = ParserFactory.getParser(json).register(new DataStoreApiDefinitionConverter(dataStoreApiParser));
        this.blockingProperty = properties.contractRules().blockingProperty();
        this.connectionProperty = properties.contractRules().connectionProperty();
    }

    public ContractRules read(DataProductVersionDescriptor descriptor) {
        DataProductVersion dataProductVersion = parse(descriptor);
        ContractRulesCollector collector = new ContractRulesCollector(json, blockingProperty);
        DataProductRulesVisitor visitor = new DataProductRulesVisitor(collector, connectionProperty);
        if (dataProductVersion.getInfo() != null) {
            dataProductVersion.getInfo().accept(visitor);
        }
        if (dataProductVersion.getInterfaceComponents() != null) {
            dataProductVersion.getInterfaceComponents().accept(visitor);
        }
        return collector.result();
    }

    private DataProductVersion parse(DataProductVersionDescriptor descriptor) {
        if (descriptor.content() == null || !descriptor.content().isObject()) {
            throw unreadable(descriptor, "the Registry returned no descriptor content");
        }
        try {
            return parser.deserialize(descriptor.content());
        } catch (IOException | RuntimeException e) {
            throw unreadable(descriptor, e.getMessage());
        }
    }

    private static BadRequestException unreadable(DataProductVersionDescriptor descriptor, String reason) {
        return new BadRequestException("The descriptor of " + descriptor.dataProductFqn() + " version "
                + descriptor.versionNumber() + " cannot be parsed: " + reason);
    }
}
