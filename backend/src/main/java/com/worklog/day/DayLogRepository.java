package com.worklog.day;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DayLogRepository extends JpaRepository<DayLog, Long> {

    Optional<DayLog> findByUserIdAndDate(Long userId, LocalDate date);

    List<DayLog> findByUserIdAndDateBetweenOrderByDate(Long userId, LocalDate from, LocalDate to);
}
