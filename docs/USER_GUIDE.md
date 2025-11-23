# User Guide

## Getting Started

### Prerequisites
- Java 11 or higher
- Gradle 7.0 or higher

### Building the Project

```bash
./gradlew build
```

### Running Components

#### Start CS Host
```bash
./scripts/start-cs-host.sh
```

#### Start a Node
```bash
./scripts/start-node.sh config/nodes/node1.json
```

#### Start Visualizer
```bash
./scripts/start-visualizer.sh
```

#### Run Full Demo
```bash
./scripts/demo-scenario.sh
```

## Using the Node UI

1. **Request CS**: Click "Request CS" to request access to the critical section
2. **Release CS**: Click "Release CS" to exit the critical section
3. **Status Bar**: Shows current CS status and Lamport clock value
4. **Event Log**: Displays recent events
5. **Peers List**: Shows connected nodes

## Using the Visualizer

The visualizer provides multiple views:

- **Dashboard**: Overview of system state
- **Resources**: View and monitor shared resources
- **Timeline**: Access history timeline
- **Network**: Network topology visualization
- **Statistics**: Performance metrics and charts

## Configuration

Edit configuration files in `config/` directory:

- `config/nodes/node*.json`: Node configurations
- `config/cs-host/cs-host-config.json`: CS Host configuration

## Troubleshooting

### Port Conflicts
If you get port conflicts, modify the port numbers in configuration files.

### Nodes Not Discovering Each Other
- Ensure multicast is enabled on your network
- Check firewall settings
- Verify discovery port (default: 8888) is not blocked

### CS Host Not Responding
- Check if CS Host is running on port 8080
- Verify the URL in node configuration files

