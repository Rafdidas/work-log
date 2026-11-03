package com.worklog.report;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

public record EffortReport(String label, YearMonth from, YearMonth to, List<MonthMd> months, BigDecimal total,
        String text) {

    public record MonthMd(YearMonth month, BigDecimal md) {}
}
