package com.worklog.report;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

import com.worklog.day.DayType;

public record WeeklyReport(String title, List<PlanItem> todo, List<AchievedItem> achieved, List<LeaveLine> leaves,
        List<DayGroup> byDay, List<PlanItem> next, String text) {

    public record PlanItem(long itemId, String title, String tag) {}

    public record DayMemo(DayOfWeek day, String memo) {}

    public record AchievedItem(long itemId, String title, List<DayOfWeek> days, BigDecimal md, boolean completed,
            List<DayMemo> memos) {}

    public record LeaveLine(LocalDate date, DayType type) {}

    /** 실제 달성한 일을 요일별로 묶은 것. leave는 연차·반차·휴일일 때만 값이 있다. */
    public record DayGroup(LocalDate date, DayType leave, List<DayEntry> entries) {}

    public record DayEntry(long itemId, String title, BigDecimal md, String memo, boolean completed) {}
}
