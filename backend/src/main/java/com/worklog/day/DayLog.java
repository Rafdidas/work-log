package com.worklog.day;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "day_logs")
public class DayLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false)
    private LocalDate date;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_type", nullable = false)
    private DayType dayType;

    @Column(name = "work_md", nullable = false)
    private BigDecimal workMd;

    private String memo;

    protected DayLog() {}

    public DayLog(long userId, LocalDate date, DayType dayType) {
        this.userId = userId;
        this.date = date;
        setDayType(dayType);
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public LocalDate getDate() { return date; }
    public DayType getDayType() { return dayType; }
    public BigDecimal getWorkMd() { return workMd; }
    public String getMemo() { return memo; }
    public void setMemo(String memo) { this.memo = memo; }

    public void setDayType(DayType dayType) {
        this.dayType = dayType;
        this.workMd = dayType.workMd();
    }
}
