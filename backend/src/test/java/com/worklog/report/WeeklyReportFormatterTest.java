package com.worklog.report;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.worklog.day.DayType;
import com.worklog.report.WeeklyReport.AchievedItem;
import com.worklog.report.WeeklyReport.DayMemo;
import com.worklog.report.WeeklyReport.LeaveLine;
import com.worklog.report.WeeklyReport.PlanItem;

class WeeklyReportFormatterTest {

    private static final String TITLE = "2026-10-26 ~ 2026-10-30 개발1팀";

    private static WeeklyReport report(List<PlanItem> todo, List<AchievedItem> achieved, List<LeaveLine> leaves,
            List<PlanItem> next) {
        return new WeeklyReport(TITLE, todo, achieved, leaves, next, "");
    }

    @Test
    void format_matchesSpecExample() {
        PlanItem voc = new PlanItem(1, "위시드 VOC 홈페이지", null);
        WeeklyReport report = report(
                List.of(voc),
                List.of(
                        new AchievedItem(1, "위시드 VOC 홈페이지",
                                List.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY),
                                new BigDecimal("1.50"), false,
                                List.of(new DayMemo(DayOfWeek.MONDAY, "회원가입\n휴대전화인증"))),
                        new AchievedItem(2, "ADS AI 질의 메뉴", List.of(DayOfWeek.THURSDAY, DayOfWeek.FRIDAY),
                                new BigDecimal("1.20"), true, List.of()),
                        new AchievedItem(3, "회의",
                                List.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                                        DayOfWeek.THURSDAY, DayOfWeek.FRIDAY),
                                new BigDecimal("0.80"), false, List.of())),
                List.of(new LeaveLine(LocalDate.of(2026, 10, 30), DayType.HALF_PM)),
                List.of(voc));

        assertThat(WeeklyReportFormatter.format(report)).isEqualTo("""
                2026-10-26 ~ 2026-10-30 개발1팀

                [해야 할 일]
                - 위시드 VOC 홈페이지

                [실제 달성한 일]
                - 위시드 VOC 홈페이지 (월·화·수 / 1.50MD)
                  · 월: 회원가입, 휴대전화인증
                - ADS AI 질의 메뉴 (완료) (목·금 / 1.20MD)
                - 회의 (월~금 / 0.80MD)
                - 10/30(금) 오후 반차

                [차주 업무]
                - 위시드 VOC 홈페이지""");
    }

    @Test
    void format_allFiveDays_showsRange() {
        AchievedItem meeting = new AchievedItem(1, "회의",
                List.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY,
                        DayOfWeek.FRIDAY),
                new BigDecimal("2.7"), false, List.of());

        assertThat(WeeklyReportFormatter.format(report(List.of(), List.of(meeting), List.of(), List.of())))
                .contains("- 회의 (월~금 / 2.70MD)");
    }

    @Test
    void format_emptySections_showNone() {
        assertThat(WeeklyReportFormatter.format(report(List.of(), List.of(), List.of(), List.of()))).isEqualTo("""
                2026-10-26 ~ 2026-10-30 개발1팀

                [해야 할 일]
                - 없음

                [실제 달성한 일]
                - 없음

                [차주 업무]
                - 없음""");
    }

    @Test
    void format_leaveOnlyWeek_listsLeaveInsteadOfNone() {
        WeeklyReport report = report(List.of(), List.of(),
                List.of(new LeaveLine(LocalDate.of(2026, 10, 27), DayType.ANNUAL)), List.of());

        assertThat(WeeklyReportFormatter.format(report)).contains("[실제 달성한 일]\n- 10/27(화) 연차\n");
    }
}
