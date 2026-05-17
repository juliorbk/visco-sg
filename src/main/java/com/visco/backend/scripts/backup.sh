#!/bin/bash
# backup.sh — Backup automático de PostgreSQL para Visco Orinoco
# Uso: ./backup.sh
# Se ejecuta desde el cron del contenedor de backup (ver docker-compose.yml)

set -euo pipefail

# ── Config (viene de variables de entorno del contenedor) ─────────────────────
DB_HOST="${DB_HOST:-postgres}"
DB_PORT="${DB_PORT:-5432}"
DB_NAME="${DB_NAME:-visco_db}"
DB_USER="${DB_USER:-visco_admin}"
PGPASSWORD="${DB_PASSWORD:-admin123}"
export PGPASSWORD

BACKUP_DIR="/backups"
RETENTION_DAYS="${RETENTION_DAYS:-7}"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
FILENAME="visco_backup_${TIMESTAMP}.sql.gz"
FILEPATH="${BACKUP_DIR}/${FILENAME}"

# ── Crear directorio si no existe ─────────────────────────────────────────────
mkdir -p "${BACKUP_DIR}"

echo "🗄️  [$(date '+%Y-%m-%d %H:%M:%S')] Iniciando backup de ${DB_NAME}..."

# ── Dump + compresión en un solo pipe ─────────────────────────────────────────
pg_dump \
  --host="${DB_HOST}" \
  --port="${DB_PORT}" \
  --username="${DB_USER}" \
  --dbname="${DB_NAME}" \
  --no-password \
  --format=plain \
  --no-owner \
  --no-privileges \
  | gzip > "${FILEPATH}"

SIZE=$(du -sh "${FILEPATH}" | cut -f1)
echo "✅ Backup completado: ${FILENAME} (${SIZE})"

# ── Rotar backups: eliminar los de más de N días ──────────────────────────────
echo "🔄 Rotando backups con más de ${RETENTION_DAYS} días..."
DELETED=$(find "${BACKUP_DIR}" -name "visco_backup_*.sql.gz" \
          -mtime +"${RETENTION_DAYS}" -print -delete | wc -l)
echo "🗑️  ${DELETED} backup(s) eliminado(s)"

# ── Listar backups actuales ───────────────────────────────────────────────────
echo "📁 Backups disponibles:"
ls -lh "${BACKUP_DIR}"/visco_backup_*.sql.gz 2>/dev/null || echo "  (ninguno)"

echo "✔️  [$(date '+%Y-%m-%d %H:%M:%S')] Proceso finalizado."