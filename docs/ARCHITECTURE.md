# Architecture Documentation

## Overview

The Ricart-Agrawala Distributed Mutual Exclusion Demo is a multi-module Kotlin application that demonstrates the Ricart-Agrawala algorithm for distributed mutual exclusion.

## Module Structure

### shared
Contains shared models and protocols used across all modules:
- Message protocols (RAMessage, CSProtocol)
- Data models (NodeConfig, CSEntry, CSState, Violation)

### core
Implements the core Ricart-Agrawala algorithm:
- LamportClock: Logical clock implementation
- RicartAgrawala: Main algorithm implementation
- RAState: State management

### network
Provides network communication layer:
- ServiceDiscovery: Multicast-based node discovery
- WebSocketServer/Client: Real-time communication
- HttpServer/Client: REST API and fallback communication

### cs-host
Critical Section Host that manages shared resources:
- CSHost: Main coordinator
- ResourceManager: Manages shared resources
- AccessMonitor: Monitors access patterns
- ViolationDetector: Detects protocol violations
- REST API: HTTP endpoints for state and control

### node
Node application that participates in the distributed system:
- NodeApplication: Main orchestrator
- NodeController: Business logic
- NodeUI: Desktop UI using Compose

### visualizer
Advanced visualization tool:
- Real-time state monitoring
- Resource visualization
- Timeline and statistics
- Network topology

### common-ui
Shared UI components used by node and visualizer modules.

## Communication Flow

1. Nodes discover each other via multicast service discovery
2. Nodes establish WebSocket connections for real-time communication
3. When a node wants to enter CS:
   - Sends REQUEST message to all other nodes
   - Waits for REPLY from all nodes
   - Enters CS when all replies received
   - Sends RELEASE when exiting CS
4. CS Host manages actual resource access and monitors for violations

## Technology Stack

- Kotlin
- Kotlinx Serialization
- Ktor (HTTP/WebSocket)
- Compose Desktop (UI)
- Gradle (Build)

