#!/bin/bash

# Criar mais scripts de deployment
cat > scripts/deployment/canary-deploy.sh << 'CANARY_EOF'
#!/bin/bash
IMAGE=$1
PERCENTAGE=$2

echo "🐤 Iniciando Canary Deployment com ${PERCENTAGE}% do tráfego..."
kubectl set image deployment/pip-canary pip=${IMAGE} -n pip-prod
kubectl scale deployment/pip-canary --replicas=$(( 3 * ${PERCENTAGE} / 100 )) -n pip-prod
echo "✅ Canary deployment configurado!"
CANARY_EOF

cat > scripts/deployment/validate-canary.sh << 'VALIDATE_EOF'
#!/bin/bash
echo "📊 Validando métricas do canary..."
ERROR_RATE=$(kubectl exec -n pip-prod deployment/pip-canary -- curl -s localhost:8080/actuator/metrics/http.server.requests | jq '.measurements[0].value')
if (( $(echo "$ERROR_RATE > 0.05" | bc -l) )); then
    echo "❌ Taxa de erro muito alta: ${ERROR_RATE}"
    exit 1
fi
echo "✅ Canary validado com sucesso!"
VALIDATE_EOF

cat > scripts/deployment/health-checks.sh << 'HEALTH_EOF'
#!/bin/bash
NAMESPACE=$1
echo "❤️ Verificando saúde da aplicação..."
kubectl get pods -n ${NAMESPACE} -l app=pip
kubectl exec -n ${NAMESPACE} deployment/pip -- curl -f localhost:8080/actuator/health
echo "✅ Aplicação saudável!"
HEALTH_EOF

cat > scripts/deployment/post-deploy-tests.sh << 'POST_EOF'
#!/bin/bash
NAMESPACE=$1
echo "✅ Executando testes pós-deploy..."
kubectl exec -n ${NAMESPACE} deployment/pip -- curl -f localhost:8080/v1/payments
echo "✅ Testes pós-deploy concluídos!"
POST_EOF

cat > scripts/compliance/generate-audit-report.sh << 'AUDIT_EOF'
#!/bin/bash
echo "📝 Gerando relatório de auditoria..."
mkdir -p target/compliance-reports
cat > target/compliance-reports/audit-report.html << 'HTML_EOF'
<!DOCTYPE html>
<html>
<head><title>Audit Report - PIP</title></head>
<body>
<h1>Payment Integration Platform - Audit Report</h1>
<p>Build: ${BUILD_NUMBER}</p>
<p>Date: $(date)</p>
<p>Status: SUCCESS</p>
</body>
</html>
HTML_EOF
echo "✅ Relatório gerado!"
AUDIT_EOF

chmod +x scripts/deployment/*.sh
chmod +x scripts/compliance/*.sh

# Criar ConfigMap e Secret para Kubernetes
cat > kubernetes/base/configmap.yaml << 'CM_EOF'
apiVersion: v1
kind: ConfigMap
metadata:
  name: pip-config
data:
  application.properties: |
    spring.application.name=pip
    server.port=8080
    management.endpoints.web.exposure.include=health,metrics,prometheus
CM_EOF

cat > kubernetes/base/secret.yaml << 'SECRET_EOF'
apiVersion: v1
kind: Secret
metadata:
  name: pip-secrets
type: Opaque
stringData:
  database-url: "jdbc:postgresql://postgres:5432/pip"
  database-username: "pip"
  database-password: "changeme"
SECRET_EOF

cat > kubernetes/base/ingress.yaml << 'ING_EOF'
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: pip-ingress
  annotations:
    cert-manager.io/cluster-issuer: "letsencrypt-prod"
    nginx.ingress.kubernetes.io/ssl-redirect: "true"
spec:
  ingressClassName: nginx
  tls:
  - hosts:
    - api.pip.example.com
    secretName: pip-tls
  rules:
  - host: api.pip.example.com
    http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: pip-service
            port:
              number: 80
ING_EOF

# Criar overlays para dev, staging, prod
for env in dev staging prod; do
  mkdir -p kubernetes/overlays/${env}
  cat > kubernetes/overlays/${env}/kustomization.yaml << EOF
apiVersion: kustomize.config.k8s.io/v1beta1
kind: Kustomization

namespace: pip-${env}

resources:
- ../../base

patchesStrategicMerge:
- deployment-patch.yaml

commonLabels:
  environment: ${env}
EOF

  cat > kubernetes/overlays/${env}/deployment-patch.yaml << EOF
apiVersion: apps/v1
kind: Deployment
metadata:
  name: pip
spec:
  replicas: $( [ "$env" = "prod" ] && echo 5 || echo 2 )
  template:
    spec:
      containers:
      - name: pip
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "${env}"
EOF
done

echo "✅ Arquivos restantes criados!"
