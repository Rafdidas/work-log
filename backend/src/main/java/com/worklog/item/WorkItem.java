package com.worklog.item;

import java.time.Instant;
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
@Table(name = "work_items")
public class WorkItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String title;

    private String tag;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WorkItemStatus status;

    @Column(nullable = false)
    private boolean recurring;

    @Column(name = "show_in_plan", nullable = false)
    private boolean showInPlan = true;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "started_at")
    private LocalDate startedAt;

    @Column(name = "completed_at")
    private LocalDate completedAt;

    protected WorkItem() {}

    public WorkItem(long userId, String title, WorkItemStatus status, int sortOrder, Instant createdAt) {
        this.userId = userId;
        this.title = title;
        this.status = status;
        this.sortOrder = sortOrder;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getTag() { return tag; }
    public void setTag(String tag) { this.tag = tag; }
    public WorkItemStatus getStatus() { return status; }
    public void setStatus(WorkItemStatus status) { this.status = status; }
    public boolean isRecurring() { return recurring; }
    public void setRecurring(boolean recurring) { this.recurring = recurring; }
    public boolean isShowInPlan() { return showInPlan; }
    public void setShowInPlan(boolean showInPlan) { this.showInPlan = showInPlan; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
    public Instant getCreatedAt() { return createdAt; }
    public LocalDate getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDate startedAt) { this.startedAt = startedAt; }
    public LocalDate getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDate completedAt) { this.completedAt = completedAt; }
}
