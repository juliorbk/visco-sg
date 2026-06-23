$ErrorActionPreference = "Stop"
$backupFile = Join-Path $PSScriptRoot "..\visco_backup.sql" | Resolve-Path -Relative

if (-not (Test-Path $backupFile)) {
    Write-Host "❌ No se encuentra $backupFile. Ejecutá primero dump-from-render.ps1" -ForegroundColor Red
    exit 1
}

Write-Host "⏳ Restaurando $backupFile en Docker local..." -ForegroundColor Cyan

Get-Content $backupFile | docker exec -i visco_db psql -U visco_user -d visco_db

if ($LASTEXITCODE -eq 0) {
    Write-Host "✅ Restauración completada" -ForegroundColor Green
} else {
    Write-Host "❌ Error al restaurar" -ForegroundColor Red
    exit 1
}
