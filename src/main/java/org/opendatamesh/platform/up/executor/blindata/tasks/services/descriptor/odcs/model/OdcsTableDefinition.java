package org.opendatamesh.platform.up.executor.blindata.tasks.services.descriptor.odcs.model;

import org.opendatamesh.dpds.model.core.ComponentBase;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The definition of a datastore API table ({@code json-schema}), read for its ODCS data contract: the table-level
 * {@code quality} rules and the {@code quality} rules of each column. Other keys stay in the additional properties.
 */
public class OdcsTableDefinition extends ComponentBase {

    private String name;
    private List<OdcsQualityRule> quality = new ArrayList<>();
    private Map<String, OdcsPropertyDefinition> properties = new LinkedHashMap<>();

    public OdcsTableDefinition() {
        // Jackson
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<OdcsQualityRule> getQuality() {
        return quality;
    }

    public void setQuality(List<OdcsQualityRule> quality) {
        this.quality = quality;
    }

    public Map<String, OdcsPropertyDefinition> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, OdcsPropertyDefinition> properties) {
        this.properties = properties;
    }
}
