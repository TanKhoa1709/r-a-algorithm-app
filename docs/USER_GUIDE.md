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

#### Start Bank Host
```bash
./scripts/start-cs-host.sh
```

The Bank Host will start on `http://localhost:8080` with an initial balance of 100,000.

#### Start Bank Branches
```bash
# Terminal 1 - Branch 1
./scripts/start-node.sh config/nodes/node1.json

# Terminal 2 - Branch 2
./scripts/start-node.sh config/nodes/node2.json

# Terminal 3 - Branch 3
./scripts/start-node.sh config/nodes/node3.json
```

#### Start Bank Dashboard
```bash
./scripts/start-visualizer.sh
```

## Using the Branch UI

1. **Withdraw**: Enter amount and click "Withdraw" to withdraw money from the shared bank account
2. **Deposit**: Enter amount and click "Deposit" to deposit money to the shared bank account
3. **Status Bar**: Shows current CS status (IN CS / IDLE) and Lamport clock value
4. **Event Log**: Displays transaction events and Ricart-Agrawala messages
5. **Other Branches**: Shows connected branches

**Note**: You can only perform transactions when the branch is not in CS and has no pending requests.

## Using the Bank Dashboard

The Bank Dashboard provides:

- **Current Balance**: Large display of current bank balance
- **Transaction History**: Complete list of all transactions (withdrawals and deposits)
- **Bank Statistics**: Total transactions, withdrawals, deposits, and amounts
- **Branch Status**: Real-time status of all branches

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

### Bank Host Not Responding
- Check if Bank Host is running on port 8080
- Verify the URL in branch configuration files
- Transaction will be cancelled automatically if connection fails

### Transaction Fails
- **Insufficient Balance**: Withdrawal will fail if balance is less than requested amount
- **Connection Error**: Transaction will be cancelled if Bank Host is unreachable
- Check event log for detailed error messages

