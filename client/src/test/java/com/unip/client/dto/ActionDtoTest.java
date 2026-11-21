package com.unip.client.dto;

import org.junit.jupiter.api.Test;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ActionDtoTest {

    private ActionDto action(String status, LocalDate deadline) {
        return new ActionDto(1L, 1L, "Fix", 2L, "John", deadline, status, 1, null, null);
    }

    @Test
    void pendingActionPastDeadlineIsOverdue() {
        assertTrue(action("PENDING", LocalDate.now().minusDays(1)).overdue());
    }

    @Test
    void completedActionPastDeadlineIsNotOverdue() {
        assertFalse(action("COMPLETED", LocalDate.now().minusDays(1)).overdue());
    }

    @Test
    void pendingActionDueTodayOrLaterIsNotOverdue() {
        assertFalse(action("PENDING", LocalDate.now()).overdue());
        assertFalse(action("PENDING", LocalDate.now().plusDays(3)).overdue());
    }
}