package org.opendatamesh.platform.up.executor.blindata.config;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

import java.util.Optional;

/** ODM product-plane services the executor calls, in the ODM services' configuration pattern. */
@ConfigMapping(prefix = "odm.product-plane")
public interface OdmServicesProperties {

    /** Registry v2: descriptor of the version to verify. Required. */
    Service registryService();

    /** DevOps v2: target of the results post. Inactive means the report stays in the task logs only. */
    Service devopsService();

    interface Service {

        @WithDefault("false")
        boolean active();

        Optional<String> address();
    }
}
