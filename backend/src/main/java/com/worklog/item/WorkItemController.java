package com.worklog.item;

import java.time.LocalDate;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.worklog.auth.CurrentUser;
import com.worklog.item.WorkItemService.CreateCommand;
import com.worklog.item.WorkItemService.UpdateCommand;
import com.worklog.item.WorkItemService.WorkItemResponse;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/api/work-items")
public class WorkItemController {

    public record CreateRequest(@NotBlank @Size(max = 200) String title, @Size(max = 100) String tag,
            Boolean recurring, Boolean showInPlan, WorkItemStatus status) {}

    public record UpdateRequest(@Size(max = 200) String title, @Size(max = 100) String tag, Boolean recurring,
            Boolean showInPlan, LocalDate startedAt, LocalDate completedAt) {}

    public record StatusRequest(@NotNull WorkItemStatus status) {}

    private final WorkItemService service;

    public WorkItemController(WorkItemService service) {
        this.service = service;
    }

    @GetMapping
    List<WorkItemResponse> list(@AuthenticationPrincipal CurrentUser user) {
        return service.list(user.id());
    }

    @GetMapping("/tags")
    List<String> tags(@AuthenticationPrincipal CurrentUser user) {
        return service.tags(user.id());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    WorkItemResponse create(@AuthenticationPrincipal CurrentUser user, @Valid @RequestBody CreateRequest req) {
        return service.create(user.id(),
                new CreateCommand(req.title(), req.tag(), req.recurring(), req.showInPlan(), req.status()));
    }

    @PatchMapping("/{id}")
    WorkItemResponse update(@AuthenticationPrincipal CurrentUser user, @PathVariable long id,
            @Valid @RequestBody UpdateRequest req) {
        return service.update(user.id(), id, new UpdateCommand(req.title(), req.tag(), req.recurring(),
                req.showInPlan(), req.startedAt(), req.completedAt()));
    }

    @PatchMapping("/{id}/status")
    WorkItemResponse changeStatus(@AuthenticationPrincipal CurrentUser user, @PathVariable long id,
            @Valid @RequestBody StatusRequest req) {
        return service.changeStatus(user.id(), id, req.status());
    }

    @PutMapping("/order")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void reorder(@AuthenticationPrincipal CurrentUser user, @RequestBody List<Long> ids) {
        service.reorder(user.id(), ids);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@AuthenticationPrincipal CurrentUser user, @PathVariable long id) {
        service.delete(user.id(), id);
    }
}
