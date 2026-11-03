package com.worklog.report;

import java.math.BigDecimal;

/** 월별 MD 합계 집계 결과 한 줄. */
public record MonthTotal(Integer year, Integer month, BigDecimal total) {}
