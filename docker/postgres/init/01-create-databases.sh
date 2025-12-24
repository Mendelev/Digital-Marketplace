#!/bin/sh
set -e

create_user_and_db() {
  db="$1"
  user="$2"
  pass="$3"

  psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" <<-EOSQL
    DO $$
    BEGIN
      IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = '${user}') THEN
        CREATE ROLE ${user} LOGIN PASSWORD '${pass}';
      END IF;
    END
    $$;
EOSQL

  if [ "$(psql -tAc "SELECT 1 FROM pg_database WHERE datname='${db}'" --username "$POSTGRES_USER")" != "1" ]; then
    createdb --username "$POSTGRES_USER" --owner="${user}" "${db}"
  fi
}

create_user_and_db "auth_db" "auth_user" "auth_pass"
create_user_and_db "catalog_db" "catalog_user" "catalog_pass"
create_user_and_db "user_db" "user_user" "user_pass"
create_user_and_db "cart_db" "cart_user" "cart_pass"
create_user_and_db "order_db" "order_user" "order_pass"
create_user_and_db "payment_db" "payment_user" "payment_pass"
create_user_and_db "inventory_db" "inventory_user" "inventory_pass"
create_user_and_db "shipping_db" "shipping_user" "shipping_pass"
