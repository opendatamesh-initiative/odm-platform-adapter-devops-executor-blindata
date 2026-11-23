package org.opendatamesh.platform.up.executor.blindata.tasks.services.descriptor.odcs;

import org.opendatamesh.dpds.model.core.ComponentBase;

import java.util.ArrayList;
import java.util.List;

/** A column of a table definition, with its ODCS quality rules. Other column keys stay in the additional properties. */
public class OdcsPropertyDefinition extends ComponentBase {

    private String name;
    private List<OdcsQualityRule> quality = new ArrayList<>();

    public OdcsPropertyDefinition() {
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
}
