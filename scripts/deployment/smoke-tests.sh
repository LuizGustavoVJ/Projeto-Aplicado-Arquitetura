#!/bin/bash
set -e

NAMESPACE=$1
BASE_URL="http://pip-service.${NAMESPACE}.svc.cluster.local"

echo "💨 Executando smoke tests em ${NAMESPACE}..."

# Test 1: Health check
echo "Test 1: Health check..."
curl -f "${BASE_URL}/actuator/health" || exit 1

# Test 2: Metrics endpoint
echo "Test 2: Metrics..."
curl -f "${BASE_URL}/actuator/metrics" || exit 1

# Test 3: API endpoint
echo "Test 3: API endpoint..."
curl -f -H "X-API-Key: test" "${BASE_URL}/v1/payments" || exit 1

echo "✅ Smoke tests concluídos com sucesso!"
