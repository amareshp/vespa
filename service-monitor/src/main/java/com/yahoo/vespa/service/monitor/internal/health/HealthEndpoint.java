// Copyright 2018 Yahoo Holdings. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.
package com.yahoo.vespa.service.monitor.internal.health;

import com.yahoo.config.provision.HostName;
import com.yahoo.vespa.athenz.api.AthenzIdentity;
import com.yahoo.vespa.athenz.identity.ServiceIdentityProvider;
import com.yahoo.vespa.athenz.tls.AthenzIdentityVerifier;

import javax.net.ssl.HostnameVerifier;
import java.net.URL;
import java.util.Collections;
import java.util.Optional;

import static com.yahoo.yolean.Exceptions.uncheck;

/**
 * @author hakon
 */
class HealthEndpoint {
    private final URL url;
    private final Optional<HostnameVerifier> hostnameVerifier;
    private final Optional<ServiceIdentityProvider> serviceIdentityProvider;

    static HealthEndpoint forHttp(HostName hostname, int port) {
        URL url = uncheck(() -> new URL("http", hostname.value(), port, "/state/v1/health"));
        return new HealthEndpoint(url, Optional.empty(), Optional.empty());
    }

    static HealthEndpoint forHttps(HostName hostname,
                                   int port,
                                   ServiceIdentityProvider serviceIdentityProvider,
                                   AthenzIdentity remoteIdentity) {
        URL url = uncheck(() -> new URL("https", hostname.value(), port, "/state/v1/health"));
        HostnameVerifier peerVerifier = new AthenzIdentityVerifier(Collections.singleton(remoteIdentity));
        return new HealthEndpoint(url, Optional.of(serviceIdentityProvider), Optional.of(peerVerifier));
    }

    private HealthEndpoint(URL url,
                           Optional<ServiceIdentityProvider> serviceIdentityProvider,
                           Optional<HostnameVerifier> hostnameVerifier) {
        this.url = url;
        this.serviceIdentityProvider = serviceIdentityProvider;
        this.hostnameVerifier = hostnameVerifier;
    }

    public URL getStateV1HealthUrl() {
        return url;
    }

    public Optional<ServiceIdentityProvider> getServiceIdentityProvider() {
        return serviceIdentityProvider;
    }

    public Optional<HostnameVerifier> getHostnameVerifier() {
        return hostnameVerifier;
    }
}
