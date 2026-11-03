package com.worklog.item;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import com.jayway.jsonpath.JsonPath;
import com.worklog.IntegrationTest;

import jakarta.servlet.http.Cookie;

class WorkItemIntegrationTest extends IntegrationTest {

    private long createItem(Cookie cookie, String json) throws Exception {
        String body = mvc.perform(post("/api/work-items").cookie(cookie)
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private void setStatus(Cookie cookie, long id, String status) throws Exception {
        mvc.perform(patch("/api/work-items/" + id + "/status").cookie(cookie)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"" + status + "\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void create_defaultsToInProgress() throws Exception {
        Cookie cookie = login("a@example.com");

        mvc.perform(post("/api/work-items").cookie(cookie).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"위시드 VOC 홈페이지\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.startedAt").value("2026-11-04"))
                .andExpect(jsonPath("$.recurring").value(false))
                .andExpect(jsonPath("$.showInPlan").value(true))
                .andExpect(jsonPath("$.sortOrder").value(1))
                .andExpect(jsonPath("$.totalMd").value(0.0));
    }

    @Test
    void createInProgress_usesSeoulToday() throws Exception {
        Cookie cookie = login("a@example.com");
        clock.set(Instant.parse("2026-11-03T23:30:00Z")); // KST 2026-11-04 08:30

        mvc.perform(post("/api/work-items").cookie(cookie).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"새 업무\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.startedAt").value("2026-11-04"));
    }

    @Test
    void createWaiting_hasNoStartDate() throws Exception {
        Cookie cookie = login("a@example.com");

        mvc.perform(post("/api/work-items").cookie(cookie).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"나중에\",\"status\":\"WAITING\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("WAITING"))
                .andExpect(jsonPath("$.startedAt").doesNotExist());
    }

    @Test
    void complete_setsCompletedAt_thenReopen_clearsIt() throws Exception {
        Cookie cookie = login("a@example.com");
        long id = createItem(cookie, "{\"title\":\"VOC\"}");

        mvc.perform(patch("/api/work-items/" + id + "/status").cookie(cookie)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"DONE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DONE"))
                .andExpect(jsonPath("$.completedAt").value("2026-11-04"));

        mvc.perform(patch("/api/work-items/" + id + "/status").cookie(cookie)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"IN_PROGRESS\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.completedAt").doesNotExist());
    }

    @Test
    void waitingToInProgress_setsStartedAtToday() throws Exception {
        Cookie cookie = login("a@example.com");
        long id = createItem(cookie, "{\"title\":\"나중에\",\"status\":\"WAITING\"}");

        mvc.perform(patch("/api/work-items/" + id + "/status").cookie(cookie)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"IN_PROGRESS\"}"))
                .andExpect(jsonPath("$.startedAt").value("2026-11-04"));
    }

    @Test
    void doneToWaiting_409() throws Exception {
        Cookie cookie = login("a@example.com");
        long id = createItem(cookie, "{\"title\":\"VOC\"}");
        setStatus(cookie, id, "DONE");

        mvc.perform(patch("/api/work-items/" + id + "/status").cookie(cookie)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"WAITING\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_TRANSITION"));
    }

    @Test
    void update_changesFieldsAndClearsBlankTag() throws Exception {
        Cookie cookie = login("a@example.com");
        long id = createItem(cookie, "{\"title\":\"VOC\",\"tag\":\"위시드\"}");

        mvc.perform(patch("/api/work-items/" + id).cookie(cookie).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"VOC 홈페이지\",\"tag\":\"\",\"showInPlan\":false,\"recurring\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("VOC 홈페이지"))
                .andExpect(jsonPath("$.tag").doesNotExist())
                .andExpect(jsonPath("$.showInPlan").value(false))
                .andExpect(jsonPath("$.recurring").value(true));
    }

    @Test
    void delete_withoutEntries_204() throws Exception {
        Cookie cookie = login("a@example.com");
        long id = createItem(cookie, "{\"title\":\"VOC\"}");

        mvc.perform(delete("/api/work-items/" + id).cookie(cookie)).andExpect(status().isNoContent());
    }

    @Test
    void delete_withEntries_409() throws Exception {
        Cookie cookie = login("a@example.com");
        long id = createItem(cookie, "{\"title\":\"VOC\"}");
        insertEntry(id, "2026-11-02", "1.50");

        mvc.perform(delete("/api/work-items/" + id).cookie(cookie))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("HAS_ENTRIES"));
    }

    @Test
    void list_includesTotalMdAndSortOrder() throws Exception {
        Cookie cookie = login("a@example.com");
        long first = createItem(cookie, "{\"title\":\"A\"}");
        createItem(cookie, "{\"title\":\"B\"}");
        insertEntry(first, "2026-11-02", "1.50");
        insertEntry(first, "2026-11-03", "0.25");

        mvc.perform(get("/api/work-items").cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("A"))
                .andExpect(jsonPath("$[0].totalMd").value(1.75))
                .andExpect(jsonPath("$[1].title").value("B"))
                .andExpect(jsonPath("$[1].totalMd").value(0.0))
                .andExpect(jsonPath("$[2].title").value("회의"))
                .andExpect(jsonPath("$[3].title").value("기타"));
    }

    @Test
    void otherUsersItem_404() throws Exception {
        Cookie owner = login("a@example.com");
        Cookie other = login("b@example.com");
        long id = createItem(owner, "{\"title\":\"VOC\"}");

        mvc.perform(patch("/api/work-items/" + id).cookie(other).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"탈취\"}"))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/work-items/" + id).cookie(other)).andExpect(status().isNotFound());
    }

    @Test
    void tags_distinctSorted() throws Exception {
        Cookie cookie = login("a@example.com");
        createItem(cookie, "{\"title\":\"1\",\"tag\":\"위시드콘\"}");
        createItem(cookie, "{\"title\":\"2\",\"tag\":\"위시드콘\"}");
        createItem(cookie, "{\"title\":\"3\",\"tag\":\"CRM\"}");
        createItem(cookie, "{\"title\":\"4\"}");

        mvc.perform(get("/api/work-items/tags").cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0]").value("CRM"))
                .andExpect(jsonPath("$[1]").value("위시드콘"));
    }

    @Test
    void reorder_appliesOrder() throws Exception {
        Cookie cookie = login("a@example.com");
        long a = createItem(cookie, "{\"title\":\"A\"}");
        long b = createItem(cookie, "{\"title\":\"B\"}");

        mvc.perform(put("/api/work-items/order").cookie(cookie).contentType(MediaType.APPLICATION_JSON)
                        .content("[" + b + "," + a + "]"))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/work-items").cookie(cookie))
                .andExpect(jsonPath("$[0].title").value("B"))
                .andExpect(jsonPath("$[1].title").value("A"));
    }

    private void insertEntry(long itemId, String date, String md) {
        Long userId = jdbc.queryForObject("select user_id from work_items where id = ?", Long.class, itemId);
        Long dayLogId = jdbc.query("select id from day_logs where user_id = ? and date = ?::date",
                rs -> rs.next() ? rs.getLong(1) : null, userId, date);
        if (dayLogId == null) {
            dayLogId = jdbc.queryForObject(
                    "insert into day_logs(user_id, date, day_type, work_md) values (?, ?::date, 'WORK', 1.00) returning id",
                    Long.class, userId, date);
        }
        jdbc.update("insert into day_entries(day_log_id, work_item_id, weight, md) values (?, ?, 1, ?::numeric)",
                dayLogId, itemId, md);
    }
}
