package com.worklog.day;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import com.worklog.IntegrationTest;

import jakarta.servlet.http.Cookie;

class DayIntegrationTest extends IntegrationTest {

    private static String entries(String dayMemo, String... entryJson) {
        return "{\"memo\":" + (dayMemo == null ? "null" : "\"" + dayMemo + "\"") + ",\"entries\":["
                + String.join(",", entryJson) + "]}";
    }

    private static String entry(long itemId, int weight, String memo) {
        return "{\"itemId\":" + itemId + ",\"weight\":" + weight + ",\"memo\":"
                + (memo == null ? "null" : "\"" + memo + "\"") + "}";
    }

    private void setType(Cookie cookie, String date, String type, boolean confirm, int expectedStatus)
            throws Exception {
        mvc.perform(put("/api/days/" + date + "/type").cookie(cookie).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dayType\":\"" + type + "\",\"confirm\":" + confirm + "}"))
                .andExpect(status().is(expectedStatus));
    }

    @Test
    void save_distributesMd() throws Exception {
        Cookie cookie = login("a@example.com");
        long voc = newItem(cookie, "{\"title\":\"VOC\"}");

        mvc.perform(put("/api/days/2026-11-02").cookie(cookie).contentType(MediaType.APPLICATION_JSON)
                        .content(entries(null, entry(voc, 2, "회원가입"), entry(itemId("회의"), 1, null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workMd").value(1.0))
                .andExpect(jsonPath("$.entries[0].title").value("VOC"))
                .andExpect(jsonPath("$.entries[0].md").value(0.67))
                .andExpect(jsonPath("$.entries[0].memo").value("회원가입"))
                .andExpect(jsonPath("$.entries[1].title").value("회의"))
                .andExpect(jsonPath("$.entries[1].md").value(0.33))
                .andExpect(jsonPath("$.entries[2].title").value("기타"))
                .andExpect(jsonPath("$.entries[2].weight").value(0));
    }

    @Test
    void save_waitingItem_becomesInProgress_startedOnThatDate() throws Exception {
        Cookie cookie = login("a@example.com");
        long waiting = newItem(cookie, "{\"title\":\"나중에\",\"status\":\"WAITING\"}");

        saveDay(cookie, "2026-10-28", entries(null, entry(waiting, 2, null)));

        mvc.perform(get("/api/work-items").cookie(cookie))
                .andExpect(jsonPath("$[0].title").value("나중에"))
                .andExpect(jsonPath("$[0].status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$[0].startedAt").value("2026-10-28"));
    }

    @Test
    void save_earlierDate_pullsStartedAtBack() throws Exception {
        Cookie cookie = login("a@example.com");
        long voc = newItem(cookie, "{\"title\":\"VOC\"}"); // startedAt 2026-11-04

        saveDay(cookie, "2026-11-02", entries(null, entry(voc, 1, null)));
        saveDay(cookie, "2026-11-03", entries(null, entry(voc, 1, null)));

        mvc.perform(get("/api/work-items").cookie(cookie))
                .andExpect(jsonPath("$[0].startedAt").value("2026-11-02"));
    }

    @Test
    void save_allZeroWeights_keepsMemos() throws Exception {
        Cookie cookie = login("a@example.com");
        long voc = newItem(cookie, "{\"title\":\"VOC\"}");

        mvc.perform(put("/api/days/2026-11-02").cookie(cookie).contentType(MediaType.APPLICATION_JSON)
                        .content(entries("하루 메모", entry(voc, 0, "정리"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.memo").value("하루 메모"))
                .andExpect(jsonPath("$.entries[0].memo").value("정리"))
                .andExpect(jsonPath("$.entries[0].md").value(0.0));
    }

    @Test
    void save_zeroWeightAndBlankMemo_removesRow() throws Exception {
        Cookie cookie = login("a@example.com");
        long voc = newItem(cookie, "{\"title\":\"VOC\"}");
        saveDay(cookie, "2026-11-02", entries(null, entry(voc, 1, null)));

        saveDay(cookie, "2026-11-02", entries(null, entry(voc, 0, null)));

        assertThat(jdbc.queryForObject("select count(*) from day_entries", Long.class)).isZero();
    }

    @Test
    void save_onAnnual_409() throws Exception {
        Cookie cookie = login("a@example.com");
        long voc = newItem(cookie, "{\"title\":\"VOC\"}");
        setType(cookie, "2026-11-05", "ANNUAL", false, 200);

        mvc.perform(put("/api/days/2026-11-05").cookie(cookie).contentType(MediaType.APPLICATION_JSON)
                        .content(entries(null, entry(voc, 1, null))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DAY_LOCKED"));
    }

    @Test
    void changeToAnnual_withEntries_requiresConfirm() throws Exception {
        Cookie cookie = login("a@example.com");
        long voc = newItem(cookie, "{\"title\":\"VOC\"}");
        saveDay(cookie, "2026-11-02", entries("하루 메모", entry(voc, 2, null), entry(itemId("회의"), 1, null)));

        setType(cookie, "2026-11-02", "ANNUAL", false, 409);
        mvc.perform(put("/api/days/2026-11-02/type").cookie(cookie).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dayType\":\"ANNUAL\",\"confirm\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dayType").value("ANNUAL"))
                .andExpect(jsonPath("$.workMd").value(0.0))
                .andExpect(jsonPath("$.memo").value("하루 메모"));

        assertThat(jdbc.queryForObject("select count(*) from day_entries", Long.class)).isZero();
    }

    @Test
    void changeToHalfAm_recomputes() throws Exception {
        Cookie cookie = login("a@example.com");
        long voc = newItem(cookie, "{\"title\":\"VOC\"}");
        saveDay(cookie, "2026-11-02", entries(null, entry(voc, 2, null), entry(itemId("회의"), 1, null)));

        mvc.perform(put("/api/days/2026-11-02/type").cookie(cookie).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dayType\":\"HALF_AM\",\"confirm\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workMd").value(0.5))
                .andExpect(jsonPath("$.entries[0].md").value(0.33))
                .andExpect(jsonPath("$.entries[1].md").value(0.17));
    }

    @Test
    void get_hidesItemsCompletedBeforeDate_unlessEntryExists() throws Exception {
        Cookie cookie = login("a@example.com");
        long voc = newItem(cookie, "{\"title\":\"VOC\"}");
        mvc.perform(patch("/api/work-items/" + voc + "/status").cookie(cookie)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"DONE\"}"))
                .andExpect(status().isOk()); // completedAt 2026-11-04

        mvc.perform(get("/api/days/2026-11-05").cookie(cookie))
                .andExpect(jsonPath("$.entries[?(@.title=='VOC')]").isEmpty());
        mvc.perform(get("/api/days/2026-11-04").cookie(cookie))
                .andExpect(jsonPath("$.entries[?(@.title=='VOC')]").isNotEmpty());

        saveDay(cookie, "2026-11-05", entries(null, entry(voc, 1, "뒤늦은 기록")));
        mvc.perform(get("/api/days/2026-11-05").cookie(cookie))
                .andExpect(jsonPath("$.entries[0].title").value("VOC"))
                .andExpect(jsonPath("$.entries[0].completedBefore").value(true));
    }

    @Test
    void get_listsWaitingItemsSeparately() throws Exception {
        Cookie cookie = login("a@example.com");
        newItem(cookie, "{\"title\":\"나중에\",\"status\":\"WAITING\"}");

        mvc.perform(get("/api/days/2026-11-02").cookie(cookie))
                .andExpect(jsonPath("$.waitingItems.length()").value(1))
                .andExpect(jsonPath("$.waitingItems[0].title").value("나중에"))
                .andExpect(jsonPath("$.entries[?(@.title=='나중에')]").isEmpty())
                .andExpect(jsonPath("$.dayType").value("WORK"))
                .andExpect(jsonPath("$.dayTypeLabel").value("근무"));
    }

    @Test
    void range_summary() throws Exception {
        Cookie cookie = login("a@example.com");
        long voc = newItem(cookie, "{\"title\":\"VOC\"}");
        saveDay(cookie, "2026-11-02", entries(null, entry(voc, 1, null)));
        setType(cookie, "2026-11-03", "HALF_PM", false, 200);

        mvc.perform(get("/api/days?from=2026-11-02&to=2026-11-06").cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].date").value("2026-11-02"))
                .andExpect(jsonPath("$[0].dayType").value("WORK"))
                .andExpect(jsonPath("$[0].totalMd").value(1.0))
                .andExpect(jsonPath("$[0].hasEntries").value(true))
                .andExpect(jsonPath("$[1].date").value("2026-11-03"))
                .andExpect(jsonPath("$[1].dayType").value("HALF_PM"))
                .andExpect(jsonPath("$[1].totalMd").value(0.0))
                .andExpect(jsonPath("$[1].hasEntries").value(false));
    }

    @Test
    void leaves_listsNonWorkDays() throws Exception {
        Cookie cookie = login("a@example.com");
        setType(cookie, "2026-11-06", "HALF_AM", false, 200);
        setType(cookie, "2026-11-05", "ANNUAL", false, 200);

        mvc.perform(get("/api/days/leaves?from=2026-11-01&to=2026-11-30").cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].date").value("2026-11-05"))
                .andExpect(jsonPath("$[0].dayType").value("ANNUAL"))
                .andExpect(jsonPath("$[1].date").value("2026-11-06"))
                .andExpect(jsonPath("$[1].dayTypeLabel").value("오전 반차"));
    }

    @Test
    void save_invalidWeight_400() throws Exception {
        Cookie cookie = login("a@example.com");
        long voc = newItem(cookie, "{\"title\":\"VOC\"}");

        mvc.perform(put("/api/days/2026-11-02").cookie(cookie).contentType(MediaType.APPLICATION_JSON)
                        .content(entries(null, entry(voc, 4, null))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void save_otherUsersItem_404() throws Exception {
        Cookie owner = login("a@example.com");
        Cookie other = login("b@example.com");
        long voc = newItem(owner, "{\"title\":\"VOC\"}");

        mvc.perform(put("/api/days/2026-11-02").cookie(other).contentType(MediaType.APPLICATION_JSON)
                        .content(entries(null, entry(voc, 1, null))))
                .andExpect(status().isNotFound());
    }
}
