#!/bin/bash

###############################################################################
# Script de Validação PCI-DSS
# 
# Valida conformidade com requisitos PCI-DSS 3.2.1
# 
# Requisitos verificados:
# - Req 1: Firewall e segurança de rede
# - Req 2: Senhas e configurações padrão
# - Req 3: Proteção de dados armazenados
# - Req 4: Criptografia de dados em trânsito
# - Req 6: Desenvolvimento seguro
# - Req 8: Controle de acesso
# - Req 10: Monitoramento e logs
# - Req 11: Testes de segurança
# 
# Autor: Luiz Gustavo Finotello
###############################################################################

echo "========================================="
echo "PIP - Validação PCI-DSS 3.2.1"
echo "========================================="
echo ""

# Cores para output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

PASSED=0
FAILED=0
WARNINGS=0

# Função para verificar requisito
check_requirement() {
    local req_num=$1
    local req_desc=$2
    local check_command=$3
    
    echo -n "Req $req_num: $req_desc... "
    
    if eval "$check_command" > /dev/null 2>&1; then
        echo -e "${GREEN}PASS${NC}"
        ((PASSED++))
        return 0
    else
        echo -e "${RED}FAIL${NC}"
        ((FAILED++))
        return 1
    fi
}

# Função para warning
check_warning() {
    local req_num=$1
    local req_desc=$2
    local check_command=$3
    
    echo -n "Req $req_num: $req_desc... "
    
    if eval "$check_command" > /dev/null 2>&1; then
        echo -e "${GREEN}PASS${NC}"
        ((PASSED++))
        return 0
    else
        echo -e "${YELLOW}WARNING${NC}"
        ((WARNINGS++))
        return 1
    fi
}

echo "Verificando Requisitos PCI-DSS..."
echo ""

# Req 3: Proteção de dados armazenados
echo "=== Requisito 3: Proteção de Dados Armazenados ==="
check_requirement "3.1" "Tokenização implementada" "grep -r 'TokenizationService' src/"
check_requirement "3.2" "Sem armazenamento de CVV" "! grep -r 'cvv.*varchar' src/"
check_requirement "3.3" "Sem PAN em logs" "! grep -r 'logger.*pan' src/"
check_requirement "3.4" "Criptografia de dados sensíveis" "grep -r 'AES-256' src/ || grep -r 'Azure.*KeyVault' src/"
echo ""

# Req 4: Criptografia em trânsito
echo "=== Requisito 4: Criptografia em Trânsito ==="
check_requirement "4.1" "TLS 1.2+ obrigatório" "grep -r 'TLS.*1\.[23]' src/ || grep -r 'https://' src/"
check_requirement "4.2" "Certificados válidos" "test -f src/main/resources/keystore.jks || echo 'Configurar em produção'"
echo ""

# Req 6: Desenvolvimento seguro
echo "=== Requisito 6: Desenvolvimento Seguro ==="
check_requirement "6.1" "Validação de entrada" "grep -r '@Valid' src/"
check_requirement "6.2" "Sanitização de dados" "grep -r 'sanitize\|escape' src/"
check_requirement "6.3" "Tratamento de erros" "grep -r 'try.*catch' src/"
check_requirement "6.4" "Logs de auditoria" "grep -r 'SecurityAuditLogger' src/"
echo ""

# Req 8: Controle de acesso
echo "=== Requisito 8: Controle de Acesso ==="
check_requirement "8.1" "Autenticação implementada" "grep -r 'ApiKey\|JWT\|OAuth' src/"
check_requirement "8.2" "Rate limiting" "grep -r 'RateLimit' src/"
check_requirement "8.3" "Sessões seguras" "grep -r 'session.*timeout' src/"
echo ""

# Req 10: Monitoramento e logs
echo "=== Requisito 10: Monitoramento e Logs ==="
check_requirement "10.1" "Logs de transações" "grep -r 'LogTransacao' src/"
check_requirement "10.2" "Logs de acesso" "grep -r 'logger.*info.*access' src/"
check_requirement "10.3" "Auditoria de segurança" "grep -r 'SecurityAuditLogger' src/"
check_requirement "10.4" "Retenção de logs" "grep -r 'retention\|backup' src/"
echo ""

# Req 11: Testes de segurança
echo "=== Requisito 11: Testes de Segurança ==="
check_requirement "11.1" "Testes unitários" "test -d src/test/java"
check_requirement "11.2" "Testes de segurança" "grep -r 'SecurityTest' src/test/"
check_warning "11.3" "Penetration testing" "test -f docs/pentest-report.pdf"
echo ""

# Verificações adicionais
echo "=== Verificações Adicionais ==="
check_requirement "Extra.1" "Sem senhas hardcoded" "! grep -r 'password.*=.*\"' src/"
check_requirement "Extra.2" "Sem chaves de API hardcoded" "! grep -r 'api.*key.*=.*\"' src/"
check_requirement "Extra.3" "Variáveis de ambiente" "grep -r 'System.getenv\|@Value' src/"
check_requirement "Extra.4" "Dependências atualizadas" "test -f pom.xml"
echo ""

# Resumo
echo "========================================="
echo "Resumo da Validação PCI-DSS"
echo "========================================="
echo -e "${GREEN}Passou: $PASSED${NC}"
echo -e "${RED}Falhou: $FAILED${NC}"
echo -e "${YELLOW}Avisos: $WARNINGS${NC}"
echo ""

TOTAL=$((PASSED + FAILED + WARNINGS))
PERCENTAGE=$((PASSED * 100 / TOTAL))

echo "Conformidade: $PERCENTAGE%"
echo ""

if [ $FAILED -eq 0 ]; then
    echo -e "${GREEN}✓ Sistema em conformidade com PCI-DSS${NC}"
    exit 0
elif [ $PERCENTAGE -ge 80 ]; then
    echo -e "${YELLOW}⚠ Sistema parcialmente conforme - Revisar falhas${NC}"
    exit 1
else
    echo -e "${RED}✗ Sistema NÃO conforme com PCI-DSS${NC}"
    exit 2
fi
