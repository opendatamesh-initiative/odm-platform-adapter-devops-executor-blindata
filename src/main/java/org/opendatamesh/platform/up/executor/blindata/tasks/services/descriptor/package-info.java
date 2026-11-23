/**
 * Reading the ODCS data contract declared in a data product descriptor.
 *
 * <ul>
 *   <li>{@link org.opendatamesh.platform.up.executor.blindata.tasks.services.descriptor.DescriptorContractRulesReader}: entry point, parse then walk.</li>
 *   <li>{@code parser}: the ODM descriptor parser library, set up with the plugins the contract needs.</li>
 *   <li>{@code odcs}: the ODCS plugin of the parser (a datastore API table definition converter) and, in
 *   {@code odcs.model}, the model it produces.</li>
 *   <li>{@code visitors}: the walk of the parsed model with the library's visitor interfaces, and the collector that
 *   turns each ODCS rule into a contract rule.</li>
 * </ul>
 */
package org.opendatamesh.platform.up.executor.blindata.tasks.services.descriptor;
