package org.opendatamesh.platform.up.executor.blindata.tasks.model;

import com.fasterxml.jackson.databind.JsonNode;

/** One data product version read from the Registry, with its descriptor content (internal references resolved). */
public record DataProductVersionDescriptor(String dataProductFqn, String versionNumber, String tag, JsonNode content) {
}
