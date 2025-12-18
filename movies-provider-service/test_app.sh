#!/bin/bash

echo "Starting the application..."
cd /Users/maurosls/Documents/arq-soft/movies-provider-service

# Start the application in background
sbt "runMain Main" &
APP_PID=$!

# Wait for services to start
sleep 10

echo "Testing Movie HTTP API..."
curl -s "http://localhost:9090/movie/Inception" | head -c 100
echo ""

echo "Testing GraphQL API..."
curl -s -X POST "http://localhost:8081/graphql" \
  -H "Content-Type: application/json" \
  -d '{"query": "{ movie(title: \"Inception\") { title year } }"}' | head -c 100
echo ""

# Stop the application
kill $APP_PID 2>/dev/null
wait $APP_PID 2>/dev/null

echo "Test completed."
