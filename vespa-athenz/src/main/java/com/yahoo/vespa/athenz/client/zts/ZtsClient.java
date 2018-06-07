// Copyright 2018 Yahoo Holdings. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.
package com.yahoo.vespa.athenz.client.zts;

import com.yahoo.vespa.athenz.api.AthenzDomain;
import com.yahoo.vespa.athenz.api.AthenzService;
import com.yahoo.vespa.athenz.api.ZToken;
import com.yahoo.vespa.athenz.tls.Pkcs10Csr;

import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.time.Duration;

/**
 * Interface for a ZTS client.
 *
 * @author bjorncs
 */
public interface ZtsClient extends AutoCloseable {

    /**
     * Register an instance using the specified provider.
     *
     * @param attestationData The signed identity documented serialized to a string.
     * @return A x509 certificate + service token (optional)
     */
    InstanceIdentity registerInstance(AthenzService providerIdentity,
                                      AthenzService instanceIdentity,
                                      String instanceId, // TODO Remove this parameter (unused/unnecessary)
                                      String attestationData,
                                      boolean requestServiceToken,
                                      Pkcs10Csr csr);

    /**
     * Refresh an existing instance
     *
     * @return A x509 certificate + service token (optional)
     */
    InstanceIdentity refreshInstance(AthenzService providerIdentity,
                                     AthenzService instanceIdentity,
                                     String instanceId,
                                     boolean requestServiceToken,
                                     Pkcs10Csr csr);

    /**
     * Fetch a role token for the target domain
     *
     * @param domain Target domain
     * @return A role token
     */
    ZToken getRoleToken(AthenzDomain domain);

    /**
     * Fetch a role token for the target domain and role
     *
     * @param domain Target domain
     * @param roleName Target role
     * @return A role token
     */
    ZToken getRoleToken(AthenzDomain domain, String roleName);

    /**
     * Fetch role certificate for the target domain and role
     *
     * @param domain Target domain
     * @param roleName Target role
     * @param expiry Certificate expiry
     * @param keyPair Key pair which will be used to generate CSR (certificate signing request)
     * @param cloud The cloud suffix used in DNS SAN entries
     * @return A role certificate
     */
    X509Certificate getRoleCertificate(AthenzDomain domain,
                                       String roleName,
                                       Duration expiry,
                                       KeyPair keyPair,
                                       String cloud);

    /**
     * Fetch role certificate for the target domain and role
     *
     * @param domain Target domain
     * @param roleName Target role
     * @param keyPair Key pair which will be used to generate CSR (certificate signing request)
     * @param cloud The cloud suffix used in DNS SAN entries
     * @return A role certificate
     */
    X509Certificate getRoleCertificate(AthenzDomain domain,
                                       String roleName,
                                       KeyPair keyPair,
                                       String cloud);

    void close();
}
