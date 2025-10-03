pipeline {
    agent any
    
    environment {
        // Configurações gerais
        DOCKER_REGISTRY = 'ghcr.io'
        DOCKER_IMAGE = "${DOCKER_REGISTRY}/luizgustavovj/pip"
        K8S_NAMESPACE_DEV = 'pip-dev'
        K8S_NAMESPACE_STAGING = 'pip-staging'
        K8S_NAMESPACE_PROD = 'pip-prod'
        
        // Credenciais (armazenadas no Jenkins Credentials)
        GITHUB_TOKEN = credentials('github-token')
        AZURE_CREDENTIALS = credentials('azure-sp')
        SONARQUBE_TOKEN = credentials('sonarqube-token')
        
        // Configurações de segurança
        PCI_DSS_COMPLIANCE = 'true'
        SECURITY_SCAN_ENABLED = 'true'
        AUDIT_ENABLED = 'true'
    }
    
    options {
        buildDiscarder(logRotator(numToKeepStr: '30'))
        timeout(time: 1, unit: 'HOURS')
        disableConcurrentBuilds()
        timestamps()
    }
    
    stages {
        // CASO DE USO 1: CHECKOUT E VALIDAÇÃO
        stage('Checkout & Validation') {
            steps {
                script {
                    echo '🔍 [CASO DE USO 1] Checkout e Validação Inicial'
                    checkout scm
                    
                    sh 'test -f pom.xml || exit 1'
                    sh 'test -d src/main/java || exit 1'
                    
                    env.PROJECT_VERSION = sh(
                        script: "mvn help:evaluate -Dexpression=project.version -q -DforceStdout",
                        returnStdout: true
                    ).trim()
                    
                    echo "📦 Versão: ${env.PROJECT_VERSION}"
                }
            }
        }
        
        // CASO DE USO 2: BUILD
        stage('Build') {
            steps {
                echo '🔨 [CASO DE USO 2] Build e Compilação'
                sh 'mvn clean compile -DskipTests'
            }
        }
        
        // CASO DE USO 3: TESTES UNITÁRIOS
        stage('Unit Tests') {
            steps {
                echo '🧪 [CASO DE USO 3] Testes Unitários'
                sh 'mvn test -Dtest=!*IntegrationTest,!*StressTest'
            }
            post {
                always {
                    junit '**/target/surefire-reports/*.xml'
                }
            }
        }
        
        // CASO DE USO 4: ANÁLISE DE CÓDIGO
        stage('Code Quality') {
            steps {
                echo '📊 [CASO DE USO 4] Análise de Qualidade'
                withSonarQubeEnv('SonarQube') {
                    sh 'mvn sonar:sonar -Dsonar.projectKey=pip'
                }
            }
        }
        
        // CASO DE USO 5: SECURITY SCANNING
        stage('Security Scan') {
            parallel {
                stage('OWASP') {
                    steps {
                        echo '🔒 [CASO DE USO 5.1] OWASP Dependency Check'
                        sh 'mvn org.owasp:dependency-check-maven:check'
                    }
                }
                stage('Trivy') {
                    steps {
                        echo '🔒 [CASO DE USO 5.2] Trivy Scan'
                        sh 'trivy image ${DOCKER_IMAGE}:latest || true'
                    }
                }
            }
        }
        
        // CASO DE USO 6: COMPLIANCE PCI-DSS
        stage('PCI-DSS Compliance') {
            steps {
                echo '🛡️ [CASO DE USO 6] Validação PCI-DSS'
                sh 'bash scripts/compliance/pci-dss-validation.sh'
            }
        }
        
        // CASO DE USO 7: TESTES DE INTEGRAÇÃO
        stage('Integration Tests') {
            steps {
                echo '🔗 [CASO DE USO 7] Testes de Integração'
                sh 'mvn test -Dtest=*IntegrationTest'
            }
        }
        
        // CASO DE USO 8: TESTES DE PERFORMANCE
        stage('Performance Tests') {
            steps {
                echo '💪 [CASO DE USO 8] Testes de Performance'
                sh 'mvn gatling:test || true'
            }
        }
        
        // CASO DE USO 9: BUILD DOCKER
        stage('Build Docker') {
            steps {
                echo '🐳 [CASO DE USO 9] Build Docker Image'
                sh """
                    docker build \\
                        -t ${DOCKER_IMAGE}:${PROJECT_VERSION} \\
                        -t ${DOCKER_IMAGE}:latest \\
                        -f docker/Dockerfile .
                """
            }
        }
        
        // CASO DE USO 10: PUSH DOCKER
        stage('Push Docker') {
            steps {
                echo '📤 [CASO DE USO 10] Push Docker Image'
                sh """
                    echo ${GITHUB_TOKEN} | docker login ${DOCKER_REGISTRY} -u github --password-stdin
                    docker push ${DOCKER_IMAGE}:${PROJECT_VERSION}
                    docker push ${DOCKER_IMAGE}:latest
                """
            }
        }
        
        // CASO DE USO 11: DEPLOY DEV
        stage('Deploy DEV') {
            steps {
                echo '🚀 [CASO DE USO 11] Deploy para DEV'
                sh """
                    kubectl apply -k kubernetes/overlays/dev
                    kubectl set image deployment/pip pip=${DOCKER_IMAGE}:${PROJECT_VERSION} -n ${K8S_NAMESPACE_DEV}
                    kubectl rollout status deployment/pip -n ${K8S_NAMESPACE_DEV}
                """
            }
        }
        
        // CASO DE USO 12: SMOKE TESTS DEV
        stage('Smoke Tests DEV') {
            steps {
                echo '💨 [CASO DE USO 12] Smoke Tests DEV'
                sh 'bash scripts/deployment/smoke-tests.sh ${K8S_NAMESPACE_DEV}'
            }
        }
        
        // CASO DE USO 13: DEPLOY STAGING
        stage('Deploy STAGING') {
            steps {
                echo '🚀 [CASO DE USO 13] Deploy para STAGING'
                sh """
                    kubectl apply -k kubernetes/overlays/staging
                    kubectl set image deployment/pip pip=${DOCKER_IMAGE}:${PROJECT_VERSION} -n ${K8S_NAMESPACE_STAGING}
                    kubectl rollout status deployment/pip -n ${K8S_NAMESPACE_STAGING}
                """
            }
        }
        
        // CASO DE USO 14: SMOKE TESTS STAGING
        stage('Smoke Tests STAGING') {
            steps {
                echo '💨 [CASO DE USO 14] Smoke Tests STAGING'
                sh 'bash scripts/deployment/smoke-tests.sh ${K8S_NAMESPACE_STAGING}'
            }
        }
        
        // CASO DE USO 15: APPROVAL PRODUÇÃO
        stage('Approval PROD') {
            steps {
                echo '✋ [CASO DE USO 15] Aprovação para PRODUÇÃO'
                timeout(time: 24, unit: 'HOURS') {
                    input(
                        message: 'Aprovar deploy para PRODUÇÃO?',
                        submitter: 'admin,tech-lead'
                    )
                }
            }
        }
        
        // CASO DE USO 16: CANARY DEPLOYMENT
        stage('Canary PROD') {
            steps {
                echo '🐤 [CASO DE USO 16] Canary Deployment'
                sh 'bash scripts/deployment/canary-deploy.sh ${DOCKER_IMAGE}:${PROJECT_VERSION} 10'
                sleep(time: 5, unit: 'MINUTES')
                sh 'bash scripts/deployment/validate-canary.sh'
            }
        }
        
        // CASO DE USO 17: FULL DEPLOYMENT PROD
        stage('Full Deploy PROD') {
            steps {
                echo '🚀 [CASO DE USO 17] Deploy Completo PROD'
                sh """
                    kubectl apply -k kubernetes/overlays/prod
                    kubectl set image deployment/pip pip=${DOCKER_IMAGE}:${PROJECT_VERSION} -n ${K8S_NAMESPACE_PROD}
                    kubectl rollout status deployment/pip -n ${K8S_NAMESPACE_PROD}
                """
            }
        }
        
        // CASO DE USO 18: POST-DEPLOY TESTS
        stage('Post-Deploy Tests') {
            steps {
                echo '✅ [CASO DE USO 18] Testes Pós-Deploy'
                sh 'bash scripts/deployment/post-deploy-tests.sh ${K8S_NAMESPACE_PROD}'
            }
        }
        
        // CASO DE USO 19: HEALTH CHECKS
        stage('Health Checks') {
            steps {
                echo '❤️ [CASO DE USO 19] Health Checks'
                sh 'bash scripts/deployment/health-checks.sh ${K8S_NAMESPACE_PROD}'
            }
        }
        
        // CASO DE USO 20: AUDIT REPORT
        stage('Audit Report') {
            steps {
                echo '📝 [CASO DE USO 20] Relatório de Auditoria'
                sh 'bash scripts/compliance/generate-audit-report.sh'
            }
            post {
                always {
                    archiveArtifacts artifacts: 'audit.log, target/compliance-reports/**/*'
                }
            }
        }
    }
    
    post {
        success {
            echo '✅ Pipeline concluído com sucesso!'
        }
        failure {
            echo '❌ Pipeline falhou! Iniciando rollback...'
            sh 'kubectl rollout undo deployment/pip -n ${K8S_NAMESPACE_PROD} || true'
        }
        always {
            cleanWs()
        }
    }
}
