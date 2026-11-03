package com.worklog.report;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

import com.worklog.day.DayType;

public record MonthlySummary(YearMonth month, List<MonthlyRow> rows, int workDays,
        Map<DayType, Integer> leaveCounts, BigDecimal totalMd, String tsv) {

    public record MonthlyRow(String tag, String title, BigDecimal md, BigDecimal ratioPercent) {}
}
