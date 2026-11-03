package com.worklog.day;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.worklog.common.ApiException;
import com.worklog.item.WorkItem;
import com.worklog.item.WorkItemRepository;
import com.worklog.item.WorkItemService;
import com.worklog.item.WorkItemStatus;

@Service
@Transactional
public class DayService {

    public record EntryView(Long itemId, String title, String tag, WorkItemStatus status, boolean recurring,
            int weight, BigDecimal md, String memo, boolean completedBefore) {}

    public record WaitingItemView(Long itemId, String title, String tag) {}

    public record DayView(LocalDate date, DayType dayType, String dayTypeLabel, BigDecimal workMd, String memo,
            List<EntryView> entries, List<WaitingItemView> waitingItems) {}

    public record EntryInput(long itemId, int weight, String memo) {}

    public record SaveCommand(String memo, List<EntryInput> entries) {}

    public record DaySummary(LocalDate date, DayType dayType, BigDecimal totalMd, boolean hasEntries) {}

    public record LeaveView(LocalDate date, DayType dayType, String dayTypeLabel) {}

    private final DayLogRepository logs;
    private final DayEntryRepository entries;
    private final WorkItemRepository items;
    private final WorkItemService itemService;

    public DayService(DayLogRepository logs, DayEntryRepository entries, WorkItemRepository items,
            WorkItemService itemService) {
        this.logs = logs;
        this.entries = entries;
        this.items = items;
        this.itemService = itemService;
    }

    @Transactional(readOnly = true)
    public DayView get(long userId, LocalDate date) {
        return view(userId, date);
    }

