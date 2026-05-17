#!/bin/bash
set -e

# keycloak 전용 데이터베이스 생성
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "postgres" <<-EOSQL
    SELECT 'CREATE DATABASE keycloak'
    WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'keycloak')\gexec
EOSQL

# sparta_logistics DB에 서비스별 스키마 생성
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    CREATE SCHEMA IF NOT EXISTS user_db;
    CREATE SCHEMA IF NOT EXISTS hub_db;
    CREATE SCHEMA IF NOT EXISTS company_db;
    CREATE SCHEMA IF NOT EXISTS order_db;
    CREATE SCHEMA IF NOT EXISTS shipment_db;
    CREATE SCHEMA IF NOT EXISTS notification_db;
EOSQL

echo "PostgreSQL 초기화 완료: keycloak DB, 서비스별 스키마 생성됨"
