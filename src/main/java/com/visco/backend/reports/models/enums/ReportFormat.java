package com.visco.backend.reports.models.enums;

public enum ReportFormat {
    PDF("application/pdf"),
    EXCEL("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
    JSON("application/json");

    private final String mimeType;

    ReportFormat(String mimeType) {
        this.mimeType = mimeType;
    }

    public String getMimeType() {
        return mimeType;
    }
}
