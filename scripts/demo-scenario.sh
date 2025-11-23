#!/bin/bash

echo "Starting Ricart-Agrawala Demo Scenario"
echo "======================================"

# Start CS Host in background
echo "Starting CS Host..."
./gradlew :cs-host:run &
CS_HOST_PID=$!
sleep 3

# Start nodes in background
echo "Starting Node 1..."
./gradlew :node:run --args="config/nodes/node1.json" &
NODE1_PID=$!
sleep 2

echo "Starting Node 2..."
./gradlew :node:run --args="config/nodes/node2.json" &
NODE2_PID=$!
sleep 2

echo "Starting Node 3..."
./gradlew :node:run --args="config/nodes/node3.json" &
NODE3_PID=$!
sleep 2

# Start visualizer
echo "Starting Visualizer..."
./gradlew :visualizer:run &
VISUALIZER_PID=$!

echo ""
echo "All components started!"
echo "CS Host PID: $CS_HOST_PID"
echo "Node 1 PID: $NODE1_PID"
echo "Node 2 PID: $NODE2_PID"
echo "Node 3 PID: $NODE3_PID"
echo "Visualizer PID: $VISUALIZER_PID"
echo ""
echo "Press Ctrl+C to stop all components"

# Wait for interrupt
trap "kill $CS_HOST_PID $NODE1_PID $NODE2_PID $NODE3_PID $VISUALIZER_PID 2>/dev/null; exit" INT
wait

