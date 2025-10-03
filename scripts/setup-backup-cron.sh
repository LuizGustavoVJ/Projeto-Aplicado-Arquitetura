#!/bin/bash

# Script para configurar cron job de backup automatizado
# Executa backup diariamente às 2h AM

set -e

echo "========================================="
echo "PIP - Setup Backup Cron Job"
echo "========================================="

# Caminho do script de backup
BACKUP_SCRIPT="/opt/pip/scripts/backup-database.sh"
LOG_FILE="/var/log/pip/backup.log"

# Criar diretório de logs se não existir
sudo mkdir -p /var/log/pip
sudo chown ubuntu:ubuntu /var/log/pip

# Criar entrada do cron
CRON_ENTRY="0 2 * * * $BACKUP_SCRIPT >> $LOG_FILE 2>&1"

# Verificar se já existe
if crontab -l 2>/dev/null | grep -q "$BACKUP_SCRIPT"; then
    echo "Cron job already exists"
    echo "Current cron jobs:"
    crontab -l | grep "$BACKUP_SCRIPT"
else
    # Adicionar ao crontab
    (crontab -l 2>/dev/null; echo "$CRON_ENTRY") | crontab -
    echo "Cron job added successfully"
    echo "Schedule: Daily at 2:00 AM"
    echo "Log file: $LOG_FILE"
fi

echo ""
echo "Current crontab:"
crontab -l

echo ""
echo "========================================="
echo "Setup completed!"
echo "========================================="
echo ""
echo "To test the backup script manually:"
echo "  sudo $BACKUP_SCRIPT"
echo ""
echo "To view backup logs:"
echo "  tail -f $LOG_FILE"
echo ""
echo "To remove the cron job:"
echo "  crontab -e"
echo ""
