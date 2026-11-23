package org.opendatamesh.platform.up.executor.blindata.tasks.services.descriptor;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import org.opendatamesh.dpds.model.DataProductVersion;
import org.opendatamesh.platform.up.executor.blindata.config.BlindataProperties;
import org.opendatamesh.platform.up.executor.blindata.exceptions.BadRequestException;
import org.opendatamesh.platform.up.executor.blindata.tasks.model.ContractRules;
import org.opendatamesh.platform.up.executor.blindata.tasks.model.DataProductVersionDescriptor;
import org.opendatamesh.platform.up.executor.blindata.tasks.services.descriptor.parser.DescriptorParser;
import org.opendatamesh.platform.up.executor.blindata.tasks.services.descriptor.visitors.ContractRulesWalker;

import java.io.IOException;

/**
 * Reads the ODCS data contract of a data product version: parses its descriptor with the ODM descriptor parser and
 * the ODCS plugin ({@code parser}, {@code odcs}), then walks the parsed model with visitors ({@code visitors}).
 */
@ApplicationScoped
public class DescriptorContractRulesReader {

    private final DescriptorParser parser;
    private final ContractRulesWalker walker;

    public DescriptorContractRulesReader(BlindataProperties properties) {
        ObjectMapper json = new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL);
        this.parser = new DescriptorParser(json);
        this.walker = new ContractRulesWalker(json, properties.contractRules().blockingProperty(),
                properties.contractRules().connectionProperty());
    }

    public ContractRules read(DataProductVersionDescriptor descriptor) {
        return walker.walk(parse(descriptor));
    }

    private DataProductVersion parse(DataProductVersionDescriptor descriptor) {
        if (descriptor.content() == null || !descriptor.content().isObject()) {
            throw unreadable(descriptor, "the Registry returned no descriptor content");
        }
        try {
            return parser.parse(descriptor.content());
        } catch (IOException | RuntimeException e) {
            throw unreadable(descriptor, e.getMessage());
        }
    }

    private static BadRequestException unreadable(DataProductVersionDescriptor descriptor, String reason) {
        return new BadRequestException("The descriptor of " + descriptor.dataProductFqn() + " version "
                + descriptor.versionNumber() + " cannot be parsed: " + reason);
    }
}
