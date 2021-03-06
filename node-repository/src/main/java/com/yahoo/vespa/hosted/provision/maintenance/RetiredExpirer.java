// Copyright 2017 Yahoo Holdings. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.
package com.yahoo.vespa.hosted.provision.maintenance;

import com.yahoo.config.provision.ApplicationId;
import com.yahoo.config.provision.Deployer;
import com.yahoo.config.provision.Deployment;
import com.yahoo.config.provision.NodeType;
import com.yahoo.vespa.applicationmodel.HostName;
import com.yahoo.vespa.hosted.provision.Node;
import com.yahoo.vespa.hosted.provision.NodeRepository;
import com.yahoo.vespa.hosted.provision.node.History;
import com.yahoo.vespa.orchestrator.OrchestrationException;
import com.yahoo.vespa.orchestrator.Orchestrator;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * Maintenance job which deactivates retired nodes, if given permission by orchestrator, or
 * after the system has been given sufficient time to migrate data to other nodes.
 *
 * @author hakon
 */
public class RetiredExpirer extends Maintainer {

    private final Deployer deployer;
    private final Orchestrator orchestrator;
    private final Duration retiredExpiry;
    private final Clock clock;

    public RetiredExpirer(NodeRepository nodeRepository,
                          Orchestrator orchestrator,
                          Deployer deployer,
                          Clock clock,
                          Duration maintenanceInterval,
                          Duration retiredExpiry,
                          JobControl jobControl) {
        super(nodeRepository, maintenanceInterval, jobControl);
        this.deployer = deployer;
        this.orchestrator = orchestrator;
        this.retiredExpiry = retiredExpiry;
        this.clock = clock;
    }

    @Override
    protected void maintain() {
        List<Node> activeNodes = nodeRepository().getNodes(Node.State.active);

        Map<ApplicationId, List<Node>> retiredNodesByApplication = activeNodes.stream()
                .filter(node -> node.allocation().isPresent())
                .filter(node -> node.allocation().get().membership().retired())
                .collect(Collectors.groupingBy(node -> node.allocation().get().owner()));

        for (Map.Entry<ApplicationId, List<Node>> entry : retiredNodesByApplication.entrySet()) {
            ApplicationId application = entry.getKey();
            List<Node> retiredNodes = entry.getValue();

            try {
                Optional<Deployment> deployment = deployer.deployFromLocalActive(application);
                if ( ! deployment.isPresent()) continue; // this will be done at another config server

                List<Node> nodesToRemove = retiredNodes.stream().filter(this::canRemove).collect(Collectors.toList());
                if (nodesToRemove.isEmpty()) {
                    continue;
                }

                nodeRepository().setRemovable(application, nodesToRemove);

                deployment.get().activate();

                String nodeList = nodesToRemove.stream().map(Node::hostname).collect(Collectors.joining(", "));
                log.info("Redeployed " + application + " to deactivate retired nodes: " +  nodeList);
            } catch (RuntimeException e) {
                String nodeList = retiredNodes.stream().map(Node::hostname).collect(Collectors.joining(", "));
                log.log(Level.WARNING, "Exception trying to deactivate retired nodes from " + application
                        + ": " + nodeList, e);
            }
        }
    }

    /**
     * Checks if the node can be removed:
     * if the node is a docker host, it will only be removed if it has no children,
     * or all its children are parked or failed.
     * Otherwise, a removal is allowed if either of these are true:
     * - The node has been in state {@link History.Event.Type#retired} for longer than {@link #retiredExpiry}
     * - Orchestrator allows it
     */
    private boolean canRemove(Node node) {
        if (node.type().isDockerHost()) {
            return nodeRepository()
                    .getChildNodes(node.hostname()).stream()
                    .allMatch(child -> child.state() == Node.State.parked || child.state() == Node.State.failed);
        }

        Optional<Instant> timeOfRetiredEvent = node.history().event(History.Event.Type.retired).map(History.Event::at);
        Optional<Instant> retireAfter = timeOfRetiredEvent.map(retiredEvent -> retiredEvent.plus(retiredExpiry));
        boolean shouldRetireNowBecauseExpired = retireAfter.map(time -> time.isBefore(clock.instant())).orElse(false);
        if (shouldRetireNowBecauseExpired) {
            return true;
        }

        try {
            orchestrator.acquirePermissionToRemove(new HostName(node.hostname()));
            return true;
        } catch (OrchestrationException e) {
            log.info("Did not get permission to remove retired " + node + ": " + e.getMessage());
            return false;
        }
    }

}
