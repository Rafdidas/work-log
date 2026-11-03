package com.worklog.report;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.worklog.auth.User;
import com.worklog.auth.UserRepository;
import com.worklog.common.ApiException;
import com.worklog.common.TimeConfig;
import com.worklog.day.DayLog;
import com.worklog.day.DayLogRepository;
import com.worklog.day.DayType;
import com.worklog.item.WorkItem;
import com.worklog.item.WorkItemRepository;
import com.worklog.report.WeeklyReport.AchievedItem;
import com.worklog.report.WeeklyReport.DayMemo;
import com.worklog.report.WeeklyReport.LeaveLine;
import com.worklog.report.WeeklyReport.PlanItem;

@Service
@Transactional(readOnly = true)
public class WeeklyReportService {

    private final UserRepository users;
    private final WorkItemRepository items;
    private final DayLogRepository logs;
    private final ReportQueries queries;

    public WeeklyReportService(UserRepository users, WorkItemRepository items, DayLogRepository logs,
            ReportQueries queries) {
        this.users = users;
        this.items = items;
        this.logs = logs;
        this.queries = queries;
    }

    public WeeklyReport build(long userId, LocalDate monday) {
        if (monday.getDayOfWeek() != DayOfWeek.MONDAY) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "NOT_MONDAY", "monday는 월요일 날짜여야 합니다.");
        }
        LocalDate friday = monday.plusDays(4);
        User user = users.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "UNKNOWN_USER", "사용자를 찾을 수 없습니다."));
        List<WorkItem> all = items.findByUserIdOrderBySortOrderAscIdAsc(userId);

        // 계획 섹션: 그 주 금요일까지 만들어졌고 "계획 섹션에 표시"가 켜진 항목만
        Predicate<WorkItem> planned = i -> i.isShowInPlan() && !createdDate(i).isAfter(friday);
        List<PlanItem> todo = all.stream()
                .filter(planned.and(i -> i.getCompletedAt() == null || !i.getCompletedAt().isBefore(monday)))
                .map(WeeklyReportService::plan).toList();
        List<PlanItem> next = all.stream()
                .filter(planned.and(i -> i.getCompletedAt() == null || i.getCompletedAt().isAfter(friday)))
                .map(WeeklyReportService::plan).toList();

        Map<Long, List<EntryRow>> rowsByItem = queries.findRows(userId, monday, friday).stream()
                .collect(Collectors.groupingBy(EntryRow::itemId));
        List<AchievedItem> achieved = new ArrayList<>();
        for (WorkItem item : all) {
            List<EntryRow> rows = rowsByItem.getOrDefault(item.getId(), List.of());
            BigDecimal total = rows.stream().map(EntryRow::md).reduce(BigDecimal.ZERO, BigDecimal::add);
            if (total.signum() <= 0) {
                continue;
            }
            List<DayOfWeek> days = new TreeSet<>(rows.stream().filter(r -> r.md().signum() > 0)
                    .map(r -> r.date().getDayOfWeek()).toList()).stream().toList();
            List<DayMemo> memos = rows.stream().filter(r -> r.memo() != null && !r.memo().isBlank())
                    .map(r -> new DayMemo(r.date().getDayOfWeek(), r.memo())).toList();
            boolean completed = item.getCompletedAt() != null && !item.getCompletedAt().isBefore(monday)
                    && !item.getCompletedAt().isAfter(friday);
            achieved.add(new AchievedItem(item.getId(), item.getTitle(), days, total.setScale(2), completed, memos));
        }

        List<LeaveLine> leaves = logs.findByUserIdAndDateBetweenOrderByDate(userId, monday, friday).stream()
                .filter(l -> l.getDayType() != DayType.WORK)
                .map((DayLog l) -> new LeaveLine(l.getDate(), l.getDayType())).toList();

        String title = monday + " ~ " + friday + " " + user.getTeamLabel();
        WeeklyReport draft = new WeeklyReport(title, todo, achieved, leaves, next, "");
        return new WeeklyReport(title, todo, achieved, leaves, next, WeeklyReportFormatter.format(draft));
    }

    private static LocalDate createdDate(WorkItem item) {
        return item.getCreatedAt().atZone(TimeConfig.SEOUL).toLocalDate();
    }

    private static PlanItem plan(WorkItem item) {
        return new PlanItem(item.getId(), item.getTitle(), item.getTag());
    }
}
