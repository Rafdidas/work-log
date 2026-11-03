package com.worklog.day;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.worklog.auth.CurrentUser;
import com.worklog.day.DayService.DaySummary;
import com.worklog.day.DayService.DayView;
import com.worklog.day.DayService.EntryInput;
import com.worklog.day.DayService.LeaveView;
import com.worklog.day.DayService.SaveCommand;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/api/days")
public class DayController {

    public record EntryRequest(@NotNull Long itemId, @Min(0) @Max(3) int weight, @Size(max = 2000) String memo) {}

    public record SaveRequest(@Size(max = 2000) String memo, @NotNull List<@Valid EntryRequest> entries) {}

    public record TypeRequest(@NotNull DayType dayType, boolean confirm) {}

    private final DayService service;

    public DayController(DayService service) {
        this.service = service;
    }

    @GetMapping
    List<DaySummary> range(@AuthenticationPrincipal CurrentUser user,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return service.range(user.id(), from, to);
    }

    @GetMapping("/leaves")
    List<LeaveView> leaves(@AuthenticationPrincipal CurrentUser user,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return service.leaves(user.id(), from, to);
    }

    @GetMapping("/{date}")
    DayView get(@AuthenticationPrincipal CurrentUser user,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return service.get(user.id(), date);
    }

    @PutMapping("/{date}")
    DayView save(@AuthenticationPrincipal CurrentUser user,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @Valid @RequestBody SaveRequest req) {
        List<EntryInput> inputs = req.entries().stream()
                .map(e -> new EntryInput(e.itemId(), e.weight(), e.memo())).toList();
        return service.save(user.id(), date, new SaveCommand(req.memo(), inputs));
    }

    @PutMapping("/{date}/type")
    DayView changeType(@AuthenticationPrincipal CurrentUser user,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @Valid @RequestBody TypeRequest req) {
        return service.changeType(user.id(), date, req.dayType(), req.confirm());
    }
}
