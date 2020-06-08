/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.client

import kotlinx.coroutines.launch
import mu.KotlinLogging
import orbit.client.actor.ActorProxyFactory
import orbit.client.addressable.AddressableDefinitionDirectory
import orbit.client.addressable.AddressableProxyFactory
import orbit.client.addressable.CapabilitiesScanner
import orbit.client.addressable.InvocationSystem
import orbit.client.execution.AddressableDeactivator
import orbit.client.execution.ExecutionLeases
import orbit.client.execution.ExecutionSystem
import orbit.client.mesh.AddressableLeaser
import orbit.client.mesh.NodeLeaseRenewalFailed
import orbit.client.mesh.NodeLeaseRenewalFailedHandler
import orbit.client.mesh.NodeLeaser
import orbit.client.net.ClientAuthInterceptor
import orbit.client.net.ClientState
import orbit.client.net.ConnectionHandler
import orbit.client.net.GrpcClient
import orbit.client.net.LocalNode
import orbit.client.net.MessageHandler
import orbit.client.serializer.Serializer
import orbit.shared.mesh.NodeId
import orbit.util.concurrent.SupervisorScope
import orbit.util.di.ComponentContainer
import orbit.util.time.ConstantTicker
import orbit.util.time.stopwatch
import kotlin.coroutines.CoroutineContext

class OrbitClient(val config: OrbitClientConfig = OrbitClientConfig()) {
    internal val status: ClientState get() = localNode.status.clientState
    val nodeId: NodeId? get() = localNode.status.nodeInfo?.id

    private val logger = KotlinLogging.logger { }

    private val container = ComponentContainer()
    private val clock = config.clock

    private val scope = SupervisorScope(
        pool = config.pool,
        exceptionHandler = this::onUnhandledException
    )

    private val ticker = ConstantTicker(
        scope = scope,
        targetTickRate = config.tickRate.toMillis(),
        clock = clock,
        logger = logger,
        exceptionHandler = this::onUnhandledException,
        autoStart = false,
        onTick = this::tick
    )

    init {
        container.configure {
            instance(this@OrbitClient)
            instance(config)
            instance(scope)
            instance(clock)
            instance(LocalNode(config))

            definition<GrpcClient>()
            definition<ClientAuthInterceptor>()
            definition<ConnectionHandler>()
            definition<MessageHandler>()

            definition<NodeLeaser>()
            definition<AddressableLeaser>()
            externallyConfigured(config.nodeLeaseRenewalFailedHandler)

            definition<Serializer>()

            definition<CapabilitiesScanner>()
            definition<AddressableProxyFactory>()
            definition<InvocationSystem>()
            definition<AddressableDefinitionDirectory>()
            externallyConfigured(config.addressableConstructor)
            externallyConfigured(config.addressableDeactivator)


            definition<ExecutionSystem>()
            definition<ExecutionLeases>()

            definition<ActorProxyFactory>()

            // Hook to allow overriding container definitions
            config.containerOverrides(this)
        }
    }

    private val nodeLeaser by container.inject<NodeLeaser>()
    private val messageHandler by container.inject<MessageHandler>()

    private val connectionHandler by container.inject<ConnectionHandler>()
    private val capabilitiesScanner by container.inject<CapabilitiesScanner>()
    private val localNode by container.inject<LocalNode>()
    private val definitionDirectory by container.inject<AddressableDefinitionDirectory>()
    private val executionSystem by container.inject<ExecutionSystem>()
    private val nodeLeaseRenewalFailedHandler by container.inject<NodeLeaseRenewalFailedHandler>()

    val actorFactory by container.inject<ActorProxyFactory>()

    fun start() = scope.launch {
        logger.info("Starting Orbit client...")
        val (elapsed, _) = stopwatch(clock) {
            // Flip state
            localNode.manipulate {
                it.copy(clientState = ClientState.CONNECTING)
            }

            // Scan for capabilities
            capabilitiesScanner.scan()
            definitionDirectory.setupDefinition(
                interfaceClasses = capabilitiesScanner.addressableInterfaces,
                impls = capabilitiesScanner.interfaceLookup
            )
            localNode.manipulate {
                it.copy(capabilities = definitionDirectory.generateCapabilities())
            }

            // Get first lease
            nodeLeaser.joinCluster()

            // Open message channel
            connectionHandler.connect()

            localNode.manipulate {
                it.copy(clientState = ClientState.CONNECTED)
            }

            // Start tick
            ticker.start()
        }

        logger.info("Orbit client started successfully in {}ms.", elapsed)
    }

    private suspend fun tick() {
        // Keep stream open
        connectionHandler.tick()

        // See if lease needs renewing
        nodeLeaser.tick()

        // Timeout messages etc
        messageHandler.tick()

        // Handle actor deactivations and leases
        executionSystem.tick()
    }

    fun stop(deactivator: AddressableDeactivator? = null) = scope.launch {
        logger.info("Stopping Orbit node ${nodeId}...")
        val (elapsed, _) = stopwatch(clock) {
            localNode.manipulate {
                it.copy(clientState = ClientState.STOPPING)
            }

            nodeLeaser.leaveCluster()

            // Drain all addressables
            executionSystem.stop(deactivator)

            // Stop the tick
            ticker.stop()

            // Stop messaging
            connectionHandler.disconnect()

            localNode.manipulate {
                it.copy(clientState = ClientState.IDLE)
            }
        }

        logger.info("Orbit stopped successfully in {}ms.", elapsed)
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onUnhandledException(coroutineContext: CoroutineContext, throwable: Throwable) =
        onUnhandledException(throwable)

    private fun onUnhandledException(throwable: Throwable) {
        when (throwable) {
            is NodeLeaseRenewalFailed -> {
                logger.error { "Node lease renewal failed..." }
                nodeLeaseRenewalFailedHandler.onLeaseRenewalFailed()
            }
            else -> logger.error(throwable) { "Unhandled exception in Orbit Client." }
        }
    }

}