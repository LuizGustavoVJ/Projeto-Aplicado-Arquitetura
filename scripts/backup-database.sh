#!/bin/bash

# Script de backup automatizado do PostgreSQL
# Executa backup completo, comprime e faz upload para Azure Blob Storage

set -e

echo "========================================="
echo "PIP - Database Backup Script"
echo "========================================="

# Configurações
DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-5432}"
DB_NAME="${DB_NAME:-pip_db}"
DB_USER="${DB_USER:-pip_user}"
BACKUP_DIR="${BACKUP_DIR:-/var/backups/pip}"
RETENTION_DAYS="${RETENTION_DAYS:-30}"
AZURE_STORAGE_ACCOUNT="${AZURE_STORAGE_ACCOUNT:-}"
AZURE_STORAGE_KEY="${AZURE_STORAGE_KEY:-}"
AZURE_CONTAINER="${AZURE_CONTAINER:-pip-backups}"
NOTIFICATION_EMAIL="${NOTIFICATION_EMAIL:-}"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
BACKUP_FILE="pip_backup_${TIMESTAMP}.sql"
COMPRESSED_FILE="${BACKUP_FILE}.gz"

# Criar diretório de backup se não existir
mkdir -p "$BACKUP_DIR"

echo "Starting backup at $(date)"
echo "Database: $DB_NAME"
echo "Host: $DB_HOST:$DB_PORT"
echo "Backup directory: $BACKUP_DIR"
echo ""

# Função para enviar notificação
send_notification() {
    local subject=$1
    local message=$2
    
    if [ -n "$NOTIFICATION_EMAIL" ]; then
        echo "$message" | mail -s "$subject" "$NOTIFICATION_EMAIL"
    fi
    
    # Log para syslog
    logger -t pip-backup "$subject: $message"
}

# Função para fazer backup
perform_backup() {
    echo "Creating database dump..."
    
    PGPASSWORD=$DB_PASSWORD pg_dump \
        -h $DB_HOST \
        -p $DB_PORT \
        -U $DB_USER \
        -d $DB_NAME \
        -F p \
        --no-owner \
        --no-acl \
        --clean \
        --if-exists \
        -f "$BACKUP_DIR/$BACKUP_FILE"
    
    if [ $? -ne 0 ]; then
        send_notification "PIP Backup FAILED" "Failed to create database dump"
        exit 1
    fi
    
    echo "Database dump created successfully"
}

# Função para comprimir backup
compress_backup() {
    echo "Compressing backup..."
    
    gzip -9 "$BACKUP_DIR/$BACKUP_FILE"
    
    if [ $? -ne 0 ]; then
        send_notification "PIP Backup FAILED" "Failed to compress backup"
        exit 1
    fi
    
    echo "Backup compressed successfully"
    
    # Mostrar tamanho do arquivo
    BACKUP_SIZE=$(du -h "$BACKUP_DIR/$COMPRESSED_FILE" | cut -f1)
    echo "Backup size: $BACKUP_SIZE"
}

# Função para fazer upload para Azure
upload_to_azure() {
    if [ -z "$AZURE_STORAGE_ACCOUNT" ] || [ -z "$AZURE_STORAGE_KEY" ]; then
        echo "Azure credentials not configured, skipping upload"
        return 0
    fi
    
    echo "Uploading backup to Azure Blob Storage..."
    
    # Verificar se az CLI está instalado
    if ! command -v az &> /dev/null; then
        echo "Azure CLI not installed, installing..."
        curl -sL https://aka.ms/InstallAzureCLIDeb | sudo bash
    fi
    
    # Fazer upload
    az storage blob upload \
        --account-name "$AZURE_STORAGE_ACCOUNT" \
        --account-key "$AZURE_STORAGE_KEY" \
        --container-name "$AZURE_CONTAINER" \
        --name "$COMPRESSED_FILE" \
        --file "$BACKUP_DIR/$COMPRESSED_FILE" \
        --overwrite
    
    if [ $? -ne 0 ]; then
        send_notification "PIP Backup WARNING" "Backup created but upload to Azure failed"
        return 1
    fi
    
    echo "Backup uploaded to Azure successfully"
}

# Função para limpar backups antigos
cleanup_old_backups() {
    echo "Cleaning up backups older than $RETENTION_DAYS days..."
    
    # Limpar backups locais
    find "$BACKUP_DIR" -name "pip_backup_*.sql.gz" -type f -mtime +$RETENTION_DAYS -delete
    
    LOCAL_COUNT=$(find "$BACKUP_DIR" -name "pip_backup_*.sql.gz" -type f | wc -l)
    echo "Local backups remaining: $LOCAL_COUNT"
    
    # Limpar backups no Azure
    if [ -n "$AZURE_STORAGE_ACCOUNT" ] && [ -n "$AZURE_STORAGE_KEY" ]; then
        CUTOFF_DATE=$(date -d "$RETENTION_DAYS days ago" +%Y%m%d)
        
        az storage blob list \
            --account-name "$AZURE_STORAGE_ACCOUNT" \
            --account-key "$AZURE_STORAGE_KEY" \
            --container-name "$AZURE_CONTAINER" \
            --prefix "pip_backup_" \
            --query "[?properties.creationTime < '$CUTOFF_DATE'].name" \
            -o tsv | while read blob; do
                az storage blob delete \
                    --account-name "$AZURE_STORAGE_ACCOUNT" \
                    --account-key "$AZURE_STORAGE_KEY" \
                    --container-name "$AZURE_CONTAINER" \
                    --name "$blob"
                echo "Deleted old backup: $blob"
            done
    fi
}

# Função para verificar integridade do backup
verify_backup() {
    echo "Verifying backup integrity..."
    
    # Descomprimir para teste
    gunzip -t "$BACKUP_DIR/$COMPRESSED_FILE"
    
    if [ $? -ne 0 ]; then
        send_notification "PIP Backup FAILED" "Backup file is corrupted"
        exit 1
    fi
    
    echo "Backup integrity verified"
}

# Executar backup
echo ""
echo "Step 1/5: Creating backup..."
perform_backup

echo ""
echo "Step 2/5: Compressing backup..."
compress_backup

echo ""
echo "Step 3/5: Verifying backup..."
verify_backup

echo ""
echo "Step 4/5: Uploading to Azure..."
upload_to_azure

echo ""
echo "Step 5/5: Cleaning up old backups..."
cleanup_old_backups

# Calcular duração
END_TIME=$(date +%s)
DURATION=$((END_TIME - $(date -d "$(echo $TIMESTAMP | sed 's/_/ /')" +%s)))

echo ""
echo "========================================="
echo "Backup completed successfully!"
echo "========================================="
echo "Backup file: $COMPRESSED_FILE"
echo "Location: $BACKUP_DIR/$COMPRESSED_FILE"
echo "Duration: ${DURATION}s"
echo "Timestamp: $(date)"
echo ""

# Enviar notificação de sucesso
send_notification "PIP Backup SUCCESS" "Backup completed successfully. File: $COMPRESSED_FILE, Size: $BACKUP_SIZE, Duration: ${DURATION}s"

exit 0
