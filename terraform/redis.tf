# Azure Cache for Redis
resource "azurerm_redis_cache" "pip" {
  name                = "${var.project_name}-redis"
  location            = azurerm_resource_group.pip.location
  resource_group_name = azurerm_resource_group.pip.name
  capacity            = var.redis_capacity
  family              = var.redis_family
  sku_name            = var.redis_sku_name
  enable_non_ssl_port = false
  minimum_tls_version = "1.2"
  
  redis_configuration {
    enable_authentication           = true
    maxmemory_reserved              = 50
    maxmemory_delta                 = 50
    maxmemory_policy                = "allkeys-lru"
    maxfragmentationmemory_reserved = 50
    
    # Persistence (Premium only)
    rdb_backup_enabled            = var.redis_sku_name == "Premium" ? true : false
    rdb_backup_frequency          = var.redis_sku_name == "Premium" ? 60 : null
    rdb_backup_max_snapshot_count = var.redis_sku_name == "Premium" ? 1 : null
    rdb_storage_connection_string = var.redis_sku_name == "Premium" ? azurerm_storage_account.backups.primary_blob_connection_string : null
  }
  
  # Clustering (Premium only)
  shard_count = var.redis_sku_name == "Premium" ? var.redis_shard_count : null
  
  # Zones (Premium only)
  zones = var.redis_sku_name == "Premium" ? ["1", "2", "3"] : null
  
  # Patch Schedule
  patch_schedule {
    day_of_week    = "Sunday"
    start_hour_utc = 2
  }
  
  tags = var.tags
}

# Redis Firewall Rule - Allow Azure Services
resource "azurerm_redis_firewall_rule" "azure_services" {
  name                = "AllowAzureServices"
  redis_cache_name    = azurerm_redis_cache.pip.name
  resource_group_name = azurerm_resource_group.pip.name
  start_ip            = "0.0.0.0"
  end_ip              = "0.0.0.0"
}

# Private Endpoint for Redis (Premium only)
resource "azurerm_private_endpoint" "redis" {
  count = var.redis_sku_name == "Premium" ? 1 : 0
  
  name                = "${var.project_name}-redis-pe"
  location            = azurerm_resource_group.pip.location
  resource_group_name = azurerm_resource_group.pip.name
  subnet_id           = azurerm_subnet.data.id
  
  private_service_connection {
    name                           = "${var.project_name}-redis-psc"
    private_connection_resource_id = azurerm_redis_cache.pip.id
    is_manual_connection           = false
    subresource_names              = ["redisCache"]
  }
  
  private_dns_zone_group {
    name                 = "redis-dns-zone-group"
    private_dns_zone_ids = [azurerm_private_dns_zone.redis[0].id]
  }
  
  tags = var.tags
}

# Private DNS Zone for Redis (Premium only)
resource "azurerm_private_dns_zone" "redis" {
  count = var.redis_sku_name == "Premium" ? 1 : 0
  
  name                = "privatelink.redis.cache.windows.net"
  resource_group_name = azurerm_resource_group.pip.name
  
  tags = var.tags
}

# Link Private DNS Zone to VNet (Premium only)
resource "azurerm_private_dns_zone_virtual_network_link" "redis" {
  count = var.redis_sku_name == "Premium" ? 1 : 0
  
  name                  = "${var.project_name}-redis-vnet-link"
  private_dns_zone_name = azurerm_private_dns_zone.redis[0].name
  virtual_network_id    = azurerm_virtual_network.pip.id
  resource_group_name   = azurerm_resource_group.pip.name
  
  tags = var.tags
}
