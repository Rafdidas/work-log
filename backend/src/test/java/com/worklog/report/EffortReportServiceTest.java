package com.worklog.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import com.jayway.jsonpath.JsonPath;
import com.worklog.IntegrationTest;

import jakarta.servlet.http.Cookie;

class EffortReportServiceTest extends IntegrationTest {

    private static String entry(long itemId, int weight) {
        return "{\"itemId\":" + itemId + ",\"weight\":" + weight + ",\"memo\":null}";
    }

    private static String day(String... entries) {
        return "{\"memo\":null,\"entries\":[" + String.join(",", entries) + "]}";
    }

    private void setType(Cookie cookie, String date, String type) throws Exception {
        mvc.perform(put("/api/days/" + date + "/type").cookie(cookie).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dayType\":\"" + type + "\",\"confirm\":false}"))
                .andExpect(status().isOk());
    }

    @Test
    void effort_byTag_sumsAcrossItems() throws Exception {
        Cookie cookie = login("a@example.com");
        long issue = newItem(cookie, "{\"title\":\"위시드콘 이슈처리\",\"tag\":\"위시드콘\"}");
        long ads = newItem(cookie, "{\"title\":\"위시드콘 ADS 메뉴\",\"tag\":\"위시드콘\"}");
        long crm = newItem(cookie, "{\"title\":\"CRM 유지보수\",\"tag\":\"CRM\"}");
        saveDay(cookie, "2026-05-04", day(entry(issue, 1), entry(crm, 1))); // 위시드콘 0.50
        saveDay(cookie, "2026-06-02", day(entry(issue, 1), entry(ads, 1))); // 위시드콘 0.50 + 0.50

        mvc.perform(get("/api/reports/effort").cookie(cookie).param("tag", "위시드콘")
                        .param("from", "2026-04").param("to", "2026-06"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.label").value("위시드콘"))
                .andExpect(jsonPath("$.months.length()").value(3))
                .andExpect(jsonPath("$.months[0].month").value("2026-04"))
                .andExpect(jsonPath("$.months[0].md").value(0.0))
                .andExpect(jsonPath("$.months[1].md").value(0.5))
                .andExpect(jsonPath("$.months[2].md").value(1.0))
                .andExpect(jsonPath("$.total").value(1.5))
                .andExpect(jsonPath("$.text").value("""
                        위시드콘 투입 공수 (2026-04 ~ 2026-06)
                        4월 0.00MD
                        5월 0.50MD
                        6월 1.00MD
                        합계 1.50MD"""));
    }

    @Test
    void effort_fillsEmptyMonths_andYearBoundaryLabels() throws Exception {
        Cookie cookie = login("a@example.com");

        mvc.perform(get("/api/reports/effort").cookie(cookie).param("tag", "없는태그")
                        .param("from", "2026-12").param("to", "2027-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text").value("""
                        없는태그 투입 공수 (2026-12 ~ 2027-01)
                        2026-12 0.00MD
                        2027-01 0.00MD
                        합계 0.00MD"""));
    }

    @Test
    void effort_includesDoneItems() throws Exception {
        Cookie cookie = login("a@example.com");
        long voc = newItem(cookie, "{\"title\":\"VOC\"}");
        saveDay(cookie, "2026-11-02", day(entry(voc, 1)));
        mvc.perform(patch("/api/work-items/" + voc + "/status").cookie(cookie)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"DONE\"}"))
                .andExpect(status().isOk());

        mvc.perform(get("/api/reports/effort").cookie(cookie).param("itemId", String.valueOf(voc))
                        .param("from", "2026-11").param("to", "2026-11"))
                .andExpect(jsonPath("$.label").value("VOC"))
                .andExpect(jsonPath("$.total").value(1.0))
                .andExpect(jsonPath("$.text").value("""
                        VOC 투입 공수 (2026-11 ~ 2026-11)
                        11월 1.00MD
                        합계 1.00MD"""));
    }

    @Test
    void effort_bothOrNeitherTarget_400() throws Exception {
        Cookie cookie = login("a@example.com");

        mvc.perform(get("/api/reports/effort").cookie(cookie).param("from", "2026-04").param("to", "2026-06"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/reports/effort").cookie(cookie).param("tag", "x").param("itemId", "1")
                        .param("from", "2026-04").param("to", "2026-06"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void effort_fromAfterTo_400() throws Exception {
        Cookie cookie = login("a@example.com");

        mvc.perform(get("/api/reports/effort").cookie(cookie).param("tag", "x")
                        .param("from", "2026-06").param("to", "2026-04"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void effort_otherUsersItem_404() throws Exception {
        Cookie owner = login("a@example.com");
        Cookie other = login("b@example.com");
        long voc = newItem(owner, "{\"title\":\"VOC\"}");

        mvc.perform(get("/api/reports/effort").cookie(other).param("itemId", String.valueOf(voc))
                        .param("from", "2026-11").param("to", "2026-11"))
                .andExpect(status().isNotFound());
    }

    @Test
    void monthly_rowsRatioWorkDaysLeaves() throws Exception {
        Cookie cookie = login("a@example.com");
        long voc = newItem(cookie, "{\"title\":\"VOC\",\"tag\":\"위시드\"}");
        long meeting = itemId("회의");
        saveDay(cookie, "2026-11-02", day(entry(voc, 2), entry(meeting, 1))); // 0.67 / 0.33
        saveDay(cookie, "2026-11-03", day(entry(voc, 1))); // 1.00
        setType(cookie, "2026-11-05", "ANNUAL");
        setType(cookie, "2026-11-06", "HALF_AM");
        saveDay(cookie, "2026-11-06", day(entry(voc, 1))); // 0.50
        saveDay(cookie, "2026-12-01", day(entry(voc, 1))); // 다른 달

        mvc.perform(get("/api/reports/monthly").cookie(cookie).param("month", "2026-11"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.month").value("2026-11"))
                .andExpect(jsonPath("$.totalMd").value(2.5))
                .andExpect(jsonPath("$.workDays").value(3))
                .andExpect(jsonPath("$.leaveCounts.ANNUAL").value(1))
                .andExpect(jsonPath("$.leaveCounts.HALF_AM").value(1))
                .andExpect(jsonPath("$.leaveCounts.HALF_PM").value(0))
                .andExpect(jsonPath("$.rows.length()").value(2))
                .andExpect(jsonPath("$.rows[0].title").value("VOC"))
                .andExpect(jsonPath("$.rows[0].tag").value("위시드"))
                .andExpect(jsonPath("$.rows[0].md").value(2.17))
                .andExpect(jsonPath("$.rows[0].ratioPercent").value(86.8))
                .andExpect(jsonPath("$.rows[1].title").value("회의"))
                .andExpect(jsonPath("$.rows[1].md").value(0.33))
                .andExpect(jsonPath("$.rows[1].ratioPercent").value(13.2));
    }

    @Test
    void monthly_tsvHeader() throws Exception {
        Cookie cookie = login("a@example.com");
        long voc = newItem(cookie, "{\"title\":\"VOC\",\"tag\":\"위시드\"}");
        saveDay(cookie, "2026-11-02", day(entry(voc, 1)));

        String body = mvc.perform(get("/api/reports/monthly").cookie(cookie).param("month", "2026-11"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat((String) JsonPath.read(body, "$.tsv"))
                .isEqualTo("태그\t업무\tMD\t비율(%)\n위시드\tVOC\t1.00\t100.0");
    }
}
