package com.worklog.report;

import java.math.BigDecimal;

/** 항목별 MD 합계 집계 결과 한 줄. */
public record ItemTotal(Long itemId, BigDecimal total) {}
