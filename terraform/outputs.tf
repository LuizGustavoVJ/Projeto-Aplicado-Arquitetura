output "resource_group_name" {
  description = "Nome do Resource Group"
  value       = azurerm_resource_group.pip.name
}

output "resource_group_location" {
  description = "Localização do Resource Group"
  value       = azurerm_resource_group.pip.location
}

# Database Outputs
output "postgresql_fqdn" {
  description = "FQDN do PostgreSQL"
  value       = azurerm_postgresql_flexible_server.pip.fqdn
}

output "postgresql_database_name" {
  description = "Nome do banco de dados"
  value       = azurerm_postgresql_flexible_server_database.pip.name
}

output "postgresql_connection_string" {
  description = "Connection string do PostgreSQL (sensível)"
  value       = "postgresql://${var.db_admin_username}:${var.db_admin_password}@${azurerm_postgresql_flexible_server.pip.fqdn}:5432/${var.db_name}?sslmode=require"
  sensitive   = true
}

# Redis Outputs
output "redis_hostname" {
  description = "Hostname do Redis"
  value       = azurerm_redis_cache.pip.hostname
}

output "redis_ssl_port" {
  description = "Porta SSL do Redis"
  value       = azurerm_redis_cache.pip.ssl_port
}

output "redis_primary_access_key" {
  description = "Chave primária do Redis (sensível)"
  value       = azurerm_redis_cache.pip.primary_access_key
  sensitive   = true
}

output "redis_connection_string" {
  description = "Connection string do Redis (sensível)"
  value       = "${azurerm_redis_cache.pip.hostname}:${azurerm_redis_cache.pip.ssl_port},password=${azurerm_redis_cache.pip.primary_access_key},ssl=True,abortConnect=False"
  sensitive   = true
}

# Key Vault Outputs
output "key_vault_name" {
  description = "Nome do Key Vault"
  value       = azurerm_key_vault.pip.name
}

output "key_vault_uri" {
  description = "URI do Key Vault"
  value       = azurerm_key_vault.pip.vault_uri
}

output "managed_identity_client_id" {
  description = "Client ID da Managed Identity"
  value       = azurerm_user_assigned_identity.pip.client_id
}

output "managed_identity_principal_id" {
  description = "Principal ID da Managed Identity"
  value       = azurerm_user_assigned_identity.pip.principal_id
}

# App Service Outputs
output "staging_url" {
  description = "URL do ambiente de staging"
  value       = "https://${azurerm_linux_web_app.staging.default_hostname}"
}

output "production_url" {
  description = "URL do ambiente de produção"
  value       = "https://${azurerm_linux_web_app.production.default_hostname}"
}

output "production_blue_slot_url" {
  description = "URL do slot blue (produção)"
  value       = "https://${azurerm_linux_web_app_slot.production_blue.default_hostname}"
}

# Storage Outputs
output "storage_account_name" {
  description = "Nome da Storage Account para backups"
  value       = azurerm_storage_account.backups.name
}

output "storage_account_primary_access_key" {
  description = "Chave primária da Storage Account (sensível)"
  value       = azurerm_storage_account.backups.primary_access_key
  sensitive   = true
}

output "backup_container_name" {
  description = "Nome do container de backups"
  value       = azurerm_storage_container.backups.name
}

# Monitoring Outputs
output "log_analytics_workspace_id" {
  description = "ID do Log Analytics Workspace"
  value       = azurerm_log_analytics_workspace.pip.id
}

output "application_insights_instrumentation_key" {
  description = "Instrumentation Key do Application Insights (sensível)"
  value       = azurerm_application_insights.pip.instrumentation_key
  sensitive   = true
}

output "application_insights_connection_string" {
  description = "Connection String do Application Insights (sensível)"
  value       = azurerm_application_insights.pip.connection_string
  sensitive   = true
}
