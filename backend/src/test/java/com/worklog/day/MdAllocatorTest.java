package com.worklog.day;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

class MdAllocatorTest {

    private static LinkedHashMap<Long, Integer> weights(int... values) {
        LinkedHashMap<Long, Integer> map = new LinkedHashMap<>();
        for (int i = 0; i < values.length; i++) {
            map.put((long) (i + 1), values[i]);
        }
        return map;
    }

    private static BigDecimal md(String value) {
        return new BigDecimal(value);
    }

    @Test
    void twoToOne() {
        Map<Long, BigDecimal> result = MdAllocator.allocate(md("1.00"), weights(2, 1));

        assertThat(result).containsEntry(1L, md("0.67")).containsEntry(2L, md("0.33"));
    }

    @Test
    void threeEqual_sumsExactly() {
        Map<Long, BigDecimal> result = MdAllocator.allocate(md("1.00"), weights(1, 1, 1));

        assertThat(result).containsEntry(1L, md("0.34")).containsEntry(2L, md("0.33")).containsEntry(3L, md("0.33"));
    }

    @Test
    void halfDay() {
        assertThat(MdAllocator.allocate(md("0.50"), weights(1))).containsEntry(1L, md("0.50"));
    }

    @Test
    void halfDay_twoToOne_largestRemainderGetsTheSpare() {
        Map<Long, BigDecimal> result = MdAllocator.allocate(md("0.50"), weights(2, 1));

        assertThat(result).containsEntry(1L, md("0.33")).containsEntry(2L, md("0.17"));
    }

    @Test
    void zeroWeightGetsZero() {
        Map<Long, BigDecimal> result = MdAllocator.allocate(md("1.00"), weights(3, 0));

        assertThat(result).containsEntry(1L, md("1.00")).containsEntry(2L, md("0.00"));
    }

    @Test
    void allZero() {
        Map<Long, BigDecimal> result = MdAllocator.allocate(md("1.00"), weights(0, 0));

        assertThat(result).containsEntry(1L, md("0.00")).containsEntry(2L, md("0.00"));
    }

    @Test
    void zeroWorkMd_allZero() {
        Map<Long, BigDecimal> result = MdAllocator.allocate(md("0.00"), weights(2, 1));

        assertThat(result).containsEntry(1L, md("0.00")).containsEntry(2L, md("0.00"));
    }

    @Test
    void sevenWays_sumIsWorkMd() {
        Map<Long, BigDecimal> result = MdAllocator.allocate(md("1.00"), weights(1, 1, 1, 1, 1, 1, 1));

        BigDecimal sum = result.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(sum).isEqualByComparingTo("1.00");
        assertThat(result).hasSize(7);
    }
}
