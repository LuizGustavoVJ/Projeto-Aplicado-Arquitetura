#!/bin/bash
NAMESPACE=$1
echo "❤️ Verificando saúde da aplicação..."
kubectl get pods -n ${NAMESPACE} -l app=pip
kubectl exec -n ${NAMESPACE} deployment/pip -- curl -f localhost:8080/actuator/health
echo "✅ Aplicação saudável!"
