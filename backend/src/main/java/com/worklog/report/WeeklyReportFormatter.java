package com.worklog.report;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.worklog.report.WeeklyReport.AchievedItem;
import com.worklog.report.WeeklyReport.LeaveLine;
import com.worklog.report.WeeklyReport.PlanItem;

/** 주간 보고 초안을 회사 보고서에 붙여넣기 좋은 순수 텍스트로 만든다. */
public final class WeeklyReportFormatter {

    private static final String NONE = "- 없음";

    private WeeklyReportFormatter() {}

    public static String format(WeeklyReport report) {
        List<String> lines = new ArrayList<>();
        lines.add(report.title());
        lines.add("");

        lines.add("[해야 할 일]");
        addPlan(lines, report.todo());
        lines.add("");

        lines.add("[실제 달성한 일]");
        if (report.achieved().isEmpty() && report.leaves().isEmpty()) {
            lines.add(NONE);
        }
        for (AchievedItem item : report.achieved()) {
            lines.add("- " + item.title() + (item.completed() ? " (완료)" : "") + " (" + days(item.days()) + " / "
                    + md(item.md()) + "MD)");
            item.memos().forEach(m -> lines.add("  · " + dayName(m.day()) + ": " + oneLine(m.memo())));
        }
        for (LeaveLine leave : report.leaves()) {
            LocalDate d = leave.date();
            lines.add("- " + d.getMonthValue() + "/" + d.getDayOfMonth() + "(" + dayName(d.getDayOfWeek()) + ") "
                    + leave.type().label());
        }
        lines.add("");

        lines.add("[차주 업무]");
        addPlan(lines, report.next());
        return String.join("\n", lines);
    }

    private static void addPlan(List<String> lines, List<PlanItem> items) {
        if (items.isEmpty()) {
            lines.add(NONE);
            return;
        }
        items.forEach(i -> lines.add("- " + i.title()));
    }

    private static String days(List<DayOfWeek> days) {
        if (days.size() == 5) {
            return "월~금";
        }
        return days.stream().map(WeeklyReportFormatter::dayName).collect(Collectors.joining("·"));
    }

    private static String dayName(DayOfWeek day) {
        return switch (day) {
            case MONDAY -> "월";
            case TUESDAY -> "화";
            case WEDNESDAY -> "수";
            case THURSDAY -> "목";
            case FRIDAY -> "금";
            case SATURDAY -> "토";
            case SUNDAY -> "일";
        };
    }

    private static String md(BigDecimal md) {
        return md.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private static String oneLine(String memo) {
        return memo.trim().replaceAll("\\s*\\R\\s*", ", ");
    }
}
