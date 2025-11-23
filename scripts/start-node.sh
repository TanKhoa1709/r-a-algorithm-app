#!/bin/bash

NODE_CONFIG=${1:-config/nodes/node1.json}

echo "Starting Node with config: $NODE_CONFIG"
./gradlew :node:run --args="$NODE_CONFIG"

