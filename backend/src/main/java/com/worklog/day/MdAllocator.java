package com.worklog.day;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 하루 근무 MD를 비중대로 0.01 단위로 나눈다. 합계가 항상 정확히 근무 MD가 되도록 최대 나머지 방식을 쓴다. */
public final class MdAllocator {

    private MdAllocator() {}

    public static Map<Long, BigDecimal> allocate(BigDecimal workMd, LinkedHashMap<Long, Integer> weights) {
        int totalUnits = workMd.movePointRight(2).intValueExact();
        long weightSum = weights.values().stream().mapToLong(Integer::longValue).sum();

        Map<Long, Integer> units = new LinkedHashMap<>();
        weights.keySet().forEach(id -> units.put(id, 0));
        if (weightSum == 0 || totalUnits == 0) {
            return toMd(units);
        }

        record Remainder(long id, long remainder, int order) {}
        List<Remainder> remainders = new ArrayList<>();
        int assigned = 0;
        int order = 0;
        for (Map.Entry<Long, Integer> e : weights.entrySet()) {
            long scaled = (long) totalUnits * e.getValue();
            int base = (int) (scaled / weightSum);
            units.put(e.getKey(), base);
            assigned += base;
            if (e.getValue() > 0) {
                remainders.add(new Remainder(e.getKey(), scaled % weightSum, order));
            }
            order++;
        }

        remainders.sort(Comparator.comparingLong(Remainder::remainder).reversed()
                .thenComparingInt(Remainder::order));
        int leftover = totalUnits - assigned;
        for (int i = 0; i < leftover; i++) {
            Remainder r = remainders.get(i);
            units.merge(r.id(), 1, Integer::sum);
        }
        return toMd(units);
    }

    private static Map<Long, BigDecimal> toMd(Map<Long, Integer> units) {
        Map<Long, BigDecimal> result = new LinkedHashMap<>();
        units.forEach((id, u) -> result.put(id, BigDecimal.valueOf(u, 2)));
        return result;
    }
}
