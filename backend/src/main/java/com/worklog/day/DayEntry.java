package com.worklog.day;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "day_entries")
public class DayEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "day_log_id", nullable = false)
    private Long dayLogId;

    @Column(name = "work_item_id", nullable = false)
    private Long workItemId;

    @Column(nullable = false)
    private short weight;

    @Column(nullable = false)
    private BigDecimal md = BigDecimal.ZERO.setScale(2);

    private String memo;

    protected DayEntry() {}

    public DayEntry(long dayLogId, long workItemId) {
        this.dayLogId = dayLogId;
        this.workItemId = workItemId;
    }

    public Long getId() { return id; }
    public Long getDayLogId() { return dayLogId; }
    public Long getWorkItemId() { return workItemId; }
    public int getWeight() { return weight; }
    public void setWeight(int weight) { this.weight = (short) weight; }
    public BigDecimal getMd() { return md; }
    public void setMd(BigDecimal md) { this.md = md; }
    public String getMemo() { return memo; }
    public void setMemo(String memo) { this.memo = memo; }
}
