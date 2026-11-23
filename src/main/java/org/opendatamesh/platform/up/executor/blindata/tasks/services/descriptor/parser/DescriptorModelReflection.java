package org.opendatamesh.platform.up.executor.blindata.tasks.services.descriptor.parser;

import io.quarkus.runtime.annotations.RegisterForReflection;
import org.opendatamesh.dpds.datastoreapi.v1.model.DataStoreApi;
import org.opendatamesh.dpds.datastoreapi.v1.model.DataStoreApiConnectionProtocolObject;
import org.opendatamesh.dpds.datastoreapi.v1.model.DataStoreApiContact;
import org.opendatamesh.dpds.datastoreapi.v1.model.DataStoreApiDatabaseService;
import org.opendatamesh.dpds.datastoreapi.v1.model.DataStoreApiExternalResourceObject;
import org.opendatamesh.dpds.datastoreapi.v1.model.DataStoreApiInfo;
import org.opendatamesh.dpds.datastoreapi.v1.model.DataStoreApiJdbcConnectionObject;
import org.opendatamesh.dpds.datastoreapi.v1.model.DataStoreApiLicense;
import org.opendatamesh.dpds.datastoreapi.v1.model.DataStoreApiOdbcConnectionObject;
import org.opendatamesh.dpds.datastoreapi.v1.model.DataStoreApiSchema;
import org.opendatamesh.dpds.datastoreapi.v1.model.DataStoreApiServerInfo;
import org.opendatamesh.dpds.datastoreapi.v1.model.DataStoreApiStandardDefinitionObject;
import org.opendatamesh.dpds.datastoreapi.v1.model.DatastoreApiVariableObject;
import org.opendatamesh.dpds.model.DataProductVersion;
import org.opendatamesh.dpds.model.blueprint.Blueprint;
import org.opendatamesh.dpds.model.components.Components;
import org.opendatamesh.dpds.model.core.ComponentBase;
import org.opendatamesh.dpds.model.core.ExternalDocs;
import org.opendatamesh.dpds.model.core.StandardDefinition;
import org.opendatamesh.dpds.model.info.ContactPoint;
import org.opendatamesh.dpds.model.info.Info;
import org.opendatamesh.dpds.model.info.Owner;
import org.opendatamesh.dpds.model.interfaces.Expectations;
import org.opendatamesh.dpds.model.interfaces.InterfaceComponents;
import org.opendatamesh.dpds.model.interfaces.Obligations;
import org.opendatamesh.dpds.model.interfaces.Port;
import org.opendatamesh.dpds.model.interfaces.Promises;
import org.opendatamesh.dpds.model.internals.ApplicationComponent;
import org.opendatamesh.dpds.model.internals.InfrastructuralComponent;
import org.opendatamesh.dpds.model.internals.InternalComponents;
import org.opendatamesh.dpds.model.internals.LifecycleTaskInfo;
import org.opendatamesh.platform.up.executor.blindata.tasks.services.descriptor.odcs.model.OdcsPropertyDefinition;
import org.opendatamesh.platform.up.executor.blindata.tasks.services.descriptor.odcs.model.OdcsQualityRule;
import org.opendatamesh.platform.up.executor.blindata.tasks.services.descriptor.odcs.model.OdcsTableDefinition;

/** The descriptor model is bound by Jackson through reflection: registered for the native image. */
@RegisterForReflection(targets = {
        DataProductVersion.class, Info.class, Owner.class, ContactPoint.class, Blueprint.class,
        InterfaceComponents.class, Port.class, Promises.class, Expectations.class, Obligations.class,
        InternalComponents.class, ApplicationComponent.class, InfrastructuralComponent.class, LifecycleTaskInfo.class,
        Components.class, ComponentBase.class, StandardDefinition.class, ExternalDocs.class,
        DataStoreApi.class, DataStoreApiInfo.class, DataStoreApiContact.class, DataStoreApiLicense.class,
        DataStoreApiDatabaseService.class, DataStoreApiServerInfo.class, DataStoreApiConnectionProtocolObject.class,
        DataStoreApiJdbcConnectionObject.class, DataStoreApiOdbcConnectionObject.class, DatastoreApiVariableObject.class,
        DataStoreApiExternalResourceObject.class, DataStoreApiSchema.class, DataStoreApiStandardDefinitionObject.class,
        OdcsTableDefinition.class, OdcsPropertyDefinition.class, OdcsQualityRule.class
})
final class DescriptorModelReflection {

    private DescriptorModelReflection() {
    }
}
