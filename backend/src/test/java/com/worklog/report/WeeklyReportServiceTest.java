package com.worklog.report;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import com.worklog.IntegrationTest;

import jakarta.servlet.http.Cookie;

class WeeklyReportServiceTest extends IntegrationTest {

    private static final String WEEK = "/api/reports/weekly?monday=2026-11-02"; // 2026-11-02 ~ 2026-11-06

    private static String day(String memo, long itemId, int weight, String itemMemo) {
        return "{\"memo\":" + (memo == null ? "null" : "\"" + memo + "\"") + ",\"entries\":[{\"itemId\":" + itemId
                + ",\"weight\":" + weight + ",\"memo\":" + (itemMemo == null ? "null" : "\"" + itemMemo + "\"")
                + "}]}";
    }

    private void complete(Cookie cookie, long itemId) throws Exception {
        mvc.perform(patch("/api/work-items/" + itemId + "/status").cookie(cookie)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"DONE\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void title_usesTeamLabel() throws Exception {
        Cookie cookie = login("a@example.com");

        mvc.perform(get("/api/reports/weekly?monday=2026-10-26").cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("2026-10-26 ~ 2026-10-30 개발1팀"));

        mvc.perform(patch("/api/me").cookie(cookie).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"teamLabel\":\"개발2팀\"}"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/reports/weekly?monday=2026-10-26").cookie(cookie))
                .andExpect(jsonPath("$.title").value("2026-10-26 ~ 2026-10-30 개발2팀"));
    }

    @Test
    void itemCompletedMidWeek() throws Exception {
        Cookie cookie = login("a@example.com");
        long voc = newItem(cookie, "{\"title\":\"VOC\"}");
        saveDay(cookie, "2026-11-03", day(null, voc, 1, null));
        complete(cookie, voc); // 2026-11-04 (수)

        mvc.perform(get(WEEK).cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.todo.length()").value(1))
                .andExpect(jsonPath("$.todo[0].title").value("VOC"))
                .andExpect(jsonPath("$.achieved[0].title").value("VOC"))
                .andExpect(jsonPath("$.achieved[0].completed").value(true))
                .andExpect(jsonPath("$.byDay.length()").value(5))
                .andExpect(jsonPath("$.byDay[1].date").value("2026-11-03"))
                .andExpect(jsonPath("$.byDay[1].entries[0].title").value("VOC"))
                .andExpect(jsonPath("$.byDay[1].entries[0].completed").value(true))
                .andExpect(jsonPath("$.next.length()").value(0));
    }

    @Test
    void itemCompletedLastWeek_excluded() throws Exception {
        Cookie cookie = login("a@example.com");
        clock.set(Instant.parse("2026-10-30T01:00:00Z"));
        long voc = newItem(cookie, "{\"title\":\"VOC\"}");
        complete(cookie, voc); // completedAt 2026-10-30 (금) — 지난주

        mvc.perform(get(WEEK).cookie(cookie))
                .andExpect(jsonPath("$.todo.length()").value(0))
                .andExpect(jsonPath("$.next.length()").value(0));
    }

    @Test
    void itemCreatedNextWeek_excluded() throws Exception {
        Cookie cookie = login("a@example.com");
        clock.set(Instant.parse("2026-11-10T01:00:00Z"));
        newItem(cookie, "{\"title\":\"다음 주 업무\"}");

        mvc.perform(get(WEEK).cookie(cookie))
                .andExpect(jsonPath("$.todo.length()").value(0))
                .andExpect(jsonPath("$.next.length()").value(0));
    }

    @Test
    void waitingAndInProgressItems_appearInTodoAndNext() throws Exception {
        Cookie cookie = login("a@example.com");
        newItem(cookie, "{\"title\":\"진행\"}");
        newItem(cookie, "{\"title\":\"대기\",\"status\":\"WAITING\"}");

        mvc.perform(get(WEEK).cookie(cookie))
                .andExpect(jsonPath("$.todo.length()").value(2))
                .andExpect(jsonPath("$.next.length()").value(2))
                .andExpect(jsonPath("$.next[0].title").value("진행"))
                .andExpect(jsonPath("$.next[1].title").value("대기"));
    }

    @Test
    void backDatedStartDate_includesItemInPastWeeksPlan() throws Exception {
        Cookie cookie = login("a@example.com");
        long voc = newItem(cookie, "{\"title\":\"VOC\"}"); // 오늘(2026-11-04)에 등록
        mvc.perform(patch("/api/work-items/" + voc).cookie(cookie).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"startedAt\":\"2026-10-01\"}"))
                .andExpect(status().isOk());

        mvc.perform(get("/api/reports/weekly?monday=2026-10-26").cookie(cookie))
                .andExpect(jsonPath("$.todo.length()").value(1))
                .andExpect(jsonPath("$.todo[0].title").value("VOC"))
                .andExpect(jsonPath("$.next.length()").value(1));
        mvc.perform(get("/api/reports/weekly?monday=2026-09-21").cookie(cookie)) // 시작일(10/1) 이전 주
                .andExpect(jsonPath("$.todo.length()").value(0));
    }

    @Test
    void recurringHiddenFromPlan_onlyInAchieved() throws Exception {
        Cookie cookie = login("a@example.com");
        saveDay(cookie, "2026-11-02", day(null, itemId("회의"), 1, "주간회의"));

        mvc.perform(get(WEEK).cookie(cookie))
                .andExpect(jsonPath("$.todo.length()").value(0))
                .andExpect(jsonPath("$.next.length()").value(0))
                .andExpect(jsonPath("$.achieved[0].title").value("회의"))
                .andExpect(jsonPath("$.achieved[0].md").value(1.0));
    }

    @Test
    void zeroMdEntries_notAchieved() throws Exception {
        Cookie cookie = login("a@example.com");
        long voc = newItem(cookie, "{\"title\":\"VOC\"}");
        saveDay(cookie, "2026-11-02", day(null, voc, 0, "정리만 함"));

        mvc.perform(get(WEEK).cookie(cookie)).andExpect(jsonPath("$.achieved.length()").value(0));
    }

    @Test
    void achieved_collectsDaysMdAndMemos() throws Exception {
        Cookie cookie = login("a@example.com");
        long voc = newItem(cookie, "{\"title\":\"VOC\"}");
        saveDay(cookie, "2026-11-02", day(null, voc, 1, "회원가입"));
        saveDay(cookie, "2026-11-03", day(null, voc, 1, null));
        saveDay(cookie, "2026-11-04", day(null, voc, 1, "VOC 작성 완료"));

        mvc.perform(get(WEEK).cookie(cookie))
                .andExpect(jsonPath("$.achieved.length()").value(1))
                .andExpect(jsonPath("$.achieved[0].days[0]").value("MONDAY"))
                .andExpect(jsonPath("$.achieved[0].days[1]").value("TUESDAY"))
                .andExpect(jsonPath("$.achieved[0].days[2]").value("WEDNESDAY"))
                .andExpect(jsonPath("$.achieved[0].md").value(3.0))
                .andExpect(jsonPath("$.achieved[0].memos.length()").value(2))
                .andExpect(jsonPath("$.achieved[0].memos[0].day").value("MONDAY"))
                .andExpect(jsonPath("$.achieved[0].memos[0].memo").value("회원가입"))
                .andExpect(jsonPath("$.achieved[0].memos[1].day").value("WEDNESDAY"))
                .andExpect(jsonPath("$.achieved[0].completed").value(false));
    }

    @Test
    void leaveLines() throws Exception {
        Cookie cookie = login("a@example.com");
        mvc.perform(put("/api/days/2026-11-05/type").cookie(cookie).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dayType\":\"HALF_PM\",\"confirm\":false}"))
                .andExpect(status().isOk());

        mvc.perform(get(WEEK).cookie(cookie))
                .andExpect(jsonPath("$.leaves.length()").value(1))
                .andExpect(jsonPath("$.leaves[0].date").value("2026-11-05"))
                .andExpect(jsonPath("$.leaves[0].type").value("HALF_PM"))
                .andExpect(jsonPath("$.text").value(org.hamcrest.Matchers.containsString("목(11/5) 오후 반차")));
    }

    @Test
    void text_isTheFormattedReport() throws Exception {
        Cookie cookie = login("a@example.com");
        long voc = newItem(cookie, "{\"title\":\"VOC\"}");
        saveDay(cookie, "2026-11-02", day(null, voc, 1, "회원가입"));

        mvc.perform(get(WEEK).cookie(cookie))
                .andExpect(jsonPath("$.text").value("""
                        2026-11-02 ~ 2026-11-06 개발1팀

                        [해야 할 일]
                        - VOC

                        [실제 달성한 일]
                        월(11/2)
                        - VOC (1.00MD)
                          회원가입
                        화(11/3)
                        - 없음
                        수(11/4)
                        - 없음
                        목(11/5)
                        - 없음
                        금(11/6)
                        - 없음

                        [차주 업무]
                        - VOC"""));
    }

    @Test
    void nonMonday_returns400() throws Exception {
        Cookie cookie = login("a@example.com");

        mvc.perform(get("/api/reports/weekly?monday=2026-10-27").cookie(cookie))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("NOT_MONDAY"));
    }
}
