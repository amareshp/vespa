// Copyright 2017 Yahoo Holdings. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.
package com.yahoo.vespa.athenz.identityprovider.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.yahoo.container.core.identity.IdentityConfig;
import com.yahoo.vespa.athenz.api.AthenzService;
import com.yahoo.vespa.athenz.identityprovider.api.EntityBindingsMapper;
import com.yahoo.vespa.athenz.identityprovider.api.SignedIdentityDocument;
import com.yahoo.vespa.athenz.identityprovider.api.bindings.SignedIdentityDocumentEntity;
import com.yahoo.vespa.athenz.tls.KeyAlgorithm;
import com.yahoo.vespa.athenz.tls.KeyUtils;
import com.yahoo.vespa.athenz.tls.Pkcs10Csr;
import com.yahoo.vespa.athenz.tls.Pkcs10CsrUtils;
import com.yahoo.vespa.athenz.tls.SslContextBuilder;

import javax.net.ssl.SSLContext;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

import static com.yahoo.vespa.athenz.tls.KeyStoreType.JKS;

/**
 * @author bjorncs
 */
class AthenzCredentialsService {

    private static final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private final IdentityConfig identityConfig;
    private final IdentityDocumentClient identityDocumentClient;
    private final ZtsClient ztsClient;
    private final File trustStoreJks;

    AthenzCredentialsService(IdentityConfig identityConfig,
                             IdentityDocumentClient identityDocumentClient,
                             ZtsClient ztsClient,
                             File trustStoreJks) {
        this.identityConfig = identityConfig;
        this.identityDocumentClient = identityDocumentClient;
        this.ztsClient = ztsClient;
        this.trustStoreJks = trustStoreJks;
    }

    AthenzCredentials registerInstance() {
        KeyPair keyPair = KeyUtils.generateKeypair(KeyAlgorithm.RSA);
        String rawDocument = identityDocumentClient.getSignedIdentityDocument();
        SignedIdentityDocument document = parseSignedIdentityDocument(rawDocument);
        InstanceCsrGenerator instanceCsrGenerator = new InstanceCsrGenerator(document.dnsSuffix());
        Pkcs10Csr csr = instanceCsrGenerator.generateCsr(
                new AthenzService(identityConfig.domain(), identityConfig.service()),
                document.providerUniqueId(),
                document.identityDocument().ipAddresses(),
                keyPair);
        InstanceRegisterInformation instanceRegisterInformation =
                new InstanceRegisterInformation(document.providerService().getFullName(),
                                                identityConfig.domain(),
                                                identityConfig.service(),
                                                rawDocument,
                                                Pkcs10CsrUtils.toPem(csr));
        InstanceIdentity instanceIdentity = ztsClient.sendInstanceRegisterRequest(instanceRegisterInformation,
                                                                                  document.ztsEndpoint());
        return toAthenzCredentials(instanceIdentity, keyPair, document);
    }

    AthenzCredentials updateCredentials(SignedIdentityDocument document, SSLContext sslContext) {
        KeyPair newKeyPair = KeyUtils.generateKeypair(KeyAlgorithm.RSA);
        InstanceCsrGenerator instanceCsrGenerator = new InstanceCsrGenerator(document.dnsSuffix());
        Pkcs10Csr csr = instanceCsrGenerator.generateCsr(
                new AthenzService(identityConfig.domain(), identityConfig.service()),
                document.providerUniqueId(),
                document.identityDocument().ipAddresses(),
                newKeyPair);
        InstanceRefreshInformation refreshInfo = new InstanceRefreshInformation(Pkcs10CsrUtils.toPem(csr));
        InstanceIdentity instanceIdentity =
                ztsClient.sendInstanceRefreshRequest(document.providerService().getFullName(),
                                                     identityConfig.domain(),
                                                     identityConfig.service(),
                                                     document.providerUniqueId().asDottedString(),
                                                     refreshInfo,
                                                     document.ztsEndpoint(),
                                                     sslContext);
        return toAthenzCredentials(instanceIdentity, newKeyPair, document);
    }

    private AthenzCredentials toAthenzCredentials(InstanceIdentity instanceIdentity,
                                                  KeyPair keyPair,
                                                  SignedIdentityDocument identityDocument) {
        X509Certificate certificate = instanceIdentity.getX509Certificate();
        String serviceToken = instanceIdentity.getServiceToken();
        SSLContext identitySslContext = createIdentitySslContext(keyPair.getPrivate(), certificate);
        return new AthenzCredentials(serviceToken, certificate, keyPair, identityDocument, identitySslContext);
    }

    private SSLContext createIdentitySslContext(PrivateKey privateKey, X509Certificate certificate) {
        return new SslContextBuilder()
                .withKeyStore(privateKey, certificate)
                .withTrustStore(trustStoreJks, JKS)
                .build();
    }

    private static SignedIdentityDocument parseSignedIdentityDocument(String rawDocument) {
        try {
            return EntityBindingsMapper.toSignedIdentityDocument(mapper.readValue(rawDocument, SignedIdentityDocumentEntity.class));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
