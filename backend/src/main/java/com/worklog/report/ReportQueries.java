package com.worklog.report;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import com.worklog.day.DayEntry;

public interface ReportQueries extends Repository<DayEntry, Long> {

    @Query("select new com.worklog.report.EntryRow(l.date, e.workItemId, e.md, e.memo) "
            + "from DayEntry e join DayLog l on l.id = e.dayLogId "
            + "where l.userId = :userId and l.date between :from and :to order by l.date, e.id")
    List<EntryRow> findRows(@Param("userId") Long userId, @Param("from") LocalDate from,
            @Param("to") LocalDate to);
}
