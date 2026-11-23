package com.unip.client.dto;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReportDtosTest {

    private SummaryDto summary(long total) {
        return new SummaryDto(total, Map.of(), Map.of(), Map.of());
    }

    @Test
    void percentIsRoundedShareOfTheTotal() {
        assertEquals(50, summary(4).percent(2));
        assertEquals(33, summary(3).percent(1));
        assertEquals(100, summary(5).percent(5));
    }

    @Test
    void percentWithNoIncidentsIsZeroInsteadOfDividingByZero() {
        assertEquals(0, summary(0).percent(0));
    }

    private MyActionDto action(LocalDate deadline) {
        return new MyActionDto(1L, 1L, "Dripping tap", "IN_PROGRESS", "Fix", deadline, 1);
    }

    @Test
    void actionIsOverdueOnlyAfterItsDeadline() {
        assertTrue(action(LocalDate.now().minusDays(1)).overdue());
        assertFalse(action(LocalDate.now()).overdue());
        assertFalse(action(LocalDate.now().plusDays(2)).overdue());
    }
}