package com.worklog.item;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WorkItemRepository extends JpaRepository<WorkItem, Long> {

    interface TotalMd {
        Long getWorkItemId();

        BigDecimal getTotal();
    }

    List<WorkItem> findByUserIdOrderBySortOrderAscIdAsc(Long userId);

    Optional<WorkItem> findByIdAndUserId(Long id, Long userId);

    List<WorkItem> findByUserIdAndTag(Long userId, String tag);

    @Query("select max(w.sortOrder) from WorkItem w where w.userId = :userId and w.recurring = :recurring")
    Integer maxSortOrder(@Param("userId") Long userId, @Param("recurring") boolean recurring);

    @Query("select max(w.sortOrder) from WorkItem w where w.userId = :userId")
    Integer maxSortOrder(@Param("userId") Long userId);

    @Query("select distinct w.tag from WorkItem w where w.userId = :userId and w.tag is not null order by w.tag")
    List<String> findTags(@Param("userId") Long userId);

    @Query(value = "select work_item_id as workItemId, sum(md) as total from day_entries "
            + "where work_item_id in (:ids) group by work_item_id", nativeQuery = true)
    List<TotalMd> totalsFor(@Param("ids") Collection<Long> ids);

    @Query(value = "select count(*) > 0 from day_entries where work_item_id = :id", nativeQuery = true)
    boolean hasEntries(@Param("id") Long id);
}
