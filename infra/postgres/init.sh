#!/bin/bash
set -e

# keycloak ?„ìš© ?°ì´?°ë² ?´ìŠ¤ ?ì„±
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "postgres" <<-EOSQL
    SELECT 'CREATE DATABASE keycloak'
    WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'keycloak')\gexec
EOSQL

# sparta_logistics DB???œë¹„?¤ë³„ ?¤í‚¤ë§??ì„±
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    CREATE SCHEMA IF NOT EXISTS user_db;
    CREATE SCHEMA IF NOT EXISTS hub_db;
    CREATE SCHEMA IF NOT EXISTS company_db;
    CREATE SCHEMA IF NOT EXISTS order_db;
    CREATE SCHEMA IF NOT EXISTS shipment_db;
    CREATE SCHEMA IF NOT EXISTS notification_db;
EOSQL

echo "PostgreSQL ì´ˆê¸°???„ë£Œ: keycloak DB, ?œë¹„?¤ë³„ ?¤í‚¤ë§??ì„±??
