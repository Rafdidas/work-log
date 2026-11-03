package com.worklog.report;

import java.time.LocalDate;

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

    public ReportController(WeeklyReportService weekly) {
        this.weekly = weekly;
    }

    @GetMapping("/weekly")
    WeeklyReport weekly(@AuthenticationPrincipal CurrentUser user,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate monday) {
        return weekly.build(user.id(), monday);
    }
}
