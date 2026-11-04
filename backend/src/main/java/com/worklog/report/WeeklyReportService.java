package com.worklog.report;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
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
import com.worklog.report.WeeklyReport.DayEntry;
import com.worklog.report.WeeklyReport.DayGroup;
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
        Predicate<WorkItem> planned = i -> i.isShowInPlan() && !effectiveStart(i).isAfter(friday);
        List<PlanItem> todo = all.stream()
                .filter(planned.and(i -> i.getCompletedAt() == null || !i.getCompletedAt().isBefore(monday)))
                .map(WeeklyReportService::plan).toList();
        List<PlanItem> next = all.stream()
                .filter(planned.and(i -> i.getCompletedAt() == null || i.getCompletedAt().isAfter(friday)))
                .map(WeeklyReportService::plan).toList();

        Map<Long, List<EntryRow>> rowsByItem = queries.findRows(userId, monday, friday).stream()
                .collect(Collectors.groupingBy(EntryRow::itemId));
        List<AchievedItem> achieved = new ArrayList<>();
        Map<LocalDate, List<DayEntry>> entriesByDate = new HashMap<>();
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

            List<EntryRow> dayRows = rows.stream()
                    .filter(r -> r.md().signum() > 0 || (r.memo() != null && !r.memo().isBlank()))
                    .sorted(Comparator.comparing(EntryRow::date)).toList();
            for (int i = 0; i < dayRows.size(); i++) {
                EntryRow r = dayRows.get(i);
                String memo = r.memo() == null || r.memo().isBlank() ? null : r.memo();
                boolean last = i == dayRows.size() - 1;
                entriesByDate.computeIfAbsent(r.date(), d -> new ArrayList<>()).add(new DayEntry(item.getId(),
                        item.getTitle(), r.md().setScale(2), memo, completed && last));
            }
        }

        List<LeaveLine> leaves = logs.findByUserIdAndDateBetweenOrderByDate(userId, monday, friday).stream()
                .filter(l -> l.getDayType() != DayType.WORK)
                .map((DayLog l) -> new LeaveLine(l.getDate(), l.getDayType())).toList();

        Map<LocalDate, DayType> leaveByDate = leaves.stream()
                .collect(Collectors.toMap(LeaveLine::date, LeaveLine::type));
        List<DayGroup> byDay = monday.datesUntil(friday.plusDays(1))
                .map(d -> new DayGroup(d, leaveByDate.get(d), entriesByDate.getOrDefault(d, List.of()))).toList();

        String title = monday + " ~ " + friday + " " + user.getTeamLabel();
        WeeklyReport draft = new WeeklyReport(title, todo, achieved, leaves, byDay, next, "");
        return new WeeklyReport(title, todo, achieved, leaves, byDay, next, WeeklyReportFormatter.format(draft));
    }

    /** 항목이 계획에 올라온 날: 등록일과 (소급해서 고친) 시작일 중 이른 날. */
    private static LocalDate effectiveStart(WorkItem item) {
        LocalDate created = item.getCreatedAt().atZone(TimeConfig.SEOUL).toLocalDate();
        LocalDate started = item.getStartedAt();
        return started != null && started.isBefore(created) ? started : created;
    }

    private static PlanItem plan(WorkItem item) {
        return new PlanItem(item.getId(), item.getTitle(), item.getTag());
    }
}
