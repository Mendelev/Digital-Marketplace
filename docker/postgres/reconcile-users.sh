#!/bin/sh
set -e

POSTGRES_CONTAINER="${POSTGRES_CONTAINER:-marketplace-postgres}"
ADMIN_USER="${ADMIN_USER:-marketplace_admin}"

run_psql() {
  docker exec -i "$POSTGRES_CONTAINER" psql -U "$ADMIN_USER" -d postgres "$@"
}

set_role_password() {
  role="$1"
  pass="$2"
  run_psql <<-EOSQL
DO \$\$
BEGIN
  IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = '${role}') THEN
    CREATE ROLE ${role} LOGIN PASSWORD '${pass}';
  ELSE
    ALTER ROLE ${role} WITH PASSWORD '${pass}';
  END IF;
END
\$\$;
EOSQL
}

ensure_db() {
  db="$1"
  owner="$2"
  exists="$(run_psql -tAc "SELECT 1 FROM pg_database WHERE datname='${db}'")"
  if [ "$exists" != "1" ]; then
    docker exec -i "$POSTGRES_CONTAINER" createdb -U "$ADMIN_USER" -O "$owner" "$db"
  fi
}

set_role_password "auth_user" "auth_pass"
set_role_password "catalog_user" "catalog_pass"
set_role_password "user_user" "user_pass"
set_role_password "cart_user" "cart_pass"
set_role_password "order_user" "order_pass"
set_role_password "payment_user" "payment_pass"
set_role_password "inventory_user" "inventory_pass"

ensure_db "auth_db" "auth_user"
ensure_db "catalog_db" "catalog_user"
ensure_db "user_db" "user_user"
ensure_db "cart_db" "cart_user"
ensure_db "order_db" "order_user"
ensure_db "payment_db" "payment_user"
ensure_db "inventory_db" "inventory_user"
