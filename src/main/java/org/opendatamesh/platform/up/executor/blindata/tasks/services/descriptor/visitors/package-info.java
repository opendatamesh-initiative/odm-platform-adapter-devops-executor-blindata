/**
 * The walk of a parsed descriptor, with the library's visitor interfaces: data product → port → datastore API →
 * ODCS table. {@link org.opendatamesh.platform.up.executor.blindata.tasks.services.descriptor.visitors.ContractRulesWalker} is the only public type; the visitors hand each rule to the
 * collector, which applies the mapping to contract rules.
 */
package org.opendatamesh.platform.up.executor.blindata.tasks.services.descriptor.visitors;
