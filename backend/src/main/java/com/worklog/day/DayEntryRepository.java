package com.worklog.day;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DayEntryRepository extends JpaRepository<DayEntry, Long> {

    List<DayEntry> findByDayLogId(Long dayLogId);

    List<DayEntry> findByDayLogIdIn(Collection<Long> dayLogIds);
}
