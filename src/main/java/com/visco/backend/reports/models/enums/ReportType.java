package com.visco.backend.reports.models.enums;

public enum ReportType {
    STOCK_INVENTORY("Reporte de Stock"),
    STOCK_MOVEMENTS("Reporte de Movimientos"),
    CRITICAL_ALERTS("Alertas Críticas"),
    WAREHOUSE_ANALYSIS("Análisis por Almacén");

    private final String displayName;

    ReportType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
