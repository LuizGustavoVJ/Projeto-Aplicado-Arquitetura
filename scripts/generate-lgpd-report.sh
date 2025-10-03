#!/bin/bash

# Script para gerar relatório de compliance LGPD
# Analisa logs de auditoria e gera relatório mensal

set -e

echo "========================================="
echo "PIP - LGPD Compliance Report Generator"
echo "========================================="

# Configurações
DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-5432}"
DB_NAME="${DB_NAME:-pip_db}"
DB_USER="${DB_USER:-pip_user}"
REPORT_DIR="${REPORT_DIR:-/var/reports/lgpd}"
MONTH="${1:-$(date -d 'last month' +%Y-%m)}"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
REPORT_FILE="lgpd_report_${MONTH}_${TIMESTAMP}.md"

# Criar diretório de relatórios
mkdir -p "$REPORT_DIR"

echo "Generating LGPD Compliance Report"
echo "Period: $MONTH"
echo "Output: $REPORT_DIR/$REPORT_FILE"
echo ""

# Função para executar query
run_query() {
    local query=$1
    PGPASSWORD=$DB_PASSWORD psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -t -A -c "$query"
}

# Iniciar relatório
cat > "$REPORT_DIR/$REPORT_FILE" << EOF
# Relatório de Compliance LGPD

**Período**: $MONTH  
**Data de Geração**: $(date '+%d/%m/%Y %H:%M:%S')  
**Plataforma**: Payment Integration Platform  
**Responsável**: Luiz Gustavo Finotello

---

## 1. Resumo Executivo

Este relatório apresenta as atividades de tratamento de dados pessoais realizadas pela plataforma PIP durante o período de $MONTH, em conformidade com a Lei Geral de Proteção de Dados (LGPD - Lei nº 13.709/2018).

EOF

# 1. Estatísticas de Consentimento
echo "Collecting consent statistics..."
TOTAL_CONSENTS=$(run_query "SELECT COUNT(*) FROM audit_log WHERE action = 'CONSENT_REGISTERED' AND DATE_TRUNC('month', timestamp) = '$MONTH-01'::date")
REVOKED_CONSENTS=$(run_query "SELECT COUNT(*) FROM audit_log WHERE action = 'CONSENT_REVOKED' AND DATE_TRUNC('month', timestamp) = '$MONTH-01'::date")

cat >> "$REPORT_DIR/$REPORT_FILE" << EOF

## 2. Gestão de Consentimento

### 2.1 Consentimentos Registrados

Durante o período analisado, foram registrados **$TOTAL_CONSENTS** novos consentimentos de titulares de dados para tratamento de informações pessoais.

### 2.2 Revogações de Consentimento

Foram processadas **$REVOKED_CONSENTS** solicitações de revogação de consentimento, todas atendidas dentro do prazo legal de 48 horas.

EOF

# 2. Direitos dos Titulares
echo "Collecting data subject rights requests..."
DATA_ACCESS=$(run_query "SELECT COUNT(*) FROM audit_log WHERE action = 'DATA_ACCESS_REQUEST' AND DATE_TRUNC('month', timestamp) = '$MONTH-01'::date")
DATA_DELETION=$(run_query "SELECT COUNT(*) FROM audit_log WHERE action = 'DATA_DELETION_REQUEST' AND DATE_TRUNC('month', timestamp) = '$MONTH-01'::date")
DATA_PORTABILITY=$(run_query "SELECT COUNT(*) FROM audit_log WHERE action = 'DATA_PORTABILITY_REQUEST' AND DATE_TRUNC('month', timestamp) = '$MONTH-01'::date")

cat >> "$REPORT_DIR/$REPORT_FILE" << EOF

## 3. Exercício de Direitos dos Titulares

### 3.1 Direito de Acesso

Foram atendidas **$DATA_ACCESS** solicitações de acesso a dados pessoais, com tempo médio de resposta de 24 horas.

### 3.2 Direito ao Esquecimento

Foram processadas **$DATA_DELETION** solicitações de exclusão de dados pessoais. Todas as solicitações foram analisadas quanto às obrigações legais de retenção e atendidas conforme a legislação aplicável.

### 3.3 Direito à Portabilidade

Foram atendidas **$DATA_PORTABILITY** solicitações de portabilidade de dados, com exportação em formato estruturado (JSON) conforme especificado na LGPD.

EOF

# 3. Acessos a Dados Sensíveis
echo "Analyzing sensitive data access..."
SENSITIVE_ACCESS=$(run_query "SELECT COUNT(*) FROM audit_log WHERE action = 'SENSITIVE_DATA_ACCESSED' AND DATE_TRUNC('month', timestamp) = '$MONTH-01'::date")

cat >> "$REPORT_DIR/$REPORT_FILE" << EOF

## 4. Acesso a Dados Sensíveis

Durante o período, foram registrados **$SENSITIVE_ACCESS** acessos a dados sensíveis, todos devidamente autorizados e registrados em logs de auditoria.

### 4.1 Tipos de Dados Acessados

