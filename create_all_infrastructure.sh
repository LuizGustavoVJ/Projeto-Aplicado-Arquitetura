#!/bin/bash
set -e

echo "🚀 Criando infraestrutura completa Jenkins + Docker + Kubernetes..."

# Criar Dockerfile
cat > docker/Dockerfile << 'DOCKERFILE_EOF'
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# Adicionar usuário não-root (segurança PCI-DSS)
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
DOCKERFILE_EOF

# Criar docker-compose para desenvolvimento
cat > docker-compose.yml << 'COMPOSE_EOF'
version: '3.8'

services:
  pip-app:
    build:
      context: .
      dockerfile: docker/Dockerfile
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=dev
      - SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/pip
      - SPRING_DATASOURCE_USERNAME=pip
      - SPRING_DATASOURCE_PASSWORD=pip123
      - SPRING_REDIS_HOST=redis
      - SPRING_RABBITMQ_HOST=rabbitmq
    depends_on:
      - postgres
      - redis
      - rabbitmq
    networks:
      - pip-network

  postgres:
    image: postgres:15-alpine
    environment:
      - POSTGRES_DB=pip
      - POSTGRES_USER=pip
      - POSTGRES_PASSWORD=pip123
    volumes:
      - postgres-data:/var/lib/postgresql/data
    networks:
      - pip-network

  redis:
    image: redis:7-alpine
    networks:
      - pip-network

  rabbitmq:
    image: rabbitmq:3-management-alpine
    ports:
      - "15672:15672"
    networks:
      - pip-network

  jenkins:
    image: jenkins/jenkins:lts
    ports:
      - "8081:8080"
      - "50000:50000"
    volumes:
      - jenkins-data:/var/jenkins_home
      - /var/run/docker.sock:/var/run/docker.sock
    networks:
      - pip-network

volumes:
  postgres-data:
  jenkins-data:

networks:
  pip-network:
    driver: bridge
COMPOSE_EOF

# Criar Kubernetes base deployment
cat > kubernetes/base/deployment.yaml << 'K8S_DEPLOY_EOF'
apiVersion: apps/v1
kind: Deployment
metadata:
  name: pip
  labels:
    app: pip
    version: v1
spec:
  replicas: 3
  selector:
    matchLabels:
      app: pip
  template:
    metadata:
      labels:
        app: pip
        version: v1
    spec:
      securityContext:
        runAsNonRoot: true
        runAsUser: 1000
        fsGroup: 1000
      containers:
      - name: pip
        image: ghcr.io/luizgustavovj/pip:latest
        ports:
        - containerPort: 8080
          protocol: TCP
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "prod"
        - name: JAVA_OPTS
          value: "-Xmx512m -Xms256m"
        resources:
          requests:
            memory: "512Mi"
            cpu: "250m"
          limits:
            memory: "1Gi"
            cpu: "1000m"
        livenessProbe:
          httpGet:
            path: /actuator/health/liveness
            port: 8080
          initialDelaySeconds: 60
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 5
        securityContext:
          allowPrivilegeEscalation: false
          readOnlyRootFilesystem: true
          runAsNonRoot: true
          capabilities:
            drop:
            - ALL
K8S_DEPLOY_EOF

# Criar Kubernetes service
cat > kubernetes/base/service.yaml << 'K8S_SVC_EOF'
apiVersion: v1
kind: Service
metadata:
  name: pip-service
  labels:
    app: pip
spec:
  type: ClusterIP
  ports:
  - port: 80
    targetPort: 8080
    protocol: TCP
    name: http
  selector:
    app: pip
K8S_SVC_EOF

# Criar Kubernetes kustomization base
cat > kubernetes/base/kustomization.yaml << 'K8S_KUST_EOF'
apiVersion: kustomize.config.k8s.io/v1beta1
kind: Kustomization

resources:
- deployment.yaml
- service.yaml
- ingress.yaml
- configmap.yaml
- secret.yaml

commonLabels:
  app: pip
  managed-by: kustomize
K8S_KUST_EOF

# Criar script de smoke tests
cat > scripts/deployment/smoke-tests.sh << 'SMOKE_EOF'
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
SMOKE_EOF

chmod +x scripts/deployment/smoke-tests.sh

# Criar script de validação PCI-DSS
cat > scripts/compliance/pci-dss-validation.sh << 'PCI_EOF'
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
PCI_EOF

chmod +x scripts/compliance/pci-dss-validation.sh

echo "✅ Infraestrutura completa criada!"
