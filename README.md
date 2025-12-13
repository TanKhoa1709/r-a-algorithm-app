# Ricart-Agrawala Distributed Banking System

A comprehensive implementation and demonstration of the **Ricart-Agrawala algorithm** for distributed mutual exclusion, featuring a distributed banking system where multiple bank branches coordinate access to a shared bank account using message passing and logical clocks (Lamport clocks).

## Overview

This project implements the Ricart-Agrawala algorithm in a practical banking scenario. Multiple bank branches (nodes) coordinate access to a shared bank account using the distributed mutual exclusion algorithm. Each branch can withdraw or deposit money, with the algorithm ensuring that only one branch accesses the account at a time.

**Important Note**: The Ricart-Agrawala algorithm itself is fully distributed - bank branches coordinate directly with each other via message passing. The Bank Host (formerly called CS Host) is an **optional resource management layer** that manages the actual shared bank account and is NOT a coordinator for the Ricart-Agrawala algorithm. The algorithm runs independently between branches to decide access order.

### Key Features

- **Distributed Banking System**: Bank branches coordinate access to shared bank account using Ricart-Agrawala algorithm
- **Bank Transactions**: Withdraw and deposit operations with mutual exclusion guarantee
- **Distributed Mutual Exclusion**: Full implementation of Ricart-Agrawala algorithm (pure distributed, no central coordinator)
- **Service Discovery**: Automatic node discovery via UDP multicast
- **Real-time Communication**: WebSocket-based message passing between nodes
- **Bank Host**: Resource management layer for managing the shared bank account (NOT a coordinator for the algorithm)
- **Desktop UI**: Compose Desktop UI for branch operations and monitoring
- **Bank Dashboard**: Real-time visualization of bank balance, transactions, and branch status
- **Lamport Clocks**: Logical clock implementation for event ordering
- **Transaction History**: Complete audit trail of all bank transactions

## Prerequisites

- **Java**: JDK 11 or higher
- **Gradle**: 7.0 or higher (included via wrapper)
- **Network**: Multicast support enabled (for service discovery)

## 🚀 Quick Start

### 1. Clone and Build

```bash
git clone <repository-url>
cd ricart-agrawala
./gradlew build
```

### 2. Start Bank Host

```bash
./scripts/start-cs-host.sh
```

The Bank Host will start on `http://localhost:8080` by default with an initial balance of 100,000.

### 3. Start Bank Branches

In separate terminals, start multiple bank branches:

```bash
# Terminal 1
./scripts/start-node.sh config/nodes/node1.json

# Terminal 2
./scripts/start-node.sh config/nodes/node2.json

# Terminal 3
./scripts/start-node.sh config/nodes/node3.json
```

### 4. Use the UI

Each branch (node) will open a desktop window where you can:

- **Withdraw**: Withdraw money from the shared bank account
- **Deposit**: Deposit money to the shared bank account
- **View Status**: See current CS status and Lamport clock value
- **Monitor Transactions**: View transaction history in event log
- **View Other Branches**: See connected branches

### 5. Start Bank Dashboard (Visualizer)

```bash
./scripts/start-visualizer.sh
```

The Bank Dashboard will show:
- Current bank balance
- Transaction history
- Branch status and activity
- Bank statistics

## Project Structure

```text
ricart-agrawala/
├── core/              # Core Ricart-Agrawala algorithm implementation
│   ├── LamportClock.kt
│   ├── RicartAgrawala.kt
│   └── RAState.kt
│
├── network/           # Network communication layer
│   ├── discovery/     # Service discovery (multicast)
│   ├── websocket/     # WebSocket server/client
│   └── http/         # HTTP server/client
│
├── shared/            # Shared models and protocols
│   ├── models/       # Data models (NodeConfig, CSState, etc.)
│   └── proto/        # Message protocols (RAMessage, CSProtocol)
│
├── cs-host/          # Bank Host (Resource Manager)
│   ├── CSHost.kt     # Bank resource manager (NOT a coordinator)
│   ├── resources/    # Bank account and other resources
│   ├── monitor/      # Monitoring and violation detection
│   └── api/          # REST API and WebSocket handlers
│
├── node/             # Bank Branch application
│   ├── NodeApplication.kt
│   ├── controller/   # Business logic for bank transactions
│   └── ui/           # Desktop UI (Compose) for branch operations
│
├── config/           # Configuration files
│   ├── nodes/        # Bank branch configurations
│   └── cs-host/      # Bank Host configuration
│
└── docs/             # Documentation
    ├── ARCHITECTURE.md
    ├── NETWORK_ARCHITECTURE.md
    ├── RA_STATE_EXPLANATION.md
    ├── API.md
    └── USER_GUIDE.md
```

