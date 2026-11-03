package com.worklog.report;

import java.time.LocalDate;
import java.time.YearMonth;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.worklog.auth.CurrentUser;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final WeeklyReportService weekly;
    private final EffortReportService effort;

    public ReportController(WeeklyReportService weekly, EffortReportService effort) {
        this.weekly = weekly;
        this.effort = effort;
    }

    @GetMapping("/weekly")
    WeeklyReport weekly(@AuthenticationPrincipal CurrentUser user,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate monday) {
        return weekly.build(user.id(), monday);
    }

    @GetMapping("/effort")
    EffortReport effort(@AuthenticationPrincipal CurrentUser user, @RequestParam(required = false) String tag,
            @RequestParam(required = false) Long itemId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth from,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth to) {
        return effort.effort(user.id(), tag, itemId, from, to);
    }

    @GetMapping("/monthly")
    MonthlySummary monthly(@AuthenticationPrincipal CurrentUser user,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth month) {
        return effort.monthly(user.id(), month);
    }
}
