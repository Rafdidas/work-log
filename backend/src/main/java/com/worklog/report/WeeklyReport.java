package com.worklog.report;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

import com.worklog.day.DayType;

public record WeeklyReport(String title, List<PlanItem> todo, List<AchievedItem> achieved, List<LeaveLine> leaves,
        List<PlanItem> next, String text) {

    public record PlanItem(long itemId, String title, String tag) {}

    public record DayMemo(DayOfWeek day, String memo) {}

    public record AchievedItem(long itemId, String title, List<DayOfWeek> days, BigDecimal md, boolean completed,
            List<DayMemo> memos) {}

    public record LeaveLine(LocalDate date, DayType type) {}
}
