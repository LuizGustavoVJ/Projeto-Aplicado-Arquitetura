terraform {
  required_version = ">= 1.0"
  
  required_providers {
    azurerm = {
      source  = "hashicorp/azurerm"
      version = "~> 3.0"
    }
  }
  
  backend "azurerm" {
    resource_group_name  = "pip-terraform-rg"
    storage_account_name = "piptfstate"
    container_name       = "tfstate"
    key                  = "prod.terraform.tfstate"
  }
}

provider "azurerm" {
  features {
    key_vault {
      purge_soft_delete_on_destroy = false
    }
    
    resource_group {
      prevent_deletion_if_contains_resources = true
    }
  }
}

# Resource Group
resource "azurerm_resource_group" "pip" {
  name     = var.resource_group_name
  location = var.location
  
  tags = var.tags
}

# Virtual Network
resource "azurerm_virtual_network" "pip" {
  name                = "${var.project_name}-vnet"
  address_space       = ["10.0.0.0/16"]
  location            = azurerm_resource_group.pip.location
  resource_group_name = azurerm_resource_group.pip.name
  
  tags = var.tags
}

# Subnets
resource "azurerm_subnet" "app" {
  name                 = "${var.project_name}-app-subnet"
  resource_group_name  = azurerm_resource_group.pip.name
  virtual_network_name = azurerm_virtual_network.pip.name
  address_prefixes     = ["10.0.1.0/24"]
  
  service_endpoints = ["Microsoft.Sql", "Microsoft.Storage", "Microsoft.KeyVault"]
}

resource "azurerm_subnet" "data" {
  name                 = "${var.project_name}-data-subnet"
  resource_group_name  = azurerm_resource_group.pip.name
  virtual_network_name = azurerm_virtual_network.pip.name
  address_prefixes     = ["10.0.2.0/24"]
  
  service_endpoints = ["Microsoft.Sql", "Microsoft.Storage"]
  
  delegation {
    name = "postgresql-delegation"
    
    service_delegation {
      name = "Microsoft.DBforPostgreSQL/flexibleServers"
      actions = [
        "Microsoft.Network/virtualNetworks/subnets/join/action",
      ]
    }
  }
}

# Network Security Group
resource "azurerm_network_security_group" "pip" {
  name                = "${var.project_name}-nsg"
  location            = azurerm_resource_group.pip.location
  resource_group_name = azurerm_resource_group.pip.name
  
  security_rule {
    name                       = "AllowHTTPS"
    priority                   = 100
    direction                  = "Inbound"
    access                     = "Allow"
    protocol                   = "Tcp"
    source_port_range          = "*"
    destination_port_range     = "443"
    source_address_prefix      = "*"
    destination_address_prefix = "*"
  }
  
  security_rule {
    name                       = "AllowHTTP"
    priority                   = 110
    direction                  = "Inbound"
    access                     = "Allow"
    protocol                   = "Tcp"
    source_port_range          = "*"
    destination_port_range     = "80"
    source_address_prefix      = "*"
    destination_address_prefix = "*"
  }
  
  tags = var.tags
}

# Associate NSG with App Subnet
resource "azurerm_subnet_network_security_group_association" "app" {
  subnet_id                 = azurerm_subnet.app.id
  network_security_group_id = azurerm_network_security_group.pip.id
}

# Storage Account for Backups
resource "azurerm_storage_account" "backups" {
  name                     = "${var.project_name}backups"
  resource_group_name      = azurerm_resource_group.pip.name
  location                 = azurerm_resource_group.pip.location
  account_tier             = "Standard"
  account_replication_type = "GRS"
  
  blob_properties {
    versioning_enabled = true
    
    delete_retention_policy {
      days = 30
    }
  }
  
  tags = var.tags
}

# Storage Container for Backups
resource "azurerm_storage_container" "backups" {
  name                  = "pip-backups"
  storage_account_name  = azurerm_storage_account.backups.name
  container_access_type = "private"
}

# Log Analytics Workspace
resource "azurerm_log_analytics_workspace" "pip" {
  name                = "${var.project_name}-logs"
  location            = azurerm_resource_group.pip.location
  resource_group_name = azurerm_resource_group.pip.name
  sku                 = "PerGB2018"
  retention_in_days   = 90
  
  tags = var.tags
}

# Application Insights
resource "azurerm_application_insights" "pip" {
  name                = "${var.project_name}-insights"
  location            = azurerm_resource_group.pip.location
  resource_group_name = azurerm_resource_group.pip.name
  workspace_id        = azurerm_log_analytics_workspace.pip.id
  application_type    = "web"
  
  tags = var.tags
}
