// Copyright 2016 Yahoo Inc. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.
package com.yahoo.vespa.hosted.dockerapi;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URL;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

/**
 * Requires docker daemon, see {@link com.yahoo.vespa.hosted.dockerapi.DockerTestUtils} for more details.
 *
 * @author freva
 * @author dybdahl
 */
public class DockerTest {
    private DockerImpl docker;
    private static final DockerImage dockerImage = new DockerImage("simple-ipv6-server:Dockerfile");
    private static final String CONTAINER_NAME_DOCKER_TEST_PREFIX = "docker-test-";

    // It is ignored since it is a bit slow and unstable, at least on Mac.
    @Ignore
    @Test
    public void testDockerImagePullDelete() throws ExecutionException, InterruptedException {
        DockerImage dockerImage = new DockerImage("busybox:1.24.0");

        // Pull the image and wait for the pull to complete
        docker.pullImageAsync(dockerImage).get();
        assertTrue("Failed to download " + dockerImage.asString() + " image", docker.imageIsDownloaded(dockerImage));

        // Remove the image
        docker.deleteImage(dockerImage);
        assertFalse("Failed to delete " + dockerImage.asString() + " image", docker.imageIsDownloaded(dockerImage));
    }

    // Ignored because the test is very slow (several minutes) when swap is enabled, to disable: (Linux)
    // $ sudo swapoff -a
    @Ignore
    @Test
    public void testOutOfMemoryDoesNotAffectOtherContainers() throws InterruptedException, ExecutionException, IOException {
        String hostName1 = "docker10.test.yahoo.com";
        String hostName2 = "docker11.test.yahoo.com";
        ContainerName containerName1 = new ContainerName(CONTAINER_NAME_DOCKER_TEST_PREFIX + 1);
        ContainerName containerName2 = new ContainerName(CONTAINER_NAME_DOCKER_TEST_PREFIX + 2);
        InetAddress inetAddress1 = InetAddress.getByName("172.18.0.10");
        InetAddress inetAddress2 = InetAddress.getByName("172.18.0.11");

        docker.createContainerCommand(dockerImage, containerName1, hostName1)
                .withNetworkMode(DockerImpl.DOCKER_CUSTOM_MACVLAN_NETWORK_NAME)
                .withIpAddress(inetAddress1)
                .withMemoryInMb(100).create();
        docker.startContainer(containerName1);

        docker.createContainerCommand(dockerImage, containerName2, hostName2)
                .withNetworkMode(DockerImpl.DOCKER_CUSTOM_MACVLAN_NETWORK_NAME)
                .withIpAddress(inetAddress2)
                .withMemoryInMb(100).create();
        docker.startContainer(containerName2);

        // 137 = 128 + 9 = kill -9 (SIGKILL)
        assertThat(docker.executeInContainer(containerName2, "python", "/pysrc/fillmem.py", "90").getExitStatus(), is(137));

        // Verify that both HTTP servers are still up
        testReachabilityFromHost("http://" + inetAddress1.getHostAddress() + "/ping");
        testReachabilityFromHost("http://" + inetAddress2.getHostAddress() + "/ping");

        docker.stopContainer(containerName1);
        docker.deleteContainer(containerName1);

        docker.stopContainer(containerName2);
        docker.deleteContainer(containerName2);
    }

    @Test
    public void testContainerCycle() throws IOException, InterruptedException, ExecutionException {
        final ContainerName containerName = new ContainerName(CONTAINER_NAME_DOCKER_TEST_PREFIX + "foo");
        final String containerHostname = "hostName1";

        docker.createContainerCommand(dockerImage, containerName, containerHostname).create();
        Optional<Container> container = docker.getContainer(containerHostname);
        assertTrue(container.isPresent());
        assertFalse(container.get().isRunning);

        docker.startContainer(containerName);
        container = docker.getContainer(containerHostname);
        assertTrue(container.isPresent());
        assertTrue(container.get().isRunning);

        docker.stopContainer(containerName);
        container = docker.getContainer(containerHostname);
        assertTrue(container.isPresent());
        assertFalse(container.get().isRunning);

        docker.deleteContainer(containerName);
        assertThat(docker.getAllManagedContainers().isEmpty(), is(true));
    }

    @Test
    public void testDockerNetworking() throws InterruptedException, ExecutionException, IOException {
        String hostName1 = "docker10.test.yahoo.com";
        String hostName2 = "docker11.test.yahoo.com";
        ContainerName containerName1 = new ContainerName(CONTAINER_NAME_DOCKER_TEST_PREFIX + 1);
        ContainerName containerName2 = new ContainerName(CONTAINER_NAME_DOCKER_TEST_PREFIX + 2);
        InetAddress inetAddress1 = InetAddress.getByName("172.18.0.10");
        InetAddress inetAddress2 = InetAddress.getByName("172.18.0.11");

        docker.createContainerCommand(dockerImage, containerName1, hostName1)
                .withNetworkMode(DockerImpl.DOCKER_CUSTOM_MACVLAN_NETWORK_NAME).withIpAddress(inetAddress1).create();
        docker.startContainer(containerName1);

        docker.createContainerCommand(dockerImage, containerName2, hostName2)
                .withNetworkMode(DockerImpl.DOCKER_CUSTOM_MACVLAN_NETWORK_NAME).withIpAddress(inetAddress2).create();
        docker.startContainer(containerName2);

        testReachabilityFromHost("http://" + inetAddress1.getHostAddress() + "/ping");
        testReachabilityFromHost("http://" + inetAddress2.getHostAddress() + "/ping");

        String[] curlFromNodeToNode = new String[]{"curl", "-g", "http://" + inetAddress2.getHostAddress() + "/ping"};
        ProcessResult result = docker.executeInContainer(containerName1, curlFromNodeToNode);
        assertThat("Could not reach " + containerName2.asString() + " from " + containerName1.asString(),
                result.getOutput(), is("pong\n"));

        docker.stopContainer(containerName1);
        docker.deleteContainer(containerName1);

        docker.stopContainer(containerName2);
        docker.deleteContainer(containerName2);
    }

    @Before
    public void setup() throws InterruptedException, ExecutionException, IOException {
        if (docker == null) {
            assumeTrue(DockerTestUtils.dockerDaemonIsPresent());

            docker = DockerTestUtils.getDocker();
            DockerTestUtils.buildSimpleHttpServerDockerImage(docker, dockerImage);
        }

        // Clean up any non deleted containers from previous tests
        docker.getAllContainers(false).forEach(container -> {
            if (container.name.asString().startsWith(CONTAINER_NAME_DOCKER_TEST_PREFIX)) {
                if (container.isRunning) docker.stopContainer(container.name);
                docker.deleteContainer(container.name);
            }
        });
    }

    private void testReachabilityFromHost(String target) throws IOException, InterruptedException {
        URL url = new URL(target);
        String containerServer = IOUtils.toString(url.openStream());
        assertThat(containerServer, is("pong\n"));
    }
}
