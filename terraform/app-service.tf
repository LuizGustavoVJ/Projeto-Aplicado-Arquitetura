# App Service Plan
resource "azurerm_service_plan" "pip" {
  name                = "${var.project_name}-asp"
  resource_group_name = azurerm_resource_group.pip.name
  location            = azurerm_resource_group.pip.location
  os_type             = "Linux"
  sku_name            = var.app_service_sku
  
  tags = var.tags
}

# App Service (Staging)
resource "azurerm_linux_web_app" "staging" {
  name                = "${var.project_name}-staging"
  resource_group_name = azurerm_resource_group.pip.name
  location            = azurerm_service_plan.pip.location
  service_plan_id     = azurerm_service_plan.pip.id
  
  https_only = true
  
  site_config {
    always_on = true
    
    application_stack {
      docker_image_name   = "${var.docker_registry}/${var.docker_image}:latest"
      docker_registry_url = "https://${var.docker_registry}"
    }
    
    health_check_path = "/actuator/health"
    
    cors {
      allowed_origins = ["*"]
    }
  }
  
  app_settings = {
    "WEBSITES_PORT"                      = "8080"
    "SPRING_PROFILES_ACTIVE"             = "prod"
    "SPRING_DATASOURCE_URL"              = "@Microsoft.KeyVault(SecretUri=${azurerm_key_vault_secret.db_connection_string.id})"
    "SPRING_REDIS_HOST"                  = azurerm_redis_cache.pip.hostname
    "SPRING_REDIS_PORT"                  = azurerm_redis_cache.pip.ssl_port
    "SPRING_REDIS_PASSWORD"              = "@Microsoft.KeyVault(SecretUri=${azurerm_key_vault_secret.redis_connection_string.id})"
    "AZURE_KEYVAULT_URI"                 = azurerm_key_vault.pip.vault_uri
    "APPLICATIONINSIGHTS_CONNECTION_STRING" = azurerm_application_insights.pip.connection_string
  }
  
  identity {
    type         = "UserAssigned"
    identity_ids = [azurerm_user_assigned_identity.pip.id]
  }
  
  logs {
    application_logs {
      file_system_level = "Information"
    }
    
    http_logs {
      file_system {
        retention_in_days = 7
        retention_in_mb   = 35
      }
    }
  }
  
  tags = var.tags
}

# App Service (Production)
resource "azurerm_linux_web_app" "production" {
  name                = "${var.project_name}-prod"
  resource_group_name = azurerm_resource_group.pip.name
  location            = azurerm_service_plan.pip.location
  service_plan_id     = azurerm_service_plan.pip.id
  
  https_only = true
  
  site_config {
    always_on = true
    
    application_stack {
      docker_image_name   = "${var.docker_registry}/${var.docker_image}:stable"
      docker_registry_url = "https://${var.docker_registry}"
    }
    
    health_check_path = "/actuator/health"
    
    cors {
      allowed_origins = var.allowed_origins
    }
  }
  
  app_settings = {
    "WEBSITES_PORT"                      = "8080"
    "SPRING_PROFILES_ACTIVE"             = "prod"
    "SPRING_DATASOURCE_URL"              = "@Microsoft.KeyVault(SecretUri=${azurerm_key_vault_secret.db_connection_string.id})"
    "SPRING_REDIS_HOST"                  = azurerm_redis_cache.pip.hostname
    "SPRING_REDIS_PORT"                  = azurerm_redis_cache.pip.ssl_port
    "SPRING_REDIS_PASSWORD"              = "@Microsoft.KeyVault(SecretUri=${azurerm_key_vault_secret.redis_connection_string.id})"
    "AZURE_KEYVAULT_URI"                 = azurerm_key_vault.pip.vault_uri
    "APPLICATIONINSIGHTS_CONNECTION_STRING" = azurerm_application_insights.pip.connection_string
  }
  
  identity {
    type         = "UserAssigned"
    identity_ids = [azurerm_user_assigned_identity.pip.id]
  }
  
  logs {
    application_logs {
      file_system_level = "Warning"
    }
    
    http_logs {
      file_system {
        retention_in_days = 30
        retention_in_mb   = 100
      }
    }
  }
  
  tags = var.tags
}

# Deployment Slot for Blue/Green (Production only)
resource "azurerm_linux_web_app_slot" "production_blue" {
  name           = "blue"
  app_service_id = azurerm_linux_web_app.production.id
  
  https_only = true
  
  site_config {
    always_on = true
    
    application_stack {
      docker_image_name   = "${var.docker_registry}/${var.docker_image}:stable"
      docker_registry_url = "https://${var.docker_registry}"
    }
    
    health_check_path = "/actuator/health"
  }
  
  app_settings = azurerm_linux_web_app.production.app_settings
  
  identity {
    type         = "UserAssigned"
    identity_ids = [azurerm_user_assigned_identity.pip.id]
  }
  
  tags = var.tags
}

# Custom Domain (Optional)
resource "azurerm_app_service_custom_hostname_binding" "staging" {
  count = var.staging_custom_domain != "" ? 1 : 0
  
  hostname            = var.staging_custom_domain
  app_service_name    = azurerm_linux_web_app.staging.name
  resource_group_name = azurerm_resource_group.pip.name
}

resource "azurerm_app_service_custom_hostname_binding" "production" {
  count = var.production_custom_domain != "" ? 1 : 0
  
  hostname            = var.production_custom_domain
  app_service_name    = azurerm_linux_web_app.production.name
  resource_group_name = azurerm_resource_group.pip.name
}

# Diagnostic Settings
resource "azurerm_monitor_diagnostic_setting" "staging" {
  name                       = "${var.project_name}-staging-diag"
  target_resource_id         = azurerm_linux_web_app.staging.id
  log_analytics_workspace_id = azurerm_log_analytics_workspace.pip.id
  
  enabled_log {
    category = "AppServiceHTTPLogs"
  }
  
  enabled_log {
    category = "AppServiceConsoleLogs"
  }
  
  enabled_log {
    category = "AppServiceAppLogs"
  }
  
  metric {
    category = "AllMetrics"
  }
}

resource "azurerm_monitor_diagnostic_setting" "production" {
  name                       = "${var.project_name}-prod-diag"
  target_resource_id         = azurerm_linux_web_app.production.id
  log_analytics_workspace_id = azurerm_log_analytics_workspace.pip.id
  
  enabled_log {
    category = "AppServiceHTTPLogs"
  }
  
  enabled_log {
    category = "AppServiceConsoleLogs"
  }
  
  enabled_log {
    category = "AppServiceAppLogs"
  }
  
  metric {
    category = "AllMetrics"
  }
}
