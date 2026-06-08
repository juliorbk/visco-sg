package com.visco.backend.reports.models.dtos;

import java.util.List;
import java.util.Map;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ReportAnalyticsDTO {

	private long totalReports;
	private long totalScheduledReports;
	private long completedReports;
	private long failedReports;
	private long pendingReports;
	private long totalRecordsExported;
	private Map<String, Long> reportsByType;
	private Map<String, Long> reportsByStatus;
	private List<MonthlyCount> monthlyTrend;

	@Data
	@Builder
	public static class MonthlyCount {
		private String month;
		private long count;
	}
}