    public DayView save(long userId, LocalDate date, SaveCommand cmd) {
        Map<Long, WorkItem> userItems = items.findByUserIdOrderBySortOrderAscIdAsc(userId).stream()
                .collect(Collectors.toMap(WorkItem::getId, Function.identity(), (a, b) -> a, LinkedHashMap::new));
        Set<Long> seen = new HashSet<>();
        for (EntryInput input : cmd.entries()) {
            if (!userItems.containsKey(input.itemId())) {
                throw new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "업무 항목을 찾을 수 없습니다.");
            }
            if (!seen.add(input.itemId())) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "DUPLICATE_ITEM", "같은 항목이 중복되었습니다.");
            }
        }

        DayLog log = findOrCreate(userId, date);
        boolean anyWeight = cmd.entries().stream().anyMatch(e -> e.weight() > 0);
        if (log.getDayType().locked() && anyWeight) {
            throw new ApiException(HttpStatus.CONFLICT, "DAY_LOCKED",
                    log.getDayType().label() + "인 날에는 업무 비중을 입력할 수 없습니다.");
        }
        log.setMemo(blankToNull(cmd.memo()));

        Map<Long, DayEntry> existing = entries.findByDayLogId(log.getId()).stream()
                .collect(Collectors.toMap(DayEntry::getWorkItemId, Function.identity()));
        Set<Long> keep = new HashSet<>();
        for (EntryInput input : cmd.entries()) {
            String memo = blankToNull(input.memo());
            if (input.weight() == 0 && memo == null) {
                continue;
            }
            DayEntry entry = existing.get(input.itemId());
            if (entry == null) {
                entry = entries.save(new DayEntry(log.getId(), input.itemId()));
            }
            entry.setWeight(input.weight());
            entry.setMemo(memo);
            keep.add(input.itemId());
            if (input.weight() > 0) {
                itemService.markStartedOn(userItems.get(input.itemId()), date);
            }
        }
        existing.values().stream().filter(e -> !keep.contains(e.getWorkItemId())).forEach(entries::delete);

        recompute(log, userItems);
        return view(userId, date);
    }

    public DayView changeType(long userId, LocalDate date, DayType type, boolean confirm) {
        DayLog log = findOrCreate(userId, date);
        if (type.locked()) {
            List<DayEntry> current = entries.findByDayLogId(log.getId());
            if (current.stream().anyMatch(e -> e.getWeight() > 0)) {
                if (!confirm) {
                    throw new ApiException(HttpStatus.CONFLICT, "ENTRIES_EXIST",
                            "이 날에 입력한 업무 기록이 있습니다. 계속하면 기록이 삭제됩니다.");
                }
                entries.deleteAll(current);
            }
        }
        log.setDayType(type);
        Map<Long, WorkItem> userItems = items.findByUserIdOrderBySortOrderAscIdAsc(userId).stream()
                .collect(Collectors.toMap(WorkItem::getId, Function.identity(), (a, b) -> a, LinkedHashMap::new));
        recompute(log, userItems);
        return view(userId, date);
    }

    @Transactional(readOnly = true)
    public List<DaySummary> range(long userId, LocalDate from, LocalDate to) {
        List<DayLog> found = logs.findByUserIdAndDateBetweenOrderByDate(userId, from, to);
        Map<Long, List<DayEntry>> byLog = entriesByLog(found);
        return found.stream().map(l -> {
            List<DayEntry> es = byLog.getOrDefault(l.getId(), List.of());
            BigDecimal total = es.stream().map(DayEntry::getMd).reduce(BigDecimal.ZERO, BigDecimal::add)
                    .setScale(2);
            return new DaySummary(l.getDate(), l.getDayType(), total, es.stream().anyMatch(e -> e.getWeight() > 0));
        }).toList();
    }

    @Transactional(readOnly = true)
    public List<LeaveView> leaves(long userId, LocalDate from, LocalDate to) {
        return logs.findByUserIdAndDateBetweenOrderByDate(userId, from, to).stream()
                .filter(l -> l.getDayType() != DayType.WORK)
                .map(l -> new LeaveView(l.getDate(), l.getDayType(), l.getDayType().label()))
                .toList();
    }

    private DayLog findOrCreate(long userId, LocalDate date) {
        return logs.findByUserIdAndDate(userId, date)
                .orElseGet(() -> logs.save(new DayLog(userId, date, DayType.WORK)));
    }

    /** 그날 기록된 비중으로 MD를 다시 계산해 저장한다(항목 정렬 순서가 나머지 배분의 동률 기준). */
    private void recompute(DayLog log, Map<Long, WorkItem> userItems) {
        Map<Long, DayEntry> byItem = entries.findByDayLogId(log.getId()).stream()
                .collect(Collectors.toMap(DayEntry::getWorkItemId, Function.identity()));
        LinkedHashMap<Long, Integer> weights = new LinkedHashMap<>();
        for (Long itemId : userItems.keySet()) {
            DayEntry e = byItem.get(itemId);
            if (e != null) {
                weights.put(itemId, e.getWeight());
            }
        }
        Map<Long, BigDecimal> md = MdAllocator.allocate(log.getWorkMd(), weights);
        byItem.forEach((itemId, entry) -> entry.setMd(md.get(itemId)));
    }

    private DayView view(long userId, LocalDate date) {
        DayLog log = logs.findByUserIdAndDate(userId, date).orElse(null);
        DayType type = log == null ? DayType.WORK : log.getDayType();
        Map<Long, DayEntry> byItem = log == null ? Map.of()
                : entries.findByDayLogId(log.getId()).stream()
                        .collect(Collectors.toMap(DayEntry::getWorkItemId, Function.identity()));

        List<EntryView> entryViews = new ArrayList<>();
        List<WaitingItemView> waiting = new ArrayList<>();
        for (WorkItem item : items.findByUserIdOrderBySortOrderAscIdAsc(userId)) {
            DayEntry entry = byItem.get(item.getId());
            boolean completedBefore = entry != null && item.getCompletedAt() != null
                    && item.getCompletedAt().isBefore(date);
            boolean visible = entry != null || item.getStatus() == WorkItemStatus.IN_PROGRESS
                    || (item.getStatus() == WorkItemStatus.DONE && item.getCompletedAt() != null
                            && !item.getCompletedAt().isBefore(date));
            if (visible) {
                entryViews.add(new EntryView(item.getId(), item.getTitle(), item.getTag(), item.getStatus(),
                        item.isRecurring(), entry == null ? 0 : entry.getWeight(),
                        entry == null ? BigDecimal.ZERO.setScale(2) : entry.getMd(),
                        entry == null ? null : entry.getMemo(), completedBefore));
            } else if (item.getStatus() == WorkItemStatus.WAITING) {
                waiting.add(new WaitingItemView(item.getId(), item.getTitle(), item.getTag()));
            }
        }
        return new DayView(date, type, type.label(), type.workMd(), log == null ? null : log.getMemo(),
                entryViews, waiting);
    }

    private Map<Long, List<DayEntry>> entriesByLog(List<DayLog> found) {
        if (found.isEmpty()) {
            return Map.of();
        }
        return entries.findByDayLogIdIn(found.stream().map(DayLog::getId).toList()).stream()
                .collect(Collectors.groupingBy(DayEntry::getDayLogId));
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s;
    }
}
