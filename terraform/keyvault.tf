# Get current client configuration
data "azurerm_client_config" "current" {}

# Azure Key Vault
resource "azurerm_key_vault" "pip" {
  name                        = "${var.project_name}-kv"
  location                    = azurerm_resource_group.pip.location
  resource_group_name         = azurerm_resource_group.pip.name
  enabled_for_disk_encryption = true
  tenant_id                   = data.azurerm_client_config.current.tenant_id
  soft_delete_retention_days  = 90
  purge_protection_enabled    = true
  sku_name                    = "premium"
  
  enable_rbac_authorization = false
  
  network_acls {
    bypass                     = "AzureServices"
    default_action             = "Deny"
    virtual_network_subnet_ids = [azurerm_subnet.app.id]
  }
  
  tags = var.tags
}

# Access Policy for Current User (Terraform)
resource "azurerm_key_vault_access_policy" "terraform" {
  key_vault_id = azurerm_key_vault.pip.id
  tenant_id    = data.azurerm_client_config.current.tenant_id
  object_id    = data.azurerm_client_config.current.object_id
  
  key_permissions = [
    "Get", "List", "Create", "Delete", "Update", "Recover", "Purge", "GetRotationPolicy", "SetRotationPolicy"
  ]
  
  secret_permissions = [
    "Get", "List", "Set", "Delete", "Recover", "Purge"
  ]
  
  certificate_permissions = [
    "Get", "List", "Create", "Delete", "Update", "Recover", "Purge"
  ]
}

# Access Policy for Application (Managed Identity)
resource "azurerm_key_vault_access_policy" "app" {
  key_vault_id = azurerm_key_vault.pip.id
  tenant_id    = data.azurerm_client_config.current.tenant_id
  object_id    = azurerm_user_assigned_identity.pip.principal_id
  
  key_permissions = [
    "Get", "List", "UnwrapKey", "WrapKey"
  ]
  
  secret_permissions = [
    "Get", "List"
  ]
}

# Managed Identity for Application
resource "azurerm_user_assigned_identity" "pip" {
  name                = "${var.project_name}-identity"
  location            = azurerm_resource_group.pip.location
  resource_group_name = azurerm_resource_group.pip.name
  
  tags = var.tags
}

# Key for Data Encryption
resource "azurerm_key_vault_key" "data_encryption" {
  name         = "data-encryption-key"
  key_vault_id = azurerm_key_vault.pip.id
  key_type     = "RSA"
  key_size     = 4096
  
  key_opts = [
    "decrypt",
    "encrypt",
    "sign",
    "unwrapKey",
    "verify",
    "wrapKey",
  ]
  
  rotation_policy {
    automatic {
      time_before_expiry = "P30D"
    }
    
    expire_after         = "P90D"
    notify_before_expiry = "P29D"
  }
  
  depends_on = [azurerm_key_vault_access_policy.terraform]
}

# Secrets
resource "azurerm_key_vault_secret" "db_connection_string" {
  name         = "database-connection-string"
  value        = "postgresql://${var.db_admin_username}:${var.db_admin_password}@${azurerm_postgresql_flexible_server.pip.fqdn}:5432/${var.db_name}?sslmode=require"
  key_vault_id = azurerm_key_vault.pip.id
  
  depends_on = [azurerm_key_vault_access_policy.terraform]
  
  tags = var.tags
}

resource "azurerm_key_vault_secret" "redis_connection_string" {
  name         = "redis-connection-string"
  value        = "${azurerm_redis_cache.pip.hostname}:${azurerm_redis_cache.pip.ssl_port},password=${azurerm_redis_cache.pip.primary_access_key},ssl=True,abortConnect=False"
  key_vault_id = azurerm_key_vault.pip.id
  
  depends_on = [azurerm_key_vault_access_policy.terraform]
  
  tags = var.tags
}

resource "azurerm_key_vault_secret" "storage_connection_string" {
  name         = "storage-connection-string"
  value        = azurerm_storage_account.backups.primary_connection_string
  key_vault_id = azurerm_key_vault.pip.id
  
  depends_on = [azurerm_key_vault_access_policy.terraform]
  
  tags = var.tags
}

# Diagnostic Settings for Key Vault
resource "azurerm_monitor_diagnostic_setting" "keyvault" {
  name                       = "${var.project_name}-kv-diag"
  target_resource_id         = azurerm_key_vault.pip.id
  log_analytics_workspace_id = azurerm_log_analytics_workspace.pip.id
  
  enabled_log {
    category = "AuditEvent"
  }
  
  metric {
    category = "AllMetrics"
  }
}
