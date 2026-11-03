package com.worklog.day;

import java.math.BigDecimal;

public enum DayType {
    WORK("근무", "1.00"),
    HALF_AM("오전 반차", "0.50"),
    HALF_PM("오후 반차", "0.50"),
    ANNUAL("연차", "0.00"),
    HOLIDAY("휴일", "0.00");

    private final String label;
    private final BigDecimal workMd;

    DayType(String label, String workMd) {
        this.label = label;
        this.workMd = new BigDecimal(workMd);
    }

    public String label() {
        return label;
    }

    public BigDecimal workMd() {
        return workMd;
    }

    /** 연차·휴일은 비중을 입력할 수 없다. */
    public boolean locked() {
        return this == ANNUAL || this == HOLIDAY;
    }
}
