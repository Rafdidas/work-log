package com.worklog.report;

import java.math.BigDecimal;
import java.time.LocalDate;

/** 보고서 집계용으로 날짜와 함께 읽어 온 체크인 기록 한 줄. */
public record EntryRow(LocalDate date, Long itemId, BigDecimal md, String memo) {}
