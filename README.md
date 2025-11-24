# Ricart-Agrawala Distributed Mutual Exclusion

A comprehensive implementation and demonstration of the **Ricart-Agrawala algorithm** for distributed mutual exclusion, featuring a multi-module Kotlin architecture with real-time network communication, critical section management, and desktop UI visualization.

## Overview

This project implements the Ricart-Agrawala algorithm, a distributed algorithm that ensures mutual exclusion in a distributed system without requiring a central coordinator. The system consists of multiple nodes that coordinate access to shared resources using message passing and logical clocks (Lamport clocks).

### Key Features

- **Distributed Mutual Exclusion**: Full implementation of Ricart-Agrawala algorithm
- **Service Discovery**: Automatic node discovery via UDP multicast
- **Real-time Communication**: WebSocket-based message passing between nodes
- **Critical Section Host**: Centralized resource management and monitoring
- **Desktop UI**: Compose Desktop UI for node visualization and control
- **Lamport Clocks**: Logical clock implementation for event ordering
- **Violation Detection**: Automatic detection of protocol violations
- **Resource Management**: Multiple shared resource types (Bank Account, Printer, Document, Counter)

## Prerequisites

- **Java**: JDK 11 or higher
- **Gradle**: 7.0 or higher (included via wrapper)
- **Network**: Multicast support enabled (for service discovery)

## 🚀 Quick Start

### 1. Clone and Build

```bash
git clone <repository-url>
cd r-a-algorithm-app-1
./gradlew build
```

### 2. Start Critical Section Host

```bash
./scripts/start-cs-host.sh
```

The CS Host will start on `http://localhost:8080` by default.

### 3. Start Nodes

In separate terminals, start multiple nodes:

```bash
# Terminal 1
./scripts/start-node.sh config/nodes/node1.json

# Terminal 2
./scripts/start-node.sh config/nodes/node2.json

# Terminal 3
./scripts/start-node.sh config/nodes/node3.json
```

### 4. Use the UI

Each node will open a desktop window where you can:

- **Request CS**: Request access to the critical section
- **Release CS**: Release the critical section
- **View Status**: See current CS status and Lamport clock value
- **Monitor Peers**: View connected nodes

## Project Structure

```text
r-a-algorithm-app-1/
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
├── cs-host/          # Critical Section Host
│   ├── CSHost.kt     # Main coordinator
│   ├── resources/    # Shared resource implementations
│   ├── monitor/      # Monitoring and violation detection
│   └── api/          # REST API and WebSocket handlers
│
├── node/             # Node application
│   ├── NodeApplication.kt
│   ├── controller/   # Business logic
│   └── ui/           # Desktop UI (Compose)
│
├── config/           # Configuration files
│   ├── nodes/        # Node configurations
│   └── cs-host/      # CS Host configuration
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

### CS Host Configuration

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

### Ricart-Agrawala Algorithm Flow

1. **Request Phase**: Node sends `REQUEST` message to all other nodes with its Lamport timestamp
2. **Reply Phase**: Other nodes reply immediately if:
   - They're not requesting CS, OR
   - They're requesting but have lower priority (higher timestamp or lower nodeId)
3. **Enter CS**: Node enters CS when it receives `REPLY` from all other nodes
4. **Release Phase**: Node sends `RELEASE` message when exiting CS

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
- **[Network Architecture](docs/NETWORK_ARCHITECTURE.md)**: Detailed network layer documentation
- **[RA State Explanation](docs/RA_STATE_EXPLANATION.md)**: Understanding RAState
- **[API Documentation](docs/API.md)**: REST API endpoints
- **[User Guide](docs/USER_GUIDE.md)**: Detailed usage instructions

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
# Run CS Host
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

### CS Host Not Responding

- Verify CS Host is running: `curl http://localhost:8080/api/state`
- Check port 8080 is not blocked
- Verify `csHostUrl` in node configurations

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
