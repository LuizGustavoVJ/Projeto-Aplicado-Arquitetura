#!/bin/bash
NAMESPACE=$1
echo "✅ Executando testes pós-deploy..."
kubectl exec -n ${NAMESPACE} deployment/pip -- curl -f localhost:8080/v1/payments
echo "✅ Testes pós-deploy concluídos!"
