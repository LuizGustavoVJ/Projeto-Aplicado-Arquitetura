#!/bin/bash
set -e

echo "Initializing Payment Integration Platform database..."

# Create extensions
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    -- Enable required extensions
    CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
    CREATE EXTENSION IF NOT EXISTS "pg_stat_statements";
    CREATE EXTENSION IF NOT EXISTS "pgcrypto";
    
    -- Create audit schema
    CREATE SCHEMA IF NOT EXISTS audit;
    
    -- Create audit function
    CREATE OR REPLACE FUNCTION audit.log_changes()
    RETURNS TRIGGER AS \$\$
    BEGIN
        IF TG_OP = 'INSERT' THEN
            INSERT INTO audit.audit_log (table_name, operation, new_data, changed_at, changed_by)
            VALUES (TG_TABLE_NAME, TG_OP, row_to_json(NEW), NOW(), current_user);
            RETURN NEW;
        ELSIF TG_OP = 'UPDATE' THEN
            INSERT INTO audit.audit_log (table_name, operation, old_data, new_data, changed_at, changed_by)
            VALUES (TG_TABLE_NAME, TG_OP, row_to_json(OLD), row_to_json(NEW), NOW(), current_user);
            RETURN NEW;
        ELSIF TG_OP = 'DELETE' THEN
            INSERT INTO audit.audit_log (table_name, operation, old_data, changed_at, changed_by)
            VALUES (TG_TABLE_NAME, TG_OP, row_to_json(OLD), NOW(), current_user);
            RETURN OLD;
        END IF;
    END;
    \$\$ LANGUAGE plpgsql;
    
    -- Create audit log table
    CREATE TABLE IF NOT EXISTS audit.audit_log (
        id BIGSERIAL PRIMARY KEY,
        table_name VARCHAR(255) NOT NULL,
        operation VARCHAR(10) NOT NULL,
        old_data JSONB,
        new_data JSONB,
        changed_at TIMESTAMP NOT NULL DEFAULT NOW(),
        changed_by VARCHAR(255) NOT NULL
    );
    
    -- Create indexes for audit log
    CREATE INDEX IF NOT EXISTS idx_audit_log_table_name ON audit.audit_log(table_name);
    CREATE INDEX IF NOT EXISTS idx_audit_log_changed_at ON audit.audit_log(changed_at);
    CREATE INDEX IF NOT EXISTS idx_audit_log_operation ON audit.audit_log(operation);
    
    -- Grant permissions
    GRANT USAGE ON SCHEMA audit TO $POSTGRES_USER;
    GRANT SELECT, INSERT ON audit.audit_log TO $POSTGRES_USER;
    GRANT USAGE, SELECT ON SEQUENCE audit.audit_log_id_seq TO $POSTGRES_USER;
EOSQL

echo "Database initialization completed successfully!"
