# Ricart-Agrawala Distributed Mutual Exclusion Demo

A comprehensive demonstration of the Ricart-Agrawala algorithm for distributed mutual exclusion, featuring a multi-module architecture with network communication, critical section management, and real-time visualization.

## Project Structure

This project is organized into multiple modules:

- **shared**: Shared models and protocols used across modules
- **core**: Core Ricart-Agrawala algorithm implementation
- **network**: Network layer with service discovery and WebSocket communication
- **cs-host**: Critical Section Host with resource management and monitoring
- **node**: Node application with UI for participating in the distributed system
- **visualizer**: Advanced visualization tool for monitoring system state
- **common-ui**: Shared UI components

## Building

```bash
./gradlew build
```

## Running

### Start Critical Section Host
```bash
./scripts/start-cs-host.sh
```

### Start Node
```bash
./scripts/start-node.sh
```

### Start Visualizer
```bash
./scripts/start-visualizer.sh
```

## Documentation

See the `docs/` directory for detailed documentation:
- `ARCHITECTURE.md`: System architecture overview
- `API.md`: API documentation
- `USER_GUIDE.md`: User guide

## License

MIT

