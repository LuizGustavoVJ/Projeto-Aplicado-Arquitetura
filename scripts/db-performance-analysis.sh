#!/bin/bash

# Script para análise de performance do PostgreSQL
# Identifica queries lentas, índices não utilizados e oportunidades de otimização

set -e

echo "========================================="
echo "PIP - Database Performance Analysis"
echo "========================================="

# Configurações
DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-5432}"
DB_NAME="${DB_NAME:-pip_db}"
DB_USER="${DB_USER:-pip_user}"
REPORT_DIR="./db-reports"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)

# Criar diretório de relatórios
mkdir -p "$REPORT_DIR"

echo "Database: $DB_NAME"
echo "Host: $DB_HOST:$DB_PORT"
echo "Report Directory: $REPORT_DIR"
echo ""

# Função para executar query e salvar resultado
run_query() {
    local query=$1
    local output_file=$2
    
    PGPASSWORD=$DB_PASSWORD psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME \
        -c "$query" -o "$output_file"
}

# 1. Queries mais lentas
echo "Analyzing slow queries..."
run_query "
SELECT 
    query,
    calls,
    total_exec_time,
    mean_exec_time,
    max_exec_time,
    stddev_exec_time
FROM pg_stat_statements
ORDER BY mean_exec_time DESC
LIMIT 20;
" "$REPORT_DIR/slow-queries-${TIMESTAMP}.txt"

# 2. Índices não utilizados
echo "Checking for unused indexes..."
run_query "
SELECT
    schemaname,
    tablename,
    indexname,
    idx_scan,
    idx_tup_read,
    idx_tup_fetch,
    pg_size_pretty(pg_relation_size(indexrelid)) as index_size
FROM pg_stat_user_indexes
WHERE idx_scan = 0
    AND indexrelname NOT LIKE 'pg_toast%'
ORDER BY pg_relation_size(indexrelid) DESC;
" "$REPORT_DIR/unused-indexes-${TIMESTAMP}.txt"

# 3. Tabelas com mais inserts/updates/deletes
echo "Analyzing table activity..."
run_query "
SELECT
    schemaname,
    tablename,
    n_tup_ins as inserts,
    n_tup_upd as updates,
    n_tup_del as deletes,
    n_live_tup as live_tuples,
    n_dead_tup as dead_tuples,
    last_vacuum,
    last_autovacuum,
    last_analyze,
    last_autoanalyze
FROM pg_stat_user_tables
ORDER BY (n_tup_ins + n_tup_upd + n_tup_del) DESC
LIMIT 20;
" "$REPORT_DIR/table-activity-${TIMESTAMP}.txt"

# 4. Tamanho das tabelas e índices
echo "Analyzing table and index sizes..."
run_query "
SELECT
    schemaname,
    tablename,
    pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) as total_size,
    pg_size_pretty(pg_relation_size(schemaname||'.'||tablename)) as table_size,
    pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename) - pg_relation_size(schemaname||'.'||tablename)) as indexes_size
FROM pg_tables
WHERE schemaname NOT IN ('pg_catalog', 'information_schema')
ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;
" "$REPORT_DIR/table-sizes-${TIMESTAMP}.txt"

# 5. Cache hit ratio
echo "Checking cache hit ratio..."
run_query "
SELECT
    'cache hit rate' as metric,
    sum(heap_blks_hit) / (sum(heap_blks_hit) + sum(heap_blks_read)) as ratio
FROM pg_statio_user_tables;
" "$REPORT_DIR/cache-hit-ratio-${TIMESTAMP}.txt"

# 6. Conexões ativas
echo "Analyzing active connections..."
run_query "
SELECT
    datname,
    usename,
    application_name,
    client_addr,
    state,
    query_start,
    state_change,
    wait_event_type,
    wait_event,
    query
FROM pg_stat_activity
WHERE datname = '$DB_NAME'
ORDER BY query_start;
" "$REPORT_DIR/active-connections-${TIMESTAMP}.txt"

# 7. Bloat em tabelas
echo "Checking for table bloat..."
run_query "
SELECT
    schemaname,
    tablename,
    pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) as size,
    n_dead_tup,
    n_live_tup,
    round(n_dead_tup * 100.0 / NULLIF(n_live_tup + n_dead_tup, 0), 2) as dead_tuple_percent
FROM pg_stat_user_tables
WHERE n_dead_tup > 1000
ORDER BY n_dead_tup DESC;
" "$REPORT_DIR/table-bloat-${TIMESTAMP}.txt"

# 8. Locks ativos
echo "Checking for locks..."
run_query "
SELECT
    locktype,
    database,
    relation::regclass,
    page,
    tuple,
    virtualxid,
    transactionid,
    mode,
    granted
FROM pg_locks
WHERE NOT granted
ORDER BY relation;
" "$REPORT_DIR/locks-${TIMESTAMP}.txt"

echo ""
echo "========================================="
echo "Analysis completed!"
echo "========================================="
echo "Reports saved to: $REPORT_DIR"
echo ""
echo "Key Metrics:"
echo "- Slow queries: $REPORT_DIR/slow-queries-${TIMESTAMP}.txt"
echo "- Unused indexes: $REPORT_DIR/unused-indexes-${TIMESTAMP}.txt"
echo "- Cache hit ratio: $REPORT_DIR/cache-hit-ratio-${TIMESTAMP}.txt"
echo ""
echo "Recommendations:"
echo "1. Review slow queries and optimize them"
echo "2. Consider removing unused indexes"
echo "3. Run VACUUM ANALYZE on tables with high dead tuple count"
echo "4. Monitor cache hit ratio (should be > 0.95)"
echo ""