## Configuration

### Node Configuration

Edit `config/nodes/node*.json`:

```json
{
  "nodeId": "node1",
  "host": "localhost",
  "port": 8081,
  "csHostUrl": "http://localhost:8080",
  "discoveryPort": 8888,
  "heartbeatInterval": 5000,
  "requestTimeout": 30000
}
```

### Bank Host Configuration

Edit `config/cs-host/cs-host-config.json`:

```json
{
  "port": 8080,
  "maxConcurrentAccess": 1,
  "accessTimeout": 30000,
  "enableMonitoring": true,
  "enableViolationDetection": true
}
```

## 📖 How It Works

### Architecture Overview

The system has **two layers**:

1. **Ricart-Agrawala Algorithm Layer** (Distributed):
   - Nodes coordinate directly with each other via message passing
   - No central coordinator required
   - Pure distributed mutual exclusion

2. **Bank Host Layer** (Optional Resource Manager):
   - Manages the shared bank account
   - Provides transaction processing and balance management
   - NOT a coordinator for the algorithm - just a resource management service

### Ricart-Agrawala Algorithm Flow (Banking Context)

1. **Transaction Request**: Branch initiates withdraw/deposit operation
2. **Request Phase**: Node sends `REQUEST` message to all other branches with its Lamport timestamp
3. **Reply Phase**: Other branches reply immediately if:
   - They're not requesting CS, OR
   - They're requesting but have lower priority (higher timestamp or lower nodeId)
4. **Enter CS**: Branch enters CS when it receives `REPLY` from all other branches (Ricart-Agrawala algorithm decides)
5. **Bank Transaction**: **After entering CS**, branch executes transaction on Bank Host (withdraw/deposit)
6. **Transaction Processing**: Bank Host processes transaction and updates balance
7. **Release Phase**: Branch sends `RELEASE` message when exiting CS, and replies to any pending requests

**Important**: The Ricart-Agrawala algorithm runs independently between branches and decides who accesses the bank account. Bank Host is **NOT** involved in this decision - it's only consulted **after** a branch has already entered CS (according to Ricart-Agrawala) for actual bank transaction processing.

### Message Types

- **REQUEST**: Request to enter critical section
- **REPLY**: Grant permission to enter critical section
- **RELEASE**: Notification of exiting critical section

### Lamport Clock

Each node maintains a logical clock that:

- Increments on local events
- Updates to `max(local_clock, received_timestamp) + 1` when receiving messages

## Running Tests

```bash
./gradlew test
```

## 📚 Documentation

- **[Architecture](docs/ARCHITECTURE.md)**: System architecture overview
- **[API Documentation](docs/API.md)**: REST API endpoints for bank operations
- **[User Guide](docs/USER_GUIDE.md)**: Detailed usage instructions for banking system

## Development

### Building Individual Modules

```bash
# Build specific module
./gradlew :core:build
./gradlew :network:build
./gradlew :cs-host:build
./gradlew :node:build
```

### Running in Development Mode

```bash
# Run Bank Host
./gradlew :cs-host:run

# Run Node with custom config
./gradlew :node:run --args="config/nodes/node1.json"
```

## Troubleshooting

### Port Conflicts

If you encounter port conflicts:

- Modify port numbers in configuration files
- Ensure ports are not in use by other applications

### Nodes Not Discovering Each Other

- **Check multicast**: Ensure multicast is enabled on your network
- **Firewall**: Verify firewall allows UDP on discovery port (default: 8888)
- **Network**: Ensure all nodes are on the same network segment

### Bank Host Not Responding

- Verify Bank Host is running: `curl http://localhost:8080/api/state`
- Check port 8080 is not blocked
- Verify `csHostUrl` in branch configurations
- Transactions will be automatically cancelled if connection fails

## Technology Stack

- **Language**: Kotlin
- **Build Tool**: Gradle
- **Serialization**: Kotlinx Serialization (JSON)
- **Networking**: Ktor (HTTP/WebSocket)
- **UI**: Compose Desktop
- **Concurrency**: Kotlin Coroutines

## License

MIT License

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## Contact

For questions or issues, please open an issue on the repository.

---

**Note**: This is an educational project demonstrating distributed systems concepts. For production use, additional considerations such as fault tolerance, security, and scalability should be addressed.
