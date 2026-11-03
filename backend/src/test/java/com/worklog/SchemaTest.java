package com.worklog;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class SchemaTest extends IntegrationTest {

    @Autowired JdbcTemplate template;

    @Test
    void migrationsApply() {
        List<String> tables = template.queryForList(
                "select table_name from information_schema.tables where table_schema = 'public'", String.class);
        assertThat(tables).contains("users", "teams", "work_items", "day_logs", "day_entries");
    }
}
