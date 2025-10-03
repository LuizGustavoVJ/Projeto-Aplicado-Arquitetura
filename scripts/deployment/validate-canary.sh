#!/bin/bash
echo "📊 Validando métricas do canary..."
ERROR_RATE=$(kubectl exec -n pip-prod deployment/pip-canary -- curl -s localhost:8080/actuator/metrics/http.server.requests | jq '.measurements[0].value')
if (( $(echo "$ERROR_RATE > 0.05" | bc -l) )); then
    echo "❌ Taxa de erro muito alta: ${ERROR_RATE}"
    exit 1
fi
echo "✅ Canary validado com sucesso!"
