#!/bin/bash

# Script para executar testes de segurança automatizados
# Utiliza OWASP ZAP para scanning de vulnerabilidades

set -e

echo "========================================="
echo "PIP - Security Scanning"
echo "========================================="

# Configurações
TARGET_URL="${TARGET_URL:-http://localhost:8080}"
ZAP_PORT="${ZAP_PORT:-8090}"
REPORT_DIR="./security-reports"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)

# Criar diretório de relatórios
mkdir -p "$REPORT_DIR"

echo "Target URL: $TARGET_URL"
echo "Report Directory: $REPORT_DIR"
echo ""

# Verificar se o ZAP está disponível
if ! command -v docker &> /dev/null; then
    echo "Error: Docker is required but not installed."
    exit 1
fi

echo "Starting OWASP ZAP Docker container..."
docker run -d --name zap \
    -p $ZAP_PORT:$ZAP_PORT \
    -v $(pwd)/$REPORT_DIR:/zap/wrk/:rw \
    owasp/zap2docker-stable \
    zap.sh -daemon -host 0.0.0.0 -port $ZAP_PORT -config api.disablekey=true

# Aguardar ZAP iniciar
echo "Waiting for ZAP to start..."
sleep 30

# Executar spider (crawling)
echo ""
echo "Running spider scan..."
docker exec zap zap-cli --zap-url http://localhost:$ZAP_PORT spider $TARGET_URL

# Aguardar spider completar
echo "Waiting for spider to complete..."
sleep 10

# Executar active scan
echo ""
echo "Running active scan..."
docker exec zap zap-cli --zap-url http://localhost:$ZAP_PORT active-scan $TARGET_URL

# Aguardar scan completar
echo "Waiting for active scan to complete..."
sleep 30

# Gerar relatório HTML
echo ""
echo "Generating HTML report..."
docker exec zap zap-cli --zap-url http://localhost:$ZAP_PORT report \
    -o /zap/wrk/security-report-${TIMESTAMP}.html -f html

# Gerar relatório JSON
echo "Generating JSON report..."
docker exec zap zap-cli --zap-url http://localhost:$ZAP_PORT report \
    -o /zap/wrk/security-report-${TIMESTAMP}.json -f json

# Gerar relatório XML
echo "Generating XML report..."
docker exec zap zap-cli --zap-url http://localhost:$ZAP_PORT report \
    -o /zap/wrk/security-report-${TIMESTAMP}.xml -f xml

# Obter alertas
echo ""
echo "Checking for security alerts..."
ALERTS=$(docker exec zap zap-cli --zap-url http://localhost:$ZAP_PORT alerts -l High)

# Parar e remover container
echo ""
echo "Stopping ZAP container..."
docker stop zap
docker rm zap

# Verificar se há alertas críticos
if [ -n "$ALERTS" ]; then
    echo ""
    echo "========================================="
    echo "WARNING: High severity alerts found!"
    echo "========================================="
    echo "$ALERTS"
    echo ""
    echo "Full reports available at: $REPORT_DIR/security-report-${TIMESTAMP}.*"
    exit 1
else
    echo ""
    echo "========================================="
    echo "Security scan completed successfully!"
    echo "========================================="
    echo "No high severity vulnerabilities found."
    echo "Full reports available at: $REPORT_DIR/security-report-${TIMESTAMP}.*"
    exit 0
fi
