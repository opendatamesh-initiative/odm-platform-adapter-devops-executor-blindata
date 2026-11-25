package org.opendatamesh.platform.up.executor.blindata.tasks.services.descriptor.odcs.model;

import com.fasterxml.jackson.databind.JsonNode;
import org.opendatamesh.dpds.model.core.ComponentBase;

/**
 * One ODCS 3.1 data quality rule of a table or a column. The fields the executor reasons on are typed; every other
 * ODCS key (operators, {@code arguments}, {@code dimension}, …) is kept as it is written in
 * {@link #getAdditionalProperties()}, so the rule can be sent to the agent unchanged.
 */
public class OdcsQualityRule extends ComponentBase {

    private String id;
    private String name;
    private String type;
    private String metric;
    private String rule;
    private String query;
    private JsonNode customProperties;

    public OdcsQualityRule() {
        // Jackson
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getMetric() {
        return metric;
    }

    public void setMetric(String metric) {
        this.metric = metric;
    }

    /** Deprecated ODCS key, same meaning as {@link #getMetric()}. */
    public String getRule() {
        return rule;
    }

    public void setRule(String rule) {
        this.rule = rule;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    /** ODCS list form {@code [{property, value}]} or object form {@code {key: value}}. */
    public JsonNode getCustomProperties() {
        return customProperties;
    }

    public void setCustomProperties(JsonNode customProperties) {
        this.customProperties = customProperties;
    }
}
