package com.worklog.item;

import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkItemRepository extends JpaRepository<WorkItem, Long> {
}
