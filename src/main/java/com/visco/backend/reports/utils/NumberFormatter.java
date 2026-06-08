package com.visco.backend.reports.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
// Utility for formatting currency, percentages, and numbers for report output.
public final class NumberFormatter {

    private static final NumberFormat CURRENCY_FMT = NumberFormat.getCurrencyInstance(new Locale("es", "VE"));
    private static final DecimalFormat PERCENT_FMT = new DecimalFormat("#0.0");
    private static final DecimalFormat NUMBER_FMT = new DecimalFormat("#,##0.##");

    static {
        CURRENCY_FMT.setMinimumFractionDigits(2);
        CURRENCY_FMT.setMaximumFractionDigits(2);
    }

    public static String formatCurrency(BigDecimal amount) {
        if (amount == null) return "$0.00";
        return CURRENCY_FMT.format(amount.setScale(2, RoundingMode.HALF_UP));
    }

    public static String formatPercent(Double value) {
        if (value == null) return "0.0%";
        return PERCENT_FMT.format(value) + "%";
    }

    public static String formatNumber(BigDecimal value) {
        if (value == null) return "0";
        return NUMBER_FMT.format(value);
    }

    public static String formatNumber(Integer value) {
        if (value == null) return "0";
        return NUMBER_FMT.format(value);
    }

    public static BigDecimal toBigDecimal(Object value) {
        if (value == null) return BigDecimal.ZERO;
        if (value instanceof BigDecimal bd) return bd;
        if (value instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        try {
            return new BigDecimal(value.toString());
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }
}
