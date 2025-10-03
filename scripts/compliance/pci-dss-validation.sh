#!/bin/bash
set -e

echo "🛡️ Validando conformidade PCI-DSS..."

# Verificar se dados sensíveis não estão em logs
echo "Verificando logs..."
if grep -r "cardNumber\|cvv\|password" src/main/java/ --exclude-dir=test; then
    echo "❌ Dados sensíveis encontrados no código!"
    exit 1
fi

# Verificar TLS
echo "Verificando TLS..."
grep -q "server.ssl.enabled=true" src/main/resources/application.properties || echo "⚠️ TLS não configurado"

# Verificar criptografia
echo "Verificando criptografia..."
grep -q "AES" src/main/java/ || echo "⚠️ Criptografia AES não encontrada"

echo "✅ Validação PCI-DSS concluída!"
