package com.worklog.report;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.worklog.common.ApiException;
import com.worklog.day.DayLog;
import com.worklog.day.DayLogRepository;
import com.worklog.day.DayType;
import com.worklog.item.WorkItem;
import com.worklog.item.WorkItemRepository;
import com.worklog.report.EffortReport.MonthMd;
import com.worklog.report.MonthlySummary.MonthlyRow;

@Service
@Transactional(readOnly = true)
public class EffortReportService {

    private static final int MAX_MONTHS = 120;

    private final WorkItemRepository items;
    private final DayLogRepository logs;
    private final ReportQueries queries;

    public EffortReportService(WorkItemRepository items, DayLogRepository logs, ReportQueries queries) {
        this.items = items;
        this.logs = logs;
        this.queries = queries;
    }

    /** tag 또는 itemId 중 정확히 하나로 대상을 정하고, from~to 모든 달의 MD를 돌려준다. 태그는 항목의 현재 태그 기준. */
    public EffortReport effort(long userId, String tag, Long itemId, YearMonth from, YearMonth to) {
        boolean hasTag = tag != null && !tag.isBlank();
        if (hasTag == (itemId != null)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_TARGET", "tag 또는 itemId 중 하나만 지정하세요.");
        }
        if (from.isAfter(to) || ChronoUnit.MONTHS.between(from, to) >= MAX_MONTHS) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_RANGE",
                    "조회 기간이 올바르지 않습니다(최대 " + MAX_MONTHS + "개월).");
        }

        String label;
        List<Long> itemIds;
        if (hasTag) {
            label = tag.trim();
            itemIds = items.findByUserIdAndTag(userId, label).stream().map(WorkItem::getId).toList();
        } else {
            WorkItem item = items.findByIdAndUserId(itemId, userId).orElseThrow(
                    () -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "업무 항목을 찾을 수 없습니다."));
            label = item.getTitle();
            itemIds = List.of(item.getId());
        }

        Map<YearMonth, BigDecimal> byMonth = new HashMap<>();
        if (!itemIds.isEmpty()) {
            queries.monthlyTotals(userId, from.atDay(1), to.atEndOfMonth(), itemIds)
                    .forEach(t -> byMonth.put(YearMonth.of(t.year(), t.month()), t.total()));
        }

        List<MonthMd> months = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO.setScale(2);
        for (YearMonth m = from; !m.isAfter(to); m = m.plusMonths(1)) {
            BigDecimal md = byMonth.getOrDefault(m, BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
            months.add(new MonthMd(m, md));
            total = total.add(md);
        }

        boolean sameYear = from.getYear() == to.getYear();
        StringBuilder text = new StringBuilder(label + " 투입 공수 (" + from + " ~ " + to + ")");
        for (MonthMd m : months) {
            text.append('\n').append(sameYear ? m.month().getMonthValue() + "월" : m.month().toString())
                    .append(' ').append(m.md().toPlainString()).append("MD");
        }
        text.append("\n합계 ").append(total.toPlainString()).append("MD");
        return new EffortReport(label, from, to, months, total, text.toString());
    }

    public MonthlySummary monthly(long userId, YearMonth month) {
        LocalDate start = month.atDay(1);
        LocalDate end = month.atEndOfMonth();

        Map<Long, WorkItem> itemsById = items.findByUserIdOrderBySortOrderAscIdAsc(userId).stream()
                .collect(Collectors.toMap(WorkItem::getId, i -> i));
        List<ItemTotal> totals = queries.itemTotals(userId, start, end).stream()
                .filter(t -> t.total().signum() > 0)
                .sorted(Comparator.comparing(ItemTotal::total).reversed()
                        .thenComparing(t -> itemsById.get(t.itemId()).getSortOrder()))
                .toList();
        BigDecimal totalMd = totals.stream().map(ItemTotal::total).reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        List<MonthlyRow> rows = totals.stream().map(t -> {
            WorkItem item = itemsById.get(t.itemId());
            BigDecimal ratio = t.total().multiply(BigDecimal.valueOf(100)).divide(totalMd, 1, RoundingMode.HALF_UP);
            return new MonthlyRow(item.getTag(), item.getTitle(), t.total().setScale(2, RoundingMode.HALF_UP), ratio);
        }).toList();

        Map<DayType, Integer> leaveCounts = new EnumMap<>(DayType.class);
        for (DayType type : DayType.values()) {
            if (type != DayType.WORK) {
                leaveCounts.put(type, 0);
            }
        }
        for (DayLog log : logs.findByUserIdAndDateBetweenOrderByDate(userId, start, end)) {
            if (log.getDayType() != DayType.WORK) {
                leaveCounts.merge(log.getDayType(), 1, Integer::sum);
            }
        }

        int workDays = queries.workDates(userId, start, end).size();
        return new MonthlySummary(month, rows, workDays, leaveCounts, totalMd, tsv(rows));
    }

    private static String tsv(List<MonthlyRow> rows) {
        StringBuilder sb = new StringBuilder("태그\t업무\tMD\t비율(%)");
        for (MonthlyRow r : rows) {
            sb.append('\n').append(r.tag() == null ? "" : r.tag()).append('\t').append(r.title()).append('\t')
                    .append(r.md().toPlainString()).append('\t').append(r.ratioPercent().toPlainString());
        }
        return sb.toString();
    }
}
