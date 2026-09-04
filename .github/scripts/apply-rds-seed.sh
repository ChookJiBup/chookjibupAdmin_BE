#!/usr/bin/env bash
# Apply Admin BE RDS light seed on EC2 via Gradle/JDBC (no psql/python).
# One-shot helper invoked by the Seed RDS Actions workflow.
set -euo pipefail

SECRET_FILE="${SECRET_FILE:-/etc/chookjibup-admin/application-secret.yml}"
DEPLOY_PATH="${DEPLOY_PATH:-/home/ec2-user/app/chookjibupAdmin_BE}"
SEED_SQL="${SEED_SQL:-${DEPLOY_PATH}/docs/seed-data/시드데이터_RDS경량_통합.sql}"
JAVA_HOME="${JAVA_HOME:-/usr/lib/jvm/java-21-amazon-corretto.x86_64}"

cd "$DEPLOY_PATH"
test -f "$SEED_SQL"
test -f "$SECRET_FILE"
chmod +x ./gradlew

export SECRET_FILE SEED_SQL JAVA_HOME
export HOME="${HOME:-/home/ec2-user}"

echo "Running applyRdsSeed with JAVA_HOME=$JAVA_HOME"
./gradlew applyRdsSeed --no-daemon
