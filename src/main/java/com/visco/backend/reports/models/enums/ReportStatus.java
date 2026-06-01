package com.visco.backend.reports.models.enums;

public enum ReportStatus {
    PENDING("Pendiente"),
    PROCESSING("Procesando"),
    COMPLETED("Completado"),
    FAILED("Error");

    private final String displayName;

    ReportStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
