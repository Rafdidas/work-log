package com.worklog.item;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.worklog.common.ApiException;

@Service
@Transactional
public class WorkItemService {

    public record WorkItemResponse(Long id, String title, String tag, WorkItemStatus status, boolean recurring,
            boolean showInPlan, int sortOrder, LocalDate startedAt, LocalDate completedAt, BigDecimal totalMd) {}

    public record CreateCommand(String title, String tag, Boolean recurring, Boolean showInPlan,
            WorkItemStatus status) {}

    public record UpdateCommand(String title, String tag, Boolean recurring, Boolean showInPlan,
            LocalDate startedAt, LocalDate completedAt) {}

    private final WorkItemRepository items;
    private final Clock clock;

    public WorkItemService(WorkItemRepository items, Clock clock) {
        this.items = items;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<WorkItemResponse> list(long userId) {
        List<WorkItem> all = items.findByUserIdOrderBySortOrderAscIdAsc(userId);
        Map<Long, BigDecimal> totals = totals(all);
        return all.stream().map(i -> toResponse(i, totals.get(i.getId()))).toList();
    }

    public WorkItemResponse create(long userId, CreateCommand cmd) {
        WorkItemStatus status = cmd.status() == null ? WorkItemStatus.IN_PROGRESS : cmd.status();
        if (status == WorkItemStatus.DONE) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_STATUS", "새 항목은 완료 상태로 만들 수 없습니다.");
        }
        boolean recurring = Boolean.TRUE.equals(cmd.recurring());
        Integer max = recurring ? items.maxSortOrder(userId) : items.maxSortOrder(userId, false);
        WorkItem item = new WorkItem(userId, cmd.title().trim(), status, (max == null ? 0 : max) + 1,
                clock.instant());
        item.setTag(normalizeTag(cmd.tag()));
        item.setRecurring(recurring);
        item.setShowInPlan(cmd.showInPlan() == null || cmd.showInPlan());
        if (status == WorkItemStatus.IN_PROGRESS) {
            item.setStartedAt(today());
        }
        return toResponse(items.save(item), BigDecimal.ZERO);
    }

    public WorkItemResponse update(long userId, long id, UpdateCommand cmd) {
        WorkItem item = find(userId, id);
        if (cmd.title() != null) {
            if (cmd.title().isBlank()) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "제목은 비울 수 없습니다.");
            }
            item.setTitle(cmd.title().trim());
        }
        if (cmd.tag() != null) {
            item.setTag(normalizeTag(cmd.tag()));
        }
        if (cmd.recurring() != null) {
            item.setRecurring(cmd.recurring());
        }
        if (cmd.showInPlan() != null) {
            item.setShowInPlan(cmd.showInPlan());
        }
        if (cmd.startedAt() != null) {
            item.setStartedAt(cmd.startedAt());
        }
        if (cmd.completedAt() != null) {
            if (item.getStatus() != WorkItemStatus.DONE) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_COMPLETED_AT",
                        "완료일은 완료된 항목에만 지정할 수 있습니다.");
            }
            item.setCompletedAt(cmd.completedAt());
        }
        return toResponse(item, totals(List.of(item)).get(item.getId()));
    }

    public WorkItemResponse changeStatus(long userId, long id, WorkItemStatus target) {
        WorkItem item = find(userId, id);
        LocalDate today = today();
        switch (target) {
            case IN_PROGRESS -> {
                if (item.getStartedAt() == null) {
                    item.setStartedAt(today);
                }
                item.setCompletedAt(null);
            }
            case DONE -> {
                item.setCompletedAt(today);
                if (item.getStartedAt() == null) {
                    item.setStartedAt(today);
                }
            }
            case WAITING -> {
                if (item.getStatus() == WorkItemStatus.DONE) {
                    throw new ApiException(HttpStatus.CONFLICT, "INVALID_TRANSITION",
                            "완료된 항목은 대기로 되돌릴 수 없습니다. 진행 중으로 변경하세요.");
                }
            }
        }
        item.setStatus(target);
        return toResponse(item, totals(List.of(item)).get(item.getId()));
    }

    /** 나열한 id를 앞에 두고, 나머지는 기존 순서대로 뒤에 붙여 1부터 다시 번호를 매긴다. */
    public void reorder(long userId, List<Long> orderedIds) {
        Map<Long, WorkItem> byId = new LinkedHashMap<>();
        items.findByUserIdOrderBySortOrderAscIdAsc(userId).forEach(i -> byId.put(i.getId(), i));
        List<WorkItem> ordered = new ArrayList<>();
        for (Long id : orderedIds) {
            WorkItem item = byId.remove(id);
            if (item == null) {
                throw notFound();
            }
            ordered.add(item);
        }
        ordered.addAll(byId.values());
        for (int i = 0; i < ordered.size(); i++) {
            ordered.get(i).setSortOrder(i + 1);
        }
    }

    public void delete(long userId, long id) {
        WorkItem item = find(userId, id);
        if (items.hasEntries(item.getId())) {
            throw new ApiException(HttpStatus.CONFLICT, "HAS_ENTRIES", "기록이 있는 항목은 삭제할 수 없습니다. 완료 처리하세요.");
        }
        items.delete(item);
    }

    @Transactional(readOnly = true)
    public List<String> tags(long userId) {
        // DB 로케일에 따라 정렬이 달라지므로 코드 포인트 순으로 직접 정렬한다.
        return items.findTags(userId).stream().sorted().toList();
    }

    /** 대기 항목에 처음 기록이 생기면 진행 중으로 바꾸고, 시작일을 더 이른 기록 날짜로 당긴다. */
    public void markStartedOn(WorkItem item, LocalDate date) {
        if (item.getStatus() == WorkItemStatus.WAITING) {
            item.setStatus(WorkItemStatus.IN_PROGRESS);
        }
        if (item.getStartedAt() == null || date.isBefore(item.getStartedAt())) {
            item.setStartedAt(date);
        }
    }

    private WorkItem find(long userId, long id) {
        return items.findByIdAndUserId(id, userId).orElseThrow(WorkItemService::notFound);
    }

    private static ApiException notFound() {
        return new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "업무 항목을 찾을 수 없습니다.");
    }

    private LocalDate today() {
        return LocalDate.now(clock);
    }

    private static String normalizeTag(String tag) {
        if (tag == null || tag.isBlank()) {
            return null;
        }
        return tag.trim();
    }

    private Map<Long, BigDecimal> totals(List<WorkItem> list) {
        Map<Long, BigDecimal> result = new HashMap<>();
        if (!list.isEmpty()) {
            items.totalsFor(list.stream().map(WorkItem::getId).toList())
                    .forEach(t -> result.put(t.getWorkItemId(), t.getTotal()));
        }
        return result;
    }

    private static WorkItemResponse toResponse(WorkItem i, BigDecimal total) {
        return new WorkItemResponse(i.getId(), i.getTitle(), i.getTag(), i.getStatus(), i.isRecurring(),
                i.isShowInPlan(), i.getSortOrder(), i.getStartedAt(), i.getCompletedAt(),
                total == null ? BigDecimal.ZERO : total);
    }
}
