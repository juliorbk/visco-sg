package com.visco.backend.reports.models.enums;

// Lifecycle status of a report from creation to completion or failure.
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
