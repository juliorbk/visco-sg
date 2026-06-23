$ErrorActionPreference = "Stop"
$backupFile = Join-Path $PSScriptRoot "..\visco_backup.sql" | Resolve-Path -Relative

Write-Host "⚠️  Dump desde Render (Supabase)..." -ForegroundColor Cyan

docker run --rm `
  -e PGPASSWORD=viscojsuarez2026 `
  postgres:16-alpine `
  pg_dump --no-owner --no-acl `
    -h aws-1-us-east-1.pooler.supabase.com `
    -p 5432 `
    -U postgres.fixgkflhsjydxcbcdgoa `
    -d postgres `
  > $backupFile

if ($LASTEXITCODE -eq 0) {
    Write-Host "✅ Backup guardado en $backupFile" -ForegroundColor Green
} else {
    Write-Host "❌ Error al hacer dump" -ForegroundColor Red
    exit 1
}