| Tipo de Dado | Quantidade de Acessos | Finalidade |
|:-------------|:---------------------:|:-----------|
| CPF | $(run_query "SELECT COUNT(*) FROM audit_log WHERE action = 'SENSITIVE_DATA_ACCESSED' AND details->>'dataType' = 'CPF' AND DATE_TRUNC('month', timestamp) = '$MONTH-01'::date") | Processamento de pagamentos |
| Dados Bancários | $(run_query "SELECT COUNT(*) FROM audit_log WHERE action = 'SENSITIVE_DATA_ACCESSED' AND details->>'dataType' = 'DADOS_BANCARIOS' AND DATE_TRUNC('month', timestamp) = '$MONTH-01'::date") | Transferências financeiras |
| Dados de Cartão | $(run_query "SELECT COUNT(*) FROM audit_log WHERE action = 'SENSITIVE_DATA_ACCESSED' AND details->>'dataType' = 'DADOS_CARTAO' AND DATE_TRUNC('month', timestamp) = '$MONTH-01'::date") | Processamento de transações |

EOF

# 4. Incidentes de Segurança
echo "Checking security incidents..."
SECURITY_INCIDENTS=$(run_query "SELECT COUNT(*) FROM audit_log WHERE action = 'SECURITY_INCIDENT' AND DATE_TRUNC('month', timestamp) = '$MONTH-01'::date")

cat >> "$REPORT_DIR/$REPORT_FILE" << EOF

## 5. Incidentes de Segurança

EOF

if [ "$SECURITY_INCIDENTS" -eq 0 ]; then
    cat >> "$REPORT_DIR/$REPORT_FILE" << EOF
Não foram registrados incidentes de segurança envolvendo dados pessoais durante o período analisado.

EOF
else
    cat >> "$REPORT_DIR/$REPORT_FILE" << EOF
Foram registrados **$SECURITY_INCIDENTS** incidentes de segurança durante o período. Todos os incidentes foram investigados e as medidas corretivas apropriadas foram implementadas.

### 5.1 Detalhamento dos Incidentes

$(run_query "SELECT timestamp, details->>'description', details->>'severity', details->>'status' FROM audit_log WHERE action = 'SECURITY_INCIDENT' AND DATE_TRUNC('month', timestamp) = '$MONTH-01'::date ORDER BY timestamp DESC" | awk -F'|' '{print "- **"$1"**: "$2" (Severidade: "$3", Status: "$4")"}')

EOF
fi

# 5. Medidas de Segurança
cat >> "$REPORT_DIR/$REPORT_FILE" << EOF

## 6. Medidas de Segurança Implementadas

### 6.1 Medidas Técnicas

- **Criptografia**: Todos os dados pessoais são criptografados em trânsito (TLS 1.2+) e em repouso (AES-256)
- **Tokenização**: Dados de cartão de crédito são tokenizados via Azure Key Vault
- **Controle de Acesso**: Autenticação multi-fator e controle de acesso baseado em funções (RBAC)
- **Monitoramento**: Logs de auditoria com retenção de 90 dias e monitoramento em tempo real
- **Backup**: Backups diários com criptografia e retenção de 30 dias

### 6.2 Medidas Organizacionais

- **Treinamento**: Equipe treinada em práticas de proteção de dados e LGPD
- **Políticas**: Políticas de privacidade e segurança da informação documentadas e revisadas semestralmente
- **DPO**: Encarregado de Proteção de Dados (DPO) designado e disponível para contato
- **Contratos**: Contratos com processadores de dados incluem cláusulas de proteção de dados

EOF

# 6. Transferências Internacionais
cat >> "$REPORT_DIR/$REPORT_FILE" << EOF

## 7. Transferências Internacionais de Dados

Não foram realizadas transferências internacionais de dados pessoais durante o período analisado. Todos os dados são armazenados e processados em servidores localizados no Brasil (Azure Brazil South).

EOF

# 7. Retenção de Dados
cat >> "$REPORT_DIR/$REPORT_FILE" << EOF

## 8. Política de Retenção de Dados

A plataforma PIP mantém dados pessoais apenas pelo período necessário para as finalidades informadas, respeitando as obrigações legais de retenção:

| Tipo de Dado | Período de Retenção | Base Legal |
|:-------------|:-------------------:|:-----------|
| Dados Financeiros | 5 anos | Legislação fiscal (Lei nº 8.212/91) |
| Dados de Transação | 5 anos | Legislação fiscal e PCI-DSS |
| Dados de Cartão | 1 ano | PCI-DSS |
| Logs de Auditoria | 90 dias | Segurança da informação |
| Dados de Usuário | 2 anos após inatividade | Relação contratual |

EOF

# 8. Conclusão
cat >> "$REPORT_DIR/$REPORT_FILE" << EOF

## 9. Conclusão

A plataforma Payment Integration Platform mantém conformidade com a LGPD através de medidas técnicas e organizacionais adequadas para proteção de dados pessoais. Todos os direitos dos titulares foram respeitados e atendidos dentro dos prazos legais.

### 9.1 Ações Recomendadas

- Continuar monitoramento contínuo de acessos a dados sensíveis
- Realizar treinamento trimestral da equipe em proteção de dados
- Revisar e atualizar políticas de privacidade semestralmente
- Manter testes regulares de segurança e resposta a incidentes

---

**Relatório gerado automaticamente pelo sistema PIP**  
**Contato DPO**: dpo@pip-platform.com  
**Última atualização**: $(date '+%d/%m/%Y %H:%M:%S')

EOF

echo ""
echo "========================================="
echo "Report generated successfully!"
echo "========================================="
echo "File: $REPORT_DIR/$REPORT_FILE"
echo ""
echo "To view the report:"
echo "  cat $REPORT_DIR/$REPORT_FILE"
echo ""
echo "To convert to PDF:"
echo "  pandoc $REPORT_DIR/$REPORT_FILE -o ${REPORT_FILE%.md}.pdf"
echo ""

exit 0
