# API Documentation

## Bank Host REST API

### Base URL
`http://localhost:8080/api`

### Endpoints

#### GET /api/state
Get current bank host state.

**Response:**
```json
{
  "isLocked": false,
  "currentHolder": "node1",
  "queue": [],
  "totalAccesses": 42,
  "violations": []
}
```

#### POST /api/request
Request access to bank resource (legacy endpoint, not used in banking flow).

**Request:**
```json
{
  "nodeId": "node1",
  "requestId": "req-123"
}
```

**Response:**
```json
{
  "granted": true
}
```

#### POST /api/release
Release bank resource access.

**Request:**
```json
{
  "nodeId": "node1",
  "requestId": "req-123"
}
```

**Response:**
```json
{
  "success": true
}
```

#### POST /api/bank/withdraw
Withdraw money from bank account.

**Request:**
```json
{
  "nodeId": "node1",
  "requestId": "req-123",
  "amount": 5000
}
```

**Response:**
```json
{
  "success": true,
  "message": "Withdrew 5000, new balance: 95000",
  "balance": 95000
}
```

**Error Response (insufficient balance):**
```json
{
  "success": false,
  "message": "Insufficient balance. Current balance: 1000, requested: 5000",
  "balance": 1000
}
```

#### POST /api/bank/deposit
Deposit money to bank account.

**Request:**
```json
{
  "nodeId": "node1",
  "requestId": "req-123",
  "amount": 10000
}
```

**Response:**
```json
{
  "success": true,
  "message": "Deposited 10000, new balance: 110000",
  "balance": 110000
}
```

#### GET /api/bank/balance
Get current bank balance.

**Response:**
```json
{
  "balance": 100000
}
```

#### GET /api/history
Get transaction history.

**Response:**
```json
[
  {
    "nodeId": "node1",
    "requestId": "req-123",
    "timestamp": 1234567890,
    "entryTime": 1234567890,
    "exitTime": 1234567900,
    "duration": 10000,
    "transactionType": "WITHDRAW",
    "amount": 5000,
    "balance": 95000
  },
  {
    "nodeId": "node2",
    "requestId": "req-124",
    "timestamp": 1234568000,
    "entryTime": 1234568000,
    "exitTime": 1234568100,
    "duration": 10000,
    "transactionType": "DEPOSIT",
    "amount": 10000,
    "balance": 105000
  }
]
```

#### GET /api/resources
Get all resources.

**Response:**
```json
[
  {
    "resourceId": "counter",
    "currentUser": null,
    "accessCount": 10,
    "lastAccessTime": 1234567890,
    "metadata": {}
  }
]
```

#### POST /api/resources/{resourceId}/access
Access a specific resource.

**Request:**
```json
{
  "nodeId": "node1",
  "requestId": "req-123"
}
```

**Response:**
```json
{
  "success": true,
  "message": "Resource accessed",
  "data": {}
}
```

## WebSocket Endpoints

### /ws
Branch-to-branch communication endpoint (Ricart-Agrawala messages).

### /ws/cs-host
Bank Host state updates endpoint (for branches).

### /visualizer
Bank Dashboard updates endpoint (real-time bank balance and transactions).

