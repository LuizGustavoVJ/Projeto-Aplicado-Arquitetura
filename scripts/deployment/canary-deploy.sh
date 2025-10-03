#!/bin/bash
IMAGE=$1
PERCENTAGE=$2

echo "🐤 Iniciando Canary Deployment com ${PERCENTAGE}% do tráfego..."
kubectl set image deployment/pip-canary pip=${IMAGE} -n pip-prod
kubectl scale deployment/pip-canary --replicas=$(( 3 * ${PERCENTAGE} / 100 )) -n pip-prod
echo "✅ Canary deployment configurado!"
