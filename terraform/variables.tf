variable "project_name" {
  description = "Nome do projeto (usado como prefixo para recursos)"
  type        = string
  default     = "pip"
}

variable "resource_group_name" {
  description = "Nome do Resource Group"
  type        = string
  default     = "pip-rg"
}

variable "location" {
  description = "Localização dos recursos no Azure"
  type        = string
  default     = "brazilsouth"
}

variable "tags" {
  description = "Tags para todos os recursos"
  type        = map(string)
  default = {
    Environment = "Production"
    Project     = "Payment Integration Platform"
    ManagedBy   = "Terraform"
  }
}

# Database Variables
variable "db_admin_username" {
  description = "Username do administrador do PostgreSQL"
  type        = string
  sensitive   = true
}

variable "db_admin_password" {
  description = "Senha do administrador do PostgreSQL"
  type        = string
  sensitive   = true
}

variable "db_name" {
  description = "Nome do banco de dados"
  type        = string
  default     = "pip_db"
}

variable "db_sku_name" {
  description = "SKU do PostgreSQL (GP_Standard_D2s_v3, GP_Standard_D4s_v3, etc)"
  type        = string
  default     = "GP_Standard_D2s_v3"
}

variable "enable_read_replica" {
  description = "Habilitar réplica de leitura do PostgreSQL"
  type        = bool
  default     = false
}

# Redis Variables
variable "redis_capacity" {
  description = "Capacidade do Redis (0-6 para Basic/Standard, 1-5 para Premium)"
  type        = number
  default     = 1
}

variable "redis_family" {
  description = "Família do Redis (C para Basic/Standard, P para Premium)"
  type        = string
  default     = "C"
}

variable "redis_sku_name" {
  description = "SKU do Redis (Basic, Standard, Premium)"
  type        = string
  default     = "Standard"
}

variable "redis_shard_count" {
  description = "Número de shards para Redis Premium"
  type        = number
  default     = 3
}

# App Service Variables
variable "app_service_sku" {
  description = "SKU do App Service Plan (B1, B2, B3, S1, S2, S3, P1v2, P2v2, P3v2)"
  type        = string
  default     = "P1v2"
}

variable "docker_registry" {
  description = "Registry do Docker (ex: docker.io, ghcr.io)"
  type        = string
  default     = "docker.io"
}

variable "docker_image" {
  description = "Nome da imagem Docker"
  type        = string
  default     = "luizgustavovj/payment-integration-platform"
}

variable "allowed_origins" {
  description = "Origens permitidas para CORS (produção)"
  type        = list(string)
  default     = ["https://app.pip-platform.com"]
}

variable "staging_custom_domain" {
  description = "Domínio customizado para staging (opcional)"
  type        = string
  default     = ""
}

variable "production_custom_domain" {
  description = "Domínio customizado para produção (opcional)"
  type        = string
  default     = ""
}
