# PostgreSQL Flexible Server
resource "azurerm_postgresql_flexible_server" "pip" {
  name                   = "${var.project_name}-postgres"
  resource_group_name    = azurerm_resource_group.pip.name
  location               = azurerm_resource_group.pip.location
  version                = "15"
  delegated_subnet_id    = azurerm_subnet.data.id
  private_dns_zone_id    = azurerm_private_dns_zone.postgres.id
  administrator_login    = var.db_admin_username
  administrator_password = var.db_admin_password
  zone                   = "1"
  
  storage_mb = 32768
  
  sku_name   = var.db_sku_name
  
  backup_retention_days        = 30
  geo_redundant_backup_enabled = true
  
  high_availability {
    mode                      = "ZoneRedundant"
    standby_availability_zone = "2"
  }
  
  maintenance_window {
    day_of_week  = 0
    start_hour   = 2
    start_minute = 0
  }
  
  depends_on = [azurerm_private_dns_zone_virtual_network_link.postgres]
  
  tags = var.tags
}

# PostgreSQL Database
resource "azurerm_postgresql_flexible_server_database" "pip" {
  name      = var.db_name
  server_id = azurerm_postgresql_flexible_server.pip.id
  collation = "en_US.utf8"
  charset   = "utf8"
}

# PostgreSQL Configuration
resource "azurerm_postgresql_flexible_server_configuration" "max_connections" {
  name      = "max_connections"
  server_id = azurerm_postgresql_flexible_server.pip.id
  value     = "200"
}

resource "azurerm_postgresql_flexible_server_configuration" "shared_buffers" {
  name      = "shared_buffers"
  server_id = azurerm_postgresql_flexible_server.pip.id
  value     = "262144"
}

resource "azurerm_postgresql_flexible_server_configuration" "effective_cache_size" {
  name      = "effective_cache_size"
  server_id = azurerm_postgresql_flexible_server.pip.id
  value     = "786432"
}

resource "azurerm_postgresql_flexible_server_configuration" "maintenance_work_mem" {
  name      = "maintenance_work_mem"
  server_id = azurerm_postgresql_flexible_server.pip.id
  value     = "131072"
}

resource "azurerm_postgresql_flexible_server_configuration" "checkpoint_completion_target" {
  name      = "checkpoint_completion_target"
  server_id = azurerm_postgresql_flexible_server.pip.id
  value     = "0.9"
}

resource "azurerm_postgresql_flexible_server_configuration" "wal_buffers" {
  name      = "wal_buffers"
  server_id = azurerm_postgresql_flexible_server.pip.id
  value     = "2048"
}

resource "azurerm_postgresql_flexible_server_configuration" "default_statistics_target" {
  name      = "default_statistics_target"
  server_id = azurerm_postgresql_flexible_server.pip.id
  value     = "100"
}

resource "azurerm_postgresql_flexible_server_configuration" "random_page_cost" {
  name      = "random_page_cost"
  server_id = azurerm_postgresql_flexible_server.pip.id
  value     = "1.1"
}

resource "azurerm_postgresql_flexible_server_configuration" "effective_io_concurrency" {
  name      = "effective_io_concurrency"
  server_id = azurerm_postgresql_flexible_server.pip.id
  value     = "200"
}

resource "azurerm_postgresql_flexible_server_configuration" "work_mem" {
  name      = "work_mem"
  server_id = azurerm_postgresql_flexible_server.pip.id
  value     = "5242"
}

# Private DNS Zone for PostgreSQL
resource "azurerm_private_dns_zone" "postgres" {
  name                = "privatelink.postgres.database.azure.com"
  resource_group_name = azurerm_resource_group.pip.name
  
  tags = var.tags
}

# Link Private DNS Zone to VNet
resource "azurerm_private_dns_zone_virtual_network_link" "postgres" {
  name                  = "${var.project_name}-postgres-vnet-link"
  private_dns_zone_name = azurerm_private_dns_zone.postgres.name
  virtual_network_id    = azurerm_virtual_network.pip.id
  resource_group_name   = azurerm_resource_group.pip.name
  
  tags = var.tags
}

# Read Replica (Optional - for high read workloads)
resource "azurerm_postgresql_flexible_server" "pip_replica" {
  count = var.enable_read_replica ? 1 : 0
  
  name                = "${var.project_name}-postgres-replica"
  resource_group_name = azurerm_resource_group.pip.name
  location            = azurerm_resource_group.pip.location
  
  create_mode         = "Replica"
  source_server_id    = azurerm_postgresql_flexible_server.pip.id
  
  tags = var.tags
}
