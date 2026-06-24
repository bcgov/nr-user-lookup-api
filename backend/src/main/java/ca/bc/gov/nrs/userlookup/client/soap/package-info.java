/**
 * JAXB-bound request/response types for the BCeID V10 SOAP web service.
 *
 * <p>The Node proxy this service replaces parsed the live WSDL at runtime; here
 * we instead hand-write the small subset of the schema we actually use
 * (searchInternalAccount, getAccountDetail) and POST directly to the service
 * endpoint, so no WSDL fetch (and no WSDL credentials) is needed at build or
 * run time.</p>
 *
 * <p><b>Single source of truth for the namespace.</b> If the BCeID schema
 * namespace ever differs from the value below, change it here only — every
 * generated element inherits it via {@code elementFormDefault = QUALIFIED}.
 * This namespace must match the live WSDL's {@code targetNamespace}; verify it
 * before going live (see the README "BCeID SOAP integration" section).</p>
 */
@XmlSchema(
    namespace = BceidSoapNamespace.URI,
    elementFormDefault = XmlNsForm.QUALIFIED)
package ca.bc.gov.nrs.userlookup.client.soap;

import jakarta.xml.bind.annotation.XmlNsForm;
import jakarta.xml.bind.annotation.XmlSchema;
