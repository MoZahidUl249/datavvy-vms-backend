#!/bin/bash
# Runs the VMS API on http://localhost:8080 with demo data (Swagger: /swagger-ui.html).
# Database: MySQL on localhost:3306 if one is running (or Docker is available),
# otherwise a local H2 file database (~/.vms/vmsdb, MySQL mode).
# Credentials come from .env (created with random values on first run).
# Log: backend-run.log. Close this window (or Ctrl+C) to stop.
source "$(dirname "$0")/tools/_env.sh" || { read -n1 -p "Press any key to close"; exit 1; }

PROFILES=local
if command -v docker >/dev/null 2>&1 && docker info >/dev/null 2>&1; then
  echo "== Starting MySQL in Docker"
  docker compose up -d mysql
  for i in $(seq 1 30); do docker exec vms-mysql mysqladmin ping -h localhost -u"$VMS_DB_USER" -p"$VMS_DB_PASSWORD" >/dev/null 2>&1 && break; sleep 2; done
elif (exec 3<>/dev/tcp/127.0.0.1/3306) 2>/dev/null; then
  echo "== Using MySQL already running on localhost:3306 (database and user from .env)"
else
  echo "== No MySQL found — using the local H2 database (~/.vms/vmsdb, MySQL mode)"
  PROFILES=local,h2
fi

echo "== Building modules"
"$MVN" -B -q install -DskipTests || { echo "!! build failed"; read -n1; exit 1; }
echo "== Starting the API (profiles: $PROFILES) — log: backend-run.log"
"$MVN" -B -f vms-app/pom.xml spring-boot:run -Dspring-boot.run.profiles=$PROFILES 2>&1 | tee "$ROOT/backend-run.log"
