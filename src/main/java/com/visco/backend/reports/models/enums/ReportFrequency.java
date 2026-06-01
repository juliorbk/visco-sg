package com.visco.backend.reports.models.enums;

public enum ReportFrequency {
    DAILY("Diario"),
    WEEKLY("Semanal"),
    MONTHLY("Mensual");

    private final String displayName;

    ReportFrequency(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
