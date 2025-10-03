# Terraform - Payment Integration Platform

Este diretório contém a infraestrutura como código (IaC) para provisionar todos os recursos necessários do Payment Integration Platform no Azure.

## Recursos Provisionados

### Rede
- Virtual Network com subnets segregadas (app e data)
- Network Security Group com regras de firewall
- Private DNS Zones para PostgreSQL e Redis

### Banco de Dados
- Azure Database for PostgreSQL Flexible Server
- Alta disponibilidade com Zone Redundancy
- Backup automático com retenção de 30 dias
- Configurações otimizadas de performance
- Réplica de leitura (opcional)

### Cache
- Azure Cache for Redis
- Suporte para Basic, Standard e Premium tiers
- Clustering e persistência (Premium)
- Private Endpoint (Premium)

### Segurança
- Azure Key Vault com Purge Protection
- Managed Identity para acesso sem credenciais
- Chaves de criptografia com rotação automática
- Network isolation com Private Endpoints

### Aplicação
- App Service Plan Linux
- App Service para Staging
- App Service para Produção
- Deployment Slot para Blue/Green deployment
- Integração com Key Vault e Application Insights

### Monitoramento
- Log Analytics Workspace
- Application Insights
- Diagnostic Settings para todos os recursos

### Armazenamento
- Storage Account para backups
- Geo-redundância (GRS)
- Versionamento de blobs
- Retenção de 30 dias

## Pré-requisitos

1. **Azure CLI** instalado e autenticado
   ```bash
   az login
   az account set --subscription <subscription-id>
   ```

2. **Terraform** instalado (versão >= 1.0)
   ```bash
   # Linux/macOS
   wget https://releases.hashicorp.com/terraform/1.6.0/terraform_1.6.0_linux_amd64.zip
   unzip terraform_1.6.0_linux_amd64.zip
   sudo mv terraform /usr/local/bin/
   
   # Verificar instalação
   terraform version
   ```

3. **Conta Azure** com permissões de Owner ou Contributor

## Configuração Inicial

### 1. Criar Backend do Terraform

O Terraform precisa de um backend para armazenar o state file. Execute os comandos abaixo uma única vez:

```bash
# Criar Resource Group para o backend
az group create --name pip-terraform-rg --location brazilsouth

# Criar Storage Account
az storage account create \
  --name piptfstate \
  --resource-group pip-terraform-rg \
  --location brazilsouth \
  --sku Standard_LRS \
  --encryption-services blob

# Criar Container
az storage container create \
  --name tfstate \
  --account-name piptfstate
```

### 2. Configurar Variáveis

```bash
# Copiar arquivo de exemplo
cp terraform.tfvars.example terraform.tfvars

# Editar com seus valores
nano terraform.tfvars
```

**Importante**: Nunca commite o arquivo `terraform.tfvars` pois ele contém credenciais sensíveis.

### 3. Inicializar Terraform

```bash
cd terraform
terraform init
```

## Uso

### Planejar Mudanças

```bash
terraform plan
```

Este comando mostra quais recursos serão criados, modificados ou destruídos.

### Aplicar Mudanças

```bash
terraform apply
```

Revise as mudanças e digite `yes` para confirmar.

### Destruir Recursos

```bash
terraform destroy
```

**ATENÇÃO**: Este comando remove TODOS os recursos. Use com cuidado!

## Outputs

Após o `terraform apply`, você receberá os seguintes outputs:

- **PostgreSQL FQDN**: Endereço do banco de dados
- **Redis Hostname**: Endereço do Redis
- **Key Vault URI**: URI do Key Vault
- **Staging URL**: URL do ambiente de staging
- **Production URL**: URL do ambiente de produção
- **Connection Strings**: Strings de conexão (sensíveis)

Para ver os outputs novamente:

```bash
terraform output
terraform output -json  # Formato JSON
```

Para ver outputs sensíveis:

```bash
terraform output postgresql_connection_string
terraform output redis_primary_access_key
```

## Configuração de Ambientes

### Desenvolvimento (Custo Reduzido)

```hcl
db_sku_name       = "B_Standard_B1ms"  # 1 vCore, 2GB RAM
redis_sku_name    = "Basic"
redis_capacity    = 0
app_service_sku   = "B1"
enable_read_replica = false
```

**Custo estimado**: ~R$ 500/mês

### Produção (Recomendado)

```hcl
db_sku_name       = "GP_Standard_D2s_v3"  # 2 vCores, 8GB RAM
redis_sku_name    = "Standard"
redis_capacity    = 1
app_service_sku   = "P1v2"
enable_read_replica = false
```

**Custo estimado**: ~R$ 2.000/mês

### Produção (Alta Disponibilidade)

```hcl
db_sku_name       = "GP_Standard_D4s_v3"  # 4 vCores, 16GB RAM
redis_sku_name    = "Premium"
redis_capacity    = 1
redis_shard_count = 3
app_service_sku   = "P2v2"
enable_read_replica = true
```

**Custo estimado**: ~R$ 5.000/mês

## Configurar GitHub Actions

Após provisionar a infraestrutura, configure os seguintes secrets no GitHub:

```bash
# Obter Service Principal (criar se não existir)
az ad sp create-for-rbac \
  --name "pip-github-actions" \
  --role contributor \
  --scopes /subscriptions/<subscription-id>/resourceGroups/pip-rg \
  --sdk-auth

# Adicionar no GitHub Secrets:
# AZURE_CREDENTIALS = <output do comando acima>

# Outros secrets necessários:
terraform output -json | jq -r '.postgresql_connection_string.value'
terraform output -json | jq -r '.redis_connection_string.value'
terraform output -json | jq -r '.key_vault_uri.value'
```

## Manutenção

### Atualizar Recursos

1. Modificar os arquivos `.tf` ou `terraform.tfvars`
2. Executar `terraform plan` para revisar mudanças
3. Executar `terraform apply` para aplicar

### Backup do State

O state file é armazenado no Azure Storage e possui versionamento habilitado. Para fazer backup manual:

```bash
terraform state pull > terraform.tfstate.backup
```

### Importar Recursos Existentes

Se você já tem recursos no Azure e quer gerenciá-los com Terraform:

```bash
terraform import azurerm_resource_group.pip /subscriptions/<subscription-id>/resourceGroups/pip-rg
```

## Troubleshooting

### Erro: Backend initialization required

```bash
terraform init -reconfigure
```

### Erro: Insufficient permissions

Verifique se você tem permissões de Owner ou Contributor no Resource Group.

### Erro: Resource already exists

Use `terraform import` para importar o recurso existente ou altere o nome no código.

### Erro: State lock

Se um apply foi interrompido, o state pode ficar travado:

```bash
terraform force-unlock <lock-id>
```

## Segurança

- **Nunca commite** `terraform.tfvars` ou `*.tfstate`
- **Use** Managed Identity ao invés de credenciais hardcoded
- **Habilite** Private Endpoints para recursos críticos
- **Configure** Network Security Groups adequadamente
- **Rotacione** chaves e senhas regularmente

## Suporte

Para dúvidas ou problemas:
- Email: finotello22@hotmail.com
- GitHub Issues: https://github.com/LuizGustavoVJ/Payment-Integration-Platform/issues

---

**Autor**: Luiz Gustavo Finotello  
**Última Atualização**: Outubro 2025
