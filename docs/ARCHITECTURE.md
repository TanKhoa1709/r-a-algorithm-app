# Architecture Documentation

## Overview

The Ricart-Agrawala Distributed Banking System is a multi-module Kotlin application that demonstrates the Ricart-Agrawala algorithm for distributed mutual exclusion in a practical banking scenario. Multiple bank branches coordinate access to a shared bank account using the distributed algorithm.

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
Bank Host that manages the shared bank account (Resource Manager, NOT a coordinator):
- CSHost: Bank resource manager (manages the shared bank account, NOT a coordinator for the algorithm)
- ResourceManager: Manages bank account and other resources
- BankAccountResource: Shared bank account with withdraw/deposit operations
- AccessMonitor: Monitors transaction patterns
- ViolationDetector: Detects protocol violations
- REST API: HTTP endpoints for bank transactions and state

### node
Bank branch application that participates in the distributed banking system:
- NodeApplication: Main orchestrator
- NodeController: Business logic for bank transactions
- NodeUI: Desktop UI for branch operations (withdraw/deposit)

### visualizer
Bank Dashboard for real-time monitoring:
- Current bank balance display
- Transaction history
- Branch status and activity
- Bank statistics (total transactions, withdrawals, deposits)

### common-ui
Shared UI components used by node and visualizer modules.

## Communication Flow

1. Bank branches discover each other via multicast service discovery
2. Branches establish WebSocket connections for real-time communication
3. When a branch wants to perform a transaction (withdraw/deposit):
   - User initiates withdraw/deposit operation
   - Branch sends REQUEST message to all other branches (Ricart-Agrawala algorithm)
   - Waits for REPLY from all branches
   - Enters CS when all replies received (Ricart-Agrawala decides)
   - **After entering CS**, executes transaction on Bank Host (withdraw/deposit)
   - Bank Host processes transaction and updates balance
   - Branch sends RELEASE when exiting CS
4. Bank Host manages the actual bank account and transaction history (NOT a coordinator - just resource management)

## Technology Stack

- Kotlin
- Kotlinx Serialization
- Ktor (HTTP/WebSocket)
- Compose Desktop (UI)
- Gradle (Build)

