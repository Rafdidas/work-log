package com.worklog.report;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.worklog.day.DayType;
import com.worklog.report.WeeklyReport.AchievedItem;
import com.worklog.report.WeeklyReport.DayEntry;
import com.worklog.report.WeeklyReport.DayGroup;
import com.worklog.report.WeeklyReport.LeaveLine;
import com.worklog.report.WeeklyReport.PlanItem;

class WeeklyReportFormatterTest {

    private static final String TITLE = "2026-10-26 ~ 2026-10-30 개발1팀";
    private static final LocalDate MON = LocalDate.of(2026, 10, 26);

    private static DayGroup day(int offset, DayType leave, DayEntry... entries) {
        return new DayGroup(MON.plusDays(offset), leave, List.of(entries));
    }

    private static AchievedItem anyAchieved() {
        return new AchievedItem(1, "x", List.of(DayOfWeek.MONDAY), BigDecimal.ONE, false, List.of());
    }

    @Test
    void format_groupsAchievedByWeekday() {
        PlanItem voc = new PlanItem(1, "위시드 VOC 홈페이지", null);
        List<DayGroup> byDay = List.of(
                day(0, null,
                        new DayEntry(1, "위시드 VOC 홈페이지", new BigDecimal("0.50"), "회원가입\n  휴대전화인증\n", false),
                        new DayEntry(3, "회의", new BigDecimal("0.20"), null, false)),
                day(1, null),
                day(2, DayType.ANNUAL),
                day(3, null, new DayEntry(2, "ADS AI 질의 메뉴", new BigDecimal("1.00"), null, true)),
                day(4, DayType.HALF_PM, new DayEntry(3, "회의", new BigDecimal("0.5"), null, false)));
        WeeklyReport report = new WeeklyReport(TITLE, List.of(voc), List.of(anyAchieved()),
                List.of(new LeaveLine(MON.plusDays(2), DayType.ANNUAL)), byDay, List.of(voc), "");

        assertThat(WeeklyReportFormatter.format(report)).isEqualTo("""
                2026-10-26 ~ 2026-10-30 개발1팀

                [해야 할 일]
                - 위시드 VOC 홈페이지

                [실제 달성한 일]
                월(10/26)
                - 위시드 VOC 홈페이지 (0.50MD)
                  회원가입
                  휴대전화인증
                - 회의 (0.20MD)
                화(10/27)
                - 없음
                수(10/28) 연차
                목(10/29)
                - ADS AI 질의 메뉴 (1.00MD) (완료)
                금(10/30) 오후 반차
                - 회의 (0.50MD)

                [차주 업무]
                - 위시드 VOC 홈페이지""");
    }

    @Test
    void format_emptySections_showNone() {
        List<DayGroup> byDay = List.of(day(0, null), day(1, null), day(2, null), day(3, null), day(4, null));
        WeeklyReport report = new WeeklyReport(TITLE, List.of(), List.of(), List.of(), byDay, List.of(), "");

        assertThat(WeeklyReportFormatter.format(report)).isEqualTo("""
                2026-10-26 ~ 2026-10-30 개발1팀

                [해야 할 일]
                - 없음

                [실제 달성한 일]
                - 없음

                [차주 업무]
                - 없음""");
    }

    @Test
    void format_leaveOnlyWeek_showsDays() {
        List<DayGroup> byDay = List.of(day(0, null), day(1, DayType.ANNUAL), day(2, null), day(3, null),
                day(4, null));
        WeeklyReport report = new WeeklyReport(TITLE, List.of(), List.of(),
                List.of(new LeaveLine(MON.plusDays(1), DayType.ANNUAL)), byDay, List.of(), "");

        assertThat(WeeklyReportFormatter.format(report)).contains("월(10/26)\n- 없음\n화(10/27) 연차\n수(10/28)\n");
    }
}
