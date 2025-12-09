#!/bin/bash

# Start three node instances with predefined configs.
# Usage: ./scripts/start-three-nodes.sh

set -e

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

echo "Starting three nodes..."

./gradlew :node:run --args="config/nodes/node1.json" &
NODE1_PID=$!
echo "Node 1 started (PID $NODE1_PID)"
sleep 2

./gradlew :node:run --args="config/nodes/node2.json" &
NODE2_PID=$!
echo "Node 2 started (PID $NODE2_PID)"
sleep 2

./gradlew :node:run --args="config/nodes/node3.json" &
NODE3_PID=$!
echo "Node 3 started (PID $NODE3_PID)"

echo ""
echo "All three nodes are running."
echo "Node1 PID: $NODE1_PID"
echo "Node2 PID: $NODE2_PID"
echo "Node3 PID: $NODE3_PID"
echo "Press Ctrl+C to stop them."

trap "kill $NODE1_PID $NODE2_PID $NODE3_PID 2>/dev/null; exit" INT
wait

