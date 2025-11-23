# API Documentation

## CS Host REST API

### Base URL
`http://localhost:8080/api`

### Endpoints

#### GET /api/state
Get current critical section state.

**Response:**
```json
{
  "isLocked": false,
  "currentHolder": "node1",
  "queue": ["node2", "node3"],
  "totalAccesses": 42,
  "violations": []
}
```

#### POST /api/request
Request access to critical section.

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
Release critical section.

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

#### GET /api/history
Get access history.

**Response:**
```json
[
  {
    "nodeId": "node1",
    "requestId": "req-123",
    "timestamp": 1234567890,
    "entryTime": 1234567890,
    "exitTime": 1234567900,
    "duration": 10000
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
Node-to-node communication endpoint.

### /ws/cs-host
CS Host state updates endpoint.

